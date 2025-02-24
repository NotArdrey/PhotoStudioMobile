package com.example.photostudio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val splashLayout = findViewById<LinearLayout>(R.id.splashLayout) // Get root layout

        Handler(Looper.getMainLooper()).postDelayed({
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            splashLayout.startAnimation(fadeOut) // Apply animation

            fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    startActivity(Intent(this@SplashScreenActivity, LandingPage::class.java))
                    overridePendingTransition(0, 0) // Prevents flicker
                    finish() // Close splash screen
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
        }, 2000) // Show splash for 2 sec before fade-out
    }
}
