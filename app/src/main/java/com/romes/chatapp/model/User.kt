package com.romes.chatapp.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded

data class User(
    val userId: String="",
    val name: String ="",
    var activeStatus: Boolean=false,
    val image: String="",
    val fcmToken: String="",
    var locationLatitude: Double=0.0,
    var locationLongitude: Double=0.0,
    var locationAddress: String=""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userId)
        parcel.writeString(name)
        parcel.writeByte(if (activeStatus) 1 else 0)
        parcel.writeString(image)
        parcel.writeString(fcmToken)
        parcel.writeDouble(locationLatitude)
        parcel.writeDouble(locationLongitude)
        parcel.writeString(locationAddress)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}

