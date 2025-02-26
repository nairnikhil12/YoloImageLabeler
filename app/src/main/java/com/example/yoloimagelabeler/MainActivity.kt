package com.example.yoloimagelabeler

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Scaffold
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
import com.example.yoloimagelabeler.data.ImageData
import com.example.yoloimagelabeler.data.LabelData
import com.example.yoloimagelabeler.ui.theme.YoloImageLabelerTheme
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var onSelectFolderSuccess: () -> Unit

    private var imageData: ArrayList<ImageData> = arrayListOf()
    private var labelData: ArrayList<LabelData> = arrayListOf()
    private var storeUri: Uri? = null

    private var labelToIndexMap: MutableMap<String, Int> = mutableMapOf()
    private var indexToLabelMap: MutableMap<Int, String> = mutableMapOf()
    private var labelFileDocumentId: String = ""

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YoloImageLabelerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    Screen()
                }
            }
        }

        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    listFilesInFolder(it, onSelectFolderSuccess)
                }
            } else {
                Toast.makeText(this, "Folder selection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun listFilesInFolder(uri: Uri, onSuccess: () -> Unit) {
        val documentId = DocumentsContract.getTreeDocumentId(uri)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, documentId)

        storeUri = uri

        val resolver: ContentResolver = contentResolver
        val cursor = resolver.query(childrenUri, null, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val displayName =
                    it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                val mimeType =
                    it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE))
                val docId = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))

                if (mimeType.startsWith("image/")) {
                    imageData.add(ImageData(filename=displayName, documentId=docId))
                    Log.i("MainActivity", "Image: $displayName, $mimeType")
                } else if (mimeType.startsWith("text/plain")) {
                    Log.i("MainActivity", "Label: $displayName, $mimeType")

                    if (displayName == "labels.txt") {
                        labelFileDocumentId = docId
                    } else {
                        labelData.add(LabelData(filename=displayName, documentId=docId))
                    }
                }
            }
        }

        if (labelFileDocumentId != "") {
            generateMapFromLabelFile()
        }

        onSuccess()
    }

    private fun generateMapFromLabelFile() {
        val uri: Uri = DocumentsContract.buildDocumentUriUsingTree(
            storeUri,
            labelFileDocumentId
        )

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader->
                    val labels = reader.readText().split("\r\n")

                    labels.forEachIndexed { i, it ->
                        labelToIndexMap[it] = i
                        indexToLabelMap[i] = it
                    }
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    fun Screen() {
        var isFolderSelected by remember { mutableStateOf(false) }

        Column(
            modifier=Modifier.fillMaxSize(),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center
        ) {
            if (isFolderSelected) {
                FolderSelectedScreen()
            } else {
                SelectFolderButton {
                    isFolderSelected = true
                }
            }
        }
    }

    @Composable
    fun SelectFolderButton(onSuccess: () -> Unit) {
        Button(
            onClick={
                onSelectFolderSuccess = {
                    onSuccess()
                }
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                folderPickerLauncher.launch(intent)
            }
        ) {
            Text(text="Select Image Folder")
        }
    }

    private fun generateFilenameFromImageId(imageId: Int, extension: String = ".txt"): String {
        return imageData[imageId].filename.split(".")[0] + extension
    }

    @Composable
    fun FolderSelectedScreen() {
        val context = LocalContext.current

        var labelId by remember { mutableIntStateOf(0) }
        var currentImageIdx by remember { mutableIntStateOf(0) }
        val rectangles = remember { mutableStateListOf<Pair<Int, Rect>>(
            *getRectanglesFromFile(context, generateFilenameFromImageId(currentImageIdx)).toTypedArray()
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
                horizontalArrangement=Arrangement.SpaceEvenly,
                verticalAlignment=Alignment.CenterVertically
            ) {
                LabelDropDown(labelId) { newLabel -> labelId = newLabel }
                DisplayCurrentImageIndex(currentImageIdx)

                // Go to previous image
                IconButton(
                    onClick={
                        currentImageIdx = max(0, currentImageIdx - 1)
                        rectangles.clear()
                        rectangles.addAll(getRectanglesFromFile(context, generateFilenameFromImageId(currentImageIdx)))
                    },
                    colors= IconButtonDefaults.iconButtonColors(containerColor=Color.Black)
                ) {
                    Icon(imageVector=Icons.AutoMirrored.Filled.ArrowBack, "")
                }

                // Go to next image
                IconButton(
                    onClick={
                        currentImageIdx = min(imageData.size - 1, currentImageIdx + 1)
                        rectangles.clear()
                        rectangles.addAll(getRectanglesFromFile(context, generateFilenameFromImageId(currentImageIdx)))
                    },
                    colors= IconButtonDefaults.iconButtonColors(containerColor=Color.Black)
                ) {
                    Icon(imageVector=Icons.AutoMirrored.Filled.ArrowForward, "")
                }

                // Save the image
                Button(onClick={
                    saveRectanglesToFile(
                        context,
                        imageData[currentImageIdx].filename.split(".")[0] + ".txt",
                        rectangles
                    )

                    Toast.makeText(context, "Label Saved", Toast.LENGTH_SHORT).show()
                }, colors=ButtonDefaults.buttonColors(Color.Green)) {
                    Text("Save", color=Color.White)
                }

                // Reset the changes
                Button(onClick={
                    rectangles.clear()
                }, colors=ButtonDefaults.buttonColors(Color.Red)) {
                    Text("Reset", color=Color.White)
                }
            }

            DisplayImage(documentId = imageData[currentImageIdx].documentId, rectangles) { rect ->
                rectangles.add(Pair(labelId, rect))
            }
        }
    }

    @Composable
    fun DisplayImage(documentId: String, rectangles: SnapshotStateList<Pair<Int, Rect>>, addRectangleOnDragEnd: (Rect) -> Unit) {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var containerTopLeft by remember { mutableStateOf(Offset(0f, 0f)) }
        var imageTopLeft by remember { mutableStateOf(Offset(0f, 0f))}
        var imageBottomRight by remember { mutableStateOf(Offset(0f, 0f))}
        var imageSize by remember { mutableStateOf(Size(0f, 0f)) }
        var currentRectangle by remember { mutableStateOf<Rect?>(null) }

        val context = LocalContext.current

        LaunchedEffect(documentId) {
            bitmap = getBitmapFromDocumentId(context, documentId)
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

    private fun _getExistingFileUri(context: Context, filename: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            storeUri, DocumentsContract.getTreeDocumentId(storeUri)
        )

        var existingFileUri: Uri? = null

        contentResolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                if (documentId.endsWith(filename)) {
                    existingFileUri = DocumentsContract.buildDocumentUriUsingTree(storeUri, documentId)
                    break
                }
            }
        }

        return existingFileUri
    }

    private fun getRectanglesFromFile(context: Context, filename: String): ArrayList<Pair<Int, Rect>> {
        var rectangles = ArrayList<Pair<Int, Rect>>()

        var existingFileUri: Uri = _getExistingFileUri(context, filename) ?: return rectangles

        // Open output stream for writing
        existingFileUri.let { it ->
            contentResolver.openInputStream(it)?.use { inputStream ->
                val labelData = inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }

                val labelDataArray = labelData.split("\n")

                labelDataArray.forEach {
                    if (it.length > 1) {
                        val splitLine = it.split(" ")

                        val labelId = splitLine[0].toInt()
                        val center = Offset(splitLine[1].toFloat(), splitLine[2].toFloat())
                        val size = Size(splitLine[3].toFloat(), splitLine[4].toFloat())
                        val topLeft = Offset(center.x - size.width / 2, center.y - size.height / 2)

                        rectangles.add(Pair(labelId, Rect(topLeft, size)))
                    }
                }
            }
        }

        return rectangles
    }

    private fun saveRectanglesToFile(context: Context, filename: String, rectangles: SnapshotStateList<Pair<Int, Rect>>) {
        var existingFileUri: Uri? = _getExistingFileUri(context, filename)

        val docUri: Uri = DocumentsContract.buildDocumentUriUsingTree(
            storeUri,
            DocumentsContract.getTreeDocumentId(storeUri)
        )

        // Convert the Tree URI into a valid Document URI
        val fileUri = existingFileUri ?: DocumentsContract.createDocument(
            contentResolver,
            docUri!!,
            "text/plain",
            filename
        )

        // Open output stream for writing
        fileUri?.let {
            contentResolver.openOutputStream(fileUri, "wt")?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    rectangles.forEach { rect ->
                        writer.write("${rect.first} ${rect.second.center.x} ${rect.second.center.y} ${rect.second.width} ${rect.second.height}\n")
                    }
                }
            }
        }
    }

    // Make the coordinates range between 0 and 1
    private fun normalizeRectangle(rect: Rect, size: Size): Rect {
        return Rect(
            rect.topLeft.x / size.width,
            rect.topLeft.y / size.height,
            rect.bottomRight.x / size.width,
            rect.bottomRight.y / size.height
        )
    }

    // Bring the normalized coordinates to their original values
    private fun denormalizeRectangle(rect: Rect, size: Size): Rect {
        return Rect(
            rect.topLeft.x * size.width,
            rect.topLeft.y * size.height,
            rect.bottomRight.x * size.width,
            rect.bottomRight.y * size.height
        )
    }

    private fun isInsideImage(point: Offset, imageTopLeft: Offset, imageBottomRight: Offset): Boolean {
        return point.x in imageTopLeft.x..imageBottomRight.x &&
                point.y in imageTopLeft.y..imageBottomRight.y
    }

    @Composable
    fun LabelDropDown(label: Int, onLabelChange: (newLabel: Int) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .padding(16.dp)
        ) {
            indexToLabelMap[label]?.let {
                ClickableText(text= AnnotatedString(it), style=TextStyle(textDecoration =TextDecoration.Underline), onClick= {
                    expanded = !expanded
                })
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                labelToIndexMap.forEach {
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
        Text(
            text="${currentImageIdx + 1} / ${imageData.size}",
            color=Color.Black)
    }

    private fun getBitmapFromDocumentId(context: Context, documentId: String): Bitmap {
        val uri: Uri = DocumentsContract.buildDocumentUriUsingTree(
            storeUri,
            documentId
        )

        val source = ImageDecoder.createSource(context.contentResolver, uri)
        
        return ImageDecoder.decodeBitmap(source)
    }
}

