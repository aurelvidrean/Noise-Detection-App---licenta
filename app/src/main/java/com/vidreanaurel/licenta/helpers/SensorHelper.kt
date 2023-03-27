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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.vidreanaurel.licenta.LoginActivity
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.log10

class SensorHelper {

    private var dbLevel: Double = 0.0

    fun instanceLocationRequest(locationRequest: LocationRequest) {
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000L
        locationRequest.fastestInterval = 2000L
    }

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
                                val userConnected = FirebaseAuth.getInstance().currentUser?.uid.toString()
                                val database = FirebaseDatabase.getInstance("https://licenta-e0111-default-rtdb.europe-west1.firebasedatabase" +
                                        ".app/").getReference(userConnected).child("Location")
                                database.child("latitude").setValue(latitude)
                                database.child("longitude").setValue(longitude)
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
        mMap.addMarker(MarkerOptions().position(LatLng(a, b)).title("Marker").icon(getMarkerColorByDBLevel()))
       // getData(mMap)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(a, b)))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
    }
//    private fun getData(map: GoogleMap) {
//        val databaseReference = FirebaseDatabase.getInstance("https://licenta-e0111-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Location")
//        databaseReference.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                var lat = snapshot.child("latitude").value.toString().substringAfter("=")
//                var long = snapshot.child("longitude").value.toString().substringAfter("=")
//                var stringLat = lat.split(", ")
//                var stringLong = long.split(", ")
//                var latitude = stringLat[stringLat.size - 1].substring(0, lat.length - 1).toDouble()
//                var longitude = stringLong[stringLong.size - 1].substring(0, long.length - 1).toDouble()
//                map.addMarker(MarkerOptions().position(LatLng(latitude, longitude)).title("Marker").icon(getMarkerColorByDBLevel()))
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//
//        })
//    }

    fun getMarkerColorByDBLevel(): BitmapDescriptor {
        return when {
            dbLevel < 120 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            (dbLevel <= 130) -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
            (dbLevel <= 140) -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            (dbLevel <= 150) -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        }
    }

}