package com.vidreanaurel.licenta

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.vidreanaurel.licenta.helpers.SensorHelper
import com.vidreanaurel.licenta.models.UserDetails
import java.util.ArrayList

class DetailsViewModel: ViewModel() {

    val userDetailsList = MutableLiveData<MutableSet<UserDetails>>(null)

    fun getUserDetails() {
        val userList: MutableSet<UserDetails> = mutableSetOf()
        val dbRef = FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    userList.clear()
                    snapshot.children.forEach {
                        val latitude = it.child("LatLng").child("Latitude").getValue(Double::class.java)
                        val longitude = it.child("LatLng").child("Longitude").getValue(Double::class.java)
                        if (latitude != null && longitude != null) {
                            val userEmail = it.child("Email").value.toString()
                            val location = LatLng(latitude, longitude).toString()
                            val carDetection = it.child("CarDetection").child("Car").value.toString()
                            val personDetection = it.child("PersonDetection").child("Speech").value.toString()
                            val soundLevel = it.child("soundLevel").value.toString()

                            val userDetails = UserDetails(userEmail, location, soundLevel, personDetection, carDetection)
                            userList.add(userDetails)
                            userList.distinctBy { userEmail }
                        }
                    }
                userDetailsList.postValue(userList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

}