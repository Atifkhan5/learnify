package com.example.myapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LearnifyBlue = Color(0xFF2563EB)
private val LearnifyGray = Color(0xFF64748B)
private val LearnifyBackground = Color(0xFFF6F8FC)

@Composable
fun HomeScreen() {

    var selectedTab by remember { mutableIntStateOf(0) }

    // Drives Home -> Course Details -> Lesson Player navigation.
    // selectedCourse == null: showing the bottom-tab content.
    // selectedCourse != null, selectedLesson == null: showing course details.
    // both non-null: showing the lesson player.
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var selectedLessonTotalCount by remember { mutableIntStateOf(0) }

    val isAdmin = remember { CourseRepository.isCurrentUserAdmin() }

    when {

        selectedCourse != null && selectedLesson != null -> {

            LessonPlayerScreen(
                courseId = selectedCourse!!.id,
                lesson = selectedLesson!!,
                totalLessons = selectedLessonTotalCount,
                isCompleted = false,
                onBackClick = {
                    selectedLesson = null
                },
                onLessonCompleted = {},
                onLessonUncompleted = {}
            )
        }

        selectedCourse != null -> {

            CourseDetailsScreen(
                course = selectedCourse!!,
                onBackClick = {
                    selectedCourse = null
                },
                onLessonClick = { lesson, totalCount ->
                    selectedLesson = lesson
                    selectedLessonTotalCount = totalCount
                }
            )
        }

        else -> {

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = LearnifyBackground,

                bottomBar = {

                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {

                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home",
                                    tint = if (selectedTab == 0) LearnifyBlue else LearnifyGray
                                )
                            },
                            label = { Text(text = "Home", fontSize = 11.sp) }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "My Courses",
                                    tint = if (selectedTab == 1) LearnifyBlue else LearnifyGray
                                )
                            },
                            label = { Text(text = "My Courses", fontSize = 11.sp) }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (selectedTab == 2) LearnifyBlue else LearnifyGray
                                )
                            },
                            label = { Text(text = "Search", fontSize = 11.sp) }
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = "Dashboard",
                                    tint = if (selectedTab == 3) LearnifyBlue else LearnifyGray
                                )
                            },
                            label = { Text(text = "Dashboard", fontSize = 11.sp) }
                        )

                        if (isAdmin) {
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin",
                                        tint = if (selectedTab == 4) LearnifyBlue else LearnifyGray
                                    )
                                },
                                label = { Text(text = "Admin", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            ) { paddingValues ->

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(LearnifyBackground)
                ) {

                    when (selectedTab) {

                        0 -> {
                            HomeTab(
                                onCourseClick = { course ->
                                    selectedCourse = course
                                }
                            )
                        }

                        1 -> {
                            MyCoursesScreen(
                                onCourseClick = { course ->
                                    selectedCourse = course
                                }
                            )
                        }

                        2 -> {
                            SearchScreen()
                        }

                        3 -> {
                            DashboardScreen()
                        }

                        4 -> {
                            if (isAdmin) {
                                AdminScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Search")
    }
}

@Composable
fun DashboardScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Dashboard")
    }
}