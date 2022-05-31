package com.romes.chatapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.romes.chatapp.database.ConversationDatabase
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.model.User


class ChatApp : Application(),DefaultLifecycleObserver{
    val db by lazy{
            ConversationDatabase.getInstance(this)
    }

}