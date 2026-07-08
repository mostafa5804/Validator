@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.History
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import com.example.data.history.AppDatabase
import com.example.data.history.HistoryItem
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CityRepository

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun MainScreen(cityRepository: CityRepository, historyDao: com.example.data.history.HistoryDao, isDarkMode: Boolean, onThemeToggle: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel { 
        MainViewModel(cityRepository, historyDao) 
    }
    val state by viewModel.state.collectAsState()
    val history by viewModel.history.collectAsState()
    val validCount by viewModel.validCount.collectAsState()
    val invalidCount by viewModel.invalidCount.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isHapticEnabled by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color.White
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1C1E)
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray

    val scanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        if (result.contents != null) {
            viewModel.onInputChanged(result.contents.replace(Regex("[^0-9]"), ""))
        }
    }

    LaunchedEffect(state.isValid) {
        if (state.isValid != null && isHapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("تنظیمات و درباره", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column {
                    Text(
                        text = "این برنامه جهت اعتبارسنجی آفلاین شماره کارت، شبا و کد ملی طراحی شده است. هیچ‌گونه اطلاعاتی در بستر اینترنت ارسال نمی‌شود.",
                        textAlign = TextAlign.Justify,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("بازخورد لمسی (ویبره)", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = { isHapticEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1E88E5), checkedTrackColor = Color(0xFFBBDEFB))
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("بستن", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textColor,
            textContentColor = textColor
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            // Background Header Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50))
                        ),
                        shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                // Custom Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اعتبارسنج",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { onThemeToggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "تغییر تم", tint = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { showSettingsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Input Section (Glassmorphism look)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                        .background(surfaceColor, RoundedCornerShape(24.dp))
                        .border(1.dp, if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        TypeSelector(selectedType = state.type, isDarkMode = isDarkMode) {
                            viewModel.onTypeChanged(it)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val labelText = when (state.type) {
                            ValidationType.CARD -> "شماره ۱۶ رقمی کارت"
                            ValidationType.SHABA -> "شماره شبا (بدون IR)"
                            ValidationType.NATIONAL_ID -> "کد ملی (۱۰ رقمی)"
                        }

                        Text(
                            text = labelText,
                            color = Color(0xFF1E88E5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                        )

                        val visualTransformation = when (state.type) {
                            ValidationType.CARD -> CardNumberTransformation()
                            ValidationType.SHABA -> ShabaTransformation()
                            ValidationType.NATIONAL_ID -> NationalIdTransformation()
                        }

                        OutlinedTextField(
                            value = state.input,
                            onValueChange = { 
                                if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onInputChanged(it) 
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = visualTransformation,
                            modifier = Modifier.fillMaxWidth().onKeyEvent { event ->
                                if (event.isCtrlPressed && event.key == Key.Backspace) {
                                    viewModel.onInputChanged("")
                                    if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    true
                                } else {
                                    false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = textColor
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E88E5),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = surfaceColor,
                                unfocusedContainerColor = bgColor,
                            ),
                            trailingIcon = {
                                if (state.input.isEmpty()) {
                                    Row {
                                        IconButton(onClick = {
                                            scanLauncher.launch(com.journeyapps.barcodescanner.ScanOptions().apply {
                                                setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                                                setPrompt("اسکن بارکد")
                                                setBeepEnabled(false)
                                            })
                                        }) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = "اسکن", tint = Color(0xFF1E88E5))
                                        }
                                        IconButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount!! > 0) {
                                                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                                if (text != null) {
                                                    viewModel.onInputChanged(text)
                                                }
                                            } else {
                                                Toast.makeText(context, "حافظه خالی است", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "جایگذاری", tint = Color(0xFF1E88E5))
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.onInputChanged("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                        }
                                        if (state.isValid == true) {
                                            Box(modifier = Modifier.padding(end = 12.dp).size(24.dp).background(Color(0xFF4CAF50), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Check, contentDescription = "Valid", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        } else if (state.isValid == false) {
                                            Box(modifier = Modifier.padding(end = 12.dp).size(24.dp).background(Color(0xFFF44336), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Close, contentDescription = "Invalid", tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                            // Result Card
                AnimatedVisibility(visible = state.isLoading || state.isValid != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                            .background(surfaceColor, RoundedCornerShape(24.dp))
                            .border(1.dp, if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = state.isLoading,
                            transitionSpec = { fadeIn() with fadeOut() }
                        ) { loading ->
                            if (loading) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF1E88E5), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("در حال اعتبارسنجی...", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.isValid == true) {
                            if (state.type == ValidationType.NATIONAL_ID) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color(0xFFE8F5E9), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "کد ملی معتبر است",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                if (state.city != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "صادره از: ${state.city}",
                                        fontSize = 16.sp,
                                        color = textColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (state.bankInfo != null) {
                                if (state.bankInfo!!.logoResId != null) {
                                    Image(
                                        painter = painterResource(id = state.bankInfo!!.logoResId!!),
                                        contentDescription = "Bank Logo",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(surfaceColor)
                                            .padding(4.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(state.bankInfo!!.color, RoundedCornerShape(20.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.bankInfo!!.bankName.take(3),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "اطلاعات معتبر است",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "بانک ${state.bankInfo!!.bankName}",
                                    fontSize = 16.sp,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFFFEBEE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "خطا در اعتبارسنجی",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                fontSize = 15.sp,
                                color = textColor,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .background(bgColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth()
                            )
                        }

                        if (state.isValid == true) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val textToCopy = buildString {
                                            append("نوع: ${
                                                when (state.type) {
                                                    ValidationType.CARD -> "شماره کارت"
                                                    ValidationType.SHABA -> "شماره شبا"
                                                    ValidationType.NATIONAL_ID -> "کد ملی"
                                                }
                                            }\n")
                                            append("مقدار: ${state.input}\n")
                                            if (state.bankInfo != null) append("بانک: ${state.bankInfo!!.bankName}\n")
                                            if (state.city != null) append("شهر: ${state.city}\n")
                                            append("وضعیت: معتبر")
                                        }
                                        val clip = ClipData.newPlainText("Validation Result", textToCopy)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE3F2FD),
                                        contentColor = Color(0xFF1976D2)
                                    )
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("کپی نتیجه", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }

                                IconButton(
                                    onClick = {
                                        val textToCopy = buildString {
                                            append("نوع: ${
                                                when (state.type) {
                                                    ValidationType.CARD -> "شماره کارت"
                                                    ValidationType.SHABA -> "شماره شبا"
                                                    ValidationType.NATIONAL_ID -> "کد ملی"
                                                }
                                            }\n")
                                            append("مقدار: ${state.input}\n")
                                            if (state.bankInfo != null) append("بانک: ${state.bankInfo!!.bankName}\n")
                                            if (state.city != null) append("شهر: ${state.city}\n")
                                            append("وضعیت: معتبر")
                                        }
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, textToCopy)
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFFE3F2FD), RoundedCornerShape(16.dp))
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "اشتراک گذاری", tint = Color(0xFF1976D2))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
                
    Spacer(modifier = Modifier.height(24.dp))
                
                if (validCount > 0 || invalidCount > 0) {
                    StatsSection(validCount, invalidCount, isDarkMode)
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                if (history.isNotEmpty()) {
                    HistorySection(history, context, isDarkMode)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun TypeSelector(selectedType: ValidationType, isDarkMode: Boolean, onTypeSelected: (ValidationType) -> Unit) {
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TypeButton(
            title = "شماره کارت",
            icon = Icons.Rounded.CreditCard,
            isSelected = selectedType == ValidationType.CARD,
            isDarkMode = isDarkMode,
            onClick = { onTypeSelected(ValidationType.CARD) },
            modifier = Modifier.weight(1f)
        )
        TypeButton(
            title = "شماره شبا",
            icon = Icons.Rounded.Numbers,
            isSelected = selectedType == ValidationType.SHABA,
            isDarkMode = isDarkMode,
            onClick = { onTypeSelected(ValidationType.SHABA) },
            modifier = Modifier.weight(1f)
        )
        TypeButton(
            title = "کد ملی",
            icon = Icons.Rounded.Person,
            isSelected = selectedType == ValidationType.NATIONAL_ID,
            isDarkMode = isDarkMode,
            onClick = { onTypeSelected(ValidationType.NATIONAL_ID) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TypeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val containerColor = if (isSelected) surfaceColor else Color.Transparent
    val contentColor = if (isSelected) Color(0xFF1976D2) else Color(0xFF757575)

    Box(
        modifier = modifier
            .height(48.dp)
            .then(
                if (isSelected) Modifier.shadow(4.dp, RoundedCornerShape(12.dp))
                else Modifier
            )
            .background(containerColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun StatsSection(validCount: Int, invalidCount: Int, isDarkMode: Boolean) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1C1E)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f))
            .background(surfaceColor, RoundedCornerShape(24.dp))
            .border(1.dp, if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Text(
            text = "آمار نشست فعلی",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("معتبر", fontSize = 14.sp, color = Color(0xFF2E7D32))
                    Text("$validCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("نامعتبر", fontSize = 14.sp, color = Color(0xFFD32F2F))
                    Text("$invalidCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Composable
fun HistorySection(history: List<HistoryItem>, context: android.content.Context, isDarkMode: Boolean) {
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1C1E)
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color.White
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.1f))
            .background(surfaceColor, RoundedCornerShape(24.dp))
            .border(1.dp, if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تاریخچه اخیر",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            TextButton(onClick = { exportToCsv(context, history) }) {
                Text("خروجی CSV", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        history.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (item.type) {
                            ValidationType.CARD -> "شماره کارت"
                            ValidationType.SHABA -> "شماره شبا"
                            ValidationType.NATIONAL_ID -> "کد ملی"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.input,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

fun exportToCsv(context: android.content.Context, history: List<HistoryItem>) {
    val csv = java.lang.StringBuilder()
    csv.append("\uFEFF") // UTF-8 BOM for Excel support
    csv.append("Type,Input\n")
    for (item in history) {
        val typeStr = when (item.type) {
            ValidationType.CARD -> "Card"
            ValidationType.SHABA -> "Shaba"
            ValidationType.NATIONAL_ID -> "National ID"
        }
        csv.append("$typeStr,${item.input}\n")
    }
    
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, csv.toString())
        type = "text/csv"
    }
    val shareIntent = android.content.Intent.createChooser(sendIntent, "صدور به CSV")
    context.startActivity(shareIntent)
}

class CardNumberTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.substring(0..15) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 15) out += "-"
        }
        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                if (offset <= 16) return offset + 3
                return 19
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                if (offset <= 19) return offset - 3
                return 16
            }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMapping)
    }
}

class ShabaTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 25) out += " "
        }
        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return offset + (offset / 4)
            }
            override fun transformedToOriginal(offset: Int): Int {
                return offset - (offset / 5)
            }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMapping)
    }
}

class NationalIdTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2 || i == 8) out += "-"
        }
        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 8) return offset + 1
                if (offset <= 10) return offset + 2
                return 12
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 10) return offset - 1
                if (offset <= 12) return offset - 2
                return 10
            }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), offsetMapping)
    }
}
