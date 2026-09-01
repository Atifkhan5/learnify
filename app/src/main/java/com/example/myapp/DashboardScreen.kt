
package com.example.myapp

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val DashboardBlue = Color(0xFF2563EB)
private val DashboardPurple = Color(0xFF7C3AED)
private val DashboardDark = Color(0xFF172554)
private val DashboardText = Color(0xFF1E293B)
private val DashboardGray = Color(0xFF64748B)
private val DashboardGreen = Color(0xFF16A34A)
private val DashboardLightGreen = Color(0xFFF0FDF4)
private val DashboardLightBlue = Color(0xFFEFF6FF)
private val DashboardBackground = Color(0xFFF8FAFC)

@Composable
fun DashboardScreen(
    onCourseClick: (Course) -> Unit = {}
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    var enrolledCourses by remember {
        mutableStateOf<List<EnrolledCourse>>(emptyList())
    }

    var totalMinutesWatched by remember {
        mutableStateOf(0)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        if (auth.currentUser == null) {
            isLoading = false
            errorMessage = "Please login to view your dashboard"
            return@LaunchedEffect
        }

        try {

            coroutineScope {

                val coursesDeferred = async {
                    CourseRepository.getEnrolledCourses()
                }

                val minutesDeferred = async {
                    CourseRepository.getTotalMinutesWatched()
                }

                enrolledCourses = coursesDeferred.await()
                totalMinutesWatched = minutesDeferred.await()
            }

        } catch (exception: CancellationException) {

            throw exception

        } catch (exception: Exception) {

            errorMessage =
                exception.message ?: "Failed to load dashboard"

        } finally {

            isLoading = false
        }
    }

    val totalLessons = enrolledCourses.sumOf {
        it.totalLessons
    }

    val completedLessons = enrolledCourses.sumOf {
        it.completedLessons
    }

    val completedCourses = enrolledCourses.count {
        it.isCompleted
    }

    val overallProgress =
        if (totalLessons > 0) {
            completedLessons.toFloat() / totalLessons.toFloat()
        } else {
            0f
        }

    val progressPercentage =
        (overallProgress * 100).toInt()

    val inProgressCourses =
        enrolledCourses.filter {
            !it.isCompleted
        }

    val finishedCourses =
        enrolledCourses.filter {
            it.isCompleted
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBackground)
    ) {

        when {

            isLoading -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = DashboardBlue
                    )
                }
            }

            errorMessage != null -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "Dashboard",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardDark
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    Text(
                        text = errorMessage ?: "Something went wrong",
                        fontSize = 14.sp,
                        color = DashboardGray
                    )
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = 35.dp
                    )
                ) {

                    item {

                        DashboardHeader(
                            progressPercentage = progressPercentage
                        )
                    }

                    item {

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        DashboardStatistics(
                            courseCount = enrolledCourses.size,
                            completedLessons = completedLessons,
                            completedCourses = completedCourses,
                            totalMinutesWatched = totalMinutesWatched
                        )
                    }

                    item {

                        Spacer(
                            modifier = Modifier.height(25.dp)
                        )

                        Text(
                            text = "Overall Progress",
                            modifier = Modifier.padding(
                                horizontal = 20.dp
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardText
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        OverallProgressCard(
                            progress = overallProgress,
                            progressPercentage = progressPercentage,
                            completedLessons = completedLessons,
                            totalLessons = totalLessons
                        )
                    }

                    item {

                        Spacer(
                            modifier = Modifier.height(25.dp)
                        )

                        Text(
                            text = "Enrolled Courses",
                            modifier = Modifier.padding(
                                horizontal = 20.dp
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardText
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )
                    }

                    if (inProgressCourses.isEmpty()) {

                        item {

                            EmptyDashboardCard(
                                message = "No courses currently in progress"
                            )
                        }

                    } else {

                        items(
                            items = inProgressCourses,
                            key = {
                                it.course.id
                            }
                        ) { enrolled ->

                            DashboardCourseCard(
                                enrolled = enrolled,
                                onClick = {
                                    onCourseClick(
                                        enrolled.course
                                    )
                                }
                            )
                        }
                    }

                    if (finishedCourses.isNotEmpty()) {

                        item {

                            Spacer(
                                modifier = Modifier.height(25.dp)
                            )

                            Text(
                                text = "Completed Courses",
                                modifier = Modifier.padding(
                                    horizontal = 20.dp
                                ),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DashboardText
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )
                        }

                        items(
                            items = finishedCourses,
                            key = {
                                "completed_${it.course.id}"
                            }
                        ) { enrolled ->

                            DashboardCourseCard(
                                enrolled = enrolled,
                                onClick = {
                                    onCourseClick(
                                        enrolled.course
                                    )
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
private fun DashboardHeader(
    progressPercentage: Int
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DashboardBlue,
                        DashboardPurple
                    )
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 22.dp,
                    vertical = 30.dp
                )
        ) {

            Text(
                text = "My Dashboard",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = "Keep learning and reach your goals",
                fontSize = 14.sp,
                color = Color.White.copy(
                    alpha = 0.85f
                )
            )

            Spacer(
                modifier = Modifier.height(27.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Your Progress",
                        fontSize = 13.sp,
                        color = Color.White.copy(
                            alpha = 0.8f
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "$progressPercentage%",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(
                            RoundedCornerShape(22.dp)
                        )
                        .background(
                            Color.White.copy(
                                alpha = 0.15f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "$progressPercentage%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatistics(
    courseCount: Int,
    completedLessons: Int,
    completedCourses: Int,
    totalMinutesWatched: Int
) {

    val hours =
        totalMinutesWatched / 60

    val minutes =
        totalMinutesWatched % 60

    val watchTime =
        if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 15.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(
            10.dp
        )
    ) {

        DashboardStatCard(
            modifier = Modifier.weight(1f),
            icon = "📚",
            value = courseCount.toString(),
            label = "Courses"
        )

        DashboardStatCard(
            modifier = Modifier.weight(1f),
            icon = "✓",
            value = completedLessons.toString(),
            label = "Lessons"
        )

        DashboardStatCard(
            modifier = Modifier.weight(1f),
            icon = "⏱",
            value = watchTime,
            label = "Watch Time"
        )
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier,
    icon: String,
    value: String,
    label: String
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 15.dp,
                    horizontal = 5.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = icon,
                fontSize = 18.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DashboardBlue
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = label,
                fontSize = 10.sp,
                color = DashboardGray
            )
        }
    }
}

@Composable
private fun OverallProgressCard(
    progress: Float,
    progressPercentage: Int,
    completedLessons: Int,
    totalLessons: Int
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "Learning Progress",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardText
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "$completedLessons of $totalLessons lessons completed",
                        fontSize = 12.sp,
                        color = DashboardGray
                    )
                }

                Text(
                    text = "$progressPercentage%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DashboardBlue
                )
            }

            Spacer(
                modifier = Modifier.height(13.dp)
            )

            LinearProgressIndicator(
                progress = {
                    progress.coerceIn(
                        0f,
                        1f
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    ),
                color = DashboardBlue,
                trackColor = DashboardLightBlue
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = when {
                    progressPercentage == 100 ->
                        "🎉 You've completed all your lessons!"

                    progressPercentage >= 75 ->
                        "You're almost there! Keep going."

                    progressPercentage >= 50 ->
                        "Great progress! Keep it up."

                    progressPercentage > 0 ->
                        "Good start! Continue learning."

                    else ->
                        "Start a lesson to begin your progress."
                },
                fontSize = 12.sp,
                color = DashboardGray
            )
        }
    }
}

@Composable
private fun DashboardCourseCard(
    enrolled: EnrolledCourse,
    onClick: () -> Unit
) {

    val course = enrolled.course

    val progress =
        enrolled.progress.coerceIn(
            0f,
            1f
        )

    val percentage =
        (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp,
                vertical = 6.dp
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column {

            if (course.thumbnailUrl.isNotBlank()) {

                AsyncImage(
                    model = course.thumbnailUrl,
                    contentDescription = course.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp
                            )
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    DashboardBlue,
                                    DashboardPurple
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "Course",
                        modifier = Modifier.size(45.dp),
                        tint = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier.padding(17.dp)
            ) {

                Text(
                    text = course.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardText
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = course.instructorName,
                    fontSize = 12.sp,
                    color = DashboardGray
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "${enrolled.completedLessons}/${enrolled.totalLessons} lessons",
                        fontSize = 12.sp,
                        color = DashboardGray
                    )

                    Text(
                        text = "$percentage%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enrolled.isCompleted) {
                            DashboardGreen
                        } else {
                            DashboardBlue
                        }
                    )
                }

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                LinearProgressIndicator(
                    progress = {
                        progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        ),
                    color = if (enrolled.isCompleted) {
                        DashboardGreen
                    } else {
                        DashboardBlue
                    },
                    trackColor = if (enrolled.isCompleted) {
                        DashboardLightGreen
                    } else {
                        DashboardLightBlue
                    }
                )

                Spacer(
                    modifier = Modifier.height(13.dp)
                )

                if (enrolled.isCompleted) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(18.dp),
                            tint = DashboardGreen
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "Course Completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardGreen
                        )
                    }

                } else {

                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DashboardBlue
                        )
                    ) {

                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Continue",
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(7.dp)
                        )

                        Text(
                            text = "Continue Learning",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboardCard(
    message: String
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 20.dp
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = "No courses",
                modifier = Modifier.size(40.dp),
                tint = DashboardGray
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = message,
                fontSize = 14.sp,
                color = DashboardGray
            )
        }
    }
}
