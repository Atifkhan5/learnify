package com.example.myapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

private val LearnifyGreen = Color(0xFF16A34A)
private val LearnifyDark = Color(0xFF172554)
private val LearnifyGray = Color(0xFF64748B)

data class Lesson(
    val id: Int = 0,
    val title: String = "",
    val youtubeVideoId: String = "",
    val durationMinutes: Int = 0
)

private class YouTubeWebViewContainer(
    context: Context
) : FrameLayout(context) {

    val webView: WebView

    var customView: View? = null
    var customViewCallback: WebChromeClient.CustomViewCallback? = null

    init {

        webView = WebView(context)

        webView.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        addView(webView)
    }
}

@Composable
fun LessonPlayerScreen(
    modifier: Modifier = Modifier,
    courseId: String,
    lesson: Lesson,
    totalLessons: Int,
    isCompleted: Boolean,
    hasNextLesson: Boolean = false,
    onBackClick: () -> Unit = {},
    onLessonCompleted: () -> Unit = {},
    onLessonUncompleted: () -> Unit = {},
    onNextLesson: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var completed by remember(isCompleted) {
        mutableStateOf(isCompleted)
    }

    var playerError by remember(lesson.id) {
        mutableStateOf(false)
    }

    var showCompletionPrompt by remember(lesson.id) {
        mutableStateOf(false)
    }

    var isSavingCompletion by remember(lesson.id) {
        mutableStateOf(false)
    }

    var completionError by remember(lesson.id) {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBackClick
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LearnifyDark
                )
            }
        }

        YouTubeWebView(
            videoId = lesson.youtubeVideoId,
            onError = {
                playerError = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        if (playerError) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 12.dp
                    )
            ) {

                Text(
                    text = "This video couldn't be loaded in the app. Watch it directly on YouTube below.",
                    fontSize = 13.sp,
                    color = LearnifyGray
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Button(
                    onClick = {
                        openInYoutube(
                            context = context,
                            videoId = lesson.youtubeVideoId
                        )
                    }
                ) {

                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text("Watch on YouTube")
                }
            }
        }

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                text = lesson.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LearnifyDark
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "${lesson.durationMinutes} min",
                fontSize = 13.sp,
                color = LearnifyGray
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (completed) {

                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LearnifyGreen
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "Completed",
                        color = LearnifyGreen,
                        fontWeight = FontWeight.SemiBold
                    )

                } else {

                    Text(
                        text = "Watch the lesson to complete it",
                        color = LearnifyGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            if (completionError != null) {

                Text(
                    text = "Couldn't save progress: $completionError",
                    color = Color.Red,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            if (!completed) {

                if (!showCompletionPrompt) {

                    Button(
                        onClick = {
                            showCompletionPrompt = true
                        }
                    ) {
                        Text("Mark lesson progress")
                    }

                } else {

                    Text(
                        text = "Did you finish the lesson?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LearnifyDark
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Button(
                            onClick = {

                                completionError = null
                                isSavingCompletion = true

                                scope.launch {

                                    try {

                                        CourseRepository.markLessonCompleted(
                                            courseId = courseId,
                                            lessonId = lesson.id,
                                            lessonDurationMinutes = lesson.durationMinutes
                                        )

                                        completed = true
                                        showCompletionPrompt = false

                                        onLessonCompleted()

                                        if (hasNextLesson) {
                                            onNextLesson()
                                        }

                                    } catch (exception: Exception) {

                                        completionError =
                                            exception.message
                                                ?: "Something went wrong"

                                    } finally {

                                        isSavingCompletion = false
                                    }
                                }
                            },
                            enabled = !isSavingCompletion,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LearnifyGreen
                            )
                        ) {

                            if (isSavingCompletion) {

                                CircularProgressIndicator(
                                    modifier = Modifier.height(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )

                            } else {

                                Text("Yes")
                            }
                        }

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                showCompletionPrompt = false
                            },
                            enabled = !isSavingCompletion
                        ) {
                            Text("No")
                        }
                    }
                }
            }
        }
    }
}

private fun openInYoutube(
    context: Context,
    videoId: String
) {
    val cleanId = extractYoutubeVideoId(videoId)

    if (cleanId.isBlank()) {
        return
    }

    val youtubeAppIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("vnd.youtube:$cleanId")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {

        context.startActivity(youtubeAppIntent)

    } catch (exception: ActivityNotFoundException) {

        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://www.youtube.com/watch?v=$cleanId"
            )
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {

            context.startActivity(browserIntent)

        } catch (browserException: Exception) {

            Log.e(
                "LearnifyYouTube",
                "Unable to open YouTube",
                browserException
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeWebView(
    videoId: String,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanVideoId = remember(videoId) {
        extractYoutubeVideoId(videoId)
    }

    AndroidView(
        modifier = modifier,

        factory = { context ->

            val container = YouTubeWebViewContainer(context)
            val webView = container.webView

            webView.setBackgroundColor(AndroidColor.BLACK)

            webView.settings.apply {

                javaScriptEnabled = true

                domStorageEnabled = true

                databaseEnabled = true

                mediaPlaybackRequiresUserGesture = false

                javaScriptCanOpenWindowsAutomatically = false

                loadsImagesAutomatically = true

                blockNetworkImage = false

                allowFileAccess = false

                allowContentAccess = true

                setSupportZoom(false)

                builtInZoomControls = false

                displayZoomControls = false

                useWideViewPort = true

                loadWithOverviewMode = false

                cacheMode = WebSettings.LOAD_DEFAULT

                mixedContentMode =
                    WebSettings.MIXED_CONTENT_NEVER_ALLOW

                userAgentString =
                    WebSettings.getDefaultUserAgent(context)
            }

            CookieManager
                .getInstance()
                .setAcceptCookie(true)

            CookieManager
                .getInstance()
                .setAcceptThirdPartyCookies(
                    webView,
                    true
                )

            webView.webChromeClient =
                object : WebChromeClient() {

                    override fun onConsoleMessage(
                        message: ConsoleMessage
                    ): Boolean {

                        Log.d(
                            "LearnifyYouTube",
                            "${message.message()} | " +
                                    "line=${message.lineNumber()} | " +
                                    "source=${message.sourceId()}"
                        )

                        return true
                    }

                    override fun getDefaultVideoPoster(): Bitmap {
                        return Bitmap.createBitmap(
                            1,
                            1,
                            Bitmap.Config.ARGB_8888
                        )
                    }

                    override fun onShowCustomView(
                        view: View?,
                        callback: CustomViewCallback?
                    ) {

                        if (view == null) {
                            return
                        }

                        if (container.customView != null) {
                            callback?.onCustomViewHidden()
                            return
                        }

                        container.customView = view
                        container.customViewCallback = callback

                        webView.visibility = View.GONE

                        val params = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        container.addView(
                            view,
                            params
                        )
                    }

                    override fun onHideCustomView() {

                        val view = container.customView

                        if (view != null) {
                            container.removeView(view)
                        }

                        container.customView = null

                        webView.visibility = View.VISIBLE

                        container.customViewCallback
                            ?.onCustomViewHidden()

                        container.customViewCallback = null
                    }
                }

            webView.webViewClient =
                object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {

                        return false
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {

                        super.onReceivedError(
                            view,
                            request,
                            error
                        )

                        if (
                            request != null &&
                            request.isForMainFrame
                        ) {

                            Log.e(
                                "LearnifyYouTube",
                                "WebView error " +
                                        "${error?.errorCode}: " +
                                        "${error?.description}"
                            )

                            onError()
                        }
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: android.graphics.Bitmap?
                    ) {

                        super.onPageStarted(
                            view,
                            url,
                            favicon
                        )

                        Log.d(
                            "LearnifyYouTube",
                            "Page started: $url"
                        )
                    }

                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {

                        super.onPageFinished(
                            view,
                            url
                        )

                        Log.d(
                            "LearnifyYouTube",
                            "Page finished: $url"
                        )
                    }
                }

            container
        },

        update = { container ->

            val webView = container.webView

            if (cleanVideoId.isBlank()) {

                Log.e(
                    "LearnifyYouTube",
                    "Invalid YouTube ID: $videoId"
                )

                onError()

                return@AndroidView
            }

            val embedUrl =
                "https://www.youtube.com/embed/$cleanVideoId" +
                        "?autoplay=0" +
                        "&playsinline=1" +
                        "&controls=1" +
                        "&rel=0" +
                        "&enablejsapi=1"

            if (webView.tag != embedUrl) {

                webView.tag = embedUrl

                Log.d(
                    "LearnifyYouTube",
                    "Loading: $embedUrl"
                )

                val headers = mapOf(
                    "Referer" to "https://www.google.com/"
                )

                webView.loadUrl(
                    embedUrl,
                    headers
                )
            }
        }
    )
}

private fun extractYoutubeVideoId(
    urlOrId: String
): String {

    var input = urlOrId.trim()

    if (input.isEmpty()) {
        return ""
    }

    input = input
        .removePrefix("\"")
        .removeSuffix("\"")
        .removePrefix("'")
        .removeSuffix("'")
        .trim()

    val directIdPattern =
        Regex(
            "^[a-zA-Z0-9_-]{11}$"
        )

    if (directIdPattern.matches(input)) {
        return input
    }

    try {

        val uri = Uri.parse(input)

        val host =
            uri.host
                ?.lowercase()
                ?: ""

        val path =
            uri.path
                ?: ""

        if (
            host == "youtube.com" ||
            host == "www.youtube.com" ||
            host == "m.youtube.com"
        ) {

            val watchId =
                uri.getQueryParameter("v")

            if (
                watchId != null &&
                directIdPattern.matches(watchId)
            ) {
                return watchId
            }

            if (path.startsWith("/embed/")) {

                val id =
                    path
                        .removePrefix("/embed/")
                        .substringBefore("/")

                if (directIdPattern.matches(id)) {
                    return id
                }
            }

            if (path.startsWith("/shorts/")) {

                val id =
                    path
                        .removePrefix("/shorts/")
                        .substringBefore("/")

                if (directIdPattern.matches(id)) {
                    return id
                }
            }

            if (path.startsWith("/live/")) {

                val id =
                    path
                        .removePrefix("/live/")
                        .substringBefore("/")

                if (directIdPattern.matches(id)) {
                    return id
                }
            }
        }

        if (
            host == "youtu.be" ||
            host == "www.youtu.be"
        ) {

            val id =
                path
                    .removePrefix("/")
                    .substringBefore("/")

            if (directIdPattern.matches(id)) {
                return id
            }
        }

        if (
            host == "youtube-nocookie.com" ||
            host == "www.youtube-nocookie.com"
        ) {

            if (path.startsWith("/embed/")) {

                val id =
                    path
                        .removePrefix("/embed/")
                        .substringBefore("/")

                if (directIdPattern.matches(id)) {
                    return id
                }
            }
        }

    } catch (exception: Exception) {

        Log.e(
            "LearnifyYouTube",
            "URL parsing failed",
            exception
        )
    }

    val patterns = listOf(

        Regex(
            """youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})"""
        ),

        Regex(
            """youtube\.com/embed/([a-zA-Z0-9_-]{11})"""
        ),

        Regex(
            """youtube-nocookie\.com/embed/([a-zA-Z0-9_-]{11})"""
        ),

        Regex(
            """youtu\.be/([a-zA-Z0-9_-]{11})"""
        )
    )

    for (pattern in patterns) {

        val match =
            pattern.find(input)

        if (match != null) {
            return match.groupValues[1]
        }
    }

    return ""
}