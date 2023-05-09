package com.vidreanaurel.licenta.fragments

import SoundLevelMeter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.vidreanaurel.licenta.RegisterActivity
import com.vidreanaurel.licenta.adapters.ProbabilitiesAdapter
import com.vidreanaurel.licenta.databinding.FragmentMainBinding
import com.vidreanaurel.licenta.helpers.AudioClassificationHelper
import com.vidreanaurel.licenta.helpers.AudioClassificationListener
import com.vidreanaurel.licenta.helpers.SensorHelper
import org.tensorflow.lite.support.label.Category

class MainFragment : Fragment(), OnMapReadyCallback, SoundLevelMeter.Listener {

    private lateinit var mapView: MapView
    private lateinit var mMap: GoogleMap
    private lateinit var locationRequest: LocationRequest

    private val adapter by lazy { ProbabilitiesAdapter() }
    private lateinit var audioHelper: AudioClassificationHelper
    private var _fragmentBinding: FragmentMainBinding? = null
    private val fragmentMainBinding get() = _fragmentBinding!!
    private lateinit var decibelTextView: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var soundLevelMeter: SoundLevelMeter

    lateinit var logoutbutton: Button

    private val audioClassificationListener = object : AudioClassificationListener {
        override fun onResult(results: List<Category>, inferenceTime: Long) {
            if (isAdded) {
                requireActivity().runOnUiThread {
                    adapter.categoryList = results
                    adapter.notifyDataSetChanged()
                    fragmentMainBinding.bottomSheetLayout.inferenceTimeVal.text =
                        String.format("%d ms", inferenceTime)
                }
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
        soundLevelMeter = SoundLevelMeter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _fragmentBinding = FragmentMainBinding.inflate(inflater, container, false)
        locationRequest = LocationRequest.create()
        SensorHelper().instanceLocationRequest(locationRequest)
        mapView = fragmentMainBinding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        decibelTextView = fragmentMainBinding.dbTV

        logoutbutton = fragmentMainBinding.logoutbtn
        logoutbutton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), RegisterActivity::class.java))
        }
        return fragmentMainBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = fragmentMainBinding.recyclerView
        recyclerView.adapter = adapter
        audioHelper = AudioClassificationHelper(requireContext(), audioClassificationListener)

        audioHelper.stopAudioClassification()
        audioHelper.currentModel = AudioClassificationHelper.YAMNET_MODEL
        audioHelper.initClassifier()


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
        soundLevelMeter.start(requireContext())
        if (::audioHelper.isInitialized) {
            audioHelper.startAudioClassification()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        soundLevelMeter.stop()
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
        SensorHelper().getCurrentLocation(requireActivity(), locationRequest, mMap)
    }

    var x = 0.0

    override fun onSPLMeasured(spl: Double) {
        if (isAdded) {
            requireActivity().runOnUiThread {
                decibelTextView.text = String.format("%.1f dB", spl)
                if (spl > x) {
                    x = spl
                }
            }
        }
        val userConnected = FirebaseAuth.getInstance().currentUser?.uid
        val database = userConnected?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it) }
        database?.child("soundLevel")?.setValue(String.format("%.1f dB", x))
    }

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}