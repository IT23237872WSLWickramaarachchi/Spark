package com.example.spark.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.spark.MainActivity
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.UserRepository
import com.example.spark.databinding.ActivityLoginBinding
import com.example.spark.util.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager

    companion object {
        const val EXTRA_PREFILLED_EMAIL = "extra_prefilled_email"
    }

    private val registerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val registeredEmail = result.data?.getStringExtra(EXTRA_PREFILLED_EMAIL)
            if (!registeredEmail.isNullOrBlank()) {
                binding.etEmail.setText(registeredEmail)
                binding.etPassword.requestFocus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        userRepository = UserRepository(db.userDao())
        sessionManager = SessionManager(this)

        setupPrefilledEmail()
        setupListeners()
    }

    private fun setupPrefilledEmail() {
        val prefilled = intent.getStringExtra(EXTRA_PREFILLED_EMAIL)
        if (!prefilled.isNullOrBlank()) {
            binding.etEmail.setText(prefilled)
            binding.etPassword.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.etEmail.doAfterTextChanged {
            binding.tilEmail.error = null
        }

        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvRegisterPrompt.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            registerLauncher.launch(intent)
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.register_error_email_empty)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.register_error_email_invalid)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.register_error_password_empty)
            isValid = false
        }

        if (!isValid) return

        binding.btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val result = userRepository.loginUser(email, password)
            withContext(Dispatchers.Main) {
                binding.btnLogin.isEnabled = true
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    sessionManager.saveSession(user)
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_success_toast, user.fullName),
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message
                        ?: getString(R.string.login_error_failed)
                    binding.tilPassword.error = errorMsg
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showForgotPasswordDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.login_forgot_password_title)
            .setMessage(R.string.login_forgot_password_msg)
            .setPositiveButton(R.string.action_done, null)
            .show()
    }
}
