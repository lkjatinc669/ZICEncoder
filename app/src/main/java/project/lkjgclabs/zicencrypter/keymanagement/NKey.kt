package project.lkjgclabs.zicencrypter.keymanagement

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import project.lkjgclabs.zicencrypter.PINput
import project.lkjgclabs.zicencrypter.R
import project.lkjgclabs.zicencrypter.auth.AuthManager
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NKey : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "zic_preferences"
        private const val KEY_ZIC = "zic_key"
    }

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: MaterialButton
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCapturing = false

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nkey)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<MaterialButton>(R.id.btnBackMain)
            .setOnClickListener { finish() }

        btnCapture.setOnClickListener {
            capturePhotoAndSaveKey()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({
            if (isFinishing || isDestroyed) return@addListener

            try {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = viewFinder.surfaceProvider
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
                    .build()

                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )

                imageCapture = capture
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Could not start the camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhotoAndSaveKey() {
        if (isCapturing) return

        val capture = imageCapture ?: run {
            Toast.makeText(
                this,
                "Camera is not ready.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isCapturing = true
        btnCapture.isEnabled = false

        val photoFile = File(
            cacheDir,
            "zic_${System.currentTimeMillis()}.jpg"
        )

        val options = ImageCapture.OutputFileOptions.Builder(
            photoFile
        ).build()

        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exception: ImageCaptureException) {
                    photoFile.delete()
                    isCapturing = false
                    btnCapture.isEnabled = true

                    Toast.makeText(
                        this@NKey,
                        "Photo capture failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onImageSaved(
                    output: ImageCapture.OutputFileResults
                ) {
                    cameraExecutor.execute {
                        try {
                            val key = photoFile.inputStream().use { stream ->
                                val digest = MessageDigest.getInstance("SHA-512")
                                val buffer = ByteArray(8192)

                                while (true) {
                                    val count = stream.read(buffer)
                                    if (count < 0) break
                                    digest.update(buffer, 0, count)
                                }

                                digest.digest().joinToString("") {
                                    "%02x".format(it.toInt() and 0xff)
                                }
                            }

                            photoFile.delete()

                            runOnUiThread {
                                if (isFinishing || isDestroyed) return@runOnUiThread

                                val saved = getSharedPreferences(
                                    PREFS_NAME,
                                    MODE_PRIVATE
                                ).edit()
                                    .putString(KEY_ZIC, key)
                                    .commit()

                                isCapturing = false

                                if (saved) {
                                    Toast.makeText(
                                        this@NKey,
                                        "New ZIC key saved.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                } else {
                                    btnCapture.isEnabled = true
                                    Toast.makeText(
                                        this@NKey,
                                        "Could not save the key.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            photoFile.delete()

                            runOnUiThread {
                                isCapturing = false
                                btnCapture.isEnabled = true
                                Toast.makeText(
                                    this@NKey,
                                    "Could not process the photo.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (!AuthManager.isAuthenticated) {
            startActivity(
                Intent(this, PINput::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }
}