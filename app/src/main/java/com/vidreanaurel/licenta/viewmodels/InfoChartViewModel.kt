package com.vidreanaurel.licenta.viewmodels

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.vidreanaurel.licenta.models.InfoChartDetails
import java.util.Locale

class InfoChartViewModel : ViewModel() {

    val pieChartEntries = MutableLiveData<List<InfoChartDetails>>(null)

    fun getPieChartEntries(context: Context) {
        val infoList = mutableListOf<InfoChartDetails>()

        FirebaseDatabase.getInstance(DB_URL).getReference("User").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    infoList.clear()
                    snapshot.children.forEach {
                        val lDay = it.child(L_DAY).getValue(Double::class.java)
                        val lEvening = it.child(L_EVENING).getValue(Double::class.java)
                        val lNight = it.child(L_NIGHT).getValue(Double::class.java)
                        val userEmail = it.child("Email").value.toString()

                        val latitude = it.child("LatLng").child("Latitude").getValue(Double::class.java)
                        val longitude = it.child("LatLng").child("Longitude").getValue(Double::class.java)
                        if (latitude != null && longitude != null) {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val address = geocoder.getFromLocation(latitude, longitude, 1)?.get(0)?.getAddressLine(0)
                            val entryList = listOf<PieEntry>(
                                PieEntry(lNight?.toFloat() ?: 0f, L_NIGHT),
                                PieEntry(lEvening?.toFloat() ?: 0f, L_EVENING),
                                PieEntry(lDay?.toFloat() ?: 0f, L_DAY)
                            )
                            infoList.add(InfoChartDetails(address, userEmail, entryList))
                        }
                    }
                    pieChartEntries.postValue(infoList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    companion object {
        const val DB_URL = "https://licenta-e0111-default-rtdb.europe-west1.firebasedatabase.app/"
        const val L_DAY = "L_Day"
        const val L_EVENING = "L_Evening"
        const val L_NIGHT = "L_Night"
    }
}