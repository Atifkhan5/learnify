package com.example.myapp

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val LearnifyBlue = Color(0xFF2563EB)
private val LearnifyDark = Color(0xFF172554)
private val LearnifyText = Color(0xFF1E293B)
private val LearnifyGray = Color(0xFF64748B)
private val LearnifyCardBg = Color(0xFFF8FAFC)

@Composable
fun AdminScreen(modifier: Modifier = Modifier) {

    if (!CourseRepository.isCurrentUserAdmin()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You dont have access to this screen",
                color = LearnifyGray
            )
        }
        return
    }

    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var instructorName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var durationMinutesText by remember { mutableStateOf("") }
    var youtubeLink by remember { mutableStateOf("") }

    // Thumbnail: local picked image + upload state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedThumbnailUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            uploadedThumbnailUrl = null
            uploadError = null

            isUploadingImage = true
            scope.launch {
                try {
                    uploadedThumbnailUrl = CourseRepository.uploadThumbnail(uri)
                } catch (exception: Exception) {
                    uploadError = exception.message ?: "Failed to upload image"
                    selectedImageUri = null
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf<String?>(null) }

    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var isLoadingCourses by remember { mutableStateOf(true) }
    var deletingCourseId by remember { mutableStateOf<String?>(null) }

    suspend fun refreshCourses() {
        isLoadingCourses = true
        try {
            courses = CourseRepository.getAllCourses()
        } catch (_: Exception) {
            // Leave the existing list as-is on failure.
        } finally {
            isLoadingCourses = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCourses()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "Admin: Manage Courses",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LearnifyDark
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Course title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- Thumbnail picker ---
        Text(
            text = "Thumbnail image",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LearnifyText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LearnifyCardBg)
                .clickable {
                    imagePickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                isUploadingImage -> {
                    CircularProgressIndicator(color = LearnifyBlue)
                }
                selectedImageUri != null -> {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected thumbnail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = LearnifyGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap to choose an image",
                            fontSize = 12.sp,
                            color = LearnifyGray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                imagePickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploadingImage
        ) {
            Text(if (selectedImageUri == null) "Choose Image" else "Change Image")
        }

        if (uploadError != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Upload failed: $uploadError", color = Color.Red, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = instructorName,
            onValueChange = { instructorName = it },
            label = { Text("Instructor name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category (e.g. Programming)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = durationMinutesText,
            onValueChange = { durationMinutesText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Duration (minutes)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = youtubeLink,
            onValueChange = { youtubeLink = it },
            label = { Text("YouTube video link, or a playlist link for multiple lessons") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (saveError != null) {
            Text(text = "Couldn't add course: $saveError", color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (saveSuccess != null) {
            Text(text = saveSuccess!!, color = Color(0xFF16A34A), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                saveError = null
                saveSuccess = null

                val thumbnailUrl = uploadedThumbnailUrl

                if (title.isBlank() || thumbnailUrl.isNullOrBlank() || youtubeLink.isBlank()) {
                    saveError = when {
                        thumbnailUrl.isNullOrBlank() && isUploadingImage -> "Please wait for the image to finish uploading"
                        thumbnailUrl.isNullOrBlank() -> "Please choose a thumbnail image"
                        else -> "Title and YouTube link are required"
                    }
                    return@Button
                }

                isSaving = true
                scope.launch {
                    try {
                        val newCourse = Course(
                            title = title.trim(),
                            instructorName = instructorName.trim(),
                            description = description.trim(),
                            thumbnailUrl = thumbnailUrl,
                            durationMinutes = durationMinutesText.toIntOrNull() ?: 0,
                            category = category.trim(),
                            featured = false,
                            popular = false
                        )

                        val trimmedLink = youtubeLink.trim()

                        if (YouTubePlaylistRepository.isPlaylistLink(trimmedLink)) {

                            val playlistId = YouTubePlaylistRepository.extractPlaylistId(trimmedLink)
                                ?: throw Exception("Couldn't read playlist ID from that link")

                            val playlistVideos = YouTubePlaylistRepository.fetchPlaylistVideos(playlistId)

                            if (playlistVideos.isEmpty()) {
                                throw Exception("No videos found in that playlist")
                            }

                            CourseRepository.addCourseWithLessons(newCourse, playlistVideos)

                            saveSuccess = "Course added with ${playlistVideos.size} lessons from playlist"

                        } else {

                            val videoId = extractYoutubeVideoId(trimmedLink)
                            CourseRepository.addCourse(newCourse, videoId)

                            saveSuccess = "Course added successfully"
                        }

                        title = ""
                        instructorName = ""
                        description = ""
                        category = ""
                        durationMinutesText = ""
                        youtubeLink = ""
                        selectedImageUri = null
                        uploadedThumbnailUrl = null

                        refreshCourses()

                    } catch (exception: Exception) {
                        saveError = exception.message ?: "Something went wrong"
                    } finally {
                        isSaving = false
                    }
                }
            },
            enabled = !isSaving && !isUploadingImage,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), color = Color.White)
            } else {
                Text("Add Course")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Existing Courses",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = LearnifyText
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoadingCourses) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LearnifyBlue)
            }
        } else if (courses.isEmpty()) {
            Text(text = "No courses yet", fontSize = 13.sp, color = LearnifyGray)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                courses.forEach { course ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LearnifyCardBg)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LearnifyText,
                                maxLines = 1
                            )
                            Text(
                                text = course.category,
                                fontSize = 12.sp,
                                color = LearnifyGray
                            )
                        }

                        if (deletingCourseId == course.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = LearnifyBlue
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    deletingCourseId = course.id
                                    scope.launch {
                                        try {
                                            CourseRepository.deleteCourse(course.id)
                                            refreshCourses()
                                        } catch (_: Exception) {
                                            // Could surface an error toast here if needed.
                                        } finally {
                                            deletingCourseId = null
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete course",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}