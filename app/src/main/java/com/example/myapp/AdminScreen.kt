
package com.example.myapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val AdminBlue = Color(0xFF2563EB)
private val AdminDark = Color(0xFF172554)
private val AdminText = Color(0xFF1E293B)
private val AdminGray = Color(0xFF64748B)
private val AdminBackground = Color(0xFFF6F8FC)
private val AdminCard = Color.White
private val AdminGreen = Color(0xFF16A34A)
private val AdminRed = Color(0xFFDC2626)
private val AdminOrange = Color(0xFFF59E0B)
private val AdminPurple = Color(0xFF7C3AED)

private data class AdminStudent(
    val id: String,
    val name: String,
    val email: String
)

private data class AdminLesson(
    val id: String,
    val title: String,
    val description: String,
    val youtubeVideoId: String,
    val durationMinutes: Int,
    val order: Int
)

private data class YoutubeVideoInfo(
    val id: String,
    val title: String,
    val durationMinutes: Int
)

private fun extractYoutubePlaylistId(
    input: String
): String? {
    val value = input.trim()

    if (value.isBlank()) {
        return null
    }

    val playlistRegex =
        Regex("""[?&]list=([a-zA-Z0-9_-]+)""")

    val match =
        playlistRegex.find(value)

    return match?.groupValues?.getOrNull(1)
}

private suspend fun getYoutubeDurationMinutes(
    videoId: String
): Int {
    return withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_API_KEY

        if (apiKey.isBlank()) {
            throw Exception(
                "YouTube API key is not configured."
            )
        }

        val url =
            "https://www.googleapis.com/youtube/v3/videos" +
                    "?part=contentDetails" +
                    "&id=$videoId" +
                    "&key=$apiKey"

        val connection =
            URL(url).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode =
                connection.responseCode

            if (
                responseCode !=
                HttpURLConnection.HTTP_OK
            ) {
                throw Exception(
                    "Unable to retrieve YouTube video information."
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val json =
                JSONObject(response)

            val items =
                json.optJSONArray("items")

            if (
                items == null ||
                items.length() == 0
            ) {
                throw Exception(
                    "YouTube video was not found."
                )
            }

            val contentDetails =
                items
                    .getJSONObject(0)
                    .getJSONObject("contentDetails")

            val duration =
                contentDetails.getString("duration")

            parseYoutubeDuration(duration)
        } finally {
            connection.disconnect()
        }
    }
}

private suspend fun getYoutubePlaylistVideos(
    playlistId: String
): List<YoutubeVideoInfo> {
    return withContext(Dispatchers.IO) {
        val apiKey =
            BuildConfig.YOUTUBE_API_KEY

        if (apiKey.isBlank()) {
            throw Exception(
                "YouTube API key is not configured."
            )
        }

        val videoIds =
            mutableListOf<String>()

        var nextPageToken: String? = null

        do {
            val pageTokenPart =
                if (
                    nextPageToken.isNullOrBlank()
                ) {
                    ""
                } else {
                    "&pageToken=$nextPageToken"
                }

            val url =
                "https://www.googleapis.com/youtube/v3/playlistItems" +
                        "?part=snippet" +
                        "&maxResults=50" +
                        "&playlistId=$playlistId" +
                        pageTokenPart +
                        "&key=$apiKey"

            val connection =
                URL(url).openConnection()
                        as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {
                    throw Exception(
                        "Unable to retrieve YouTube playlist."
                    )
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                val json =
                    JSONObject(response)

                val items =
                    json.optJSONArray("items")

                if (items != null) {
                    for (
                    index in
                    0 until items.length()
                    ) {
                        val item =
                            items.getJSONObject(index)

                        val snippet =
                            item.getJSONObject("snippet")

                        val resourceId =
                            snippet.optJSONObject(
                                "resourceId"
                            )

                        val videoId =
                            resourceId
                                ?.optString("videoId")
                                ?: ""

                        if (videoId.isNotBlank()) {
                            videoIds.add(videoId)
                        }
                    }
                }

                nextPageToken =
                    json.optString(
                        "nextPageToken",
                        ""
                    )
            } finally {
                connection.disconnect()
            }
        } while (
            !nextPageToken.isNullOrBlank()
        )

        if (videoIds.isEmpty()) {
            throw Exception(
                "No videos were found in this playlist."
            )
        }

        val result =
            mutableListOf<YoutubeVideoInfo>()

        videoIds
            .chunked(50)
            .forEach { batch ->

                val ids =
                    batch.joinToString(",")

                val url =
                    "https://www.googleapis.com/youtube/v3/videos" +
                            "?part=contentDetails,snippet" +
                            "&id=$ids" +
                            "&key=$apiKey"

                val connection =
                    URL(url).openConnection()
                            as HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    val responseCode =
                        connection.responseCode

                    if (
                        responseCode !=
                        HttpURLConnection.HTTP_OK
                    ) {
                        throw Exception(
                            "Unable to retrieve playlist video information."
                        )
                    }

                    val response =
                        connection.inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    val json =
                        JSONObject(response)

                    val items =
                        json.optJSONArray("items")

                    if (items != null) {
                        for (
                        index in
                        0 until items.length()
                        ) {
                            val item =
                                items.getJSONObject(index)

                            val id =
                                item.optString("id")

                            val snippet =
                                item.optJSONObject("snippet")

                            val title =
                                snippet?.optString("title")
                                    ?: "Untitled Lesson"

                            val contentDetails =
                                item.optJSONObject(
                                    "contentDetails"
                                )

                            val duration =
                                contentDetails
                                    ?.optString("duration")
                                    ?: "PT0S"

                            result.add(
                                YoutubeVideoInfo(
                                    id = id,
                                    title = title,
                                    durationMinutes =
                                        parseYoutubeDuration(
                                            duration
                                        )
                                )
                            )
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }

        videoIds.mapNotNull { videoId ->
            result.find {
                it.id == videoId
            }
        }
    }
}

private fun parseYoutubeDuration(
    duration: String
): Int {
    var hours = 0
    var minutes = 0
    var seconds = 0

    val regex =
        Regex(
            "PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?"
        )

    val match =
        regex.matchEntire(duration)
            ?: return 0

    hours =
        match.groupValues[1]
            .takeIf {
                it.isNotBlank()
            }
            ?.toInt()
            ?: 0

    minutes =
        match.groupValues[2]
            .takeIf {
                it.isNotBlank()
            }
            ?.toInt()
            ?: 0

    seconds =
        match.groupValues[3]
            .takeIf {
                it.isNotBlank()
            }
            ?.toInt()
            ?: 0

    return (
            hours * 60 +
                    minutes +
                    if (seconds >= 30) 1 else 0
            )
}

@Composable
fun AdminScreen(
    modifier: Modifier = Modifier
) {
    if (!CourseRepository.isCurrentUserAdmin()) {
        Box(
            modifier =
                modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {
            Card(
                modifier =
                    Modifier.padding(24.dp),
                shape =
                    RoundedCornerShape(18.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    )
            ) {
                Column(
                    modifier =
                        Modifier.padding(28.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.School,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(48.dp),
                        tint =
                            AdminRed
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "Access Denied",
                        fontSize =
                            20.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            AdminDark
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "You don't have permission to access the admin dashboard.",
                        fontSize =
                            13.sp,
                        color =
                            AdminGray
                    )
                }
            }
        }

        return
    }

    val firestore =
        remember {
            FirebaseFirestore.getInstance()
        }

    val scope =
        rememberCoroutineScope()

    var selectedSection by remember {
        mutableIntStateOf(0)
    }

    var courses by remember {
        mutableStateOf<List<Course>>(
            emptyList()
        )
    }

    var students by remember {
        mutableStateOf<List<AdminStudent>>(
            emptyList()
        )
    }

    var selectedCourseForLessons by remember {
        mutableStateOf<Course?>(null)
    }

    var lessons by remember {
        mutableStateOf<List<AdminLesson>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    var globalError by remember {
        mutableStateOf<String?>(null)
    }

    var totalLessons by remember {
        mutableIntStateOf(0)
    }

    var completedRecords by remember {
        mutableIntStateOf(0)
    }

    var courseToDelete by remember {
        mutableStateOf<Course?>(null)
    }

    var studentToDelete by remember {
        mutableStateOf<AdminStudent?>(null)
    }

    var lessonToDelete by remember {
        mutableStateOf<AdminLesson?>(null)
    }

    var courseToEdit by remember {
        mutableStateOf<Course?>(null)
    }

    suspend fun loadCourses() {
        courses =
            CourseRepository.getAllCourses()
    }

    suspend fun loadStudents() {
        val snapshot =
            firestore
                .collection("users")
                .get()
                .await()

        students =
            snapshot.documents
                .map { document ->
                    AdminStudent(
                        id =
                            document.id,
                        name =
                            document.getString("name")
                                ?: document.getString(
                                    "displayName"
                                )
                                ?: "Student",
                        email =
                            document.getString("email")
                                ?: "No email"
                    )
                }
                .sortedBy {
                    it.name.lowercase()
                }
    }

    suspend fun loadLessons(
        course: Course
    ) {
        val snapshot =
            firestore
                .collection("courses")
                .document(course.id)
                .collection("lessons")
                .orderBy(
                    "order",
                    Query.Direction.ASCENDING
                )
                .get()
                .await()

        lessons =
            snapshot.documents.map { document ->
                AdminLesson(
                    id =
                        document.id,
                    title =
                        document.getString("title")
                            ?: "Untitled Lesson",
                    description =
                        document.getString("description")
                            ?: "",
                    youtubeVideoId =
                        document.getString(
                            "youtubeVideoId"
                        )
                            ?: "",
                    durationMinutes =
                        (
                                document.getLong(
                                    "durationMinutes"
                                )
                                    ?: 0L
                                ).toInt(),
                    order =
                        (
                                document.getLong(
                                    "order"
                                )
                                    ?: 0L
                                ).toInt()
                )
            }
    }

    suspend fun loadAnalytics() {
        val lessonCount =
            firestore
                .collectionGroup("lessons")
                .get()
                .await()
                .size()

        totalLessons =
            lessonCount

        var completedCount = 0

        try {
            val progressSnapshot =
                firestore
                    .collectionGroup("progress")
                    .get()
                    .await()

            completedCount =
                progressSnapshot.documents.count { document ->
                    document.getBoolean(
                        "completed"
                    ) == true
                }
        } catch (_: Exception) {
            completedCount = 0
        }

        completedRecords =
            completedCount
    }

    suspend fun refreshEverything() {
        isRefreshing = true
        globalError = null

        try {
            loadCourses()

            if (selectedSection == 2) {
                loadStudents()
            }

            if (selectedSection == 3) {
                loadAnalytics()
            }
        } catch (exception: Exception) {
            globalError =
                exception.message
                    ?: "Unable to refresh admin data"
        } finally {
            isRefreshing = false
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            loadCourses()
        } catch (exception: Exception) {
            globalError =
                exception.message
                    ?: "Unable to load courses"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedSection) {
        try {
            when (selectedSection) {
                2 -> loadStudents()
                3 -> loadAnalytics()
            }
        } catch (exception: Exception) {
            globalError =
                exception.message
                    ?: "Unable to load data"
        }
    }

    if (courseToDelete != null) {
        val target =
            courseToDelete!!

        AlertDialog(
            onDismissRequest = {
                courseToDelete = null
            },
            title = {
                Text(
                    text =
                        "Delete Course",
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "Delete \"${target.title}\"? This will also delete all lessons inside this course."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id =
                            target.id

                        courseToDelete =
                            null

                        scope.launch {
                            try {
                                firestore
                                    .collection("courses")
                                    .document(id)
                                    .collection("lessons")
                                    .get()
                                    .await()
                                    .documents
                                    .forEach { lesson ->
                                        lesson.reference
                                            .delete()
                                            .await()
                                    }

                                CourseRepository
                                    .deleteCourse(id)

                                loadCourses()
                            } catch (
                                exception: Exception
                            ) {
                                globalError =
                                    exception.message
                                        ?: "Failed to delete course"
                            }
                        }
                    }
                ) {
                    Text(
                        text =
                            "Delete",
                        color =
                            AdminRed,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        courseToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (studentToDelete != null) {
        val target =
            studentToDelete!!

        AlertDialog(
            onDismissRequest = {
                studentToDelete = null
            },
            title = {
                Text(
                    text =
                        "Remove Student",
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "Remove ${target.name}'s profile from Learnify?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id =
                            target.id

                        studentToDelete =
                            null

                        scope.launch {
                            try {
                                firestore
                                    .collection("users")
                                    .document(id)
                                    .delete()
                                    .await()

                                loadStudents()
                            } catch (
                                exception: Exception
                            ) {
                                globalError =
                                    exception.message
                                        ?: "Failed to remove student"
                            }
                        }
                    }
                ) {
                    Text(
                        text =
                            "Remove",
                        color =
                            AdminRed,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        studentToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (
        lessonToDelete != null &&
        selectedCourseForLessons != null
    ) {
        val targetLesson =
            lessonToDelete!!

        val targetCourse =
            selectedCourseForLessons!!

        AlertDialog(
            onDismissRequest = {
                lessonToDelete = null
            },
            title = {
                Text(
                    text =
                        "Delete Lesson",
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "Delete \"${targetLesson.title}\" from ${targetCourse.title}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        lessonToDelete = null

                        scope.launch {
                            try {
                                firestore
                                    .collection("courses")
                                    .document(
                                        targetCourse.id
                                    )
                                    .collection("lessons")
                                    .document(
                                        targetLesson.id
                                    )
                                    .delete()
                                    .await()

                                loadLessons(
                                    targetCourse
                                )
                            } catch (
                                exception: Exception
                            ) {
                                globalError =
                                    exception.message
                                        ?: "Failed to delete lesson"
                            }
                        }
                    }
                ) {
                    Text(
                        text =
                            "Delete",
                        color =
                            AdminRed,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        lessonToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (courseToEdit != null) {
        EditCourseDialog(
            course =
                courseToEdit!!,
            onDismiss = {
                courseToEdit = null
            },
            onSaved = {
                courseToEdit = null

                scope.launch {
                    try {
                        loadCourses()
                    } catch (
                        exception: Exception
                    ) {
                        globalError =
                            exception.message
                                ?: "Failed to refresh courses"
                    }
                }
            }
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    AdminBackground
                )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            "Admin Dashboard",
                        fontSize =
                            25.sp,
                        fontWeight =
                            FontWeight.ExtraBold,
                        color =
                            AdminDark
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            "Manage Learnify from one place",
                        fontSize =
                            13.sp,
                        color =
                            AdminGray
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            refreshEverything()
                        }
                    }
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(21.dp),
                            color =
                                AdminBlue,
                            strokeWidth =
                                2.dp
                        )
                    } else {
                        Icon(
                            imageVector =
                                Icons.Default.Refresh,
                            contentDescription =
                                "Refresh",
                            tint =
                                AdminDark
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            AdminOverviewCards(
                courses =
                    courses.size,
                students =
                    students.size,
                lessons =
                    totalLessons
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            ScrollableTabRow(
                selectedTabIndex =
                    selectedSection,
                edgePadding =
                    0.dp,
                containerColor =
                    Color.Transparent,
                divider = {}
            ) {
                AdminTab(
                    selected =
                        selectedSection == 0,
                    icon =
                        Icons.Default.MenuBook,
                    text =
                        "Courses",
                    onClick = {
                        selectedSection = 0
                    }
                )

                AdminTab(
                    selected =
                        selectedSection == 1,
                    icon =
                        Icons.Default.VideoLibrary,
                    text =
                        "Lessons",
                    onClick = {
                        selectedSection = 1
                    }
                )

                AdminTab(
                    selected =
                        selectedSection == 2,
                    icon =
                        Icons.Default.People,
                    text =
                        "Students",
                    onClick = {
                        selectedSection = 2
                    }
                )

                AdminTab(
                    selected =
                        selectedSection == 3,
                    icon =
                        Icons.Default.Analytics,
                    text =
                        "Analytics",
                    onClick = {
                        selectedSection = 3
                    }
                )
            }
        }

        if (globalError != null) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        ),
                shape =
                    RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFFFF1F2)
                    )
            ) {
                Text(
                    text =
                        globalError!!,
                    modifier =
                        Modifier.padding(14.dp),
                    fontSize =
                        13.sp,
                    color =
                        AdminRed
                )
            }
        }

        when (selectedSection) {
            0 -> {
                CourseManagementSection(
                    courses =
                        courses,
                    isLoading =
                        isLoading,
                    onAddCourse = {
                        courseToEdit =
                            Course(
                                title = "",
                                instructorName = "",
                                description = "",
                                thumbnailUrl = "",
                                durationMinutes = 0,
                                category = "",
                                featured = false,
                                popular = false
                            )
                    },
                    onEditCourse = {
                        courseToEdit = it
                    },
                    onDeleteCourse = {
                        courseToDelete = it
                    }
                )
            }

            1 -> {
                LessonManagementSection(
                    courses =
                        courses,
                    selectedCourse =
                        selectedCourseForLessons,
                    lessons =
                        lessons,
                    onCourseSelected = { course ->
                        selectedCourseForLessons =
                            course

                        scope.launch {
                            try {
                                loadLessons(course)
                            } catch (
                                exception: Exception
                            ) {
                                globalError =
                                    exception.message
                                        ?: "Unable to load lessons"
                            }
                        }
                    },
                    onDeleteLesson = {
                        lessonToDelete = it
                    }
                )
            }

            2 -> {
                StudentManagementSection(
                    students =
                        students,
                    isLoading =
                        isRefreshing,
                    onDeleteStudent = {
                        studentToDelete = it
                    }
                )
            }

            3 -> {
                AnalyticsSection(
                    courses =
                        courses.size,
                    students =
                        students.size,
                    lessons =
                        totalLessons,
                    completedRecords =
                        completedRecords
                )
            }
        }
    }
}

@Composable
private fun AdminOverviewCards(
    courses: Int,
    students: Int,
    lessons: Int
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        AdminStatCard(
            modifier =
                Modifier.weight(1f),
            icon =
                Icons.Default.MenuBook,
            title =
                "Courses",
            value =
                courses.toString(),
            color =
                AdminBlue
        )

        AdminStatCard(
            modifier =
                Modifier.weight(1f),
            icon =
                Icons.Default.VideoLibrary,
            title =
                "Lessons",
            value =
                lessons.toString(),
            color =
                AdminPurple
        )

        AdminStatCard(
            modifier =
                Modifier.weight(1f),
            icon =
                Icons.Default.People,
            title =
                "Students",
            value =
                students.toString(),
            color =
                AdminGreen
        )
    }
}

@Composable
private fun AdminStatCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape =
            RoundedCornerShape(15.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    AdminCard
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {
        Column(
            modifier =
                Modifier.padding(13.dp)
        ) {
            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(21.dp),
                tint =
                    color
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    value,
                fontSize =
                    20.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    AdminDark
            )

            Text(
                text =
                    title,
                fontSize =
                    10.sp,
                color =
                    AdminGray
            )
        }
    }
}

@Composable
private fun AdminTab(
    selected: Boolean,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Tab(
        selected =
            selected,
        onClick =
            onClick,
        text = {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(17.dp),
                    tint =
                        if (selected) {
                            AdminBlue
                        } else {
                            AdminGray
                        }
                )

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    text =
                        text,
                    fontSize =
                        12.sp
                )
            }
        }
    )
}

@Composable
private fun CourseManagementSection(
    courses: List<Course>,
    isLoading: Boolean,
    onAddCourse: () -> Unit,
    onEditCourse: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "Course Management",
                    fontSize =
                        19.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AdminText
                )

                Text(
                    text =
                        "Create, edit and remove courses",
                    fontSize =
                        12.sp,
                    color =
                        AdminGray
                )
            }

            Button(
                onClick =
                    onAddCourse,
                shape =
                    RoundedCornerShape(12.dp),
                contentPadding =
                    PaddingValues(
                        horizontal = 13.dp,
                        vertical = 8.dp
                    )
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Add,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(17.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                Text(
                    text =
                        "Add",
                    fontSize =
                        12.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        if (isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 50.dp
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator(
                    color =
                        AdminBlue
                )
            }
        } else if (courses.isEmpty()) {
            EmptyAdminCard(
                icon =
                    Icons.Default.MenuBook,
                title =
                    "No courses yet",
                description =
                    "Create your first course using the Add button."
            )
        } else {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                courses.forEach { course ->
                    AdminCourseCard(
                        course =
                            course,
                        onEdit = {
                            onEditCourse(course)
                        },
                        onDelete = {
                            onDeleteCourse(course)
                        }
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )
    }
}

@Composable
private fun AdminCourseCard(
    course: Course,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(17.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {
        Column {
            AsyncImage(
                model =
                    course.thumbnailUrl,
                contentDescription =
                    course.title,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 17.dp,
                                topEnd = 17.dp
                            )
                        ),
                contentScale =
                    ContentScale.Crop
            )

            Column(
                modifier =
                    Modifier.padding(15.dp)
            ) {
                Text(
                    text =
                        course.title,
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AdminText,
                    maxLines =
                        2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text =
                        course.category.ifBlank {
                            "Uncategorized"
                        },
                    fontSize =
                        12.sp,
                    color =
                        AdminBlue
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text =
                        "${course.instructorName.ifBlank { "No instructor" }} • ${course.durationMinutes} min",
                    fontSize =
                        11.sp,
                    color =
                        AdminGray
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {
                    if (course.featured) {
                        AdminBadge(
                            text =
                                "FEATURED",
                            color =
                                AdminOrange
                        )
                    }

                    if (course.popular) {
                        AdminBadge(
                            text =
                                "POPULAR",
                            color =
                                AdminGreen
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(13.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick =
                            onEdit,
                        modifier =
                            Modifier.weight(1f),
                        shape =
                            RoundedCornerShape(11.dp)
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Edit,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(17.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text("Edit")
                    }

                    Button(
                        onClick =
                            onDelete,
                        modifier =
                            Modifier.weight(1f),
                        shape =
                            RoundedCornerShape(11.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    AdminRed
                            )
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Delete,
                            contentDescription =
                                null,
                            modifier =
                                Modifier.size(17.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(5.dp)
                        )

                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminBadge(
    text: String,
    color: Color
) {
    Text(
        text =
            text,
        fontSize =
            9.sp,
        fontWeight =
            FontWeight.Bold,
        color =
            color,
        modifier =
            Modifier
                .background(
                    color.copy(alpha = 0.12f),
                    RoundedCornerShape(6.dp)
                )
                .padding(
                    horizontal = 7.dp,
                    vertical = 4.dp
                )
    )
}

@Composable
private fun LessonManagementSection(
    courses: List<Course>,
    selectedCourse: Course?,
    lessons: List<AdminLesson>,
    onCourseSelected: (Course) -> Unit,
    onDeleteLesson: (AdminLesson) -> Unit
) {
    var showAddLesson by remember {
        mutableStateOf(false)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)
    ) {
        Text(
            text =
                "Lesson Management",
            fontSize =
                19.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                AdminText
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                "Add individual videos or entire YouTube playlists",
            fontSize =
                12.sp,
            color =
                AdminGray
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                "Select Course",
            fontSize =
                13.sp,
            fontWeight =
                FontWeight.SemiBold,
            color =
                AdminText
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        if (courses.isEmpty()) {
            EmptyAdminCard(
                icon =
                    Icons.Default.MenuBook,
                title =
                    "No courses available",
                description =
                    "Create a course before adding lessons."
            )
        } else {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                courses.forEach { course ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCourseSelected(
                                        course
                                    )
                                },
                        shape =
                            RoundedCornerShape(13.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (
                                        selectedCourse?.id ==
                                        course.id
                                    ) {
                                        AdminBlue.copy(
                                            alpha = 0.08f
                                        )
                                    } else {
                                        Color.White
                                    }
                            )
                    ) {
                        Row(
                            modifier =
                                Modifier.padding(12.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model =
                                    course.thumbnailUrl,
                                contentDescription =
                                    course.title,
                                modifier =
                                    Modifier
                                        .size(55.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                9.dp
                                            )
                                        ),
                                contentScale =
                                    ContentScale.Crop
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(11.dp)
                            )

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {
                                Text(
                                    text =
                                        course.title,
                                    fontSize =
                                        14.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    color =
                                        AdminText,
                                    maxLines =
                                        1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )

                                Text(
                                    text =
                                        course.category
                                            .ifBlank {
                                                "Uncategorized"
                                            },
                                    fontSize =
                                        11.sp,
                                    color =
                                        AdminGray
                                )
                            }

                            if (
                                selectedCourse?.id ==
                                course.id
                            ) {
                                Icon(
                                    imageVector =
                                        Icons.Default.CheckCircle,
                                    contentDescription =
                                        null,
                                    tint =
                                        AdminBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedCourse != null) {
            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            "Lessons",
                        fontSize =
                            18.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            AdminText
                    )

                    Text(
                        text =
                            "${lessons.size} lesson${if (lessons.size == 1) "" else "s"}",
                        fontSize =
                            11.sp,
                        color =
                            AdminGray
                    )
                }

                Button(
                    onClick = {
                        showAddLesson = true
                    },
                    shape =
                        RoundedCornerShape(11.dp)
                ) {
                    Icon(
                        imageVector =
                            Icons.Default.Add,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(17.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )

                    Text("Add Lesson")
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (lessons.isEmpty()) {
                EmptyAdminCard(
                    icon =
                        Icons.Default.PlayCircle,
                    title =
                        "No lessons yet",
                    description =
                        "Add a video or YouTube playlist."
                )
            } else {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    lessons.forEach { lesson ->
                        LessonAdminCard(
                            lesson =
                                lesson,
                            onDelete = {
                                onDeleteLesson(
                                    lesson
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )
    }

    if (
        showAddLesson &&
        selectedCourse != null
    ) {
        AddLessonDialog(
            course =
                selectedCourse,
            nextOrder =
                lessons.size + 1,
            onDismiss = {
                showAddLesson = false
            },
            onSaved = {
                showAddLesson = false
            }
        )
    }
}

@Composable
private fun LessonAdminCard(
    lesson: AdminLesson,
    onDelete: () -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {
        Row(
            modifier =
                Modifier.padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(45.dp)
                        .clip(
                            RoundedCornerShape(11.dp)
                        )
                        .background(
                            AdminBlue.copy(
                                alpha = 0.10f
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector =
                        Icons.Default.PlayCircle,
                    contentDescription =
                        null,
                    tint =
                        AdminBlue,
                    modifier =
                        Modifier.size(26.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.width(11.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "${lesson.order}. ${lesson.title}",
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AdminText,
                    maxLines =
                        2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        "${lesson.durationMinutes} min • ${lesson.youtubeVideoId}",
                    fontSize =
                        11.sp,
                    color =
                        AdminGray
                )
            }

            IconButton(
                onClick =
                    onDelete
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Delete,
                    contentDescription =
                        "Delete lesson",
                    tint =
                        AdminRed
                )
            }
        }
    }
}

@Composable
private fun StudentManagementSection(
    students: List<AdminStudent>,
    isLoading: Boolean,
    onDeleteStudent: (AdminStudent) -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        "Student Management",
                    fontSize =
                        19.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AdminText
                )

                Text(
                    text =
                        "${students.size} registered students",
                    fontSize =
                        12.sp,
                    color =
                        AdminGray
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(15.dp)
        )

        if (isLoading) {
            Box(
                modifier =
                    Modifier.fillMaxWidth(),
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator(
                    color =
                        AdminBlue
                )
            }
        } else if (students.isEmpty()) {
            EmptyAdminCard(
                icon =
                    Icons.Default.People,
                title =
                    "No students found",
                description =
                    "Registered students will appear here."
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                items(
                    students,
                    key = {
                        it.id
                    }
                ) { student ->
                    StudentAdminCard(
                        student =
                            student,
                        onDelete = {
                            onDeleteStudent(
                                student
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentAdminCard(
    student: AdminStudent,
    onDelete: () -> Unit
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {
        Row(
            modifier =
                Modifier.padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(45.dp)
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .background(
                            AdminBlue.copy(
                                alpha = 0.10f
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text =
                        student.name
                            .firstOrNull()
                            ?.uppercase()
                            ?: "S",
                    fontSize =
                        17.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AdminBlue
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        student.name,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AdminText
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        student.email,
                    fontSize =
                        11.sp,
                    color =
                        AdminGray,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick =
                    onDelete
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Delete,
                    contentDescription =
                        "Remove student",
                    tint =
                        AdminRed
                )
            }
        }
    }
}

@Composable
private fun AnalyticsSection(
    courses: Int,
    students: Int,
    lessons: Int,
    completedRecords: Int
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)
    ) {
        Text(
            text =
                "Learning Analytics",
            fontSize =
                19.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                AdminText
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                "Overview of your Learnify platform",
            fontSize =
                12.sp,
            color =
                AdminGray
        )

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        AnalyticsCard(
            icon =
                Icons.Default.People,
            title =
                "Registered Students",
            value =
                students.toString(),
            description =
                "Students with a Learnify account",
            color =
                AdminBlue
        )

        Spacer(
            modifier =
                Modifier.height(11.dp)
        )

        AnalyticsCard(
            icon =
                Icons.Default.MenuBook,
            title =
                "Published Courses",
            value =
                courses.toString(),
            description =
                "Courses currently available",
            color =
                AdminPurple
        )

        Spacer(
            modifier =
                Modifier.height(11.dp)
        )

        AnalyticsCard(
            icon =
                Icons.Default.VideoLibrary,
            title =
                "Total Lessons",
            value =
                lessons.toString(),
            description =
                "Video lessons across all courses",
            color =
                AdminOrange
        )

        Spacer(
            modifier =
                Modifier.height(11.dp)
        )

        AnalyticsCard(
            icon =
                Icons.Default.CheckCircle,
            title =
                "Completed Lessons",
            value =
                completedRecords.toString(),
            description =
                "Completed progress records",
            color =
                AdminGreen
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(17.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {
            Column(
                modifier =
                    Modifier.padding(18.dp)
            ) {
                Text(
                    text =
                        "Platform Summary",
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AdminText
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                AnalyticsRow(
                    label =
                        "Students per course",
                    value =
                        if (courses > 0) {
                            String.format(
                                "%.1f",
                                students.toDouble() /
                                        courses
                            )
                        } else {
                            "0.0"
                        }
                )

                Spacer(
                    modifier =
                        Modifier.height(11.dp)
                )

                AnalyticsRow(
                    label =
                        "Lessons per course",
                    value =
                        if (courses > 0) {
                            String.format(
                                "%.1f",
                                lessons.toDouble() /
                                        courses
                            )
                        } else {
                            "0.0"
                        }
                )

                Spacer(
                    modifier =
                        Modifier.height(11.dp)
                )

                AnalyticsRow(
                    label =
                        "Completion records",
                    value =
                        completedRecords.toString()
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(30.dp)
        )
    }
}

@Composable
private fun AnalyticsCard(
    icon: ImageVector,
    title: String,
    value: String,
    description: String,
    color: Color
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {
        Row(
            modifier =
                Modifier.padding(17.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(50.dp)
                        .clip(
                            RoundedCornerShape(13.dp)
                        )
                        .background(
                            color.copy(
                                alpha = 0.10f
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(27.dp),
                    tint =
                        color
                )
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        title,
                    fontSize =
                        13.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        AdminText
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        description,
                    fontSize =
                        11.sp,
                    color =
                        AdminGray
                )
            }

            Text(
                text =
                    value,
                fontSize =
                    23.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    color
            )
        }
    }
}

@Composable
private fun AnalyticsRow(
    label: String,
    value: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text =
                label,
            fontSize =
                13.sp,
            color =
                AdminGray
        )

        Text(
            text =
                value,
            fontSize =
                13.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                AdminText
        )
    }
}

@Composable
private fun EmptyAdminCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(15.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(40.dp),
                tint =
                    AdminGray
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    title,
                fontSize =
                    15.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    AdminText
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    description,
                fontSize =
                    12.sp,
                color =
                    AdminGray
            )
        }
    }
}

@Composable
private fun AddLessonDialog(
    course: Course,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val firestore =
        remember {
            FirebaseFirestore.getInstance()
        }

    val scope =
        rememberCoroutineScope()

    var title by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var youtubeLink by remember {
        mutableStateOf("")
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val playlistId =
        remember(youtubeLink) {
            extractYoutubePlaylistId(
                youtubeLink.trim()
            )
        }

    val videoId =
        remember(youtubeLink) {
            try {
                if (
                    youtubeLink.isBlank() ||
                    playlistId != null
                ) {
                    null
                } else {
                    CourseRepository
                        .extractYoutubeVideoId(
                            youtubeLink.trim()
                        )
                }
            } catch (_: Exception) {
                null
            }
        }

    val isPlaylist =
        playlistId != null

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        title = {
            Text(
                text =
                    "Add Lesson",
                fontWeight =
                    FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text =
                        course.title,
                    fontSize =
                        12.sp,
                    color =
                        AdminGray
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value =
                        title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text(
                            if (isPlaylist) {
                                "Lesson title prefix (optional)"
                            } else {
                                "Lesson title"
                            }
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true
                )

                Spacer(
                    modifier =
                        Modifier.height(9.dp)
                )

                OutlinedTextField(
                    value =
                        youtubeLink,
                    onValueChange = {
                        youtubeLink = it
                        error = null
                    },
                    label = {
                        Text(
                            "YouTube video or playlist link"
                        )
                    },
                    placeholder = {
                        Text(
                            "https://youtube.com/playlist?list=..."
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true
                )

                if (isPlaylist) {
                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Playlist detected. All videos will be added as lessons.",
                        fontSize =
                            11.sp,
                        color =
                            AdminGreen,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(9.dp)
                )

                OutlinedTextField(
                    value =
                        description,
                    onValueChange = {
                        description = it
                    },
                    label = {
                        Text("Description")
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    minLines =
                        2
                )

                if (error != null) {
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            error!!,
                        fontSize =
                            12.sp,
                        color =
                            AdminRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled =
                    !isSaving &&
                            youtubeLink.isNotBlank() &&
                            (
                                    isPlaylist ||
                                            title.isNotBlank()
                                    ),
                onClick = {
                    error = null
                    isSaving = true

                    scope.launch {
                        try {
                            if (playlistId != null) {
                                val playlistVideos =
                                    getYoutubePlaylistVideos(
                                        playlistId
                                    )

                                if (
                                    playlistVideos.isEmpty()
                                ) {
                                    throw Exception(
                                        "No videos were found in this playlist."
                                    )
                                }

                                var order =
                                    nextOrder

                                playlistVideos.forEach { video ->
                                    val lessonTitle =
                                        if (title.isBlank()) {
                                            video.title
                                        } else {
                                            "${title.trim()} - ${video.title}"
                                        }

                                    val lessonData =
                                        hashMapOf<String, Any>(
                                            "title" to
                                                    lessonTitle,
                                            "description" to
                                                    description.trim(),
                                            "youtubeVideoId" to
                                                    video.id,
                                            "durationMinutes" to
                                                    video.durationMinutes,
                                            "order" to
                                                    order,
                                            "createdAt" to
                                                    System.currentTimeMillis()
                                        )

                                    firestore
                                        .collection("courses")
                                        .document(course.id)
                                        .collection("lessons")
                                        .add(lessonData)
                                        .await()

                                    order++
                                }
                            } else {
                                val validVideoId =
                                    videoId
                                        ?: throw Exception(
                                            "Please enter a valid YouTube video or playlist link."
                                        )

                                val durationMinutes =
                                    getYoutubeDurationMinutes(
                                        validVideoId
                                    )

                                val lessonData =
                                    hashMapOf<String, Any>(
                                        "title" to
                                                title.trim(),
                                        "description" to
                                                description.trim(),
                                        "youtubeVideoId" to
                                                validVideoId,
                                        "durationMinutes" to
                                                durationMinutes,
                                        "order" to
                                                nextOrder,
                                        "createdAt" to
                                                System.currentTimeMillis()
                                    )

                                firestore
                                    .collection("courses")
                                    .document(course.id)
                                    .collection("lessons")
                                    .add(lessonData)
                                    .await()
                            }

                            isSaving = false
                            onSaved()
                        } catch (
                            exception: Exception
                        ) {
                            isSaving = false

                            error =
                                exception.message
                                    ?: "Failed to add lesson"
                        }
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(18.dp),
                        color =
                            Color.White,
                        strokeWidth =
                            2.dp
                    )
                } else {
                    Text(
                        text =
                            if (isPlaylist) {
                                "Add Playlist"
                            } else {
                                "Add Lesson"
                            }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled =
                    !isSaving,
                onClick =
                    onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditCourseDialog(
    course: Course,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val firestore =
        remember {
            FirebaseFirestore.getInstance()
        }

    val scope =
        rememberCoroutineScope()

    val isNewCourse =
        course.id.isBlank()

    var title by remember {
        mutableStateOf(
            course.title
        )
    }

    var instructor by remember {
        mutableStateOf(
            course.instructorName
        )
    }

    var description by remember {
        mutableStateOf(
            course.description
        )
    }

    var category by remember {
        mutableStateOf(
            course.category
        )
    }

    var youtubeLink by remember {
        mutableStateOf("")
    }

    var featured by remember {
        mutableStateOf(
            course.featured
        )
    }

    var popular by remember {
        mutableStateOf(
            course.popular
        )
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val playlistId =
        remember(youtubeLink) {
            extractYoutubePlaylistId(
                youtubeLink.trim()
            )
        }

    val videoId =
        remember(youtubeLink) {
            try {
                if (
                    youtubeLink.isBlank() ||
                    playlistId != null
                ) {
                    null
                } else {
                    CourseRepository
                        .extractYoutubeVideoId(
                            youtubeLink.trim()
                        )
                }
            } catch (_: Exception) {
                null
            }
        }

    val isPlaylist =
        playlistId != null

    AlertDialog(
        onDismissRequest = {
            if (!saving) {
                onDismiss()
            }
        },
        title = {
            Text(
                text =
                    if (isNewCourse) {
                        "Add Course"
                    } else {
                        "Edit Course"
                    },
                fontWeight =
                    FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                OutlinedTextField(
                    value =
                        title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Course title")
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value =
                        instructor,
                    onValueChange = {
                        instructor = it
                    },
                    label = {
                        Text("Instructor")
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value =
                        category,
                    onValueChange = {
                        category = it
                    },
                    label = {
                        Text("Category")
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value =
                        description,
                    onValueChange = {
                        description = it
                    },
                    label = {
                        Text("Description")
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    minLines =
                        3
                )

                if (isNewCourse) {
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value =
                            youtubeLink,
                        onValueChange = {
                            youtubeLink = it
                            error = null
                        },
                        label = {
                            Text(
                                "YouTube video or playlist link"
                            )
                        },
                        placeholder = {
                            Text(
                                "https://youtube.com/playlist?list=..."
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        singleLine =
                            true
                    )

                    if (isPlaylist) {
                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )

                        Text(
                            text =
                                "Playlist detected. All videos will be added as lessons.",
                            fontSize =
                                11.sp,
                            color =
                                AdminGreen,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected =
                            featured,
                        onClick = {
                            featured =
                                !featured
                        }
                    )

                    Text(
                        text =
                            "Featured",
                        fontSize =
                            13.sp
                    )
                }

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected =
                            popular,
                        onClick = {
                            popular =
                                !popular
                        }
                    )

                    Text(
                        text =
                            "Popular",
                        fontSize =
                            13.sp
                    )
                }

                if (error != null) {
                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Text(
                        text =
                            error!!,
                        fontSize =
                            12.sp,
                        color =
                            AdminRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled =
                    !saving &&
                            title.isNotBlank() &&
                            (
                                    !isNewCourse ||
                                            youtubeLink.isNotBlank()
                                    ),
                onClick = {
                    error = null
                    saving = true

                    scope.launch {
                        try {
                            if (isNewCourse) {
                                if (playlistId != null) {
                                    val playlistVideos =
                                        getYoutubePlaylistVideos(
                                            playlistId
                                        )

                                    if (
                                        playlistVideos.isEmpty()
                                    ) {
                                        throw Exception(
                                            "No videos were found in this playlist."
                                        )
                                    }

                                    val totalDuration =
                                        playlistVideos.sumOf {
                                            it.durationMinutes
                                        }

                                    val firstVideo =
                                        playlistVideos.first()

                                    val courseRef =
                                        firestore
                                            .collection(
                                                "courses"
                                            )
                                            .document()

                                    val newCourse =
                                        Course(
                                            id =
                                                courseRef.id,
                                            title =
                                                title.trim(),
                                            instructorName =
                                                instructor.trim(),
                                            description =
                                                description.trim(),
                                            thumbnailUrl =
                                                CourseRepository
                                                    .getYoutubeThumbnailUrl(
                                                        firstVideo.id
                                                    ),
                                            durationMinutes =
                                                totalDuration,
                                            category =
                                                category.trim(),
                                            featured =
                                                featured,
                                            popular =
                                                popular
                                        )

                                    courseRef
                                        .set(newCourse)
                                        .await()

                                    var order = 1

                                    playlistVideos.forEach { video ->
                                        val lessonData =
                                            hashMapOf<String, Any>(
                                                "title" to
                                                        video.title,
                                                "description" to
                                                        description.trim(),
                                                "youtubeVideoId" to
                                                        video.id,
                                                "durationMinutes" to
                                                        video.durationMinutes,
                                                "order" to
                                                        order,
                                                "createdAt" to
                                                        System.currentTimeMillis()
                                            )

                                        courseRef
                                            .collection("lessons")
                                            .add(lessonData)
                                            .await()

                                        order++
                                    }
                                } else {
                                    val validVideoId =
                                        videoId
                                            ?: throw Exception(
                                                "Please enter a valid YouTube video or playlist link."
                                            )

                                    val durationMinutes =
                                        getYoutubeDurationMinutes(
                                            validVideoId
                                        )

                                    val courseRef =
                                        firestore
                                            .collection(
                                                "courses"
                                            )
                                            .document()

                                    val newCourse =
                                        Course(
                                            id =
                                                courseRef.id,
                                            title =
                                                title.trim(),
                                            instructorName =
                                                instructor.trim(),
                                            description =
                                                description.trim(),
                                            thumbnailUrl =
                                                CourseRepository
                                                    .getYoutubeThumbnailUrl(
                                                        validVideoId
                                                    ),
                                            durationMinutes =
                                                durationMinutes,
                                            category =
                                                category.trim(),
                                            featured =
                                                featured,
                                            popular =
                                                popular
                                        )

                                    courseRef
                                        .set(newCourse)
                                        .await()

                                    val lessonData =
                                        hashMapOf<String, Any>(
                                            "title" to
                                                    title.trim(),
                                            "description" to
                                                    description.trim(),
                                            "youtubeVideoId" to
                                                    validVideoId,
                                            "durationMinutes" to
                                                    durationMinutes,
                                            "order" to
                                                    1,
                                            "createdAt" to
                                                    System.currentTimeMillis()
                                        )

                                    courseRef
                                        .collection("lessons")
                                        .add(lessonData)
                                        .await()
                                }
                            } else {
                                val updates =
                                    hashMapOf<String, Any>(
                                        "title" to
                                                title.trim(),
                                        "instructorName" to
                                                instructor.trim(),
                                        "description" to
                                                description.trim(),
                                        "category" to
                                                category.trim(),
                                        "featured" to
                                                featured,
                                        "popular" to
                                                popular
                                    )

                                firestore
                                    .collection("courses")
                                    .document(course.id)
                                    .update(updates)
                                    .await()
                            }

                            saving = false
                            onSaved()
                        } catch (
                            exception: Exception
                        ) {
                            saving = false

                            error =
                                exception.message
                                    ?: "Unable to save course"
                        }
                    }
                }
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(18.dp),
                        color =
                            Color.White,
                        strokeWidth =
                            2.dp
                    )
                } else {
                    Text(
                        text =
                            if (isNewCourse) {
                                if (isPlaylist) {
                                    "Add Playlist"
                                } else {
                                    "Add Course"
                                }
                            } else {
                                "Save Changes"
                            }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled =
                    !saving,
                onClick =
                    onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
