package com.artifex.mupdf.viewer.app

import com.artifex.mupdf.viewer.SecurePreferences

import android.os.Bundle
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.artifex.mupdf.viewer.DocumentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AiDocumentActivity : DocumentActivity(), LifecycleOwner, SavedStateRegistryOwner {

    // ── Lifecycle wiring (required for ComposeView inside plain Activity) ──────
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ── AI state ──────────────────────────────────────────────────────────────
    private val uiState = AiUiState()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var exporter: ObsidianExporter
    private var aiClient: AiClient? = null

    // ── Lifecycle callbacks ───────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        exporter = ObsidianExporter(this)
        // No longer initializing with static key here
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        scope.cancel()
        super.onDestroy()
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    override fun createUI(savedInstanceState: Bundle?) {
        super.createUI(savedInstanceState)

        val rootLayout = findRelativeLayout(window.decorView.rootView as ViewGroup) ?: return

        // Required: inject lifecycle owners so ComposeView can find them
        rootLayout.setViewTreeLifecycleOwner(this)
        rootLayout.setViewTreeSavedStateRegistryOwner(this)

        val composeView = ComposeView(this).apply {
            setContent {
                val s by uiState.flow.collectAsState()
                AiOverlay(
                    uiState = s,
                    onToggleAiMode = { uiState.toggleAiMode() },
                    onCircleComplete = { handleCircleComplete() },
                    onAddNote = { 
                        uiState.addNoteToSession() 
                    },
                    onExportSession = {
                        val pdfName = mDocTitle ?: "Unknown_PDF"
                        exporter.exportSession(pdfName, s.sessionNotes)
                        uiState.clearSession()
                    },
                    onOpenNotes = {
                        startActivity(android.content.Intent(this@AiDocumentActivity, NoteBrowserActivity::class.java))
                    },
                    onClosePanel = { uiState.closePanel() }
                )
            }
        }

        rootLayout.addView(
            composeView,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun handleCircleComplete() {
        val pageNum = mDocView?.getDisplayedViewIndex() ?: return
        uiState.startLoading()

        scope.launch {
            val pageText = withContext(Dispatchers.IO) {
                core?.getPageText(pageNum) ?: ""
            }
            if (pageText.isBlank()) {
                uiState.setError("No text found on this page.")
                return@launch
            }

            val key = SecurePreferences.getApiKey(this@AiDocumentActivity)
            val model = SecurePreferences.getModel(this@AiDocumentActivity)
            val baseUrl = SecurePreferences.getBaseUrl(this@AiDocumentActivity)

            if (key.isNullOrEmpty()) {
                val provider = when {
                    baseUrl.contains("deepseek") || model.contains("deepseek") -> "DeepSeek"
                    baseUrl.contains("dashscope") || model.contains("qwen") -> "Qwen"
                    baseUrl.contains("anthropic") || model.contains("claude") -> "Anthropic (Claude)"
                    baseUrl.contains("googleapis") || model.contains("gemini") -> "Gemini"
                    else -> "OpenAI"
                }
                uiState.setError("$provider API Key not set. Please set it in the AI Settings (Wrench icon).")
                return@launch
            }

            if (aiClient == null || aiClient?.apiKey != key || aiClient?.model != model || aiClient?.baseUrl != baseUrl) {
                // Load prompt from assets
                val prompt = try {
                    assets.open("prompts/explain_note.txt").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    "Please explain the following content:\n\n" // Fallback
                }
                aiClient = AiClient(key, baseUrl, model, prompt)
            }

            aiClient!!.explain(pageText)
                .catch { e -> uiState.setError(e.message ?: "Request failed") }
                .collect { chunk -> uiState.appendChunk(chunk) }
            uiState.finishLoading()
        }
    }

    private fun findRelativeLayout(view: ViewGroup): RelativeLayout? {
        if (view is RelativeLayout) return view
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i) as? ViewGroup ?: continue
            findRelativeLayout(child)?.let { return it }
        }
        return null
    }
}

// ── State holder ──────────────────────────────────────────────────────────────

data class AiUiData(
    val aiModeEnabled: Boolean = false,
    val isPanelVisible: Boolean = false,
    val isLoading: Boolean = false,
    val response: String = "",
    val sessionNotes: List<String> = emptyList()
)

class AiUiState {
    private val _flow = MutableStateFlow(AiUiData())
    val flow = _flow.asStateFlow()

    fun toggleAiMode() = _flow.update {
        it.copy(
            aiModeEnabled = !it.aiModeEnabled,
            isPanelVisible = if (it.aiModeEnabled) false else it.isPanelVisible
        )
    }
    fun startLoading() = _flow.update { it.copy(isPanelVisible = true, isLoading = true, response = "") }
    fun appendChunk(chunk: String) = _flow.update { it.copy(response = it.response + chunk) }
    fun setError(msg: String) = _flow.update { it.copy(isLoading = false, response = "[Error] $msg") }
    fun finishLoading() = _flow.update { it.copy(isLoading = false) }
    fun closePanel() = _flow.update { 
        it.copy(isPanelVisible = false, response = "", aiModeEnabled = false) 
    }

    fun addNoteToSession() = _flow.update {
        if (it.response.isNotEmpty()) {
            it.copy(
                sessionNotes = it.sessionNotes + it.response,
                response = "",
                isPanelVisible = false
            )
        } else it
    }

    fun clearSession() = _flow.update { it.copy(sessionNotes = emptyList()) }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
fun AiOverlay(
    uiState: AiUiData,
    onToggleAiMode: () -> Unit,
    onCircleComplete: () -> Unit,
    onAddNote: () -> Unit,
    onExportSession: () -> Unit,
    onOpenNotes: () -> Unit,
    onClosePanel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Gesture layer — active only in AI mode when panel is not showing
        if (uiState.aiModeEnabled && !uiState.isPanelVisible) {
            IonizationOverlay(onCircleComplete = { onCircleComplete() })
        }

        // Bottom panel with streaming AI response
        AiPanel(
            isVisible = uiState.isPanelVisible,
            streamedText = uiState.response,
            isLoading = uiState.isLoading,
            savedCount = uiState.sessionNotes.size,
            onAddNote = onAddNote,
            onExportSession = onExportSession,
            onOpenNotes = onOpenNotes,
            onClose = onClosePanel
        )

        // Floating AI toggle button (bottom-right, above MuPDF's bottom bar)
        FloatingActionButton(
            onClick = onToggleAiMode,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp),
            containerColor = if (uiState.aiModeEnabled) Color(0xFF1976D2) else Color.White,
            contentColor = if (uiState.aiModeEnabled) Color.White else Color(0xFF1976D2),
            elevation = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Text(text = "AI", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
