package com.yusufteker.planora.core.export

import com.yusufteker.planora.shared.api.TaskDto
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * iOS implementation of [TaskCsvExporter].
 *
 * Writes the CSV content to the system temporary directory, then presents
 * a [UIActivityViewController] (the native iOS share sheet) from the root
 * view controller so the user can AirDrop, save to Files, email, etc.
 */
actual class TaskCsvExporter {

    /**
     * Exports [tasks] as a file and presents the iOS share sheet.
     *
     * @param tasks List of tasks to include in the export.
     * @param fileName Suggested name for the output file (without extension).
     * @param format Export format (CSV or Report).
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun export(tasks: List<TaskDto>, fileName: String, format: ExportFormat) {
        if (tasks.isEmpty()) return

        val (content, extension) = when (format) {
            ExportFormat.CSV -> Pair(tasks.toCsvString(), "csv")
            ExportFormat.REPORT -> Pair(tasks.toReportString(), "txt")
        }
        val tempPath = "${NSTemporaryDirectory()}$fileName.$extension"

        val nsString = content as NSString
        nsString.writeToFile(tempPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)

        val fileUrl = platform.Foundation.NSURL.fileURLWithPath(tempPath)

        val activityVC = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null
        )

        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(activityVC, animated = true, completion = null)
    }
}
