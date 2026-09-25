package com.yusufteker.planora.core.export

import com.yusufteker.planora.shared.api.TaskDto
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

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
     * @return True if the share sheet was successfully presented, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun export(tasks: List<TaskDto>, fileName: String, format: ExportFormat): Boolean {
        if (tasks.isEmpty()) return false

        return try {
            val (content, extension) = when (format) {
                ExportFormat.CSV -> Pair(tasks.toCsvString(), "csv")
                ExportFormat.REPORT -> Pair(tasks.toReportString(), "txt")
            }
            val tempPath = "${NSTemporaryDirectory()}$fileName.$extension"

            val nsString = (content as Any) as NSString
            nsString.writeToFile(tempPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)

            val fileUrl = platform.Foundation.NSURL.fileURLWithPath(tempPath)

            val activityVC = UIActivityViewController(
                activityItems = listOf(fileUrl),
                applicationActivities = null
            )

            val window = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
                ?.keyWindow
                ?: UIApplication.sharedApplication.keyWindow
                ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)

            var topVC = window?.rootViewController
            while (topVC?.presentedViewController != null) {
                topVC = topVC.presentedViewController
            }

            if (topVC == null) return false

            activityVC.popoverPresentationController?.apply {
                sourceView = topVC.view
                sourceRect = platform.CoreGraphics.CGRectMake(0.0, 0.0, 1.0, 1.0)
            }

            topVC.presentViewController(activityVC, animated = true, completion = null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
