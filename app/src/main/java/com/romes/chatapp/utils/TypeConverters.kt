package com.romes.chatapp.utils

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.romes.chatapp.model.Conversation
import com.romes.chatapp.model.Message
import com.romes.chatapp.model.User
import java.lang.reflect.Member
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList


class TypeConverters {
    private val gson = GsonBuilder().create()

    @TypeConverter
    fun stringToMemberList(data: String?): ArrayList<User?>? {
        if (data == null) {
            return ArrayList()
        }

        return  gson.fromJson<ArrayList<User?>>(data, object :TypeToken<ArrayList<User?>>(){}.type)
    }

    @TypeConverter
    fun memberListToString(someObjects: ArrayList<User?>?): String {
        return gson.toJson(someObjects)
    }

    @TypeConverter
    fun stringToMessageList(data: String?): ArrayList<Message?>? {
        if (data == null) {
            return ArrayList()
        }
        val listType: Type = object : TypeToken<ArrayList<Message?>?>() {}.type
        return gson.fromJson(data, listType)
    }

    @TypeConverter
    fun messageListToString(someObjects: ArrayList<Message?>?): String {
        return gson.toJson(someObjects)
    }
}