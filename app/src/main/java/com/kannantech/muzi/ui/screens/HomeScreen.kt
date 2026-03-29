package com.kannantech.muzi.ui.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.kannantech.muzi.LocalDatabase
import com.kannantech.muzi.LocalMenuState
import com.kannantech.muzi.LocalPlayerAwareWindowInsets
import com.kannantech.muzi.LocalPlayerConnection
import com.kannantech.muzi.R
import com.kannantech.muzi.constants.GridThumbnailHeight
import com.kannantech.muzi.constants.ListItemHeight
import com.kannantech.muzi.constants.ListThumbnailSize
import com.kannantech.muzi.constants.LocalLibraryEnableKey
import com.kannantech.muzi.constants.ThumbnailCornerRadius
import com.kannantech.muzi.db.entities.Album
import com.kannantech.muzi.db.entities.Artist
import com.kannantech.muzi.db.entities.LocalItem
import com.kannantech.muzi.db.entities.Playlist
import com.kannantech.muzi.db.entities.Song
import com.kannantech.muzi.extensions.togglePlayPause
import com.kannantech.muzi.models.toMediaMetadata
import com.kannantech.muzi.playback.queues.ListQueue
import com.kannantech.muzi.playback.queues.YouTubeAlbumRadio
import com.kannantech.muzi.playback.queues.YouTubeQueue
import com.kannantech.muzi.ui.component.ChipsRow
import com.kannantech.muzi.ui.component.HideOnScrollFAB
import com.kannantech.muzi.ui.component.NavigationTile
import com.kannantech.muzi.ui.component.NavigationTitle
import com.kannantech.muzi.ui.component.ScrollToTopManager
import com.kannantech.muzi.ui.component.items.AlbumGridItem
import com.kannantech.muzi.ui.component.items.ArtistGridItem
import com.kannantech.muzi.ui.component.items.SongGridItem
import com.kannantech.muzi.ui.component.items.SongListItem
import com.kannantech.muzi.ui.component.items.YouTubeGridItem
import com.kannantech.muzi.ui.component.shimmer.GridItemPlaceHolder
import com.kannantech.muzi.ui.component.shimmer.ShimmerHost
import com.kannantech.muzi.ui.component.shimmer.TextPlaceholder
import com.kannantech.muzi.ui.menu.AlbumMenu
import com.kannantech.muzi.ui.menu.ArtistMenu
import com.kannantech.muzi.ui.menu.SongMenu
import com.kannantech.muzi.ui.menu.YouTubeAlbumMenu
import com.kannantech.muzi.ui.menu.YouTubeArtistMenu
import com.kannantech.muzi.ui.menu.YouTubePlaylistMenu
import com.kannantech.muzi.ui.menu.YouTubeSongMenu
import com.kannantech.muzi.ui.utils.SnapLayoutInfoProvider
import com.kannantech.muzi.utils.rememberPreference
import com.kannantech.muzi.viewmodels.HomeViewModel
import com.kannantech.innertube.models.AlbumItem
import com.kannantech.innertube.models.ArtistItem
import com.kannantech.innertube.models.PlaylistItem
import com.kannantech.innertube.models.SongItem
import com.kannantech.innertube.models.WatchEndpoint
import com.kannantech.innertube.models.YTItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val density = LocalDensity.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val selectedChip by viewModel.selectedChip.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val localLibEnable by rememberPreference(LocalLibraryEnableKey, defaultValue = true)

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }


    val localGridItem: @Composable (LocalItem, String) -> Unit = { it, source ->
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                val song = it.toMediaMetadata()
                                if (song.isLocal) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = source,
                                            items = listOf(song)
                                        )
                                    )
                                } else {
                                    playerConnection.playQueue(YouTubeQueue.radio(song), isRadio = true)
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                ),
                                isRadio = true,
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    navController = navController,
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val listThumbnailSize = (ListThumbnailSize.value * density.density).roundToInt()

        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        ScrollToTopManager(navController, lazylistState)
        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item {
                ChipsRow(
                    chips = homePage?.chips?.mapNotNull { it to it.title } ?: emptyList(),
                    currentValue = selectedChip,
                    onValueUpdate = {
                        viewModel.toggleChip(it)
                    }
                )
            }



            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.quick_picks),
                        modifier = Modifier
                    )
                }

                item {
                    LazyHorizontalGrid(
                        state = quickPicksLazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * 4 + 16.dp) // Extra height for padding
                    ) {
                        items(
                            items = quickPicks,
                            key = { it.id }
                        ) { originalSong ->
                            SongListItem(
                                song = originalSong,
                                navController = navController,

                                isActive = originalSong.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                inSelectMode = null,
                                isSelected = false,
                                onSelectedChange = {},
                                swipeEnabled = false,

                                thumbnailSize = listThumbnailSize,
                                onPlay = {
                                    playerConnection.playQueue(
                                        YouTubeQueue.radio(originalSong.toMediaMetadata()),
                                        isRadio = true
                                    )
                                },
                                modifier = Modifier.width(horizontalLazyGridItemWidth)
                            )
                        }
                    }
                }
            }

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.forgotten_favorites),
                        modifier = Modifier
                    )
                }

                item {
                    val queueTitle = stringResource(R.string.forgotten_favorites)
                    // take min in case list size is less than 4
                    val rows = min(4, forgottenFavorites.size)
                    LazyHorizontalGrid(
                        state = forgottenFavoritesLazyGridState,
                        rows = GridCells.Fixed(rows),
                        flingBehavior = rememberSnapFlingBehavior(forgottenFavoritesSnapLayoutInfoProvider),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight * rows + 16.dp)
                    ) {
                        itemsIndexed(
                            items = forgottenFavorites,
                            key = { _, song -> song.id }
                        ) { index, originalSong ->
                            SongListItem(
                                song = originalSong,
                                navController = navController,

                                isActive = originalSong.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                inSelectMode = null,
                                isSelected = false,
                                onSelectedChange = {},
                                swipeEnabled = false,

                                thumbnailSize = listThumbnailSize,
                                onPlay = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = queueTitle,
                                            items = forgottenFavorites.map { it.toMediaMetadata() },
                                            startIndex = index
                                        )
                                    )
                                },
                                modifier = Modifier.width(horizontalLazyGridItemWidth)
                            )
                        }
                    }
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.keep_listening),
                        modifier = Modifier
                    )
                }

                item {
                    val rows = if (keepListening.size > 6) 2 else 1
                    LazyHorizontalGrid(
                        state = rememberLazyGridState(),
                        rows = GridCells.Fixed(rows),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((GridThumbnailHeight + 24.dp + with(LocalDensity.current) {
                                MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                        MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                            }) * rows)
                    ) {
                        items(keepListening) {
                            localGridItem(it, stringResource(R.string.keep_listening))
                        }
                    }
                }
            }

            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.your_youtube_playlists),
                        onClick = {
                            navController.navigate("account")
                        },
                        modifier = Modifier
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                    ) {
                        items(
                            items = accountPlaylists,
                            key = { it.id },
                        ) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            similarRecommendations?.forEach {
                item {
                    NavigationTitle(
                        label = stringResource(R.string.similar_to),
                        title = it.title.title,
                        thumbnail = it.title.thumbnailUrl?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (it.title is Artist) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = {
                            when (it.title) {
                                is Song -> navController.navigate("album/${it.title.album!!.id}")
                                is Album -> navController.navigate("album/${it.title.id}")
                                is Artist -> navController.navigate("artist/${it.title.id}")
                                is Playlist -> {}
                            }
                        },
                        modifier = Modifier
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                    ) {
                        items(it.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            homePage?.sections?.forEach {
                item {
                    NavigationTitle(
                        title = it.title,
                        label = it.label,
                        thumbnail = it.thumbnail?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (it.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = it.endpoint?.browseId?.let { browseId ->
                            {
                                if (browseId == "FEmusic_moods_and_genres")
                                    navController.navigate("mood_and_genres")
                                else
                                    navController.navigate("browse/$browseId")
                            }
                        },
                        modifier = Modifier
                    )
                }

                item {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                    ) {
                        items(it.items) { item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            if (homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                item {
                    ShimmerHost(
                        modifier = Modifier
                    ) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .width(250.dp),
                        )
                        LazyRow {
                            items(4) {
                                GridItemPlaceHolder()
                            }
                        }
                    }
                }
            }

            explorePage?.moodAndGenres?.let { moodAndGenres ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.mood_and_genres),
                        onClick = {
                            navController.navigate("mood_and_genres")
                        },
                        modifier = Modifier
                    )
                }
                item {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier
                            .height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp)
                    ) {
                        items(moodAndGenres) {
                            MoodAndGenresButton(
                                title = it.title,
                                onClick = {
                                    navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                },
                                modifier = Modifier
                                    .padding(6.dp)
                                    .width(180.dp)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    ShimmerHost(
                        modifier = Modifier
                    ) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .width(250.dp),
                        )
                        LazyRow {
                            items(4) {
                                GridItemPlaceHolder()
                            }
                        }
                    }
                }
            }
        }
        HideOnScrollFAB(
            visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
            lazyListState = lazylistState,
            icon = Icons.Rounded.Casino,
            onClick = {
                val local = when {
                    allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                    allLocalItems.isNotEmpty() -> true
                    else -> false
                }
                if (local) {
                    when (val luckyItem = allLocalItems.random()) {
                        is Song -> playerConnection.playQueue(
                            YouTubeQueue.radio(luckyItem.toMediaMetadata()),
                            isRadio = true
                        )

                        is Album -> {
                            scope.launch(Dispatchers.IO) {
                                val songs = database.albumSongs(luckyItem.id).first()
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = luckyItem.title,
                                        items = songs.map(Song::toMediaMetadata)
                                    )
                                )
                            }
                        }
                        // not possible, already filtered out
                        is Artist -> {}
                        is Playlist -> {}
                    }
                } else {
                    when (val luckyItem = allYtItems.random()) {
                        is SongItem -> playerConnection.playQueue(
                            YouTubeQueue.radio(luckyItem.toMediaMetadata()),
                            isRadio = true
                        )

                        is AlbumItem -> playerConnection.playQueue(
                            YouTubeAlbumRadio(luckyItem.playlistId),
                            isRadio = true
                        )

                        is ArtistItem -> luckyItem.radioEndpoint?.let {
                            playerConnection.playQueue(YouTubeQueue(it), isRadio = true)
                        }

                        is PlaylistItem -> luckyItem.playEndpoint?.let {
                            playerConnection.playQueue(YouTubeQueue(it), isRadio = true)
                        }
                    }
                }
            }
        )

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}
