package com.example.yoloimagelabeler

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.yoloimagelabeler.ui.theme.YoloImageLabelerTheme

class MainActivity : ComponentActivity() {
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Intent>;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YoloImageLabelerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Screen()
                }
            }
        }

        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    listFilesInFolder(it)
                }
            } else {
                Toast.makeText(this, "Folder selection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun listFilesInFolder(uri: Uri) {
        val resolver: ContentResolver = contentResolver

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.Document.COLUMN_DISPLAY_NAME)

        val cursor = resolver.query(childrenUri, null, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val displayName = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
                val mimeType = it.getString(it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE))

                if (mimeType.startsWith("image/") || mimeType == "text/plain") {
                    println("Found File: $displayName, $mimeType")
                }
            }
        }
    }

    @Composable
    fun Screen() {
        Column(
            modifier=Modifier.fillMaxSize(),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.Center
        ) {
            SelectFolderButton()
        }
    }

    @Composable
    fun SelectFolderButton(modifier: Modifier = Modifier) {
        Button(
            onClick={
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                folderPickerLauncher.launch(intent)
            }
        ) {
            Text(text="Select Image Folder")
        }
    }
}

