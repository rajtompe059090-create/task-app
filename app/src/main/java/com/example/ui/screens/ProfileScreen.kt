package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.remote.SupabaseConfig
import com.example.data.repository.ReviewTaskRepository
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: ReviewTaskRepository,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val profile by repository.currentProfile.collectAsStateWithLifecycle(initialValue = null)

    val coroutineScope = rememberCoroutineScope()
    var showCloudSettingsDialog by remember { mutableStateOf(false) }
    var supabaseUrlInput by remember { mutableStateOf(SupabaseConfig.supabaseUrl) }
    var supabaseKeyInput by remember { mutableStateOf(SupabaseConfig.supabaseAnonKey) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("profile_screen")
        ) {
            // User Avatar Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(VibrantBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile?.name?.take(1)?.uppercase() ?: "U",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnVibrantBlueContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.name ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+91 ${profile?.phone ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(status = profile?.status ?: "ACTIVE")
                            if (profile?.isAdmin == true) {
                                StatusBadge(status = "ADMIN")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Profile Details Section
            Text("Personal Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            DetailCard(
                icon = Icons.Default.Phone,
                label = "Mobile Number",
                value = "+91 ${profile?.phone ?: "Not provided"}"
            )
            DetailCard(
                icon = Icons.Default.Chat,
                label = "WhatsApp Number",
                value = "+91 ${profile?.whatsapp?.ifEmpty { profile?.phone } ?: "Not provided"}"
            )
            DetailCard(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Registered UPI ID",
                value = profile?.upiId?.ifEmpty { "Not configured" } ?: "Not configured"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Referral Program Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VibrantAmberContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = OnVibrantAmberContainer, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Your Referral Code", style = MaterialTheme.typography.labelSmall, color = OnVibrantAmberContainer.copy(alpha = 0.8f))
                        Text(
                            text = profile?.referralCode ?: "TASK2026",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnVibrantAmberContainer
                        )
                        Text("Earn ₹10 for every verified friend who signs up.", style = MaterialTheme.typography.bodySmall, color = OnVibrantAmberContainer.copy(alpha = 0.8f))
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Referral Code", profile?.referralCode ?: "TASK2026")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = OnVibrantAmberContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Fraud & Security Architecture Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VibrantGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fraud & Safety Protection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• One verified phone account per member\n• Duplicate campaign completion prevention active\n• Suspicious feedback speed tracking active\n• Google Maps non-manipulation compliance verified",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Admin Area Portal Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAdmin() }
                    .testTag("nav_to_admin_portal_btn"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(VibrantAmberContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = OnVibrantAmberContainer, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Admin Control Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Manage users, campaigns, feedbacks & payouts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Supabase Cloud Integration Settings Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCloudSettingsDialog = true }
                    .testTag("supabase_config_btn"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(VibrantBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = VibrantBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Supabase Cloud Endpoint", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("PostgreSQL & PostgREST connection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            OutlinedButton(
                onClick = {
                    repository.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VibrantError)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showCloudSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showCloudSettingsDialog = false },
            title = { Text("Supabase Cloud Settings") },
            text = {
                Column {
                    Text(
                        "ReviewTask connects to your live Supabase PostgreSQL backend using the configured project URL and API key:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = supabaseUrlInput,
                        onValueChange = { 
                            supabaseUrlInput = it
                            testResultText = null
                        },
                        label = { Text("Supabase Project URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("supabase_url_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = supabaseKeyInput,
                        onValueChange = { 
                            supabaseKeyInput = it
                            testResultText = null
                        },
                        label = { Text("Supabase Anon Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("supabase_key_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test Connection Button & Status
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            testResultText = "Testing connection..."
                            coroutineScope.launch {
                                val res = SupabaseConfig.verifyConnection(supabaseUrlInput, supabaseKeyInput)
                                isTestingConnection = false
                                testResultText = if (res.isSuccess) {
                                    "✓ " + res.getOrNull()
                                } else {
                                    "✗ " + (res.exceptionOrNull()?.message ?: "Connection failed")
                                }
                            }
                        },
                        enabled = !isTestingConnection && supabaseUrlInput.isNotBlank() && supabaseKeyInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("test_supabase_btn")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Connection")
                        }
                    }

                    if (testResultText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isSuccess = testResultText?.startsWith("✓") == true
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSuccess) VibrantGreenContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testResultText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSuccess) OnVibrantGreenContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        SupabaseConfig.supabaseUrl = supabaseUrlInput.trim()
                        SupabaseConfig.supabaseAnonKey = supabaseKeyInput.trim()
                        showCloudSettingsDialog = false
                        Toast.makeText(context, "Supabase connection settings saved", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
