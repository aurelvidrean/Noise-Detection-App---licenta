package com.vidreanaurel.licenta.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.vidreanaurel.licenta.viewmodels.DetailsViewModel
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.adapters.ListItemAdapter
import com.vidreanaurel.licenta.helpers.SensorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailsFragment : Fragment(), SoundLevelMeter.Listener {

    private val viewModel by lazy {
        ViewModelProvider(this)[DetailsViewModel::class.java]
    }

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.visibility = View.VISIBLE


        viewModel.getUserDetails()

        val detailsAdapter = ListItemAdapter()

        recyclerView.adapter = detailsAdapter
        recyclerView.layoutManager = LinearLayoutManager(view.context)


        viewModel.userDetailsList.observe(viewLifecycleOwner) { userDetails ->
            if (userDetails != null) {
                detailsAdapter.userDetailsList = userDetails.toList()
                detailsAdapter.notifyDataSetChanged()
            } else {
                detailsAdapter.userDetailsList = emptyList()
            }

        }
    }



    companion object {
        fun newInstance(): DetailsFragment {
            return DetailsFragment()
        }
    }

    var x = 0.0
    override fun onSPLMeasured(spl: Double) {
//        if (isAdded) {
//            requireActivity().runOnUiThread {
//                if (spl > x) {
//                    x = spl
//                }
//            }
//        }
//        val userConnected = FirebaseAuth.getInstance().currentUser?.uid
//        val database = userConnected?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it) }
//        database?.child("soundLevel")?.setValue(String.format("%.1f dB", x))
        if (isAdded) {
            lifecycleScope.launch(Dispatchers.Main) {
                if (spl > x) {
                    x = spl
                }
                //  soundLevelMeter.checkArea(mMap, requireContext())
            }
        }
        val userConnected = FirebaseAuth.getInstance().currentUser?.uid
        val database = userConnected?.let { FirebaseDatabase.getInstance(SensorHelper.DB_URL).getReference("User").child(it) }
        database?.child("soundLevel")?.setValue(x)
    }

    override fun onLdayCalculated(lday: Double) {
    }

    override fun onLeveningCalculated(levening: Double) {
    }

    override fun onLnightCalculated(lnight: Double) {
    }
}