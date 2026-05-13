package com.artifex.mupdf.viewer.app

import android.os.Bundle
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64

class NoteBrowserActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteBrowserScreen(onBack = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBrowserScreen(onBack: () -> Unit) {
    var selectedNote by remember { mutableStateOf<File?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val notes = remember { mutableStateListOf<File>() }

    // Load notes from ObsidianVault
    LaunchedEffect(Unit) {
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ObsidianVault/Atoms")
        if (baseDir.exists()) {
            val files = baseDir.listFiles { _, name -> name.endsWith(".md") }?.sortedByDescending { it.lastModified() }
            if (files != null) {
                notes.addAll(files)
            }
        }
    }

    val filteredNotes = if (searchQuery.isEmpty()) {
        notes
    } else {
        notes.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedNote == null) "Internal Notes" else selectedNote!!.name.replace(".md", "")) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedNote != null) selectedNote = null else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FA)
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedNote == null) {
                Column {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        placeholder = { Text("Search notes...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (filteredNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No notes found in ObsidianVault/Atoms", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredNotes) { file ->
                                NoteItem(file) { selectedNote = file }
                            }
                        }
                    }
                }
            } else {
                NoteViewer(selectedNote!!)
            }
        }
    }
}

@Composable
fun NoteItem(file: File, onClick: () -> Unit) {
    val isIndex = file.name.contains("_Index_")
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isIndex) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isIndex) "📖 " else "📄 ",
                    fontSize = 18.sp
                )
                Text(
                    text = file.name.replace(".md", ""),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A2E)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last modified: $date",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun NoteViewer(file: File) {
    val content = remember(file) { file.readText() }
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl("file:///android_asset/katex_renderer.html")
                
                // Once page is loaded, update content
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val encoded = Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
                        evaluateJavascript("updateContent(decodeURIComponent(escape(atob('$encoded'))))", null)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
