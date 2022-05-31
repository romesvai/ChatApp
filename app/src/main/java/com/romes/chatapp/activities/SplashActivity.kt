package com.romes.chatapp.activities

import android.content.Intent
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            //Not signed in, launch the Sign in Activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}