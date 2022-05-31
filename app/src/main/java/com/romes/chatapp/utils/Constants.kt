package com.romes.chatapp.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

object Constants {
    const val AUTHORITY = "com.romes.chatapp.fileProvider"
    const  val EXTRA_IMAGE="extra_image"
    const val EXTRA_PROFILE_UPDATE_FLAG="extra_profile_updated_flag"
    const val USERS = "Users"
    const val EXTRA_USER = "extra_user"
    const val IMAGE = "image"
    const val NAME = "name"
    const val ACTIVE_STATUS = "activeStatus"
    const val LOCATION_ADDRESS ="locationAddress"
    const val LOCATION_LATITUDE="locationLatitude"
    const val LOCATION_LONGITUDE="locationLongitude"
    const val EXTRA_SIGN_OUT="extra_sign_out"
    const val EXTRA_FROM_SEARCH="extra_from_search"
    const val DOCUMENT_ID = "documentId"
    const val EXTRA_CONVERSATION_DOCUMENT_ID = "extra_document_id"
    const val CONVERSATION = "Conversation"
    const val MEMBER_LIST = "memberList"
    const val PREF_TOKEN = "pref_token"
    const val FCM_TOKEN_UPDATED = "fcmTokenUpdated"
    const val FCM_TOKEN = "fcmToken"
    const val FCM_BASE_URL: String = "https://fcm.googleapis.com/fcm/send"
    const val FCM_AUTHORIZATION: String = "authorization"
    const val FCM_KEY: String = "key"
    const val FCM_SERVER_KEY: String =
        "AAAA7SBQIb0:APA91bFYnYdyslJ_ZGhKEeKGkJABP9GvUQUBGvbns1mANKGcRwMHHob8Ot-qpQtX5E1kxgC0MM9BleUUhLLuf3CjshPAjkzgE8m1SdQ19kBD8QdO5YU4Pb174IBS710SqzifaowPGT3j"
    const val FCM_KEY_TITLE: String = "title"
    const val FCM_KEY_MESSAGE: String = "message"
    const val FCM_KEY_NOTIFICATION: String = "notification"
    const val FCM_KEY_TO: String = "to"
    const val FIREBASE_MESSAGE_TAG = "MyFirebaseMessage"
    const val MESSAGE_LIST = "messageList"
    const val CONVERSATION_DATABASE = "conversation-database"
    const val CONVERSATION_TABLE = "conversation-table"
    const val EXTRA_BACK_FROM_CONVERSATION="extra_back_from_conversation"
    fun getExtension(activity: Activity, uri: Uri?): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(activity.contentResolver.getType(uri!!))
    }

    fun choosePhotoFromGallery(
        activity: Activity,
        openGalleryLauncher: ActivityResultLauncher<Intent>
    ) {
        Dexter.withContext(activity).withPermissions(
            Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val mediaIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(mediaIntent)
                }

            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?
            ) {
                showRationaleDialogForPermission(activity)
            }
        }).onSameThread().check()
    }

    fun showRationaleDialogForPermission(activity: Activity) {
        AlertDialog.Builder(activity)
            .setMessage("It looks like you turned off permissions required for this feature it can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    fun isNetworkAvailable(context: Context): Boolean
    {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when{
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)-> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)-> true
            else-> false

        }


    }
}