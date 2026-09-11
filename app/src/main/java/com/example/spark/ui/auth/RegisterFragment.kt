package com.example.spark.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.databinding.FragmentRegisterBinding
import com.google.android.material.textfield.TextInputLayout

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    val binding get() = _binding!!

    /**
     * Configurable click listeners for testing and modular host binding.
     */
    var onRegisterClick: (() -> Unit)? = null
    var onLoginClick: (() -> Unit)? = null
    var onBackClick: (() -> Unit)? = null

    /**
     * Exposed error property placeholders wired to the respective TextInputLayouts.
     */
    var passwordError: String? = null
        set(value) {
            field = value
            _binding?.let { setError(it.tilPassword, value) }
        }

    var confirmPasswordError: String? = null
        set(value) {
            field = value
            _binding?.let { setError(it.tilConfirmPassword, value) }
        }

    var nameError: String? = null
        set(value) {
            field = value
            _binding?.let { setError(it.tilName, value) }
        }

    var emailError: String? = null
        set(value) {
            field = value
            _binding?.let { setError(it.tilEmail, value) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyPendingErrors()
        setupClickListeners()
        setupLoginSpannable()
    }

    /**
     * Shows error conditionally on the given TextInputLayout.
     */
    fun setError(layout: TextInputLayout, errorText: String?) {
        layout.error = errorText
        layout.isErrorEnabled = !errorText.isNullOrEmpty()
    }

    private fun applyPendingErrors() {
        passwordError?.let { setError(binding.tilPassword, it) }
        confirmPasswordError?.let { setError(binding.tilConfirmPassword, it) }
        nameError?.let { setError(binding.tilName, it) }
        emailError?.let { setError(binding.tilEmail, it) }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackClick?.invoke() ?: run {
                findNavController().navigateUp()
            }
        }

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                onRegisterClick?.invoke() ?: run {
                    findNavController().navigate(R.id.action_register_to_dashboard)
                }
            }
        }
    }

    /**
     * Client-side validation enforcing required fields and minimum 8-character password.
     */
    fun validateInputs(): Boolean {
        var isValid = true

        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        if (name.isEmpty()) {
            nameError = "Full Name is required"
            isValid = false
        } else {
            nameError = null
        }

        if (email.isEmpty()) {
            emailError = "Email Address is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            passwordError = "Minimum 8 characters"
            isValid = false
        } else {
            passwordError = null
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Please confirm your password"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        return isValid
    }

    /**
     * Sets up a ClickableSpan on "Log In" in the bottom prompt text.
     */
    private fun setupLoginSpannable() {
        val fullText = getString(R.string.register_prompt_login)
        val loginWord = getString(R.string.action_login)

        val spannable = SpannableString(fullText)
        val startIndex = fullText.indexOf(loginWord)

        if (startIndex != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onLoginClick?.invoke() ?: run {
                        findNavController().navigateUp()
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(requireContext(), R.color.primary)
                    ds.isUnderlineText = false
                    ds.isFakeBoldText = true
                }
            }

            spannable.setSpan(
                clickableSpan,
                startIndex,
                startIndex + loginWord.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.tvLoginPrompt.apply {
            text = spannable
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
