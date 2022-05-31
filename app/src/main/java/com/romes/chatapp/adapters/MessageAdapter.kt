package com.romes.chatapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.romes.chatapp.databinding.ItemMessageBinding
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.model.Message
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.romes.chatapp.R
import com.romes.chatapp.activities.ImageActivity
import com.romes.chatapp.model.Location
import com.romes.chatapp.utils.Constants
import java.io.IOException


class MessageAdapter(val context: Context, private val messageList: ArrayList<Message>,private val ownLocation: Location) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    private var mMediaPlayer : MediaPlayer? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messageList[position]
        if(message.sender?.userId == FirestoreClass().getCurrentUserId()){
            holder.cvMessageOther.visibility = View.GONE
            holder.cvMessageOwn.visibility = View.VISIBLE
            when {
                message.locationAddress.isNotEmpty() -> {
                    holder.ibPlayButtonOwn.visibility = View.GONE
                    holder.tvRecordingOwn.visibility = View.GONE
                    holder.tvMessageBodyOwn.visibility = View.VISIBLE
                    holder.ivLocationImageOwn.visibility = View.VISIBLE
                    holder.ivMessageImageOwn.visibility = View.GONE
                    val locationString = ("Location: ${message.locationAddress}")
                    val toShowString = "${locationString.substring(0, 28)} ...."
                    holder.tvMessageBodyOwn.text = toShowString
                }
                message.recording.isNotEmpty() -> {
                    holder.ibPlayButtonOwn.visibility = View.VISIBLE
                    holder.tvRecordingOwn.visibility = View.VISIBLE
                    val recording = "Recording"
                    holder.tvRecordingOwn.text = recording
                    holder.ivLocationImageOwn.visibility = View.GONE
                    holder.tvMessageBodyOwn.visibility = View.GONE
                    holder.ivMessageImageOwn.visibility = View.GONE
                    holder.ibPlayButtonOwn.setOnClickListener {
                        if (mMediaPlayer?.isPlaying == true) {
                            holder.ibPlayButtonOwn.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    context.resources,
                                    R.drawable.ic_play_button, null
                                )
                            )
                            mMediaPlayer?.stop()
                            mMediaPlayer?.reset()
                        } else {
                            holder.ibPlayButtonOwn.setImageDrawable(
                                ResourcesCompat.getDrawable(
                                    context.resources,
                                    R.drawable.ic_stop, null
                                )
                            )
                            val audioAttributes = AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                            mMediaPlayer = MediaPlayer()
                            mMediaPlayer!!.setAudioAttributes(audioAttributes)
                            try {
                                mMediaPlayer!!.setDataSource(message.recording)
                                // below line is use to prepare
                                // and start our media player.
                                mMediaPlayer!!.prepare()
                                mMediaPlayer!!.start()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            Toast.makeText(context, "Playing", Toast.LENGTH_SHORT).show()
                        }
                        mMediaPlayer?.setOnCompletionListener {
                            holder.ibPlayButtonOwn.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.ic_play_button,null))
                        }
                    }
                }
                else -> {
                    holder.ibPlayButtonOwn.visibility = View.GONE
                    holder.tvRecordingOwn.visibility = View.GONE
                    holder.tvMessageBodyOwn.visibility = View.VISIBLE
                    holder.tvMessageBodyOwn.text = message.messageBody
                    holder.ivLocationImageOwn.visibility = View.GONE
                    holder.ivMessageImageOwn.visibility = View.GONE
                }
            }
            holder.tvTimeStampOwn.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timeStamp)
            if(message.image.isNotEmpty()){
                holder.ibPlayButtonOwn.visibility = View.GONE
                holder.tvRecordingOwn.visibility = View.GONE
                holder.tvMessageBodyOwn.visibility= View.VISIBLE
                holder.ivMessageImageOwn.visibility = View.VISIBLE
                Glide
                    .with(context)
                    .load(Uri.parse(message.image))
                    .centerCrop()
                    .into(holder.ivMessageImageOwn)
                holder.ivMessageImageOwn.setOnClickListener {
                    val intent = Intent(context, ImageActivity::class.java)
                    intent.putExtra(Constants.EXTRA_IMAGE, message.image)
                    context.startActivity(intent)
                }
            }
        }
        else {
            holder.cvMessageOwn.visibility = View.GONE
            holder.cvMessageOther.visibility = View.VISIBLE
            if(message.locationAddress.isNotEmpty()){
                holder.ibPlayButtonOther.visibility = View.GONE
                holder.tvRecordingOther.visibility = View.GONE
                holder.ivLocationImageOther.visibility = View.VISIBLE
                holder.tvMessageBodyOther.visibility = View.VISIBLE
                holder.ivMessageImageOther.visibility = View.GONE
                val locationString = ("Location: ${message.locationAddress}")
                val toShowString = "${locationString.substring(0,30)} ...."
                holder.tvMessageBodyOther.setOnClickListener {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?saddr=${ownLocation.locationLatitude},${ownLocation.locationLongitude}&daddr=${message.locationLatitude},${message.locationLongitude}"))
                    context.startActivity(intent)
                }
                holder.tvMessageBodyOther.text = toShowString
            }
            else {
                holder.tvMessageBodyOther.visibility = View.VISIBLE
                holder.tvMessageBodyOther.text = message.messageBody
                holder.ivLocationImageOther.visibility = View.GONE
                holder.ivMessageImageOther.visibility = View.GONE
                holder.tvRecordingOther.visibility = View.GONE
                holder.ibPlayButtonOther.visibility = View.GONE

            }
            Glide
                .with(context)
                .load(message.sender?.image)
                .circleCrop()
                .into(holder.ivProfileImageOther)
            holder.tvTimeStampOther.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timeStamp)

            if(message.image.isNotEmpty()){
                holder.ibPlayButtonOther.visibility = View.GONE
                holder.tvRecordingOther.visibility = View.GONE
                holder.ivMessageImageOther.visibility = View.VISIBLE
                holder.tvMessageBodyOther.visibility = View.VISIBLE
                Glide
                    .with(context)
                    .load(Uri.parse(message.image))
                    .centerCrop()
                    .into(holder.ivMessageImageOther)
                holder.ivMessageImageOther.setOnClickListener {
                    val intent = Intent(context,ImageActivity::class.java)
                    intent.putExtra(Constants.EXTRA_IMAGE,message.image)
                    context.startActivity(intent)

                }

            }
            if(message.recording.isNotEmpty()){
                holder.ibPlayButtonOther.visibility = View.VISIBLE
                holder.tvRecordingOther.visibility = View.VISIBLE
                val recording = "Recording"
                holder.ivLocationImageOther.visibility = View.GONE
                holder.tvRecordingOther.text = recording
                holder.tvMessageBodyOther.visibility = View.GONE
                holder.ivMessageImageOther.visibility = View.GONE
                holder.ibPlayButtonOther.setOnClickListener {
                    if(mMediaPlayer?.isPlaying == true){
                        holder.ibPlayButtonOther.setImageDrawable(
                            ResourcesCompat.getDrawable(context.resources,
                                R.drawable.ic_play_button_other,null))
                        mMediaPlayer?.stop()
                        mMediaPlayer?.reset()

                    }
                    else {
                        holder.ibPlayButtonOther.setImageDrawable(
                            ResourcesCompat.getDrawable(context.resources,
                                R.drawable.ic_stop_button_other,null))
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                        mMediaPlayer = MediaPlayer()
                        mMediaPlayer!!.setAudioAttributes(audioAttributes)
                        try {
                            mMediaPlayer!!.setDataSource(message.recording)
                            // below line is use to prepare
                            // and start our media player.
                            mMediaPlayer!!.prepare()
                            mMediaPlayer!!.start()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        mMediaPlayer?.setOnCompletionListener {
                            holder.ibPlayButtonOther.setImageDrawable(ResourcesCompat.getDrawable(context.resources,R.drawable.ic_play_button_other,null))
                        }
                        Toast.makeText(context,"Playing",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    }

    override fun getItemCount(): Int {
        return messageList.size
    }



    inner class ViewHolder(binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        val cvMessageOwn = binding.cvMessageOwn
        val cvMessageOther = binding.cvMessageOther
        val tvMessageBodyOwn = binding.tvMessageBodyOwn
        val tvTimeStampOwn = binding.tvTimestampOwn
        val ivProfileImageOther = binding.ivMessageProfileImageOther
        val tvMessageBodyOther = binding.tvMessageBodyOther
        val tvTimeStampOther = binding.tvTimestampOther
        val ivMessageImageOther = binding.ivMessageImageOther
        val ivMessageImageOwn = binding.ivMessageImageOwn
        val ivLocationImageOwn = binding.ivLocationOwn
        val ivLocationImageOther = binding.ivLocationOther
        val ibPlayButtonOwn = binding.ibPlay
        val tvRecordingOwn = binding.tvRecording
        val ibPlayButtonOther = binding.ibPlayOther
        val tvRecordingOther = binding.tvRecordingOther
    }



}