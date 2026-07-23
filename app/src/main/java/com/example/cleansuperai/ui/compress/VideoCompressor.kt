package com.example.cleansuperai.ui.compress

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.view.Surface
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Compresses a video by re-encoding to H.264 at a capped resolution/bitrate.
 * Audio is copied when available. Falls back to a lower-bitrate re-encode path
 * using a Surface decoder/encoder pipeline.
 */
object VideoCompressor {
    private const val TIMEOUT_US = 10_000L
    private const val OUTPUT_MIME = "video/avc"

    suspend fun compress(
        context: Context,
        inputUri: Uri,
        maxHeight: Int = 720,
        bitrate: Int = 2_000_000,
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

        val videoTrack = findTrack(extractor, "video/")
            ?: error("No video track")
        val audioTrack = findTrack(extractor, "audio/")

        extractor.selectTrack(videoTrack)
        val inputVideoFormat = extractor.getTrackFormat(videoTrack)
        val durationUs = inputVideoFormat.getLongOrDefault(MediaFormat.KEY_DURATION, estimateDurationUs(context, inputUri))
        val rotation = inputVideoFormat.getIntegerOrDefault(MediaFormat.KEY_ROTATION, 0)
        val srcWidth = inputVideoFormat.getInteger(MediaFormat.KEY_WIDTH)
        val srcHeight = inputVideoFormat.getInteger(MediaFormat.KEY_HEIGHT)
        val (outWidth, outHeight) = scaledSize(srcWidth, srcHeight, maxHeight, rotation)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        if (rotation != 0) {
            muxer.setOrientationHint(rotation)
        }

        var muxerStarted = false
        var videoMuxIndex = -1
        var audioMuxIndex = -1

        val encoderFormat = MediaFormat.createVideoFormat(OUTPUT_MIME, outWidth, outHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, inputVideoFormat.frameRateOrDefault(30))
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(OUTPUT_MIME)
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface: Surface = encoder.createInputSurface()
        encoder.start()

        val decoder = MediaCodec.createDecoderByType(inputVideoFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(inputVideoFormat, inputSurface, null, 0)
        decoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var videoDone = false
        var inputDone = false
        var lastProgress = 0f

        try {
            while (!videoDone) {
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(
                                inIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                var decoderOutputAvailable = true
                while (decoderOutputAvailable) {
                    val outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> decoderOutputAvailable = false
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                        outIndex >= 0 -> {
                            val doRender = bufferInfo.size != 0
                            decoder.releaseOutputBuffer(outIndex, doRender)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                encoder.signalEndOfInputStream()
                                decoderOutputAvailable = false
                            }
                        }
                    }
                }

                var encoderOutputAvailable = true
                while (encoderOutputAvailable) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> encoderOutputAvailable = false
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) error("Format changed twice")
                            videoMuxIndex = muxer.addTrack(encoder.outputFormat)
                            if (audioTrack != null) {
                                extractor.unselectTrack(videoTrack)
                                extractor.selectTrack(audioTrack)
                                audioMuxIndex = muxer.addTrack(extractor.getTrackFormat(audioTrack))
                                extractor.unselectTrack(audioTrack)
                                extractor.selectTrack(videoTrack)
                            }
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val encoded = encoder.getOutputBuffer(outIndex)
                                ?: error("Null encoder buffer")
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size != 0) {
                                if (!muxerStarted) error("Muxer not started")
                                encoded.position(bufferInfo.offset)
                                encoded.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(videoMuxIndex, encoded, bufferInfo)
                                if (durationUs > 0) {
                                    val progress = (bufferInfo.presentationTimeUs.toFloat() / durationUs)
                                        .coerceIn(0f, 0.95f)
                                    if (progress - lastProgress >= 0.02f) {
                                        lastProgress = progress
                                        onProgress(progress)
                                    }
                                }
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                videoDone = true
                                encoderOutputAvailable = false
                            }
                        }
                    }
                }
                yield()
            }

            if (audioTrack != null && muxerStarted && audioMuxIndex >= 0) {
                copyAudioTrack(extractor, audioTrack, muxer, audioMuxIndex)
            }
            onProgress(1f)
        } finally {
            runCatching { decoder.stop() }
            runCatching { decoder.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            inputSurface.release()
            extractor.release()
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private fun copyAudioTrack(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        muxIndex: Int,
    ) {
        extractor.unselectTrack(findTrack(extractor, "video/") ?: return)
        extractor.selectTrack(trackIndex)
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val buffer = ByteBuffer.allocate(256 * 1024)
        val info = MediaCodec.BufferInfo()
        while (true) {
            val size = extractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = extractor.sampleTime
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(muxIndex, buffer, info)
            extractor.advance()
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith(prefix)) return i
        }
        return null
    }

    private fun scaledSize(
        width: Int,
        height: Int,
        maxHeight: Int,
        rotation: Int,
    ): Pair<Int, Int> {
        val swapped = rotation == 90 || rotation == 270
        val w = if (swapped) height else width
        val h = if (swapped) width else height
        if (h <= maxHeight) {
            return alignEven(w) to alignEven(h)
        }
        val scale = maxHeight.toFloat() / h.toFloat()
        return alignEven((w * scale).toInt()) to alignEven(maxHeight)
    }

    private fun alignEven(value: Int): Int = (value / 2) * 2

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
        if (containsKey(key)) getInteger(key) else default

    private fun MediaFormat.getLongOrDefault(key: String, default: Long): Long =
        if (containsKey(key)) getLong(key) else default

    private fun MediaFormat.frameRateOrDefault(default: Int): Int {
        if (!containsKey(MediaFormat.KEY_FRAME_RATE)) return default
        return runCatching { getInteger(MediaFormat.KEY_FRAME_RATE) }.getOrElse {
            runCatching { getFloat(MediaFormat.KEY_FRAME_RATE).toInt() }.getOrDefault(default)
        }.coerceAtLeast(1)
    }
}
