package com.example.nammasantheledger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        setContentView(R.layout.activity_login)

        val pinInput        = findViewById<EditText>(R.id.pinInput)
        val confirmPinInput = findViewById<EditText>(R.id.confirmPinInput)
        val loginBtn        = findViewById<Button>(R.id.loginButton)
        val titleText       = findViewById<TextView>(R.id.loginStatusText)
        val subTitle        = findViewById<TextView>(R.id.loginSubtitle)

        val hasPin = prefs.getString("pin", null) != null

        if (!hasPin) {
            titleText.text = "Set Up Your PIN"
            subTitle.text  = "Create a 4-digit PIN to protect your data"
            loginBtn.text  = "Create PIN"
            confirmPinInput.visibility = View.VISIBLE
        } else {
            titleText.text = "Welcome Back!"
            subTitle.text  = "Enter your PIN to continue"
            loginBtn.text  = "Login"
            confirmPinInput.visibility = View.GONE
        }

        loginBtn.setOnClickListener {
            val pin = pinInput.text.toString().trim()

            if (pin.length != 4) {
                Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!hasPin) {
                val confirmPin = confirmPinInput.text.toString().trim()
                if (pin != confirmPin) {
                    Toast.makeText(this, "PINs do not match!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                prefs.edit().putString("pin", pin).apply()
                Toast.makeText(this, "✅ PIN set successfully!", Toast.LENGTH_SHORT).show()
            } else {
                val savedPin = prefs.getString("pin", "")
                if (pin != savedPin) {
                    Toast.makeText(this, "❌ Incorrect PIN", Toast.LENGTH_SHORT).show()
                    pinInput.setText("")
                    return@setOnClickListener
                }
            }

            // Save login state and enter app
            prefs.edit().putBoolean("isLoggedIn", true).apply()
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}