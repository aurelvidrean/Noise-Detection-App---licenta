package com.vidreanaurel.licenta.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.adapters.ProbabilitiesAdapter
import com.vidreanaurel.licenta.databinding.FragmentMainBinding
import com.vidreanaurel.licenta.helpers.AudioClassificationHelper
import org.tensorflow.lite.support.label.Category

interface AudioClassificationListener {
    fun onError(error: String)
    fun onResult(results: List<Category>, inferenceTime: Long)
}

class MainFragment : Fragment(), OnMapReadyCallback, OnMarkerClickListener {

    private lateinit var mapView: MapView
    private lateinit var mMap: GoogleMap
    private lateinit var locationRequest: LocationRequest

    private val adapter by lazy { ProbabilitiesAdapter() }
    private lateinit var audioHelper: AudioClassificationHelper
    private var _fragmentBinding: FragmentMainBinding? = null
    private val fragmentMainBinding get() = _fragmentBinding!!

    private lateinit var recyclerView: RecyclerView
    
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentMainBinding.inflate(inflater, container, false)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000L
        locationRequest.fastestInterval = 2000L
        mapView = fragmentMainBinding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
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

        fragmentMainBinding.bottomSheetLayout.modelSelector.setOnCheckedChangeListener(
            object : RadioGroup.OnCheckedChangeListener {
                override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
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
            })

        fragmentMainBinding.bottomSheetLayout.spinnerOverlap.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
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
                fragmentMainBinding.bottomSheetLayout.resultsValue.text =
                    audioHelper.numOfResults.toString()
            }
        }

        fragmentMainBinding.bottomSheetLayout.resultsPlus.setOnClickListener {
            if (audioHelper.numOfResults < 5) {
                audioHelper.numOfResults++
                audioHelper.stopAudioClassification()
                audioHelper.initClassifier()
                fragmentMainBinding.bottomSheetLayout.resultsValue.text =
                    audioHelper.numOfResults.toString()
            }
        }

        fragmentMainBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (audioHelper.classificationThreshold >= 0.2) {
                audioHelper.stopAudioClassification()
                audioHelper.classificationThreshold -= 0.1f
                audioHelper.initClassifier()
                fragmentMainBinding.bottomSheetLayout.thresholdValue.text =
                    String.format("%.2f", audioHelper.classificationThreshold)
            }
        }

        fragmentMainBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (audioHelper.classificationThreshold <= 0.8) {
                audioHelper.stopAudioClassification()
                audioHelper.classificationThreshold += 0.1f
                audioHelper.initClassifier()
                fragmentMainBinding.bottomSheetLayout.thresholdValue.text =
                    String.format("%.2f", audioHelper.classificationThreshold)
            }
        }

        fragmentMainBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (audioHelper.numThreads > 1) {
                audioHelper.stopAudioClassification()
                audioHelper.numThreads--
                fragmentMainBinding.bottomSheetLayout.threadsValue.text = audioHelper
                    .numThreads
                    .toString()
                audioHelper.initClassifier()
            }
        }

        fragmentMainBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (audioHelper.numThreads < 4) {
                audioHelper.stopAudioClassification()
                audioHelper.numThreads++
                fragmentMainBinding.bottomSheetLayout.threadsValue.text = audioHelper
                    .numThreads
                    .toString()
                audioHelper.initClassifier()
            }
        }

        fragmentMainBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    audioHelper.stopAudioClassification()
                    audioHelper.currentDelegate = position
                    audioHelper.initClassifier()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        fragmentMainBinding.bottomSheetLayout.spinnerOverlap.setSelection(
            2,
            false
        )
        fragmentMainBinding.bottomSheetLayout.spinnerDelegate.setSelection(
            0,
            false
        )
    }

    override fun onResume() {
        super.onResume()
       mapView.onResume()
        if (::audioHelper.isInitialized ) {
            audioHelper.startAudioClassification()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (::audioHelper.isInitialized ) {
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



    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        getCurrentLocation()
    }

    fun setMarkerOnMap(a: Double, b: Double) {
        mMap.addMarker(MarkerOptions().position(LatLng(a, b)).title("Marker in Your " + "Location"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(a, b)))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            if (isGPSEnabled()) {
                LocationServices.getFusedLocationProviderClient(requireActivity()).requestLocationUpdates(locationRequest, object: LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)

                        LocationServices.getFusedLocationProviderClient(requireActivity()).removeLocationUpdates(this)

                        if (locationResult.locations.size > 0) {
                            val index = locationResult.locations.size - 1
                            val latitude = locationResult.locations[index].latitude
                            val longitude = locationResult.locations[index].longitude

                            val latLng = LatLng(latitude, longitude)
                            Log.d("position2", "${latLng.latitude} ${latLng.longitude}")
                            setMarkerOnMap(latitude, longitude)
                        }
                    }

                }, Looper.getMainLooper())
            } else {
                turnOnGPS()
            }
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1);
        }
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager: LocationManager? = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var isEnabled = false

        if (locationManager != null) {
            isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
        return isEnabled
    }

    private fun turnOnGPS() {
        val builder: LocationSettingsRequest.Builder  = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(requireContext()).checkLocationSettings(builder.build())

        result.addOnCompleteListener(object: OnCompleteListener<LocationSettingsResponse> {
            override fun onComplete(task: Task<LocationSettingsResponse>) {

                try {
                    val response: LocationSettingsResponse = task.getResult(ApiException::class.java)
                    Toast.makeText(requireContext(), "GPS is already turned on", Toast.LENGTH_SHORT).show()
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                val resolvableApiException: ResolvableApiException = e as ResolvableApiException
                                resolvableApiException.startResolutionForResult(requireActivity(), 2)
                            } catch (ex: IntentSender.SendIntentException) {
                                ex.printStackTrace()
                            }
                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            return
                        }
                    }
                }
            }
        })
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        recyclerView.visibility = View.VISIBLE
        return true
    }


}