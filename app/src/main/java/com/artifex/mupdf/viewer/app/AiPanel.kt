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

@Composable
fun AiPanel(
    isVisible: Boolean,
    streamedText: String,
    isLoading: Boolean,
    onSaveNote: () -> Unit,
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
                        TextButton(onClick = onClose) {
                            Text("Close", color = Color(0xFF888888), fontSize = 13.sp)
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFEEEEEE)
                    )

                    // Scrollable AI response
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = when {
                                isLoading && streamedText.isEmpty() -> "Analyzing selected text..."
                                streamedText.isEmpty() -> ""
                                else -> streamedText
                            },
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = if (isLoading && streamedText.isEmpty())
                                Color(0xFF999999) else Color(0xFF333333)
                        )
                    }

                    // Save button — only after streaming completes
                    if (!isLoading && streamedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onSaveNote,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save Atomic Note", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
