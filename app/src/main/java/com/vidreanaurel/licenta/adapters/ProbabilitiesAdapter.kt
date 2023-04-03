package com.vidreanaurel.licenta.adapters

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.databinding.ItemProbabilityBinding
import com.vidreanaurel.licenta.helpers.SensorHelper
import org.tensorflow.lite.support.label.Category

class ProbabilitiesAdapter : RecyclerView.Adapter<ProbabilitiesAdapter.ViewHolder>() {
    var categoryList: List<Category> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProbabilityBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categoryList[position]
        if (category.label.equals(CAR) || category.label.equals(MOTOR_VEHICLE) || category.label.equals(CAR_PASSING_BY) || category.label.equals
                (CAR_HORN) || category.label.equals(VEHICLE) || category.label.equals(ACCELERATING)) {
            holder.bind(CAR, category.score, category.index)
            if (FirebaseAuth.getInstance().currentUser != null) {
                val userConnected = FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@")
                val database = userConnected?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it)}
                database?.child("CarDetection")?.setValue(category.index)
            }
        }
        if (category.label.equals(SPEECH)) {
            holder.bind(SPEECH, category.score, category.index)
        }
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    class ViewHolder(private val binding: ItemProbabilityBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var primaryProgressColorList: IntArray
        private var backgroundProgressColorList: IntArray

        init {
            primaryProgressColorList =
                binding.root.resources.getIntArray((R.array.colors_progress_primary))
            backgroundProgressColorList =
                binding.root.resources.getIntArray((R.array.colors_progress_background))
        }

        fun bind(label: String, score: Float, index: Int) {
            with(binding) {
                labelTextView.text = label
                labelTextView.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE

                progressBar.progressBackgroundTintList =
                    ColorStateList.valueOf(
                        backgroundProgressColorList[index % backgroundProgressColorList.size]
                    )

                progressBar.progressTintList =
                    ColorStateList.valueOf(
                        primaryProgressColorList[index % primaryProgressColorList.size]
                    )

                val newValue = (score * 100).toInt()
                progressBar.progress = newValue
            }
        }
    }

    companion object {
        private const val CAR = "Car"
        private const val MOTOR_VEHICLE = "Motor vehicle (road)"
        private const val CAR_PASSING_BY = "Car passing by"
        private const val CAR_HORN = "Vehicle horn, car horn, honking"
        private const val VEHICLE = "Vehicle"
        private const val ACCELERATING = "Accelerating, revving, vroom"
        private const val SPEECH = "Speech"
    }
}