
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private val ProfileBlue = Color(0xFF2563EB)
private val ProfileDark = Color(0xFF172554)
private val ProfileText = Color(0xFF1E293B)
private val ProfileGray = Color(0xFF64748B)
private val ProfileBackground = Color(0xFFF6F8FC)
private val ProfileRed = Color(0xFFDC2626)
private val ProfileDarkRed = Color(0xFFB91C1C)

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {}
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    var userName by remember {
        mutableStateOf("")
    }

    var userEmail by remember {
        mutableStateOf(
            auth.currentUser?.email ?: ""
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var logoutLoading by remember {
        mutableStateOf(false)
    }

    var deleteLoading by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(auth.currentUser?.uid) {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            isLoading = false
            return@LaunchedEffect
        }

        userEmail = currentUser.email ?: ""

        try {

            val document = firestore
                .collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            userName =
                document.getString("name")
                    ?: document.getString("displayName")
                            ?: currentUser.displayName
                            ?: ""

        } catch (exception: Exception) {

            errorMessage =
                "Profile error: ${
                    exception.message ?: "Unable to load profile"
                }"

        } finally {

            isLoading = false
        }
    }

    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                if (!deleteLoading) {
                    showDeleteDialog = false
                }
            },
            title = {
                Text(
                    text = "Delete Account",
                    fontWeight = FontWeight.Bold,
                    color = ProfileDark
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete your account? Your profile and account will be removed. This action cannot be undone.",
                    color = ProfileText
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        if (deleteLoading) {
                            return@TextButton
                        }

                        val currentUser = auth.currentUser

                        if (currentUser == null) {

                            showDeleteDialog = false
                            errorMessage = "No signed-in user found."

                            return@TextButton
                        }

                        deleteLoading = true
                        errorMessage = null

                        val uid = currentUser.uid

                        firestore
                            .collection("users")
                            .document(uid)
                            .delete()
                            .addOnCompleteListener { firestoreTask ->

                                if (!firestoreTask.isSuccessful) {

                                    deleteLoading = false

                                    errorMessage =
                                        "Failed to delete account data: ${
                                            firestoreTask.exception?.message
                                                ?: "Unknown error"
                                        }"

                                    return@addOnCompleteListener
                                }

                                currentUser
                                    .delete()
                                    .addOnCompleteListener { deleteTask ->

                                        deleteLoading = false

                                        if (deleteTask.isSuccessful) {

                                            showDeleteDialog = false

                                            auth.signOut()

                                            onLogout()

                                        } else {

                                            errorMessage =
                                                if (
                                                    deleteTask.exception?.message
                                                        ?.contains(
                                                            "recent",
                                                            ignoreCase = true
                                                        ) == true
                                                ) {

                                                    "For security, please log in again before deleting your account."

                                                } else {

                                                    "Account deletion failed: ${
                                                        deleteTask.exception?.message
                                                            ?: "Unknown error"
                                                    }"
                                                }
                                        }
                                    }
                            }
                    }
                ) {

                    if (deleteLoading) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ProfileRed,
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            text = "Delete",
                            color = ProfileRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        if (!deleteLoading) {
                            showDeleteDialog = false
                        }
                    },
                    enabled = !deleteLoading
                ) {

                    Text(
                        text = "Cancel",
                        color = ProfileGray
                    )
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileBackground)
            .padding(20.dp)
    ) {

        Text(
            text = "Profile",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = ProfileDark
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Manage your account",
            fontSize = 13.sp,
            color = ProfileGray
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(ProfileBlue),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(42.dp),
                        tint = Color.White
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                if (isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ProfileBlue
                    )

                } else {

                    Text(
                        text = if (userName.isBlank()) {
                            "Student"
                        } else {
                            userName
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfileDark
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = userEmail.ifBlank {
                            "No email available"
                        },
                        fontSize = 13.sp,
                        color = ProfileGray
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {

                Text(
                    text = "Account Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ProfileText
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                ProfileInfoRow(
                    icon = Icons.Default.Person,
                    label = "Name",
                    value = if (userName.isBlank()) {
                        "Not set"
                    } else {
                        userName
                    }
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = if (userEmail.isBlank()) {
                        "Not available"
                    } else {
                        userEmail
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        if (errorMessage != null) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF1F2)
                )
            ) {

                Text(
                    text = errorMessage!!,
                    modifier = Modifier.padding(14.dp),
                    color = ProfileDarkRed,
                    fontSize = 13.sp
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        Button(
            onClick = {

                if (logoutLoading || deleteLoading) {
                    return@Button
                }

                errorMessage = null
                logoutLoading = true

                try {

                    auth.signOut()

                    val userAfterLogout =
                        auth.currentUser

                    if (userAfterLogout == null) {

                        logoutLoading = false

                        onLogout()

                    } else {

                        logoutLoading = false

                        errorMessage =
                            "Logout failed: Firebase user is still signed in."
                    }

                } catch (exception: Exception) {

                    logoutLoading = false

                    errorMessage =
                        "Logout error: ${
                            exception.message
                                ?: exception.javaClass.simpleName
                        }"
                }
            },
            enabled = !logoutLoading && !deleteLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ProfileRed
            )
        ) {

            if (logoutLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )

            } else {

                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout"
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Logout",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {

                if (!logoutLoading && !deleteLoading) {
                    showDeleteDialog = true
                }
            },
            enabled = !logoutLoading && !deleteLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = ProfileRed
            )
        ) {

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Account"
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "Delete Account",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ProfileGray
            )

            Spacer(
                modifier = Modifier.width(5.dp)
            )

            Text(
                text = "Learnify Account",
                fontSize = 12.sp,
                color = ProfileGray
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
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
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = ProfileBlue
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = label,
                fontSize = 11.sp,
                color = ProfileGray
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ProfileText
            )
        }
    }
}
