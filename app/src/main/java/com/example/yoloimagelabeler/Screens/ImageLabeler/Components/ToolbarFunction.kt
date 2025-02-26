package com.example.yoloimagelabeler.Screens.ImageLabeler.Components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ToolbarFunction(
    imageVector: ImageVector,
    performAction: () -> Unit
) {
    IconButton(
        onClick={ performAction() },
        colors=IconButtonDefaults.iconButtonColors(containerColor=Color.Black)
    ) {
        Icon(imageVector=imageVector, "")
    }
}