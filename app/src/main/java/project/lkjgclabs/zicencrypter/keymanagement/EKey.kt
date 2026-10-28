
package project.lkjgclabs.zicencrypter.keymanagement

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import project.lkjgclabs.zicencrypter.PINput
import project.lkjgclabs.zicencrypter.R
import project.lkjgclabs.zicencrypter.auth.AuthManager
import java.util.EnumMap

class EKey : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "zic_preferences"
        private const val KEY_NAME = "zic_key"
        private const val QR_SIZE = 800
        private const val QR_PREFIX = "zic_key:"
    }

    private lateinit var qrImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_ekey)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.barcodeMain)
        ) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        qrImage = findViewById(R.id.ivKeyBarcode)
        statusText = findViewById(R.id.tvBarcodeStatus)
        btnBack = findViewById(R.id.btnBarcodeBack)

        btnBack.setOnClickListener {
            finish()
        }

        loadKeyAndGenerateBarcode()
    }

    private fun loadKeyAndGenerateBarcode() {
        val key = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        ).getString(KEY_NAME, null)

        if (key.isNullOrBlank()) {
            showError("No saved ZIC key found.")
            return
        }

        if (!isValidKey(key)) {
            showError("The saved ZIC key is invalid.")
            return
        }

        try {
            val bitmap = createQrBitmap(
                content = "$QR_PREFIX$key",
                size = QR_SIZE
            )

            qrImage.setImageBitmap(bitmap)
            qrImage.visibility = View.VISIBLE
            statusText.text = "ZIC key QR code generated."

        } catch (e: Exception) {
            showError("Unable to generate QR code.")
        }
    }

    private fun isValidKey(key: String): Boolean {
        return key.length == 128 &&
                key.all {
                    it in "0123456789abcdefABCDEF"
                }
    }

    private fun showError(message: String) {
        qrImage.setImageDrawable(null)
        qrImage.visibility = View.GONE
        statusText.text = message
    }

    private fun createQrBitmap(
        content: String,
        size: Int
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(
            EncodeHintType::class.java
        ).apply {
            put(EncodeHintType.MARGIN, 4)
            put(
                EncodeHintType.ERROR_CORRECTION,
                ErrorCorrectionLevel.M
            )
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }

        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val pixels = IntArray(size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] =
                    if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        return Bitmap.createBitmap(
            pixels,
            size,
            size,
            Bitmap.Config.ARGB_8888
        )
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
