package com.xckevin.android.app.webview.test.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcode: (String) -> Unit,
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(),
) : ImageAnalysis.Analyzer {
    private val lock = Any()
    private val emittedRawValues = mutableSetOf<String>()
    private var hasEmitted = false

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )

        runCatching {
            barcodeScanner.process(inputImage)
        }.onSuccess { task ->
            task.addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstNotNullOfOrNull { barcode ->
                    barcode.rawValue?.takeIf { it.isNotBlank() }
                }
                if (rawValue != null) {
                    emitIfFirstUnique(rawValue)
                }
            }.addOnFailureListener {
                // ImageProxy is closed in the completion listener below.
            }.addOnCompleteListener {
                imageProxy.close()
            }
        }.onFailure {
            imageProxy.close()
        }
    }

    fun close() {
        barcodeScanner.close()
    }

    private fun emitIfFirstUnique(rawValue: String) {
        val shouldEmit = synchronized(lock) {
            if (hasEmitted || !emittedRawValues.add(rawValue)) {
                false
            } else {
                hasEmitted = true
                true
            }
        }

        if (shouldEmit) {
            onBarcode(rawValue)
        }
    }
}
