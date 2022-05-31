package com.romes.chatapp.activities


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.romes.chatapp.R
import com.romes.chatapp.databinding.ActivityProfileBinding
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.model.Message
import com.romes.chatapp.model.User
import com.romes.chatapp.utils.Constants
import java.io.IOException

class ProfileActivity : BaseActivity() {
    private var binding: ActivityProfileBinding? = null
    private var mUser: User? = null
    private var mProfileImageDownLoadUri: String = ""
    private var mSelectedImageFileUri: Uri? = null
    private var mUpdatedProfileUser: User? = null
    private var mConversationListSize: Int = 0
    private var mCheckConversationSize: Int = 0
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                try {
                    mSelectedImageFileUri = result.data?.data
                    Glide
                        .with(this)
                        .load(mSelectedImageFileUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(binding?.ivProfileImage!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpActionBar()
        if(intent.hasExtra(Constants.EXTRA_USER)){
            mUser = intent.getParcelableExtra(Constants.EXTRA_USER)
        }
        updateUI()
        binding?.btnUpdate?.setOnClickListener {
            if (mSelectedImageFileUri != null) {
                uploadUserImages()
            } else {
                updateUserProfileData()
            }
        }
        binding?.ivProfileImage?.setOnClickListener {
            Constants.choosePhotoFromGallery(this, openGalleryLauncher)
        }
    }
    fun profileUpdateSuccess() {
        hideProgressDialog()
        FirestoreClass().getCurrentUser(this)

    }


    private fun updateUI() {
        binding?.etUsername?.setText(mUser?.name)
        Glide
            .with(this)
            .load(mUser?.image)
            .circleCrop()
            .into(binding?.ivProfileImage!!)

    }
    private fun updateUserProfileData() {
        showProgressDialog(getString(R.string.please_wait))
        val userHashMap = HashMap<String, Any>()
        var anyChangesMade = false
        if (mProfileImageDownLoadUri.isNotEmpty() && mProfileImageDownLoadUri != mUser!!.image) {
            userHashMap[Constants.IMAGE] = mProfileImageDownLoadUri
            anyChangesMade = true
        }
        if (binding?.etUsername?.text.toString()
                .isNotEmpty() && binding?.etUsername?.text.toString() != mUser!!.name
        ) {
            userHashMap[Constants.NAME] = binding?.etUsername?.text.toString()
            anyChangesMade = true
        }
        if (anyChangesMade) {
            FirestoreClass().updateUserProfileData(this, userHashMap)
        } else {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show()
        }
    }
    private fun uploadUserImages() {
        showProgressDialog(getString(R.string.please_wait))
        mSelectedImageFileUri?.let {
            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." + Constants.getExtension(this, it)
            )
            sRef.putFile(it).addOnSuccessListener { taskSnapshot ->
                Log.d(
                    "Firebase Image URL ",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("Downloadable Image Uri", uri.toString())
                    mProfileImageDownLoadUri = uri.toString()
                    hideProgressDialog()
                    updateUserProfileData()

                }

            }.addOnFailureListener { e ->
                Log.d("Error rom", e.message.toString())
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }

        }

    }
    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarProfile)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        }
        binding?.toolbarProfile?.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    fun onGettingConversationListOnProfile(conversationList: ArrayList<Conversation>) {
        mConversationListSize = conversationList.size
        if(conversationList.size>0){
            for (i in conversationList) {
                val memberList = ArrayList<User>()
                for (j in i.memberList) {
                    if (j.userId == mUser?.userId) {
                        memberList.add(mUpdatedProfileUser!!)
                    }
                    else {
                        memberList.add(j)
                    }
                }
                val conversationHashMap = HashMap<String, Any>()
                conversationHashMap[Constants.MEMBER_LIST] = memberList
                FirestoreClass().updateConversation(this, conversationHashMap, i.documentId,false)
            }

            for(mConversation in conversationList) {
                lateinit var sender: User
                val messageList = ArrayList<Message>()
                for (j in mConversation.messageList) {
                    val messageBody = j.messageBody
                    val recording = j.recording
                    val image = j.image
                    val locationAddress = j.locationAddress
                    val locationLatitude = j.locationLatitude
                    val locationLongitude = j.locationLongitude
                    sender = if (j.sender?.userId == mUser?.userId) {
                        mUpdatedProfileUser!!
                    } else {
                        j.sender!!
                    }
                    val timeStamp = j.timeStamp
                    val updatedMessage = Message(
                        messageBody,
                        timeStamp,
                        sender,
                        locationLatitude,
                        locationLongitude,
                        locationAddress,
                        image,
                        recording
                    )
                    messageList.add(updatedMessage)
                }
                val conversationHashMap = HashMap<String, Any>()
                conversationHashMap[Constants.MESSAGE_LIST] = messageList
                FirestoreClass().updateConversationMessage(
                    this,
                    conversationHashMap,
                    mConversation.documentId
                )
            }
        }
    }

    fun onGettingProfileUpdatedUser(user: User?) {
        mUpdatedProfileUser = user
        mUser?.activeStatus = false
        FirestoreClass().getConversationList(this,mUser!!)

    }

    fun onConversationUpdatedOnProfile() {
        mCheckConversationSize++
        if(mCheckConversationSize == mConversationListSize) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

}