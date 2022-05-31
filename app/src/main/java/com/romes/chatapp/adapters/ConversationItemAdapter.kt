package com.romes.chatapp.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.romes.chatapp.databinding.ItemConversationBinding
import com.romes.chatapp.firebase.FirestoreClass
import com.romes.chatapp.model.Conversation


class ConversationItemAdapter(val context: Context, private val conversationList: ArrayList<Conversation>) :
    RecyclerView.Adapter<ConversationItemAdapter.ViewHolder>() {
    private var onClickListener: OnClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemConversationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val memberList = conversationList[position].memberList
        for (i in memberList){
            if(i.userId == FirestoreClass().getCurrentUserId()){
                continue
            }
            holder.tvConversationTitle.text = i.name
            Glide
                .with(context)
                .load(i.image)
                .circleCrop()
                .into(holder.ivProfileImage)
            if(i.activeStatus){
                holder.ivActiveStatus.visibility = View.VISIBLE
                holder.ivProfileImage.borderColor = Color.GREEN
                holder.ivProfileImage.borderWidth = 3
            }
            else{
                holder.ivActiveStatus.visibility = View.GONE
                holder.ivProfileImage.borderColor = Color.GRAY
                holder.ivProfileImage.borderWidth = 3
            }
        }
        if(conversationList[position].messageList.size>0){
            holder.tvConversationSubtitle.text = conversationList[position].messageList[conversationList[position].messageList.size-1].messageBody
        }
        else{
            holder.tvConversationSubtitle.text= conversationList[position].subTitle
        }
        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, conversationList[position])
            }
        }

    }

    override fun getItemCount(): Int {
        return conversationList.size
    }

    fun onClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }


    inner class ViewHolder(binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root) {
        val tvConversationTitle = binding.tvConversationTitle
        val tvConversationSubtitle = binding.tvSubtitle
        val ivProfileImage = binding.civProfileImage
        val ivActiveStatus = binding.ivActive
    }

    interface OnClickListener {
        fun onClick(position: Int, model: Conversation)

    }


}