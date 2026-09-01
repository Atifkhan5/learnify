package com.example.myapp

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch

private val LearnifyBlue = Color(0xFF2563EB)
private val LearnifyGreen = Color(0xFF16A34A)
private val LearnifyDark = Color(0xFF172554)
private val LearnifyText = Color(0xFF1E293B)
private val LearnifyGray = Color(0xFF64748B)
private val LearnifyCardBg = Color(0xFFF8FAFC)

@Composable
fun CourseDetailsScreen(
    modifier: Modifier = Modifier,
    course: Course,
    onBackClick: () -> Unit = {},
    onLessonClick: (Lesson, Int) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()

    var lessonProgressList by remember { mutableStateOf<List<LessonProgress>>(emptyList()) }
    var isLoadingLessons by remember { mutableStateOf(true) }
    var lessonsError by remember { mutableStateOf<String?>(null) }

    var isEnrolled by remember { mutableStateOf(false) }
    var isCheckingEnrollment by remember { mutableStateOf(true) }
    var isEnrolling by remember { mutableStateOf(false) }
    var enrollError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(course.id) {
        try {
            lessonProgressList = CourseRepository.getLessonsWithProgress(course.id)
        } catch (exception: Exception) {
            lessonsError = exception.message ?: "Failed to load lessons"
        } finally {
            isLoadingLessons = false
        }

        try {
            val progress = CourseProgressRepository.getCourseProgress(course.id)
            isEnrolled = progress.enrolled
        } catch (exception: Exception) {
        } finally {
            isCheckingEnrollment = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        Box {
            AsyncImage(
                model = course.thumbnailUrl,
                contentDescription = course.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LearnifyDark
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {

            Text(
                text = course.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LearnifyDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = course.instructorName,
                fontSize = 14.sp,
                color = LearnifyGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${course.durationMinutes} min · ${course.category}",
                fontSize = 13.sp,
                color = LearnifyBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (course.description.isNotBlank()) {
                Text(
                    text = course.description,
                    fontSize = 14.sp,
                    color = LearnifyText
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (enrollError != null) {
                Text(
                    text = "Couldn't enroll: $enrollError",
                    color = Color.Red,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    if (!isEnrolled) {
                        enrollError = null
                        isEnrolling = true
                        scope.launch {
                            try {
                                CourseProgressRepository.enrollCourse(
                                    courseId = course.id,
                                    totalLessons = lessonProgressList.size
                                )
                                isEnrolled = true
                            } catch (exception: Exception) {
                                enrollError = exception.message ?: "Something went wrong"
                            } finally {
                                isEnrolling = false
                            }
                        }
                    }
                },
                enabled = !isEnrolling && !isCheckingEnrollment,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnrolled) LearnifyGreen else LearnifyBlue
                )
            ) {
                if (isEnrolling || isCheckingEnrollment) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        color = Color.White
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEnrolled) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isEnrolled) "Enrolled" else "Enroll Now")
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Lessons",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LearnifyText
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoadingLessons -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LearnifyBlue)
                    }
                }

                lessonsError != null -> {
                    Text(
                        text = "Couldn't load lessons: $lessonsError",
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                lessonProgressList.isEmpty() -> {
                    Text(
                        text = "No lessons added yet",
                        fontSize = 13.sp,
                        color = LearnifyGray
                    )
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        lessonProgressList.forEachIndexed { index, progress ->
                            val locked = !isEnrolled || !progress.isUnlocked

                            LessonRow(
                                index = index + 1,
                                lesson = progress.lesson,
                                completed = progress.isCompleted,
                                locked = locked,
                                onClick = {
                                    if (!locked) {
                                        onLessonClick(progress.lesson, lessonProgressList.size)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(
    index: Int,
    lesson: Lesson,
    completed: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LearnifyCardBg)
            .clickable(enabled = !locked) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LearnifyGray
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lesson.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LearnifyText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${lesson.durationMinutes} min",
                fontSize = 12.sp,
                color = LearnifyGray
            )
        }

        when {
            completed -> {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Completed",
                    tint = LearnifyGreen
                )
            }
            locked -> {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = LearnifyGray
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "Play",
                    tint = LearnifyBlue
                )
            }
        }
    }
}