@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.data.history.HistoryDao
import com.example.data.history.HistoryItem
import kotlinx.coroutines.launch

enum class TabItem {
    SMART, BULK, GENERATOR, CITIES, BANKS
}

data class DropdownModel(val label: String, val value: String)

fun toEnglishDigits(str: String): String {
    var result = str
    val farsi = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    val arabic = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    for (i in 0..9) {
        result = result.replace(farsi[i], i.toString()).replace(arabic[i], i.toString())
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    cityRepository: CityRepository,
    historyDao: HistoryDao,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Configuration / Preferences states
    var isHapticEnabled by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Navigation state
    var selectedTab by remember { mutableStateOf(TabItem.SMART) }

    // Colors
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF9FBFC)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val borderCol = if (isDarkMode) Color(0xFF333333) else Color(0xFFEFEFEF)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Tab 1: Smart Detector States
    var smartInput by remember { mutableStateOf("") }
    val cleanSmartInput = toEnglishDigits(smartInput).replace("IR", "", ignoreCase = true).replace(Regex("[^0-9]"), "")
    val hasIrPrefix = toEnglishDigits(smartInput).uppercase().startsWith("IR")

    val detectedType = when {
        hasIrPrefix || cleanSmartInput.length > 16 -> ValidationType.SHABA
        cleanSmartInput.length <= 10 -> ValidationType.NATIONAL_ID
        else -> ValidationType.CARD
    }

    val visualTransformation = when (detectedType) {
        ValidationType.CARD -> CardNumberTransformation()
        ValidationType.SHABA -> ShabaTransformation()
        ValidationType.NATIONAL_ID -> NationalIdTransformation()
    }

    val isSmartValid = when (detectedType) {
        ValidationType.CARD -> Validator.validateCardNumber(cleanSmartInput)
        ValidationType.NATIONAL_ID -> Validator.validateNationalId(cleanSmartInput)
        ValidationType.SHABA -> Validator.validateShaba(cleanSmartInput)
    }

    val smartBankInfo = when (detectedType) {
        ValidationType.CARD -> BankRepository.findBankByCard(cleanSmartInput)
        ValidationType.SHABA -> BankRepository.findBankByShaba(cleanSmartInput)
        ValidationType.NATIONAL_ID -> null
    }

    val smartCityOfIssue = if (detectedType == ValidationType.NATIONAL_ID && isSmartValid) {
        cityRepository.getCityByNationalId(cleanSmartInput)
    } else null

    // Perform subtle haptic feedback on validation status changes
    var lastValidState by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(cleanSmartInput) {
        val currentTargetLen = when (detectedType) {
            ValidationType.CARD -> 16
            ValidationType.NATIONAL_ID -> 10
            ValidationType.SHABA -> 24
        }
        if (cleanSmartInput.length == currentTargetLen) {
            if (isSmartValid != lastValidState) {
                lastValidState = isSmartValid
                if (isHapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                // Save valid inputs to offline history
                if (isSmartValid) {
                    launch {
                        historyDao.insertItem(HistoryItem(input = cleanSmartInput, type = detectedType))
                        historyDao.cleanupOldEntries()
                    }
                }
            }
        } else {
            lastValidState = null
        }
    }

    // Tab 2: Bulk Validation States
    var bulkInput by remember { mutableStateOf("") }
    var bulkResults by remember { mutableStateOf<List<BulkResultItem>>(emptyList()) }
    var isBulkProcessed by remember { mutableStateOf(false) }

    // Tab 3: Test Generator States
    var genSelectedCardBank by remember { mutableStateOf("any") }
    var genSelectedShabaBank by remember { mutableStateOf("any") }
    var genSelectedMelliCity by remember { mutableStateOf("any") }
    var generatedCode by remember { mutableStateOf("") }

    // Tab 4: Cities Database States
    var citySearchQuery by remember { mutableStateOf("") }

    // QR Code scanner launcher
    val scanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        if (result.contents != null) {
            val digits = toEnglishDigits(result.contents)
            smartInput = digits
            if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, "بارکد با موفقیت اسکن شد", Toast.LENGTH_SHORT).show()
        }
    }

    // Settings/About dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "تنظیمات و درباره سامانه",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "این برنامه جهت اعتبارسنجی آفلاین، سریع و امن شماره کارت‌های شتابی، حساب‌های شبا و کدهای ملی کشور طراحی شده است. تمام پردازش‌ها بر روی دستگاه شما انجام شده و هیچ داده‌ای در بستر شبکه یا اینترنت ارسال نمی‌گردد.",
                        textAlign = TextAlign.Justify,
                        lineHeight = 24.sp,
                        fontSize = 13.sp,
                        color = textColor
                    )
                    Divider(color = borderCol)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بازخورد لمسی (ویبره خفیف)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Switch(
                            checked = isHapticEnabled,
                            onCheckedChange = { isHapticEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4F46E5),
                                checkedTrackColor = Color(0xFFC7D2FE)
                            )
                        )
                    }
                    Divider(color = borderCol)
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "نسخه برنامه: ۳.۱.۰ (بهینه شده)", fontSize = 11.sp, color = secondaryTextColor)
                        Text(text = "طراحی مدرن بر اساس Material Design 3", fontSize = 10.sp, color = secondaryTextColor)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Text("فهمیدم و قبول است", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = surfaceColor,
            titleContentColor = textColor,
            textContentColor = textColor,
            shape = RoundedCornerShape(28.dp)
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "صحت‌سنج هوشمند ایرانی",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = textColor
                            )
                            Text(
                                text = "پردازش ۱۰۰٪ آفلاین هویتی و بانکی",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryTextColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "تنظیمات",
                                tint = secondaryTextColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "تغییر تم",
                                tint = secondaryTextColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = surfaceColor
                    ),
                    modifier = Modifier.shadow(2.dp)
                )
            },
            bottomBar = {
                // Bottom bar for compact screens / phones
                NavigationBar(
                    containerColor = surfaceColor,
                    tonalElevation = 8.dp,
                    modifier = Modifier.shadow(16.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == TabItem.SMART,
                        onClick = { selectedTab = TabItem.SMART },
                        icon = { Icon(Icons.Default.Search, contentDescription = "هوشمند") },
                        label = { Text("هوشمند", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4F46E5),
                            selectedTextColor = Color(0xFF4F46E5),
                            indicatorColor = Color(0xFFEEF2F6)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == TabItem.BULK,
                        onClick = { selectedTab = TabItem.BULK },
                        icon = { Icon(Icons.Rounded.List, contentDescription = "گروهی") },
                        label = { Text("گروهی", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4F46E5),
                            selectedTextColor = Color(0xFF4F46E5),
                            indicatorColor = Color(0xFFEEF2F6)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == TabItem.GENERATOR,
                        onClick = { selectedTab = TabItem.GENERATOR },
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = "ژنراتور") },
                        label = { Text("ژنراتور", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4F46E5),
                            selectedTextColor = Color(0xFF4F46E5),
                            indicatorColor = Color(0xFFEEF2F6)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == TabItem.CITIES,
                        onClick = { selectedTab = TabItem.CITIES },
                        icon = { Icon(Icons.Rounded.Map, contentDescription = "شهرها") },
                        label = { Text("شهرها", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4F46E5),
                            selectedTextColor = Color(0xFF4F46E5),
                            indicatorColor = Color(0xFFEEF2F6)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == TabItem.BANKS,
                        onClick = { selectedTab = TabItem.BANKS },
                        icon = { Icon(Icons.Rounded.AccountBalance, contentDescription = "بانک‌ها") },
                        label = { Text("بانک‌ها", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4F46E5),
                            selectedTextColor = Color(0xFF4F46E5),
                            indicatorColor = Color(0xFFEEF2F6)
                        )
                    )
                }
            },
            containerColor = bgColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Render selected tab content
                Crossfade(targetState = selectedTab) { tab ->
                    when (tab) {
                        TabItem.SMART -> {
                            SmartDetectorTab(
                                input = smartInput,
                                onInputChange = { 
                                    val sanitized = toEnglishDigits(it).replace(Regex("[^0-9a-zA-Z]"), "")
                                    smartInput = sanitized
                                    if (isHapticEnabled && sanitized.isNotEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                cleanInput = cleanSmartInput,
                                detectedType = detectedType,
                                visualTransformation = visualTransformation,
                                isSmartValid = isSmartValid,
                                smartBankInfo = smartBankInfo,
                                smartCityOfIssue = smartCityOfIssue,
                                hasIrPrefix = hasIrPrefix,
                                scanLauncher = scanLauncher,
                                isDarkMode = isDarkMode,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                surfaceColor = surfaceColor,
                                borderCol = borderCol,
                                bgColor = bgColor,
                                context = context
                            )
                        }
                        TabItem.BULK -> {
                            BulkValidationTab(
                                bulkInput = bulkInput,
                                onBulkInputChange = { 
                                    bulkInput = it
                                    if (isBulkProcessed) {
                                        isBulkProcessed = false
                                        bulkResults = emptyList()
                                    }
                                },
                                bulkResults = bulkResults,
                                isBulkProcessed = isBulkProcessed,
                                onProcess = {
                                    val lines = bulkInput.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                    val resultsList = lines.map { rawLine ->
                                        val lineEng = toEnglishDigits(rawLine)
                                        val lineClean = lineEng.replace("IR", "", ignoreCase = true).replace(Regex("[^0-9]"), "")
                                        val hasIr = lineEng.uppercase().startsWith("IR")
                                        val lineType = when {
                                            hasIr || lineClean.length > 16 -> ValidationType.SHABA
                                            lineClean.length <= 10 -> ValidationType.NATIONAL_ID
                                            else -> ValidationType.CARD
                                        }
                                        val isValid = when (lineType) {
                                            ValidationType.CARD -> Validator.validateCardNumber(lineClean)
                                            ValidationType.NATIONAL_ID -> Validator.validateNationalId(lineClean)
                                            ValidationType.SHABA -> Validator.validateShaba(lineClean)
                                        }
                                        val issuer = when (lineType) {
                                            ValidationType.CARD -> BankRepository.findBankByCard(lineClean)?.bankName ?: "سایر بانک‌ها"
                                            ValidationType.SHABA -> BankRepository.findBankByShaba(lineClean)?.bankName ?: "بانک نامشخص شبا"
                                            ValidationType.NATIONAL_ID -> cityRepository.getCityByNationalId(lineClean)
                                        }
                                        BulkResultItem(rawLine, lineType, isValid, issuer)
                                    }
                                    bulkResults = resultsList
                                    isBulkProcessed = true
                                    if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onClear = {
                                    bulkInput = ""
                                    bulkResults = emptyList()
                                    isBulkProcessed = false
                                },
                                onDemo = {
                                    bulkInput = "IR270570077700000176199001\nIR350570077700014009791001\nIR720540160000000000873184460\nIR0301701200000000002100859755\n6037991234567890\n1234567890"
                                },
                                isDarkMode = isDarkMode,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                surfaceColor = surfaceColor,
                                borderCol = borderCol
                            )
                        }
                        TabItem.GENERATOR -> {
                            GeneratorTab(
                                genSelectedCardBank = genSelectedCardBank,
                                onGenSelectedCardBankChange = { genSelectedCardBank = it },
                                genSelectedShabaBank = genSelectedShabaBank,
                                onGenSelectedShabaBankChange = { genSelectedShabaBank = it },
                                genSelectedMelliCity = genSelectedMelliCity,
                                onGenSelectedMelliCityChange = { genSelectedMelliCity = it },
                                generatedCode = generatedCode,
                                onGenerateCode = { code ->
                                    generatedCode = code
                                    if (isHapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "کد تست تولید شد", Toast.LENGTH_SHORT).show()
                                },
                                cityRepository = cityRepository,
                                isDarkMode = isDarkMode,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                surfaceColor = surfaceColor,
                                borderCol = borderCol,
                                context = context
                            )
                        }
                        TabItem.CITIES -> {
                            CitiesDatabaseTab(
                                query = citySearchQuery,
                                onQueryChange = { citySearchQuery = it },
                                cityRepository = cityRepository,
                                isDarkMode = isDarkMode,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                surfaceColor = surfaceColor,
                                borderCol = borderCol
                            )
                        }
                        TabItem.BANKS -> {
                            BanksReferenceTab(
                                isDarkMode = isDarkMode,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                surfaceColor = surfaceColor,
                                borderCol = borderCol
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Tab 1: Smart Detector Tab
// ==========================================
@Composable
fun SmartDetectorTab(
    input: String,
    onInputChange: (String) -> Unit,
    cleanInput: String,
    detectedType: ValidationType,
    visualTransformation: VisualTransformation,
    isSmartValid: Boolean,
    smartBankInfo: BankInfo?,
    smartCityOfIssue: String?,
    hasIrPrefix: Boolean,
    scanLauncher: androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions>,
    isDarkMode: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    surfaceColor: Color,
    borderCol: Color,
    bgColor: Color,
    context: Context
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Intro
        Column {
            Text(
                text = "شناساگر هوشمند آنی",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )
            Text(
                text = "کافیست شماره را تایپ یا جایگذاری کنید. سیستم خودکار نوع کارت، شبا یا کد ملی را بررسی می‌کند.",
                fontSize = 11.sp,
                color = secondaryTextColor,
                lineHeight = 18.sp
            )
        }

        // Unified Input Section
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                .border(1.dp, borderCol, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val targetLength = when (detectedType) {
                    ValidationType.CARD -> 16
                    ValidationType.NATIONAL_ID -> 10
                    ValidationType.SHABA -> 24
                }
                val dynamicBorderColor = if (cleanInput.isEmpty()) {
                    borderCol
                } else if (cleanInput.length == targetLength) {
                    if (isSmartValid) Color(0xFF10B981) else Color(0xFFEF4444)
                } else if (cleanInput.length > targetLength) {
                    Color(0xFFEF4444)
                } else {
                    borderCol
                }

                val formatExplanation = when (detectedType) {
                    ValidationType.CARD -> "کارت: ۱۶ رقم (مثلاً ۳۴۵۶ ۹۰۱۲ ۵۶۷۸ ۱۲۳۴)"
                    ValidationType.NATIONAL_ID -> "کد ملی: ۱۰ رقم (مثلاً ۰-۴۵۶۷۸۹-۱۲۳)"
                    ValidationType.SHABA -> "شبا: ۲۴ رقم (مثلاً IR123...)"
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("کارت، شبا یا کد ملی...", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // LocalSoftwareKeyboardController will handle dismissing
                        }
                    ),
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = textColor,
                        textAlign = TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (dynamicBorderColor == borderCol) Color(0xFF4F46E5) else dynamicBorderColor,
                        unfocusedBorderColor = dynamicBorderColor,
                        focusedContainerColor = bgColor,
                        unfocusedContainerColor = bgColor,
                    ),
                    modifier = Modifier.fillMaxWidth().onKeyEvent { event ->
                        if (event.nativeKeyEvent.isCtrlPressed && event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                            onInputChange("")
                            true
                        } else {
                            false
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (input.isNotEmpty()) {
                                IconButton(onClick = { onInputChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = secondaryTextColor)
                                }
                            } else {
                                IconButton(onClick = {
                                    scanLauncher.launch(com.journeyapps.barcodescanner.ScanOptions().apply {
                                        setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.ALL_CODE_TYPES)
                                        setPrompt("بارکد یا QR Code را اسکن کنید")
                                        setBeepEnabled(false)
                                    })
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "اسکن", tint = Color(0xFF4F46E5))
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount!! > 0) {
                                        val pastedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        onInputChange(pastedText)
                                    } else {
                                        Toast.makeText(context, "حافظه موقت خالی است", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "جایگذاری", tint = Color(0xFF4F46E5))
                                }
                            }
                        }
                    }
                )
                
                // Info tooltip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "راهنما",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatExplanation,
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )
                }
            }
        }

        // Emulator Canvas Section
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = detectedType,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { type ->
                when (type) {
                    ValidationType.CARD -> {
                        CardEmulator(
                            cleanInput = cleanInput,
                            bankInfo = smartBankInfo,
                            isDarkMode = isDarkMode
                        )
                    }
                    ValidationType.NATIONAL_ID -> {
                        MelliEmulator(
                            cleanInput = cleanInput,
                            cityOfIssue = smartCityOfIssue,
                            isDarkMode = isDarkMode
                        )
                    }
                    ValidationType.SHABA -> {
                        ShabaEmulator(
                            cleanInput = cleanInput,
                            bankInfo = smartBankInfo,
                            hasIrPrefix = hasIrPrefix,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }

        // Validation Summary & Analysis Results (Visible if input exists)
        val targetLength = when (detectedType) {
            ValidationType.CARD -> 16
            ValidationType.NATIONAL_ID -> 10
            ValidationType.SHABA -> 24
        }
        if (cleanInput.length == targetLength) {
            val typeTitle = when (detectedType) {
                ValidationType.CARD -> "شماره کارت ۱۶ رقمی شتاب"
                ValidationType.NATIONAL_ID -> "کد ملی ۱۰ رقمی ایرانی"
                ValidationType.SHABA -> "شناسه بین‌المللی شبا (IBAN)"
            }

            val statusText = if (isSmartValid) "اطلاعات کاملاً معتبر است" else "خطا در صحت‌سنجی ریاضی"
            val statusColor = if (isSmartValid) Color(0xFF10B981) else Color(0xFFEF4444)
            val statusBg = if (isSmartValid) Color(0xFFECFDF5) else Color(0xFFFEF2F2)

            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                    .border(1.dp, borderCol, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Header status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "نوع داده شناسایی شده:", fontSize = 11.sp, color = secondaryTextColor)
                            Text(text = typeTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val copyText = "نوع: $typeTitle\nشماره: $input\nوضعیت: $statusText"
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                                    Toast.makeText(context, "در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(surfaceColor)
                                    .border(1.dp, borderCol, CircleShape)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی", modifier = Modifier.size(16.dp), tint = textColor)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(statusBg)
                                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Text(
                                        text = if (isSmartValid) "معتبر" else "نامعتبر",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = statusColor
                                )
                            }
                        }
                        }
                    }

                    Divider(color = borderCol)

                    // Explanation field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "توضیحات و صادرکننده:", fontSize = 12.sp, color = secondaryTextColor)
                        val explanation = when (detectedType) {
                            ValidationType.CARD -> smartBankInfo?.let { "بانک ${it.bankName}" } ?: "صادرکننده ناشناس شتاب"
                            ValidationType.SHABA -> smartBankInfo?.let { "حساب بانک ${it.bankName}" } ?: "بانک صادرکننده شبا ناشناس"
                            ValidationType.NATIONAL_ID -> smartCityOfIssue?.let { "شهرستان صادرکننده: $it" } ?: "کد شهرستان نامشخص"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (smartBankInfo?.logoResId != null && (detectedType == ValidationType.CARD || detectedType == ValidationType.SHABA)) {
                                Image(
                                    painter = painterResource(id = smartBankInfo.logoResId),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                                )
                            }
                            Text(text = explanation, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        }
                    }

                    // Live mathematical algorithm breakdown
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(16.dp))
                            .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "مراحل محاسبات و تحلیل الگوریتم زنده:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = secondaryTextColor,
                                fontFamily = FontFamily.SansSerif
                            )
                            val mathStepsText = when (detectedType) {
                                ValidationType.CARD -> {
                                    val luhnSum = getLuhnSum(cleanInput)
                                    "لان (Luhn): جمع کل ارقام تعاملی با ضریب ۲ متناوب برابر است با $luhnSum. از آنجا که بخش‌پذیری بر ۱۰ بررسی می‌شود: $luhnSum % 10 = ${luhnSum % 10}."
                                }
                                ValidationType.NATIONAL_ID -> {
                                    val (sum, rem, ctrl, expected) = getMelliAlgorithm(cleanInput)
                                    "ملی (Modulo 11): مجموع حاصلضرب ارقام در موقعیت‌ها = $sum. باقی‌مانده بر ۱۱ = $rem. رقم کنترلی ورودی $ctrl و رقم مورد انتظار بر اساس فرمول $expected می‌باشد."
                                }
                                ValidationType.SHABA -> {
                                    val rem = getShabaMod97(cleanInput)
                                    "شبا (ISO 7064 / Mod 97): چینش مجدد شناسه بین‌المللی شبا انجام گردید. حاصل تقسیم زنجیره‌ای بر ۹۷ برابر با باقی‌مانده $rem شد (انتظار تایید ریاضی = ۱)."
                                }
                            }
                            Text(
                                text = mathStepsText,
                                fontSize = 11.sp,
                                color = textColor,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Bottom quick actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val textToCopy = buildString {
                                    append("صحت‌سنج هوشمند\n")
                                    append("نوع: $typeTitle\n")
                                    append("مقدار: $input\n")
                                    if (smartBankInfo != null) append("صادرکننده: بانک ${smartBankInfo.bankName}\n")
                                    if (smartCityOfIssue != null) append("صادرکننده: $smartCityOfIssue\n")
                                    append("وضعیت: ${if (isSmartValid) "✅ معتبر" else "❌ نامعتبر"}")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Validation Info", textToCopy))
                                Toast.makeText(context, "با موفقیت کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("کپی نتیجه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val textToCopy = buildString {
                                    append("صحت‌سنج هوشمند\n")
                                    append("نوع: $typeTitle\n")
                                    append("مقدار: $input\n")
                                    if (smartBankInfo != null) append("صادرکننده: بانک ${smartBankInfo.bankName}\n")
                                    if (smartCityOfIssue != null) append("صادرکننده: $smartCityOfIssue\n")
                                    append("وضعیت: ${if (isSmartValid) "✅ معتبر" else "❌ نامعتبر"}")
                                }
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, textToCopy)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "اشتراک", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اشتراک‌گذاری", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Emulators Brushes
fun getCardBrush(bankInfo: BankInfo?): Brush {
    if (bankInfo == null) {
        return Brush.linearGradient(colors = listOf(Color(0xFF334155), Color(0xFF1E293B)))
    }
    val base = bankInfo.color
    return Brush.linearGradient(colors = listOf(base.copy(alpha = 0.85f), base, Color(0xFF0F172A)))
}

@Composable
fun CardEmulator(cleanInput: String, bankInfo: BankInfo?, isDarkMode: Boolean) {
    val displayNum = cleanInput.padEnd(16, '•')
    val formatted = "${displayNum.substring(0,4)}  ${displayNum.substring(4,8)}  ${displayNum.substring(8,12)}  ${displayNum.substring(12,16)}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(20.dp))
            .background(getCardBrush(bankInfo))
            .padding(16.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Card Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("کارت عضو شتاب", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
                    Text(bankInfo?.bankName ?: "بانک ناشناس", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bankInfo?.logoResId != null) {
                        Image(painter = painterResource(id = bankInfo.logoResId), contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Rounded.CreditCard, contentDescription = null, tint = Color.White)
                    }
                }
            }

            // Golden Chip
            Box(
                modifier = Modifier
                    .size(28.dp, 20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.linearGradient(colors = listOf(Color(0xFFFCD34D), Color(0xFFF59E0B))))
                    .border(0.5.dp, Color(0xFFD97706), RoundedCornerShape(4.dp))
            )

            // Number Display
            Text(
                text = formatted,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = Color.White,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Footer info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("دارنده کارت", fontSize = 7.sp, color = Color.White.copy(alpha = 0.5f))
                    Text("کاربر شتاب", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CVV2", fontSize = 7.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("***", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("انقضا", fontSize = 7.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("۱۴۱۲/۱۲", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MelliEmulator(cleanInput: String, cityOfIssue: String?, isDarkMode: Boolean) {
    val displayNum = cleanInput.padEnd(10, '•')
    val formatted = if (displayNum.length >= 10) {
        "${displayNum.substring(0,3)}-${displayNum.substring(3,9)}-${displayNum.substring(9,10)}"
    } else displayNum

    val melliBg = if (isDarkMode) {
        Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFFBAE6FD)))
    }
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(20.dp))
            .background(melliBg)
            .border(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFF93C5FD), RoundedCornerShape(20.dp))
            .padding(12.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
    ) {
        // Iranian flag stripe on top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(70.dp, 3.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF10B981), Color.White, Color(0xFFEF4444))
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("جمهوری اسلامی ایران", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text("کارت هوشمند ملی", fontSize = 7.sp, color = textColor.copy(alpha = 0.6f))
                }
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF3B82F6))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(50.dp, 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(36.dp))
                }

                // Fields
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "نام: شهروند نمونه", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(text = "نام خانوادگی: ایرانی", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(
                        text = "کد ملی: $formatted",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF4F46E5)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(text = "محل صدور: ${cityOfIssue ?: "---"}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(text = "تاریخ انقضا: ۱۴۱۵/۰۴", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = textColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun ShabaEmulator(cleanInput: String, bankInfo: BankInfo?, hasIrPrefix: Boolean, isDarkMode: Boolean) {
    val displayNum = cleanInput.padEnd(24, '•')
    val formatted = "IR${displayNum.substring(0,2)} ${displayNum.substring(2,6)} ${displayNum.substring(6,10)} ${displayNum.substring(10,14)} ${displayNum.substring(14,18)} ${displayNum.substring(18,22)} ${displayNum.substring(22,24)}"

    val shabaBg = if (isDarkMode) {
        Brush.linearGradient(colors = listOf(Color(0xFF065F46), Color(0xFF064E3B)))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFFA7F3D0)))
    }
    val textColor = if (isDarkMode) Color.White else Color(0xFF064E3B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(20.dp))
            .background(shabaBg)
            .border(1.dp, if (isDarkMode) Color(0xFF047857) else Color(0xFF6EE7B7), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("حواله پایا و ساتنا (شبا)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.7f))
                    Text(bankInfo?.let { "حساب بانک ${it.bankName}" } ?: "بانک صادرکننده شبا ناشناس", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0xFF064E3B) else Color.White.copy(alpha = 0.6f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bankInfo?.logoResId != null) {
                        Image(painter = painterResource(id = bankInfo.logoResId), contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = textColor)
                    }
                }
            }

            Column {
                Text("شناسه بین‌المللی حساب بانکی (IBAN):", fontSize = 7.sp, color = textColor.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = textColor,
                    letterSpacing = 0.5.sp
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                    Text("تراکنش امن شتابی", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
                }
                Text("کشور صادر کننده: ایران (IR)", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.6f))
            }
        }
    }
}

// Algorithmic details calculations
fun getLuhnSum(cleanInput: String): Int {
    if (cleanInput.length < 1) return 0
    var sum = 0
    for (i in cleanInput.indices) {
        var d = cleanInput[i].toString().toInt()
        if (i % 2 == 0) {
            d *= 2
            if (d > 9) d -= 9
        }
        sum += d
    }
    return sum
}

data class MelliAlgOutput(val sum: Int, val remainder: Int, val controlDigit: Int, val expected: Int)
fun getMelliAlgorithm(cleanInput: String): MelliAlgOutput {
    if (cleanInput.length < 10) return MelliAlgOutput(0, 0, 0, 0)
    var sum = 0
    for (i in 0..8) {
        sum += cleanInput[i].toString().toInt() * (10 - i)
    }
    val remainder = sum % 11
    val controlDigit = cleanInput[9].toString().toInt()
    val expected = if (remainder < 2) remainder else 11 - remainder
    return MelliAlgOutput(sum, remainder, controlDigit, expected)
}

fun getShabaMod97(cleanInput: String): Int {
    if (cleanInput.length < 24) return 0
    val rearranged = cleanInput.substring(2) + "1827" + cleanInput.substring(0, 2)
    var remainder = 0
    for (i in rearranged.indices) {
        remainder = (remainder * 10 + rearranged[i].toString().toInt()) % 97
    }
    return remainder
}


// ==========================================
// Tab 2: Bulk Validation Tab
// ==========================================
data class BulkResultItem(val rawInput: String, val type: ValidationType, val isValid: Boolean, val issuer: String)

@Composable
fun BulkValidationTab(
    bulkInput: String,
    onBulkInputChange: (String) -> Unit,
    bulkResults: List<BulkResultItem>,
    isBulkProcessed: Boolean,
    onProcess: () -> Unit,
    onClear: () -> Unit,
    onDemo: () -> Unit,
    isDarkMode: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    surfaceColor: Color,
    borderCol: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = "صحت‌سنجی فله‌ای (گروهی)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = textColor
                )
                Text(
                    text = "لیست شماره‌ها را خط به خط در کادر زیر قرار دهید.",
                    fontSize = 11.sp,
                    color = secondaryTextColor
                )
            }
            TextButton(onClick = onDemo) {
                Text("بارگذاری دیتای تستی", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF4F46E5))
            }
        }

        // Textarea input card
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                OutlinedTextField(
                    value = bulkInput,
                    onValueChange = onBulkInputChange,
                    placeholder = { Text("شماره کارت‌ها، شباها یا کدهای ملی را هر کدام در یک خط قرار دهید...", fontSize = 12.sp, lineHeight = 18.sp) },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = textColor
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                
                // Info tooltip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "راهنما",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "طول مجاز: کارت (۱۶ رقم)، کد ملی (۱۰ رقم)، شبا (۲۴ رقم پس از IR)",
                        fontSize = 11.sp,
                        color = secondaryTextColor
                    )
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onProcess,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("شروع صحت‌سنجی گروهی", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onClear,
                border = BorderStroke(1.dp, borderCol),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                Spacer(modifier = Modifier.width(6.dp))
                Text("پاکسازی کادر", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Red)
            }
        }

        // Results table list
        if (isBulkProcessed) {
            val totalValid = bulkResults.count { it.isValid }
            val totalInvalid = bulkResults.count { !it.isValid }

            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Summary stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("نتایج استخراج شده:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFECFDF5))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("$totalValid معتبر", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("$totalInvalid نامعتبـر", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = borderCol)

                    // Table items lazy scroll
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bulkResults) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDarkMode) Color(0xFF262626) else Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = item.rawInput,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val typeText = when (item.type) {
                                        ValidationType.CARD -> "کارت"
                                        ValidationType.SHABA -> "شبا"
                                        ValidationType.NATIONAL_ID -> "کد ملی"
                                    }
                                    Text(text = "$typeText • ${item.issuer}", fontSize = 10.sp, color = secondaryTextColor)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (item.isValid) Color(0xFF10B981) else Color(0xFFEF4444))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (item.isValid) "معتبر" else "نامعتبر",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// Tab 3: Generator Tab
// ==========================================
@Composable
fun GeneratorTab(
    genSelectedCardBank: String,
    onGenSelectedCardBankChange: (String) -> Unit,
    genSelectedShabaBank: String,
    onGenSelectedShabaBankChange: (String) -> Unit,
    genSelectedMelliCity: String,
    onGenSelectedMelliCityChange: (String) -> Unit,
    generatedCode: String,
    onGenerateCode: (String) -> Unit,
    cityRepository: CityRepository,
    isDarkMode: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    surfaceColor: Color,
    borderCol: Color,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "ژنراتور کدهای معتبر تست",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = textColor
            )
            Text(
                text = "کدهای تستی و ساختارمند هماهنگ با فرمول‌های شتاب، ISO شبا و کد ملی ایران بسازید.",
                fontSize = 11.sp,
                color = secondaryTextColor,
                lineHeight = 18.sp
            )
        }

        // 3 Generator cards
        // Card Generator
        GeneratorOptionCard(
            title = "تولید شماره کارت معتبر",
            description = "شماره کارت ۱۶ رقمی شتابی هماهنگ با فرمول Luhn",
            icon = Icons.Rounded.CreditCard,
            iconTint = Color(0xFF4F46E5),
            iconBg = Color(0xFFEEF2F6),
            dropdownOptions = BankRepository.banks.map { DropdownModel(it.bankName, it.cardPrefixes.first()) },
            selectedVal = genSelectedCardBank,
            onValChange = onGenSelectedCardBankChange,
            placeholderText = "انتخاب تصادفی بانک شتاب",
            btnText = "تولید کارت تست",
            btnColor = Color(0xFF4F46E5),
            onGenerate = {
                val prefix = if (genSelectedCardBank == "any") {
                    BankRepository.banks.random().cardPrefixes.first()
                } else genSelectedCardBank
                var num = prefix
                while (num.length < 15) {
                    num += (0..9).random().toString()
                }
                var sum = 0
                for (i in 0..14) {
                    var d = num[i].toString().toInt()
                    if (i % 2 == 0) {
                        d *= 2
                        if (d > 9) d -= 9
                    }
                    sum += d
                }
                val check = (10 - (sum % 10)) % 10
                onGenerateCode(num + check)
            },
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            surfaceColor = surfaceColor,
            borderCol = borderCol
        )

        // Shaba Generator
        GeneratorOptionCard(
            title = "تولید شماره شبا معتبر",
            description = "شبا معتبر ۲۴ رقمی و چک‌دیجیت منطبق با Mod 97",
            icon = Icons.Rounded.AccountBalance,
            iconTint = Color(0xFF10B981),
            iconBg = Color(0xFFECFDF5),
            dropdownOptions = BankRepository.banks.map { DropdownModel(it.bankName, it.shabaCode) },
            selectedVal = genSelectedShabaBank,
            onValChange = onGenSelectedShabaBankChange,
            placeholderText = "انتخاب تصادفی بانک شبا",
            btnText = "تولید شبا تست",
            btnColor = Color(0xFF10B981),
            onGenerate = {
                val bankCode = if (genSelectedShabaBank == "any") {
                    BankRepository.banks.random().shabaCode
                } else genSelectedShabaBank
                val bbb = bankCode.padStart(3, '0')
                var account = ""
                for (i in 1..19) {
                    account += (0..9).random().toString()
                }
                val bban = bbb + account
                val rearranged = bban + "182700"
                var remainder = 0
                for (i in rearranged.indices) {
                    remainder = (remainder * 10 + rearranged[i].toString().toInt()) % 97
                }
                var ccNum = (98 - remainder) % 97
                if (ccNum < 2) ccNum += 97
                val cc = ccNum.toString().padStart(2, '0')
                onGenerateCode("IR$cc$bban")
            },
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            surfaceColor = surfaceColor,
            borderCol = borderCol
        )

        // National ID Generator
        val citiesOptions = cityRepository.getAllCities().entries.map { DropdownModel(it.value, it.key) }.sortedBy { it.label }
        GeneratorOptionCard(
            title = "تولید کد ملی معتبر",
            description = "کد ملی معتبر ریاضی متصل به کد شهر صادرکننده",
            icon = Icons.Rounded.Person,
            iconTint = Color(0xFF8B5CF6),
            iconBg = Color(0xFFF5F3FF),
            dropdownOptions = citiesOptions,
            selectedVal = genSelectedMelliCity,
            onValChange = onGenSelectedMelliCityChange,
            placeholderText = "انتخاب تصادفی شهرستان",
            btnText = "تولید کد ملی تست",
            btnColor = Color(0xFF8B5CF6),
            onGenerate = {
                val prefix = if (genSelectedMelliCity == "any") {
                    cityRepository.getAllCities().keys.random()
                } else genSelectedMelliCity
                var num = prefix.padStart(3, '0')
                while (num.length < 9) {
                    num += (0..9).random().toString()
                }
                var sum = 0
                for (i in 0..8) {
                    sum += num[i].toString().toInt() * (10 - i)
                }
                val remainder = sum % 11
                val check = if (remainder < 2) remainder else 11 - remainder
                onGenerateCode(num + check)
            },
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            surfaceColor = surfaceColor,
            borderCol = borderCol
        )

        // Result Banner Card (if code is generated)
        if (generatedCode.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2F6)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("کد تولید شده تستی:", fontSize = 10.sp, color = secondaryTextColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = generatedCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF4F46E5),
                            letterSpacing = 1.sp
                        )
                    }
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Generated Test Code", generatedCode))
                            Toast.makeText(context, "کد تستی کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("کپی کد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratorOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    dropdownOptions: List<DropdownModel>,
    selectedVal: String,
    onValChange: (String) -> Unit,
    placeholderText: String,
    btnText: String,
    btnColor: Color,
    onGenerate: () -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    surfaceColor: Color,
    borderCol: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                    Text(text = description, fontSize = 10.sp, color = secondaryTextColor)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown box selector
                Box(modifier = Modifier.weight(1.5f)) {
                    val currentLabel = if (selectedVal == "any") placeholderText else {
                        dropdownOptions.firstOrNull { it.value == selectedVal }?.label ?: selectedVal
                    }
                    OutlinedButton(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentLabel, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.heightIn(max = 220.dp).width(180.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(placeholderText, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                onValChange("any")
                                expanded = false
                            }
                        )
                        for (opt in dropdownOptions) {
                            DropdownMenuItem(
                                text = { Text(opt.label, fontSize = 11.sp) },
                                onClick = {
                                    onValChange(opt.value)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Gen button
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(text = btnText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}


// ==========================================
// Tab 4: Cities Database Tab
// ==========================================
@Composable
fun CitiesDatabaseTab(
    query: String,
    onQueryChange: (String) -> Unit,
    cityRepository: CityRepository,
    isDarkMode: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    surfaceColor: Color,
    borderCol: Color
) {
    val allCities = remember { cityRepository.getAllCities().entries.toList().sortedBy { it.key } }
    val filteredCities = allCities.filter {
        it.key.contains(query) || it.value.contains(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "کدهای ۳ رقمی صادرکننده ملی",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = textColor
            )
            Text(
                text = "۳ رقم اول هر کد ملی مشخص کننده محل صدور شناسنامه می‌باشد.",
                fontSize = 11.sp,
                color = secondaryTextColor
            )
        }

        // Live searchable input
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("جستجوی نام شهرستان یا کد ۳ رقمی...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = secondaryTextColor) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = secondaryTextColor)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4F46E5),
                unfocusedBorderColor = borderCol,
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor
            )
        )

        // Table List
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("کد ۳ رقمی صادرکننده", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                    Text("شهرستان صادر کننده", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                }
                Divider(color = borderCol)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCities) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF262626) else Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.key,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF4F46E5)
                            )
                            Text(
                                text = entry.value,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// Tab 5: Banks Reference Tab
// ==========================================
@Composable
fun BanksReferenceTab(
    isDarkMode: Boolean,
    textColor: Color,
    secondaryTextColor: Color,
    surfaceColor: Color,
    borderCol: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "پیش‌شماره‌های شتاب و شناسه شبا",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = textColor
            )
            Text(
                text = "مرجع ارقام اختصاصی کارت‌های بانکی (۶ رقم اول) و ارقام شناسه شبا بانک‌ها.",
                fontSize = 11.sp,
                color = secondaryTextColor
            )
        }

        // Table reference
        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, borderCol, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("بانک صادر کننده", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                    }
                    Text("شناسه شبا", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor)
                    Text("پیش‌شماره کارت", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = secondaryTextColor, textAlign = TextAlign.End)
                }
                Divider(color = borderCol)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(BankRepository.banks) { bank ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF262626) else Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bank.logoResId != null) {
                                        Image(painter = painterResource(id = bank.logoResId), contentDescription = null, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Text(text = bank.bankName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
                            }

                            Text(
                                text = bank.shabaCode,
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF10B981)
                            )

                            Text(
                                text = bank.cardPrefixes.first(),
                                modifier = Modifier.weight(1.2f),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF4F46E5),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// Transformations
// ==========================================
class CardNumberTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != trimmed.lastIndex) out += " "
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= trimmed.length) return out.length
                return offset + (offset / 4)
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= out.length) return trimmed.length
                return offset - (offset / 5)
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class ShabaTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != trimmed.lastIndex) out += " "
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= trimmed.length) return out.length
                return offset + (offset / 4)
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= out.length) return trimmed.length
                return offset - (offset / 5)
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class NationalIdTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if ((i == 2 || i == 8) && i != trimmed.lastIndex) out += "-"
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= trimmed.length) return out.length
                var spaces = 0
                if (offset > 2) spaces++
                if (offset > 8) spaces++
                return offset + spaces
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= out.length) return trimmed.length
                var spaces = 0
                if (offset > 3) spaces++
                if (offset > 10) spaces++
                return offset - spaces
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
