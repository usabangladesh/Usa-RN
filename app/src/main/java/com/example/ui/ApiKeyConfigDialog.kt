package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gemini.ApiKeyManager
import com.example.gemini.GeminiLiveClient
import com.example.ui.theme.AmberAlert
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ApiKeyConfigDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputKey by remember {
        mutableStateOf(ApiKeyManager.getCustomApiKey(context) ?: ApiKeyManager.getActiveApiKey(context))
    }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }

    val isConfigured = ApiKeyManager.isConfigured(context)
    val maskedKey = ApiKeyManager.getMaskedKey(context)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("api_key_dialog"),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyanNeon.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini API Key",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Active Key Status Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isConfigured) EmeraldGlow else AmberAlert,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isConfigured) "সক্রিয় কী (Active Key)" else "কী প্রয়োজন (API Key Missing)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConfigured) EmeraldGlow else AmberAlert
                        )
                        Text(
                            text = maskedKey,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "আপনার Google Gemini API Key এখানে পেস্ট বা পরিবর্তন করুন। এটি নিরাপদে আপনার ডিভাইসে সংরক্ষিত থাকবে।",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input Field
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it
                        testResultMsg = null
                        testSuccess = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    label = { Text("API Key প্রবেশ করান") },
                    placeholder = { Text("AIzaSy... অথবা AQ...") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        cursorColor = CyanNeon,
                        focusedLabelColor = CyanNeon,
                        unfocusedLabelColor = TextMuted,
                        focusedContainerColor = DarkCanvas,
                        unfocusedContainerColor = DarkCanvas
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide Key" else "Show Key",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val pasteText = clip.getItemAt(0).text?.toString()?.trim()
                                        if (!pasteText.isNullOrBlank()) {
                                            inputKey = pasteText
                                            testResultMsg = null
                                            testSuccess = null
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste from Clipboard",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )

                // Test connection feedback
                if (testResultMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (testSuccess == true) EmeraldGlow.copy(alpha = 0.12f) else AmberAlert.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (testSuccess == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (testSuccess == true) EmeraldGlow else AmberAlert,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = testResultMsg ?: "",
                            fontSize = 12.sp,
                            color = if (testSuccess == true) EmeraldGlow else AmberAlert
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions: Test, Reset, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Test connection button
                    OutlinedButton(
                        onClick = {
                            if (inputKey.isBlank()) {
                                testResultMsg = "অনুগ্রহ করে একটি API Key লিখুন।"
                                testSuccess = false
                                return@OutlinedButton
                            }
                            isTesting = true
                            testResultMsg = null
                            testSuccess = null
                            scope.launch {
                                val originalOverride = GeminiLiveClient.runtimeApiKeyOverride
                                GeminiLiveClient.runtimeApiKeyOverride = inputKey.trim()
                                val client = GeminiLiveClient()
                                val resp = client.sendVoiceQuery("হাই, সংযোগ যাচাই করো")
                                if (resp.error == null) {
                                    testSuccess = true
                                    testResultMsg = "সংযোগ সফল! Gemini প্রস্তুত।"
                                } else {
                                    testSuccess = false
                                    testResultMsg = resp.error
                                }
                                GeminiLiveClient.runtimeApiKeyOverride = originalOverride
                                isTesting = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_api_key_button"),
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyanNeon,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("যাচাই", fontSize = 12.sp)
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            ApiKeyManager.saveCustomApiKey(context, inputKey)
                            onSaved()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_api_key_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanNeon,
                            contentColor = DarkCanvas
                        )
                    ) {
                        Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Revert / Reset to Default Key from .env
                OutlinedButton(
                    onClick = {
                        ApiKeyManager.clearCustomApiKey(context)
                        inputKey = ApiKeyManager.getActiveApiKey(context)
                        testResultMsg = "ডিফল্ট কনফিগারেশন রিস্টোর করা হয়েছে।"
                        testSuccess = true
                        onSaved()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_api_key_button"),
                    border = BorderStroke(1.dp, DarkSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("ডিফল্ট (.env) কী-তে রিসেট করুন", fontSize = 12.sp)
                }
            }
        }
    }
}
