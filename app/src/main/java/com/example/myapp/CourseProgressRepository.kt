package com.example.myapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class CourseProgress(
    val courseId: String = "",
    val enrolled: Boolean = false,
    val completedLessons: List<Int> = emptyList(),
    val progress: Int = 0,
    val totalLessons: Int = 0
)

object CourseProgressRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private fun enrollmentReference(courseId: String) =
        firestore
            .collection("users")
            .document(auth.currentUser?.uid ?: "")
            .collection("enrollments")
            .document(courseId)

    suspend fun getCourseProgress(
        courseId: String
    ): CourseProgress {

        val uid = auth.currentUser?.uid
            ?: return CourseProgress(courseId = courseId)

        val document = firestore
            .collection("users")
            .document(uid)
            .collection("enrollments")
            .document(courseId)
            .get()
            .await()

        if (!document.exists()) {
            return CourseProgress(courseId = courseId)
        }

        return CourseProgress(
            courseId = courseId,
            enrolled = document.getBoolean("enrolled") ?: false,
            completedLessons =
                (document.get("completedLessons") as? List<*>)
                    ?.mapNotNull {
                        (it as? Long)?.toInt()
                    }
                    ?: emptyList(),
            progress = (document.getLong("progress") ?: 0L).toInt(),
            totalLessons =
                (document.getLong("totalLessons") ?: 0L).toInt()
        )
    }

    suspend fun enrollCourse(
        courseId: String,
        totalLessons: Int
    ) {

        val uid = auth.currentUser?.uid ?: return

        firestore
            .collection("users")
            .document(uid)
            .collection("enrollments")
            .document(courseId)
            .set(
                mapOf(
                    "courseId" to courseId,
                    "enrolled" to true,
                    "completedLessons" to emptyList<Int>(),
                    "progress" to 0,
                    "totalLessons" to totalLessons,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    suspend fun markLessonCompleted(
        courseId: String,
        lessonId: Int,
        totalLessons: Int
    ) {

        val uid = auth.currentUser?.uid ?: return

        val reference =
            firestore
                .collection("users")
                .document(uid)
                .collection("enrollments")
                .document(courseId)

        val snapshot = reference.get().await()

        val existingLessons =
            (snapshot.get("completedLessons") as? List<*>)
                ?.mapNotNull {
                    (it as? Long)?.toInt()
                }
                ?.toMutableSet()
                ?: mutableSetOf()

        existingLessons.add(lessonId)

        val progress =
            if (totalLessons == 0) {
                0
            } else {
                ((existingLessons.size.toFloat() /
                        totalLessons.toFloat()) * 100f)
                    .toInt()
            }

        reference.set(
            mapOf(
                "courseId" to courseId,
                "enrolled" to true,
                "completedLessons" to existingLessons.toList(),
                "progress" to progress,
                "totalLessons" to totalLessons,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun unmarkLessonCompleted(
        courseId: String,
        lessonId: Int,
        totalLessons: Int
    ) {

        val uid = auth.currentUser?.uid ?: return

        val reference =
            firestore
                .collection("users")
                .document(uid)
                .collection("enrollments")
                .document(courseId)

        val snapshot = reference.get().await()

        val existingLessons =
            (snapshot.get("completedLessons") as? List<*>)
                ?.mapNotNull {
                    (it as? Long)?.toInt()
                }
                ?.toMutableSet()
                ?: mutableSetOf()

        existingLessons.remove(lessonId)

        val progress =
            if (totalLessons == 0) {
                0
            } else {
                ((existingLessons.size.toFloat() /
                        totalLessons.toFloat()) * 100f)
                    .toInt()
            }

        reference.set(
            mapOf(
                "completedLessons" to existingLessons.toList(),
                "progress" to progress,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }
}