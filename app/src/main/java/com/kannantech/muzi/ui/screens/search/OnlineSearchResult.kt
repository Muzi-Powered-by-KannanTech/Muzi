package com.kannantech.muzi.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.kannantech.muzi.LocalMenuState
import com.kannantech.muzi.LocalPlayerAwareWindowInsets
import com.kannantech.muzi.LocalPlayerConnection
import com.kannantech.muzi.LocalSnackbarHostState
import com.kannantech.muzi.R
import com.kannantech.muzi.constants.AppBarHeight
import com.kannantech.muzi.constants.SearchFilterHeight
import com.kannantech.muzi.constants.SwipeToQueueKey
import com.kannantech.muzi.extensions.toMediaItem
import com.kannantech.muzi.extensions.togglePlayPause
import com.kannantech.muzi.models.toMediaMetadata
import com.kannantech.muzi.playback.queues.ListQueue
import com.kannantech.muzi.ui.component.ChipsRow
import com.kannantech.muzi.ui.component.EmptyPlaceholder
import com.kannantech.muzi.ui.component.LazyColumnScrollbar
import com.kannantech.muzi.ui.component.NavigationTitle
import com.kannantech.muzi.ui.component.SwipeToQueueBox
import com.kannantech.muzi.ui.component.button.IconButton
import com.kannantech.muzi.ui.component.items.YouTubeListItem
import com.kannantech.muzi.ui.component.shimmer.ListItemPlaceHolder
import com.kannantech.muzi.ui.component.shimmer.ShimmerHost
import com.kannantech.muzi.ui.menu.YouTubeAlbumMenu
import com.kannantech.muzi.ui.menu.YouTubeArtistMenu
import com.kannantech.muzi.ui.menu.YouTubePlaylistMenu
import com.kannantech.muzi.ui.menu.YouTubeSongMenu
import com.kannantech.muzi.utils.rememberPreference
import com.kannantech.muzi.viewmodels.OnlineSearchViewModel
import com.kannantech.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.kannantech.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.kannantech.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.kannantech.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.kannantech.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.kannantech.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.kannantech.innertube.models.AlbumItem
import com.kannantech.innertube.models.ArtistItem
import com.kannantech.innertube.models.PlaylistItem
import com.kannantech.innertube.models.SongItem
import com.kannantech.innertube.models.YTItem
import kotlinx.coroutines.launch
import java.net.URLDecoder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val swipeEnabled by rememberPreference(SwipeToQueueKey, true)

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val snackbarHostState = LocalSnackbarHostState.current

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem, List<YTItem>) -> Unit =
        { item: YTItem, collection: List<YTItem> ->
            val content: @Composable () -> Unit = {
                YouTubeListItem(
                    item = item,
                    isActive = when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        else -> false
                    },
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
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
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = null
                            )
                        }

                    },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            val songSuggestions = collection.filter { it is SongItem }
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = "${context.getString(R.string.queue_searched_songs_ot)} ${
                                                        URLDecoder.decode(
                                                            viewModel.query,
                                                            "UTF-8"
                                                        )
                                                    }",
                                                    items = songSuggestions.map { (it as SongItem).toMediaMetadata() },
                                                    startIndex = songSuggestions.indexOf(item)
                                                ),
                                                replace = true,
                                            )
                                        }
                                    }

                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            },
                            onLongClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )

                                        else -> {}
                                    }
                                }
                            }
                        )
                        .animateItem()
                )
            }

            if (item !is SongItem) content()
            else SwipeToQueueBox(
                item = item.toMediaItem(),
                swipeEnabled = swipeEnabled,
                snackbarHostState = snackbarHostState,
                content = { content() },
            )
        }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = SearchFilterHeight))
            .asPaddingValues()
    ) {
        if (searchFilter == null) {
            searchSummary?.summaries?.forEach { summary ->
                item {
                    NavigationTitle(summary.title)
                }

                items(
                    items = summary.items,
                    key = { "${summary.title}/${it.id}" }
                ) { item ->
                    ytItemContent(item, summary.items)
                }
            }

            if (searchSummary?.summaries?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Search,
                        text = stringResource(R.string.no_results_found),
                        modifier = Modifier.animateItem()
                    )
                }
            }
        } else {
            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id }
            ) { item ->
                ytItemContent(item, itemsPage?.items.orEmpty())
            }

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = Icons.Rounded.Search,
                        text = stringResource(R.string.no_results_found),
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }
    LazyColumnScrollbar(
        state = lazyListState,
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

    ChipsRow(
        chips = listOf(
            null to stringResource(R.string.filter_all),
            FILTER_SONG to stringResource(R.string.filter_songs),
            FILTER_VIDEO to stringResource(R.string.filter_videos),
            FILTER_ALBUM to stringResource(R.string.filter_albums),
            FILTER_ARTIST to stringResource(R.string.filter_artists),
            FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
            FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists)
        ),
        currentValue = searchFilter,
        onValueUpdate = {
            if (viewModel.filter.value != it) {
                viewModel.filter.value = it
            }
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        },
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top).add(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)))
            .padding(top = AppBarHeight)
    )
}
