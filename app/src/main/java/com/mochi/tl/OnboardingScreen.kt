package com.mochi.tl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochi.tl.designsystem.MochiAppTheme
import com.mochi.tl.designsystem.components.*

@Composable
internal fun OnboardingScreen(
    providers: List<ProviderConfig>,
    activeProvider: ProviderConfig,
    apiKeyText: String,
    onSelectProvider: (ProviderConfig) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    isTesting: Boolean,
    testStatus: String?,
    onCompleteOnboarding: () -> Unit,
    onSkipOnboarding: () -> Unit
) {
    var isKeyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero Section with Mascot Logo
        Image(
            painter = painterResource(R.drawable.mochitl_mascot),
            contentDescription = "MochiTL Mascot Logo",
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Selamat Datang di MochiTL",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Workspace Penerjemah AI Novel, Manga & Dokumen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MochiChip(text = "Keystore Safe", icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp)) })
            MochiChip(text = "Multi-LLM Provider")
        }

        // Step Progress Bar
        MochiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Langkah 1 dari 2: Konfigurasi Engine AI",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("50%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // Provider Selector
        MochiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pilih AI Provider Utama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                providers.forEach { prov ->
                    val isSelected = prov.id == activeProvider.id
                    MochiCard(
                        onClick = { onSelectProvider(prov) },
                        modifier = Modifier.fillMaxWidth(),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prov.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (!prov.requiresApiKey) {
                                MochiChip(text = "Lokal")
                            }
                        }
                    }
                }

                if (activeProvider.requiresApiKey) {
                    MochiTextField(
                        value = apiKeyText,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key (${activeProvider.name})") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle key")
                            }
                        }
                    )
                }

                MochiOutlinedButton(
                    onClick = onTestConnection,
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Menguji...")
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tes Koneksi")
                    }
                }

                if (testStatus != null) {
                    val isConnected = testStatus == "Connected"
                    MochiChip(
                        text = if (isConnected) "Status: Connected ✓" else "Status: Disconnected ✗",
                        containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MochiGhostButton(
                onClick = onSkipOnboarding,
                modifier = Modifier.weight(1f)
            ) {
                Text("Lewati")
            }

            // Single Solid Emerald Primary CTA Button
            MochiButton(
                onClick = onCompleteOnboarding,
                modifier = Modifier.weight(2f)
            ) {
                Text("Lanjutkan ke Workspace", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Preview(name = "Onboarding Screen - Dark Theme", showBackground = true)
@Composable
fun PreviewOnboardingScreenDark() {
    MochiAppTheme(darkTheme = true, dynamicColor = false) {
        OnboardingScreen(
            providers = BuiltIns.providers,
            activeProvider = BuiltIns.providers.first(),
            apiKeyText = "••••••••",
            onSelectProvider = {},
            onApiKeyChange = {},
            onTestConnection = {},
            isTesting = false,
            testStatus = "Connected",
            onCompleteOnboarding = {},
            onSkipOnboarding = {}
        )
    }
}
