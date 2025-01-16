package com.example.tasknumbertree

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация камеры
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().apply {
                setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy)
                }
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val detector = FaceDetection.getClient()

        detector.process(image)
            .addOnSuccessListener { faces ->
                val overlay = findViewById<FaceOverlayView>(R.id.drawingView)
                val previewView = findViewById<PreviewView>(R.id.previewView)

                // Получаем размеры PreviewView
                val previewWidth = previewView.width.toFloat()
                val previewHeight = previewView.height.toFloat()

                // Преобразование координат
                val faceRectangles = faces.map { face ->
                    val boundingBox = face.boundingBox

                    // Преобразование в RectF с учетом масштаба PreviewView
                    RectF(
                        boundingBox.left * previewWidth / image.height,
                        boundingBox.top * previewHeight / image.width,
                        boundingBox.right * previewWidth / image.height,
                        boundingBox.bottom * previewHeight / image.width
                    )
                }

                // Передаем преобразованные координаты в overlay
                overlay.drawFaceRectangles(faceRectangles)
            }
            .addOnFailureListener { e -> e.printStackTrace() }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}