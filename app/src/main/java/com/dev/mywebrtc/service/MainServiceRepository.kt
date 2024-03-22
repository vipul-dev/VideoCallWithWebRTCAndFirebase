package com.dev.mywebrtc.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.dev.mywebrtc.utils.FirebaseFieldNames.AUDIO_DEVICE_TYPE
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_CALLER
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_SCREEN_SHARE
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_VIDEO_CALL
import com.dev.mywebrtc.utils.FirebaseFieldNames.SHOULD_BE_MUTED
import com.dev.mywebrtc.utils.FirebaseFieldNames.TARGET
import com.dev.mywebrtc.utils.FirebaseFieldNames.USERNAME
import javax.inject.Inject

class MainServiceRepository @Inject constructor(
    private val context: Context
) {

    fun startService(username: String) {
        Thread {
            val intent = Intent(context, MainService::class.java).apply {
                putExtra(USERNAME, username)
                action = MainServiceActions.START_SERVICE.name
            }
            startServiceIntent(intent)
        }.start()
    }

    private fun startServiceIntent(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun setupViews(videoCall: Boolean, caller: Boolean, target: String?) {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.SETUP_VIEWS.name
            putExtra(IS_VIDEO_CALL,videoCall)
            putExtra(TARGET,target)
            putExtra(IS_CALLER,caller)
        }
        startServiceIntent(intent)
    }

    fun sendEndCall() {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.END_CALL.name
        }
        startServiceIntent(intent)
    }

    fun switchCamera() {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.SWITCH_CAMERA.name
        }
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.TOGGLE_AUDIO.name
            putExtra(SHOULD_BE_MUTED,shouldBeMuted)
        }
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.TOGGLE_VIDEO.name
            putExtra(SHOULD_BE_MUTED,shouldBeMuted)
        }
        startServiceIntent(intent)
    }

    fun toggleAudioDevice(type: String) {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.TOGGLE_AUDIO_DEVICE.name
            putExtra(AUDIO_DEVICE_TYPE,type)
        }
        startServiceIntent(intent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.TOGGLE_SCREEN_SHARE.name
            putExtra(IS_SCREEN_SHARE,isStarting)
        }
        startServiceIntent(intent)

    }

    fun stopService() {
        val intent = Intent(context,MainService::class.java).apply {
            action = MainServiceActions.STOP_SERVICE.name
        }
        startServiceIntent(intent)
    }
}