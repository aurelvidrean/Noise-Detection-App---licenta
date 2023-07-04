package com.vidreanaurel.licenta.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.vidreanaurel.licenta.R
import com.vidreanaurel.licenta.models.InfoChartDetails

class InfoChartAdapter: RecyclerView.Adapter<ListInfoChartViewHolder>() {

    var infoChartDetails: List<InfoChartDetails> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListInfoChartViewHolder {
        return ListInfoChartViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_info_chart, parent, false))
    }

    override fun getItemCount(): Int {
        return infoChartDetails.size
    }

    override fun onBindViewHolder(holder: ListInfoChartViewHolder, position: Int) {
        val infoDetails: InfoChartDetails = infoChartDetails[position]
        holder.bind(infoDetails.region, infoDetails.email, infoDetails.pieChartEntries)
    }
}

class ListInfoChartViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    private val regionTextView: TextView = itemView.findViewById(R.id.region)
    private val userEmailTextView: TextView = itemView.findViewById(R.id.user_email)
    private val pieChart: PieChart = itemView.findViewById(R.id.pie_chart)

    fun bind(region: String?, userEmail: String, pieChartEntries: List<PieEntry>) {
        regionTextView.text = region
        userEmailTextView.text = userEmail
        configPieChart(pieChartEntries)
    }

    private fun configPieChart(pieChartEntries: List<PieEntry>) {

        val pieDataSet = PieDataSet(pieChartEntries, "Categories")
        val colorList: MutableList<Int> = mutableListOf()
        for (joyfulColor in ColorTemplate.COLORFUL_COLORS) {
            colorList.add(joyfulColor)
        }
        pieDataSet.colors = colorList
        pieDataSet.valueTextColor = Color.BLACK
        pieDataSet.valueTextSize = 16f

        pieChart.data = PieData(pieDataSet)
        pieChart.animate()
        pieChart.description = Description().apply {
            text = "\n\nChart based on average noise for each moment of the day"
        }
        pieChart.data.notifyDataChanged()
        pieChart.notifyDataSetChanged()
        pieChart.invalidate()
        pieChart.postInvalidate()
    }
}