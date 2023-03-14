package com.vidreanaurel.licenta.fragments

import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Marker
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.adapters.ProbabilitiesAdapter
import com.vidreanaurel.licenta.databinding.FragmentMainBinding
import com.vidreanaurel.licenta.helpers.AudioClassificationHelper
import com.vidreanaurel.licenta.helpers.AudioClassificationListener
import com.vidreanaurel.licenta.helpers.SensorHelper
import org.tensorflow.lite.support.label.Category
import java.io.IOException

class MainFragment : Fragment(), OnMapReadyCallback, OnMarkerClickListener {

    private lateinit var mapView: MapView
    private lateinit var mMap: GoogleMap
    private lateinit var locationRequest: LocationRequest

    private val adapter by lazy { ProbabilitiesAdapter() }
    private lateinit var audioHelper: AudioClassificationHelper
    private var _fragmentBinding: FragmentMainBinding? = null
    private val fragmentMainBinding get() = _fragmentBinding!!

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var decibelTextView: TextView

    private lateinit var recyclerView: RecyclerView

    var runner: Thread? = null

    val updater = Runnable { updateTv() }
    val mHandler: Handler = Handler()

    private val audioClassificationListener = object : AudioClassificationListener {
        override fun onResult(results: List<Category>, inferenceTime: Long) {
            requireActivity().runOnUiThread {
                adapter.categoryList = results
                adapter.notifyDataSetChanged()
                fragmentMainBinding.bottomSheetLayout.inferenceTimeVal.text =
                    String.format("%d ms", inferenceTime)
            }
        }

        override fun onError(error: String) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                adapter.categoryList = emptyList()
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (runner == null) {
            runner = object : Thread() {
                override fun run() {
                    while (runner != null) {
                        try {
                            sleep(1000)
                        } catch (_: InterruptedException) {
                        }
                        mHandler.post(updater)
                    }
                }
            }
            (runner as Thread).start()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _fragmentBinding = FragmentMainBinding.inflate(inflater, container, false)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000L
        locationRequest.fastestInterval = 2000L
        mapView = fragmentMainBinding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        decibelTextView = fragmentMainBinding.dbTV

        return fragmentMainBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = fragmentMainBinding.recyclerView
        recyclerView.adapter = adapter
        recyclerView.visibility = View.INVISIBLE
        audioHelper = AudioClassificationHelper(
            requireContext(),
            audioClassificationListener
        )

        fragmentMainBinding.bottomSheetLayout.modelSelector.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.yamnet -> {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentModel = AudioClassificationHelper.YAMNET_MODEL
                    audioHelper.initClassifier()
                }
                R.id.speech_command -> {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentModel = AudioClassificationHelper.SPEECH_COMMAND_MODEL
                    audioHelper.initClassifier()
                }
            }
        }

        fragmentMainBinding.bottomSheetLayout.spinnerOverlap.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    audioHelper.stopAudioClassification()
                    audioHelper.overlap = 0.25f * position
                    audioHelper.startAudioClassification()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    // no op
                }
            }

        fragmentMainBinding.bottomSheetLayout.resultsMinus.setOnClickListener {
            if (audioHelper.numOfResults > 1) {
                audioHelper.numOfResults--
                audioHelper.stopAudioClassification()
                audioHelper.initClassifier()
                fragmentMainBinding.bottomSheetLayout.resultsValue.text = audioHelper.numOfResults.toString()
            }
        }

        fragmentMainBinding.bottomSheetLayout.resultsPlus.setOnClickListener {
            if (audioHelper.numOfResults < 5) {
                audioHelper.numOfResults++
                audioHelper.stopAudioClassification()
                audioHelper.initClassifier()
                fragmentMainBinding.bottomSheetLayout.resultsValue.text = audioHelper.numOfResults.toString()
            }
        }

        fragmentMainBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (audioHelper.classificationThreshold >= 0.2) {
                audioHelper.stopAudioClassification()
                audioHelper.classificationThreshold -= 0.1f
                audioHelper.initClassifier()
                fragmentMainBinding.bottomSheetLayout.thresholdValue.text = String.format("%.2f", audioHelper.classificationThreshold)
            }
        }

        fragmentMainBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (audioHelper.classificationThreshold <= 0.8) {
                audioHelper.stopAudioClassification()
                audioHelper.classificationThreshold += 0.1f
                audioHelper.initClassifier()
                fragmentMainBinding.bottomSheetLayout.thresholdValue.text = String.format("%.2f", audioHelper.classificationThreshold)
            }
        }

        fragmentMainBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (audioHelper.numThreads > 1) {
                audioHelper.stopAudioClassification()
                audioHelper.numThreads--
                fragmentMainBinding.bottomSheetLayout.threadsValue.text = audioHelper.numThreads.toString()
                audioHelper.initClassifier()
            }
        }

        fragmentMainBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (audioHelper.numThreads < 4) {
                audioHelper.stopAudioClassification()
                audioHelper.numThreads++
                fragmentMainBinding.bottomSheetLayout.threadsValue.text = audioHelper.numThreads.toString()
                audioHelper.initClassifier()
            }
        }

        fragmentMainBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentDelegate = position
                    audioHelper.initClassifier()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        fragmentMainBinding.bottomSheetLayout.spinnerOverlap.setSelection(2, false)
        fragmentMainBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
    }

    override fun onResume() {
        super.onResume()
        startRecorder()
        if (::audioHelper.isInitialized) {
            audioHelper.startAudioClassification()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        stopRecorder()
        if (::audioHelper.isInitialized) {
            audioHelper.stopAudioClassification()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }


    override fun onDestroyView() {
        _fragmentBinding = null
        super.onDestroyView()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        SensorHelper().getCurrentLocation(requireActivity(), locationRequest, mMap)
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        recyclerView.visibility = View.VISIBLE
        decibelTextView.visibility = View.VISIBLE
        return true
    }

    fun updateTv() {
        decibelTextView.text = String.format("%.1f", SensorHelper().soundDb(mediaRecorder))
    }

    fun startRecorder() {
        if (mediaRecorder == null) {
            mediaRecorder = MediaRecorder()

            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder?.setOutputFile("${requireActivity().externalCacheDir?.absolutePath}/test.3gp")
            try {
                mediaRecorder?.prepare()
            } catch (ioe: IOException) {
                Log.e("[Monkey]", "IOException: " + Log.getStackTraceString(ioe))
            } catch (e: SecurityException) {
                Log.e("[Monkey]", "SecurityException: " + Log.getStackTraceString(e))
            }
            try {
                mediaRecorder?.start()
            } catch (e: SecurityException) {
                Log.e("[Monkey]", "SecurityException: " + Log.getStackTraceString(e))
            }
        }
    }

    private fun stopRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder!!.stop()
            mediaRecorder!!.release()
            mediaRecorder = null
        }
    }

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}