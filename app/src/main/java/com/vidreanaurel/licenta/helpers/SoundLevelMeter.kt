import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.TIMER_ACTION
import com.vidreanaurel.licenta.adapters.secondsToTime
import com.vidreanaurel.licenta.helpers.NotificationHelper
import com.vidreanaurel.licenta.models.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.log10
import kotlin.math.sqrt


const val SERVICE_COMMAND = "Command"
const val NOTIFICATION_TEXT = "NotificationText"
class SoundLevelMeter(private val listener: Listener): Service(), CoroutineScope {

    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private val helper by lazy { NotificationHelper(this) }
    private var currentTime: Int = 0
    private var startedAtTimestamp: Int = 0

    private var isRunning = false
    private lateinit var audioRecord: AudioRecord

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable = object : Runnable {
        override fun run() {
            currentTime++
            broadcastUpdate()
            // Repeat every 1 second
            handler.postDelayed(this, 1000)
        }
    }
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    var serviceState: TimerState = TimerState.INITIALIZED

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.extras?.run {
            when (getSerializable(SERVICE_COMMAND) as TimerState) {
                TimerState.START -> startTimer()
                TimerState.PAUSE -> pauseTimerService()
                TimerState.STOP -> endTimerService()
                else -> return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        job.cancel()
    }

    private fun startTimer() {
        serviceState = TimerState.START

        startedAtTimestamp = 0

        // publish notification
        startForeground(NotificationHelper.NOTIFICATION_ID, helper.getNotification())
        broadcastUpdate()

        startCoroutineTimer()
    }

    private fun broadcastUpdate() {
        // update notification
        if (serviceState == TimerState.START) {
            // count elapsed time
            val elapsedTime = (currentTime - startedAtTimestamp)

            // send time to update UI
            sendBroadcast(
                Intent(TIMER_ACTION)
                    .putExtra(NOTIFICATION_TEXT, elapsedTime)
            )

            helper.updateNotification(
                getString(R.string.time_is_running, elapsedTime.secondsToTime())
            )
        } else if (serviceState == TimerState.PAUSE) {
            helper.updateNotification(getString(R.string.get_back))
        }
    }

    private fun pauseTimerService() {
        serviceState = TimerState.PAUSE
        handler.removeCallbacks(runnable)
        broadcastUpdate()
    }

    private fun endTimerService() {
        serviceState = TimerState.STOP
        handler.removeCallbacks(runnable)
        broadcastUpdate()
        stopService()
    }
    private fun stopService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        } else {
            stopSelf()
        }
    }

    private fun startCoroutineTimer() {
        launch(coroutineContext) {
            handler.post(runnable)
        }
    }
}
