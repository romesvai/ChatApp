package com.romes.chatapp.activities
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.navigation.NavigationView
import com.romes.chatapp.R
import com.romes.chatapp.adapters.ConversationItemAdapter
import com.romes.chatapp.databinding.*
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.model.User
import com.romes.chatapp.utils.Constants
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.messaging.FirebaseMessaging
import com.romes.chatapp.ChatApp
import com.romes.chatapp.dao.ConversationDao


class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var bindingMain: ActivityMainBinding? = null
    private var bindingAppBar: AppBarMainBinding? = null
    private var bindingNavHeader: NavHeaderMainBinding? = null
    private var bindingContentMain: MainContentBinding? = null
    private var mUser: User? = null
    private var mUnUpdatedUser: User? = null
    private val conversationCollectionRef = FirestoreClass().getConversationCollection()
    private lateinit var mSharedPreferences: SharedPreferences
    private var userProfileUpdated: Boolean = false
    private var userProfiledUpdatedToSendToConversation: Boolean = false
    private var conversationDao: ConversationDao? = null
    private lateinit var mUpdatedActiveUser: User
    private var backFromConversation: Boolean = false
    private var isSigningOut: Boolean = false
    private var conversationListener: ListenerRegistration? = null
    private var mConversationList = ArrayList<Conversation>()
    private var mFromSeach: Boolean = false
    private var mNormalExecution : Boolean = false
    private var mTokenUpdated : Boolean = false
    private val profileActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                userProfileUpdated = true


            } else {
                Log.e("Romes", "Cancelled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingMain = ActivityMainBinding.inflate(layoutInflater)
        bindingAppBar = bindingMain!!.appBarMain
        val view = bindingMain!!.navView.getHeaderView(0)
        bindingNavHeader = NavHeaderMainBinding.bind(view)
        bindingContentMain = bindingAppBar!!.mainContent
        setContentView(bindingMain?.root)
        setUpActionBar()
        if (intent.hasExtra(Constants.EXTRA_BACK_FROM_CONVERSATION)) {
            backFromConversation =
                intent.getBooleanExtra(Constants.EXTRA_BACK_FROM_CONVERSATION, false)
        }
        if(intent.hasExtra(Constants.EXTRA_FROM_SEARCH)){
            mFromSeach = intent.getBooleanExtra(Constants.EXTRA_FROM_SEARCH,false)
        }
        bindingMain?.navView?.setNavigationItemSelectedListener(this)
        bindingMain?.drawerLayout?.setScrimColor(Color.TRANSPARENT)

        mSharedPreferences = getSharedPreferences(Constants.PREF_TOKEN, Context.MODE_PRIVATE)
        conversationDao = (application as ChatApp).db.conversationDao()
       mTokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)
        showProgressDialog(getString(R.string.please_wait))
        FirestoreClass().getCurrentUser(this)


        subscribeToRealtimeUpdates()


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_search, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra(Constants.EXTRA_USER, mUser)
                mUnUpdatedUser = mUser
                profileActivityLauncher.launch(intent)
            }
            R.id.nav_sign_out -> {
                isSigningOut = true
                conversationListener?.remove()
                val userActiveHashMap = HashMap<String, Any>()
                userActiveHashMap[Constants.ACTIVE_STATUS] = false
                FirestoreClass().updateUserProfileData(this@MainActivity, userActiveHashMap,updatedActiveStatus = true,signingOut = true)
                }
            }
        bindingMain?.drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.activity_main_menu_search -> {
               val intent = Intent(this,SearchActivity::class.java)
                intent.putExtra(Constants.EXTRA_USER,mUser)
                   startActivity(intent)

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (bindingMain?.drawerLayout?.isDrawerOpen(GravityCompat.START)!!) {
            bindingMain?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    override fun onRestart() {
        super.onRestart()
        val userActiveHashMap = HashMap<String, Any>()
        userActiveHashMap[Constants.ACTIVE_STATUS] = true
        FirestoreClass().updateUserProfileData(this@MainActivity, userActiveHashMap, true)
    }


    override fun onStop() {
        super.onStop()
        if(!isSigningOut) {
            val userActiveHashMap = HashMap<String, Any>()
            userActiveHashMap[Constants.ACTIVE_STATUS] = false
            FirestoreClass().updateUserProfileData(
                this@MainActivity,
                userActiveHashMap,
                true
            )
        }
    }

    fun registerUser() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val name = account?.displayName
        val userId = FirestoreClass().getCurrentUserId()
        val image = account?.photoUrl
        val user = User(userId = userId, name = name!!, image = image.toString())
        FirestoreClass().registerUser(this, user)
    }

    fun onUserRegistered() {
        FirestoreClass().getCurrentUser(this)
    }

    fun onGetUserSuccess(user: User) {
        hideProgressDialog()
        mUser = user
        bindingNavHeader?.tvName?.text = user.name
        Glide
            .with(this)
            .load(user.image)
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .into(bindingNavHeader?.ivProfileImage!!)
        val userActiveHashMap = HashMap<String, Any>()
        userActiveHashMap[Constants.ACTIVE_STATUS] = true
        FirestoreClass().updateUserProfileData(this, userActiveHashMap, true)
    }

    fun updateConversationListToUI(conversationList: ArrayList<Conversation>) {
        mConversationList = conversationList
        if (conversationList.size > 0) {
//            if (userProfileUpdated) {
//                val updateConversationHashMap = HashMap<String, Any>()
//                for (i in conversationList) {
//                    val newMemberList = ArrayList<User>()
//                    val oldMemberList = i.memberList
//                    for (j in oldMemberList) {
//                        if (j.userId != mUser?.userId) {
//                            newMemberList.add(j)
//                        }
//                    }
//                    mUser?.let { newMemberList.add(it) }
//                    updateConversationHashMap[Constants.MEMBER_LIST] = newMemberList
//                    updateConversationHashMap[Constants.DOCUMENT_ID] = i.documentId
//                    val conversationDocumentId = i.documentId
//                    FirestoreClass().updateConversation(
//                        this,
//                        updateConversationHashMap,
//                        conversationDocumentId
//                    )
//                }
//                userProfileUpdated = false
//            }
            bindingContentMain?.tvNoConversation?.visibility = View.GONE
            bindingContentMain?.rvConversation?.visibility = View.VISIBLE
            bindingContentMain?.rvConversation?.layoutManager = LinearLayoutManager(this)
            bindingContentMain?.rvConversation?.setHasFixedSize(true)
            val adapter = ConversationItemAdapter(this, conversationList)
            bindingContentMain?.rvConversation?.adapter = adapter
            adapter.onClickListener(object : ConversationItemAdapter.OnClickListener {
                override fun onClick(position: Int, model: Conversation) {
                    val intent = Intent(this@MainActivity, ConversationActivity::class.java)
                    intent.putExtra(Constants.EXTRA_CONVERSATION_DOCUMENT_ID, model.documentId)
                    intent.putExtra(
                        Constants.EXTRA_PROFILE_UPDATE_FLAG,
                        userProfiledUpdatedToSendToConversation
                    )
                    userProfiledUpdatedToSendToConversation = false
                    startActivity(intent)
                    finish()
                }
            })
        } else {
            bindingContentMain?.rvConversation?.visibility = View.GONE
            bindingContentMain?.tvNoConversation?.visibility = View.VISIBLE
        }

    }

    private fun subscribeToRealtimeUpdates() {
        if (!isSigningOut) {
            conversationListener =
                conversationCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    firebaseFirestoreException?.let {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                        return@addSnapshotListener
                    }
                    querySnapshot?.let {
                        val conversationList = ArrayList<Conversation>()
                        for (i in it.documents) {
                            val conversation = i.toObject<Conversation>()
                            conversation?.documentId = i.id
                            if (conversation != null) {
                                for (j in conversation.memberList) {
                                    if (j.userId == mUser?.userId) {
                                        conversationList.add(conversation)
                                        break
                                    }
                                }
                            }

                        }
                        updateConversationListToUI(conversationList)
                    }
                }
        }
    }







    fun tokenUpdateSuccess() {
        val editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        mTokenUpdated = true
        FirestoreClass().getCurrentUser(this)
    }

    fun onUpdateActiveStatus() {
        FirestoreClass().getCurrentUser(this,updateActiveStatus = true,isSigningOut)
        if (backFromConversation) {
            mUser?.activeStatus = false
            backFromConversation = false

        }

        else{
            mNormalExecution = true
        }
    }

    fun onGetActiveStatusUpdatedUser(user: User,signingOut: Boolean= false) {
        mUpdatedActiveUser = user
        mUpdatedActiveUser.activeStatus= !mUpdatedActiveUser.activeStatus
        if(mNormalExecution){
            FirestoreClass().getConversationList(this,mUpdatedActiveUser,true,signingOut)
        }
        else {
            FirestoreClass().getConversationList(this, mUser!!, true, signingOut)
       }


    }

    fun onGettingActiveUpdatedConversationList(conversationList: ArrayList<Conversation>,isSigningOut: Boolean=false) {

        if(conversationList.size>0){
        for (i in conversationList) {
            val memberList = ArrayList<User>()
            for (j in i.memberList) {
                if (j.userId == mUser?.userId) {
                    continue
                }
                memberList.add(j)
            }
            memberList.add(mUpdatedActiveUser)
            val conversationHashMap = HashMap<String, Any>()
            conversationHashMap[Constants.MEMBER_LIST] = memberList
            mUpdatedActiveUser.activeStatus= !mUpdatedActiveUser.activeStatus
            mUser = mUpdatedActiveUser

            FirestoreClass().updateConversation(this, conversationHashMap, i.documentId,isSigningOut)
        }
        }
        else{
            FirestoreClass().signOutWithoutUpdate(this,isSigningOut)
            if(!mTokenUpdated){
                FirebaseMessaging.getInstance().token.addOnSuccessListener {
                    updateFCMToken(it)
                }
            }
        }

    }

    private fun setUpActionBar() {
        setSupportActionBar(bindingAppBar?.toolbarMainActivity)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        bindingAppBar?.toolbarMainActivity!!.setNavigationIcon(R.drawable.ic_menu)
        bindingAppBar?.toolbarMainActivity?.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    private fun toggleDrawer() {
        if (bindingMain?.drawerLayout?.isDrawerOpen(GravityCompat.START)!!) {
            bindingMain?.drawerLayout?.closeDrawer(GravityCompat.START)
        } else {
            bindingMain?.drawerLayout?.openDrawer(GravityCompat.START)
        }
    }
    private fun updateFCMToken(token: String) {
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        FirestoreClass().updateUserProfileData(this, userHashMap)

    }

    fun onSignOut() {
        onUpdateActiveStatus()

    }

    fun onConversationUpdatedSignOut() {
        val intent = Intent(this,SignInActivity::class.java)
        mSharedPreferences.edit().clear().apply()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Constants.EXTRA_SIGN_OUT,isSigningOut)
        startActivity(intent)
        finish()
    }

    fun checkIfUserProfileUpdated() {
//        if(userProfileUpdated){
//            FirestoreClass().getConversationList(this,mUnUpdatedUser!!)
//        }
        if(!mTokenUpdated){
            FirebaseMessaging.getInstance().token.addOnSuccessListener {
                updateFCMToken(it)
            }
        }
        if(userProfileUpdated){
            FirestoreClass().getUpdatedUser(this)
        }

    }

    fun onGetUpdatedUser(user: User) {
        bindingNavHeader?.tvName?.text = user.name
        Glide
            .with(this)
            .load(user.image)
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            .into(bindingNavHeader?.ivProfileImage!!)
        userProfileUpdated= false
    }


}

