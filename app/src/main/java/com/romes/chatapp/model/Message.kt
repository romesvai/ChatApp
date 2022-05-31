package com.romes.chatapp.model

import android.os.Parcel
import android.os.Parcelable

data class Message(
    val messageBody: String="",
    val timeStamp: Long=0,
    val sender: User?= null,
    var locationLatitude: Double=0.0,
    var locationLongitude: Double=0.0,
    var locationAddress: String="",
    var image: String ="",
    var recording: String =""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readParcelable(User::class.java.classLoader),
        parcel.readDouble()!!,
        parcel.readDouble()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(messageBody)
        parcel.writeLong(timeStamp)
        parcel.writeParcelable(sender, flags)
        parcel.writeDouble(locationLatitude)
        parcel.writeDouble(locationLongitude)
        parcel.writeString(locationAddress)
        parcel.writeString(image)
        parcel.writeString(recording)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Message> {
        override fun createFromParcel(parcel: Parcel): Message {
            return Message(parcel)
        }

        override fun newArray(size: Int): Array<Message?> {
            return arrayOfNulls(size)
        }
    }
}

