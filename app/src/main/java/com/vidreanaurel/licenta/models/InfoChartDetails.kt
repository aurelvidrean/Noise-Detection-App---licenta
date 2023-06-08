package com.vidreanaurel.licenta.models

import com.github.mikephil.charting.data.PieEntry

data class InfoChartDetails(
    val region: String?,
    val email: String,
    val pieChartEntries: List<PieEntry>
)
