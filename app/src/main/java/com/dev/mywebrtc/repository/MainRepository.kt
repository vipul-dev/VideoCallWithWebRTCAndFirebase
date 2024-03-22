package com.dev.mywebrtc.repository

import android.content.Intent
import com.dev.mywebrtc.firebaseclient.FirebaseClient
import com.dev.mywebrtc.utils.DataModel
import com.dev.mywebrtc.utils.DataModelType
import com.dev.mywebrtc.utils.UserStatus
import com.dev.mywebrtc.webrtc.MyPeerObserver
import com.dev.mywebrtc.webrtc.WebRtcClient
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRtcClient: WebRtcClient,
    private val gson: Gson
) : WebRtcClient.Listener {

    var listener: Listener? = null
    private var target: String? = null
    private var remoteSurfaceView: SurfaceViewRenderer? = null

    fun login(username: String, password: String, isDone: (Boolean, String?) -> Unit) {
        firebaseClient.login(username, password, isDone)
    }

    fun observeUsersStatus(status: (List<Pair<String, String>>) -> Unit) {
        firebaseClient.observeUsersStatus(status)
    }


    fun initFirebase() {
        firebaseClient.subscribeForLatestEvent(object : FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                listener?.onLatestEventReceived(event)
                when (event.type) {
                    /*
                    * Start,Offer,Answer call using WebRTC
                    * */
                    DataModelType.Offer -> {
                        webRtcClient.onRemoteSessionReceived(
                            SessionDescription(SessionDescription.Type.OFFER, event.data.toString())
                        )
                        webRtcClient.answer(target!!)
                    }

                    DataModelType.Answer -> {
                        webRtcClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data.toString()
                            )
                        )
                    }

                    DataModelType.IceCandidates -> {
                        val candidate: IceCandidate? = try {
                            gson.fromJson(event.data.toString(), IceCandidate::class.java)
                        } catch (e: Exception) {
                            null
                        }

                        candidate?.let {
                            webRtcClient.addIceCandidateToPeer(it)
                        }
                    }

                    DataModelType.EndCall -> {
                        listener?.endCall()
                    }

                    else -> Unit
                }
            }

        })
    }


    fun sendConnectionRequest(username: String, isVideoCall: Boolean, success: (Boolean) -> Unit) {
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                type = if (isVideoCall) DataModelType.StartVideoCall else DataModelType.StartAudioCall,
                target = username
            ), success
        )

    }

    fun setTarget(target: String?) {
        this.target = target

    }

    interface Listener {
        fun onLatestEventReceived(event: DataModel)
        fun endCall()
    }

    fun initWebRtcClient(username: String) {
        webRtcClient.listener = this
        webRtcClient.initializeWebrtcClient(username, object : MyPeerObserver() {

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                try {
                    p0?.videoTracks?.get(0)?.addSink(remoteSurfaceView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    webRtcClient.sendIceCandidate(target!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    //1. change my status to in call
                    changeMyStatus(UserStatus.IN_CALL)
                    //2. clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                }
            }
        })
    }

    private fun changeMyStatus(status: UserStatus) {
        firebaseClient.changeMyStatus(status)
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, isVideoCall: Boolean) {
        webRtcClient.initLocalSurfaceView(view, isVideoCall)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRtcClient.initRemoteSurfaceView(view)
        this.remoteSurfaceView = view
    }

    fun startCall() {
        webRtcClient.call(target!!)
    }

    fun endCall() {
        webRtcClient.closeConnection()
        changeMyStatus(UserStatus.ONLINE)
    }

    fun sendEndCall() {
        onTransferEventToSocket(
            DataModel(
                type = DataModelType.EndCall,
                target = target
            )
        )
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRtcClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRtcClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera() {
        webRtcClient.switchCamera()
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data) {}
    }

    fun setScreenCapture(screenPermissionIntent: Intent) {
        webRtcClient.setPermissionIntent(screenPermissionIntent)
    }

    fun toggleShareScreen(isStarting: Boolean) {
        if (isStarting) {
            webRtcClient.startScreenCapturing()
        } else {
            webRtcClient.stopScreenCapturing()
        }
    }

    fun logoff(function: () -> Unit) = firebaseClient.logOff(function)


}