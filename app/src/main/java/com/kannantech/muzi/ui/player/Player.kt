/*
 * Copyright (C) 2024 z-huang/InnerTune
 * Copyright (C) 2025 MUZI Project
 *
 * SPDX-License-Identifier: GPL-3.0
 *
 * For any other attributions, refer to the git commit history
 */

package com.kannantech.muzi.ui.player

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.C
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.kannantech.muzi.LocalMenuState
import com.kannantech.muzi.LocalPlayerConnection
import com.kannantech.muzi.R
import com.kannantech.muzi.constants.DEFAULT_PLAYER_BACKGROUND
import com.kannantech.muzi.constants.DarkMode
import com.kannantech.muzi.constants.DarkModeKey
import com.kannantech.muzi.constants.PlayerBackgroundStyle
import com.kannantech.muzi.constants.PlayerBackgroundStyleKey
import com.kannantech.muzi.constants.PlayerHorizontalPadding
import com.kannantech.muzi.constants.QueuePeekHeight
import com.kannantech.muzi.constants.SeekIncrement
import com.kannantech.muzi.constants.SeekIncrementKey
import com.kannantech.muzi.constants.ShowLyricsKey
import com.kannantech.muzi.constants.SwipeToSkipKey
import com.kannantech.muzi.extensions.isPowerSaver
import com.kannantech.muzi.extensions.metadata
import com.kannantech.muzi.extensions.supportsWideScreen
import com.kannantech.muzi.extensions.tabMode
import com.kannantech.muzi.extensions.togglePlayPause
import com.kannantech.muzi.extensions.toggleRepeatMode
import com.kannantech.muzi.playback.PlayerConnection
import com.kannantech.muzi.playback.QueueBoard
import com.kannantech.muzi.ui.component.BottomSheet
import com.kannantech.muzi.ui.component.BottomSheetState
import com.kannantech.muzi.ui.component.PlayerSliderTrack
import com.kannantech.muzi.ui.component.button.IconButton
import com.kannantech.muzi.ui.component.button.ResizableIconButton
import com.kannantech.muzi.ui.component.collapsedAnchor
import com.kannantech.muzi.ui.component.dismissedAnchor
import com.kannantech.muzi.ui.component.rememberBottomSheetState
import com.kannantech.muzi.ui.menu.PlayerMenu
import com.kannantech.muzi.ui.utils.SnapLayoutInfoProvider
import com.kannantech.muzi.utils.coilCoroutine
import com.kannantech.muzi.utils.makeTimeString
import com.kannantech.muzi.utils.rememberEnumPreference
import com.kannantech.muzi.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.max

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val TAG = "BottomSheetPlayer"

    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val queueBoard by playerConnection.service.queueBoard.collectAsState()

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = DEFAULT_PLAYER_BACKGROUND
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val qbInit by playerConnection.service.qbInit.collectAsState()

    LaunchedEffect(qbInit, queueBoard.masterQueues.toList()) {
        if (qbInit && !queueBoard.masterQueues.isEmpty() && state.isDismissed) {
            state.collapseSoft()
        }
    }


    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            PlayerBackground(
                playerConnection = playerConnection,
                playerBackground = playerBackground,
                showLyrics = showLyrics,
                useDarkTheme = useDarkTheme,
            )
        },
        collapsedBackgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        onDismiss = {
            playerConnection.softKillPlayer()
        },
        collapsedContent = {
            MiniPlayer()
        }
    ) {
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE && !context.tabMode() && context.supportsWideScreen()) {
            LandscapePlayer(state, navController, queueBoard)
        } else {
            PortraitPlayer(state, navController, queueBoard)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PortraitPlayer(
    playerSheetState: BottomSheetState,
    navController: NavController,
    queueBoard: QueueBoard,
    enableQueueSheet: Boolean = true,
) {
    val TAG = "BottomSheetPlayer"
    val playerConnection = LocalPlayerConnection.current ?: return

    val dismissedBound = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = playerSheetState.expandedBound,
        collapsedBound = dismissedBound + (QueuePeekHeight * 1.2f),
        initialAnchor = collapsedAnchor,
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(bottom = queueSheetState.collapsedBound)
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .nestedScroll(playerSheetState.preUpPostDownNestedScrollConnection)
        ) {
            val mediaMetadata by playerConnection.mediaMetadata.collectAsState()


            val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
            val canSkipNext by playerConnection.canSkipNext.collectAsState()

            val swipeToSkip by rememberPreference(SwipeToSkipKey, defaultValue = false)
            val previousMediaMetadata = if (swipeToSkip && playerConnection.player.hasPreviousMediaItem()) {
                val previousIndex = playerConnection.player.previousMediaItemIndex
                playerConnection.player.getMediaItemAt(previousIndex).metadata
            } else null


            val nextMediaMetadata = if (swipeToSkip && playerConnection.player.hasNextMediaItem()) {
                val nextIndex = playerConnection.player.nextMediaItemIndex
                playerConnection.player.getMediaItemAt(nextIndex).metadata
            } else null

            val mediaItems = listOfNotNull(previousMediaMetadata, mediaMetadata, nextMediaMetadata)
            val currentMediaIndex = mediaItems.indexOf(mediaMetadata)


            var sliderPosition by remember {
                mutableStateOf<Long?>(null)
            }


            if (!swipeToSkip) {
                Thumbnail(
                    modifier = Modifier
//                                .width(horizontalLazyGridItemWidth)
                        .animateContentSize(),
                    sliderPositionProvider = { sliderPosition },
                    showLyricsOnClick = true,
                    customMediaMetadata = mediaMetadata
                )
            } else {
                val thumbnailLazyGridState = rememberLazyGridState()
                val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
                val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

                LaunchedEffect(itemScrollOffset) {
                    if (!thumbnailLazyGridState.isScrollInProgress || itemScrollOffset != 0) return@LaunchedEffect

                    if (currentItem > currentMediaIndex)
                        playerConnection.player.seekToNext()
                    else if (currentItem < currentMediaIndex)
                        playerConnection.player.seekToPreviousMediaItem()
                }

                LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
                    // When the media item changes, scroll to it
                    val index = maxOf(0, currentMediaIndex)

                    // Only animate scroll when player expanded, otherwise animated scroll won't work
                    if (playerSheetState.isExpanded)
                        thumbnailLazyGridState.animateScrollToItem(index)
                    else
                        thumbnailLazyGridState.scrollToItem(index)
                }

                val horizontalLazyGridItemWidthFactor = 1f
                val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = thumbnailLazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        }
                    )
                }
                val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                LazyHorizontalGrid(
                    state = thumbnailLazyGridState,
                    rows = GridCells.Fixed(1),
                    flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                    userScrollEnabled = playerSheetState.isExpanded,
                    modifier = Modifier.padding(vertical = QueuePeekHeight / 2)
                ) {
                    items(
                        items = mediaItems,
                        key = { it.id }
                    ) {
                        Thumbnail(
                            modifier = Modifier
                                .width(horizontalLazyGridItemWidth)
                                .animateContentSize(),
                            sliderPositionProvider = { sliderPosition },
                            showLyricsOnClick = true,
                            customMediaMetadata = it
                        )
                    }
                }
            }
        }

        ControlsContent(playerSheetState, queueSheetState, navController, queueBoard)


        Spacer(Modifier.height(24.dp))


    }

    if (enableQueueSheet) {
        QueueSheet(
            state = queueSheetState,
            playerBottomSheetState = playerSheetState,
            onTerminate = {
                playerSheetState.dismiss()
                queueBoard.detachedHead = false
            },
            navController = navController
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandscapePlayer(
    playerSheetState: BottomSheetState,
    navController: NavController,
    queueBoard: QueueBoard,
    enableQueueSheet: Boolean = true,
) {
    val TAG = "BottomSheetPlayer"

    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()


    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val swipeToSkip by rememberPreference(SwipeToSkipKey, defaultValue = false)
    val previousMediaMetadata = if (swipeToSkip && playerConnection.player.hasPreviousMediaItem()) {
        val previousIndex = playerConnection.player.previousMediaItemIndex
        playerConnection.player.getMediaItemAt(previousIndex).metadata
    } else null

    val nextMediaMetadata = if (swipeToSkip && playerConnection.player.hasNextMediaItem()) {
        val nextIndex = playerConnection.player.nextMediaItemIndex
        playerConnection.player.getMediaItemAt(nextIndex).metadata
    } else null

    val mediaItems = listOfNotNull(previousMediaMetadata, mediaMetadata, nextMediaMetadata)
    val currentMediaIndex = mediaItems.indexOf(mediaMetadata)


    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    val dismissedBound = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val queueSheetState = rememberBottomSheetState(
        dismissedBound = dismissedBound,
        expandedBound = playerSheetState.expandedBound,
        collapsedBound = dismissedBound,
        initialAnchor = dismissedAnchor,
    )

    val vPadding = max(
        WindowInsets.safeDrawing.getTop(LocalDensity.current),
        WindowInsets.safeDrawing.getBottom(LocalDensity.current)
    )
    val vPaddingDp = with(LocalDensity.current) { vPadding.toDp() }
    val verticalInsets = WindowInsets(left = 0.dp, top = vPaddingDp, right = 0.dp, bottom = vPaddingDp)
    Row(
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).add(verticalInsets)
            )
            .fillMaxSize()
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .nestedScroll(playerSheetState.preUpPostDownNestedScrollConnection)
        ) {
            if (!swipeToSkip) {
                Thumbnail(
                    sliderPositionProvider = { sliderPosition },
                    modifier = Modifier
//                                .width(horizontalLazyGridItemWidth)
                        .animateContentSize(),
                    showLyricsOnClick = true,
                    customMediaMetadata = mediaMetadata
                )
            } else {
                val thumbnailLazyGridState = rememberLazyGridState()
                val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
                val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

                LaunchedEffect(itemScrollOffset) {
                    if (!thumbnailLazyGridState.isScrollInProgress || itemScrollOffset != 0) return@LaunchedEffect

                    if (currentItem > currentMediaIndex)
                        playerConnection.player.seekToNext()
                    else if (currentItem < currentMediaIndex)
                        playerConnection.player.seekToPreviousMediaItem()
                }

                LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
                    // When the media item changes, scroll to it
                    val index = maxOf(0, currentMediaIndex)

                    // Only animate scroll when player expanded, otherwise animated scroll won't work
                    if (playerSheetState.isExpanded)
                        thumbnailLazyGridState.animateScrollToItem(index)
                    else
                        thumbnailLazyGridState.scrollToItem(index)
                }

                val horizontalLazyGridItemWidthFactor = 1f
                val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = thumbnailLazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        }
                    )
                }
                val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor


                LazyHorizontalGrid(
                    state = thumbnailLazyGridState,
                    rows = GridCells.Fixed(1),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                    userScrollEnabled = playerSheetState.isExpanded && swipeToSkip
                ) {
                    items(
                        items = mediaItems,
                        key = { it.id }
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier
                                .width(horizontalLazyGridItemWidth)
                                .animateContentSize(),
                            showLyricsOnClick = true,
                            customMediaMetadata = it
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                // "percentage to half width", not "percentage of width"
                .weight(if (showLyrics) 0.65f else 1f, false)
                .animateContentSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        ) {
            Spacer(Modifier.weight(1f))

            ControlsContent(playerSheetState, queueSheetState, navController, queueBoard, context.supportsWideScreen())

            Spacer(Modifier.weight(1f))
        }
    }

    if (enableQueueSheet) {
        QueueSheet(
            state = queueSheetState,
            playerBottomSheetState = playerSheetState,
            onTerminate = {
                playerSheetState.dismiss()
                queueBoard.detachedHead = false
            },
            navController = navController
        )
    }
}


@Composable
fun ActionButtons(
    playerSheetState: BottomSheetState,
    navController: NavController,
) {
    val TAG = "ActionButtons()"
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current


    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    Spacer(modifier = Modifier.width(10.dp))

    Box(
        modifier = Modifier
            .offset(y = 5.dp)
            .size(42.dp) // Premium larger size
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .clickable {
                playerConnection.toggleLike()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
    ) {
        Icon(
            painter = painterResource(if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
            tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(22.dp),
            contentDescription = null
        )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Box(
        modifier = Modifier
            .offset(y = 5.dp)
            .size(42.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        navController = navController,
                        playerBottomSheetState = playerSheetState,
                        onDismiss = menuState::dismiss
                    )
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center),
            contentDescription = null
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsContent(
    playerSheetState: BottomSheetState,
    queueSheetState: BottomSheetState,
    navController: NavController,
    queueBoard: QueueBoard,
    showQueueHint: Boolean = false,
) {
    val TAG = "ControlsContent()"
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()


    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val playPauseRoundness by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "playPauseRoundness"
    )


    val seekIncrement by rememberEnumPreference(
        key = SeekIncrementKey,
        defaultValue = SeekIncrement.OFF
    )

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = DEFAULT_PLAYER_BACKGROUND
    )


    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.FOLLOW_THEME -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else {
                val c = MaterialTheme.colorScheme.secondary
                c.copy(alpha = 1f, red = c.red - 0.2f, green = c.green - 0.2f, blue = c.blue - 0.2f)
            }
    }


    val playbackState by playerConnection.playbackState.collectAsState()
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }

    var position by remember(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(500)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }


    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    BoxWithConstraints() {
        val maxW = maxWidth
        val compactWidth = maxW < 400.dp
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // action buttons for landscape (above title)
            if (compactWidth) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = PlayerHorizontalPadding, end = PlayerHorizontalPadding, bottom = 16.dp)
                ) {
                    ActionButtons(playerSheetState, navController)
                }
            }

            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata?.title ?: "",
                            style = MaterialTheme.typography.headlineSmall, // Editorial magnitude
                            color = onBackgroundColor,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(0.04f, androidx.compose.ui.unit.TextUnitType.Em),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .basicMarquee(
                                    iterations = 1,
                                    initialDelayMillis = 3000
                                )
                                .clickable(enabled = mediaMetadata?.album != null) {
                                    navController.navigate("album/${mediaMetadata?.album!!.id}")
                                    playerSheetState.collapseSoft()
                                }
                        )

                        Row {
                            mediaMetadata?.artists?.fastForEachIndexed { index, artist ->
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = onBackgroundColor.copy(alpha = 0.8f), // Premium hierarchy
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .basicMarquee(
                                            iterations = 1,
                                            initialDelayMillis = 5000
                                        )
                                        .clickable(enabled = artist.id != null) {
                                            navController.navigate("artist/${artist.id}")
                                            playerSheetState.collapseSoft()
                                        }
                                )

                                if (index != mediaMetadata?.artists?.lastIndex) {
                                    Text(
                                        text = ", ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = onBackgroundColor
                                    )
                                }
                            } ?: Text(
                                text = "",
                                style = MaterialTheme.typography.titleMedium,
                                color = onBackgroundColor,
                                maxLines = 1,
                            )
                        }
                    }

                    // action buttons for portrait (inline with title)
                    if (!compactWidth) {
                        ActionButtons(playerSheetState, navController)
                    }
                }
            }

            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    sliderPosition = it.toLong()
                    // slider too granular for this haptic to feel right
//                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                        position = it
                    }
                    sliderPosition = null
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors()
                    )
                },
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = onBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = onBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle_off,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center),
                        color = onBackgroundColor,
                        enabled = playerConnection.player.currentMediaItem != null,
                        onClick = {
                            playerConnection.triggerShuffle()
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        enabled = canSkipPrevious,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        color = onBackgroundColor,
                        onClick = {
                            if (playerConnection.player.currentMediaItem == null) {
                                queueBoard.setCurrQueue()
                            }
                            playerConnection.player.seekToPrevious()
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                    )
                }

                if (seekIncrement != SeekIncrement.OFF) {
                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = Icons.Rounded.FastRewind,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            color = onBackgroundColor,
                            enabled = playerConnection.player.currentMediaItem != null,
                            onClick = {
                                playerConnection.player.seekTo(playerConnection.player.currentPosition - seekIncrement.millisec)
                            }
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(if (maxW >= 320.dp) if (showLyrics) 56.dp else 72.dp else 42.dp)
                        .animateContentSize()
                        .clip(RoundedCornerShape(playPauseRoundness))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (playerConnection.player.currentMediaItem == null) {
                                queueBoard.setCurrQueue()
                                playerConnection.player.togglePlayPause()
                            } else if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                            // play/pause is slightly harder haptic
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        }
                ) {
                    Image(
                        imageVector = if (playbackState == STATE_ENDED) Icons.Rounded.Replay else if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (seekIncrement != SeekIncrement.OFF) {
                    Box(modifier = Modifier.weight(1f)) {
                        ResizableIconButton(
                            icon = Icons.Rounded.FastForward,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center),
                            color = onBackgroundColor,
                            enabled = playerConnection.player.currentMediaItem != null,
                            onClick = {
                                //ExoPlayer seek increment can only be set in builder
                                //playerConnection.player.seekForward()
                                playerConnection.player.seekTo(playerConnection.player.currentPosition + seekIncrement.millisec)
                            }
                        )
                    }
                }



                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = Icons.Rounded.SkipNext,
                        enabled = canSkipNext,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        color = onBackgroundColor,
                        onClick = {
                            playerConnection.player.seekToNext()
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = when (repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat_off
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center),
                        color = onBackgroundColor,
                        enabled = playerConnection.player.currentMediaItem != null,
                        onClick = {
                            playerConnection.player.toggleRepeatMode()
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        }
                    )
                }
            }

            // queue hint for landscape
            if (showQueueHint) {
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(QueuePeekHeight)
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                queueSheetState.expandSoft()
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            }
                        )
                ) {
                    IconButton(onClick = {
                        queueSheetState.expandSoft()
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandLess,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerBackground(
    playerConnection: PlayerConnection,
    playerBackground: PlayerBackgroundStyle,
    showLyrics: Boolean,
    useDarkTheme: Boolean,
) {
    val TAG = "PlayerBackground"
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation))
            .fillMaxSize()
    ) {

        val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
        var gradientColors by remember {
            mutableStateOf<List<Color>>(emptyList())
        }


        // gradient colours
        LaunchedEffect(mediaMetadata, playerBackground, useDarkTheme) {
            if (playerBackground != PlayerBackgroundStyle.GRADIENT || context.isPowerSaver()) return@LaunchedEffect

            gradientColors = if (useDarkTheme) {
                listOf(Color(0xFF1E1B4B), Color(0xFF000000)) // Deep Indigo to Pure Black
            } else {
                listOf(Color(0xFFE0E7FF), Color(0xFFFFFFFF)) // Soft Indigo to Pure White
            }
        }


        AnimatedContent(
            targetState = mediaMetadata,
            transitionSpec = {
                fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
            }
        ) { metadata ->
            if (playerBackground == PlayerBackgroundStyle.BLUR) {
                AsyncImage(
                    model = metadata?.getThumbnailModel(100, 100),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(100.dp)
                        .alpha(0.5f)
                )
            }
        }

        AnimatedContent(
            targetState = gradientColors,
            transitionSpec = {
                fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
            }
        ) { colors ->
            if (playerBackground == PlayerBackgroundStyle.GRADIENT && colors.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors), alpha = 0.4f)
                )
            }
        }

        if (playerBackground != PlayerBackgroundStyle.FOLLOW_THEME && showLyrics) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (useDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f))
            )
        }
    }
}
