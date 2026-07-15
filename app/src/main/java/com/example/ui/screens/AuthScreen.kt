package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.BorderStroke
import com.example.ui.utils.BiometricHelper
import com.example.R
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.components.*
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var currentMode by remember { mutableStateOf(AuthMode.LOGIN) }
    
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val isBiometricAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Form States
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Status text (e.g. for success reset email)
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Reset error when mode changes
    LaunchedEffect(currentMode) {
        viewModel.clearError()
        statusMessage = null
        password = ""
        confirmPassword = ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Logo or Decorative Hero Header
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                contentDescription = "Finance Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(com.example.ui.theme.AppShapes.roundedCardMedium)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Finance Tracker",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Manage your wealth beautifully",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Auth Error Banner
            if (authState is AuthState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimens.paddingNormal)
                        .testTag("auth_error_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = AppShapes.roundedCardMedium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error icon",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
                        Text(
                            text = (authState as AuthState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Status Message Banner
            statusMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimens.paddingNormal)
                        .testTag("auth_status_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = AppShapes.roundedCardMedium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.paddingNormal),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(AppDimens.paddingNormal))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Animated Screen switching (Login / Register / Forgot Password)
            AnimatedContent(
                targetState = currentMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "AuthFormTransition"
            ) { mode ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_form_card"),
                    shape = AppShapes.roundedCardLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimens.paddingLarge),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.paddingNormal)
                    ) {
                        when (mode) {
                            AuthMode.LOGIN -> {
                                Text(
                                    text = "Welcome Back",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        FinanceIconButton(
                                            icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            onClick = { passwordVisible = !passwordVisible },
                                            contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
                                        )
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Forgot Password?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { currentMode = AuthMode.FORGOT_PASSWORD }
                                            .testTag("forgot_password_button")
                                    )
                                }

                                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FinanceButton(
                                        text = "Log In",
                                        onClick = { viewModel.signIn(email, password) },
                                        modifier = Modifier.weight(1f),
                                        enabled = authState != AuthState.Loading,
                                        loading = authState == AuthState.Loading,
                                        height = 52.dp,
                                        testTag = "login_submit_button"
                                    )

                                    if (isBiometricAvailable) {
                                        OutlinedIconButton(
                                            onClick = {
                                                if (activity != null) {
                                                    val prefs = com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(activity, "auth_prefs")
                                                    val ivBase64 = prefs.getString("biometric_challenge_iv", null)
                                                    val iv = if (ivBase64 != null) android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP) else null
                                                    val cipher = BiometricHelper.getInitializedCipher(javax.crypto.Cipher.DECRYPT_MODE, iv)
                                                    val cryptoObject = if (cipher != null) androidx.biometric.BiometricPrompt.CryptoObject(cipher) else null

                                                    BiometricHelper.showBiometricPrompt(
                                                        activity = activity,
                                                        cryptoObject = cryptoObject,
                                                        onSuccess = { result ->
                                                            val unlockedCipher = result.cryptoObject?.cipher
                                                            if (unlockedCipher != null && BiometricHelper.decryptAndVerifyChallenge(activity, unlockedCipher)) {
                                                                viewModel.signInWithBiometrics()
                                                            } else {
                                                                viewModel.setError("Biometric verification failed. Please use password login.")
                                                            }
                                                        },
                                                        onError = { error ->
                                                            if (error != "Cancelled") {
                                                                viewModel.setError(error)
                                                            }
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(52.dp).testTag("biometric_login_button"),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline
                                            ),
                                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Login",
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "Sign Up",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { currentMode = AuthMode.REGISTER }
                                            .testTag("go_to_register_button")
                                    )
                                }
                            }

                            AuthMode.REGISTER -> {
                                Text(
                                    text = "Create Account",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().testTag("name_input")
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password (min 6 characters)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        FinanceIconButton(
                                            icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            onClick = { passwordVisible = !passwordVisible },
                                            contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
                                        )
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                                )

                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    trailingIcon = {
                                        FinanceIconButton(
                                            icon = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                            contentDescription = if (confirmPasswordVisible) "Hide Password" else "Show Password"
                                        )
                                    },
                                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("confirm_password_input")
                                )

                                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

                                FinanceButton(
                                    text = "Register",
                                    onClick = {
                                        if (password != confirmPassword) {
                                            statusMessage = null
                                            viewModel.setError("Passwords do not match")
                                        } else {
                                            viewModel.signUp(email, password, name)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = authState != AuthState.Loading,
                                    loading = authState == AuthState.Loading,
                                    height = 52.dp,
                                    testTag = "register_submit_button"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "Log In",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { currentMode = AuthMode.LOGIN }
                                            .testTag("go_to_login_button")
                                    )
                                }
                            }

                            AuthMode.FORGOT_PASSWORD -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { currentMode = AuthMode.LOGIN }
                                        .padding(vertical = AppDimens.paddingSmall)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Back to Login",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

                                Text(
                                    text = "Reset Password",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Enter your email address below and we'll send you a password reset link.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                                )

                                Spacer(modifier = Modifier.height(AppDimens.paddingSmall))

                                FinanceButton(
                                    text = "Send Reset Link",
                                    onClick = {
                                        viewModel.sendPasswordReset(email) {
                                            statusMessage = "A password reset link has been sent to $email."
                                            email = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = authState != AuthState.Loading,
                                    loading = authState == AuthState.Loading,
                                    height = 52.dp,
                                    testTag = "reset_submit_button"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))

            // Or Continue With Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppDimens.paddingNormal)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(AppDimens.paddingLarge))

            // Google Sign In (Centralized FinanceButton)
            FinanceButton(
                text = "Continue with Google",
                onClick = {
                    coroutineScope.launch {
                        try {
                            val credentialManager = androidx.credentials.CredentialManager.create(context)
                            
                            val serverClientId = context.getString(R.string.default_web_client_id)
                            val googleIdOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(
                                serverClientId = serverClientId
                            ).build()
                            
                            val getCredRequest = androidx.credentials.GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                                
                            val result = credentialManager.getCredential(
                                context = context,
                                request = getCredRequest
                            )
                            
                            val credential = result.credential
                            if (credential is com.google.android.libraries.identity.googleid.GoogleIdTokenCredential) {
                                val idToken = credential.idToken
                                viewModel.continueWithGoogle(idToken)
                            } else {
                                viewModel.setError("Unexpected credential type returned")
                            }
                        } catch (e: Exception) {
                            Log.w("AuthScreen", "CredentialManager failed", e)
                            if (com.example.BuildConfig.DEBUG) {
                                Log.d("AuthScreen", "Falling back to simulated Google sign-in (debug only)")
                                viewModel.continueWithGoogle("simulated_id_token_123")
                            } else {
                                viewModel.setError("Google Sign-In failed. Please try again.")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState != AuthState.Loading,
                icon = painterResource(id = R.drawable.ic_google_logo),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                height = 50.dp,
                testTag = "google_login_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Continue as Guest (Centralized FinanceOutlinedButton)
            FinanceOutlinedButton(
                text = "Continue as Guest",
                onClick = { viewModel.loginAsGuest() },
                modifier = Modifier.fillMaxWidth(),
                enabled = authState != AuthState.Loading,
                icon = Icons.Default.DirectionsWalk,
                contentColor = MaterialTheme.colorScheme.primary,
                borderColor = MaterialTheme.colorScheme.outline,
                height = 50.dp,
                testTag = "guest_login_button"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
