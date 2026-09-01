package com.example.myapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class Course(
    val id: String = "",
    val title: String = "",
    val instructorName: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val durationMinutes: Int = 0,
    val category: String = "",
    val featured: Boolean = false,
    val popular: Boolean = false
)

data class LessonProgress(
    val lesson: Lesson,
    val isCompleted: Boolean,
    val isUnlocked: Boolean
)

object CourseRepository {

    private const val ADMIN_EMAIL = "admin1@gmail.com"

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val coursesCollection =
        firestore.collection("courses")

    fun isCurrentUserAdmin(): Boolean {
        return auth.currentUser?.email?.equals(
            ADMIN_EMAIL,
            ignoreCase = true
        ) == true
    }

    private fun requireValidCourseId(
        courseId: String
    ): String {

        val id = courseId.trim()

        if (id.isBlank()) {
            throw Exception("Course ID is empty.")
        }

        if (id.contains("/")) {
            throw Exception(
                "Invalid course ID: course IDs cannot contain '/'."
            )
        }

        return id
    }

    suspend fun getAllCourses(): List<Course> {

        return coursesCollection
            .get()
            .await()
            .documents
            .mapNotNull { document ->

                document
                    .toObject(Course::class.java)
                    ?.copy(
                        id = document.id
                    )
            }
    }

    suspend fun getFeaturedCourses(): List<Course> {

        return coursesCollection
            .whereEqualTo(
                "featured",
                true
            )
            .get()
            .await()
            .documents
            .mapNotNull { document ->

                document
                    .toObject(Course::class.java)
                    ?.copy(
                        id = document.id
                    )
            }
    }

    suspend fun getPopularCourses(): List<Course> {

        return coursesCollection
            .whereEqualTo(
                "popular",
                true
            )
            .get()
            .await()
            .documents
            .mapNotNull { document ->

                document
                    .toObject(Course::class.java)
                    ?.copy(
                        id = document.id
                    )
            }
    }

    suspend fun getCourseById(
        courseId: String
    ): Course? {

        val id =
            requireValidCourseId(courseId)

        val document =
            coursesCollection
                .document(id)
                .get()
                .await()

        if (!document.exists()) {
            return null
        }

        return document
            .toObject(Course::class.java)
            ?.copy(
                id = document.id
            )
    }

    suspend fun getEnrolledCourses(): List<EnrolledCourse> {

        val uid =
            auth.currentUser?.uid
                ?: return emptyList()

        val enrollmentSnapshot =
            firestore
                .collection("users")
                .document(uid)
                .collection("enrollments")
                .whereEqualTo(
                    "enrolled",
                    true
                )
                .get()
                .await()

        return enrollmentSnapshot
            .documents
            .mapNotNull { document ->

                val courseId =
                    document.id

                val course =
                    getCourseById(
                        courseId
                    )
                        ?: return@mapNotNull null

                val completedLessons =
                    (
                            document.get(
                                "completedLessons"
                            ) as? List<*>
                            )?.size ?: 0

                val totalLessons =
                    (
                            document.getLong(
                                "totalLessons"
                            ) ?: 0L
                            ).toInt()

                EnrolledCourse(
                    course =
                        course,
                    completedLessons =
                        completedLessons,
                    totalLessons =
                        totalLessons
                )
            }
    }

    suspend fun getLessons(
        courseId: String
    ): List<Lesson> {

        val id =
            requireValidCourseId(courseId)

        return coursesCollection
            .document(id)
            .collection("lessons")
            .orderBy(
                "order",
                com.google.firebase.firestore.Query.Direction.ASCENDING
            )
            .get()
            .await()
            .documents
            .mapNotNull { document ->

                val lesson =
                    document.toObject(
                        Lesson::class.java
                    )

                lesson?.copy(
                    id =
                        document.id.toIntOrNull()
                            ?: 0
                )
            }
    }

    suspend fun getCompletedLessonIds(
        courseId: String
    ): Set<Int> {

        val uid =
            auth.currentUser?.uid
                ?: return emptySet()

        val id =
            requireValidCourseId(courseId)

        val enrollmentDocument =
            firestore
                .collection("users")
                .document(uid)
                .collection("enrollments")
                .document(id)
                .get()
                .await()

        return (
                enrollmentDocument.get(
                    "completedLessons"
                ) as? List<*>
                )
            ?.mapNotNull { value ->

                when (value) {

                    is Number ->
                        value.toInt()

                    is String ->
                        value.toIntOrNull()

                    else ->
                        null
                }
            }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun getLessonsWithProgress(
        courseId: String
    ): List<LessonProgress> {

        val lessons =
            getLessons(courseId)
                .sortedBy { it.id }

        val completedIds =
            getCompletedLessonIds(
                courseId
            )

        return lessons.mapIndexed { index, lesson ->

            val isCompleted =
                completedIds.contains(
                    lesson.id
                )

            val isUnlocked =
                index == 0 ||
                        completedIds.contains(
                            lessons[index - 1].id
                        )

            LessonProgress(
                lesson =
                    lesson,
                isCompleted =
                    isCompleted,
                isUnlocked =
                    isUnlocked
            )
        }
    }

    suspend fun markLessonCompleted(
        courseId: String,
        lessonId: Int,
        lessonDurationMinutes: Int
    ) {

        val uid =
            auth.currentUser?.uid
                ?: return

        val id =
            requireValidCourseId(courseId)

        val alreadyCompleted =
            getCompletedLessonIds(
                id
            ).contains(
                lessonId
            )

        firestore
            .collection("users")
            .document(uid)
            .collection("enrollments")
            .document(id)
            .set(
                mapOf(
                    "enrolled" to true,
                    "completedLessons" to
                            FieldValue.arrayUnion(
                                lessonId
                            )
                ),
                SetOptions.merge()
            )
            .await()

        if (!alreadyCompleted) {

            firestore
                .collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "totalMinutesWatched" to
                                FieldValue.increment(
                                    lessonDurationMinutes
                                        .toLong()
                                )
                    ),
                    SetOptions.merge()
                )
                .await()
        }
    }

    suspend fun getTotalMinutesWatched(): Int {

        val uid =
            auth.currentUser?.uid
                ?: return 0

        val document =
            firestore
                .collection("users")
                .document(uid)
                .get()
                .await()

        return (
                document.getLong(
                    "totalMinutesWatched"
                ) ?: 0L
                ).toInt()
    }

    fun extractYoutubeVideoId(
        urlOrId: String
    ): String {

        val input =
            urlOrId.trim()

        if (
            input.length == 11 &&
            input.matches(
                Regex(
                    "[a-zA-Z0-9_-]{11}"
                )
            )
        ) {
            return input
        }

        val patterns =
            listOf(

                Regex(
                    """(?:https?://)?(?:www\.)?youtube\.com/watch\?[^#]*[?&]v=([a-zA-Z0-9_-]{11})"""
                ),

                Regex(
                    """(?:https?://)?(?:www\.)?youtube\.com/embed/([a-zA-Z0-9_-]{11})"""
                ),

                Regex(
                    """(?:https?://)?(?:www\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})"""
                ),

                Regex(
                    """(?:https?://)?youtu\.be/([a-zA-Z0-9_-]{11})"""
                ),

                Regex(
                    """(?:https?://)?(?:www\.)?youtube-nocookie\.com/embed/([a-zA-Z0-9_-]{11})"""
                )
            )

        for (pattern in patterns) {

            val match =
                pattern.find(input)

            if (match != null) {

                return match
                    .groupValues[1]
            }
        }

        throw Exception(
            "Invalid YouTube video link"
        )
    }

    fun getYoutubeThumbnailUrl(
        videoId: String
    ): String {

        return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    }

    suspend fun addCourseWithLessons(
        course: Course,
        lessons: List<Pair<String, String>>
    ): String {

        if (lessons.isEmpty()) {

            throw Exception(
                "At least one lesson is required."
            )
        }

        val firstVideoId =
            extractYoutubeVideoId(
                lessons.first().second
            )

        val thumbnailUrl =
            getYoutubeThumbnailUrl(
                firstVideoId
            )

        val courseDocument =
            coursesCollection.document()

        val courseId =
            courseDocument.id

        val courseData =
            course.copy(
                id = courseId,
                thumbnailUrl =
                    thumbnailUrl
            )

        courseDocument
            .set(courseData)
            .await()

        lessons.forEachIndexed { index, lesson ->

            val lessonTitle =
                lesson.first

            val videoId =
                extractYoutubeVideoId(
                    lesson.second
                )

            val lessonDocument =
                courseDocument
                    .collection("lessons")
                    .document(
                        (index + 1).toString()
                    )

            lessonDocument
                .set(
                    mapOf(
                        "title" to
                                lessonTitle,
                        "description" to
                                "",
                        "youtubeVideoId" to
                                videoId,
                        "durationMinutes" to
                                course.durationMinutes,
                        "order" to
                                (index + 1),
                        "createdAt" to
                                System.currentTimeMillis()
                    )
                )
                .await()
        }

        return courseId
    }

    suspend fun addCourse(
        course: Course,
        youtubeVideoId: String
    ): String {

        val videoId =
            extractYoutubeVideoId(
                youtubeVideoId
            )

        val thumbnailUrl =
            getYoutubeThumbnailUrl(
                videoId
            )

        val courseWithThumbnail =
            course.copy(
                thumbnailUrl =
                    thumbnailUrl
            )

        return addCourseWithLessons(
            course =
                courseWithThumbnail,
            lessons =
                listOf(
                    "Lesson 1" to videoId
                )
        )
    }

    suspend fun deleteCourse(
        courseId: String
    ) {

        val id =
            requireValidCourseId(
                courseId
            )

        val courseDocument =
            coursesCollection
                .document(id)

        val lessonSnapshot =
            courseDocument
                .collection("lessons")
                .get()
                .await()

        for (
        lessonDocument
        in lessonSnapshot.documents
        ) {

            lessonDocument
                .reference
                .delete()
                .await()
        }

        courseDocument
            .delete()
            .await()
    }
}