package com.example.spark.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.UserRepository
import com.example.spark.databinding.ActivityRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        userRepository = UserRepository(db.userDao())

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.tvLoginPrompt.setOnClickListener {
            finish()
        }

        binding.etName.doAfterTextChanged { binding.tilName.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.etConfirmPassword.doAfterTextChanged { binding.tilConfirmPassword.error = null }

        binding.btnRegister.setOnClickListener {
            attemptRegister()
        }
    }

    private fun attemptRegister() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.register_error_name_empty)
            isValid = false
        }

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
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.register_error_password_too_short)
            isValid = false
        }

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.register_error_password_mismatch)
            isValid = false
        }

        if (!isValid) return

        binding.btnRegister.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val result = userRepository.registerUser(name, email, password)
            withContext(Dispatchers.Main) {
                binding.btnRegister.isEnabled = true
                if (result.isSuccess) {
                    Toast.makeText(
                        this@RegisterActivity,
                        getString(R.string.register_success_toast),
                        Toast.LENGTH_SHORT
                    ).show()

                    val resultIntent = Intent().apply {
                        putExtra(LoginActivity.EXTRA_PREFILLED_EMAIL, email)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message
                        ?: "Registration failed"
                    binding.tilEmail.error = errorMsg
                    Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
