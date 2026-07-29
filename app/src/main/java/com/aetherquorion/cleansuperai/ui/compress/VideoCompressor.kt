package com.aetherquorion.cleansuperai.ui.compress

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Re-encodes video through a decoder -> OpenGL texture -> encoder pipeline.
 * The GL hop keeps frame timestamps and avoids the black output that can happen
 * when a decoder surface is wired directly to an encoder surface on some devices.
 */
object VideoCompressor {
    private const val TIMEOUT_US = 10_000L
    private const val OUTPUT_MIME = "video/avc"
    private const val DEFAULT_FRAME_RATE = 30
    private const val DEFAULT_BITRATE = 2_000_000
    private const val MIN_BITRATE = 192_000
    private const val EGL_RECORDABLE_ANDROID = 0x3142
    private const val FRAME_WAIT_TIMEOUT_MS = 10_000L

    suspend fun compress(
        context: Context,
        inputUri: Uri,
        maxHeight: Int = 720,
        bitrate: Int = DEFAULT_BITRATE,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.mp4")
        if (outputFile.exists()) outputFile.delete()

        try {
            transcode(context, inputUri, outputFile, maxHeight, bitrate, onProgress)
            if (!outputFile.exists() || outputFile.length() <= 0L) {
                error("Compressed video is empty")
            }
            outputFile
        } catch (throwable: Throwable) {
            outputFile.delete()
            throw throwable
        }
    }

    private suspend fun transcode(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        maxHeight: Int,
        bitrate: Int,
        onProgress: (Float) -> Unit,
    ) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, inputUri, null)

        val videoTrack = findTrack(extractor, "video/") ?: error("No video track")
        val sourceAudioTrack = findTrack(extractor, "audio/")
        val audioTrack = sourceAudioTrack?.takeIf {
            isMp4CompatibleAudio(extractor.getTrackFormat(it))
        }
        extractor.selectTrack(videoTrack)

        val inputVideoFormat = extractor.getTrackFormat(videoTrack)
        val inputMime = inputVideoFormat.getString(MediaFormat.KEY_MIME) ?: error("No video mime")
        val durationUs = inputVideoFormat.getLongOrDefault(
            MediaFormat.KEY_DURATION,
            estimateDurationUs(context, inputUri),
        )
        val rotation = inputVideoFormat.getIntegerOrDefault(MediaFormat.KEY_ROTATION, 0)
        val srcWidth = inputVideoFormat.getInteger(MediaFormat.KEY_WIDTH)
        val srcHeight = inputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val (requestedWidth, requestedHeight) =
            scaledEncodeSize(srcWidth, srcHeight, maxHeight, rotation)
        val targetBitrate = targetBitrate(inputVideoFormat, bitrate)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        if (rotation != 0) {
            muxer.setOrientationHint(rotation)
        }

        var muxerStarted = false
        var videoMuxIndex = -1
        var audioMuxIndex = -1
        val bufferInfo = MediaCodec.BufferInfo()

        val encoder = MediaCodec.createEncoderByType(OUTPUT_MIME)
        val (outWidth, outHeight) =
            supportedEncodeSize(encoder, requestedWidth, requestedHeight)
        val encoderBitrate = supportedBitrate(encoder, targetBitrate)
        val encoderFormat = MediaFormat.createVideoFormat(OUTPUT_MIME, outWidth, outHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, encoderBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, inputVideoFormat.frameRateOrDefault(DEFAULT_FRAME_RATE))
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var inputSurface: CodecInputSurface? = null
        var outputSurface: CodecOutputSurface? = null
        var decoder: MediaCodec? = null

        try {
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = CodecInputSurface(encoder.createInputSurface()).also {
                it.makeCurrent()
            }
            encoder.start()

            outputSurface = CodecOutputSurface(srcWidth, srcHeight, outWidth, outHeight)
            decoder = MediaCodec.createDecoderByType(inputMime).also {
                it.configure(inputVideoFormat, outputSurface.surface, null, 0)
                it.start()
            }
            val videoDecoder = decoder
            val encoderInputSurface = inputSurface
            val decoderOutputSurface = outputSurface

            var decoderInputDone = false
            var decoderOutputDone = false
            var encoderDone = false
            var lastProgress = 0f

            while (!encoderDone) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()

                if (!decoderInputDone) {
                    val inputBufferIndex = videoDecoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = videoDecoder.getInputBuffer(inputBufferIndex)
                            ?: error("Null decoder input buffer")
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            videoDecoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            decoderInputDone = true
                        } else {
                            videoDecoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                var decoderOutputAvailable = !decoderOutputDone
                var encoderOutputAvailable = true
                while (decoderOutputAvailable || encoderOutputAvailable) {
                    val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            encoderOutputAvailable = false
                        }
                        encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) error("Encoder format changed twice")
                            videoMuxIndex = muxer.addTrack(encoder.outputFormat)
                            if (audioTrack != null) {
                                audioMuxIndex = runCatching {
                                    muxer.addTrack(extractor.getTrackFormat(audioTrack))
                                }.getOrElse { throwable ->
                                    Log.w(TAG, "Skipping an audio track the MP4 muxer rejected", throwable)
                                    -1
                                }
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                        encoderStatus >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(encoderStatus)
                                ?: error("Null encoder output buffer")
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0) {
                                if (!muxerStarted) error("Muxer has not started")
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(videoMuxIndex, encodedData, bufferInfo)
                                if (durationUs > 0) {
                                    val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs)
                                        .coerceIn(0f, 0.98f)
                                    if (progress - lastProgress >= 0.01f) {
                                        lastProgress = progress
                                        onProgress(progress)
                                    }
                                }
                            }
                            encoder.releaseOutputBuffer(encoderStatus, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                encoderDone = true
                                encoderOutputAvailable = false
                            }
                        }
                    }
                    if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
                        continue
                    }

                    if (!decoderOutputDone) {
                        val decoderStatus = videoDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                        when {
                            decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                                decoderOutputAvailable = false
                            }
                            decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                            decoderStatus >= 0 -> {
                                val doRender = bufferInfo.size > 0
                                videoDecoder.releaseOutputBuffer(decoderStatus, doRender)
                                if (doRender) {
                                    decoderOutputSurface.awaitNewImage()
                                    decoderOutputSurface.drawImage()
                                    encoderInputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000L)
                                    encoderInputSurface.swapBuffers()
                                }
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    decoderOutputDone = true
                                    decoderOutputAvailable = false
                                    encoder.signalEndOfInputStream()
                                }
                            }
                        }
                    }
                }
            }

            if (audioTrack != null && muxerStarted && audioMuxIndex >= 0) {
                copyAudioTrack(context, inputUri, audioTrack, muxer, audioMuxIndex)
            }
            onProgress(1f)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            outputSurface?.release()
            inputSurface?.release()
            extractor.release()
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private fun copyAudioTrack(
        context: Context,
        inputUri: Uri,
        trackIndex: Int,
        muxer: MediaMuxer,
        muxIndex: Int,
    ) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, inputUri, null)
            extractor.selectTrack(trackIndex)
            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val format = extractor.getTrackFormat(trackIndex)
            val maxInputSize = format.getIntegerOrDefault(MediaFormat.KEY_MAX_INPUT_SIZE, 1_048_576)
            val buffer = ByteBuffer.allocate(maxInputSize.coerceAtLeast(256 * 1024))
            val info = MediaCodec.BufferInfo()
            while (true) {
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags =
                    if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                muxer.writeSampleData(muxIndex, buffer, info)
                extractor.advance()
            }
        } finally {
            extractor.release()
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith(prefix)) return i
        }
        return null
    }

    private fun scaledEncodeSize(
        width: Int,
        height: Int,
        maxDisplayHeight: Int,
        rotation: Int,
    ): Pair<Int, Int> {
        val swapped = rotation == 90 || rotation == 270
        val displayWidth = if (swapped) height else width
        val displayHeight = if (swapped) width else height
        if (displayHeight <= maxDisplayHeight) {
            return alignEven(width) to alignEven(height)
        }
        val scale = maxDisplayHeight.toFloat() / displayHeight.toFloat()
        val encodedWidth = if (swapped) maxDisplayHeight else (displayWidth * scale).toInt()
        val encodedHeight = if (swapped) (displayWidth * scale).toInt() else maxDisplayHeight
        return alignEven(encodedWidth) to alignEven(encodedHeight)
    }

    private fun alignEven(value: Int): Int = ((value.coerceAtLeast(2)) / 2) * 2

    private fun supportedEncodeSize(
        encoder: MediaCodec,
        requestedWidth: Int,
        requestedHeight: Int,
    ): Pair<Int, Int> {
        val capabilities = runCatching {
            encoder.codecInfo.getCapabilitiesForType(OUTPUT_MIME).videoCapabilities
        }.getOrNull() ?: return requestedWidth to requestedHeight
        val widthAlignment = capabilities.widthAlignment.coerceAtLeast(2)
        val heightAlignment = capabilities.heightAlignment.coerceAtLeast(2)

        var scale = 1f
        repeat(24) {
            val width = alignDown(
                (requestedWidth * scale).toInt(),
                widthAlignment,
            )
            val height = alignDown(
                (requestedHeight * scale).toInt(),
                heightAlignment,
            )
            if (runCatching { capabilities.isSizeSupported(width, height) }.getOrDefault(false)) {
                return width to height
            }
            scale *= 0.9f
        }
        error("No supported AVC output size for ${requestedWidth}x$requestedHeight")
    }

    private fun supportedBitrate(encoder: MediaCodec, requestedBitrate: Int): Int {
        val range = runCatching {
            encoder.codecInfo.getCapabilitiesForType(OUTPUT_MIME).videoCapabilities?.bitrateRange
        }.getOrNull() ?: return requestedBitrate
        return requestedBitrate.coerceIn(range.lower, range.upper)
    }

    private fun alignDown(value: Int, alignment: Int): Int =
        (value.coerceAtLeast(alignment) / alignment) * alignment

    private fun targetBitrate(inputVideoFormat: MediaFormat, requestedBitrate: Int): Int {
        val inputBitrate = inputVideoFormat.getIntegerOrDefault(MediaFormat.KEY_BIT_RATE, 0)
        if (inputBitrate <= 0) return requestedBitrate
        return minOf(requestedBitrate, (inputBitrate * 0.72f).toInt())
            .coerceAtLeast(MIN_BITRATE)
    }

    private fun isMp4CompatibleAudio(format: MediaFormat): Boolean {
        return when (format.getString(MediaFormat.KEY_MIME).orEmpty()) {
            "audio/mp4a-latm",
            "audio/3gpp",
            "audio/amr-wb",
            -> true
            else -> false
        }
    }

    private fun estimateDurationUs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000L
        } catch (_: Throwable) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, default: Int): Int =
        if (containsKey(key)) {
            runCatching { getInteger(key) }.getOrDefault(default)
        } else {
            default
        }

    private fun MediaFormat.getLongOrDefault(key: String, default: Long): Long =
        if (containsKey(key)) getLong(key) else default

    private fun MediaFormat.frameRateOrDefault(default: Int): Int {
        if (!containsKey(MediaFormat.KEY_FRAME_RATE)) return default
        return runCatching { getInteger(MediaFormat.KEY_FRAME_RATE) }.getOrElse {
            runCatching { getFloat(MediaFormat.KEY_FRAME_RATE).toInt() }.getOrDefault(default)
        }.coerceIn(1, 30)
    }

    private class CodecInputSurface(private val surface: Surface) {
        private var eglDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface = EGL14.EGL_NO_SURFACE

        init {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) error("Unable to get EGL display")
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                error("Unable to initialize EGL")
            }
            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            val configSelected = EGL14.eglChooseConfig(
                eglDisplay,
                attribList,
                0,
                configs,
                0,
                configs.size,
                numConfigs,
                0,
            )
            if (!configSelected || numConfigs[0] <= 0 || configs[0] == null) {
                error("Unable to find a recordable EGL config")
            }
            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                contextAttribs,
                0,
            )
            checkEglError("eglCreateContext")
            if (eglContext == EGL14.EGL_NO_CONTEXT) error("Unable to create EGL context")
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
            checkEglError("eglCreateWindowSurface")
            if (eglSurface == EGL14.EGL_NO_SURFACE) error("Unable to create EGL surface")
        }

        fun makeCurrent() {
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                error("eglMakeCurrent failed")
            }
        }

        fun swapBuffers() {
            if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                checkEglError("eglSwapBuffers")
            }
        }

        fun setPresentationTime(nsecs: Long) {
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
        }

        fun release() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglReleaseThread()
                EGL14.eglTerminate(eglDisplay)
            }
            surface.release()
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
        }

        private fun checkEglError(msg: String) {
            val error = EGL14.eglGetError()
            if (error != EGL14.EGL_SUCCESS) error("$msg: EGL error 0x${Integer.toHexString(error)}")
        }
    }

    private class CodecOutputSurface(
        width: Int,
        height: Int,
        private val viewportWidth: Int,
        private val viewportHeight: Int,
    ) : SurfaceTexture.OnFrameAvailableListener {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        private val frameSyncObject = java.lang.Object()
        private var frameAvailable = false
        private val textureRender = STextureRender()
        private val surfaceTexture: SurfaceTexture
        val surface: Surface

        init {
            textureRender.surfaceCreated()
            surfaceTexture = SurfaceTexture(textureRender.textureId).apply {
                setDefaultBufferSize(width, height)
                setOnFrameAvailableListener(this@CodecOutputSurface)
            }
            surface = Surface(surfaceTexture)
        }

        override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
            synchronized(frameSyncObject) {
                frameAvailable = true
                frameSyncObject.notifyAll()
            }
        }

        fun awaitNewImage() {
            synchronized(frameSyncObject) {
                while (!frameAvailable) {
                    frameSyncObject.wait(FRAME_WAIT_TIMEOUT_MS)
                    if (!frameAvailable) error("Surface frame wait timed out")
                }
                frameAvailable = false
            }
            textureRender.checkGlError("before updateTexImage")
            surfaceTexture.updateTexImage()
        }

        fun drawImage() {
            GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
            textureRender.drawFrame(surfaceTexture)
        }

        fun release() {
            surface.release()
            surfaceTexture.release()
        }
    }

    private class STextureRender {
        private val triangleVertices: FloatBuffer = ByteBuffer.allocateDirect(TRIANGLE_VERTICES_DATA.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(TRIANGLE_VERTICES_DATA)
                position(0)
            }
        private val mvpMatrix = FloatArray(16)
        private val stMatrix = FloatArray(16)
        private var program = 0
        private var textureHandle = 0
        private var mvpMatrixHandle = 0
        private var stMatrixHandle = 0
        private var positionHandle = 0
        private var textureCoordHandle = 0
        var textureId = -1
            private set

        init {
            Matrix.setIdentityM(mvpMatrix, 0)
            Matrix.setIdentityM(stMatrix, 0)
        }

        fun surfaceCreated() {
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            if (program == 0) error("Unable to create GL program")
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            checkLocation(positionHandle, "aPosition")
            textureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
            checkLocation(textureCoordHandle, "aTextureCoord")
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
            checkLocation(mvpMatrixHandle, "uMVPMatrix")
            stMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
            checkLocation(stMatrixHandle, "uSTMatrix")
            textureHandle = GLES20.glGetUniformLocation(program, "sTexture")
            checkLocation(textureHandle, "sTexture")

            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            checkGlError("glBindTexture")
            GLES20.glTexParameterf(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR.toFloat(),
            )
            GLES20.glTexParameterf(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat(),
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            checkGlError("glTexParameter")
        }

        fun drawFrame(surfaceTexture: SurfaceTexture) {
            surfaceTexture.getTransformMatrix(stMatrix)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            checkGlError("glUseProgram")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glUniform1i(textureHandle, 0)
            triangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
            GLES20.glVertexAttribPointer(
                positionHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                triangleVertices,
            )
            GLES20.glEnableVertexAttribArray(positionHandle)
            triangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
            GLES20.glVertexAttribPointer(
                textureCoordHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                triangleVertices,
            )
            GLES20.glEnableVertexAttribArray(textureCoordHandle)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(stMatrixHandle, 1, false, stMatrix, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            checkGlError("glDrawArrays")
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        }

        fun checkGlError(op: String) {
            val error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) error("$op: GL error 0x${Integer.toHexString(error)}")
        }

        private fun loadShader(shaderType: Int, source: String): Int {
            val shader = GLES20.glCreateShader(shaderType)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val info = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("Could not compile shader $shaderType: $info")
            }
            return shader
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, pixelShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                val info = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                error("Could not link program: $info")
            }
            return program
        }

        private fun checkLocation(location: Int, label: String) {
            if (location < 0) error("Unable to locate $label in GL program")
        }

        companion object {
            private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * 4
            private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
            private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3
            private val TRIANGLE_VERTICES_DATA = floatArrayOf(
                -1.0f, -1.0f, 0f, 0f, 0f,
                1.0f, -1.0f, 0f, 1f, 0f,
                -1.0f, 1.0f, 0f, 0f, 1f,
                1.0f, 1.0f, 0f, 1f, 1f,
            )
            private const val VERTEX_SHADER = """
                uniform mat4 uMVPMatrix;
                uniform mat4 uSTMatrix;
                attribute vec4 aPosition;
                attribute vec4 aTextureCoord;
                varying vec2 vTextureCoord;
                void main() {
                    gl_Position = uMVPMatrix * aPosition;
                    vTextureCoord = (uSTMatrix * aTextureCoord).xy;
                }
            """
            private const val FRAGMENT_SHADER = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                varying vec2 vTextureCoord;
                uniform samplerExternalOES sTexture;
                void main() {
                    gl_FragColor = texture2D(sTexture, vTextureCoord);
                }
            """
        }
    }

    private const val TAG = "VideoCompressor"
}
