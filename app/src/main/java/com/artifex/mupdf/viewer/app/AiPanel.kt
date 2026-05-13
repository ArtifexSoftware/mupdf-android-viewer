package com.artifex.mupdf.viewer.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Base64

@Composable
fun AiPanel(
    isVisible: Boolean,
    streamedText: String,
    isLoading: Boolean,
    savedCount: Int,
    onAddNote: () -> Unit,
    onExportSession: () -> Unit,
    onOpenNotes: () -> Unit,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed backdrop — only when panel is visible
        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color(0xFFF8F9FA),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color(0xFFDDDDDD), RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Assistant",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A2E)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        TextButton(onClick = onOpenNotes) {
                            Text("My Notes", color = Color(0xFF1976D2), fontSize = 13.sp)
                        }
                        TextButton(onClick = onClose) {
                            Text("Close", color = Color(0xFF888888), fontSize = 13.sp)
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFEEEEEE)
                    )

                    // WebView-based AI response
                    val webView = remember { mutableStateOf<WebView?>(null) }
                    
                    LaunchedEffect(streamedText) {
                        val encoded = Base64.encodeToString(streamedText.toByteArray(), Base64.NO_WRAP)
                        webView.value?.evaluateJavascript("updateContent(decodeURIComponent(escape(atob('$encoded'))))", null)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column {
                            if (savedCount > 0 && !isLoading && streamedText.isEmpty()) {
                                Text(
                                    "Current Session Notes:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                // Simple list of session notes (just snippets or counts)
                                Text(
                                    "You have $savedCount notes in this session. Click 'Export' to save them as a directory in Obsidian.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                            
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        webViewClient = WebViewClient()
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        setBackgroundColor(AndroidColor.TRANSPARENT)
                                        loadUrl("file:///android_asset/katex_renderer.html")
                                        webView.value = this
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Session Actions
                    if (savedCount > 0 || (!isLoading && streamedText.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (savedCount > 0) {
                                Button(
                                    onClick = onExportSession,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50) // Green for finish
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Export Session ($savedCount)", fontSize = 13.sp)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            if (!isLoading && streamedText.isNotEmpty()) {
                                Button(
                                    onClick = onAddNote,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1976D2)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add to Session", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
