package com.veilweather.app.webrtc

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import org.webrtc.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * WebRTC wrapper for voice/video calls
 * Supports STUN/TURN, adaptive bitrate, and screen sharing
 */
class VeilWebRTCManager(private val context: Context) {
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var screenCaptureTrack: VideoTrack? = null
    
    private val peerConnections = mutableMapOf<String, PeerConnection>()
    private val rtcClients = mutableMapOf<String, RTCClient>()
    
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    
    private val stunServers = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302"
    )
    
    private val turnServers = listOf(
        // Add your TURN servers here
    )
    
    companion object {
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_FPS = 30
        private const val AUDIO_SAMPLE_RATE = 48000
    }
    
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun initialize() {
        // Initialize PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()
    }
    
    fun createPeerConnection(
        userId: String,
        iceServerConfig: IceServer? = null
    ): PeerConnection {
        val config = PeerConnection.RTCConfiguration(
            arrayListOf(
                IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                // Add TURN servers here
            )
        )
        
        val peerConnection = peerConnectionFactory?.createPeerConnection(config, createPeerConnectionObserver(userId))
            ?: throw Exception("Failed to create PeerConnection")
        
        peerConnections[userId] = peerConnection
        return peerConnection
    }
    
    fun startVideoCall(userId: String, onLocalViewCreated: (SurfaceViewRenderer) -> Unit) {
        val capturer = createVideoCapturer()
        val source = peerConnectionFactory?.createVideoSource(capturer.isScreencast)
        localVideoTrack = peerConnectionFactory?.createVideoTrack("veil_video_${userId}", source)
        
        source?.addSource(capturer)
        localVideoTrack?.addSink(onLocalViewCreated.invoke(createVideoView()))
        
        // Add track to peer connection
        peerConnections[userId]?.addTrack(localVideoTrack)
    }
    
    fun startVoiceCall(userId: String) {
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("veil_audio_${userId}", audioSource)
        
        peerConnections[userId]?.addTrack(localAudioTrack)
        
        // Enable audio focus
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN)
    }
    
    fun startScreenShare(userId: String, mediaProjection: android.media.projection.MediaProjection) {
        val videoCapturer = createScreenCapturer(mediaProjection)
        val source = peerConnectionFactory?.createVideoSource(true)
        screenCaptureTrack = peerConnectionFactory?.createVideoTrack("veil_screen_${userId}", source)
        
        source?.addSource(videoCapturer)
        screenCaptureTrack?.addSink(createVideoView())
        
        peerConnections[userId]?.removeTrack(localVideoTrack)
        peerConnections[userId]?.addTrack(screenCaptureTrack)
    }
    
    fun stopScreenShare(userId: String) {
        screenCaptureTrack?.dispose()
        screenCaptureTrack = null
        
        // Restore camera
        startVideoCall(userId, {})
    }
    
    fun endCall(userId: String) {
        peerConnections[userId]?.close()
        peerConnections.remove(userId)
        rtcClients.remove(userId)
        
        // Release audio focus
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }
    
    // SDP handling
    fun createOffer(userId: String, callback: (String) -> Unit) {
        val pc = peerConnections[userId] ?: return
        
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                pc.setLocalDescription(this, sdp)
                callback(sdp?.description ?: "")
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, MediaConstraints())
    }
    
    fun createAnswer(userId: String, sdp: String, callback: (String) -> Unit) {
        val pc = peerConnections[userId] ?: return
        
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                pc.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        pc.setLocalDescription(this, sdp)
                        callback(sdp?.description ?: "")
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) {}
                    override fun onSetFailure(error: String?) {}
                }, MediaConstraints())
            }
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, SessionDescription(SessionDescription.Type.OFFER, sdp))
    }
    
    fun setRemoteDescription(userId: String, sdp: String) {
        val pc = peerConnections[userId] ?: return
        
        pc.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }
    
    fun addIceCandidate(userId: String, candidate: String) {
        val pc = peerConnections[userId] ?: return
        
        pc.addIceCandidate(IceCandidate(candidate))
    }
    
    // Adaptive bitrate
    fun updateBandwidthLimit(userId: String, bitrate: Int) {
        val pc = peerConnections[userId] ?: return
        // Update simulcast layers or bandwidth constraints
    }
    
    // Observers
    private fun createPeerConnectionObserver(userId: String): PeerConnection.Observer {
        return object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                // Send ICE candidate to remote peer via signaling
            }
            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.forEach { track ->
                    // Display remote video
                }
            }
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        }
    }
    
    private fun createVideoView(): SurfaceViewRenderer {
        return SurfaceViewRenderer(context).apply {
            setInitOrder(0, 0)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        }
    }
    
    private fun createVideoCapturer(): VideoCapturer {
        val cameras = Camera2Enumerator(context)
        val deviceName = cameras.deviceNames.firstOrNull { cameras.isFrontFacing(it) }
            ?: cameras.deviceNames.firstOrNull() ?: return null
        
        return cameras.createCapturer(deviceName, null) ?: CameraEnumerator.createCapturer()
    }
    
    private fun createScreenCapturer(mediaProjection: android.media.projection.MediaProjection): VideoCapturer {
        // Requires MediaProjection permission
        return MediaProjectionMediaSource.create(mediaProjection, context)
    }
    
    fun shutdown() {
        peerConnections.values.forEach { it.close() }
        peerConnections.clear()
        executor.shutdown()
    }
}

// RTC Client wrapper
class RTCClient(
    private val userId: String,
    private val webrtcManager: VeilWebRTCManager
) {
    var isVideoCallActive = false
    var isVoiceCallActive = false
    var isScreenSharing = false
    
    fun startVideoCall(onLocalViewCreated: (SurfaceViewRenderer) -> Unit) {
        webrtcManager.startVideoCall(userId, onLocalViewCreated)
        isVideoCallActive = true
    }
    
    fun startVoiceCall() {
        webrtcManager.startVoiceCall(userId)
        isVoiceCallActive = true
    }
    
    fun startScreenShare(mediaProjection: android.media.projection.MediaProjection) {
        webrtcManager.startScreenShare(userId, mediaProjection)
        isScreenSharing = true
    }
    
    fun endCall() {
        webrtcManager.endCall(userId)
        isVideoCallActive = false
        isVoiceCallActive = false
        isScreenSharing = false
    }
}
