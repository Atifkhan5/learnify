package com.example.myapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
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
private val LearnifyGreen = Color(0xFF16A34A)
private val LearnifyRed = Color(0xFFDC2626)
private val LearnifyOrange = Color(0xFFF59E0B)

@Composable
fun AdminScreen(
    modifier: Modifier = Modifier
) {

    if (!CourseRepository.isCurrentUserAdmin()) {

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You don't have access to this screen",
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

    var isFeatured by remember { mutableStateOf(false) }
    var isPopular by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf<String?>(null) }

    var courses by remember {
        mutableStateOf<List<Course>>(emptyList())
    }

    var isLoadingCourses by remember {
        mutableStateOf(true)
    }

    var deletingCourseId by remember {
        mutableStateOf<String?>(null)
    }

    var courseToDelete by remember {
        mutableStateOf<Course?>(null)
    }

    val videoId = remember(youtubeLink) {

        try {

            if (youtubeLink.isBlank()) {
                null
            } else {
                CourseRepository.extractYoutubeVideoId(
                    youtubeLink.trim()
                )
            }

        } catch (_: Exception) {
            null
        }
    }

    val thumbnailUrl = videoId?.let {
        CourseRepository.getYoutubeThumbnailUrl(it)
    }

    suspend fun refreshCourses() {

        isLoadingCourses = true

        try {

            courses = CourseRepository.getAllCourses()

        } catch (_: Exception) {

        } finally {

            isLoadingCourses = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCourses()
    }

    if (courseToDelete != null) {

        val target = courseToDelete!!

        AlertDialog(
            onDismissRequest = {
                courseToDelete = null
            },

            title = {
                Text(
                    text = "Delete course",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.title}\"? This will also remove all lessons belonging to this course."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val id = target.id

                        courseToDelete = null
                        deletingCourseId = id

                        scope.launch {

                            try {

                                CourseRepository.deleteCourse(id)

                                refreshCourses()

                            } catch (exception: Exception) {

                                saveError =
                                    exception.message
                                        ?: "Failed to delete course"

                            } finally {

                                deletingCourseId = null
                            }
                        }
                    }
                ) {

                    Text(
                        text = "Delete",
                        color = LearnifyRed,
                        fontWeight = FontWeight.Bold
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp)
    ) {

        Text(
            text = "Admin Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = LearnifyDark
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "Manage your courses and learning content",
            fontSize = 13.sp,
            color = LearnifyGray
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Add New Course",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = LearnifyText
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
            },
            label = {
                Text("Course title")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = instructorName,
            onValueChange = {
                instructorName = it
            },
            label = {
                Text("Instructor name")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
            },
            label = {
                Text("Category")
            },
            placeholder = {
                Text("e.g. Programming")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = durationMinutesText,
            onValueChange = {

                durationMinutesText =
                    it.filter { character ->
                        character.isDigit()
                    }
            },
            label = {
                Text("Duration (minutes)")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
            },
            label = {
                Text("Description")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = youtubeLink,
            onValueChange = {
                youtubeLink = it
            },
            label = {
                Text("YouTube video ID or link")
            },
            placeholder = {
                Text("Example: 8Sj2tbh-ozE")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        if (thumbnailUrl != null) {

            Text(
                text = "YouTube Thumbnail Preview",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LearnifyText
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "YouTube thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    ),
                contentScale = ContentScale.Crop
            )

        } else if (youtubeLink.isNotBlank()) {

            Text(
                text = "Invalid YouTube video ID or link",
                color = LearnifyRed,
                fontSize = 13.sp
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Placement",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LearnifyText
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = isFeatured,
                onClick = {
                    isFeatured = !isFeatured
                }
            )

            Text(
                text = "Featured Course",
                fontSize = 13.sp,
                color = LearnifyText
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = isPopular,
                onClick = {
                    isPopular = !isPopular
                }
            )

            Text(
                text = "Popular Course",
                fontSize = 13.sp,
                color = LearnifyText
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (saveError != null) {

            Text(
                text = "Error: $saveError",
                color = LearnifyRed,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        if (saveSuccess != null) {

            Text(
                text = saveSuccess!!,
                color = LearnifyGreen,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        Button(
            onClick = {

                saveError = null
                saveSuccess = null

                if (title.isBlank()) {

                    saveError =
                        "Course title is required"

                    return@Button
                }

                if (youtubeLink.isBlank()) {

                    saveError =
                        "YouTube video ID or link is required"

                    return@Button
                }

                val validVideoId = try {

                    CourseRepository.extractYoutubeVideoId(
                        youtubeLink.trim()
                    )

                } catch (_: Exception) {

                    null
                }

                if (
                    validVideoId == null ||
                    validVideoId.length != 11
                ) {

                    saveError =
                        "Please enter a valid YouTube video ID or link"

                    return@Button
                }

                isSaving = true

                scope.launch {

                    try {

                        val newCourse = Course(
                            title = title.trim(),
                            instructorName =
                                instructorName.trim(),
                            description =
                                description.trim(),
                            thumbnailUrl =
                                CourseRepository
                                    .getYoutubeThumbnailUrl(
                                        validVideoId
                                    ),
                            durationMinutes =
                                durationMinutesText
                                    .toIntOrNull()
                                    ?: 0,
                            category =
                                category.trim(),
                            featured =
                                isFeatured,
                            popular =
                                isPopular
                        )

                        CourseRepository.addCourse(
                            course = newCourse,
                            youtubeVideoId =
                                validVideoId
                        )

                        saveSuccess =
                            "Course added successfully"

                        title = ""
                        instructorName = ""
                        description = ""
                        category = ""
                        durationMinutesText = ""
                        youtubeLink = ""
                        isFeatured = false
                        isPopular = false

                        refreshCourses()

                    } catch (exception: Exception) {

                        saveError =
                            exception.message
                                ?: "Something went wrong"

                    } finally {

                        isSaving = false
                    }
                }
            },

            enabled =
                !isSaving &&
                        videoId != null &&
                        videoId.length == 11,

            modifier = Modifier.fillMaxWidth()
        ) {

            if (isSaving) {

                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            } else {

                Text("Add Course")
            }
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Existing Courses",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LearnifyText
                )

                Text(
                    text = "${courses.size} course${if (courses.size == 1) "" else "s"}",
                    fontSize = 12.sp,
                    color = LearnifyGray
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        if (isLoadingCourses) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 30.dp
                    ),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = LearnifyBlue
                )
            }

        } else if (courses.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LearnifyCardBg
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "No courses yet",
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color = LearnifyText
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Courses you add will appear here.",
                        fontSize = 12.sp,
                        color = LearnifyGray
                    )
                }
            }

        } else {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                courses.forEach { course ->

                    CourseAdminCard(
                        course = course,
                        isDeleting =
                            deletingCourseId ==
                                    course.id,
                        onDeleteClick = {
                            courseToDelete = course
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun CourseAdminCard(
    course: Course,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            AsyncImage(
                model = course.thumbnailUrl,
                contentDescription = course.title,
                modifier = Modifier
                    .size(
                        width = 105.dp,
                        height = 70.dp
                    )
                    .clip(
                        RoundedCornerShape(10.dp)
                    ),
                contentScale = ContentScale.Crop
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = course.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LearnifyText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = course.category.ifBlank {
                        "Uncategorized"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LearnifyBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    if (
                        course.instructorName
                            .isNotBlank()
                    ) {

                        Text(
                            text =
                                course.instructorName,
                            fontSize = 11.sp,
                            color = LearnifyGray,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis,
                            modifier =
                                Modifier.weight(
                                    1f,
                                    fill = false
                                )
                        )
                    }

                    if (
                        course.instructorName
                            .isNotBlank() &&
                        course.durationMinutes > 0
                    ) {

                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = LearnifyGray
                        )
                    }

                    if (
                        course.durationMinutes > 0
                    ) {

                        Text(
                            text =
                                "${course.durationMinutes} min",
                            fontSize = 11.sp,
                            color = LearnifyGray
                        )
                    }
                }

                if (
                    course.featured ||
                    course.popular
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(5.dp)
                    ) {

                        if (course.featured) {

                            Text(
                                text = "FEATURED",
                                fontSize = 9.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    LearnifyOrange,
                                modifier =
                                    Modifier
                                        .background(
                                            color =
                                                LearnifyOrange
                                                    .copy(
                                                        alpha =
                                                            0.12f
                                                    ),
                                            shape =
                                                RoundedCornerShape(
                                                    5.dp
                                                )
                                        )
                                        .padding(
                                            horizontal =
                                                6.dp,
                                            vertical =
                                                3.dp
                                        )
                            )
                        }

                        if (course.popular) {

                            Text(
                                text = "POPULAR",
                                fontSize = 9.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    LearnifyGreen,
                                modifier =
                                    Modifier
                                        .background(
                                            color =
                                                LearnifyGreen
                                                    .copy(
                                                        alpha =
                                                            0.12f
                                                    ),
                                            shape =
                                                RoundedCornerShape(
                                                    5.dp
                                                )
                                        )
                                        .padding(
                                            horizontal =
                                                6.dp,
                                            vertical =
                                                3.dp
                                        )
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            if (isDeleting) {

                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = LearnifyBlue,
                    strokeWidth = 2.dp
                )

            } else {

                IconButton(
                    onClick = onDeleteClick
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Delete,
                        contentDescription =
                            "Delete course",
                        tint = LearnifyRed
                    )
                }
            }
        }
    }
}