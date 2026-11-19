package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.LocalSyntelColors
import com.example.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    isOnline: Boolean,
    onLogin: (username: String, password: String, cafeCode: String) -> Unit,
    onClearError: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var cafeCode by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero Header Banner matching Syntel Web Purple-Pink Identity
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_syntel_login_hero_1795076414118),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.28f),
                                    cs.background.copy(alpha = 0.15f),
                                    cs.background,
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 28.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_syntel_icon_clean),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Syntel",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.brand_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }

            // Login Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    border = BorderStroke(1.dp, sem.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "ورود به حساب کاربری",
                            style = MaterialTheme.typography.titleLarge,
                            color = cs.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ویژه درخواست‌دهندگان، مدیر انبار و مسئول خرید",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                if (state.errorMessage != null) onClearError()
                            },
                            label = { Text("نام کاربری") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cs.primary,
                                unfocusedBorderColor = sem.cardBorder,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (state.errorMessage != null) onClearError()
                            },
                            label = { Text("رمز عبور") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = stringResource(R.string.cd_toggle_password),
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onLogin(username, password, cafeCode) }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cs.primary,
                                unfocusedBorderColor = sem.cardBorder,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = cafeCode,
                            onValueChange = { cafeCode = it },
                            label = { Text("کد کافه / شعبه (اختیاری)") },
                            placeholder = { Text("برای کاربران دارای شعبه ثابت خالی بگذارید") },
                            leadingIcon = { Icon(Icons.Outlined.Storefront, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = cs.primary,
                                unfocusedBorderColor = sem.cardBorder,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_cafe_input"),
                        )

                        if (!state.errorMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Surface(
                                color = sem.dangerContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_error_box"),
                            ) {
                                Text(
                                    text = state.errorMessage,
                                    color = sem.danger,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }

                        if (!isOnline) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = sem.warningContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "برای اولین ورود نیاز به اتصال اینترنت و دسترسی به سرور سایت است.",
                                    color = cs.onSurface,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = { onLogin(username, password, cafeCode) },
                            enabled = !state.isLoading && username.isNotBlank() && password.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button"),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = cs.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("در حال اتصال به سرور...")
                            } else {
                                Text(
                                    text = "ورود به سیستم",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Read-only fixed server info pill (from BuildConfig.SYNTEL_BASE_URL)
                Surface(
                    color = cs.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, sem.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) sem.success else sem.danger)
                        )
                        Icon(
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سرور متصل (ثابت در تنظیمات پروژه)",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                            )
                            Text(
                                text = BuildConfig.SYNTEL_BASE_URL.removeSuffix("/"),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
