
package project.lkjgclabs.zicencrypter.front

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import project.lkjgclabs.zicencrypter.PINput
import project.lkjgclabs.zicencrypter.R
import project.lkjgclabs.zicencrypter.auth.AuthManager
import project.lkjgclabs.zicencrypter.keymanagement.IKey
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Output : AppCompatActivity() {

    private lateinit var encodedPassword: TextView
    private lateinit var backButton: MaterialButton
    private lateinit var copyButton: MaterialButton

    companion object {
        private const val PREFS_NAME = "zic_preferences"
        private const val KEY_NAME = "zic_key"
        private const val INPUT_EXTRA = "zic_textToEncode"
        private const val MAX_INPUT_LENGTH = 16
        private const val PASSWORD_LENGTH = 32
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_output)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_output)) {
                view, insets ->

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

        encodedPassword = findViewById(R.id.encodedPassword)
        backButton = findViewById(R.id.backButton)
        copyButton = findViewById(R.id.copyButton)

        backButton.setOnClickListener {
            finish()
        }

        copyButton.setOnClickListener {
            copyPassword()
        }

        val input = intent.getStringExtra(INPUT_EXTRA)

        if (input.isNullOrBlank()) {
            Toast.makeText(
                this,
                "No input received.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        if (input.length > MAX_INPUT_LENGTH) {
            Toast.makeText(
                this,
                "Maximum 16 characters allowed.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        generatePassword(input)
    }

    private fun generatePassword(input: String) {
        val prefs = getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )

        val savedKey = prefs.getString(KEY_NAME, null)

        Log.d("KEY", savedKey.toString())

        if (savedKey.isNullOrBlank()) {
            Toast.makeText(
                this,
                "No ZIC key found. Create a key first.",
                Toast.LENGTH_LONG
            ).show()

            startActivity(Intent(this, IKey::class.java))
            finish()
            return
        }

        val password = try {
            derivePassword(savedKey, input)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(
                this,
                "The saved ZIC key is invalid. Create it again.",
                Toast.LENGTH_LONG
            ).show()
            return
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not generate the password.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        encodedPassword.text = password
    }

    private fun derivePassword(
        savedKey: String,
        input: String
    ): String {
        require(
            savedKey.length == 128 &&
                    savedKey.all {
                        it in "0123456789abcdefABCDEF"
                    }
        ) {
            "Invalid key format"
        }

        val keyBytes = savedKey.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        val mac = Mac.getInstance("HmacSHA256")

        mac.init(
            SecretKeySpec(keyBytes, "HmacSHA256")
        )

        val inputBytes = input.toByteArray(Charsets.UTF_8)

        val message = ByteBuffer
            .allocate(4 + inputBytes.size)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(inputBytes.size)
            .put(inputBytes)
            .array()

        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val special = "!@#\$%^&*()-_=+[]{}?"
        val all = upper + lower + digits + special

        var counter = 0
        var randomBytes = ByteArray(0)
        var index = 0

        fun nextByte(): Int {
            if (index >= randomBytes.size) {
                val counterBytes = ByteBuffer
                    .allocate(4)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putInt(counter++)
                    .array()

                randomBytes = mac.doFinal(
                    message + counterBytes
                )

                index = 0
            }

            return randomBytes[index++].toInt() and 0xff
        }

        fun nextIndex(bound: Int): Int {
            require(bound in 1..256)

            val limit = 256 - (256 % bound)

            while (true) {
                val value = nextByte()

                if (value < limit) {
                    return value % bound
                }
            }
        }

        // Guarantee at least one character from each category.
        val result = mutableListOf(
            upper[nextIndex(upper.length)],
            lower[nextIndex(lower.length)],
            digits[nextIndex(digits.length)],
            special[nextIndex(special.length)]
        )

        repeat(28) {
            result.add(all[nextIndex(all.length)])
        }

        // Deterministic Fisher-Yates shuffle.
        for (i in result.lastIndex downTo 1) {
            val j = nextIndex(i + 1)

            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        return result.joinToString("")
    }

    private fun copyPassword() {
        val password = encodedPassword.text.toString()

        if (password.length != PASSWORD_LENGTH) {
            Toast.makeText(
                this,
                "No generated password available.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val clipboard = getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "ZIC Password",
                password
            )
        )

        Toast.makeText(
            this,
            "Password copied.",
            Toast.LENGTH_SHORT
        ).show()
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
