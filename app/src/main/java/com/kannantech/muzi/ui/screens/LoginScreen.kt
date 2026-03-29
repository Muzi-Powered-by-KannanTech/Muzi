package com.kannantech.muzi.ui.screens
 
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.kannantech.muzi.LocalPlayerAwareWindowInsets
import com.kannantech.muzi.MainActivity
import com.kannantech.muzi.R
import com.kannantech.muzi.constants.AccountChannelHandleKey
import com.kannantech.muzi.constants.AccountEmailKey
import com.kannantech.muzi.constants.AccountNameKey
import com.kannantech.muzi.constants.DataSyncIdKey
import com.kannantech.muzi.constants.DefaultOpenTabKey
import com.kannantech.muzi.constants.InnerTubeCookieKey
import com.kannantech.muzi.constants.OobeStatusKey
import com.kannantech.muzi.constants.TopBarInsets
import com.kannantech.muzi.constants.OOBE_VERSION
import com.kannantech.muzi.constants.VisitorDataKey
import com.kannantech.muzi.ui.utils.backToMain
import com.kannantech.muzi.utils.dataStore
import com.kannantech.muzi.utils.rememberPreference
import com.kannantech.muzi.utils.reportException
import com.kannantech.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    var visitorData by rememberPreference(VisitorDataKey, "")
    var dataSyncId by rememberPreference(DataSyncIdKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")
    var oobeStatus by rememberPreference(OobeStatusKey, defaultValue = 0)

    var isRedirecting by remember { mutableStateOf(false) }
    var loginDetected by remember { mutableStateOf(false) }

    var webView: WebView? = null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.login)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (webView?.canGoBack() == true) {
                                webView?.goBack()
                            } else {
                                navController.navigateUp()
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = TopBarInsets
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                                loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt.config_.DATASYNC_ID)")

                                val cookie = CookieManager.getInstance().getCookie(url)
                                if (cookie?.contains("SAPISID") == true || cookie?.contains("__Secure-3PAPISID") == true) {
                                    loginDetected = true
                                    if (!isRedirecting) {
                                        isRedirecting = true
                                        GlobalScope.launch {
                                            try {
                                                YouTube.cookie = cookie
                                            } catch (e: Exception) {
                                                Log.e("LoginScreen", "Failed to set YouTube cookie: ${e.message}")
                                            }

                                            YouTube.accountInfo().onSuccess { accountInfo ->
                                                CookieManager.getInstance().flush()
                                                context.dataStore.edit { prefs ->
                                                    prefs[InnerTubeCookieKey] = cookie
                                                    prefs[VisitorDataKey] = visitorData
                                                    prefs[DataSyncIdKey] = dataSyncId
                                                    prefs[AccountNameKey] = accountInfo.name
                                                    prefs[AccountEmailKey] = accountInfo.email.orEmpty()
                                                    prefs[AccountChannelHandleKey] = accountInfo.channelHandle.orEmpty()
                                                    prefs[DefaultOpenTabKey] = Screens.Home.route
                                                    prefs[OobeStatusKey] = OOBE_VERSION
                                                }

                                                launch(Dispatchers.Main) {
                                                    val intent = Intent(context, MainActivity::class.java).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                    (context as? Activity)?.finish()
                                                }
                                            }.onFailure {
                                                context.dataStore.edit { prefs ->
                                                    prefs[InnerTubeCookieKey] = cookie
                                                    prefs[DefaultOpenTabKey] = Screens.Home.route
                                                    prefs[OobeStatusKey] = OOBE_VERSION
                                                }
                                                launch(Dispatchers.Main) {
                                                    val intent = Intent(context, MainActivity::class.java).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                    (context as? Activity)?.finish()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onRetrieveVisitorData(newVisitorData: String?) {
                                if (newVisitorData != null) {
                                    visitorData = newVisitorData
                                }
                            }
                            @JavascriptInterface
                            fun onRetrieveDataSyncId(newDataSyncId: String?) {
                                if (newDataSyncId != null) {
                                    dataSyncId = newDataSyncId.substringBefore("||")
                                }
                            }
                        }, "Android")
                        webView = this
                        loadUrl("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com")
                    }
                },
                update = {
                    it.requestFocus()
                }
            )

            // Loading or Manual Proceed Overlay
            if (loginDetected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isRedirecting) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("Setting up your music...", color = Color.White)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Login Detected!", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                context.startActivity(intent)
                                (context as? Activity)?.finish()
                            }) {
                                Text("Complete Setup")
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
