package com.example.yoloimagelabeler.Screens.ImageLabeler.Components

import android.graphics.drawable.shapes.OvalShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.yoloimagelabeler.data.LocalFolderConfig

@Composable
fun LabelDropDown(label: Int, onLabelChange: (newLabel: Int) -> Unit) {
    val folderConfig = LocalFolderConfig.current
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(16.dp)
    ) {
        folderConfig.indexToLabelMap[label]?.let {
            ClickableText(
                modifier=Modifier.border(1.dp, color= Color.Black, shape=RoundedCornerShape(25f)).padding(5.dp),
                text=AnnotatedString(it),
                onClick={ expanded = !expanded }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            folderConfig.labelToIndexMap.forEach {
                DropdownMenuItem(text = { Text(text=it.key) } , onClick = {
                    expanded=false
                    onLabelChange(it.value)
                })
            }
        }
    }
}
