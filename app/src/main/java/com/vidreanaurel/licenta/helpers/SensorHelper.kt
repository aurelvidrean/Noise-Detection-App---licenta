package com.vidreanaurel.licenta.helpers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.vidreanaurel.licenta.LoginActivity
import java.io.IOException
import kotlin.math.log10

class SensorHelper {

    fun getCurrentLocation(activity: Activity, locationRequest: LocationRequest, map: GoogleMap) {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            if (isGPSEnabled(activity)) {
                LocationServices.getFusedLocationProviderClient(activity)
                    .requestLocationUpdates(locationRequest, object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            super.onLocationResult(locationResult)

                            LocationServices.getFusedLocationProviderClient(activity).removeLocationUpdates(this)

                            if (locationResult.locations.size > 0) {
                                val index = locationResult.locations.size - 1
                                val latitude = locationResult.locations[index].latitude
                                val longitude = locationResult.locations[index].longitude

                                val latLng = LatLng(latitude, longitude)
                                Log.d("position2", "${latLng.latitude} ${latLng.longitude}")
                                setMarkerOnMap(map, latitude, longitude)
                            }
                        }

                    }, Looper.getMainLooper())
            } else {
                turnOnGPS(activity, locationRequest)
            }
        } else {
            requestPermissions(activity, arrayOf( android.Manifest.permission.ACCESS_FINE_LOCATION), 1);
        }
    }

    fun isGPSEnabled(activity: Activity): Boolean {
        val locationManager: LocationManager? = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var isEnabled = false

        if (locationManager != null) {
            isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
        return isEnabled
    }

    fun turnOnGPS(activity: Activity, locationRequest: LocationRequest) {
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build())

        result.addOnCompleteListener(object : OnCompleteListener<LocationSettingsResponse> {
            override fun onComplete(task: Task<LocationSettingsResponse>) {

                try {
                    val response: LocationSettingsResponse = task.getResult(ApiException::class.java)
                    Toast.makeText(activity, "GPS is already turned on", Toast.LENGTH_SHORT).show()
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                val resolvableApiException: ResolvableApiException = e as ResolvableApiException
                                resolvableApiException.startResolutionForResult(activity, 2)
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

    fun setMarkerOnMap(mMap: GoogleMap, a: Double, b: Double) {
        mMap.addMarker(MarkerOptions().position(LatLng(a, b)).title("Marker"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(a, b)))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
    }

    fun soundDb(mediaRecorder: MediaRecorder?): Double {
        return 20 * log10(getAmplitude(mediaRecorder) / 32767) * (-1)
    }

    private fun getAmplitude(mediaRecorder: MediaRecorder?): Double {
        return mediaRecorder?.maxAmplitude?.toDouble() ?: 0.toDouble()
    }

}