package com.dev.mywebrtc.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.dev.mywebrtc.R
import com.dev.mywebrtc.databinding.ItemMainRecyclerViewBinding
import com.dev.mywebrtc.utils.UserStatus

class AdapterMainRecyclerView(private val listener: OnViewClickListener) :
    Adapter<AdapterMainRecyclerView.MainRecyclerViewHolder>() {

    private var userList: List<Pair<String, String>>? = null
    fun updateList(list: List<Pair<String, String>>) {
        this.userList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MainRecyclerViewHolder(
        ItemMainRecyclerViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )


    override fun getItemCount() = userList?.size ?: 0

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        userList?.let { list ->
            val user = list[position]
            holder.bind(user, {
                listener.onVideoCallClicked(it)
            }, {
                listener.onAudioCallClicked(it)
            })
        }
    }

    interface OnViewClickListener {
        fun onVideoCallClicked(username: String)
        fun onAudioCallClicked(username: String)
    }

    class MainRecyclerViewHolder(itemMainRecyclerViewBinding: ItemMainRecyclerViewBinding) :
        ViewHolder(itemMainRecyclerViewBinding.root) {
        private val binding = itemMainRecyclerViewBinding
        private val context = binding.root.context

        fun bind(
            user: Pair<String, String>,
            videoCallClicked: (String) -> Unit,
            audioCallClicked: (String) -> Unit
        ) {
            binding.apply {
                when (user.second) {
                    UserStatus.ONLINE.toString() -> {
                        videoCallBtn.isVisible = true
                        audioCallBtn.isVisible = true
                        videoCallBtn.setOnClickListener {
                            videoCallClicked.invoke(user.first)
                        }
                        audioCallBtn.setOnClickListener {
                            audioCallClicked.invoke(user.first)
                        }
                        statusTv.setTextColor(ContextCompat.getColor(context, R.color.light_green))
                        statusTv.text = UserStatus.ONLINE.toString()
                    }

                    UserStatus.OFFLINE.toString() -> {
                        videoCallBtn.isVisible = false
                        audioCallBtn.isVisible = false
                        statusTv.setTextColor(ContextCompat.getColor(context, R.color.red))
                        statusTv.text = UserStatus.OFFLINE.toString()
                    }

                    UserStatus.IN_CALL.toString() -> {
                        videoCallBtn.isVisible = false
                        audioCallBtn.isVisible = false
                        statusTv.setTextColor(ContextCompat.getColor(context, R.color.yellow))
                        statusTv.text = UserStatus.IN_CALL.toString()
                    }
                }
                usernameTv.text = user.first
            }
        }
    }
}