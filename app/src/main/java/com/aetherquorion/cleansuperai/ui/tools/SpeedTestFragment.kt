package com.aetherquorion.cleansuperai.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aetherquorion.cleansuperai.R
import com.aetherquorion.cleansuperai.databinding.FragmentSpeedTestBinding
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeedTestFragment : Fragment() {
    private var _binding: FragmentSpeedTestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSpeedTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnStart.setOnClickListener { startTest() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startTest() {
        binding.btnStart.isEnabled = false
        binding.progressTest.isVisible = true
        binding.tvStatus.setText(R.string.speed_test_running)
        binding.tvSpeedValue.text = getString(R.string.speed_test_idle_value)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { runSpeedTest() }
            }.onSuccess { result ->
                binding.tvSpeedValue.text = String.format(Locale.US, "%.1f", result.downloadMbps)
                binding.tvDownloadResult.text = getString(
                    R.string.speed_test_download_result,
                    result.downloadMbps,
                )
                binding.tvUploadResult.text = getString(
                    R.string.speed_test_upload_result,
                    result.uploadMbps,
                )
                binding.tvLatencyResult.text = getString(
                    R.string.speed_test_latency_result,
                    result.latencyMs,
                )
                binding.tvStatus.setText(R.string.speed_test_done)
            }.onFailure {
                binding.tvStatus.setText(R.string.speed_test_failed)
                Toast.makeText(requireContext(), R.string.speed_test_failed, Toast.LENGTH_SHORT).show()
            }
            binding.btnStart.isEnabled = true
            binding.progressTest.isVisible = false
        }
    }

    private fun runSpeedTest(): SpeedTestResult {
        val latencyMs = measureLatencyMs()
        val downloadMbps = measureDownloadMbps()
        val uploadMbps = measureUploadMbps()
        return SpeedTestResult(downloadMbps, uploadMbps, latencyMs)
    }

    private fun measureLatencyMs(): Int {
        val started = System.nanoTime()
        val connection = (URL(PING_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        connection.inputStream.use { it.read() }
        connection.disconnect()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000.0
        return elapsedMs.roundToInt().coerceAtLeast(1)
    }

    private fun measureDownloadMbps(): Double {
        val bytesToRead = DOWNLOAD_BYTES
        val connection = (URL(DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 30_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        val started = System.nanoTime()
        connection.inputStream.use { input ->
            while (total < bytesToRead) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
            }
        }
        connection.disconnect()
        val seconds = (System.nanoTime() - started) / 1_000_000_000.0
        if (total <= 0L || seconds <= 0.0) error("Download failed")
        return (total * 8.0) / seconds / 1_000_000.0
    }

    private fun measureUploadMbps(): Double {
        val payload = ByteArray(UPLOAD_BYTES) { 0x5A }
        val connection = (URL(UPLOAD_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 30_000
            requestMethod = "POST"
            doOutput = true
            setFixedLengthStreamingMode(payload.size)
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        val started = System.nanoTime()
        connection.outputStream.use { output: OutputStream ->
            output.write(payload)
            output.flush()
        }
        connection.inputStream.use { it.readBytes() }
        connection.disconnect()
        val seconds = (System.nanoTime() - started) / 1_000_000_000.0
        if (seconds <= 0.0) error("Upload failed")
        return (payload.size * 8.0) / seconds / 1_000_000.0
    }

    private data class SpeedTestResult(
        val downloadMbps: Double,
        val uploadMbps: Double,
        val latencyMs: Int,
    )

    companion object {
        private const val PING_URL = "https://www.cloudflare.com/cdn-cgi/trace"
        private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=8000000"
        private const val UPLOAD_URL = "https://speed.cloudflare.com/__up"
        private const val DOWNLOAD_BYTES = 8_000_000L
        private const val UPLOAD_BYTES = 2_000_000
    }
}
