package com.romes.chatapp.dao

import androidx.room.*
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.utils.Constants
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert
    suspend fun insertConversation(conversation: Conversation)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Delete
    suspend fun deleteConversation(conversation: Conversation)

    @Query("SELECT * FROM `${Constants.CONVERSATION_TABLE}`")
    fun getAllConversation() : Flow<List<Conversation>>
}