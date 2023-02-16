package com.vidreanaurel.licenta.helpers

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.vidreanaurel.licenta.LoginActivity

class SensorHelper {

    private var locationRequest: LocationRequest = LocationRequest.create()

    init {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000L
        locationRequest.fastestInterval = 2000L
    }

    fun getCurrentLocation(context: Context): LatLng {
        var latLng: LatLng = LatLng(0.0, 0.0)
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            if (isGPSEnabled()) {
                LocationServices.getFusedLocationProviderClient(Activity()).requestLocationUpdates(locationRequest, object: LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)

                        LocationServices.getFusedLocationProviderClient(Activity()).removeLocationUpdates(this)

                        if (locationResult.locations.size > 0) {
                            val index = locationResult.locations.size - 1
                            val latitude = locationResult.locations[index].latitude
                            val longitude = locationResult.locations[index].longitude

                            latLng = LatLng(latitude, longitude)
                            Log.d("position2", "${latLng.latitude} ${latLng.longitude}")
                        }
                    }

                }, Looper.getMainLooper())
            } else {
                turnOnGPS(context)
            }
        } else {
            requestPermissions(Activity(), arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1);
        }
        return latLng
    }

    fun isGPSEnabled(): Boolean {
        val locationManager: LocationManager? = Activity().getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var isEnabled = false

        if (locationManager != null) {
            isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
        return isEnabled
    }

    fun turnOnGPS(context: Context) {
        val builder: LocationSettingsRequest.Builder  = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)

        val result: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(context).checkLocationSettings(builder.build())

        result.addOnCompleteListener(object: OnCompleteListener<LocationSettingsResponse> {
            override fun onComplete(task: Task<LocationSettingsResponse>) {

                try {
                    val response: LocationSettingsResponse = task.getResult(ApiException::class.java)
                    Toast.makeText(context, "GPS is already turned on", Toast.LENGTH_SHORT).show()
                } catch (e: ApiException) {
                    when (e.statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            try {
                                val resolvableApiException: ResolvableApiException = e as ResolvableApiException
                                resolvableApiException.startResolutionForResult(Activity(), 2)
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
}