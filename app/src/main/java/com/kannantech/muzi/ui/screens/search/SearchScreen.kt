package com.kannantech.muzi.ui.screens.search

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kannantech.muzi.LocalDatabase
import com.kannantech.muzi.LocalPlayerAwareWindowInsets
import com.kannantech.muzi.LocalPlayerConnection
import com.kannantech.muzi.R
import com.kannantech.muzi.constants.DEFAULT_ENABLED_TABS
import com.kannantech.muzi.constants.EnabledTabsKey
import com.kannantech.muzi.constants.PauseSearchHistoryKey
import com.kannantech.muzi.constants.SearchSource
import com.kannantech.muzi.constants.SearchSourceKey
import com.kannantech.muzi.constants.UpdateAvailableKey
import com.kannantech.muzi.db.entities.SearchHistory
import com.kannantech.muzi.extensions.tabMode
import com.kannantech.muzi.ui.component.SearchBar
import com.kannantech.muzi.ui.component.button.IconButton
import com.kannantech.muzi.ui.screens.Screens
import com.kannantech.muzi.utils.dataStore
import com.kannantech.muzi.utils.get
import com.kannantech.muzi.utils.rememberEnumPreference
import com.kannantech.muzi.utils.rememberPreference
import com.kannantech.muzi.utils.urlEncode
import com.kannantech.muzi.youtubeNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarContainer(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = LocalDatabase.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current

    val enabledTabs by rememberPreference(EnabledTabsKey, defaultValue = DEFAULT_ENABLED_TABS)
    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
    val updateAvailable by rememberPreference(UpdateAvailableKey, defaultValue = false)

    val navigationItems = remember { Screens.getScreens(enabledTabs) }
    val searchBarFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    val (query, onQueryChange) = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    var searchActive by rememberSaveable {
        mutableStateOf(false)
    }
    val onSearchActiveChange: (Boolean) -> Unit = { newActive ->
        searchActive = newActive
        if (!newActive) {
            focusManager.clearFocus()
            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                onQueryChange(TextFieldValue())
            }
        }
    }

    val onSearch: (String) -> Unit = {
        if (it.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Physics Pulse
            if (searchSource == SearchSource.LOCAL) {
                focusManager.clearFocus(true)
            } else {
                onSearchActiveChange(false)
                if (youtubeNavigator(
                        context,
                        navController,
                        coroutineScope,
                        playerConnection,
                        snackbarHostState,
                        it.toUri()
                    )
                ) {
                    // don't do anything
                } else {
                    navController.navigate("search/${it.urlEncode()}")
                    if (context.dataStore[PauseSearchHistoryKey] != true) {
                        database.query {
                            insert(SearchHistory(query = it))
                        }
                    }
                }
            }
        }
    }


    val shouldShowSearchBar = remember(searchActive, navBackStackEntry) {
        (searchActive || navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                navBackStackEntry?.destination?.route?.startsWith("search/") == true)
    }

    LaunchedEffect(navBackStackEntry) {
        if (searchActive) {
            onSearchActiveChange(false)
        }
    }

    AnimatedVisibility(
        visible = shouldShowSearchBar,
        enter = fadeIn(tween(350)) + scaleIn(initialScale = 0.95f, animationSpec = tween(350)),
        exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.95f, animationSpec = tween(250))
    ) {
        val searchBarInset = if (!context.tabMode()) {
            WindowInsets.safeDrawing.union(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Start))
        }
        else {
            WindowInsets()
        }
        // Signature Obsidian Elite SearchBar colors
        val premiumColors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            dividerColor = Color.Transparent, // Clean glass look
            inputFieldColors = SearchBarDefaults.inputFieldColors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedPlaceholderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        )
        SearchBar(
            modifier = Modifier
                .then(if (!searchActive) Modifier.padding(horizontal = 16.dp) else Modifier)
                .drawBehind {
                    if (searchActive) {
                        // Liquid Gradient Border (Indigo to Violet)
                        val brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7C3AED), // Indigo
                                Color(0xFFA855F7)  // Violet
                            )
                        )
                        drawRoundRect(
                            brush = brush,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                },
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            active = searchActive,
            onActiveChange = onSearchActiveChange,
            scrollBehavior = scrollBehavior,
            colors = premiumColors,
            placeholder = {
                Text(
                    text = stringResource(
                        if (!searchActive) R.string.search
                        else when (searchSource) {
                            SearchSource.LOCAL -> R.string.search_library
                            SearchSource.ONLINE -> R.string.search_yt_music
                        }
                    ),
                    fontWeight = if (searchActive) FontWeight.Normal else FontWeight.Medium,
                )
            },
            leadingIcon = {
                IconButton(
                    onClick = {
                        when {
                            searchActive -> onSearchActiveChange(false)

                            !searchActive && navBackStackEntry?.destination?.route?.startsWith(
                                "search"
                            ) == true -> {
                                navController.navigateUp()
                            }

                            else -> onSearchActiveChange(true)
                        }
                    },
                ) {
                    Icon(
                        imageVector =
                            if (searchActive || navBackStackEntry?.destination?.route?.startsWith("search") == true) {
                                Icons.AutoMirrored.Rounded.ArrowBack
                            } else {
                                Icons.Rounded.Search
                            },
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                if (searchActive) {
                    if (query.text.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange(TextFieldValue("")) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            searchSource =
                                if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                        }
                    ) {
                        Icon(
                            imageVector = when (searchSource) {
                                SearchSource.LOCAL -> Icons.Rounded.LibraryMusic
                                SearchSource.ONLINE -> Icons.Rounded.Language
                            },
                            contentDescription = null
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            },
            windowInsets = searchBarInset,
            focusRequester = searchBarFocusRequester,
        ) {
            Crossfade(
                targetState = searchSource,
                label = "",
                animationSpec = tween(durationMillis = 300),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) { searchSource ->
                when (searchSource) {
                    SearchSource.LOCAL -> LocalSearchScreen(
                        query = query.text,
                        navController = navController,
                        onDismiss = { onSearchActiveChange(false) },
                    )

                    SearchSource.ONLINE -> OnlineSearchScreen(
                        query = query.text,
                        onQueryChange = onQueryChange,
                        navController = navController,
                        onSearch = {
                            if (youtubeNavigator(
                                    context,
                                    navController,
                                    coroutineScope,
                                    playerConnection,
                                    snackbarHostState,
                                    it.toUri()
                                )
                            ) {
                                return@OnlineSearchScreen
                            } else {
                                navController.navigate("search/${it.urlEncode()}")
                                if (context.dataStore[PauseSearchHistoryKey] != true) {
                                    database.query {
                                        insert(SearchHistory(query = it))
                                    }
                                }
                            }
                        },
                        onDismiss = { onSearchActiveChange(false) },
                    )
                }
            }
        }
    }
}
