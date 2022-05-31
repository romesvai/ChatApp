package com.romes.chatapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.romes.chatapp.dao.ConversationDao
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.utils.Constants

@Database(entities = [Conversation::class],version = 1)
abstract class ConversationDatabase: RoomDatabase() {

    abstract fun conversationDao() : ConversationDao
    companion object{
        @Volatile
        private var INSTANCE: ConversationDatabase? = null

        fun getInstance(context: Context): ConversationDatabase {
            synchronized(this){
                var instance = INSTANCE
                if(instance==null){
                    instance = Room.databaseBuilder(context,ConversationDatabase::class.java,
                        Constants.CONVERSATION_DATABASE).fallbackToDestructiveMigration().build()
                }
                return instance
            }
        }

    }

}