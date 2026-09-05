package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.ReviewTaskRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    repository: ReviewTaskRepository,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var phoneNumber by remember { mutableStateOf("9876543210") }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to ReviewTask", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag("login_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = VibrantBlueContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Sign In with Mobile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your verified 10-digit mobile number to access genuine feedback tasks and your rewards wallet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    if (it.length <= 10) phoneNumber = it
                    errorMessage = null
                },
                label = { Text("Mobile Number") },
                prefix = { Text("+91  ", fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_input"),
                shape = RoundedCornerShape(16.dp),
                enabled = !isOtpSent && !isLoading
            )

            AnimatedVisibility(visible = isOtpSent) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = {
                            if (it.length <= 6) otpCode = it
                            errorMessage = null
                        },
                        label = { Text("OTP Verification Code") },
                        placeholder = { Text("Enter 123456") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input"),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Demo hint: Test verification code is 123456",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(14.dp)
                            .testTag("auth_error_message")
                    )
                }
            }

            if (infoMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = VibrantBlueContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = infoMessage ?: "",
                        color = OnVibrantBlueContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (!isOtpSent) {
                        if (phoneNumber.trim().length < 10) {
                            errorMessage = "Please enter a valid 10-digit mobile number."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            delay(600) // Simulated Supabase Auth SMS dispatch
                            isLoading = false
                            isOtpSent = true
                            infoMessage = "OTP sent to +91 $phoneNumber. (Use 123456 for instant test login)"
                        }
                    } else {
                        if (otpCode.length < 4) {
                            errorMessage = "Please enter the verification code."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            delay(500)
                            val result = repository.loginWithPhone(phoneNumber)
                            isLoading = false
                            result.onSuccess {
                                onLoginSuccess()
                            }.onFailure { err ->
                                if (err is NoSuchElementException) {
                                    errorMessage = "Account not found for this number. Please register your profile."
                                } else {
                                    errorMessage = err.message ?: "Login failed. Please verify credentials."
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_submit_btn"),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (!isOtpSent) "Send OTP" else "Verify & Continue",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("nav_to_register_btn"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("New User? Register Profile", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Demo Admin / Test User button
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        repository.switchUserSession("admin_001")
                        onLoginSuccess()
                    }
                },
                modifier = Modifier.testTag("quick_admin_login_btn")
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Instant Demo: Admin Officer Sign-In", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    repository: ReviewTaskRepository,
    onRegisterSuccess: () -> Unit,
    onNavigateBackToLogin: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var sameAsPhone by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .testTag("register_screen")
        ) {
            Text(
                text = "Registration & KYC Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Set up your payout details to receive genuine feedback task rewards directly to your UPI.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = { Text("Full Name *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_name_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 10) {
                        phone = it
                        if (sameAsPhone) whatsapp = it
                    }
                    errorMessage = null
                },
                label = { Text("Mobile Number (Verified) *") },
                prefix = { Text("+91 ") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_phone_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = whatsapp,
                onValueChange = {
                    if (it.length <= 10) whatsapp = it
                    errorMessage = null
                },
                label = { Text("WhatsApp Number") },
                prefix = { Text("+91 ") },
                leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_whatsapp_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it; errorMessage = null },
                label = { Text("UPI ID (for rewards withdrawal) *") },
                placeholder = { Text("e.g. yourname@oksbi / 9876543210@paytm") },
                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_upi_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = referralCode,
                onValueChange = { referralCode = it.uppercase() },
                label = { Text("Referral Code (Optional)") },
                placeholder = { Text("e.g. ADMIN99") },
                leadingIcon = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reg_referral_input"),
                shape = RoundedCornerShape(16.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMessage = "Please enter your full name."
                        return@Button
                    }
                    if (phone.trim().length < 10) {
                        errorMessage = "Please enter a valid 10-digit mobile number."
                        return@Button
                    }
                    if (upiId.trim().isEmpty() || !upiId.contains("@")) {
                        errorMessage = "Please enter a valid UPI ID (e.g. username@upi)."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = repository.registerUser(
                            name = name,
                            phone = phone,
                            whatsapp = whatsapp.ifEmpty { phone },
                            upiId = upiId,
                            referralCode = referralCode.ifEmpty { null }
                        )
                        isLoading = false
                        result.onSuccess {
                            onRegisterSuccess()
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Failed to register profile."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("reg_submit_btn"),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text("Complete Registration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
