package com.themovies.workmanagerdemo.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.themovies.workmanagerdemo.utils.LOGE
import com.themovies.workmanagerdemo.utils.LOGI
import com.themovies.workmanagerdemo.utils.OUTPUT_PATH
import java.io.File

/**
 * Cleans up temporary files generated during blurring process
 */
class CleanupWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        // Makes a notification when the work starts and slows down the work so that
        // it's easier to see each WorkRequest start, even on emulated devices
        makeStatusNotification("Cleaning up old temporary files", applicationContext)

        return try {
            val outputDirectory = File(applicationContext.filesDir, OUTPUT_PATH)
            if (outputDirectory.exists()) {
                val entries = outputDirectory.listFiles()
                if (entries != null) {
                    for (entry in entries) {
                        val name = entry.name
                        if (name.isNotEmpty() && name.endsWith(".png")) {
                            val deleted = entry.delete()
                            LOGI("Deleted $name - $deleted")
                        }
                    }
                }
            }
            Result.success()
        } catch (exception: Exception) {
            LOGE(exception.toString())
            Result.failure()
        }
    }
}
