package com.romes.chatapp.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.lang.Exception
import java.util.*

class GetAddressFromLatLng(context:Context,private val latitude: Double,private val longitude: Double) {

    private val geoCoder: Geocoder = Geocoder(context, Locale.getDefault())

    fun doInBackground(): String{
        try{
        val addressList : List<Address> =geoCoder.getFromLocation(latitude,longitude,1)

        if(addressList.isNotEmpty()){
            val address: Address = addressList[0]
            val sb = StringBuilder()
            for(i in 0..address.maxAddressLineIndex){
                sb.append(address.getAddressLine(i)).append(" ")
            }
            sb.deleteCharAt(sb.length-1)
            return sb.toString()
        }
        }
        catch(e:Exception){
            e.printStackTrace()
        }
        return ""


    }

}