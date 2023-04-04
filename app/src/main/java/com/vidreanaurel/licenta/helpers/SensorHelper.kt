package com.vidreanaurel.licenta.helpers

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions


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
                                val coordinates = ArrayList<Double>()
                                coordinates.add(latitude)
                                coordinates.add(longitude)
                                if (FirebaseAuth.getInstance().currentUser != null) {
                                    val userConnected = FirebaseAuth.getInstance().currentUser?.uid
                                    val database = userConnected?.let { FirebaseDatabase.getInstance(DB_URL).getReference("User").child(it) }
                                    database?.child("LatLng")?.setValue(coordinates)
                                }
                                setMarkerOnMap(map)
                            }
                        }

                    }, Looper.getMainLooper())
            } else {
                turnOnGPS(activity, locationRequest)
            }
        } else {
            requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1);
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

    fun setMarkerOnMap(mMap: GoogleMap) {
        //mMap.addMarker(MarkerOptions().position(LatLng(a, b)).title("Marker").icon(getMarkerColorByDBLevel()))
        getData(mMap)
    }

    private fun getData(map: GoogleMap) {
        val functions = FirebaseFunctions.getInstance()
        val getAllUsers = functions.getHttpsCallable("getAllUsers")

        val userList: MutableList<String> = mutableListOf()

        getAllUsers.call()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result?.data as List<HashMap<String, String>>?
                    if (result != null) {
                        var i = 0
                        for (user in result) {
                            val uid = user["uid"] as String?
                            val email = user["email"] as String?
                            Log.d("TAG", "User ID: $uid")
                            Log.d("TAG", "Email: $email")
                            if (user["uid"]?.equals(FirebaseAuth.getInstance().currentUser?.uid) == true) {
                                locateUser(user["uid"], map, true)
                            } else {
                                locateUser(user["uid"], map, false)
                            }

//                            val dbRef = user["uid"]?.let { FirebaseDatabase.getInstance(DB_URL).getReference("User").child(it) }
//                            dbRef?.addValueEventListener(object : ValueEventListener {
//                                override fun onDataChange(snapshot: DataSnapshot) {
//                                    if (snapshot.exists()) {
//                                        val latLng = snapshot.child("LatLng").value
//                                        Log.d("DOAMNE AJUTA", latLng.toString())
//                                        if (latLng != null) {
//                                            val latitude = (latLng as ArrayList<*>)[0]
//                                            val longitude = (latLng as ArrayList<*>)[1]
//                                            val marker = LatLng(latitude.toString().toDouble(), longitude.toString().toDouble())
//                                            map.addMarker(
//                                                MarkerOptions().position(marker).title("Marker")
//                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
//                                            )
//                                            drawCircle(marker, map)
//                                            //map.animateCamera(CameraUpdateFactory.zoomTo(12f))
//                                        }
//                                    }
//                                }
//
//                                override fun onCancelled(error: DatabaseError) {
//                                    TODO("Not yet implemented")
//                                }
//                            })
                        }
                    }
                } else {
                    Log.e("TAG", "Error getting all users: ", task.exception)
                }
            }
    }

    private fun locateUser(userId: String?, map: GoogleMap, isCurrentUser: Boolean) {
        val dbRef = userId?.let { FirebaseDatabase.getInstance(DB_URL).getReference("User").child(it) }
        dbRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val latLng = snapshot.child("LatLng").value
                    Log.d("DOAMNE AJUTA", latLng.toString())
                    if (latLng != null) {
                        val latitude = (latLng as ArrayList<*>)[0]
                        val longitude = (latLng as ArrayList<*>)[1]
                        val marker = LatLng(latitude.toString().toDouble(), longitude.toString().toDouble())
                        map.addMarker(
                            MarkerOptions().position(marker).title("Marker")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                        )
                        drawCircle(marker, map)
                        if (isCurrentUser) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker,12f));
                            map.animateCamera(CameraUpdateFactory.zoomIn());
                            map.animateCamera(CameraUpdateFactory.zoomTo(12f), 2000, null);
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun drawCircle(point: LatLng, map: GoogleMap) {
        // Instantiating CircleOptions to draw a circle around the marker
        val circleOptions = CircleOptions()
        circleOptions.center(point)
        circleOptions.radius(300.0)
        circleOptions.strokeColor(Color.BLACK)
        circleOptions.fillColor(Color.YELLOW)
        circleOptions.strokeWidth(2f)
        map.addCircle(circleOptions)
    }

    fun getMarkerColorByDBLevel(): BitmapDescriptor {
        return when {
            dbLevel < 120 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            (dbLevel <= 130) -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)
            (dbLevel <= 140) -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            (dbLevel <= 150) -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        }
    }

    companion object {
        const val DB_URL = "https://licenta-e0111-default-rtdb.europe-west1.firebasedatabase.app/"
    }

}