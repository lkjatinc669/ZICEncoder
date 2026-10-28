package project.lkjgclabs.zicencrypter.keymanagement

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import project.lkjgclabs.zicencrypter.PINput
import project.lkjgclabs.zicencrypter.R
import project.lkjgclabs.zicencrypter.auth.AuthManager

class IKey : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "zic_preferences"
        private const val KEY_ZIC = "zic_key"
        private const val QR_PREFIX = "zic_key:"
    }

    private lateinit var barcodeView: DecoratedBarcodeView

    private var isProcessing = false
    private var isSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ikey)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_ikey)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        barcodeView = findViewById(R.id.zxingBarcodeScanner)
        barcodeView.statusView.visibility = View.GONE

        findViewById<MaterialButton>(R.id.btnBackMain)
            .setOnClickListener { finish() }

        barcodeView.decodeContinuous(object : BarcodeCallback {

            override fun barcodeResult(result: BarcodeResult?) {
                if (isProcessing || isSaved) return

                val qrData = result?.text ?: return
                isProcessing = true
                barcodeView.pause()

                importKey(qrData)
            }
        })
    }

    private fun importKey(qrData: String) {
        val key = if (qrData.startsWith(QR_PREFIX)) {
            qrData.removePrefix(QR_PREFIX)
        } else {
            qrData
        }

        val isValid = key.length == 128 &&
                key.all { it in "0123456789abcdefABCDEF" }

        if (!isValid) {
            isProcessing = false

            Toast.makeText(
                this,
                "Invalid QR code. No valid ZIC key found.",
                Toast.LENGTH_LONG
            ).show()

            if (!isFinishing && !isDestroyed) {
                barcodeView.resume()
            }
            return
        }

        val saved = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).edit()
            .putString(KEY_ZIC, key)
            .commit()

        if (saved) {
            isSaved = true

            Toast.makeText(
                this,
                "ZIC key imported successfully.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        } else {
            isProcessing = false

            Toast.makeText(
                this,
                "Could not save the imported key.",
                Toast.LENGTH_SHORT
            ).show()

            if (!isFinishing && !isDestroyed) {
                barcodeView.resume()
            }
        }
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

        if (::barcodeView.isInitialized &&
            !isProcessing &&
            !isSaved
        ) {
            barcodeView.resume()
        }
    }

    override fun onPause() {
        if (::barcodeView.isInitialized) {
            barcodeView.pause()
        }

        super.onPause()
    }
}