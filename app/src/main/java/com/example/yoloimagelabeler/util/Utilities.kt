package com.example.yoloimagelabeler.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.example.yoloimagelabeler.data.ImageData
import java.io.OutputStreamWriter

fun getExistingFileUri(context: Context, filename: String, folderUri: Uri): Uri? {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
        folderUri, DocumentsContract.getTreeDocumentId(folderUri)
    )

    var existingFileUri: Uri? = null

    context.contentResolver.query(childrenUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val documentId = cursor.getString(0)
            if (documentId.endsWith(filename)) {
                existingFileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)
                break
            }
        }
    }

    return existingFileUri
}

fun generateFilenameFromImageId(imageId: Int, extension: String, imageData: ArrayList<ImageData>): String {
    return imageData[imageId].filename.split(".")[0] + extension
}

// Make the coordinates range between 0 and 1
fun normalizeRectangle(rect: Rect, size: Size): Rect {
    return Rect(
        rect.topLeft.x / size.width,
        rect.topLeft.y / size.height,
        rect.bottomRight.x / size.width,
        rect.bottomRight.y / size.height
    )
}

// Bring the normalized coordinates to their original values
fun denormalizeRectangle(rect: Rect, size: Size): Rect {
    return Rect(
        rect.topLeft.x * size.width,
        rect.topLeft.y * size.height,
        rect.bottomRight.x * size.width,
        rect.bottomRight.y * size.height
    )
}

fun isInsideImage(point: Offset, imageTopLeft: Offset, imageBottomRight: Offset): Boolean {
    return point.x in imageTopLeft.x..imageBottomRight.x &&
            point.y in imageTopLeft.y..imageBottomRight.y
}

fun getRectanglesFromFile(context: Context, filename: String, folderUri: Uri): ArrayList<Pair<Int, Rect>> {
    var rectangles = ArrayList<Pair<Int, Rect>>()

    var existingFileUri: Uri = getExistingFileUri(context, filename, folderUri) ?: return rectangles

    // Open output stream for writing
    existingFileUri.let { it ->
        context.contentResolver.openInputStream(it)?.use { inputStream ->
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

fun saveRectanglesToFile(
    context: Context,
    filename: String,
    rectangles: SnapshotStateList<Pair<Int, Rect>>,
    folderUri: Uri,
) {
    var existingFileUri: Uri? = getExistingFileUri(context, filename, folderUri)

    val docUri: Uri = DocumentsContract.buildDocumentUriUsingTree(
        folderUri,
        DocumentsContract.getTreeDocumentId(folderUri)
    )

    // Convert the Tree URI into a valid Document URI
    val fileUri = existingFileUri ?: DocumentsContract.createDocument(
        context.contentResolver,
        docUri!!,
        "text/plain",
        filename
    )

    // Open output stream for writing
    fileUri?.let {
        context.contentResolver.openOutputStream(fileUri, "wt")?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                rectangles.forEach { rect ->
                    writer.write("${rect.first} ${rect.second.center.x} ${rect.second.center.y} ${rect.second.width} ${rect.second.height}\n")
                }
            }
        }
    }
}

fun getBitmapFromDocumentId(context: Context, documentId: String, folderUri: Uri): Bitmap {
    val uri: Uri = DocumentsContract.buildDocumentUriUsingTree(
        folderUri,
        documentId
    )

    val source = ImageDecoder.createSource(context.contentResolver, uri)

    return ImageDecoder.decodeBitmap(source)
}
