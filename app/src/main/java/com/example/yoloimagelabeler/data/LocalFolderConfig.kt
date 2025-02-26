package com.example.yoloimagelabeler.data

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf

data class FolderConfig(
    var imageData: ArrayList<ImageData> = arrayListOf(),
    var labelData: ArrayList<LabelData> = arrayListOf(),
    var folderUri: Uri? = null,
    val labelToIndexMap: MutableMap<String, Int> = mutableMapOf(),
    var indexToLabelMap: MutableMap<Int, String> = mutableMapOf(),
)

val LocalFolderConfig = staticCompositionLocalOf { FolderConfig() }