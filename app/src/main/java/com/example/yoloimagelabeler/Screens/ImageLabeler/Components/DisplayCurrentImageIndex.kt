package com.example.yoloimagelabeler.Screens.ImageLabeler.Components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.yoloimagelabeler.data.LocalFolderConfig

@Composable
fun DisplayCurrentImageIndex(currentImageIdx: Int) {
    val folderConfig = LocalFolderConfig.current

    Text(
        text="${currentImageIdx + 1} / ${folderConfig.imageData.size}",
        color= Color.Black)
}