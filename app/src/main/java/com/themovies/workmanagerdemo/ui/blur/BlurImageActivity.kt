package com.themovies.workmanagerdemo.ui.blur

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.work.WorkInfo
import com.bumptech.glide.Glide
import com.themovies.workmanagerdemo.R
import com.themovies.workmanagerdemo.databinding.ActivityBlurImageBinding
import com.themovies.workmanagerdemo.utils.KEY_IMAGE_URI
import com.themovies.workmanagerdemo.utils.LOGE

class BlurImageActivity : AppCompatActivity() {

    lateinit var binding: ActivityBlurImageBinding
    lateinit var viewModel: BlurViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlurImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProviders.of(this).get(BlurViewModel::class.java)
        getDataFromBundle()

        binding.goButton.setOnClickListener { viewModel.applyBlur(blurLevel) }

//        // Setup view output image file button
//        binding.seeFileButton.setOnClickListener {
//            viewModel.outputUri?.let { currentUri ->
//                val actionView = Intent(Intent.ACTION_VIEW, currentUri)
//                actionView.resolveActivity(packageManager)?.run {
//                    startActivity(actionView)
//                }
//            }
//        }

        // Hookup the Cancel button
        binding.cancelButton.setOnClickListener { viewModel.cancelWork() }

        viewModel.outputWorkInfos.observe(this, workInfosObserver())

    }

    private fun getDataFromBundle() {
        // Image uri should be stored in the ViewModel; put it there then display
        val imageUriExtra = intent.getStringExtra(KEY_IMAGE_URI)
        viewModel.setImageUri(imageUriExtra)
        viewModel.imageUri?.let { imageUri ->
            Glide.with(this).load(imageUri).into(binding.imageView)
        }
    }

    private fun workInfosObserver(): Observer<List<WorkInfo>> {
        return Observer { listOfWorkInfo ->

            // Note that these next few lines grab a single WorkInfo if it exists
            // This code could be in a Transformation in the ViewModel; they are included here
            // so that the entire process of displaying a WorkInfo is in one location.

            // If there are no matching work info, do nothing
            if (listOfWorkInfo.isNullOrEmpty()) {
                return@Observer
                LOGE("NULL WORK INFO")
            }
            LOGE("NULL WORK INFO")

            // We only care about the one output status.
            // Every continuation has only one worker tagged TAG_OUTPUT
            val workInfo = listOfWorkInfo[0]

            if (workInfo.state.isFinished) {

                LOGE("FINISHED")
                showWorkFinished()

                // Normally this processing, which is not directly related to drawing views on
                // screen would be in the ViewModel. For simplicity we are keeping it here.
                val outputImageUri = workInfo.outputData.getString(KEY_IMAGE_URI)

                // If there is an output file show "See File" button
                if (!outputImageUri.isNullOrEmpty()) {
                    viewModel.setOutputUri(outputImageUri)
                }
            } else {
                LOGE("STARTED")
                showWorkInProgress()
            }
        }
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     */
    private fun showWorkInProgress() {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            goButton.visibility = View.GONE
        }
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     */
    private fun showWorkFinished() {
        with(binding) {
            progressBar.visibility = View.GONE
            cancelButton.visibility = View.GONE
            goButton.visibility = View.VISIBLE
        }
    }

    private val blurLevel: Int
        get() =
            when (binding.radioBlurGroup.checkedRadioButtonId) {
                R.id.radio_blur_little -> 1
                R.id.radio_blur_medium -> 2
                R.id.radio_blur_most -> 3
                else -> 1
            }
}