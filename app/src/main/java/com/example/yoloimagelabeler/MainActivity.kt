package com.example.yoloimagelabeler

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.yoloimagelabeler.Screens.ImageLabeler.ImageLabelerScreen
import com.example.yoloimagelabeler.data.FolderConfig
import com.example.yoloimagelabeler.data.ImageData
import com.example.yoloimagelabeler.data.LabelData
import com.example.yoloimagelabeler.data.LocalFolderConfig
import com.example.yoloimagelabeler.ui.theme.YoloImageLabelerTheme
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var onSelectFolderSuccess: () -> Unit

    private var folderConfig: FolderConfig = FolderConfig()

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
                    folderConfig.folderUri = uri
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

        var labelFileDocumentId: String = ""

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
                    folderConfig.imageData.add(ImageData(filename=displayName, documentId=docId))
                    Log.i("MainActivity", "Image: $displayName, $mimeType")
                } else if (mimeType.startsWith("text/plain")) {
                    Log.i("MainActivity", "Label: $displayName, $mimeType")

                    if (displayName == "labels.txt") {
                        labelFileDocumentId = docId
                    } else {
                        folderConfig.labelData.add(LabelData(filename=displayName, documentId=docId))
                    }
                }
            }
        }

        if (labelFileDocumentId != "") {
            generateMapFromLabelFile(labelFileDocumentId)
        }

        onSuccess()
    }

    private fun generateMapFromLabelFile(labelFileDocumentId: String) {
        val uri: Uri = DocumentsContract.buildDocumentUriUsingTree(
            folderConfig.folderUri,
            labelFileDocumentId
        )

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader->
                    val labels = reader.readText().split("\r\n")

                    labels.forEachIndexed { i, it ->
                        folderConfig.labelToIndexMap[it] = i
                        folderConfig.indexToLabelMap[i] = it
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
                CompositionLocalProvider(LocalFolderConfig provides folderConfig) {
                    ImageLabelerScreen()
                }
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
                folderPickerLauncher.launch(intent)
            }
        ) {
            Text(text="Select Image Folder")
        }
    }
}

