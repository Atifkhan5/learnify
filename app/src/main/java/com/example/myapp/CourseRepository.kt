package com.example.myapp

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

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

object CourseRepository {

    // TODO: replace with the email you'll use to log in as admin.
    private const val ADMIN_EMAIL = "admin@gmail.com"

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val coursesCollection = firestore.collection("courses")
    private val thumbnailsRef get() = storage.reference.child("course_thumbnails")

    fun isCurrentUserAdmin(): Boolean {
        return auth.currentUser?.email == ADMIN_EMAIL
    }

    suspend fun getAllCourses(): List<Course> {
        return coursesCollection
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(Course::class.java)?.copy(id = doc.id) }
    }

    suspend fun getFeaturedCourses(): List<Course> {
        return coursesCollection
            .whereEqualTo("featured", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(Course::class.java)?.copy(id = doc.id) }
    }

    suspend fun getPopularCourses(): List<Course> {
        return coursesCollection
            .whereEqualTo("popular", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.toObject(Course::class.java)?.copy(id = doc.id) }
    }

    suspend fun getCourseById(courseId: String): Course? {
        val doc = coursesCollection.document(courseId).get().await()
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
                doc.toObject(Lesson::class.java)?.copy(id = doc.id.toIntOrNull() ?: 0)
            }
    }

    suspend fun uploadThumbnail(uri: Uri): String {
        val bucket = storage.app.options.storageBucket
        if (bucket.isNullOrBlank()) {
            throw Exception("Firebase Storage bucket is not configured. Please check your google-services.json and ensure Storage is enabled in the Firebase Console.")
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val fileRef = thumbnailsRef.child(fileName)
        
        try {
            val uploadTask = fileRef.putFile(uri)
            val snapshot = uploadTask.await()
            
            return snapshot.metadata?.reference?.downloadUrl?.await()?.toString()
                ?: throw Exception("Could not get download URL")
        } catch (e: Exception) {
            if (e.message?.contains("Object does not exist", ignoreCase = true) == true) {
                throw Exception("Storage location not found. Ensure you have clicked 'Get Started' in the Storage tab of your Firebase Console and your bucket name matches your project ID.")
            }
            throw e
        }
    }

    suspend fun addCourseWithLessons(course: Course, lessons: List<Pair<String, String>>): String {
        val docRef = coursesCollection.document()
        docRef.set(course.copy(id = docRef.id)).await()

        lessons.forEachIndexed { index, (title, videoId) ->
            docRef.collection("lessons").document("${index + 1}").set(
                mapOf(
                    "title" to title,
                    "youtubeVideoId" to videoId,
                    "durationMinutes" to course.durationMinutes
                )
            ).await()
        }

        return docRef.id
    }

    suspend fun addCourse(course: Course, youtubeVideoId: String): String {
        return addCourseWithLessons(course, listOf("Lesson 1" to youtubeVideoId))
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

        coursesCollection.document(courseId).delete().await()
    }
}