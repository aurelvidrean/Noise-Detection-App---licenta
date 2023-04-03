import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlin.math.log10
import kotlin.math.sqrt

class SoundLevelMeter(private val listener: Listener) {

    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private var isRunning = false
    private lateinit var audioRecord: AudioRecord

    fun start(context: Context) {
        isRunning = true
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )
        audioRecord.startRecording()
        Thread {
            while (isRunning) {
                val buffer = ShortArray(BUFFER_SIZE)
                val read = audioRecord.read(buffer, 0, BUFFER_SIZE)
                if (read > 0) {
                    val rms = calculateRms(buffer)
                    val spl = calculateSPL(rms)
                    listener.onSPLMeasured(spl)
                }
            }
            audioRecord.stop()
            audioRecord.release()
        }.start()
    }

    fun stop() {
        isRunning = false
    }

    private fun calculateRms(buffer: ShortArray): Double {
        var sum = 0.0
        for (i in buffer.indices) {
            sum += buffer[i] * buffer[i]
        }
        val mean = sum / buffer.size
        return sqrt(mean)
    }

    private fun calculateSPL(rms: Double): Double {
        val refPressure = 20e-6 // reference sound pressure in Pascals
        return 20 * log10(rms / refPressure)
    }

    interface Listener {
        fun onSPLMeasured(spl: Double)
    }
}
