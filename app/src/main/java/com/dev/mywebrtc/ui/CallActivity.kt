package com.dev.mywebrtc.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.dev.mywebrtc.R
import com.dev.mywebrtc.databinding.ActivityCallBinding
import com.dev.mywebrtc.service.MainService
import com.dev.mywebrtc.service.MainServiceRepository
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_CALLER
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_VIDEO_CALL
import com.dev.mywebrtc.utils.FirebaseFieldNames.TARGET
import com.dev.mywebrtc.utils.convertToHumanTime
import com.dev.mywebrtc.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class CallActivity : AppCompatActivity(), MainService.EndCallListener {

    private val binding by lazy {
        ActivityCallBinding.inflate(layoutInflater)
    }

    @Inject
    lateinit var serviceRepository: MainServiceRepository
    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>

    private var target: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true

    private var isMicroPhoneMuted: Boolean = false
    private var isCameraMuted: Boolean = false
    private var isScreenCasting: Boolean = false
    private var isSpeakerMode: Boolean = true

    override fun onStart() {
        super.onStart()
        requestScreenCaptureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    //its time to give this intent to our service and service passes it to our webrtc client
                    MainService.screenPermissionIntent = intent
                    isScreenCasting = true
                    updateUiToScreenCaptureIsOn()
                    serviceRepository.toggleScreenShare(true)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        initBlock()
    }

    private fun initBlock() {
        intent.getStringExtra(TARGET)?.let {
            this.target = it
        } ?: kotlin.run {
            finish()
        }

        isVideoCall = intent.getBooleanExtra(IS_VIDEO_CALL, true)
        isCaller = intent.getBooleanExtra(IS_CALLER, true)

        binding.apply {
            if (!isVideoCall) {
                toggleCameraButton.isVisible = false
                screenShareButton.isVisible = false
                switchCameraButton.isVisible = false
            }

            MainService.remoteSurfaceView = remoteView
            MainService.localSurfaceView = localView
            serviceRepository.setupViews(isVideoCall, isCaller, target)

            callTitleTv.text = "In Call With $target"
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0..3600) {
                    delay(1000)
                    withContext(Dispatchers.Main){
                        //convert this int to human readable time
                        callTimerTv.text = i.convertToHumanTime()
                    }
                }
            }

            endCallButton.setOnClickListener {
                serviceRepository.sendEndCall()
            }

            switchCameraButton.setOnClickListener {
                serviceRepository.switchCamera()
            }

        }
        setupMicToggledClicked()
        setupCameraToggledClicked()
        setupToggleAudioDevice()
        setupScreenCasting()
        MainService.endCallListener = this
    }

    private fun setupScreenCasting() {
        binding.apply {
            screenShareButton.setOnClickListener {
                if (!isScreenCasting) {
                    //we have to start casting
                    AlertDialog.Builder(this@CallActivity).apply {
                        setTitle("Screen Casting")
                        setMessage("Are you sure to start casting?")
                        setPositiveButton("Yes") { dialog, _ ->
                            //start screen casting process
                            startScreenCapture()
                        }
                        setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        create()
                        show()
                    }
                } else {
                    //we have to end screen casting
                    isScreenCasting = false
                    updateUiToScreenCaptureIsOff()
                    serviceRepository.toggleScreenShare(false)
                }
            }
        }
    }

    private fun startScreenCapture() {
        val mediaProjectionManager =
            application.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        requestScreenCaptureLauncher.launch(captureIntent)

    }

    private fun updateUiToScreenCaptureIsOn() {
        binding.apply {
            localView.isVisible = false
            switchCameraButton.isVisible = false
            toggleCameraButton.isVisible = false
            screenShareButton.setImageResource(R.drawable.ic_stop_screen_share)
        }
    }

    private fun updateUiToScreenCaptureIsOff() {
        binding.apply {
            localView.isVisible = true
            switchCameraButton.isVisible = true
            toggleCameraButton.isVisible = true
            screenShareButton.setImageResource(R.drawable.ic_screen_share)
        }
    }

    private fun setupMicToggledClicked() {
        binding.apply {
            toggleMicrophoneButton.setOnClickListener {
                if (!isMicroPhoneMuted) {
                    //we should mute our mic
                    //1. send command to repository
                    serviceRepository.toggleAudio(true)
                    //2. update ui to mic is muted
                    toggleCameraButton.setImageResource(R.drawable.ic_mic_on)
                } else {
                    //we should set it back to normal
                    //1. send command to repository to make it back to normal status
                    serviceRepository.toggleAudio(false)
                    //2. update ui
                    toggleCameraButton.setImageResource(R.drawable.ic_mic_off)
                }
            }
        }
    }

    private fun setupToggleAudioDevice() {
        binding.apply {
            toggleAudioDevice.setOnClickListener {
                if (isSpeakerMode) {
                    //we should set it to earpiece mode
                    toggleAudioDevice.setImageResource(R.drawable.ic_speaker)
                    //we should send a command to our service to switch between devices
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)
                } else {
                    //we should set it to speaker mode
                    toggleAudioDevice.setImageResource(R.drawable.ic_ear)
                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)
                }
                isSpeakerMode = !isSpeakerMode
            }
        }
    }

    private fun setupCameraToggledClicked() {
        binding.apply {
            toggleCameraButton.setOnClickListener {
                if (!isCameraMuted) {
                    serviceRepository.toggleVideo(true)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
                } else {
                    serviceRepository.toggleVideo(false)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
                }
            }
        }
    }

    override fun onCallEnded() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        MainService.remoteSurfaceView?.release()
        MainService.remoteSurfaceView = null

        MainService.localSurfaceView?.release()
        MainService.localSurfaceView = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
        serviceRepository.sendEndCall()
    }


}