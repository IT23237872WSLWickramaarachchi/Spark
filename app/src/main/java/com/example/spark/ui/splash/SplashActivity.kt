package com.example.spark.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.spark.MainActivity
import com.example.spark.databinding.ActivitySplashBinding
import com.example.spark.ui.auth.LoginActivity
import com.example.spark.util.SessionManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager
    private val handler = Handler(Looper.getMainLooper())
    private var autoRouteRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            // Already logged in: show branded splash briefly, then route to MainActivity
            autoRouteRunnable = Runnable {
                if (!isFinishing && !isDestroyed) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            handler.postDelayed(autoRouteRunnable!!, 1200L)
        } else {
            // Not logged in: Get Started button navigates to LoginActivity
            binding.cardGetStarted.setOnClickListener {
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoRouteRunnable?.let { handler.removeCallbacks(it) }
    }
}
