package com.artifex.mupdf.viewer.app

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ObsidianExporter(private val context: Context) {

    fun exportAtomicNote(content: String, title: String? = null): Boolean {
        try {
            // Default Obsidian directory (this might need to be configurable by the user)
            val baseDir = File(Environment.getExternalStorageDirectory(), "Documents/ObsidianVault/Atoms")
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }

            val fileName = title ?: "Atom_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
            val file = File(baseDir, "$fileName.md")

            val markdownContent = """
                # $fileName
                
                Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                Tags: #atom #atomread
                
                ---
                
                $content
            """.trimIndent()

            file.writeText(markdownContent)
            Log.d("ObsidianExporter", "Exported to ${file.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e("ObsidianExporter", "Failed to export note", e)
            return false
        }
    }
}
