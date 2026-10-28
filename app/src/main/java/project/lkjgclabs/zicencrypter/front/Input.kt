package project.lkjgclabs.zicencrypter.front

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import project.lkjgclabs.zicencrypter.PINput
import project.lkjgclabs.zicencrypter.R
import project.lkjgclabs.zicencrypter.auth.AuthManager
import project.lkjgclabs.zicencrypter.keymanagement.EKey
import project.lkjgclabs.zicencrypter.keymanagement.IKey
import project.lkjgclabs.zicencrypter.keymanagement.NKey

class Input : AppCompatActivity() {

    private lateinit var textToEncode: TextInputEditText
    private lateinit var textInputLayout: TextInputLayout
    private lateinit var encodeButton: MaterialButton

    private var isMenuExpanded = false

    // Declare views
    private lateinit var manageKey: FloatingActionButton

    private lateinit var importKeyLayout: View
    private lateinit var generateKeyLayout: View
    private lateinit var exportKeyLayout: View

    private lateinit var importKey: FloatingActionButton
    private lateinit var generateKey: FloatingActionButton
    private lateinit var exportKey: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_input)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_input)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        manageKey = findViewById(R.id.manageKey)

        importKeyLayout = findViewById(R.id.importKeyLayout)
        generateKeyLayout = findViewById(R.id.generateKeyLayout)
        exportKeyLayout = findViewById(R.id.exportKeyLayout)

        importKey = findViewById(R.id.importKey)
        generateKey = findViewById(R.id.generateKey)
        exportKey = findViewById(R.id.exportKey)

        textToEncode = findViewById(R.id.textToEncode)
        encodeButton = findViewById(R.id.encodeButton)
        textInputLayout = findViewById(R.id.textInputLayout)

        // Set initial hidden states for radial menu options
        listOf(importKeyLayout, generateKeyLayout, exportKeyLayout).forEach {
            it.visibility = View.INVISIBLE
            it.alpha = 0f
            it.scaleX = 0.5f
            it.scaleY = 0.5f
        }

        manageKey.setOnClickListener {
            toggleKeyMenu()
        }

        importKey.setOnClickListener {
            startActivity(Intent(this@Input, IKey::class.java))
            collapseMenu()
        }

        generateKey.setOnClickListener {
            startActivity(Intent(this@Input, NKey::class.java))
            collapseMenu()
        }

        exportKey.setOnClickListener {
            startActivity(Intent(this@Input, EKey::class.java))
            collapseMenu()
        }

        encodeButton.setOnClickListener {
            sendInputToOutput()
        }
    }

    private fun sendInputToOutput() {
        val input = textToEncode.text?.toString().orEmpty()

        if (input.isBlank()) {
            textInputLayout.error = "Please enter your input"
            textInputLayout.requestFocus()
            return
        }

        textInputLayout.error = null

        val intent = Intent(this, Output::class.java).apply {
            putExtra("zic_textToEncode", input)
        }

        startActivity(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingText(intent)
    }

    private fun handleIncomingText(intent: Intent?) {
        val uri: Uri = intent?.data ?: return

        // Recommended format:
        // zic_encrypter://encrypt?text=hellow
        var text = uri.getQueryParameter("text")

        // Support your original format:
        // zic_encrypter://textToEncrypt:hellow
        if (text == null) {
            val authority = uri.encodedAuthority

            if (authority?.startsWith("textToEncrypt:") == true) {
                text = Uri.decode(
                    authority.removePrefix("textToEncrypt:")
                )
            }
        }

        if (text != null) {
            textToEncode.setText(text)
            textToEncode.setSelection(textToEncode.length())
        }
    }
    private fun toggleKeyMenu() {
        if (isMenuExpanded) {
            collapseMenu()
        } else {
            expandMenu()
        }
    }

    private fun expandMenu() {
        isMenuExpanded = true

        listOf(importKeyLayout, generateKeyLayout, exportKeyLayout).forEach {
            it.visibility = View.VISIBLE
        }

        // Animate North (Import Key) straight up
        importKeyLayout.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(-130f)
            .setDuration(250)
            .start()

        // Animate North-West (Generate Key) up and left
        generateKeyLayout.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationX(-100f)
            .translationY(-90f)
            .setDuration(250)
            .start()

        // Animate North-East (Export Key) up and right
        exportKeyLayout.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationX(100f)
            .translationY(-90f)
            .setDuration(250)
            .start()

        // Rotate main FAB icon
        manageKey.animate().rotation(45f).setDuration(200).start()
    }

    private fun collapseMenu() {
        isMenuExpanded = false

        importKeyLayout.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(200)
            .withEndAction { importKeyLayout.visibility = View.INVISIBLE }
            .start()

        generateKeyLayout.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(200)
            .withEndAction { generateKeyLayout.visibility = View.INVISIBLE }
            .start()

        exportKeyLayout.animate()
            .alpha(0f)
            .scaleX(0.5f)
            .scaleY(0.5f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(200)
            .withEndAction { exportKeyLayout.visibility = View.INVISIBLE }
            .start()

        // Reset main FAB icon rotation
        manageKey.animate().rotation(0f).setDuration(200).start()
    }
}