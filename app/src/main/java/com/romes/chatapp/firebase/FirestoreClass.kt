package com.romes.chatapp.firebase

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.romes.chatapp.activities.ConversationActivity
import com.romes.chatapp.activities.MainActivity
import com.romes.chatapp.activities.ProfileActivity
import com.romes.chatapp.activities.SearchActivity
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.model.User
import com.romes.chatapp.utils.Constants


class FirestoreClass {
    private val mFireStore = FirebaseFirestore.getInstance()


    fun getConversationCollection(): CollectionReference{
        return mFireStore.collection(Constants.CONVERSATION)
    }

    fun getUsersCollection(): CollectionReference{
        return mFireStore.collection(Constants.USERS)
    }

    fun getConversation(documentID: String): DocumentReference {
            return mFireStore.collection(Constants.CONVERSATION).document(documentID)
    }

    fun getCurrentUserId():String{
        var currentUserId =""
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            currentUserId = it.uid
        }
        return currentUserId
    }

    fun registerUser(activity: MainActivity, userInfo:User){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.onUserRegistered()
            }
            .addOnFailureListener {
                Log.e(activity.javaClass.simpleName, "Error writing into the database")
            }
    }

    fun getCurrentUser(activity:Activity,updateActiveStatus: Boolean = false,isSigningOut: Boolean=false){
        if(getCurrentUserId().isNotEmpty()) {
            mFireStore.collection(Constants.USERS)
                .document(getCurrentUserId())
                .get()
                .addOnSuccessListener {
                    val user = it.toObject(User::class.java)
                    if (activity is MainActivity) {
                        if (user == null) {
                            activity.registerUser()
                        } else {
                            if (updateActiveStatus) {
                                activity.onGetActiveStatusUpdatedUser(user,isSigningOut)
                            } else {
                                activity.onGetUserSuccess(user)
                            }
                        }
                    }
                    if(activity is ConversationActivity){
                        activity.onGetUserOnConversationActivity(user)
                    }
                    if(activity is ProfileActivity){
                        activity.onGettingProfileUpdatedUser(user)
                    }
                }
                .addOnFailureListener {
                    Log.e(activity.javaClass.simpleName, "Error writing into the database")
                }
        }
    }
    fun getUpdatedUser(activity:Activity,updateActiveStatus: Boolean = false,isSigningOut: Boolean=false){
        if(getCurrentUserId().isNotEmpty()) {
            mFireStore.collection(Constants.USERS)
                .document(getCurrentUserId())
                .get()
                .addOnSuccessListener {
                    val user = it.toObject(User::class.java)
                    if (activity is MainActivity) {
                        if (user != null) {
                            activity.onGetUpdatedUser(user)
                    }}}
                .addOnFailureListener {
                    Log.e(activity.javaClass.simpleName, "Error writing into the database")
                }
        }
    }
    fun updateUserProfileData(activity: Activity, hashMap: HashMap<String, Any>,updatedActiveStatus: Boolean=false,signingOut:Boolean = false) {
        if(getCurrentUserId().isNotEmpty()) {
            mFireStore.collection(Constants.USERS)
                .document(getCurrentUserId())
                .update(hashMap)
                .addOnSuccessListener {
                    Log.d("Romes", "Data updated successfully")
                    when (activity) {
                        is ProfileActivity -> {
                            activity.profileUpdateSuccess()
                        }
                        is MainActivity -> {
                            if(signingOut){
                                activity.onSignOut()
                            }
                            else {
                                if (updatedActiveStatus) {
                                    activity.onUpdateActiveStatus()
                                } else {
                                    activity.tokenUpdateSuccess()
                                }
                            }

                        }
                        is ConversationActivity -> {
                            activity.onUserUpdatedFromConversation()
                        }
                    }

                }
                .addOnFailureListener {
                    when (activity) {
                        is ProfileActivity -> {
                            activity.hideProgressDialog()
                        }
                        is ConversationActivity-> {
                            Log.d("Romes Checking","aalu")
                        }
                    }
                }
        }
    }

//    fun getUserList(activity:MainActivity){
//       mFireStore.collection(Constants.USERS)
//           .get()
//           .addOnSuccessListener {
//               Log.d("Romes", "Got BOARDS")
//               val userList = ArrayList<User>()
//               for (i in it.documents) {
//                   val user = i.toObject(User::class.java)!!
//                   userList.add(user)
//               }
//               activity.onGettingUserList(userList)
//           }
//           .addOnFailureListener {
//               Log.d("Romes", "cannot get boards")
//           }
//    }
    fun getUserFromName(activity:SearchActivity,name:String){
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.NAME,name)
            .get()
            .addOnSuccessListener {
                if (it.size() > 0) {
                    val user = it.documents[0].toObject(User::class.java)!!
                    activity.onGettingUserFromSearch(user)
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackbar("No such user found")
                }
            }.addOnFailureListener {
                Log.e(activity.javaClass.simpleName, "Error writing into the database")
            }
    }
    fun getUserList(activity:SearchActivity){
       mFireStore.collection(Constants.USERS)
           .get()
           .addOnSuccessListener {
               Log.d("Romes", "Got BOARDS")
               val userList = ArrayList<String>()
               for (i in it.documents) {
                   val user = i.toObject(User::class.java)!!
                   userList.add(user.name)
               }
               activity.onGettingUserList(userList)
           }
           .addOnFailureListener {
               Log.d("Romes", "cannot get boards")
           }
    }

    fun createConversation(activity: SearchActivity,conversation: Conversation){
        mFireStore.collection(Constants.CONVERSATION)
            .document()
            .set(conversation, SetOptions.merge())
            .addOnSuccessListener {
                activity.onConversationCreated(conversation)
            }
            .addOnFailureListener {
                Log.e(activity.javaClass.simpleName, "Error writing into the database")
            }
    }

    fun getConversationList(activity: Activity,user:User,activeStatusUpdated: Boolean = false,isSigningOut: Boolean=false){
        mFireStore.collection(Constants.CONVERSATION)
            .whereArrayContains(Constants.MEMBER_LIST,user)
            .get()
            .addOnSuccessListener {
                val conversationList = ArrayList<Conversation>()
                for (i in it.documents) {
                    val conversation = i.toObject(Conversation::class.java)!!
                    conversation.documentId = i.id
                    conversationList.add(conversation)
                }
                if(activity is MainActivity) {
                    if (activeStatusUpdated) {
                        activity.onGettingActiveUpdatedConversationList(
                            conversationList,
                            isSigningOut
                        )
                    } else {
                        activity.updateConversationListToUI(conversationList)
                    }
                }
                if(activity is ProfileActivity){
                    activity.onGettingConversationListOnProfile(conversationList)
                }

            }
    }

    fun updateConversation(activity: Activity,conversationHashMap: HashMap<String,Any>,documentId:String,isSigningOut: Boolean = false){
        mFireStore.collection(Constants.CONVERSATION)
            .document(documentId)
            .update(conversationHashMap)
            .addOnSuccessListener {
                Log.d("Romes", "Data updated successfully")
                if (activity is MainActivity) {
                    if (isSigningOut) {
                        activity.onConversationUpdatedSignOut()
                    } else {
                        activity.checkIfUserProfileUpdated()
                    }
                }
                if(activity is ProfileActivity){
                    activity.onConversationUpdatedOnProfile()
                }
            }
            .addOnFailureListener {
                if(activity is MainActivity){
                activity.hideProgressDialog()}
            }
    }

    fun getConversationDetails(conversationActivity: ConversationActivity, conversationDocumentId: String) {
        mFireStore.collection(Constants.CONVERSATION)
            .document(conversationDocumentId)
            .get()
            .addOnSuccessListener {
                val conversation = it.toObject(Conversation::class.java)!!
                conversation.documentId = conversationDocumentId
                conversationActivity.onGettingConversationDetails(conversation)
            }
            .addOnFailureListener {
                Log.d("Romes", "cannot get boards")
            }


    }

    fun updateConversationMessage(activity: Activity,conversationHashMap: HashMap<String,Any>,documentId: String) {
        mFireStore.collection(Constants.CONVERSATION)
            .document(documentId)
            .update(conversationHashMap)
            .addOnSuccessListener {
                    if(activity is ConversationActivity){
                        activity.onMessageSent()
                    }
                    if(activity is MainActivity){
                    Log.d("Romes","Conversation Updated Successfully")
                    }
                if(activity is ProfileActivity){
                    Log.d("Romes","Conversation Updated Successfully")
                }
            }
            .addOnFailureListener {
                Log.d("Romes", "cannot get boards")
            }

    }

    fun signOutWithoutUpdate(mainActivity: MainActivity, signingOut: Boolean) {
        if(signingOut){
            mainActivity.onConversationUpdatedSignOut()
        }
    }

//    fun signOutWithoutUpdate(mainActivity: MainActivity, signingOut: Boolean) {
//        if(signingOut) {
//            mainActivity.onConversationUpdatedSignOut()
//        }
//    }


}