package com.artifex.mupdf.viewer.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.artifex.mupdf.viewer.ReaderView
import kotlinx.coroutines.launch

class AtomReadActivity : ComponentActivity() {
    private lateinit var exporter: ObsidianExporter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exporter = ObsidianExporter(this)

        val uri = intent.data ?: Uri.EMPTY
        val mimetype = intent.type ?: "application/pdf"

        setContent {
            AtomReadScreen(uri, mimetype, exporter)
        }
    }
}

@Composable
fun AtomReadScreen(uri: Uri, mimetype: String, exporter: ObsidianExporter) {
    var showAiDialog by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        PdfViewer(uri, mimetype)

        IonizationOverlay(onCircleComplete = { bounds ->
            coroutineScope.launch {
                currentNote = mockAiExtraction(bounds)
                showAiDialog = true
            }
        })

        AiFloatingDialog(
            isVisible = showAiDialog,
            noteContent = currentNote,
            onExport = {
                exporter.exportAtomicNote(currentNote)
                showAiDialog = false
            },
            onClose = { showAiDialog = false }
        )
    }
}

suspend fun mockAiExtraction(bounds: androidx.compose.ui.geometry.Rect): String {
    return """
        ### Extract: Quantum Entanglement

        The circled region discusses the non-local correlation between particles.

        **Key Concept:** When particles become entangled, their states are linked such that the measurement of one instantly determines the state of the other, regardless of distance.

        **Atomic Insight:** This challenges our classical understanding of locality and causality.
    """.trimIndent()
}

@Composable
fun PdfViewer(uri: Uri, mimetype: String) {
    AndroidView(
        factory = { context ->
            ReaderView(context).apply {
                // Initialize core and adapter here
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
