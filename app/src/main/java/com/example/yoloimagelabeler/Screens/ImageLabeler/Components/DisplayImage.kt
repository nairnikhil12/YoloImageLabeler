package com.example.yoloimagelabeler.Screens.ImageLabeler.Components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.yoloimagelabeler.data.LocalFolderConfig
import com.example.yoloimagelabeler.util.denormalizeRectangle
import com.example.yoloimagelabeler.util.getBitmapFromDocumentId
import com.example.yoloimagelabeler.util.isInsideImage
import com.example.yoloimagelabeler.util.normalizeRectangle
import kotlin.math.min

const val LABEL_SIZE = 35f
const val BOX_STROKE_WIDTH = 3

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

//                        // Draw red dots at the calculated positions
//                        drawCircle(
//                            color = Color.Red,
//                            radius = 5.dp.toPx(),
//                            center = Offset(xOffset, yOffset) // Image Top-Left
//                        )
//
//                        drawCircle(
//                            color = Color.Red,
//                            radius = 5.dp.toPx(),
//                            center = Offset(
//                                xOffset + actualWidth,
//                                yOffset + actualHeight
//                            ) // Image Bottom-Right
//                        )

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
                        color = Color.Green.copy(alpha = 0.5f),
                        style=Stroke(width=BOX_STROKE_WIDTH.dp.toPx()),
                        topLeft = denormalizedRect.topLeft + imageTopLeft, // Shift the coordinates according to the actual image placement on the screen
                        size = denormalizedRect.size
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        folderConfig.indexToLabelMap[rect.first]!!,
                        (denormalizedRect.topLeft + imageTopLeft).x,
                        (denormalizedRect.topLeft + imageTopLeft).y,
                        Paint().apply{
                            color=Color.Red.toArgb()
                            textSize=LABEL_SIZE
                            typeface=Typeface.DEFAULT_BOLD
                        }
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
