package com.kannantech.muzi.ui.menu

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.kannantech.muzi.LocalDatabase
import com.kannantech.muzi.LocalDownloadUtil
import com.kannantech.muzi.LocalPlayerConnection
import com.kannantech.muzi.LocalSyncUtils
import com.kannantech.muzi.R
import com.kannantech.muzi.constants.ListThumbnailSize
import com.kannantech.muzi.constants.ThumbnailCornerRadius
import com.kannantech.muzi.db.entities.SongEntity
import com.kannantech.muzi.extensions.toMediaItem
import com.kannantech.muzi.models.MediaMetadata
import com.kannantech.muzi.models.toMediaMetadata
import com.kannantech.muzi.playback.ExoDownloadService
import com.kannantech.muzi.playback.queues.ListQueue
import com.kannantech.muzi.playback.queues.YouTubeQueue
import com.kannantech.muzi.ui.component.button.IconButton
import com.kannantech.muzi.ui.component.items.ListItem
import com.kannantech.muzi.ui.dialog.AddToPlaylistDialog
import com.kannantech.muzi.ui.dialog.AddToQueueDialog
import com.kannantech.muzi.ui.dialog.ArtistDialog
import com.kannantech.muzi.utils.joinByBullet
import com.kannantech.muzi.utils.makeTimeString
import com.kannantech.muzi.utils.syncCoroutine
import com.kannantech.innertube.YouTube
import com.kannantech.innertube.models.SongItem
import kotlinx.coroutines.launch

@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloadUtil = LocalDownloadUtil.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val queueBoard by playerConnection.queueBoard.collectAsState()
    val syncUtils = LocalSyncUtils.current

    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val artists = remember {
        song.artists.mapNotNull {
            it.id?.let { artistId ->
                MediaMetadata.Artist(id = artistId, name = it.name)
            }
        }
    }

    var showChooseQueueDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }


    ListItem(
        title = song.title,
        subtitle = joinByBullet(
            song.artists.joinToString { it.name },
            song.duration?.let { makeTimeString(it * 1000L) }
        ),
        thumbnailContent = {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    database.transaction {
                        librarySong.let { librarySong ->
                            val s: SongEntity
                            if (librarySong == null) {
                                insert(song.toMediaMetadata(), SongEntity::toggleLike)
                                s = song.toMediaMetadata().toSongEntity().let(SongEntity::toggleLike)
                            } else {
                                s = librarySong.song.toggleLike()
                                update(s)
                            }

                            syncUtils.likeSong(s)
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (librarySong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null
                )
            }
        }
    )

    HorizontalDivider()

    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        GridMenuItem(
            icon = Icons.Rounded.Radio,
            title = R.string.start_radio
        ) {
            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()), isRadio = true)
            onDismiss()
        }
        GridMenuItem(
            icon = Icons.Rounded.PlayArrow,
            title = R.string.play
        ) {
            playerConnection.playQueue(
                queue = ListQueue(
                    title = song.title,
                    items = listOf(song.toMediaMetadata())
                )
            )
            onDismiss()
        }
        GridMenuItem(
            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
            title = R.string.play_next
        ) {
            playerConnection.enqueueNext(song.toMediaItem())
            onDismiss()
        }
        GridMenuItem(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = R.string.add_to_queue
        ) {
            showChooseQueueDialog = true
        }
        GridMenuItem(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            title = R.string.add_to_playlist
        ) {
            showChoosePlaylistDialog = true
        }
        DownloadGridMenu(
            localDateTime = download,
            onDownload = {
                database.transaction {
                    insert(song.toMediaMetadata())
                }
                downloadUtil.download(song.toMediaMetadata())
            },
            onRemoveDownload = {
                DownloadService.sendRemoveDownload(
                    context,
                    ExoDownloadService::class.java,
                    song.id,
                    false
                )
            }
        )
        if (artists.isNotEmpty()) {
            GridMenuItem(
                icon = Icons.Rounded.Person,
                title = R.string.view_artist
            ) {
                if (artists.size == 1) {
                    navController.navigate("artist/${artists[0].id}")
                    onDismiss()
                } else {
                    showSelectArtistDialog = true
                }
            }
        }
        song.album?.let { album ->
            GridMenuItem(
                icon = Icons.Rounded.Album,
                title = R.string.view_album
            ) {
                navController.navigate("album/${album.id}")
                onDismiss()
            }
        }
        GridMenuItem(
            icon = Icons.Rounded.Share,
            title = R.string.share
        ) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, song.shareLink)
            }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }
    }

    /**
     * ---------------------------
     * Dialogs
     * ---------------------------
     */

    if (showChooseQueueDialog) {
        AddToQueueDialog(
            onAdd = { queueName ->
                val q = queueBoard.addQueue(
                    queueName, listOf(song.toMediaMetadata()),
                    forceInsert = true, delta = false
                )
                q?.let {
                    queueBoard.setCurrQueue(it)
                }
            },
            onDismiss = {
                showChooseQueueDialog = false
            }
        )
    }

    if (showChoosePlaylistDialog) {
        AddToPlaylistDialog(
            navController = navController,
            songIds = null,
            onPreAdd = { playlist ->
                database.transaction {
                    insert(song.toMediaMetadata())
                }

                coroutineScope.launch(syncCoroutine) {
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, song.id)
                    }
                }

                listOf(song.id)
            },
            onDismiss = { showChoosePlaylistDialog = false }
        )
    }

    if (showSelectArtistDialog) {
        ArtistDialog(
            navController = navController,
            artists = artists,
            onDismiss = { showSelectArtistDialog = false }
        )
    }
}
