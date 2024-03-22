package com.dev.mywebrtc.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dev.mywebrtc.R
import com.dev.mywebrtc.repository.MainRepository
import com.dev.mywebrtc.utils.DataModel
import com.dev.mywebrtc.utils.DataModelType
import com.dev.mywebrtc.utils.FirebaseFieldNames.AUDIO_DEVICE_TYPE
import com.dev.mywebrtc.utils.FirebaseFieldNames.CHANNEL_1
import com.dev.mywebrtc.utils.FirebaseFieldNames.FOREGROUND
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_CALLER
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_SCREEN_SHARE
import com.dev.mywebrtc.utils.FirebaseFieldNames.IS_VIDEO_CALL
import com.dev.mywebrtc.utils.FirebaseFieldNames.SHOULD_BE_MUTED
import com.dev.mywebrtc.utils.FirebaseFieldNames.TARGET
import com.dev.mywebrtc.utils.FirebaseFieldNames.USERNAME
import com.dev.mywebrtc.utils.isValid
import com.dev.mywebrtc.webrtc.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@AndroidEntryPoint
class MainService : Service(), MainRepository.Listener {

    private val TAG = "MainService"
    private var isServiceRunning = false
    private var username: String? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var rtcAudioManager: RTCAudioManager
    private var isPreviousCallStateVideo = true

    @Inject
    lateinit var mainRepository: MainRepository

    companion object {
        var listener: Listener? = null
        var endCallListener: EndCallListener? = null
        var localSurfaceView: SurfaceViewRenderer? = null
        var remoteSurfaceView: SurfaceViewRenderer? = null
        var screenPermissionIntent: Intent? = null
    }


    override fun onCreate() {
        super.onCreate()
        rtcAudioManager = RTCAudioManager.create(this)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { incomingIntent ->
            when (incomingIntent.action) {
                MainServiceActions.START_SERVICE.name -> handleStartService(incomingIntent)
                MainServiceActions.SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                MainServiceActions.END_CALL.name -> handleEndCall()
                MainServiceActions.SWITCH_CAMERA.name -> handleSwitchCamera()
                MainServiceActions.TOGGLE_AUDIO.name -> handleToggleAudio(incomingIntent)
                MainServiceActions.TOGGLE_VIDEO.name -> handleToggleVideo(incomingIntent)
                MainServiceActions.TOGGLE_AUDIO_DEVICE.name -> handleToggleAudioDevice(
                    incomingIntent
                )

                MainServiceActions.TOGGLE_SCREEN_SHARE.name -> handleToggleScreenShare(
                    incomingIntent
                )

                MainServiceActions.START_SERVICE.name -> handleStopService()

                else -> Unit
            }

        }

        return START_STICKY
    }

    private fun handleStopService() {
        mainRepository.endCall()
        mainRepository.logoff {
            isServiceRunning = false
            stopSelf()
        }
    }

    private fun handleToggleScreenShare(incomingIntent: Intent) {
        val isStarting = incomingIntent.getBooleanExtra(IS_SCREEN_SHARE, true)
        if (isStarting) {
            //we should start screen share
            //but we have to keep in mind that we should firstly remove the camera streaming.
            if (isPreviousCallStateVideo) {
                mainRepository.toggleVideo(true)
            }
            mainRepository.setScreenCapture(screenPermissionIntent!!)
            mainRepository.toggleShareScreen(true)
        } else {
            //we should stop screen share and check if camera streaming was on so we should make it on back again
            mainRepository.toggleShareScreen(false)

            if (isPreviousCallStateVideo) {
                mainRepository.toggleVideo(false)
            }
        }

    }

    private fun handleToggleAudioDevice(incomingIntent: Intent) {
        val type = when (incomingIntent.getStringExtra(AUDIO_DEVICE_TYPE)) {
            RTCAudioManager.AudioDevice.EARPIECE.name -> RTCAudioManager.AudioDevice.EARPIECE
            RTCAudioManager.AudioDevice.SPEAKER_PHONE.name -> RTCAudioManager.AudioDevice.SPEAKER_PHONE
            else -> null
        }
        type?.let {
            rtcAudioManager.setDefaultAudioDevice(it)
            rtcAudioManager.selectAudioDevice(it)
            Log.d(TAG, "handleToggleAudioDevice $it")
        }
    }

    private fun handleToggleVideo(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra(SHOULD_BE_MUTED, true)
        this.isPreviousCallStateVideo = !shouldBeMuted
        mainRepository.toggleVideo(shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra(SHOULD_BE_MUTED, true)
        mainRepository.toggleAudio(shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        mainRepository.switchCamera()
    }

    private fun handleEndCall() {
        //1. we have to send a signal to other peer that call is ended
        mainRepository.sendEndCall()
        //2. end out call process and restart our webrtc client
        endCallAndRestartRepository()

    }

    private fun endCallAndRestartRepository() {
        mainRepository.endCall()
        endCallListener?.onCallEnded()
        mainRepository.initWebRtcClient(username!!)
    }


    /*
    * setup views
    * */
    private fun handleSetupViews(incomingIntent: Intent) {
        val isCaller = incomingIntent.getBooleanExtra(IS_CALLER, false)
        val isVideoCall = incomingIntent.getBooleanExtra(IS_VIDEO_CALL, true)
        val target = incomingIntent.getStringExtra(TARGET)

        this.isPreviousCallStateVideo = isVideoCall
        mainRepository.setTarget(target)
        // initialize our widgets and start streaming our video and audio source and get prepared for call
        mainRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        mainRepository.initRemoteSurfaceView(remoteSurfaceView!!)

        if (!isCaller) {
            // start the video call
            mainRepository.startCall()
        }

    }


    /*
    * start our foreground service
    * */
    private fun handleStartService(incomingIntent: Intent) {
        if (!isServiceRunning) {
            isServiceRunning = true
            username = incomingIntent.getStringExtra(USERNAME)
            startServiceWithNotification()

            //setUp my clients

            mainRepository.listener = this
            mainRepository.initFirebase()
            mainRepository.initWebRtcClient(username!!)

        }
    }

    private fun startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_1, FOREGROUND, NotificationManager.IMPORTANCE_HIGH
            )
            val intent = Intent(this, MainServiceReceiver::class.java).apply {
                action = MainServiceActions.ACTION_EXIT.name
            }
            val pendingIntent =
                PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            notificationManager.createNotificationChannel(notificationChannel)

            val notification = NotificationCompat.Builder(this, CHANNEL_1).apply {
                setSmallIcon(R.mipmap.ic_launcher)
                addAction(R.drawable.ic_end_call, "Exit", pendingIntent)
            }

            startForeground(1, notification.build())
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onLatestEventReceived(data: DataModel) {
        Log.d(TAG, "onLatestEventReceived: $data")

        if (data.isValid()) {
            when (data.type) {
                DataModelType.StartAudioCall,
                DataModelType.StartVideoCall -> {
                    listener?.onCallReceived(data)
                }

                else -> Unit
            }
        }
    }

    override fun endCall() {
        //we are receiving end call signal from remote peer
        endCallAndRestartRepository()
    }

    interface EndCallListener {
        fun onCallEnded()
    }

    interface Listener {
        fun onCallReceived(model: DataModel)
    }
}