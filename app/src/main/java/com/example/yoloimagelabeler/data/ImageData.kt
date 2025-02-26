package com.example.yoloimagelabeler.data
import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ImageData (
    val filename: String="",
    val documentId: String=""
) : Parcelable