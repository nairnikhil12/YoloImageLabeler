package com.example.yoloimagelabeler.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LabelData(
    val filename: String="",
    val documentId: String=""
) : Parcelable