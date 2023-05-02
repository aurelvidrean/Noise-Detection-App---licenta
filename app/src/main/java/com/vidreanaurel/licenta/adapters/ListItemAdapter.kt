package com.vidreanaurel.licenta.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.models.UserDetails

class ListItemAdapter : RecyclerView.Adapter<ListItemViewHolder>() {
    var userDetailsList: List<UserDetails> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        return ListItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_view_item, parent, false))
    }

    override fun getItemCount(): Int {
        return userDetailsList.size
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        val userDetails: UserDetails = userDetailsList.elementAt(position)
        holder.bind(
            userDetails.email!!, userDetails.location!!, userDetails.soundLevel!!, userDetails.personDetected!!,
            userDetails.carDetected!!)
    }
}

class ListItemViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val emailTextView: TextView = itemView.findViewById(R.id.user_email_text_view)
    private val userLocation: TextView = itemView.findViewById(R.id.user_location)
    private val dbTextView: TextView = itemView.findViewById(R.id.db_text_view)
    private val personDetectedTextView: TextView = itemView.findViewById(R.id.person_detected)
    private val carDetectedTextView: TextView = itemView.findViewById(R.id.car_detected)

    fun bind(email: String, location: String, db: String, personDetected: String, carDetected: String) {
        emailTextView.text = email
        userLocation.text = location
        dbTextView.text = db
        personDetectedTextView.text = personDetected
        carDetectedTextView.text = carDetected
    }
}