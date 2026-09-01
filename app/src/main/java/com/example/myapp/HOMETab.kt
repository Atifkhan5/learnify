
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val LearnifyBlue = Color(0xFF2563EB)
private val LearnifyPurple = Color(0xFF7C3AED)
private val LearnifyGreen = Color(0xFF16A34A)
private val LearnifyDark = Color(0xFF172554)
private val LearnifyText = Color(0xFF1E293B)
private val LearnifyGray = Color(0xFF64748B)
private val LearnifyCardBg = Color(0xFFF8FAFC)

private val defaultCategories = listOf(
    "Programming",
    "Design",
    "Marketing",
    "Data Science"
)

@Composable
fun HomeTab(
    onMenuClick: () -> Unit = {},
    onCourseClick: (Course) -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var allCourses by remember {
        mutableStateOf<List<Course>>(emptyList())
    }

    var featuredCourses by remember {
        mutableStateOf<List<Course>>(emptyList())
    }

    var popularCourses by remember {
        mutableStateOf<List<Course>>(emptyList())
    }

    var completedCourses by remember {
        mutableStateOf<List<EnrolledCourse>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    var menuExpanded by remember {
        mutableStateOf(false)
    }

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var showSummaryDialog by remember {
        mutableStateOf(false)
    }

    var showAboutDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(refreshKey) {

        isLoading = true
        loadError = null

        try {

            coroutineScope {

                val allDeferred = async {
                    CourseRepository.getAllCourses()
                }

                val featuredDeferred = async {
                    CourseRepository.getFeaturedCourses()
                }

                val popularDeferred = async {
                    CourseRepository.getPopularCourses()
                }

                val enrolledDeferred = async {
                    CourseRepository.getEnrolledCourses()
                }

                allCourses = allDeferred.await()

                featuredCourses = featuredDeferred.await()

                popularCourses = popularDeferred.await()

                completedCourses =
                    enrolledDeferred
                        .await()
                        .filter {
                            it.isCompleted
                        }
            }

        } catch (exception: CancellationException) {

            throw exception

        } catch (exception: Exception) {

            loadError =
                exception.message
                    ?: "Failed to load courses"

        } finally {

            isLoading = false
        }
    }

    val searchResults =
        remember(
            searchQuery,
            allCourses
        ) {

            if (searchQuery.isBlank()) {

                emptyList()

            } else {

                allCourses.filter {

                    it.title.contains(
                        searchQuery,
                        ignoreCase = true
                    )
                }
            }
        }

    val enrolledCount =
        completedCourses.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF6F8FC)
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Welcome back 👋",
                        fontSize = 13.sp,
                        color = LearnifyGray
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Learnify",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LearnifyDark
                    )
                }

                Box {

                    IconButton(
                        onClick = {
                            menuExpanded = true
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "More options",
                            tint = LearnifyDark
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Refresh Courses"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null
                                )
                            },
                            onClick = {

                                menuExpanded = false

                                refreshKey++
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Clear Search"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null
                                )
                            },
                            onClick = {

                                searchQuery = ""

                                menuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Learning Summary"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Summarize,
                                    contentDescription = null
                                )
                            },
                            onClick = {

                                menuExpanded = false

                                showSummaryDialog = true
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "About Learnify"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null
                                )
                            },
                            onClick = {

                                menuExpanded = false

                                showAboutDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                placeholder = {
                    Text(
                        text = "Search courses..."
                    )
                },
                leadingIcon = {

                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = LearnifyGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(15.dp)
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            if (isLoading) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 60.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = LearnifyBlue
                    )
                }

            } else if (loadError != null) {

                Column {

                    Text(
                        text = "Couldn't load courses",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LearnifyText
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = loadError!!,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                }

            } else if (searchQuery.isNotBlank()) {

                Text(
                    text = "Results for \"$searchQuery\"",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LearnifyText
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (searchResults.isEmpty()) {

                    Text(
                        text = "No courses found",
                        fontSize = 14.sp,
                        color = LearnifyGray
                    )

                } else {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        searchResults.forEach { course ->

                            CourseListItem(
                                course = course,
                                onClick = {
                                    onCourseClick(course)
                                }
                            )
                        }
                    }
                }

            } else {

                SectionHeader(
                    title = "Categories"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        defaultCategories,
                        key = {
                            it
                        }
                    ) { category ->

                        CategoryChip(
                            name = category,
                            onClick = {
                                onCategoryClick(category)
                            }
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                SectionHeader(
                    title = "Featured Courses"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (featuredCourses.isEmpty()) {

                    Text(
                        text = "No featured courses yet",
                        fontSize = 13.sp,
                        color = LearnifyGray
                    )

                } else {

                    LazyRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        items(
                            featuredCourses,
                            key = {
                                it.id
                            }
                        ) { course ->

                            CourseCard(
                                course = course,
                                onClick = {
                                    onCourseClick(course)
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                SectionHeader(
                    title = "Popular Courses"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (popularCourses.isEmpty()) {

                    Text(
                        text = "No popular courses yet",
                        fontSize = 13.sp,
                        color = LearnifyGray
                    )

                } else {

                    LazyRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        items(
                            popularCourses,
                            key = {
                                it.id
                            }
                        ) { course ->

                            CourseCard(
                                course = course,
                                onClick = {
                                    onCourseClick(course)
                                }
                            )
                        }
                    }
                }

                if (completedCourses.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )

                    SectionHeader(
                        title = "Completed Courses"
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    LazyRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        items(
                            completedCourses,
                            key = {
                                it.course.id
                            }
                        ) { enrolled ->

                            CourseCard(
                                course = enrolled.course,
                                onClick = {
                                    onCourseClick(
                                        enrolled.course
                                    )
                                },
                                completed = true
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                SectionHeader(
                    title = "All Courses"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                if (allCourses.isEmpty()) {

                    Text(
                        text = "No courses added yet",
                        fontSize = 13.sp,
                        color = LearnifyGray
                    )

                } else {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        allCourses.forEach { course ->

                            CourseListItem(
                                course = course,
                                onClick = {
                                    onCourseClick(course)
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        if (showSummaryDialog) {

            AlertDialog(
                onDismissRequest = {
                    showSummaryDialog = false
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Summarize,
                        contentDescription = null,
                        tint = LearnifyBlue
                    )
                },
                title = {
                    Text(
                        text = "Learning Summary"
                    )
                },
                text = {

                    Column {

                        SummaryRow(
                            icon = Icons.Filled.Bookmark,
                            label = "Courses Available",
                            value = allCourses.size.toString()
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        SummaryRow(
                            icon = Icons.Filled.CheckCircle,
                            label = "Completed Courses",
                            value = enrolledCount.toString()
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        SummaryRow(
                            icon = Icons.Filled.Search,
                            label = "Search Results",
                            value = searchResults.size.toString()
                        )
                    }
                },
                confirmButton = {

                    TextButton(
                        onClick = {
                            showSummaryDialog = false
                        }
                    ) {

                        Text(
                            text = "Close",
                            color = LearnifyBlue
                        )
                    }
                }
            )
        }

        if (showAboutDialog) {

            AlertDialog(
                onDismissRequest = {
                    showAboutDialog = false
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = LearnifyBlue
                    )
                },
                title = {
                    Text(
                        text = "About Learnify"
                    )
                },
                text = {

                    Text(
                        text = "Learnify is an e-learning platform designed to help students discover courses, watch lessons, track their learning progress, and complete courses at their own pace.",
                        fontSize = 14.sp,
                        color = LearnifyGray
                    )
                },
                confirmButton = {

                    TextButton(
                        onClick = {
                            showAboutDialog = false
                        }
                    ) {

                        Text(
                            text = "Close",
                            color = LearnifyBlue
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    LearnifyBlue.copy(
                        alpha = 0.10f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LearnifyBlue
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = LearnifyGray
        )

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LearnifyText
        )
    }
}

@Composable
private fun SectionHeader(
    title: String
) {

    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = LearnifyText
    )
}

@Composable
private fun CategoryChip(
    name: String,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(20.dp)
            )
            .background(
                LearnifyBlue.copy(
                    alpha = 0.10f
                )
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 18.dp,
                vertical = 10.dp
            )
    ) {

        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = LearnifyBlue
        )
    }
}

@Composable
private fun CourseCard(
    course: Course,
    onClick: () -> Unit,
    completed: Boolean = false
) {

    Column(
        modifier = Modifier
            .width(190.dp)
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(
                LearnifyCardBg
            )
            .clickable {
                onClick()
            }
    ) {

        Box {

            AsyncImage(
                model = course.thumbnailUrl,
                contentDescription = course.title,
                placeholder = ColorPainter(
                    Color.LightGray
                ),
                error = ColorPainter(
                    Color.Red
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp
                        )
                    ),
                contentScale = ContentScale.Crop
            )

            if (completed) {

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            LearnifyGreen
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        )
                ) {

                    Text(
                        text = "Completed",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = course.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LearnifyText,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = course.instructorName,
                fontSize = 12.sp,
                color = LearnifyGray,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "${course.durationMinutes} min · ${course.category}",
                fontSize = 11.sp,
                color = LearnifyPurple
            )
        }
    }
}

@Composable
private fun CourseListItem(
    course: Course,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(16.dp)
            )
            .background(
                LearnifyCardBg
            )
            .clickable {
                onClick()
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AsyncImage(
            model = course.thumbnailUrl,
            contentDescription = course.title,
            placeholder = ColorPainter(
                Color.LightGray
            ),
            error = ColorPainter(
                Color.Red
            ),
            modifier = Modifier
                .size(64.dp)
                .clip(
                    RoundedCornerShape(12.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column {

            Text(
                text = course.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LearnifyText,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text =
                    "${course.instructorName} · ${course.category}",
                fontSize = 12.sp,
                color = LearnifyGray,
                maxLines = 1
            )
        }
    }
}
