package com.example.spark

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.spark.ui.screens.WelcomeScreen
import com.example.spark.ui.theme.SparkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SparkTheme {
                var email by rememberSaveable { mutableStateOf("") }
                var password by rememberSaveable { mutableStateOf("") }
                var passwordVisible by rememberSaveable { mutableStateOf(false) }

                WelcomeScreen(
                    email = email,
                    password = password,
                    passwordVisible = passwordVisible,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    onLoginClick = {
                        Toast.makeText(this, "Log In clicked for $email", Toast.LENGTH_SHORT).show()
                    },
                    onForgotPasswordClick = {
                        Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show()
                    },
                    onRegisterClick = {
                        Toast.makeText(this, "Register clicked", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}