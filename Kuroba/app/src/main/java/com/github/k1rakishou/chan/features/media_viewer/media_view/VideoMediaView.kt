package com.github.k1rakishou.chan.features.media_viewer.media_view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.cache.CacheHandler
import com.github.k1rakishou.chan.core.manager.GlobalWindowInsetsManager
import com.github.k1rakishou.chan.core.manager.WindowInsetsListener
import com.github.k1rakishou.chan.features.media_viewer.MediaLocation
import com.github.k1rakishou.chan.features.media_viewer.MediaViewerControllerViewModel
import com.github.k1rakishou.chan.features.media_viewer.ViewableMedia
import com.github.k1rakishou.chan.features.media_viewer.helper.CloseMediaActionHelper
import com.github.k1rakishou.chan.features.media_viewer.helper.ExoPlayerWrapper
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableProgressBar
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils
import com.github.k1rakishou.chan.utils.AppModuleAndroidUtils.getString
import com.github.k1rakishou.chan.utils.setVisibilityFast
import com.github.k1rakishou.common.awaitCatching
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.common.findChild
import com.github.k1rakishou.common.isExceptionImportant
import com.github.k1rakishou.common.updateHeight
import com.github.k1rakishou.common.updatePaddings
import com.github.k1rakishou.core_logger.Logger
import com.github.k1rakishou.core_themes.ThemeEngine
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class VideoMediaView(
  context: Context,
  initialMediaViewState: VideoMediaViewState,
  mediaViewContract: MediaViewContract,
  private val viewModel: MediaViewerControllerViewModel,
  private val cacheDataSourceFactory: DataSource.Factory,
  private val onThumbnailFullyLoaded: () -> Unit,
  private val isSystemUiHidden: () -> Boolean,
  override val viewableMedia: ViewableMedia.Video,
  override val pagerPosition: Int,
  override val totalPageItemsCount: Int,
) : MediaView<ViewableMedia.Video, VideoMediaView.VideoMediaViewState>(
  context = context,
  attributeSet = null,
  cacheDataSourceFactory = cacheDataSourceFactory,
  mediaViewContract = mediaViewContract,
  mediaViewState = initialMediaViewState
),
  WindowInsetsListener {

  @Inject
  lateinit var cacheHandler: CacheHandler
  @Inject
  lateinit var globalWindowInsetsManager: GlobalWindowInsetsManager
  @Inject
  lateinit var themeEngine: ThemeEngine

  private val movableContainer: FrameLayout
  private val thumbnailMediaView: ThumbnailMediaView
  private val actualVideoPlayerView: PlayerView
  private val bufferingProgressView: ColorizableProgressBar
  private val mainVideoPlayer = ExoPlayerWrapper(context, cacheDataSourceFactory, mediaViewContract)

  private val closeMediaActionHelper: CloseMediaActionHelper
  private val gestureDetector: GestureDetector
  private val canAutoLoad by lazy { MediaViewerControllerViewModel.canAutoLoad(cacheHandler, viewableMedia) }
  private val fullVideoDeferred = CompletableDeferred<Unit>()

  private var preloadingJob: Job? = null
  private var playJob: Job? = null

  override val hasContent: Boolean
    get() = mainVideoPlayer.isInitialized() && mainVideoPlayer.hasContent

  init {
    AppModuleAndroidUtils.extractActivityComponent(context)
      .inject(this)

    inflate(context, R.layout.media_view_video, this)

    movableContainer = findViewById(R.id.movable_container)
    thumbnailMediaView = findViewById(R.id.thumbnail_media_view)
    actualVideoPlayerView = findViewById(R.id.actual_video_view)
    bufferingProgressView = findViewById(R.id.buffering_progress_view)

    closeMediaActionHelper = CloseMediaActionHelper(
      context = context,
      movableContainer = movableContainer,
      requestDisallowInterceptTouchEvent = { this.parent.requestDisallowInterceptTouchEvent(true) },
      onAlphaAnimationProgress = { alpha ->
        mediaViewContract.changeMediaViewerBackgroundAlpha(alpha)
      },
      closeMediaViewer = { mediaViewContract.closeMediaViewer() }
    )

    gestureDetector = GestureDetector(
      context,
      GestureDetectorListener(
        thumbnailMediaView = thumbnailMediaView,
        actualVideoView = actualVideoPlayerView,
        mediaViewContract = mediaViewContract,
        tryPreloadingFunc = {
          if (viewableMedia.mediaLocation is MediaLocation.Remote && canPreload(forced = true)) {
            preloadingJob = startFullVideoPreloading(viewableMedia.mediaLocation)
            return@GestureDetectorListener true
          }

          return@GestureDetectorListener false
        }
      )
    )

    thumbnailMediaView.setOnTouchListener { v, event ->
      if (thumbnailMediaView.visibility != View.VISIBLE) {
        return@setOnTouchListener false
      }

      // Always return true for thumbnails because otherwise gestures won't work with thumbnails
      gestureDetector.onTouchEvent(event)
      return@setOnTouchListener true
    }

    actualVideoPlayerView.setOnTouchListener { v, event ->
      if (actualVideoPlayerView.visibility != View.VISIBLE) {
        return@setOnTouchListener false
      }

      return@setOnTouchListener gestureDetector.onTouchEvent(event)
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    if (ev != null && closeMediaActionHelper.onInterceptTouchEvent(ev)) {
      return true
    }

    return super.onInterceptTouchEvent(ev)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (closeMediaActionHelper.onTouchEvent(event)) {
      return true
    }

    return super.onTouchEvent(event)
  }

  override fun preload() {
    val previewLocation = viewableMedia.previewLocation
    if (previewLocation != null) {
      thumbnailMediaView.bind(
        ThumbnailMediaView.ThumbnailMediaViewParameters(
          isOriginalMediaPlayable = true,
          thumbnailLocation = previewLocation
        ),
        onThumbnailFullyLoaded = onThumbnailFullyLoaded
      )
    }

    if (viewableMedia.mediaLocation is MediaLocation.Remote && canPreload(forced = false)) {
      preloadingJob = startFullVideoPreloading(viewableMedia.mediaLocation)
    }
  }

  private fun startFullVideoPreloading(mediaLocation: MediaLocation): Job {
    return scope.launch {
      try {
        actualVideoPlayerView.player = mainVideoPlayer.actualExoPlayer
        actualVideoPlayerView.setOnClickListener(null)
        actualVideoPlayerView.useController = true
        actualVideoPlayerView.controllerAutoShow = false
        actualVideoPlayerView.controllerHideOnTouch = false
        actualVideoPlayerView.controllerShowTimeoutMs = -1
        actualVideoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
        actualVideoPlayerView.useArtwork = false
        actualVideoPlayerView.setShutterBackgroundColor(Color.TRANSPARENT)

        if (isSystemUiHidden()) {
          actualVideoPlayerView.hideController()
        } else {
          actualVideoPlayerView.showController()
        }

        updatePlayerControlsInsets()
        updateExoBufferingViewColors()

        val showBufferingJob = scope.launch {
          delay(125L)
          bufferingProgressView.setVisibilityFast(View.VISIBLE)
        }

        mainVideoPlayer.preload(mediaLocation, mediaViewState.prevPosition, mediaViewState.prevWindowIndex)

        showBufferingJob.cancel()
        bufferingProgressView.setVisibilityFast(View.INVISIBLE)

        fullVideoDeferred.complete(Unit)
      } catch (error: Throwable) {
        fullVideoDeferred.completeExceptionally(error)
      } finally {
        preloadingJob = null
      }
    }
  }

  override fun bind() {
    globalWindowInsetsManager.addInsetsUpdatesListener(this)
  }

  override fun show() {
    if (playJob != null) {
      return
    }

    playJob = scope.launch {
      if (hasContent) {
        // Already loaded and ready to play
        switchToPlayerViewAndStartPlaying()
        return@launch
      }

      fullVideoDeferred.awaitCatching()
        .onFailure { error ->
          Logger.e(TAG, "onFullVideoLoadingError()", error)

          if (error.isExceptionImportant()) {
            cancellableToast.showToast(
              context,
              getString(R.string.image_failed_video_error, error.errorMessageOrClassName())
            )
          }
        }
        .onSuccess {
          if (hasContent) {
            switchToPlayerViewAndStartPlaying()
          }
        }
    }
  }

  override fun hide() {
    playJob?.cancel()
    playJob = null

    mediaViewState.prevPosition = mainVideoPlayer.actualExoPlayer.currentPosition
    mediaViewState.prevWindowIndex = mainVideoPlayer.actualExoPlayer.currentWindowIndex

    mainVideoPlayer.pause()
  }

  override fun unbind() {
    thumbnailMediaView.unbind()
    mainVideoPlayer.release()

    preloadingJob?.cancel()
    preloadingJob = null
    globalWindowInsetsManager.removeInsetsUpdatesListener(this)
  }

  override fun onSystemUiVisibilityChanged(systemUIHidden: Boolean) {
    if (systemUIHidden) {
      actualVideoPlayerView.hideController()
    } else {
      actualVideoPlayerView.showController()
    }
  }

  override fun onInsetsChanged() {
    updatePlayerControlsInsets()
  }

  @Suppress("IfThenToSafeAccess")
  private fun updatePlayerControlsInsets() {
    val insetsView = actualVideoPlayerView
      .findChild { childView -> childView.id == R.id.exo_controls_insets_view }
      as? FrameLayout

    if (insetsView != null) {
      insetsView.updateHeight(globalWindowInsetsManager.bottom())
    }

    val rootView = actualVideoPlayerView
      .findChild { childView -> childView.id == R.id.exo_controls_view_root }
      as? LinearLayout

    if (rootView != null) {
      rootView.updatePaddings(
        left = globalWindowInsetsManager.left(),
        right = globalWindowInsetsManager.right()
      )
    }
  }

  private fun updateExoBufferingViewColors() {
    actualVideoPlayerView.findViewById<View>(R.id.exo_buffering)?.let { progressView ->
      (progressView as? ProgressBar)?.progressTintList =
        ColorStateList.valueOf(themeEngine.chanTheme.accentColor)
      (progressView as? ProgressBar)?.indeterminateTintList =
        ColorStateList.valueOf(themeEngine.chanTheme.accentColor)
    }
  }

  private suspend fun switchToPlayerViewAndStartPlaying() {
    actualVideoPlayerView.setVisibilityFast(VISIBLE)
    mainVideoPlayer.startAndAwaitFirstFrame()
    thumbnailMediaView.setVisibilityFast(INVISIBLE)
  }

  private fun canPreload(forced: Boolean): Boolean {
    if (forced) {
      return !fullVideoDeferred.isCompleted
        && (preloadingJob == null || preloadingJob?.isActive == false)
    }

    return canAutoLoad
      && !fullVideoDeferred.isCompleted
      && (preloadingJob == null || preloadingJob?.isActive == false)
  }

  class VideoMediaViewState(var prevPosition: Long = -1, var prevWindowIndex: Int = -1) : MediaViewState {
    override fun clone(): MediaViewState {
      return VideoMediaViewState(prevPosition, prevWindowIndex)
    }

    override fun updateFrom(other: MediaViewState?) {
      if (other == null) {
        prevPosition = -1
        prevWindowIndex = -1
        return
      }

      if (other !is VideoMediaViewState) {
        return
      }

      this.prevPosition = other.prevPosition
      this.prevWindowIndex = other.prevWindowIndex
    }
  }

  class GestureDetectorListener(
    private val thumbnailMediaView: ThumbnailMediaView,
    private val actualVideoView: PlayerView,
    private val mediaViewContract: MediaViewContract,
    private val tryPreloadingFunc: () -> Boolean
  ) : GestureDetector.SimpleOnGestureListener() {

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
      if (actualVideoView.visibility == View.VISIBLE) {
        mediaViewContract.onTapped()
        return true
      } else if (thumbnailMediaView.visibility == View.VISIBLE) {
        return tryPreloadingFunc()
      }

      return super.onSingleTapConfirmed(e)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
      if (e == null || actualVideoView.visibility != View.VISIBLE) {
        return false
      }

      val exoPlayer = actualVideoView.player
        ?: return false

      exoPlayer.playWhenReady = exoPlayer.playWhenReady.not()

      return super.onDoubleTap(e)
    }

  }

  companion object {
    private const val TAG = "VideoMediaView"
  }
}