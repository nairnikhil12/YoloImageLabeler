package com.example.yoloimagelabeler.Screens.ImageLabeler

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.yoloimagelabeler.Screens.ImageLabeler.Components.ChangeImageButton
import com.example.yoloimagelabeler.Screens.ImageLabeler.Components.DisplayCurrentImageIndex
import com.example.yoloimagelabeler.Screens.ImageLabeler.Components.DisplayImage
import com.example.yoloimagelabeler.Screens.ImageLabeler.Components.LabelDropDown
import com.example.yoloimagelabeler.data.LocalFolderConfig
import com.example.yoloimagelabeler.util.generateFilenameFromImageId
import com.example.yoloimagelabeler.util.getRectanglesFromFile
import com.example.yoloimagelabeler.util.saveRectanglesToFile

@Composable
fun ImageLabelerScreen() {
    val folderConfig = LocalFolderConfig.current
    val context = LocalContext.current

    var labelId by remember { mutableIntStateOf(0) }
    var currentImageIdx by remember { mutableIntStateOf(0) }
    val rectangles = remember { mutableStateListOf(
        *getRectanglesFromFile(
            context,
            generateFilenameFromImageId(currentImageIdx, ".txt", folderConfig.imageData),
            folderConfig.folderUri!!,
        ).toTypedArray()
    ) }

    val loadRectanglesFromLabelFile = { imageIndex: Int ->
        rectangles.clear()
        rectangles.addAll(
            getRectanglesFromFile(
                context,
                generateFilenameFromImageId(imageIndex, ".txt", folderConfig.imageData),
                folderConfig.folderUri!!
            )
        )
    }

    // Top bar
    Column(modifier= Modifier
        .fillMaxWidth()
        .background(color = Color.Black)) {
        Row (
            modifier= Modifier
                .horizontalScroll(rememberScrollState())
                .background(color = Color.White)
                .height(60.dp)
                .fillMaxWidth(),
            horizontalArrangement= Arrangement.SpaceEvenly,
            verticalAlignment= Alignment.CenterVertically
        ) {
            // Allows the user to select the label
            LabelDropDown(labelId) { newLabel -> labelId = newLabel }

            // Displays the current image index and total number of images
            DisplayCurrentImageIndex(currentImageIdx)

            // Go to previous image
            ChangeImageButton(Icons.AutoMirrored.Filled.ArrowForward) {
                val numOfImages = folderConfig.imageData.size

                // If index goes below 0, go to the last image
                currentImageIdx = ((currentImageIdx - 1) + numOfImages) % numOfImages
                loadRectanglesFromLabelFile(currentImageIdx)
            }

            // Go to next image
            ChangeImageButton(Icons.AutoMirrored.Filled.ArrowForward) {
                val numOfImages = folderConfig.imageData.size

                // If index beyond the last image, loop back to the first image
                currentImageIdx = (currentImageIdx + 1) % numOfImages
                loadRectanglesFromLabelFile(currentImageIdx)
            }

            // Save the image
            Button(onClick={
                saveRectanglesToFile(
                    context,
                    generateFilenameFromImageId(currentImageIdx, ".txt", folderConfig.imageData),
                    rectangles,
                    folderConfig.folderUri!!
                )

                Toast.makeText(context, "Label Saved", Toast.LENGTH_SHORT).show()
            }, colors=ButtonDefaults.buttonColors(Color.Green)) {
                Text("Save", color= Color.White)
            }

            // Reset all changes for the current image
            Button(onClick={
                rectangles.clear()
            }, colors= ButtonDefaults.buttonColors(Color.Red)) {
                Text("Reset", color= Color.White)
            }
        }

        // Display the image along with the bounding boxes
        DisplayImage(documentId = folderConfig.imageData[currentImageIdx].documentId, rectangles) { rect ->
            rectangles.add(Pair(labelId, rect))
        }
    }
}