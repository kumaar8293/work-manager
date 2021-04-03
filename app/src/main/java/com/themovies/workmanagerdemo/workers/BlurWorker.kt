package com.themovies.workmanagerdemo.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.themovies.workmanagerdemo.utils.LOGE
import com.themovies.workmanagerdemo.utils.KEY_IMAGE_URI

/**
 * Worker class is where we define work to perform in Background
 */
class BlurWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    /**
     * Override doWork() to implement the work we want done
     *
     * Here we're creating  blurred bitmap based on a image that's passed in
     */
    override fun doWork(): Result {
        LOGE("Image blurred started")
        val appContext = applicationContext
        val resourceUri = inputData.getString(KEY_IMAGE_URI)

        makeStatusNotification("Blurring image", appContext)
        return try {
            if (TextUtils.isEmpty(resourceUri)) {
                LOGE("Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }

            val resolver = appContext.contentResolver

            val picture = BitmapFactory.decodeStream(
                resolver.openInputStream(Uri.parse(resourceUri))
            )

            val output = blurBitmap(picture, appContext)

            // Write bitmap to a temp file
            val outputUri = writeBitmapToFile(appContext, output)

            // The Worker accept input and produces output with the Data Object [input data] associated with
            // key - value pair
            val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            //We need to eventually return a result to let us know weather the work was completed successfully
            // Here we are outputting the URI of the newly blurred image

            LOGE("Image blurred successfully")
            Result.success(outputData)
        } catch (throwable: Throwable) {
            LOGE("Error applying blur ${throwable.message}")
            // If there was an error return failure
            Result.failure()
            // In some cases may be want to use retry
        }
    }
}
