package com.example.myapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

private val LearnifyBlue = Color(0xFF2563EB)
private val LearnifyPurple = Color(0xFF7C3AED)
private val LearnifyDark = Color(0xFF172554)
private val LearnifyLightBlue = Color(0xFFEFF6FF)
private val LearnifyText = Color(0xFF1E293B)
private val LearnifyGray = Color(0xFF64748B)

private const val TAG = "LearnifyAuth"

class login : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "login"
            ) {

                composable("login") {

                    LoginScreen(
                        onregisterclick = {
                            navController.navigate("register")
                        },
                        onLoginSuccess = {
                            Toast.makeText(
                                this@login,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // TODO: replace "home" with your actual post-login destination
                            // and add a matching composable("home") { ... } block above.
                            // Without this, the screen just stays on "login" after success.
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("register") {

                    RegisterScreen(
                        onLoginClick = {
                            navController.navigate("login")
                        },
                        onregisterClick = {
                            navController.navigate("login")
                        }
                    )
                }

                // Placeholder so the app doesn't crash before you add your real home screen.
                composable("home") {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Home screen goes here")
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onregisterclick: () -> Unit,
    onLoginSuccess: () -> Unit
) {

    val context = LocalContext.current
    val activity = context as Activity

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var isGoogleLoading by remember {
        mutableStateOf(false)
    }

    fun verifyUserInFirestore(
        uid: String,
        name: String?,
        userEmail: String?,
        photoUrl: String?,
        authProvider: String,
        onDone: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userData = hashMapOf(
            "name" to (name ?: ""),
            "email" to (userEmail ?: ""),
            "photoUrl" to (photoUrl ?: ""),
            "authProvider" to authProvider,
            "lastLoginAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                onDone()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    val googleSignInClient = remember(activity) {

        val googleSignInOptions =
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(
                    activity.getString(
                        R.string.default_web_client_id
                    )
                )
                .requestEmail()
                .build()

        GoogleSignIn.getClient(
            activity,
            googleSignInOptions
        )
    }

    val googleLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->

            // Log the raw resultCode no matter what — this alone tells you a lot.
            Log.d(TAG, "Google sign-in activity resultCode=${result.resultCode} (RESULT_OK=${Activity.RESULT_OK})")

            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {

                // .getResult(ApiException::class.java) throws with a real status code
                // instead of silently giving you a "cancelled" toast.
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken == null) {

                    isGoogleLoading = false

                    Toast.makeText(
                        context,
                        "Google authentication failed: no ID token returned",
                        Toast.LENGTH_LONG
                    ).show()

                } else {

                    val credential =
                        GoogleAuthProvider.getCredential(
                            idToken,
                            null
                        )

                    auth.signInWithCredential(
                        credential
                    ).addOnCompleteListener { firebaseTask ->

                        if (firebaseTask.isSuccessful) {

                            val user = auth.currentUser

                            if (user != null) {

                                verifyUserInFirestore(
                                    uid = user.uid,
                                    name = user.displayName ?: account.displayName,
                                    userEmail = user.email ?: account.email,
                                    photoUrl = user.photoUrl?.toString(),
                                    authProvider = "google",
                                    onDone = {

                                        isGoogleLoading = false

                                        Toast.makeText(
                                            context,
                                            "Google sign-in successful!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        onLoginSuccess()
                                    },
                                    onError = { exception ->

                                        isGoogleLoading = false

                                        Log.e(TAG, "Firestore sync failed", exception)

                                        Toast.makeText(
                                            context,
                                            "Failed to sync profile: ${exception.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )

                            } else {

                                isGoogleLoading = false

                                Toast.makeText(
                                    context,
                                    "User information unavailable",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        } else {

                            isGoogleLoading = false

                            Log.e(TAG, "Firebase signInWithCredential failed", firebaseTask.exception)

                            Toast.makeText(
                                context,
                                firebaseTask.exception?.message
                                    ?: "Google sign-in failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            } catch (e: ApiException) {

                isGoogleLoading = false

                // statusCode 12501 = user actually cancelled the picker.
                // statusCode 10 (DEVELOPER_ERROR) = SHA-1 fingerprint / OAuth client
                // misconfiguration in Firebase console — by far the most common cause
                // of a Google sign-in that fails immediately without a real cancel.
                Log.e(TAG, "Google sign-in ApiException statusCode=${e.statusCode}", e)

                val message = when (e.statusCode) {
                    12501 -> "Google sign-in cancelled"
                    7 -> "Network error — check your connection"
                    10 -> "Google sign-in misconfigured (DEVELOPER_ERROR). " +
                            "Check that your SHA-1/SHA-256 fingerprints are added in the " +
                            "Firebase console and that default_web_client_id is the Web " +
                            "client ID, then re-download google-services.json."
                    else -> "Google sign-in failed (code ${e.statusCode}): ${e.message}"
                }

                Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {

                isGoogleLoading = false

                Log.e(TAG, "Google sign-in unexpected error", e)

                Toast.makeText(
                    context,
                    "Google sign-in failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LearnifyLightBlue,
                        Color.White,
                        Color(0xFFF5F3FF)
                    )
                )
            )
    ) {

        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(
                    x = (-70).dp,
                    y = (-50).dp
                )
                .clip(CircleShape)
                .background(
                    LearnifyBlue.copy(alpha = 0.12f)
                )
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(
                    x = 80.dp,
                    y = 70.dp
                )
                .clip(CircleShape)
                .background(
                    LearnifyPurple.copy(alpha = 0.10f)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .imePadding()
                .padding(
                    horizontal = 22.dp,
                    vertical = 28.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                LearnifyBlue,
                                LearnifyPurple
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "L",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Learnify",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LearnifyDark
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Learn. Grow. Succeed.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LearnifyGray
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(
                        horizontal = 22.dp,
                        vertical = 28.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Welcome Back",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = LearnifyText
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Login to continue your learning journey",
                    fontSize = 13.sp,
                    color = LearnifyGray
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = {
                        Text("Email Address")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp)
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = {
                        Text("Password")
                    },
                    trailingIcon = {

                        IconButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            }
                        ) {

                            Text(
                                text = if (passwordVisible) {
                                    "Hide"
                                } else {
                                    "Show"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LearnifyBlue
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp)
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                Button(
                    onClick = {

                        when {

                            email.isBlank() -> {

                                Toast.makeText(
                                    context,
                                    "Please enter your email",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            password.isBlank() -> {

                                Toast.makeText(
                                    context,
                                    "Please enter your password",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {

                                isLoading = true

                                auth.signInWithEmailAndPassword(
                                    email.trim(),
                                    password
                                ).addOnCompleteListener { task ->

                                    if (task.isSuccessful) {

                                        val user = auth.currentUser

                                        if (user != null) {

                                            verifyUserInFirestore(
                                                uid = user.uid,
                                                name = user.displayName,
                                                userEmail = user.email ?: email.trim(),
                                                photoUrl = user.photoUrl?.toString(),
                                                authProvider = "password",
                                                onDone = {
                                                    isLoading = false
                                                    onLoginSuccess()
                                                },
                                                onError = { exception ->
                                                    isLoading = false
                                                    Log.e(TAG, "Firestore sync failed", exception)
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to sync profile: ${exception.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            )

                                        } else {

                                            isLoading = false

                                            Toast.makeText(
                                                context,
                                                "User information unavailable",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    } else {

                                        isLoading = false

                                        Log.e(TAG, "Email/password sign-in failed", task.exception)

                                        Toast.makeText(
                                            context,
                                            task.exception?.message
                                                ?: "Login failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading && !isGoogleLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LearnifyBlue
                    )
                ) {

                    if (isLoading) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            text = "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Color(0xFFE2E8F0)
                            )
                    )

                    Text(
                        text = "  OR  ",
                        fontSize = 12.sp,
                        color = LearnifyGray
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Color(0xFFE2E8F0)
                            )
                    )
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Button(
                    onClick = {

                        isGoogleLoading = true

                        googleLauncher.launch(
                            googleSignInClient.signInIntent
                        )
                    },
                    enabled = !isLoading && !isGoogleLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LearnifyText
                    )
                ) {

                    if (isGoogleLoading) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = LearnifyBlue,
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            text = "G",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text(
                            text = "Continue with Google",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(15.dp)
                )

                TextButton(
                    onClick = {
                        onregisterclick()
                    }
                ) {

                    Text(
                        text = "Don't have an account? ",
                        color = LearnifyGray
                    )

                    Text(
                        text = "Register",
                        fontWeight = FontWeight.Bold,
                        color = LearnifyBlue
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            Text(
                text = "Welcome back to your learning journey",
                fontSize = 11.sp,
                color = LearnifyGray.copy(alpha = 0.7f)
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}