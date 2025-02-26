package com.example.yoloimagelabeler.Screens.ImageLabeler.Components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.yoloimagelabeler.util.generateFilenameFromImageId
import com.example.yoloimagelabeler.util.getRectanglesFromFile
import kotlin.math.min

@Composable
fun ChangeImageButton(
    imageVector: ImageVector,
    changeImageIndex: () -> Unit
) {
    IconButton(
        onClick={ changeImageIndex() },
        colors=IconButtonDefaults.iconButtonColors(containerColor=Color.Black)
    ) {
        Icon(imageVector=imageVector, "")
    }
}