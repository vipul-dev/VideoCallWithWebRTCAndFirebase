package com.dev.mywebrtc.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.dev.mywebrtc.adapter.AdapterMainRecyclerView
import com.dev.mywebrtc.databinding.ActivityMainBinding
import com.dev.mywebrtc.repository.MainRepository
import com.dev.mywebrtc.service.MainService
import com.dev.mywebrtc.service.MainServiceRepository
import com.dev.mywebrtc.utils.DataModel
import com.dev.mywebrtc.utils.DataModelType
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_CALLER
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_VIDEO_CALL
import com.dev.mywebrtc.utils.FirebaseFieldNames.TARGET
import com.dev.mywebrtc.utils.FirebaseFieldNames.USERNAME
import com.dev.mywebrtc.utils.getCameraAndMicPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), AdapterMainRecyclerView.OnViewClickListener,
    MainService.Listener {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var username: String? = null

    @Inject
    lateinit var mainRepository: MainRepository
    @Inject
    lateinit var mainServiceRepository: MainServiceRepository

    private val adapterMainRecyclerView by lazy {
        AdapterMainRecyclerView(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        initBlock()

        setRecyclerView()
    }

    private fun setRecyclerView() {
        binding.mainRecyclerView.apply {
            setLayoutManager(
                LinearLayoutManager(
                    this@MainActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            )
            adapter = adapterMainRecyclerView
        }
    }

    private fun initBlock() {
        username = intent.getStringExtra(USERNAME)
        if (username == null) finish()
        // 1. observe other user status
        subscribeObserver()
        // 2. start foreground service to listen negotiations and calls
        startMyService()
    }

    private fun startMyService() {
        mainServiceRepository.startService(username!!)
    }

    private fun subscribeObserver() {
        setRecyclerView()
        MainService.listener = this
        mainRepository.observeUsersStatus {
            adapterMainRecyclerView.updateList(it)
        }
    }

    override fun onVideoCallClicked(username: String) {
        // check if permission of mic and camera is taken

        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username,true){
                if(it){
                    // we have to start video call
                    // we wanna create an intent to move to call activity

                    startActivity(Intent(this@MainActivity,CallActivity::class.java).apply {
                        putExtra(TARGET,username)
                        putExtra(IS_VIDEO_CALL,true)
                        putExtra(IS_CALLER,true)
                    })
                }
            }
        }
    }

    override fun onAudioCallClicked(username: String) {
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username,false){
                if (it){
                    // we have to start audio call
                    // we wanna create an intent to move to call activity

                    startActivity(Intent(this@MainActivity,CallActivity::class.java).apply {
                        putExtra(TARGET,username)
                        putExtra(IS_VIDEO_CALL,false)
                        putExtra(IS_CALLER,true)
                    })
                }

            }
        }
    }

    override fun onCallReceived(model: DataModel) {
        runOnUiThread{
            binding.apply {
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        // create an intent to go to video call activity
                        startActivity(Intent(this@MainActivity,CallActivity::class.java).apply {
                            putExtra(TARGET,model.sender)
                            putExtra(IS_VIDEO_CALL,isVideoCall)
                            putExtra(IS_CALLER,false)
                        })
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mainServiceRepository.stopService()
    }

}