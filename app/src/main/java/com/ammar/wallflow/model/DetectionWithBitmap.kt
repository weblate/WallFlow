package com.ammar.wallflow.model

import android.graphics.Bitmap
import org.tensorflow.lite.task.vision.detector.Detection

data class DetectionWithBitmap(
    val detection: Detection,
    val bitmap: Bitmap,
)