package com.yusufteker.planora.core.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.yusufteker.planora.shared.api.TaskDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Android implementation of [TaskCsvExporter].
 *
 * Writes the CSV data to a temporary file in the app's cache directory,
 * then opens the system share sheet via [Intent.ACTION_SEND] so the user
 * can save or send the file using any compatible app (Files, Gmail, etc.).
 *
 * Uses [FileProvider] to safely share the file URI with external apps.
 */
actual class TaskCsvExporter : KoinComponent {

    private val context: Context by inject()

    /**
     * Exports the given [tasks] to a file and opens the system share sheet.
     *
     * @param tasks List of tasks to include in the export.
     * @param fileName Suggested name for the output file (without extension).
     * @param format Export format (CSV or Report).
     */
    actual fun export(tasks: List<TaskDto>, fileName: String, format: ExportFormat) {
        if (tasks.isEmpty()) return

        val (content, extension, mimeType) = when (format) {
            ExportFormat.CSV -> Triple(tasks.toCsvString(), "csv", "text/csv")
            ExportFormat.REPORT -> Triple(tasks.toReportString(), "txt", "text/plain")
        }

        val file = File(context.cacheDir, "$fileName.$extension").apply {
            writeText(content, Charsets.UTF_8)
        }

        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Export Tasks").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
