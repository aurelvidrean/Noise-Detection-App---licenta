package com.vidreanaurel.licenta

import android.content.Intent
import android.media.MediaPlayer.OnCompletionListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var fullName: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var phone: EditText
    private lateinit var registerButton: Button
    private lateinit var loginButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        fullName = findViewById(R.id.full_name_input)
        email = findViewById(R.id.email_input)
        password = findViewById(R.id.password_input)
        phone = findViewById(R.id.phone_input)
        registerButton = findViewById(R.id.register_button)
        loginButton = findViewById(R.id.already_have_account_textview)
        progressBar = findViewById(R.id.progress_bar)

        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth.currentUser != null) {
            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            finish()
        }

        registerButton.setOnClickListener {
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

            // register the user in firebase

            firebaseAuth.createUserWithEmailAndPassword(emailField, passwordField).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@RegisterActivity, "Account created", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                } else {
                    Toast.makeText(this@RegisterActivity, "Account not created ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }
}