package com.example.myapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
            onError = { playerError = true },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        if (playerError) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "This video couldn't be loaded. Watch it on YouTube by pressing the button below.",
                    fontSize = 13.sp,
                    color = LearnifyGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        openInYoutube(context, lesson.youtubeVideoId)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${lesson.durationMinutes} min",
                fontSize = 13.sp,
                color = LearnifyGray
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = "Watch the lesson to complete it",
                        color = LearnifyGray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (completionError != null) {
                Text(
                    text = "Couldn't save progress: $completionError",
                    color = Color.Red,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!completed) {
                if (!showCompletionPrompt) {
                    Button(onClick = { showCompletionPrompt = true }) {
                        Text("Mark lesson progress")
                    }
                } else {
                    Text(
                        text = "Did you finish the lesson?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LearnifyDark
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            exception.message ?: "Something went wrong"
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
                                    color = Color.White
                                )
                            } else {
                                Text("Yes")
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedButton(
                            onClick = { showCompletionPrompt = false },
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

private fun openInYoutube(context: android.content.Context, videoId: String) {
    val webUrl = "https://www.youtube.com/watch?v=$videoId"

    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(appIntent)
    } catch (e: android.content.ActivityNotFoundException) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }
}

private class YoutubeErrorBridge(private val onError: () -> Unit) {
    @JavascriptInterface
    fun onPlayerError(errorCode: String) {
        Handler(Looper.getMainLooper()).post {
            onError()
        }
    }
}

private fun buildEmbedHtml(videoId: String): String = """
    <html>
      <head>
        <style>
          html, body { margin: 0; padding: 0; background: #000; height: 100%; }
          #player { width: 100%; height: 100%; }
        </style>
      </head>
      <body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
          var player;
          function onYouTubeIframeAPIReady() {
            player = new YT.Player('player', {
              videoId: '$videoId',
              playerVars: {
                'playsinline': 1,
                'controls': 1,
                'rel': 0
              },
              events: {
                'onError': function(event) {
                  AndroidBridge.onPlayerError(String(event.data));
                }
              }
            });
          }
        </script>
      </body>
    </html>
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeWebView(
    videoId: String,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            WebView.setWebContentsDebuggingEnabled(true)

            WebView(context).apply {
                setBackgroundColor(AndroidColor.BLACK)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    javaScriptCanOpenWindowsAutomatically = true
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    allowFileAccess = true
                    allowContentAccess = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mixedContentMode =
                        android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                addJavascriptInterface(YoutubeErrorBridge(onError), "AndroidBridge")

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(
                        message: android.webkit.ConsoleMessage
                    ): Boolean {
                        android.util.Log.d(
                            "LearnifyYouTube",
                            "${message.message()} | line=${message.lineNumber()} | source=${message.sourceId()}"
                        )
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.d("LearnifyYouTube", "Page loaded: $url")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        android.util.Log.e(
                            "LearnifyYouTube",
                            "Error ${error?.errorCode}: ${error?.description} URL=${request?.url}"
                        )
                        onError()
                    }
                }

                val id = extractYoutubeVideoId(videoId)

                android.util.Log.d("LearnifyYouTube", "Input: $videoId")
                android.util.Log.d("LearnifyYouTube", "Extracted ID: $id")

                if (id.isNotBlank()) {
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        buildEmbedHtml(id),
                        "text/html",
                        "UTF-8",
                        null
                    )
                } else {
                    android.util.Log.e("LearnifyYouTube", "Invalid YouTube video ID")
                    onError()
                }
            }
        },
        update = { }
    )
}

private fun extractYoutubeVideoId(urlOrId: String): String {
    val input = urlOrId.trim()

    if (input.isEmpty()) {
        return ""
    }

    val patterns = listOf(
        Regex("""(?:youtube\.com/watch\?v=)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:www\.youtube\.com/watch\?v=)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:m\.youtube\.com/watch\?v=)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtu\.be/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:www\.youtu\.be/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtube\.com/embed/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:www\.youtube\.com/embed/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:youtube-nocookie\.com/embed/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:www\.youtube-nocookie\.com/embed/)([a-zA-Z0-9_-]{11})"""),
        Regex("""^([a-zA-Z0-9_-]{11})$""")
    )

    for (pattern in patterns) {
        val match = pattern.find(input)
        if (match != null) {
            return match.groupValues[1]
        }
    }

    return ""
}