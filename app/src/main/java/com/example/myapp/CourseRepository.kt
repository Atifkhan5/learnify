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
    private val coursesCollection = firestore.collection("courses")

    fun isCurrentUserAdmin(): Boolean {
        return auth.currentUser?.email == ADMIN_EMAIL
    }

    suspend fun getAllCourses(): List<Course> {
        return coursesCollection
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Course::class.java)?.copy(id = doc.id)
            }
    }

    suspend fun getFeaturedCourses(): List<Course> {
        return coursesCollection
            .whereEqualTo("featured", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Course::class.java)?.copy(id = doc.id)
            }
    }

    suspend fun getPopularCourses(): List<Course> {
        return coursesCollection
            .whereEqualTo("popular", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Course::class.java)?.copy(id = doc.id)
            }
    }

    suspend fun getCourseById(courseId: String): Course? {
        val doc = coursesCollection
            .document(courseId)
            .get()
            .await()

        return doc.toObject(Course::class.java)?.copy(id = doc.id)
    }

    suspend fun getEnrolledCourses(): List<EnrolledCourse> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        val enrollmentDocs = firestore
            .collection("users")
            .document(uid)
            .collection("enrollments")
            .whereEqualTo("enrolled", true)
            .get()
            .await()

        return enrollmentDocs.documents.mapNotNull { doc ->
            val course = getCourseById(doc.id) ?: return@mapNotNull null

            val completedLessons =
                (doc.get("completedLessons") as? List<*>)?.size ?: 0

            val totalLessons =
                (doc.getLong("totalLessons") ?: 0L).toInt()

            EnrolledCourse(
                course = course,
                completedLessons = completedLessons,
                totalLessons = totalLessons
            )
        }
    }

    suspend fun getLessons(courseId: String): List<Lesson> {
        return coursesCollection
            .document(courseId)
            .collection("lessons")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Lesson::class.java)?.copy(
                    id = doc.id.toIntOrNull() ?: 0
                )
            }
    }

    suspend fun getCompletedLessonIds(courseId: String): Set<Int> {
        val uid = auth.currentUser?.uid ?: return emptySet()

        val enrollmentDoc = firestore
            .collection("users")
            .document(uid)
            .collection("enrollments")
            .document(courseId)
            .get()
            .await()

        return (enrollmentDoc.get("completedLessons") as? List<*>)
            ?.mapNotNull { entry ->
                (entry as? Number)?.toInt() ?: (entry as? String)?.toIntOrNull()
            }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun getLessonsWithProgress(courseId: String): List<LessonProgress> {
        val lessons = getLessons(courseId).sortedBy { it.id }
        val completedIds = getCompletedLessonIds(courseId)

        return lessons.mapIndexed { index, lesson ->
            val isCompleted = completedIds.contains(lesson.id)
            val isUnlocked = index == 0 || completedIds.contains(lessons[index - 1].id)

            LessonProgress(
                lesson = lesson,
                isCompleted = isCompleted,
                isUnlocked = isUnlocked
            )
        }
    }

    suspend fun markLessonCompleted(
        courseId: String,
        lessonId: Int,
        lessonDurationMinutes: Int
    ) {
        val uid = auth.currentUser?.uid ?: return

        val alreadyCompleted = getCompletedLessonIds(courseId).contains(lessonId)

        firestore
            .collection("users")
            .document(uid)
            .collection("enrollments")
            .document(courseId)
            .set(
                mapOf(
                    "enrolled" to true,
                    "completedLessons" to FieldValue.arrayUnion(lessonId)
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
                        "totalMinutesWatched" to FieldValue.increment(
                            lessonDurationMinutes.toLong()
                        )
                    ),
                    SetOptions.merge()
                )
                .await()
        }
    }

    suspend fun getTotalMinutesWatched(): Int {
        val uid = auth.currentUser?.uid ?: return 0

        val doc = firestore
            .collection("users")
            .document(uid)
            .get()
            .await()

        return (doc.getLong("totalMinutesWatched") ?: 0L).toInt()
    }

    fun extractYoutubeVideoId(urlOrId: String): String {
        val input = urlOrId.trim()

        val patterns = listOf(
            Regex("""(?:youtube\.com/watch\?v=)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:www\.youtube\.com/watch\?v=)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:youtu\.be/)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:www\.youtu\.be/)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:youtube\.com/embed/)([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:www\.youtube\.com/embed/)([a-zA-Z0-9_-]{11})"""),
            Regex("""^([a-zA-Z0-9_-]{11})$""")
        )

        for (pattern in patterns) {
            val match = pattern.find(input)

            if (match != null) {
                return match.groupValues[1]
            }
        }

        throw Exception("Invalid YouTube video link")
    }

    fun getYoutubeThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    }

    suspend fun addCourseWithLessons(
        course: Course,
        lessons: List<Pair<String, String>>
    ): String {

        if (lessons.isEmpty()) {
            throw Exception("At least one lesson is required")
        }

        val firstVideoId = extractYoutubeVideoId(
            lessons.first().second
        )

        val thumbnailUrl = getYoutubeThumbnailUrl(
            firstVideoId
        )

        val courseWithThumbnail = course.copy(
            thumbnailUrl = thumbnailUrl
        )

        val docRef = coursesCollection.document()

        docRef.set(
            courseWithThumbnail.copy(
                id = docRef.id
            )
        ).await()

        lessons.forEachIndexed { index, lesson ->

            val lessonTitle = lesson.first

            val videoId = extractYoutubeVideoId(
                lesson.second
            )

            docRef
                .collection("lessons")
                .document("${index + 1}")
                .set(
                    mapOf(
                        "title" to lessonTitle,
                        "youtubeVideoId" to videoId,
                        "durationMinutes" to course.durationMinutes
                    )
                )
                .await()
        }

        return docRef.id
    }

    suspend fun addCourse(
        course: Course,
        youtubeVideoId: String
    ): String {

        val videoId = extractYoutubeVideoId(
            youtubeVideoId
        )

        val thumbnailUrl = getYoutubeThumbnailUrl(
            videoId
        )

        val courseWithThumbnail = course.copy(
            thumbnailUrl = thumbnailUrl
        )

        return addCourseWithLessons(
            courseWithThumbnail,
            listOf("Lesson 1" to videoId)
        )
    }

    suspend fun deleteCourse(courseId: String) {

        val lessonDocs = coursesCollection
            .document(courseId)
            .collection("lessons")
            .get()
            .await()

        for (lessonDoc in lessonDocs.documents) {
            lessonDoc.reference.delete().await()
        }

        coursesCollection
            .document(courseId)
            .delete()
            .await()
    }
}