package com.example.myapp

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val LearnifyGreen = Color(0xFF16A34A)
private val LearnifyDark = Color(0xFF172554)
private val LearnifyGray = Color(0xFF64748B)

// Minimal lesson model — adjust fields to match your Firestore "lessons" schema.
data class Lesson(
    val id: Int = 0,
    val title: String = "",
    val youtubeVideoId: String = "",
    val durationMinutes: Int = 0
)

// Fraction of the video that must be watched before a lesson auto-completes.
private const val COMPLETION_THRESHOLD = 0.75

// Injected into YouTube's own embed page after it loads. Polls the page's
// <video> element directly (no custom wrapper page, no CORS issues) and
// calls back into Kotlin once the watch threshold is crossed.
private const val PROGRESS_TRACKING_SCRIPT = """
    (function() {
        if (window.__progressTrackerInstalled) return;
        window.__progressTrackerInstalled = true;
        var reported = false;
        setInterval(function() {
            var video = document.querySelector('video');
            if (video && video.duration > 0) {
                var fraction = video.currentTime / video.duration;
                if (fraction >= $COMPLETION_THRESHOLD && !reported) {
                    reported = true;
                    AndroidBridge.onWatchThresholdReached();
                }
            }
        }, 1000);
    })();
"""

@Composable
fun LessonPlayerScreen(
    modifier: Modifier = Modifier,
    courseId: String,
    lesson: Lesson,
    totalLessons: Int,
    isCompleted: Boolean,
    onBackClick: () -> Unit = {},
    onLessonCompleted: () -> Unit = {},
    onLessonUncompleted: () -> Unit = {}
) {
    var completed by remember(isCompleted) { mutableStateOf(isCompleted) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // Set by the WebView's JS bridge once the watch threshold is crossed.
    var thresholdReached by remember { mutableStateOf(false) }

    LaunchedEffect(thresholdReached) {
        if (thresholdReached && !completed) {
            try {
                CourseProgressRepository.markLessonCompleted(
                    courseId = courseId,
                    lessonId = lesson.id,
                    totalLessons = totalLessons
                )
                completed = true
                onLessonCompleted()
            } catch (exception: Exception) {
                saveError = exception.message ?: "Couldn't save your progress"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LearnifyDark
                )
            }
        }

        YouTubeWebView(
            videoId = lesson.youtubeVideoId,
            onWatchThresholdReached = { thresholdReached = true },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        )

        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                text = lesson.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = LearnifyDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${lesson.durationMinutes} min",
                fontSize = 13.sp,
                color = LearnifyGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (saveError != null) {
                Text(
                    text = "Couldn't update progress: $saveError",
                    color = Color.Red,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = LearnifyGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Completed",
                        color = LearnifyGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Watch 75% of the video to mark this lesson complete automatically",
                        color = LearnifyGray,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
private fun YouTubeWebView(
    videoId: String,
    onWatchThresholdReached: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    // Bridges the WebView's JavaScript back to Kotlin. JS interface callbacks
    // run on a background thread, so we hop back to the main thread before
    // touching Compose state.
    val bridge = remember {
        object {
            @JavascriptInterface
            fun onWatchThresholdReached() {
                mainHandler.post { onWatchThresholdReached() }
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false

                // YouTube's embedded player needs cookies to work correctly.
                // WebView doesn't accept them by default, which is a common
                // cause of "There was a problem with the player configuration".
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                // A default WebView user-agent can be treated differently by
                // YouTube than a normal mobile browser; this avoids that.
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                // Required for YouTube's video surface to actually render
                // instead of showing black in some WebViews.
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                webChromeClient = WebChromeClient()

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(PROGRESS_TRACKING_SCRIPT, null)
                    }
                }

                addJavascriptInterface(bridge, "AndroidBridge")

                loadUrl("https://www.youtube.com/embed/$videoId?playsinline=1&rel=0")
            }
        }
    )
}

// Extracts a YouTube video ID from common URL formats, in case lessons
// store full URLs instead of bare IDs in Firestore.
// Handles: youtu.be/ID, youtube.com/watch?v=ID, youtube.com/embed/ID
fun extractYoutubeVideoId(urlOrId: String): String {
    val watchRegex = Regex("(?:youtu\\.be/|watch\\?v=|embed/)([a-zA-Z0-9_-]{11})")
    val match = watchRegex.find(urlOrId)
    return match?.groupValues?.get(1) ?: urlOrId
}