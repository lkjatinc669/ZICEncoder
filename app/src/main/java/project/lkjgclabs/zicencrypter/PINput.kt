package project.lkjgclabs.zicencrypter

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.CycleInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import project.lkjgclabs.zicencrypter.auth.AuthManager
import androidx.core.view.isVisible
import project.lkjgclabs.zicencrypter.front.Input

class PINput : AppCompatActivity() {

    private val passcodeBuilder = StringBuilder()
    private lateinit var dots: Array<ImageView>
    private lateinit var clearPass: ImageView
    private val handler = Handler(Looper.getMainLooper())
    private var isWaveDeleting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pinput)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dots = arrayOf(
            findViewById(R.id.pinic1),
            findViewById(R.id.pinic2),
            findViewById(R.id.pinic3),
            findViewById(R.id.pinic4),
            findViewById(R.id.pinic5),
            findViewById(R.id.pinic6),
            findViewById(R.id.pinic7),
            findViewById(R.id.pinic8)
        )

        clearPass = findViewById(R.id.clear_pass)

        setupKeypadListeners()
    }

    override fun onResume() {
        super.onResume()
        // If user returns and is not authenticated, clear any leftover inputs
        if (!AuthManager.isAuthenticated) {
            passcodeBuilder.clear()
            updatePasscodeUI(isTyping = false)
        }
    }

    private fun setupKeypadListeners() {
        val keypadGrid = findViewById<GridLayout>(R.id.keypadGrid)

        for (i in 0 until keypadGrid.childCount) {
            val child = keypadGrid.getChildAt(i)
            if (child is TextView && child.id != R.id.clear_pass) {
                child.setOnClickListener {
                    if (!isWaveDeleting) {
                        onDigitPressed(child.text.toString())
                    }
                }
            }
        }

        clearPass.setOnClickListener {
            if (!isWaveDeleting) {
                onDeletePressed()
            }
        }

        clearPass.setOnLongClickListener {
            if (!isWaveDeleting && passcodeBuilder.isNotEmpty()) {
                onLongDeleteWavePressed()
                true
            } else {
                false
            }
        }
    }

    private fun onDigitPressed(digit: String) {
        if (passcodeBuilder.length < 8) {
            passcodeBuilder.append(digit)
            updatePasscodeUI(isTyping = true)

            if (passcodeBuilder.length == 8) {
                verifyPasscode(passcodeBuilder.toString())
            }
        }
    }

    private fun onDeletePressed() {
        if (passcodeBuilder.isNotEmpty()) {
            val targetIndex = passcodeBuilder.length - 1
            passcodeBuilder.deleteCharAt(targetIndex)

            playDelAnimation(dots[targetIndex]) {
                updatePasscodeUI(isTyping = false)
            }
        }
    }

    private fun onLongDeleteWavePressed() {
        isWaveDeleting = true
        val startLength = passcodeBuilder.length

        for (i in (startLength - 1) downTo 0) {
            val delay = ((startLength - 1 - i) * 60).toLong()
            handler.postDelayed({
                if (passcodeBuilder.isNotEmpty()) {
                    passcodeBuilder.deleteCharAt(passcodeBuilder.length - 1)
                    playDelAnimation(dots[i]) {
                        updatePasscodeUI(isTyping = false)
                    }
                }
                if (i == 0) {
                    isWaveDeleting = false
                }
            }, delay)
        }
    }

    private fun updatePasscodeUI(isTyping: Boolean) {
        val currentLength = passcodeBuilder.length

        if (currentLength > 0 && clearPass.visibility != View.VISIBLE) {
            clearPass.alpha = 0f
            clearPass.visibility = View.VISIBLE
            clearPass.animate().alpha(1f).setDuration(150).start()
        } else if (currentLength == 0 && clearPass.isVisible) {
            clearPass.animate().alpha(0f).setDuration(150).withEndAction {
                clearPass.visibility = View.INVISIBLE
            }.start()
        }

        for (i in dots.indices) {
            if (i < currentLength) {
                dots[i].setImageResource(R.drawable.ic_passcode_filled)
                if (isTyping && i == currentLength - 1) {
                    playPopInAnimation(dots[i])
                }
            } else {
                dots[i].setImageResource(R.drawable.ic_passcode_empty)
            }
        }
    }

    private fun playPopInAnimation(view: View) {
        view.scaleX = 0.5f
        view.scaleY = 0.5f
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1.25f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1.25f, 1.0f)
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 220
            interpolator = OvershootInterpolator(2f)
            start()
        }
    }

    private fun playDelAnimation(view: View, onEnd: () -> Unit) {
        view.translationX = 0f
        val translationX = ObjectAnimator.ofFloat(view, "translationX", 0f, 15f, -15f, 0f)
        AnimatorSet().apply {
            playTogether(translationX)
            duration = 200
            start()
        }
        handler.postDelayed({
            onEnd()
        }, 100)
    }

    private fun shakePasscodeContainer() {
        val container = findViewById<View>(R.id.passcodeSection)
        val shake = ObjectAnimator.ofFloat(container, "translationX", 0f, 25f, -25f, 20f, -20f, 10f, -10f, 0f)
        shake.duration = 400
        shake.interpolator = CycleInterpolator(2f)
        shake.start()
    }

    private fun verifyPasscode(passcode: String) {
        val isCorrect = passcode == "12345678"

        if (isCorrect) {
            // 1. Set app state to authenticated
            AuthManager.isAuthenticated = true
            Toast.makeText(this, "Authentication Successful", Toast.LENGTH_SHORT).show()

            // 2. Navigate to your Main Activity (replace MainActivity::class.java with your actual main screen)
            val intent = Intent(this, Input::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } else {
            shakePasscodeContainer()
            passcodeBuilder.clear()
            updatePasscodeUI(isTyping = false)
        }
    }
}