package com.romes.chatapp.activities

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.romes.chatapp.R
import com.romes.chatapp.databinding.ActivityBaseBinding
import com.romes.chatapp.databinding.DialogPleaseWaitBinding

open class BaseActivity : AppCompatActivity() {
    private var binding: ActivityBaseBinding? = null
    private var doubleBackToExitPressedOnce = false
    private lateinit var mProgressDialog: Dialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding?.root)
    }

    fun doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(
            this,
            "Please press back once again to exit the application",
            Toast.LENGTH_SHORT
        ).show()
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)

    }

    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)
        val customBinding = DialogPleaseWaitBinding.inflate(layoutInflater)
        mProgressDialog.setContentView(customBinding.root)
        customBinding.tvText.text = text
        mProgressDialog.show()
    }

    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }
    fun showErrorSnackbar(message: String) {
        val snackbar =
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.snackbar_error_color))
        snackbar.show()


    }
}