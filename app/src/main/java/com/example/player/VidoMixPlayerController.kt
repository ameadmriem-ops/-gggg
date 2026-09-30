package com.example.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.core.AppCrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isMuted: Boolean = false,
    val currentQuality: String = "1080p (تلقائي)",
    val isEnded: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val retryAttempt: Int = 0
)

class VidoMixPlayerController(val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var retryJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var currentVideoUrl: String? = null
    private var lastSavedPositionMs: Long = 0L
    private var shouldLoopCurrent: Boolean = false
    private var maxAutoRetries = 3
    private var currentRetryCount = 0

    fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 VidoMix/1.0")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory)

            val renderersFactory = DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // min buffer
                    45000, // max buffer
                    1000,  // buffer for playback
                    2000   // buffer for playback after rebuffer
                )
                .setTargetBufferBytes(C.LENGTH_UNSET)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            exoPlayer = ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val isBuffering = playbackState == Player.STATE_BUFFERING
                            val isEnded = playbackState == Player.STATE_ENDED
                            val dur = duration.coerceAtLeast(0L)
                            
                            if (playbackState == Player.STATE_READY) {
                                currentRetryCount = 0
                                _playerState.value = _playerState.value.copy(
                                    hasError = false,
                                    errorMessage = null,
                                    retryAttempt = 0
                                )
                            }

                            _playerState.value = _playerState.value.copy(
                                isBuffering = isBuffering,
                                isEnded = isEnded,
                                durationMs = if (dur > 0) dur else _playerState.value.durationMs
                            )
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                            if (isPlaying) {
                                startProgressUpdates()
                            } else {
                                stopProgressUpdates()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            AppCrashReporter.recordError("VidoMixPlayer", "VideoPlayer", error, "Playback error: ${error.errorCodeName}")
                            handlePlaybackError(error)
                        }
                    })
                }
        }
        return exoPlayer!!
    }

    private fun handlePlaybackError(error: PlaybackException) {
        stopProgressUpdates()
        _playerState.value = _playerState.value.copy(
            isBuffering = false,
            isPlaying = false
        )

        if (currentRetryCount < maxAutoRetries && currentVideoUrl != null) {
            currentRetryCount++
            _playerState.value = _playerState.value.copy(
                isBuffering = true,
                retryAttempt = currentRetryCount,
                errorMessage = "جارٍ إعادة المحاولة تلقائياً ($currentRetryCount/$maxAutoRetries)..."
            )

            retryJob?.cancel()
            retryJob = scope.launch {
                val backoffDelay = (1000L * currentRetryCount).coerceAtMost(4000L)
                delay(backoffDelay)
                currentVideoUrl?.let { url ->
                    prepareAndPlay(url, autoPlay = true, loop = shouldLoopCurrent, resumeFromLastPosition = true)
                }
            }
        } else {
            _playerState.value = _playerState.value.copy(
                hasError = true,
                isBuffering = false,
                errorMessage = "تعذر تشغيل الفيديو. يرجى التحقق من اتصال الإنترنت والمحاولة مجدداً."
            )
        }
    }

    fun retry() {
        currentRetryCount = 0
        currentVideoUrl?.let {
            prepareAndPlay(it, autoPlay = true, loop = shouldLoopCurrent, resumeFromLastPosition = true)
        }
    }

    fun prepareAndPlay(
        videoUrl: String,
        autoPlay: Boolean = true,
        loop: Boolean = false,
        resumeFromLastPosition: Boolean = false
    ) {
        val isNewUrl = currentVideoUrl != videoUrl
        if (isNewUrl) {
            lastSavedPositionMs = 0L
            currentRetryCount = 0
        }
        currentVideoUrl = videoUrl
        shouldLoopCurrent = loop

        _playerState.value = _playerState.value.copy(
            hasError = false,
            errorMessage = null
        )

        try {
            val player = getPlayer()
            if (isNewUrl) {
                player.stop()
            }
            player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(videoUrl))
            if (videoUrl.contains(".m3u8", ignoreCase = true)) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            val mediaItem = mediaItemBuilder.build()

            player.setMediaItem(mediaItem)
            player.prepare()

            if (resumeFromLastPosition && lastSavedPositionMs > 0L) {
                player.seekTo(lastSavedPositionMs)
            }

            player.playWhenReady = autoPlay
        } catch (e: Throwable) {
            AppCrashReporter.recordError("VidoMixPlayer", "PrepareAndPlay", e)
            handlePlaybackError(PlaybackException("Failed to prepare: ${e.message}", e, PlaybackException.ERROR_CODE_UNSPECIFIED))
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    lastSavedPositionMs = pos
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else _playerState.value.durationMs
                    )
                }
                delay(300)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (_playerState.value.isEnded) {
                    player.seekTo(0)
                }
                player.play()
            }
        }
    }

    fun pausePlayback() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    fun resumePlayback() {
        exoPlayer?.let { player ->
            if (!player.isPlaying && !_playerState.value.hasError) {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        lastSavedPositionMs = positionMs
        exoPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun forward10Seconds() {
        exoPlayer?.let { player ->
            val target = (player.currentPosition + 10_000L).coerceAtMost(player.duration)
            player.seekTo(target)
            lastSavedPositionMs = target
        }
    }

    fun rewind10Seconds() {
        exoPlayer?.let { player ->
            val target = (player.currentPosition - 10_000L).coerceAtLeast(0L)
            player.seekTo(target)
            lastSavedPositionMs = target
        }
    }

    fun setSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    fun toggleMute() {
        exoPlayer?.let { player ->
            val shouldMute = !_playerState.value.isMuted
            player.volume = if (shouldMute) 0f else 1f
            _playerState.value = _playerState.value.copy(isMuted = shouldMute)
        }
    }

    fun setQualityLabel(quality: String) {
        _playerState.value = _playerState.value.copy(currentQuality = quality)
    }

    fun release() {
        stopProgressUpdates()
        retryJob?.cancel()
        retryJob = null
        try {
            exoPlayer?.release()
        } catch (e: Exception) {
            Log.w("VidoMixPlayer", "Player release note: ${e.message}")
        }
        exoPlayer = null
    }
}
