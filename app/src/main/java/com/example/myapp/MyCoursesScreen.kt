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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException

private val LearnifyBlue = Color(0xFF2563EB)
private val LearnifyDark = Color(0xFF172554)
private val LearnifyText = Color(0xFF1E293B)
private val LearnifyGray = Color(0xFF64748B)
private val LearnifyCardBg = Color(0xFFF8FAFC)

data class EnrolledCourse(
    val course: Course,
    val completedLessons: Int,
    val totalLessons: Int
) {
    val progress: Float
        get() = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons
}

@Composable
fun MyCoursesScreen(
    modifier: Modifier = Modifier,
    onCourseClick: (Course) -> Unit = {}
) {

    var enrolledCourses by remember { mutableStateOf<List<EnrolledCourse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            enrolledCourses = CourseRepository.getEnrolledCourses()
            // DEBUG: confirm what the repository actually returned
            Log.d("COURSES_DEBUG", "Loaded ${enrolledCourses.size} courses")
            enrolledCourses.forEach {
                Log.d("COURSES_DEBUG", "Course: ${it.course.title}, thumbnailUrl: '${it.course.thumbnailUrl}'")
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            loadError = exception.message ?: "Failed to load your courses"
            Log.e("COURSES_DEBUG", "Failed to load courses", exception)
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {

            Text(
                text = "My Courses",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LearnifyDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Track your enrolled courses and progress",
                fontSize = 13.sp,
                color = LearnifyGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {

                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LearnifyBlue)
                    }
                }

                loadError != null -> {
                    Text(
                        text = "Couldn't load your courses: $loadError",
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

                enrolledCourses.isEmpty() -> {
                    EmptyState()
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        enrolledCourses.forEach { enrolled ->
                            EnrolledCourseCard(
                                enrolled = enrolled,
                                onClick = { onCourseClick(enrolled.course) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No enrolled courses yet",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LearnifyText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Browse courses on the Home tab and enroll to see them here",
            fontSize = 13.sp,
            color = LearnifyGray
        )
    }
}

@Composable
private fun EnrolledCourseCard(
    enrolled: EnrolledCourse,
    onClick: () -> Unit
) {
    val course = enrolled.course

    // DEBUG: log the URL each card actually receives
    LaunchedEffect(course.thumbnailUrl) {
        Log.d("THUMBNAIL_DEBUG", "Course: ${course.title}, URL: '${course.thumbnailUrl}'")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LearnifyCardBg)
            .clickable { onClick() }
            .padding(14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            AsyncImage(
                model = course.thumbnailUrl,
                contentDescription = course.title,
                placeholder = ColorPainter(Color.LightGray), // shown while loading
                error = ColorPainter(Color.Red),             // shown if load fails — diagnostic
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LearnifyText,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = course.instructorName,
                    fontSize = 12.sp,
                    color = LearnifyGray,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { enrolled.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = LearnifyBlue,
            trackColor = LearnifyBlue.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${enrolled.completedLessons}/${enrolled.totalLessons} lessons completed",
                fontSize = 11.sp,
                color = LearnifyGray
            )
            Text(
                text = "${(enrolled.progress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = LearnifyBlue
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (enrolled.progress >= 1f) "Review Course" else "Continue Learning")
        }
    }
}