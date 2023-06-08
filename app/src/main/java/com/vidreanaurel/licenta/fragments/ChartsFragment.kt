package com.vidreanaurel.licenta.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.adapters.InfoChartAdapter
import com.vidreanaurel.licenta.viewmodels.InfoChartViewModel
import com.vidreanaurel.licenta.databinding.FragmentChartsBinding

class ChartsFragment : Fragment() {

    private val infoChartViewModel by lazy {
        ViewModelProvider(this)[InfoChartViewModel::class.java]
    }
    private lateinit var binding: FragmentChartsBinding
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChartsBinding.inflate(inflater, container, false)
        recyclerView = binding.recyclerView
        recyclerView.visibility = View.VISIBLE

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        infoChartViewModel.getPieChartEntries(view.context)

        val chartInfoAdapter = InfoChartAdapter()

        recyclerView.adapter = chartInfoAdapter
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        infoChartViewModel.pieChartEntries.observe(viewLifecycleOwner) { infoChart ->
            if (infoChart != null) {
                chartInfoAdapter.infoChartDetails = infoChart
                chartInfoAdapter.notifyDataSetChanged()
            } else {
                chartInfoAdapter.infoChartDetails = emptyList()
            }

        }
    }

    companion object {
        fun newInstance(): ChartsFragment {
            return ChartsFragment()
        }
    }
}