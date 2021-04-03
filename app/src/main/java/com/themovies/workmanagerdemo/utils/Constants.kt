package com.themovies.workmanagerdemo.utils


const val REQUEST_CODE_PERMISSIONS = 101
const val REQUEST_CODE_IMAGE = 100
const val MAX_NUMBER_REQUEST_PERMISSIONS = 2
const val KEY_PERMISSIONS_REQUEST_COUNT = "KEY_PERMISSIONS_REQUEST_COUNT"
const val KEY_IMAGE_URI = "KEY_IMAGE_URI"


// Name of Notification Channel for verbose notifications of background work
val VERBOSE_NOTIFICATION_CHANNEL_NAME: CharSequence =
    "Verbose WorkManager Notifications"
const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION =
    "Shows notifications whenever work starts"
val NOTIFICATION_TITLE: CharSequence = "WorkRequest Starting"
const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
const val NOTIFICATION_ID = 1

// The name of the image manipulation work
const val IMAGE_MANIPULATION_WORK_NAME = "image_manipulation_work"

// Other keys
const val OUTPUT_PATH = "blur_filter_outputs"
const val TAG_OUTPUT = "OUTPUT"

