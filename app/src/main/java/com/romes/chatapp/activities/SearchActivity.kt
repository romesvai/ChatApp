package com.romes.chatapp.activities

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.romes.chatapp.ChatApp
import com.romes.chatapp.R
import com.romes.chatapp.dao.ConversationDao
import com.romes.chatapp.databinding.ActivitySearchBinding
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.model.User
import com.romes.chatapp.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class SearchActivity : BaseActivity() {
    private var binding: ActivitySearchBinding? = null
    private var mUser: User? = null
    private var mAllUserNameList = ArrayList<String>()
    private var userCollectionListener: ListenerRegistration? = null
    private val usersCollectionRef = FirestoreClass().getUsersCollection()
    private var mCreatedConversation: Conversation? = null
    private var conversationDao : ConversationDao? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        showProgressDialog(getString(R.string.please_wait))
         conversationDao = (application as ChatApp).db.conversationDao()
        FirestoreClass().getUserList(this)
        if(intent.hasExtra(Constants.EXTRA_USER)){
            mUser = intent.getParcelableExtra(Constants.EXTRA_USER)!!
        }


        binding?.btnConnect?.setOnClickListener {
                val name = binding?.actSearchUser?.text.toString()
                FirestoreClass().getUserFromName(this, name)
                showProgressDialog(getString(R.string.please_wait))
            }
        subscribeToRealTimeUserUpdates()
    }

    override fun onStop() {
        super.onStop()
        userCollectionListener?.remove()
    }

    fun onGettingUserFromSearch(user: User) {
        val memberList = ArrayList<User>()
        memberList.add(user)
        mUser?.activeStatus = false
        mUser?.let { memberList.add(it) }
        mCreatedConversation =
            Conversation(memberList = memberList, subTitle = "Say Hi! to start chatting")
        FirestoreClass().createConversation(this, mCreatedConversation!!)
    }

    fun onConversationCreated(conversation: Conversation) {
        if(conversationDao!=null){
            saveConversationToRoomDb(conversationDao!!)
        }
        var result: String
        lateinit var user: User
        val memberList = conversation.memberList
        for (i in memberList) {
            if (i == mUser) {
                continue
            }
            user = i
        }
        Log.d("Token Check", user.fcmToken)
        lifecycleScope.launch(Dispatchers.IO) {
            runOnUiThread {
                showProgressDialog(getString(R.string.please_wait))
            }
            result =SendNotification(user.name, user.fcmToken).execute()
            Log.d("Result http", result)
            runOnUiThread {
                hideProgressDialog()
            }

        }
        Toast.makeText(this, "Successfully connected.", Toast.LENGTH_LONG).show()
         onBackPressed()
    }

    private fun saveConversationToRoomDb(conversationDao: ConversationDao) {
//        lifecycleScope.launch(Dispatchers.IO){
//            conversationDao.insertConversation(mCreatedConversation!!)
//        }
    }

    private fun subscribeToRealTimeUserUpdates() {
        userCollectionListener =
            usersCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    val allUserList = ArrayList<String>()
                    for (i in it.documents) {
                        val user = i.toObject<User>()
                        if (user != null) {
                            if (user.userId != FirestoreClass().getCurrentUserId()) {
                                allUserList.add(user.name)
                            }
                        }

                    }
                    if (allUserList.size > 0) {
                        mAllUserNameList = allUserList
                    }
                }
            }
    }

    fun onGettingUserList(userList: ArrayList<String>) {
        hideProgressDialog()
        mAllUserNameList = userList
        val adapterPass: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line, mAllUserNameList
        )
        binding?.actSearchUser?.setAdapter(adapterPass)

    }

    inner class SendNotification(private val userName: String, private val token: String) {


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
                    dataObject.put(Constants.FCM_KEY_TITLE, "New Connection")
                    dataObject.put(
                        Constants.FCM_KEY_MESSAGE,
                        "You are now connected with $userName"
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