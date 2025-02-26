import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.yoloimagelabeler.data.LocalFolderConfig
import com.example.yoloimagelabeler.util.denormalizeRectangle
import com.example.yoloimagelabeler.util.generateFilenameFromImageId
import com.example.yoloimagelabeler.util.getBitmapFromDocumentId
import com.example.yoloimagelabeler.util.getRectanglesFromFile
import com.example.yoloimagelabeler.util.isInsideImage
import com.example.yoloimagelabeler.util.normalizeRectangle
import com.example.yoloimagelabeler.util.saveRectanglesToFile
import kotlin.math.max
import kotlin.math.min

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
            LabelDropDown(labelId) { newLabel -> labelId = newLabel }
            DisplayCurrentImageIndex(currentImageIdx)

            // Go to previous image
            IconButton(
                onClick={
                    currentImageIdx = max(0, currentImageIdx - 1)
                    rectangles.clear()
                    rectangles.addAll(
                        getRectanglesFromFile(
                            context,
                            generateFilenameFromImageId(currentImageIdx, ".txt", folderConfig.imageData),
                            folderConfig.folderUri!!
                        )
                    )
                },
                colors= IconButtonDefaults.iconButtonColors(containerColor= Color.Black)
            ) {
                Icon(imageVector= Icons.AutoMirrored.Filled.ArrowBack, "")
            }

            // Go to next image
            IconButton(
                onClick={
                    currentImageIdx = min(folderConfig.imageData.size - 1, currentImageIdx + 1)
                    rectangles.clear()
                    rectangles.addAll(
                        getRectanglesFromFile(
                            context,
                            generateFilenameFromImageId(currentImageIdx, ".txt", folderConfig.imageData),
                            folderConfig.folderUri!!
                        ))
                },
                colors= IconButtonDefaults.iconButtonColors(containerColor= Color.Black)
            ) {
                Icon(imageVector= Icons.AutoMirrored.Filled.ArrowForward, "")
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

            // Reset the changes
            Button(onClick={
                rectangles.clear()
            }, colors= ButtonDefaults.buttonColors(Color.Red)) {
                Text("Reset", color= Color.White)
            }
        }

        DisplayImage(documentId = folderConfig.imageData[currentImageIdx].documentId, rectangles) { rect ->
            rectangles.add(Pair(labelId, rect))
        }
    }
}

@Composable
fun LabelDropDown(label: Int, onLabelChange: (newLabel: Int) -> Unit) {
    val folderConfig = LocalFolderConfig.current
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(16.dp)
    ) {
        folderConfig.indexToLabelMap[label]?.let {
            ClickableText(text= AnnotatedString(it), style= TextStyle(textDecoration = TextDecoration.Underline), onClick= {
                expanded = !expanded
            })
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

@Composable
fun DisplayCurrentImageIndex(currentImageIdx: Int) {
    val folderConfig = LocalFolderConfig.current

    Text(
        text="${currentImageIdx + 1} / ${folderConfig.imageData.size}",
        color=Color.Black)
}

@Composable
fun DisplayImage(documentId: String, rectangles: SnapshotStateList<Pair<Int, Rect>>, addRectangleOnDragEnd: (Rect) -> Unit) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var containerTopLeft by remember { mutableStateOf(Offset(0f, 0f)) }
    var imageTopLeft by remember { mutableStateOf(Offset(0f, 0f)) }
    var imageBottomRight by remember { mutableStateOf(Offset(0f, 0f)) }
    var imageSize by remember { mutableStateOf(Size(0f, 0f)) }
    var currentRectangle by remember { mutableStateOf<Rect?>(null) }

    val folderConfig = LocalFolderConfig.current
    val context = LocalContext.current

    LaunchedEffect(documentId) {
        bitmap = getBitmapFromDocumentId(context, documentId, folderConfig.folderUri!!)
    }

    bitmap?.let {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Image",
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { layoutCoordinates ->
                        containerTopLeft = layoutCoordinates.positionInRoot()
                    }
                    .drawWithContent {
                        drawContent() // Draw image normally

                        // Get original image size
                        val intrinsicSize = Size(it.width.toFloat(), it.height.toFloat())

                        val scale = min(
                            size.width / intrinsicSize.width,
                            size.height / intrinsicSize.height
                        )

                        val actualWidth = intrinsicSize.width * scale
                        val actualHeight = intrinsicSize.height * scale

                        val xOffset = (size.width - actualWidth) / 2f
                        val yOffset = (size.height - actualHeight) / 2f

                        imageTopLeft = containerTopLeft + Offset(xOffset, yOffset)
                        imageBottomRight =
                            imageTopLeft + Offset(xOffset + actualWidth, yOffset + actualHeight)

                        // Draw red dots at the calculated positions
                        drawCircle(
                            color = Color.Red,
                            radius = 5.dp.toPx(),
                            center = Offset(xOffset, yOffset) // Image Top-Left
                        )

                        drawCircle(
                            color = Color.Red,
                            radius = 5.dp.toPx(),
                            center = Offset(
                                xOffset + actualWidth,
                                yOffset + actualHeight
                            ) // Image Bottom-Right
                        )

                        imageTopLeft = Offset(xOffset, yOffset)
                        imageBottomRight = Offset(xOffset + actualWidth, yOffset + actualHeight)
                        imageSize = Size(imageBottomRight.x - imageTopLeft.x, imageBottomRight.y - imageTopLeft.y)
                    }
                    .pointerInput(Unit) {
                        //detectDragGestures { change, dragAmount ->  }
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (isInsideImage(offset, imageTopLeft, imageBottomRight)) {
                                    currentRectangle = Rect(offset, offset)
                                }
                            },
                            onDrag = { _, dragAmount ->
                                currentRectangle?.let {
                                    val newBottomRight = Offset(
                                        x = (it.bottomRight.x + dragAmount.x).coerceIn(imageTopLeft.x, imageBottomRight.x),
                                        y = (it.bottomRight.y + dragAmount.y).coerceIn(imageTopLeft.y, imageBottomRight.y)
                                    )
                                    currentRectangle = Rect(it.topLeft, newBottomRight)
                                }
                            },
                            onDragEnd = {
                                currentRectangle?.let {
                                    val normalizedRect = normalizeRectangle(
                                        Rect(it.topLeft - imageTopLeft, it.size), // Shift the coordinates by subtracting the placement of the image on the screen
                                        imageSize
                                    )
                                    addRectangleOnDragEnd(normalizedRect)
                                }
                                currentRectangle = null
                            },
                        )
                    },
                contentScale = ContentScale.Fit
            )

            Canvas(modifier = Modifier.matchParentSize()) {
                rectangles.forEach { rect ->
                    val denormalizedRect = denormalizeRectangle(
                        Rect(rect.second.topLeft, rect.second.size),
                        imageSize
                    )

                    drawRect(
                        color = Color.Red.copy(alpha = 0.5f),
                        topLeft = denormalizedRect.topLeft + imageTopLeft, // Shift the coordinates according to the actual image placement on the screen
                        size = denormalizedRect.size
                    )
                }

                // Draw the currently drawn rectangle
                currentRectangle?.let { rect ->
                    drawRect(
                        color = Color.Blue.copy(alpha = 0.5f),
                        topLeft = rect.topLeft,
                        size = rect.size
                    )
                }
            }
        }
    }
}
