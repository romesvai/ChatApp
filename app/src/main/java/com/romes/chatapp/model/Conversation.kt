package com.romes.chatapp.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*

@TypeConverters(com.romes.chatapp.utils.TypeConverters::class)
@Entity(tableName = "conversation-table")
data class Conversation(
    val title: String="",
    val subTitle: String="",
    val activeStatus: Boolean=false,
    val image:String="",
    var memberList:ArrayList<User> = ArrayList(),
    @PrimaryKey
    var documentId: String = "",
    val messageList: ArrayList<Message> = ArrayList(),
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!,
        parcel.createTypedArrayList(User.CREATOR)!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Message.CREATOR)!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(subTitle)
        parcel.writeByte(if (activeStatus) 1 else 0)
        parcel.writeString(image)
        parcel.writeString(documentId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Conversation> {
        override fun createFromParcel(parcel: Parcel): Conversation {
            return Conversation(parcel)
        }

        override fun newArray(size: Int): Array<Conversation?> {
            return arrayOfNulls(size)
        }
    }

}
