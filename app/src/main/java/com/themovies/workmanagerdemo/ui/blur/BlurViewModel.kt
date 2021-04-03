package com.themovies.workmanagerdemo.ui.blur

import android.app.Application
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.themovies.workmanagerdemo.utils.IMAGE_MANIPULATION_WORK_NAME
import com.themovies.workmanagerdemo.utils.KEY_IMAGE_URI
import com.themovies.workmanagerdemo.utils.TAG_OUTPUT
import com.themovies.workmanagerdemo.workers.BlurWorker
import com.themovies.workmanagerdemo.workers.CleanupWorker
import com.themovies.workmanagerdemo.workers.SaveImageToFileWorker
import java.util.concurrent.TimeUnit

class BlurViewModel(application: Application) : AndroidViewModel(application) {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null
    private val workManager = WorkManager.getInstance(application)

    /**
     * WorkInfo let us know the status of a work request: Blocked, cancelled, enqueued, failed, running, success
     *
     * We can get LiveData<WorkInfo> in 3 different ways....
     * 1. Using the UniqueId of a WorkRequest
     * 2. Using the WorkRequest's unique chain name
     * 3. The TAG name of the WorkRequest, that you want to optionally add
     *
     */
    internal val outputWorkInfos: LiveData<List<WorkInfo>>

    init {
        // This transformation makes sure that whenever the current work Id changes the WorkInfo
        // the UI is listening to changes
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }

    /**
     * Creates the input data bundle which includes the Uri to operate on
     * @return Data which contains the Image Uri as a String
     */
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }

    private fun onTimeRequest() {

        //Simple Work request
        // val myWorkRequest = OneTimeWorkRequest.from(BlurWorker::class.java)

        //For more complex, we can use Builder class

        val blurRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            .setInputData(createInputDataForUri())
//            .setInitialDelay(10,TimeUnit.MINUTES) // To make initial delays
            .addTag(TAG_OUTPUT)
            .build()
        workManager.beginWith(blurRequest).enqueue()

    }


    private fun periodicRequest() {
        //If we want to run our job periodically then we can use PeriodicWorkRequest
        //  Note: The minimum repeat interval that can be defined is 15 minutes (same as the JobScheduler API).
        //The following is an example of periodic work that can run during the last 15 minutes of every one hour period.


        val myUploadWork = PeriodicWorkRequestBuilder<SaveImageToFileWorker>(
            1, TimeUnit.HOURS, // repeatInterval (the period cycle)
            15, TimeUnit.MINUTES
        ) // flexInterval
            .build()

        //  The repeat interval must be greater than or equal to PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS
        //  and the flex interval must be greater than or equal to PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS.

        // workManager.beginWith(myUploadWork) doesn't work with PeriodicRequest
        workManager.enqueue(myUploadWork)
    }


    private fun retryAndBackOffPolicy() {
        /**
         * If you require that WorkManager retry your work, you can return Result.retry() from your worker.
         * Your work is then rescheduled according to a backoff delay and backoff policy.
         *      1. Backoff delay specifies the minimum amount of time to wait before retrying your work after the first attempt.
         *              This value can be no less than 10 seconds (or MIN_BACKOFF_MILLIS).
         *
         *      2. Backoff policy defines how the backoff delay should increase over time for subsequent
         *              retry attempts. WorkManager supports 2 backoff policies, LINEAR and EXPONENTIAL.
         *
         * Every work request has a backoff policy and backoff delay. The default policy is EXPONENTIAL
         *  with a delay of 10 seconds, but you can override this in your work request configuration.
         */

        val myWorkRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        //Result.retry() will be attempted again after 10 seconds, followed by 20, 30, 40, and so on,
    }

    fun tagWork() {
        /**
         * If you have a group of logically related work, you may also find it helpful to tag those work items.
         * Tagging allows you to operate with a group of work requests together.
         *
         * For example, WorkManager.cancelAllWorkByTag(String) cancels all Work Requests with a particular tag,
         * and WorkManager.getWorkInfosByTag(String) returns a list of the WorkInfo objects which can be
         * used to determine the current work state.
         */

        val myWorkRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            .addTag("cleanup")
            .build()
    }

    fun assignInputData() {
        //Your work may require input data in order to do its work.

        // Create a WorkRequest for your Worker and sending it input
        val myUploadWork = OneTimeWorkRequestBuilder<BlurWorker>()
            .setInputData(
                workDataOf(
                    "IMAGE_URI" to "http://..."
                )
            )
            .build()
    }

    private fun chainRequest() {
        val blurRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            .setInputData(createInputDataForUri())
            .addTag(TAG_OUTPUT)
            .build()

        val saveImageWorkRequest = OneTimeWorkRequest.from(SaveImageToFileWorker::class.java)


        //Calling then allow us to run task sequentially
        // It also means the output data of the first request will be automatically input for second request
        // workManager.beginWith(blurRequest).then(saveImageWorkRequest).enqueue()
        //We can run task parallel
//        workManager.beginWith(listOf(blurRequest,saveImageWorkRequest)).enqueue()

        //Suppose if a user clicks button again and again then it will chain multiple works
        // causing a-lot of redundant work and we don't want

        workManager.beginUniqueWork(
            IMAGE_MANIPULATION_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            blurRequest
        ).then(saveImageWorkRequest).enqueue()

    }


    fun constraint() {
        /**
         *  NetworkType => 	Constrains the type of network required for your work to run. For example, Wi-Fi (UNMETERED).
         *  BatteryNotLow =>	When set to true, your work will not run if the device is in low battery mode.
         *  RequiresCharging =>	When set to true, your work will only run when the device is charging.
         *  DeviceIdle =>	When set to true, this requires the user’s device to be idle before the work will run.
         *               This can be useful for running batched operations that might otherwise have a negative performance impact
         *               on other apps running actively on the user’s device.
         *  StorageNotLow =>	When set to true, your work will not run if the user’s storage space on the device is too low.
         **/

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val myWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<BlurWorker>()
                .setConstraints(constraints)
                .build()
    }

    private fun cancelRequest() {

        // by id
        // workManager.cancelWorkById(syncWorker.id)

// by name
        workManager.cancelUniqueWork("sync")

// by tag
        workManager.cancelAllWorkByTag("syncTag")

    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        onTimeRequest()
        //chainRequest()
        // Add WorkRequest to Cleanup temporary images


        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )

        // Add WorkRequests to blur the image the number of times requested
        for (i in 0 until blurLevel) {
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }

            continuation = continuation.then(blurBuilder.build())
        }

        // Create charging constraint
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .setConstraints(constraints)
            .addTag(TAG_OUTPUT)
            .build()
        continuation = continuation.then(save)

        // Actually start the work
        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    internal fun setImageUri(uri: String?) {
        imageUri = uriOrNull(uri)
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

}