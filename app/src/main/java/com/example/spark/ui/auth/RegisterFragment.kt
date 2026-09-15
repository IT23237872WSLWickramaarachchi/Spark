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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.spark.R
import com.example.spark.databinding.FragmentRegisterBinding
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels { AuthViewModel.Factory() }

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
        observeAuthEvents()
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

    private fun clearErrors() {
        setError(binding.tilName, null)
        setError(binding.tilEmail, null)
        setError(binding.tilPassword, null)
        setError(binding.tilConfirmPassword, null)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackClick?.invoke() ?: run {
                findNavController().navigateUp()
            }
        }

        binding.btnRegister.setOnClickListener {
            onRegisterClick?.invoke() ?: run {
                clearErrors()
                val name = binding.etName.text?.toString().orEmpty()
                val email = binding.etEmail.text?.toString().orEmpty()
                val password = binding.etPassword.text?.toString().orEmpty()
                val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
                authViewModel.register(name, email, password, confirmPassword)
            }
        }
    }

    private fun observeAuthEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is AuthViewModel.AuthUiEvent.Success -> {
                            Toast.makeText(requireContext(), "Welcome to Spark!", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_register_to_dashboard)
                        }
                        is AuthViewModel.AuthUiEvent.NameError -> {
                            setError(binding.tilName, event.message)
                        }
                        is AuthViewModel.AuthUiEvent.EmailError -> {
                            setError(binding.tilEmail, event.message)
                        }
                        is AuthViewModel.AuthUiEvent.PasswordError -> {
                            setError(binding.tilPassword, event.message)
                        }
                        is AuthViewModel.AuthUiEvent.ConfirmPasswordError -> {
                            setError(binding.tilConfirmPassword, event.message)
                        }
                        is AuthViewModel.AuthUiEvent.GeneralError -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthViewModel.AuthUiEvent.Loading -> {
                            // Optional loading state handling
                        }
                    }
                }
            }
        }
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
