package com.vidreanaurel.licenta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var createAccountButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email = findViewById(R.id.email_input_login)
        password = findViewById(R.id.password_input_login)
        loginButton = findViewById(R.id.login_button)
        createAccountButton = findViewById(R.id.create_account)
        progressBar = findViewById(R.id.progress_bar_login)
        firebaseAuth = FirebaseAuth.getInstance()

        createAccountButton.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            val emailField = email.text.toString().trim()
            val passwordField = password.text.toString().trim()

            if (TextUtils.isEmpty(emailField)) {
                email.error = "Email is required"
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(passwordField)) {
                password.error = "Password is required"
                return@setOnClickListener
            }

            if (passwordField.length < 4) {
                password.error = "Password needs to be at least 5 characters long"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            //authenticate the user

            firebaseAuth.signInWithEmailAndPassword(emailField, passwordField).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@LoginActivity, "Successfully logged in", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                } else {
                    Toast.makeText(this@LoginActivity, "Error ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    fun getUserEmail(): String {
        return email.text.toString().trim()
    }
}