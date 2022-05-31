package com.romes.chatapp.activities


import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.romes.chatapp.databinding.ActivityImageBinding
import com.romes.chatapp.utils.Constants

class ImageActivity : AppCompatActivity() {
    private var binding: ActivityImageBinding? = null
    private var mChosenPhotoUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        if (intent.hasExtra(Constants.EXTRA_IMAGE)) {
            mChosenPhotoUri = Uri.parse(intent.getStringExtra(Constants.EXTRA_IMAGE))
        }
        if (mChosenPhotoUri.toString().isNotEmpty()) {
            Glide.with(this)
                .load(mChosenPhotoUri)
                .centerCrop()
                .into(binding?.ivChosenPhoto!!)
        }
    }

    override fun onBackPressed() {
        finish()
    }
}