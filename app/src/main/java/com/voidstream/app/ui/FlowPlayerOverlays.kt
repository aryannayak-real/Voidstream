package com.voidstream.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.voidstream.app.R
import com.voidstream.app.data.audio.eq.EqState
import com.voidstream.app.data.local.MiniBarSwipeAction
import com.voidstream.app.data.local.PlayerPreferences
import com.voidstream.app.data.model.Video
import com.voidstream.app.data.shorts.queue.ShortsQueueSource
import com.voidstream.app.player.EnhancedMusicPlayerManager
import com.voidstream.app.player.EnhancedPlayerManager
import com.voidstream.app.player.GlobalPlayerState
import com.voidstream.app.ui.components.equalizer.LocalEqualizerState
import com.voidstream.app.ui.components.layout.FlowBottomInsets
import com.voidstream.app.ui.components.layout.LocalFlowBottomInsets
import com.voidstream.app.ui.components.music.sheet.LocalMusicMenus
import com.voidstream.app.ui.components.music.sheet.MusicMenuSheets
import com.voidstream.app.ui.components.music.sheet.MusicMenus
import com.voidstream.app.ui.components.musicplayer.sheet.MusicPlayerSheetState
import com.voidstream.app.ui.components.musicplayer.sheet.UnifiedMusicPlayerSheet
import com.voidstream.app.ui.components.shared.LocalMediaOpenOrigins
import com.voidstream.app.ui.components.shared.MediaMiniBarBounds
import com.voidstream.app.ui.components.shared.MediaOpenOrigins
import com.voidstream.app.ui.components.shared.PlayerHaptics
import com.voidstream.app.ui.components.shared.quickactions.QuickActionsHost
import com.voidstream.app.ui.components.shared.quickactions.sharedQuickActionsViewModel
import com.voidstream.app.ui.components.videoplayer.BackgroundQueueTransport
import com.voidstream.app.ui.components.videoplayer.PlayerDraggableState
import com.voidstream.app.ui.components.videoplayer.PlayerSheetValue
import com.voidstream.app.ui.components.videoplayer.SheetOpenOrigin
import com.voidstream.app.ui.components.videoplayer.VideoBackgroundBar
import com.voidstream.app.ui.components.videoplayer.VideoBarSwipeActions
import com.voidstream.app.ui.screens.player.VideoPlayerHost
import com.voidstream.app.ui.screens.player.VideoPlayerViewModel
import com.voidstream.app.ui.screens.player.dialogs.PlayerQueueSheetHost
import com.voidstream.app.ui.screens.player.state.SheetOnNewVideo
import com.voidstream.app.ui.screens.player.state.VideoPlayerUiState
import com.voidstream.app.ui.screens.player.state.sheetOnNewVideo
import com.voidstream.app.ui.screens.playon.RemoteMiniBar
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Keeps the video sheet, its visibility and [GlobalPlayerState] in step with the playback session. */
@Composable
internal fun FlowPlayerSessionEffects(
    playerSheetState: PlayerDraggableState,
    playerViewModel: VideoPlayerViewModel,
    playerUiStateResult: State<VideoPlayerUiState>,
    playerVisibleState: MutableState<Boolean>,
    isInPipMode: Boolean,
    openOrigins: MediaOpenOrigins,
) {
    val activity = LocalContext.current as? ComponentActivity
    val playerUiState by playerUiStateResult
    var playerVisible by playerVisibleState
    val currentIsInPipMode by rememberUpdatedState(isInPipMode)
    val enhancedPlayerManager = remember { EnhancedPlayerManager.getInstance() }
    var autoAdvancedInMiniPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(playerSheetState.currentValue, playerSheetState.isDragging) {
        if (!playerSheetState.isDragging) {
            when (playerSheetState.currentValue) {
                PlayerSheetValue.Expanded -> {
                    GlobalPlayerState.expandMiniPlayer()
                }

                PlayerSheetValue.Collapsed -> {
                    if (playerUiState.isBackgroundPlaybackMode) {
                        GlobalPlayerState.hideMiniPlayer()
                    } else {
                        GlobalPlayerState.collapseMiniPlayer()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        enhancedPlayerManager.autoAdvanceEvent.collect {
            autoAdvancedInMiniPlayer = playerSheetState.currentValue == PlayerSheetValue.Collapsed
        }
    }

    LaunchedEffect(playerViewModel) {
        playerViewModel.expandPlayerRequest.collect {
            val videoId = playerUiState.cachedVideo?.id
            if (!playerVisible && videoId != null) {
                playerSheetState.open(openOrigins.sheetOriginFor(videoId))
            } else {
                playerSheetState.expand()
            }
            playerVisible = true
        }
    }

    LaunchedEffect(playerUiState.cachedVideo?.id, playerUiState.isBackgroundPlaybackMode) {
        if (playerUiState.cachedVideo != null) {
            if (playerUiState.isBackgroundPlaybackMode) {
                playerSheetState.snapTo(PlayerSheetValue.Collapsed)
                GlobalPlayerState.hideMiniPlayer()
                playerVisible = false
                return@LaunchedEffect
            }
            GlobalPlayerState.setExplicitBackgroundPlaybackActive(false)
            val wasExpanded = playerVisible && playerSheetState.currentValue == PlayerSheetValue.Expanded
            playerVisible = true
            val target =
                sheetOnNewVideo(
                    isRestoredSession = playerUiState.isRestoredSession,
                    resumedInMiniPlayer = playerUiState.resumedInMiniPlayer,
                    autoAdvancedInMiniPlayer = autoAdvancedInMiniPlayer,
                    isCollapsed = playerSheetState.currentValue == PlayerSheetValue.Collapsed,
                    wasExpanded = wasExpanded,
                )
            when (target) {
                SheetOnNewVideo.COLLAPSE -> playerSheetState.collapse()
                SheetOnNewVideo.EXPAND -> playerSheetState.expand()
                SheetOnNewVideo.OPEN -> playerSheetState.open(openOrigins.sheetOriginFor(playerUiState.cachedVideo?.id))
            }

            autoAdvancedInMiniPlayer = false
        }
    }

    val dismissRequested by GlobalPlayerState.dismissRequested.collectAsState()
    LaunchedEffect(dismissRequested) {
        if (dismissRequested) {
            GlobalPlayerState.resetDismiss()
            GlobalPlayerState.hideMiniPlayer()
            playerVisible = false
            if (playerUiState.isRestoredSession) {
                playerViewModel.dismissContinueWatching()
            }
            playerViewModel.clearVideo()
            if (currentIsInPipMode) {
                activity?.moveTaskToBack(false)
            }
        }
    }
}

/**
 * The video player and the music player drawn over every page, with the menus and quick actions
 * that open on top of them. The video overlay takes the settled [bottomPadding], not an animated
 * value: it only uses the padding to pick the mini player's resting bounds, and an animated Dp
 * parameter recomposed the whole overlay on every frame of the nav bar animation.
 */
@UnstableApi
@Composable
internal fun FlowPlayerOverlays(
    navController: NavHostController,
    mediaNavigator: FlowMediaNavigator,
    playerViewModel: VideoPlayerViewModel,
    playerUiStateResult: State<VideoPlayerUiState>,
    playerVisibleState: MutableState<Boolean>,
    playerSheetState: PlayerDraggableState,
    activeVideo: Video?,
    isShortsPlayerRoute: Boolean,
    bottomPadding: Dp,
    startInset: Dp,
    miniPlayerScale: Float,
    miniPlayerShowSkipControls: Boolean,
    miniPlayerShowNextPrevControls: Boolean,
    showMusicSheet: Boolean,
    showVideoBar: Boolean,
    showRemoteBar: Boolean,
    musicPlayerSheetState: MusicPlayerSheetState,
    containerWidth: Dp,
    containerHeight: Dp,
    miniBarBounds: MediaMiniBarBounds,
    musicMenus: MusicMenus,
    equalizerState: StateFlow<EqState>,
    bottomInsets: FlowBottomInsets,
    openOrigins: MediaOpenOrigins,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
) {
    val density = LocalDensity.current
    val playerUiState by playerUiStateResult
    var playerVisible by playerVisibleState
    val currentMusicTrack by EnhancedMusicPlayerManager.currentTrack.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerPreferences = remember(context) { PlayerPreferences(context) }
    val swipeLeftAction by playerPreferences.miniBarSwipeLeftAction.collectAsState(initial = MiniBarSwipeAction.CLOSE)
    val swipeRightAction by playerPreferences.miniBarSwipeRightAction.collectAsState(initial = MiniBarSwipeAction.CLOSE)
    val playerHaptics by playerPreferences.playerHapticsEnabled.collectAsState(initial = true)
    val quickActions = sharedQuickActionsViewModel()
    val removedFromQueue = stringResource(R.string.removed_from_queue)
    val scope = rememberCoroutineScope()
    val closeVideo: () -> Unit = {
        playerVisible = false
        playerViewModel.clearVideo()
    }
    val videoBarActions =
        remember(quickActions, playerViewModel, scope, removedFromQueue) {
            VideoBarSwipeActions(
                video = { playerViewModel.uiState.value.cachedVideo },
                quickActions = quickActions,
                playerViewModel = playerViewModel,
                scope = scope,
                removedFromQueue = removedFromQueue,
                onClose = closeVideo,
            )
        }

    CompositionLocalProvider(
        *mediaNavigationLocals(mediaNavigator),
        LocalMediaOpenOrigins provides openOrigins,
        LocalMusicMenus provides musicMenus,
        LocalEqualizerState provides equalizerState,
        LocalFlowBottomInsets provides bottomInsets,
    ) {
        VideoPlayerHost(
            video = activeVideo,
            isVisible = playerVisible && !isShortsPlayerRoute,
            playerSheetState = playerSheetState,
            bottomPadding = bottomPadding.coerceAtLeast(0.dp),
            startInset = startInset,
            miniPlayerScale = miniPlayerScale,
            miniPlayerShowSkipControls = miniPlayerShowSkipControls,
            miniPlayerShowNextPrevControls = miniPlayerShowNextPrevControls,
            onClose = {
                playerVisible = false
                if (playerUiState.isRestoredSession) {
                    playerViewModel.dismissContinueWatching()
                }
                playerViewModel.clearVideo()
            },
            onMinimize = {
                playerSheetState.snapTo(PlayerSheetValue.Collapsed)
                GlobalPlayerState.hideMiniPlayer()
                playerVisible = false
            },
            onNavigateToChannel = mediaNavigator::openChannel,
            onNavigateToShorts = { videoId ->
                playerSheetState.collapse()
                navController.openShorts(ShortsQueueSource.SeededFeed(videoId))
            },
        )

        val barVideo = playerUiState.cachedVideo
        var showBarQueue by remember { mutableStateOf(false) }
        // The grown bar stays over the player while it builds. The player opens from the bar's spot
        // over a full ground, so its first frames match the bar and the bar can simply step aside.
        var barHandoffId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(barHandoffId) {
            if (barHandoffId == null) return@LaunchedEffect
            withTimeoutOrNull(BAR_HANDOFF_TIMEOUT_MS) {
                snapshotFlow { playerSheetState.openOrigin != null }.first { it }
                repeat(BAR_HANDOFF_FRAMES) { withFrameNanos { } }
            }
            barHandoffId = null
        }
        if ((showVideoBar || barHandoffId == barVideo?.id) && barVideo != null) {
            VideoBackgroundBar(
                video = barVideo,
                bounds = miniBarBounds,
                containerWidthPx = with(density) { containerWidth.toPx() },
                containerHeightPx = with(density) { containerHeight.toPx() },
                restingBottomPx = { bottomInsets.miniPlayerBaselinePx(density) },
                onRestore = { videoWindowBounds ->
                    openOrigins.holdOriginFor(barVideo.id, videoWindowBounds)
                    barHandoffId = barVideo.id
                    playerViewModel.showVideoPlayer()
                },
                onClose = closeVideo,
                onOpenQueue = { showBarQueue = true },
                swipeLeftAction = swipeLeftAction,
                swipeRightAction = swipeRightAction,
                swipeActions = videoBarActions,
            )
        }
        if (showRemoteBar) {
            RemoteMiniBar(
                bounds = miniBarBounds,
                containerHeightPx = with(density) { containerHeight.toPx() },
                restingBottomPx = { bottomInsets.miniPlayerBaselinePx(density) },
            )
        }
        if (showVideoBar && showBarQueue) {
            // The service layer keeps a background video audio-only when the queue moves on.
            PlayerQueueSheetHost(
                asSidePanel = false,
                expandedHeight = null,
                onDismiss = { showBarQueue = false },
                loadStreamsInPlayer = true,
                footer = { BackgroundQueueTransport(videoBarActions) },
            )
        }
        LaunchedEffect(showVideoBar) { if (!showVideoBar) showBarQueue = false }

        val track = currentMusicTrack
        if (showMusicSheet && track != null) {
            PlayerHaptics(enabled = playerHaptics) {
                UnifiedMusicPlayerSheet(
                    state = musicPlayerSheetState,
                    containerWidth = containerWidth,
                    containerHeight = containerHeight,
                    miniBounds = miniBarBounds,
                    restingBottomPx = { bottomInsets.miniPlayerBaselinePx(density) },
                    track = track,
                    swipeLeftAction = swipeLeftAction,
                    swipeRightAction = swipeRightAction,
                    onDismiss = {
                        EnhancedMusicPlayerManager.stop()
                        EnhancedMusicPlayerManager.clearCurrentTrack()
                    },
                )
            }
        }

        MusicMenuSheets(musicMenus)
        QuickActionsHost(snackbarHostState)
    }
}

private const val BAR_HANDOFF_TIMEOUT_MS = 2_500L

/** Frames the bar waits after the player starts opening, so the player has drawn under it. */
private const val BAR_HANDOFF_FRAMES = 2

/** The tapped thumbnail for [videoId] when one is in view, else the player rises from below. */
private fun MediaOpenOrigins.sheetOriginFor(videoId: String?): SheetOpenOrigin =
    videoId
        ?.let(::originFor)
        ?.let { SheetOpenOrigin.Thumbnail(it.windowBounds, it.cornerRadiusPx, it.imageKey, it.coversWindow) }
        ?: SheetOpenOrigin.BelowScreen
