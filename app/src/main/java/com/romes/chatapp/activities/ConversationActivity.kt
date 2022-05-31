package com.romes.chatapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.romes.chatapp.R
import com.romes.chatapp.adapters.MessageAdapter
import com.romes.chatapp.databinding.ActivityConversationBinding
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.model.Message
import com.romes.chatapp.model.User
import com.romes.chatapp.utils.Constants
import com.romes.chatapp.utils.GetAddressFromLatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ConversationActivity : BaseActivity() {
    private var binding: ActivityConversationBinding? = null
    private lateinit var mConversation: Conversation
    private lateinit var conversationRef: DocumentReference
    private lateinit var documentId: String
    private lateinit var mUser: User
    private var callFromOnStop: Boolean = false
    private var sendButtonPressed: Boolean = false
    private var conversationListener: ListenerRegistration? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationAddress: String = ""
    private var mLocationResult: com.romes.chatapp.model.Location? = null
    private var mLocationButtonPressed: Boolean = false
    private var mMessageImageDownLoadUri: String = ""
    private var mSelectedImageFileUri: Uri? = null
    private var mImageButtonClicked: Boolean = false
    private var mIsRecording:Boolean = false
    private var mediaRecorder: MediaRecorder?= null
    private var mFilePath: String=""
    private var mVoiceRecorded: Boolean = false
    private var mRecordFile: File? =null
    private var mRecordingDownloadUri: String = ""
    private var mMediaPlayer:MediaPlayer? = null
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
                        .into(binding?.ivMessageImageSelected!!)
                    binding?.ivMessageImageSelected?.visibility = View.VISIBLE
                    binding?.ibCancelSelectedImage?.visibility = View.VISIBLE
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        if (intent.hasExtra(Constants.EXTRA_CONVERSATION_DOCUMENT_ID)) {
            documentId = intent.getStringExtra(Constants.EXTRA_CONVERSATION_DOCUMENT_ID)!!
        }
//        if (intent.hasExtra(Constants.EXTRA_PROFILE_UPDATE_FLAG)) {
//            userProfileUpdated = intent.getBooleanExtra(Constants.EXTRA_PROFILE_UPDATE_FLAG, false)
//        }
        conversationRef = FirestoreClass().getConversation(documentId)
        subscribeToRealtimeUpdates()

        mFusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this@ConversationActivity)
        checkLocationPermissionAndStart()
        FirestoreClass().getConversationDetails(this@ConversationActivity, documentId)


        binding?.ibSend?.setOnClickListener {
            val messageBody = binding?.etMessage?.text?.toString()
            val timeStamp = System.currentTimeMillis()
            when {
                mLocationButtonPressed -> {
                    val message =
                        Message(
                            messageBody = messageBody!!,
                            timeStamp = timeStamp,
                            sender = mUser,
                            locationAddress = mLocationAddress,
                            locationLatitude = mLocationResult!!.locationLatitude,
                            locationLongitude = mLocationResult!!.locationLongitude
                        )
                    val messageList = ArrayList<Message>()
                    val oldMessageList = mConversation.messageList
                    for (i in oldMessageList) {
                        messageList.add(i)
                    }
                    messageList.add(message)
                    val conversationHashMap = HashMap<String, Any>()
                    conversationHashMap[Constants.MESSAGE_LIST] = messageList
                    val imm: InputMethodManager = getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding?.etMessage?.windowToken, 0)
                    binding?.etMessage?.text?.clear()
                    FirestoreClass().updateConversationMessage(
                        this,
                        conversationHashMap,
                        documentId
                    )
                    sendButtonPressed = true
                    mLocationButtonPressed = false
                }
                mImageButtonClicked -> {
                    uploadUserImages()
                }
                mVoiceRecorded -> {
                    binding?.pbVoiceLoading?.visibility= View.VISIBLE
                    val fileUri = FileProvider.getUriForFile(
                        baseContext,
                        Constants.AUTHORITY,
                        mRecordFile!!
                    )
                    fileUri?.let {
                        val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                            "Recording" + System.currentTimeMillis() + "." + Constants.getExtension(
                                this,
                                it
                            )
                        )
                        sRef.putFile(it).addOnSuccessListener { taskSnapshot ->
                            Log.d(
                                "Firebase Recording URL ",
                                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                            )
                            taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                                Log.d("Downloadable Image Uri", uri.toString())
                                mRecordingDownloadUri = uri.toString()
                                val message =
                                    Message(
                                        messageBody = messageBody!!,
                                        timeStamp = timeStamp,
                                        sender = mUser,
                                        recording = mRecordingDownloadUri
                                    )
                                val messageList = ArrayList<Message>()
                                val oldMessageList = mConversation.messageList
                                for (i in oldMessageList) {
                                    messageList.add(i)
                                }
                                messageList.add(message)
                                val conversationHashMap = HashMap<String, Any>()
                                conversationHashMap[Constants.MESSAGE_LIST] = messageList
                                val imm: InputMethodManager = getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                                ) as InputMethodManager
                                imm.hideSoftInputFromWindow(binding?.etMessage?.windowToken, 0)
                                binding?.etMessage?.text?.clear()
                                FirestoreClass().updateConversationMessage(
                                    this,
                                    conversationHashMap,
                                    documentId
                                )
                                binding?.pbVoiceLoading?.visibility = View.GONE
                                binding?.cvRecorder?.visibility = View.GONE
                                mVoiceRecorded = false
                                sendButtonPressed = true

                            }

                        }.addOnFailureListener { e ->
                            Log.d("Error rom", e.message.toString())
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            hideProgressDialog()
                        }

                    }


                }
                else -> {
                    val message =
                        Message(messageBody = messageBody!!, timeStamp = timeStamp, sender = mUser)
                    val messageList = ArrayList<Message>()
                    val oldMessageList = mConversation.messageList
                    for (i in oldMessageList) {
                        messageList.add(i)
                    }
                    messageList.add(message)
                    val conversationHashMap = HashMap<String, Any>()
                    conversationHashMap[Constants.MESSAGE_LIST] = messageList
                    val imm: InputMethodManager = getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding?.etMessage?.windowToken, 0)
                    binding?.etMessage?.text?.clear()
                    FirestoreClass().updateConversationMessage(
                        this,
                        conversationHashMap,
                        documentId
                    )
                    sendButtonPressed = true
                }
            }
        }
        binding?.ibLocation?.setOnClickListener {
            for (i in mConversation.memberList) {
                if (i.userId == mUser.userId) {
                    mLocationButtonPressed = true
                    binding?.etMessage?.setText(i.locationAddress)
                }
            }
        }
        binding?.ibImage?.setOnClickListener {
            mImageButtonClicked = true
            Constants.choosePhotoFromGallery(this, openGalleryLauncher)
        }
        binding?.ibCancelSelectedImage?.setOnClickListener {
            mSelectedImageFileUri = null
            mImageButtonClicked = false
            binding?.ibCancelSelectedImage?.visibility = View.GONE
            binding?.ivMessageImageSelected?.visibility = View.GONE


        }
        binding?.ibBackButton?.setOnClickListener {
            onBackPressed()
        }
        binding?.ibVoice?.setOnClickListener {
            //Start Recording
            mIsRecording = true
                binding?.cvRecorder?.visibility = View.VISIBLE
            binding?.ibSend?.isEnabled = false
            binding?.ibVoice?.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_voice_active,
                    null
                )
            )
                checkVoicePermissions()

            }

        binding?.ibCancelSelectedVoice?.setOnClickListener {
            val fileToDeleted = File(mFilePath)
            fileToDeleted.delete()
            mVoiceRecorded = false
            mMediaPlayer?.apply {
                this.stop()
            }
            binding?.cvRecorder?.visibility = View.GONE
            binding?.ibVoice?.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_voice,
                    null
                )
            )
            binding?.ibStopRecordButton?.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.ic_stop,null))
            binding?.chronometerRecordTimer?.base = SystemClock.elapsedRealtime()
        }
        binding?.ibStopRecordButton?.setOnClickListener {
            if (mVoiceRecorded) {
                if (mMediaPlayer?.isPlaying == true) {
                    binding?.ibStopRecordButton?.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.ic_play_button, null
                        )
                    )
                    mMediaPlayer?.stop()
                    mMediaPlayer?.reset()
                }
                else {
                   binding?.ibStopRecordButton?.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.ic_stop,null))
                    mMediaPlayer = MediaPlayer()
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    mMediaPlayer!!.setAudioAttributes(audioAttributes)
                    try {
                        mMediaPlayer!!.setDataSource(mFilePath)
                        // below line is use to prepare
                        // and start our media player.
                        mMediaPlayer!!.prepare()
                        mMediaPlayer!!.start()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                mMediaPlayer?.setOnCompletionListener {
                    binding?.ibStopRecordButton?.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.ic_play_button,null))
                }
            }
            if (mIsRecording) {
                //Stop Recording
                stopRecording()
                binding?.ibSend?.isEnabled = true
                binding?.ibVoice?.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_voice,
                        null
                    )
                )
                binding?.ibStopRecordButton?.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.ic_play_button,null))
            }

        }
    }



    private fun checkVoicePermissions(){
        Dexter.withContext(this).withPermissions(
            Manifest.permission.RECORD_AUDIO
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                  startRecording()
                }

            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {
                Constants.showRationaleDialogForPermission(this@ConversationActivity)
            }
        }).onSameThread().check()

    }

    override fun onRestart() {
        super.onRestart()
        subscribeToRealtimeUpdates()
        val memberList = ArrayList<User>()
        for (i in mConversation.memberList) {

            if (i.userId == FirestoreClass().getCurrentUserId()) {
                mUser.activeStatus = true
                memberList.add(mUser)
                continue
            }
            memberList.add(i)
        }
        val conversationHashMap = HashMap<String, Any>()
        conversationHashMap[Constants.MEMBER_LIST] = memberList
        FirestoreClass().updateConversationMessage(this, conversationHashMap, documentId)

    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        conversationListener?.remove()
        intent.putExtra(Constants.EXTRA_BACK_FROM_CONVERSATION, true)
        startActivity(intent)
        finish()
    }

    override fun onStop() {
        super.onStop()
        val memberList = ArrayList<User>()
        for (i in mConversation.memberList) {
            if (i.userId == mUser.userId) {
                val user = mUser
                user.activeStatus = false
                memberList.add(user)
                continue
            }
            memberList.add(i)
        }
        val conversationHashMap = HashMap<String, Any>()
        conversationHashMap[Constants.MEMBER_LIST] = memberList
        FirestoreClass().updateConversationMessage(this, conversationHashMap, documentId)
        callFromOnStop = true

    }

    private fun subscribeToRealtimeUpdates() {
        conversationListener =
            conversationRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    mConversation = it.toObject(Conversation::class.java)!!
                    for (i in mConversation.memberList) {
                        if (i.userId == FirestoreClass().getCurrentUserId()) {
                            i.activeStatus = true
                            mUser = i
                        }
                    }
                    for(i in mConversation.memberList){
                        if(i.userId == FirestoreClass().getCurrentUserId()){
                            continue
                        }

                    }
//                    if (userProfileUpdated) {
//                        val messageList = ArrayList<Message>()
//                        lateinit var sender: User
//                        for (j in mConversation.messageList) {
//                            val messageBody = j.messageBody
//                            val recording = j.recording
//                            val image = j.image
//                            val locationAddress = j.locationAddress
//                            val locationLatitude = j.locationLatitude
//                            val locationLongitude = j.locationLongitude
//                            sender = if (j.sender?.userId == mUser.userId) {
//                                mUser
//                            } else {
//                                j.sender!!
//                            }
//                            val timeStamp = j.timeStamp
//                            val updatedMessage = Message(messageBody, timeStamp, sender,locationLatitude,locationLongitude,locationAddress,image,recording)
//                            messageList.add(updatedMessage)
//                        }
//                        val conversationHashMap = HashMap<String, Any>()
//                        conversationHashMap[Constants.MESSAGE_LIST] = messageList
//                        FirestoreClass().updateConversationMessage(
//                            this,
//                            conversationHashMap,
//                            documentId
//                        )
//                    }
                    val memberList = ArrayList<User>()
                    if (!callFromOnStop) {
                        for (i in mConversation.memberList) {

                            if (i.userId == FirestoreClass().getCurrentUserId()) {
                                mUser.activeStatus = true
                                memberList.add(mUser)
                                continue
                            }
                            memberList.add(i)
                        }
                        val conversationHashMap = HashMap<String, Any>()
                        conversationHashMap[Constants.MEMBER_LIST] = memberList
                        FirestoreClass().updateConversationMessage(
                            this@ConversationActivity,
                            conversationHashMap,
                            documentId
                        )
                    }
                    for (i in mConversation.memberList) {
                        if (i.userId == mUser.userId) {
                            continue
                        }
                        if (i.activeStatus) {
                            binding?.ivActive?.visibility = View.VISIBLE
                        } else {
                            binding?.ivActive?.visibility = View.GONE
                        }
                    }

                    onMessageSent()
                }
            }
    }
    private fun uploadUserImages() {
        binding?.pbImageUploading?.visibility = View.VISIBLE
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
                    val messageBody = binding?.etMessage?.text?.toString()
                    val timeStamp = System.currentTimeMillis()
                    mMessageImageDownLoadUri = uri.toString()
                    val message =
                        Message(messageBody = messageBody!!, timeStamp = timeStamp, sender = mUser,image = mMessageImageDownLoadUri)
                    val messageList = ArrayList<Message>()
                    val oldMessageList = mConversation.messageList
                    for (i in oldMessageList) {
                        messageList.add(i)
                    }
                    messageList.add(message)
                    val conversationHashMap = HashMap<String, Any>()
                    conversationHashMap[Constants.MESSAGE_LIST] = messageList
                    val imm: InputMethodManager = getSystemService(
                        Context.INPUT_METHOD_SERVICE
                    ) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding?.etMessage?.windowToken, 0)
                    binding?.etMessage?.text?.clear()
                    FirestoreClass().updateConversationMessage(this, conversationHashMap, documentId)
                    sendButtonPressed = true
                    binding?.pbImageUploading?.visibility = View.GONE
                    binding?.ibCancelSelectedImage?.visibility = View.GONE
                    binding?.ivMessageImageSelected?.visibility = View.GONE
                    mImageButtonClicked = false
                    mSelectedImageFileUri = null
                }

            }.addOnFailureListener { e ->
                Log.d("Error rom", e.message.toString())
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                hideProgressDialog()
            }

        }

    }

    private fun checkLocationPermissionAndStart() {
        if (Constants.isNetworkAvailable(this)) {
            if (!isLocationEnabled()) {
                Toast.makeText(
                    this@ConversationActivity,
                    "Your location provider is turned off. Please turn it on.",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            } else {
                Dexter.withContext(this).withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                    @SuppressLint("MissingPermission")
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {

                            mFusedLocationClient.lastLocation.addOnSuccessListener {
                                if (it == null) {
                                    requestNewLocationData()
                                } else {
                                    val addressTask = GetAddressFromLatLng(this@ConversationActivity, it.latitude, it.longitude)
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val job = lifecycleScope.launch(Dispatchers.IO) {
                                            mLocationAddress = addressTask.doInBackground()
                                        }
                                        job.join()
                                        if(it.latitude!=0.0 && it.longitude!=0.0) {
                                            mLocationResult = com.romes.chatapp.model.Location(
                                                mLocationAddress,
                                                it.latitude,
                                                it.longitude
                                            )
                                            val userHashMap = HashMap<String, Any>()
                                            userHashMap[Constants.LOCATION_ADDRESS] =
                                                mLocationAddress
                                            userHashMap[Constants.LOCATION_LATITUDE] = it.latitude
                                            userHashMap[Constants.LOCATION_LONGITUDE] = it.longitude
                                            FirestoreClass().updateUserProfileData(
                                                this@ConversationActivity,
                                                userHashMap
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRationaleDialogForPermission()
                    }
                }).onSameThread().check()

            }
        } else {
            Toast.makeText(this, "Connection Not Available", Toast.LENGTH_SHORT).show()
        }

    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun showRationaleDialogForPermission() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you turned off permissions required for this feature it can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)

                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val myLocationRequest = LocationRequest.create()
        myLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        myLocationRequest.interval = 0
        myLocationRequest.fastestInterval = 0
        myLocationRequest.numUpdates = 1
        mFusedLocationClient.requestLocationUpdates(
            myLocationRequest,
            mLocationCallback,
            Looper.myLooper()!!
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            val longitude = mLastLocation.longitude
            Log.d("Romes Checking", "Current latitle : $latitude")
            Log.d("Romes Checking", "Current longitudee : $longitude")
            val addressTask = GetAddressFromLatLng(this@ConversationActivity, latitude, longitude)
            lifecycleScope.launch(Dispatchers.IO) {
                val job = lifecycleScope.launch(Dispatchers.IO) {
                    mLocationAddress = addressTask.doInBackground()
                }
                job.join()
                mLocationResult = com.romes.chatapp.model.Location(mLocationAddress,latitude,longitude)
                val userHashMap = HashMap<String, Any>()
                userHashMap[Constants.LOCATION_ADDRESS] = mLocationAddress
                userHashMap[Constants.LOCATION_LATITUDE] = latitude
                userHashMap[Constants.LOCATION_LONGITUDE] = longitude
                FirestoreClass().updateUserProfileData(this@ConversationActivity, userHashMap)

            }
        }
    }
    private fun startRecording() {
        binding?.chronometerRecordTimer?.base = SystemClock.elapsedRealtime()
        binding?.chronometerRecordTimer?.start()


        mFilePath = externalCacheDir?.absoluteFile.toString() + File.separator + "Chat App" + System.currentTimeMillis() / 1000 + ".m4a"
        mRecordFile = File(mFilePath)
        mediaRecorder = MediaRecorder().apply{
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(mFilePath)
            setAudioEncodingBitRate(384000)
            setAudioSamplingRate(48000)

            setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("checking", "prepare() failed")
            }
            start()}

    }

    private fun stopRecording() {
        binding?.chronometerRecordTimer?.stop()
        mediaRecorder?.apply {
            stop()
            reset()
            release()}
        binding?.ibStopRecordButton?.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.ic_play_button,null))
        mVoiceRecorded = true


        mediaRecorder = null



    }


    fun onGettingConversationDetails(conversation: Conversation) {
        mConversation = conversation
        for (i in conversation.memberList) {
            if (i == mUser) {
                continue
            }
            binding?.tvConversationTitle?.text = i.name
            Glide
                .with(this)
                .load(i.image)
                .circleCrop()
                .into(binding?.ivProfileImage!!)

        }

    }

    fun onMessageSent() {
        var result: String
        lateinit var user: User
        for (i in mConversation.memberList) {
            if (i.userId == mUser.userId) {
                continue
            }
            user = i
        }
        if (sendButtonPressed) {
            lifecycleScope.launch(Dispatchers.IO) {
                result = SendNotification(
                    mUser.name,
                    "New Message",
                    user.fcmToken
                ).execute()
                Log.d("Result http", result)

            }
            sendButtonPressed = false
        }
        if (mConversation.messageList.size > 0) {
            binding?.tvSayHi?.visibility = View.GONE
            binding?.rvMessage?.visibility = View.VISIBLE
            binding?.rvMessage?.layoutManager = LinearLayoutManager(this)
            binding?.rvMessage?.setHasFixedSize(true)
            if(mLocationResult!=null) {
                val adapter = MessageAdapter(this, mConversation.messageList, mLocationResult!!)
                binding?.rvMessage?.adapter = adapter
                binding?.rvMessage?.adapter?.itemCount?.minus(
                    1
                )?.let { binding?.rvMessage?.scrollToPosition(it) }
            }
        } else {
            binding?.rvMessage?.visibility = View.GONE
            binding?.tvSayHi?.visibility = View.VISIBLE
        }
    }

    fun onUserUpdatedFromConversation() {
        FirestoreClass().getCurrentUser(this)
    }

    fun onGetUserOnConversationActivity(user: User?) {
        val memberList = ArrayList<User>()
        mUser = user!!
        for (i in mConversation.memberList) {
            if (i.userId == mUser.userId) {
                continue
            }
            memberList.add(i)
        }
        memberList.add(user)
        val conversationHashMap = HashMap<String, Any>()
        conversationHashMap[Constants.MEMBER_LIST] = memberList
        FirestoreClass().updateConversationMessage(this, conversationHashMap, documentId)

    }

    inner class SendNotification(
        private val userName: String,
        private val messagePreview: String,
        private val token: String
    ) {


        suspend fun execute(): String {
            lateinit var result: String
            val job = lifecycleScope.launch(Dispatchers.IO) {
                var connection: HttpURLConnection? = null
                try {
                    val url = URL(Constants.FCM_BASE_URL)
                    connection = url.openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.doOutput = true

                    connection.instanceFollowRedirects = false
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("charset", "utf-8")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty(
                        Constants.FCM_AUTHORIZATION,
                        "${Constants.FCM_KEY} = ${Constants.FCM_SERVER_KEY}"
                    )
                    connection.useCaches = false


                    val dataOutputStream = DataOutputStream(connection.outputStream)
                    val jsonRequest = JSONObject()
                    val dataObject = JSONObject()
                    dataObject.put(Constants.FCM_KEY_TITLE, userName)
                    dataObject.put(
                        Constants.FCM_KEY_MESSAGE,
                        messagePreview
                    )

                    jsonRequest.put(Constants.FCM_KEY_NOTIFICATION, dataObject)
                    jsonRequest.put(Constants.FCM_KEY_TO, token)

                    dataOutputStream.writeBytes(jsonRequest.toString())
                    dataOutputStream.flush()
                    dataOutputStream.close()


                    val httpResult: Int = connection.responseCode
                    if (httpResult == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val stringBuilder = StringBuilder()
                        var line: String?
                        try {
                            while (reader.readLine().also {
                                    line = it
                                } != null)
                                stringBuilder.append(line + "\n")
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } finally {
                            try {
                                inputStream.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                        result = stringBuilder.toString()


                    } else {
                        result = connection.responseMessage
                    }


                } catch (e: SocketTimeoutException) {
                    result = "Connection Timeout"
                } finally {
                    connection?.disconnect()
                }


            }
            job.join()
            return result


        }


    }


}