package com.artifex.mupdf.viewer.app

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class AtomicNote(val rawContent: String) {
    // 自动从 Markdown 内容中提取 # 后的内容作为标题
    val title: String by lazy {
        rawContent.lines()
            .firstOrNull { it.trim().startsWith("# ") }
            ?.replace("# ", "")
            ?.replace(Regex("[\\\\/:*?\"<>|]"), "") // 移除非法文件名字符
            ?.trim()
            ?: "Untitled_Atom_${System.currentTimeMillis()}"
    }
}

class ObsidianExporter(private val context: Context) {

    private fun getBaseDir(): File {
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ObsidianVault/Atoms")
        if (!baseDir.exists()) baseDir.mkdirs()
        return baseDir
    }

    fun exportSession(pdfName: String, notes: List<String>): String? {
        if (notes.isEmpty()) return null
        
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val baseDir = getBaseDir()

            val atomicNotes = notes.map { AtomicNote(it) }

            // 1. 写入每一个原子笔记
            atomicNotes.forEach { note ->
                val file = File(baseDir, "${note.title}.md")
                file.writeText(note.rawContent)
            }

            // 2. 生成索引文件 (MOC)
            val cleanPdfName = pdfName.replace(".pdf", "")
            val indexFileName = "${cleanPdfName}_Index_$dateStr.md"
            val indexFile = File(baseDir, indexFileName)
            
            val indexContent = StringBuilder().apply {
                append("# 📖 学习会话索引: $cleanPdfName\n\n")
                append("---")
                append("\n- **日期**: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
                append("\n- **来源文件**: [[$pdfName]]")
                append("\n- **类型**: #session_index")
                append("\n---\n\n")
                append("## 采集到的知识原子\n\n")
                atomicNotes.forEach { note ->
                    append("- [[${note.title}]]\n")
                }
                append("\n\n---\n#atom #index")
            }.toString()

            indexFile.writeText(indexContent)
            
            Log.d("ObsidianExporter", "Exported ${notes.size} notes and index to ${indexFile.absolutePath}")
            
            // 3. 尝试直接打开 Obsidian (如果安装了)
            openInObsidian(indexFileName)
            
            return indexFile.absolutePath
        } catch (e: Exception) {
            Log.e("ObsidianExporter", "Failed to export session", e)
            return null
        }
    }

    private fun openInObsidian(fileName: String) {
        try {
            // 使用 Obsidian 的 URI Scheme: obsidian://open?vault=ObsidianVault&file=Atoms/filename
            // 注意：这里假设用户库名为 ObsidianVault，且路径正确
            val uriString = "obsidian://open?vault=ObsidianVault&file=Atoms/${fileName.replace(".md", "")}"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriString))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果没安装 Obsidian 或者 Scheme 不支持，静默失败
            Log.w("ObsidianExporter", "Could not open Obsidian via URI", e)
        }
    }

    fun exportAtomicNote(content: String, title: String? = null): Boolean {
        val note = AtomicNote(content)
        val finalTitle = title ?: note.title
        return try {
            val baseDir = getBaseDir()
            val file = File(baseDir, "$finalTitle.md")
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
}
