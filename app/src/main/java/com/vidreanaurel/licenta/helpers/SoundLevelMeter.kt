import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.helpers.SensorHelper
import com.vidreanaurel.licenta.models.CircularBuffer
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class SoundLevelMeter(private val listener: Listener) {

    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private var isRunning = false
    private lateinit var audioRecord: AudioRecord
    private val measurementsBuffer = CircularBuffer<Double>(24) // Circular buffer for storing the measurements

    private var timer: Timer? = null // Timer for calculating Lday every hour

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
        startTimerTask()
        Thread {
            while (isRunning) {
                val buffer = ShortArray(BUFFER_SIZE)
                val read = audioRecord.read(buffer, 0, BUFFER_SIZE)
                if (read > 0) {
                    val rms = calculateRms(buffer)
                    val spl = calculateSPL(rms)
                    listener.onSPLMeasured(spl)
                    addMeasurement(spl)
                }
            }
            audioRecord.stop()
            audioRecord.release()
        }.start()
    }

    fun stop() {
        isRunning = false
        stopTimerTask()
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
        val refPressure = 20 * 10.0.pow(-5) // reference sound pressure in Pascals
        return 20 * log10(rms / refPressure)
    }

    private fun addMeasurement(measurement: Double) {
        // Add the SPL measurement to the circular buffer
        synchronized(measurementsBuffer) {
            measurementsBuffer.add(measurement)
        }
    }

    private fun startTimerTask() {
        if (timer == null) {
            timer = Timer()
            val timerTask = object : TimerTask() {
                override fun run() {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val measurements = measurementsBuffer.toList() // Get the measurements from the buffer
                    if (currentHour in L_DAY_START_HOUR until L_DAY_END_HOUR) {
                        val lday = calculateLday(measurements) // Calculate Lday
                        listener.onLdayCalculated(lday)
                        measurementsBuffer.clear()
                    }
                    if (currentHour in L_EVENING_START_HOUR until L_EVENING_END_HOUR) {
                        val levening = calculateLevening(measurements)
                        listener.onLeveningCalculated(levening)
                        measurementsBuffer.clear()
                    }
                    if (currentHour in L_NIGHT_START_HOUR..23 && currentHour in 0..L_NIGHT_END_HOUR) {
                        val lnight = calculateLnight(measurements)
                        listener.onLnightCalculated(lnight)
                        measurementsBuffer.clear()
                    }

                }
            }
            timer?.scheduleAtFixedRate(timerTask, 0, 1000 * 60) // Run every minute
        }
    }

    private fun stopTimerTask() {
        timer?.cancel() // Stop the timer
        timer?.purge()
    }

    private fun calculateLday(measurements: List<Double>): Double {
        val weightedMeasurements = measurements.map { calculateWeightedMeasurement(it) }
        val sumWeightedMeasurements = weightedMeasurements.sum()

        val lDay = 10 * log10(sumWeightedMeasurements)

        if (!lDay.isNaN() && lDay != Double.NEGATIVE_INFINITY && lDay != Double.POSITIVE_INFINITY) {
            val userConnected = FirebaseAuth.getInstance().currentUser?.uid
            val database = userConnected?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it) }
            database?.child("L_Day")?.setValue(lDay)
        }
        return lDay
    }

    private fun calculateLevening(measurements: List<Double>): Double {
        val weightedMeasurements = measurements.map { calculateWeightedMeasurement(it) }
        val sumWeightedMeasurements = weightedMeasurements.sum()

        val lEvening = 10 * log10(sumWeightedMeasurements)

        if (!lEvening.isNaN() && lEvening != Double.NEGATIVE_INFINITY && lEvening != Double.POSITIVE_INFINITY) {
            val userConnected = FirebaseAuth.getInstance().currentUser?.uid
            val database = userConnected?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it) }
            database?.child("L_Evening")?.setValue(lEvening)
        }
        return lEvening - 5.0
    }

    private fun calculateLnight(measurements: List<Double>): Double {
        val weightedMeasurements = measurements.map { calculateWeightedMeasurement(it) }
        val sumWeightedMeasurements = weightedMeasurements.sum()

        val lNight = 10 * log10(sumWeightedMeasurements)

        if (!lNight.isNaN() && lNight != Double.NEGATIVE_INFINITY && lNight != Double.POSITIVE_INFINITY) {
            val userConnected = FirebaseAuth.getInstance().currentUser?.uid
            val database = userConnected?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it) }
            database?.child("L_Night")?.setValue(lNight)
        }
        return lNight - 10.0
    }
    private fun calculateWeightedMeasurement(spl: Double): Double {
        val correctionFactor = getCorrectionFactor(spl) // Get the A-weighting correction factor for the given SPL
        return spl + correctionFactor
    }

    private fun getCorrectionFactor(spl: Double): Double {
        // Define the frequency correction factors for A-weighting at different SPL ranges
        val correctionFactors = mapOf(
            40.0 to -27.0,
            50.0 to -16.0,
            60.0 to -8.0,
            70.0 to -3.0,
            80.0 to 0.0,
            90.0 to 1.2,
            100.0 to 1.0,
            110.0 to 0.8,
            120.0 to 0.7,
            130.0 to 0.7,
            140.0 to 0.7
        )

        // Find the closest lower SPL range in the correction factors map
        val closestLowerRange = correctionFactors.keys.lastOrNull { it <= spl } ?: correctionFactors.keys.first()

        // Retrieve the correction factor for the closest lower range
        return correctionFactors[closestLowerRange] ?: 0.0
    }

    fun checkArea(map: GoogleMap, context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = userId?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it) }
        dbRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                        val latitude = snapshot.child("LatLng").child("Latitude").getValue(Double::class.java)
                        val longitude = snapshot.child("LatLng").child("Longitude").getValue(Double::class.java)
                        val spl = snapshot.child("soundLevel").getValue(Double::class.java)
                        if (latitude != null && longitude != null && spl != null) {
                            val latLng = LatLng(latitude, longitude)

                            val weightedMeasurement = calculateWeightedMeasurement(spl)
                            val noiseFactor = 10 * log10(weightedMeasurement)
                            when (noiseFactor) {
                                in 0.0..30.0 -> drawCircle(latLng, map, ContextCompat.getColor(context, R.color.quiet_zone_color))
                                in 30.0..50.0 -> drawCircle(latLng, map, ContextCompat.getColor(context, R.color.traffic_zone_color))
                                in 50.0..200.0 -> drawCircle(latLng, map, ContextCompat.getColor(context, R.color.danger_zone_color))
                                else -> drawCircle(latLng, map, Color.TRANSPARENT).remove()
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }


    private fun drawCircle(point: LatLng, map: GoogleMap, color: Int): Circle {
        // Instantiating CircleOptions to draw a circle around the marker
        val circleOptions = CircleOptions()
        circleOptions.center(point)
        circleOptions.radius(10.0)
        circleOptions.strokeColor(Color.BLACK)
        circleOptions.fillColor(color)
        circleOptions.strokeWidth(2f)
        return map.addCircle(circleOptions)
    }


    interface Listener {
        fun onSPLMeasured(spl: Double)
        fun onLdayCalculated(lday: Double)
        fun onLeveningCalculated(levening: Double)
        fun onLnightCalculated(lnight: Double)
    }

    companion object {
        const val L_DAY_START_HOUR = 6
        const val L_DAY_END_HOUR = 18
        const val L_EVENING_START_HOUR = 18
        const val L_EVENING_END_HOUR = 22
        const val L_NIGHT_START_HOUR = 22
        const val L_NIGHT_END_HOUR = 6
    }
}
