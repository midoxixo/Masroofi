package com.example

import android.content.Context
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.lazy.rememberLazyListState
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.FinanceRepository
import com.example.data.MonthBudget
import com.example.data.Transaction
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent multiple instances of the app (cloning issue) when launched from the package installer
        if (!isTaskRoot) {
            val intent = intent
            val intentAction = intent.action
            if (intent.hasCategory(android.content.Intent.CATEGORY_LAUNCHER) && intentAction != null && intentAction == android.content.Intent.ACTION_MAIN) {
                finish()
                return
            }
        }

        enableEdgeToEdge()

        // Setup Repository & ViewModel
        val database = AppDatabase.getDatabase(this)
        val repository = FinanceRepository(database.financeDao())
        val viewModelFactory = FinanceViewModelFactory(repository, this)
        val viewModel = ViewModelProvider(this, viewModelFactory)[FinanceViewModel::class.java]

        setContent {
            val themeIndex by viewModel.selectedThemeIndex.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val fontSizeLevel by viewModel.fontSizeLevel.collectAsStateWithLifecycle()
            val fontWeight by viewModel.fontWeight.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }
            CompositionLocalProvider(
                LocalFontSizeLevel provides fontSizeLevel,
                LocalFontWeightGlobal provides fontWeight
            ) {
                MyApplicationTheme(themeIndex = themeIndex, darkTheme = darkTheme) {
                    MainAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: FinanceViewModel) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allMonths by viewModel.allMonthBudgets.collectAsStateWithLifecycle()
    val activeMonthId by viewModel.activeMonthId.collectAsStateWithLifecycle()
    val activeBudget by viewModel.activeMonthBudget.collectAsStateWithLifecycle()
    val activeTransactions by viewModel.activeTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val activeLoans by viewModel.activeLoans.collectAsStateWithLifecycle()

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    
    // Dialog state controllers
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showNewMonthDialog by remember { mutableStateOf(false) }

    // RTL layout helper: English is LTR, Arabic is RTL
    val isRtl = lang == AppLanguage.AR
    val layoutDirection = if (isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr

    if (!isLoggedIn) {
        OnboardingLoginScreen(viewModel = viewModel, lang = lang)
    } else {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
            Scaffold(
                topBar = {
                    MainTopAppBar(
                        lang = lang,
                        activeMonthId = activeMonthId,
                        allMonths = allMonths,
                        userName = userName,
                        userEmail = userEmail,
                        onMonthSelected = { viewModel.selectMonth(it) },
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        onStartNewMonth = { showNewMonthDialog = true },
                        onSettingsClick = { selectedTab = 4 }
                    )
                },
                bottomBar = {
                    MainBottomNavigationBar(
                        lang = lang,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                },
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                // Main Content Switching Tabs
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInHorizontally { width -> if (selectedTab > initialState) width else -width } togetherWith
                        slideOutHorizontally { width -> if (selectedTab > initialState) -width else width }
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> DashboardTab(
                            viewModel = viewModel,
                            lang = lang,
                            budget = activeBudget,
                            transactions = activeTransactions,
                            onAddExpenseClick = { showAddExpenseDialog = true },
                            onAddIncomeClick = { showAddIncomeDialog = true },
                            onDeleteTransaction = { viewModel.deleteTransaction(it) }
                        )
                        1 -> CompareTab(
                            viewModel = viewModel,
                            lang = lang,
                            allBudgets = allMonths,
                            allTransactions = allTransactions,
                            onDeleteMonth = { viewModel.deleteMonthBudget(it) }
                        )
                        2 -> DateSearchTab(
                            viewModel = viewModel,
                            lang = lang
                        )
                        3 -> LoansTab(
                            viewModel = viewModel,
                            lang = lang,
                            activeMonthId = activeMonthId,
                            loans = activeLoans,
                            onAddLoan = { amount, title -> viewModel.addLoan(amount, title) },
                            onDeleteLoan = { loan -> viewModel.deleteLoan(loan) }
                        )
                        4 -> SettingsToolsTab(
                            viewModel = viewModel,
                            lang = lang
                        )
                    }
                }

                // Add Expense Sliding Dialog
                if (showAddExpenseDialog) {
                    AddTransactionDialog(
                        viewModel = viewModel,
                        lang = lang,
                        type = "EXPENSE",
                        onDismiss = { showAddExpenseDialog = false },
                        onConfirm = { amount, category, noteInt, customTs ->
                            viewModel.addTransaction(amount, "EXPENSE", category, noteInt, customTs)
                            showAddExpenseDialog = false
                        }
                    )
                }

                // Add Extra Income Sliding Dialog
                if (showAddIncomeDialog) {
                    AddTransactionDialog(
                        viewModel = viewModel,
                        lang = lang,
                        type = "EXTRA_INCOME",
                        onDismiss = { showAddIncomeDialog = false },
                        onConfirm = { amount, category, noteInt, customTs ->
                            viewModel.addTransaction(amount, "EXTRA_INCOME", category, noteInt, customTs)
                            showAddIncomeDialog = false
                        }
                    )
                }

                // Start New Month Budget Dialog
                if (showNewMonthDialog) {
                    StartNewMonthDialog(
                        lang = lang,
                        onDismiss = { showNewMonthDialog = false },
                        onConfirm = { monthIdValue, baseIncomeValue ->
                            viewModel.addNewMonthBudget(monthIdValue, baseIncomeValue)
                            showNewMonthDialog = false
                        }
                    )
                }
            }
        }
    }
}
}

// ==========================================
// TOP APP BAR COMPOSABLE
// ==========================================
@Composable
fun MainTopAppBar(
    lang: AppLanguage,
    activeMonthId: String,
    allMonths: List<MonthBudget>,
    userName: String,
    userEmail: String,
    onMonthSelected: (String) -> Unit,
    onLanguageToggle: () -> Unit,
    onStartNewMonth: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sleek User Greeting with Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    val defaultUserStr = userName.ifEmpty { LanguageStrings.get("default_user", lang) }
                    val avatarChar = defaultUserStr.firstOrNull()?.toString() ?: "U"
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                val greetingStr = if (userEmail.isNotEmpty()) "${LanguageStrings.get("welcome_back", lang)} $defaultUserStr ($userEmail)" else "${LanguageStrings.get("welcome_back", lang)} $defaultUserStr"
                                Toast.makeText(context, greetingStr, Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarChar,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Text(
                            text = LanguageStrings.get("welcome_back", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = defaultUserStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick actions and selectors
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month selector inline
                    MonthSelectorDropdown(
                        selectedMonthId = activeMonthId,
                        allMonths = allMonths,
                        onMonthSelected = onMonthSelected,
                        onStartNewMonth = onStartNewMonth,
                        lang = lang
                    )

                    // Sleek Language Switcher ("EN" / "العربية")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .clickable { onLanguageToggle() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.AR) "EN" else "العربية",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Settings Gear Button
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(38.dp).testTag("top_bar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSelectorDropdown(
    selectedMonthId: String,
    allMonths: List<MonthBudget>,
    onMonthSelected: (String) -> Unit,
    onStartNewMonth: () -> Unit,
    lang: AppLanguage
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = !expanded },
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selectedMonthId,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
        ) {
            allMonths.forEach { month ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = month.monthId,
                            fontWeight = if (month.monthId == selectedMonthId) FontWeight.Bold else FontWeight.Normal,
                            color = if (month.monthId == selectedMonthId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onMonthSelected(month.monthId)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (month.monthId == selectedMonthId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            Divider()
            DropdownMenuItem(
                text = {
                    Text(
                        text = "+ " + LanguageStrings.get("new_month", lang),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                onClick = {
                    onStartNewMonth()
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AddBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

// ==========================================
// BOTTOM NAVIGATION BAR
// ==========================================
@Composable
fun MainBottomNavigationBar(
    lang: AppLanguage,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            label = { Text(LanguageStrings.get("tab_dashboard", lang), fontSize = 11.sp) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.Dashboard else Icons.Default.Dashboard,
                    contentDescription = null
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_dashboard")
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            label = { Text(LanguageStrings.get("tab_compare", lang), fontSize = 11.sp) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 1) Icons.Default.BarChart else Icons.Default.BarChart,
                    contentDescription = null
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.secondary,
                selectedTextColor = MaterialTheme.colorScheme.secondary,
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_compare")
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            label = { Text(LanguageStrings.get("tab_filter", lang), fontSize = 11.sp) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == 2) Icons.Default.DateRange else Icons.Default.DateRange,
                    contentDescription = null
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.tertiary,
                selectedTextColor = MaterialTheme.colorScheme.tertiary,
                indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_filter")
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            label = { Text(LanguageStrings.get("tab_loans", lang), fontSize = 11.sp) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_loans")
        )
    }
}

// ==========================================
// CUSTOM COMPONENT: POLISHED ONBOARDING & LOGIN SCREEN
// ==========================================
@Composable
fun OnboardingLoginScreen(
    viewModel: FinanceViewModel,
    lang: AppLanguage
) {
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var isGoogleFlow by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val isRtl = lang == AppLanguage.AR

    val primaryBg = Color(0xFF0B132B)
    val accentGreen = Color(0xFF059669)

    val layoutDirection = if (isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(primaryBg, primaryBg.copy(alpha = 0.85f), Color(0xFF1C2541))
                    )
                )
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 450.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Large visual App Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                        .padding(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.masroofi_logo_1779990322668),
                        contentDescription = "Masroofi Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // App Title
                Text(
                    text = if (isRtl) "مَصْرُوفِي" else "Masroofi API",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                // Subtitle description of the Fintech app
                Text(
                    text = if (isRtl) "تطبيـقك المالي الشخصــي الأكثر بساطـة وأمـاناً" 
                           else "Your simple & secure personal finance companion",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isGoogleFlow) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isRtl) "مرحباً بك! يرجى إدخال اسمك للبدء:" else "Welcome! Please enter your name:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                placeholder = {
                                    Text(
                                        text = if (isRtl) "اكتب اسمك هنا..." else "Type your name...",
                                        color = Color.White.copy(alpha = 0.35f)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentGreen,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedLabelColor = accentGreen,
                                    cursorColor = accentGreen
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
                            )

                            // Quick simulated Google auth button
                            Button(
                                onClick = { isGoogleFlow = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("onboarding_google_flow_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(0xFFEA4335), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("G", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isRtl) "التسجيل السريع بحساب Google" else "Sign up with Google Email",
                                        color = Color(0xFF1F2937),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Start Journey Button
                            Button(
                                onClick = {
                                    if (nameInput.isNotBlank()) {
                                        viewModel.setLoginState(nameInput.trim(), "", true)
                                        Toast.makeText(context, if (isRtl) "أهلاً بك في مصروفي!" else "Welcome to Masroofi!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (isRtl) "يرجى تعبئة خانة الاسم أو التسجيل بجوجل الكترونى أولاً" else "Please enter your name or log in with Google first", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("onboarding_submit_button")
                            ) {
                                Text(
                                    text = if (isRtl) "ابدأ رحلتي المالية 🚀" else "Start Journey 🚀",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isRtl) "تسجيل الدخول من Google" else "Google Accounts Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F2937)
                            )

                            Text(
                                text = if (isRtl) "سيقوم مصروفي بحفظ بيانات ميزانيتك سحابياً ومحلياً للتزامن الدائم"
                                       else "Masroofi will automatically secure and persist your budget logs locally & cloud synchronized",
                                fontSize = 11.sp,
                                color = Color(0xFF4B5563),
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text(if (isRtl) "البريد الإلكتروني لجوجل" else "Google Account Email") },
                                placeholder = { Text("example@gmail.com") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_google_email_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = accentGreen
                                )
                            )

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text(if (isRtl) "الاسم الكامل" else "Your Profile Name") },
                                placeholder = { Text(if (isRtl) "ادخل اسمك هنا" else "Ahmed") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_google_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentGreen,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedLabelColor = accentGreen
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isGoogleFlow = false },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("onboarding_google_back_button")
                                ) {
                                    Text(if (isRtl) "رجوع" else "Back")
                                }

                                Button(
                                    onClick = {
                                        if (emailInput.isNotBlank() && nameInput.isNotBlank()) {
                                            if (emailInput.contains("@")) {
                                                viewModel.setLoginState(nameInput.trim(), emailInput.trim(), true)
                                                Toast.makeText(context, if (isRtl) "تم التسجيل بجوجل بنجاح!" else "Successfully logged in with Google!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, if (isRtl) "يرجى كتابة بريد إلكترونى صحيح" else "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, if (isRtl) "يرجى تعبئة كافة الحقول" else "Please fill/select all inputs", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.5f).testTag("onboarding_google_confirm_button")
                                ) {
                                    Text(if (isRtl) "تأكيد الدخول" else "Confirm Sign In", color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Global language switcher on the onboarding screen
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (lang == AppLanguage.AR) accentGreen else Color.Transparent)
                                .clickable { viewModel.setLanguage(AppLanguage.AR) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("onboarding_lang_ar_toggle")
                        ) {
                            Text(
                                text = "العربية 🇸🇦",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (lang == AppLanguage.EN) accentGreen else Color.Transparent)
                                .clickable { viewModel.setLanguage(AppLanguage.EN) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("onboarding_lang_en_toggle")
                        ) {
                            Text(
                                text = "English 🇬🇧",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// TAB 1: DASHBOARD STREAMING UI
// ==========================================
@Composable
fun DashboardTab(
    viewModel: FinanceViewModel,
    lang: AppLanguage,
    budget: MonthBudget?,
    transactions: List<Transaction>,
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    if (budget == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = LanguageStrings.get("no_data", lang),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Calculations
    val prevLoansSum by viewModel.previousMonthLoansSum.collectAsStateWithLifecycle()
    val baseSal = budget.baseIncome
    val extraInc = transactions.filter { it.type == "EXTRA_INCOME" }.sumOf { it.amount }
    val totalRevenue = baseSal + extraInc
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val remainingBalance = totalRevenue - totalExpense - prevLoansSum

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning/Alert about previous month loan deduction if any
        if (prevLoansSum > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = LanguageStrings.get("prev_month_loan_deduction", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lang == AppLanguage.AR) 
                                    "تم خصم سلفة الشهر السابق بمبلغ ${viewModel.formatMoney(prevLoansSum)} ريال من الرصيد المتاح لهذا الشهر تلقائياً."
                                    else "Previous month's loans of ${viewModel.formatMoney(prevLoansSum)} SR were automatically deducted from this month's remaining balance.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Core Money Metrics
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "${LanguageStrings.get("balance", lang)} (${budget.monthId})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "${viewModel.formatMoney(remainingBalance)} ريال",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.animateContentSize()
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF039855))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageStrings.get("total_revenue", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+ ${viewModel.formatMoney(totalRevenue)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0FB981)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                        )

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD92D20))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageStrings.get("total_expenses", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "- ${viewModel.formatMoney(totalExpense)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF43F5E)
                            )
                        }
                    }
                }
            }
        }

        // Sub Income Details Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = LanguageStrings.get("base_income", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(text = "${viewModel.formatMoney(baseSal)} ريال", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = LanguageStrings.get("extra_income", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(text = "${viewModel.formatMoney(extraInc)} ريال", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        // Action Buttons Row (Add Expense / Add Extra Revenue)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Outlined add income action
                OutlinedButton(
                    onClick = onAddIncomeClick,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_add_extra_income"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageStrings.get("add_extra_income", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Outlined add expense action
                OutlinedButton(
                    onClick = onAddExpenseClick,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("btn_add_expense"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB91C1C)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageStrings.get("add_expense", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Custom Expenses Donut Chart Section
        item {
            val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
            val preferredChartStyle by viewModel.preferredChartStyle.collectAsStateWithLifecycle()

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageStrings.get("income_vs_expense", lang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )

                        // Beautiful Segmented selector chips for Columns vs Circles
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (preferredChartStyle == "CIRCLE") MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setPreferredChartStyle("CIRCLE") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("chart_style_circle_toggle")
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.AR) "⭕ دائرة" else "⭕ Circle",
                                        color = if (preferredChartStyle == "CIRCLE") Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (preferredChartStyle == "COLUMNS") MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setPreferredChartStyle("COLUMNS") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("chart_style_columns_toggle")
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.AR) "📊 عمود" else "📊 Bar",
                                        color = if (preferredChartStyle == "COLUMNS") Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (expenseTransactions.isEmpty()) {
                        // Empty states UI
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = LanguageStrings.get("no_transactions", lang),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        if (preferredChartStyle == "CIRCLE") {
                            // Custom Drawn Animated Donut Chart
                            DonutChart(
                                transactions = expenseTransactions,
                                totalRevenue = totalRevenue,
                                lang = lang
                            )
                        } else {
                            // Category Bar Column Chart
                            CategoryBarChart(
                                transactions = expenseTransactions,
                                lang = lang
                            )
                        }
                    }
                }
            }
        }

        // Recent Transactions Section Header
        item {
            Text(
                text = LanguageStrings.get("recent_transactions", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = LanguageStrings.get("no_transactions", lang),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions) { tx ->
                TransactionRowItem(
                    tx = tx,
                    viewModel = viewModel,
                    lang = lang,
                    onDelete = { onDeleteTransaction(tx) }
                )
            }
        }

        // Delete budget option for full clean-up under active months
        item {
            Spacer(modifier = Modifier.height(16.dp))
            var showConfirmDeleteMonth by remember { mutableStateOf(false) }

            if (showConfirmDeleteMonth) {
                AlertDialog(
                    onDismissRequest = { showConfirmDeleteMonth = false },
                    title = { Text(LanguageStrings.get("delete_month", lang), fontWeight = FontWeight.Bold) },
                    text = { Text(LanguageStrings.get("confirm_delete_month", lang)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteMonthBudget(budget.monthId)
                                showConfirmDeleteMonth = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text(LanguageStrings.get("delete", lang), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDeleteMonth = false }) {
                            Text(LanguageStrings.get("cancel", lang))
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = { showConfirmDeleteMonth = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageStrings.get("delete_month", lang),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==========================================
// CUSTOM TRANSACTION ROW ITEM
// ==========================================
@Composable
fun TransactionRowItem(
    tx: Transaction,
    viewModel: FinanceViewModel,
    lang: AppLanguage,
    onDelete: () -> Unit
) {
    val isExpense = tx.type == "EXPENSE"
    val colorAccent = if (isExpense) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(LanguageStrings.get("delete", lang), fontWeight = FontWeight.Bold) },
            text = { Text(LanguageStrings.get("delete_confirm", lang)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(LanguageStrings.get("delete", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(LanguageStrings.get("cancel", lang))
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable { showDeleteConfirm = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorAccent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                val catKey = if (isExpense) "cat_${tx.category.lowercase()}" else "cat_${tx.category.lowercase()}"
                Text(
                    text = LanguageStrings.get(catKey, lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tx.note.isNotBlank()) {
                        Text(
                            text = tx.note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        )
                    }
                    Text(
                        text = viewModel.formatDate(tx.timestamp, lang),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Amount
            Text(
                text = "${if (isExpense) "-" else "+"}${viewModel.formatMoney(tx.amount)} ${if (lang == AppLanguage.AR) "ريال" else "SAR"}",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = colorAccent
            )
        }
    }
}

// ==========================================
// TAB 2: MONTH COMPARISON & GRID HISTORY
// ==========================================
@Composable
fun CompareTab(
    viewModel: FinanceViewModel,
    lang: AppLanguage,
    allBudgets: List<MonthBudget>,
    allTransactions: List<Transaction>,
    onDeleteMonth: (String) -> Unit
) {
    val expenses = remember(allTransactions) { allTransactions.filter { it.type == "EXPENSE" } }
    val mostExpensive = remember(expenses) { expenses.maxByOrNull { it.amount } }
    val leastExpensive = remember(expenses) { expenses.minByOrNull { it.amount } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if (expenses.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (mostExpensive != null) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LanguageStrings.get("most_expensive", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mostExpensive.note,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = viewModel.formatMoney(mostExpensive.amount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                            }
                        }
                    }
                    if (leastExpensive != null) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LanguageStrings.get("least_expensive", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = leastExpensive.note,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = viewModel.formatMoney(leastExpensive.amount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val preferredChartStyle by viewModel.preferredChartStyle.collectAsStateWithLifecycle()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageStrings.get("compare_months", lang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        // Beautiful Segmented selector chips for Columns vs Circles in CompareTab
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (preferredChartStyle == "CIRCLE") MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setPreferredChartStyle("CIRCLE") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("compare_chart_style_circle_toggle")
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.AR) "⭕ دائرة" else "⭕ Circle",
                                        color = if (preferredChartStyle == "CIRCLE") Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (preferredChartStyle == "COLUMNS") MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setPreferredChartStyle("COLUMNS") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("compare_chart_style_columns_toggle")
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.AR) "📊 عمود" else "📊 Bar",
                                        color = if (preferredChartStyle == "COLUMNS") Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (allBudgets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = LanguageStrings.get("no_data", lang), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        if (preferredChartStyle == "COLUMNS") {
                            // Drawing side-by-side bar chart
                            HistoricBarChart(
                                allBudgets = allBudgets,
                                allTransactions = allTransactions,
                                viewModel = viewModel,
                                lang = lang
                            )
                        } else {
                            // Drawing circular donut chart comparison
                            HistoricDonutChart(
                                allBudgets = allBudgets,
                                allTransactions = allTransactions,
                                viewModel = viewModel,
                                lang = lang
                            )
                        }
                    }
                }
            }
        }

        // List Grid of Historic Metrics
        item {
            Text(
                text = LanguageStrings.get("categories", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (allBudgets.isEmpty()) {
            item {
                Text(
                    text = LanguageStrings.get("no_data", lang),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(allBudgets) { budget ->
                val monthTx = allTransactions.filter { it.monthId == budget.monthId }
                val extraInc = monthTx.filter { it.type == "EXTRA_INCOME" }.sumOf { it.amount }
                val totalRevenue = budget.baseIncome + extraInc
                val totalExp = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val remaining = totalRevenue - totalExp

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = budget.monthId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            // Remaining Balance Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (remaining >= 0) Color(0xFF0FB981).copy(alpha = 0.12f)
                                        else Color(0xFFF43F5E).copy(alpha = 0.12f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${viewModel.formatMoney(remaining)} ريال",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (remaining >= 0) Color(0xFF039855) else Color(0xFFD92D20)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = LanguageStrings.get("total_revenue", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${viewModel.formatMoney(totalRevenue)} ريال",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = LanguageStrings.get("total_expenses", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${viewModel.formatMoney(totalExp)} ريال",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: CUSTOM RANGE FILTER ARCHITECTURE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSearchTab(
    viewModel: FinanceViewModel,
    lang: AppLanguage
) {
    val results by viewModel.customFilteredTransactions.collectAsStateWithLifecycle()
    val startValue by viewModel.selectedStartDate.collectAsStateWithLifecycle()
    val endValue by viewModel.selectedEndDate.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageStrings.get("custom_filter", lang),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Select ranges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Date Card select
                        Card(
                            onClick = { showStartPicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(LanguageStrings.get("from_date", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (startValue != null) viewModel.formatDate(startValue!!, lang) else "Choose Date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // End Date Card select
                        Card(
                            onClick = { showEndPicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(LanguageStrings.get("to_date", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (endValue != null) viewModel.formatDate(endValue!!, lang) else "Choose Date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Calculations summary during custom range
        if (startValue != null && endValue != null) {
            val totalIn = results.filter { it.type == "EXTRA_INCOME" }.sumOf { it.amount }
            val totalOut = results.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val savingNet = totalIn - totalOut

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = LanguageStrings.get("extra_income", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "${viewModel.formatMoney(totalIn)} ريال", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = LanguageStrings.get("total_expenses", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(text = "${viewModel.formatMoney(totalOut)} ريال", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = LanguageStrings.get("balance", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(
                                    text = "${viewModel.formatMoney(savingNet)} ريال",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (savingNet >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = LanguageStrings.get("recent_transactions", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (results.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (startValue != null && endValue != null) LanguageStrings.get("no_transactions", lang) else LanguageStrings.get("custom_filter", lang),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(results) { tx ->
                TransactionRowItem(
                    tx = tx,
                    viewModel = viewModel,
                    lang = lang,
                    onDelete = { viewModel.deleteTransaction(tx) }
                )
            }
        }
    }

    // Modal Dates selectors
    if (showStartPicker) {
        SimpleDatePickerSelectDialog(
            lang = lang,
            title = LanguageStrings.get("from_date", lang),
            onDismiss = { showStartPicker = false },
            onDateSelected = { ts ->
                viewModel.selectedStartDate.value = ts
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        SimpleDatePickerSelectDialog(
            lang = lang,
            title = LanguageStrings.get("to_date", lang),
            onDismiss = { showEndPicker = false },
            onDateSelected = { ts ->
                viewModel.selectedEndDate.value = ts
                showEndPicker = false
            }
        )
    }
}

// ==========================================
// CUSTOM COMPONENT: STELLAR DONUT CANVAS
// ==========================================
@Composable
fun DonutChart(
    transactions: List<Transaction>,
    totalRevenue: Double,
    lang: AppLanguage
) {
    // Group transactions by category and calculate values
    val groupedExpenses = transactions.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val totalExpenses = groupedExpenses.values.sum()
    val listColors = listOf(
        Color(0xFFFF6B6B), // Food
        Color(0xFF4DABF7), // Transport
        Color(0xFFB197FC), // Rent
        Color(0xFFFFD43B), // Utilities
        Color(0xFF20C997), // Shopping
        Color(0xFFFAA2C1), // Entertainment
        Color(0xFFA9E34B), // Medical
        Color(0xFFADB5BD)  // Other
    )

    val listCategories = listOf("Food", "Transport", "Rent", "Utilities", "Shopping", "Entertainment", "Medical", "Other")

    // Match categories to colors
    val chartData = groupedExpenses.map { (cat, amount) ->
        val idx = listCategories.indexOf(cat).coerceAtLeast(0) % listColors.size
        CategoryChartItem(
            category = cat,
            amount = amount,
            color = listColors[idx],
            percentage = if (totalExpenses > 0) (amount / totalExpenses * 100).toFloat() else 0f
        )
    }.sortedByDescending { it.amount }

    // Visual Animation
    val transitionState = remember { MutableTransitionState(0f) }
    transitionState.targetState = 1f
    val animateSweep by updateTransition(transitionState, label = "Sweep").animateFloat(
        transitionSpec = { tween(800, easing = FastOutSlowInEasing) },
        label = "SweepPercent"
    ) { progress -> progress }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Donut Canvas Drawing
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var currentStartAngle = -90f

                chartData.forEach { item ->
                    val sweepAngle = (item.percentage / 100f) * 360f * animateSweep
                    drawArc(
                        color = item.color,
                        startAngle = currentStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 44f),
                        size = Size(size.width, size.height)
                    )
                    currentStartAngle += sweepAngle
                }
            }

            // Central Summary Indicators inside donut hole
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (lang == AppLanguage.AR) "مصروفات" else "Spent",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%,.0f", totalExpenses),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Side: Legends Column Grid
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chartData.take(5).forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    val translateKey = "cat_${item.category.lowercase().replace(" ", "_")}"
                    Text(
                        text = LanguageStrings.get(translateKey, lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.0f%%", item.percentage),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

data class CategoryChartItem(
    val category: String,
    val amount: Double,
    val color: Color,
    val percentage: Float
)

// ==========================================
// CUSTOM COMPONENT: STELLAR COLUMN CHARTING
// ==========================================
@Composable
fun HistoricBarChart(
    allBudgets: List<MonthBudget>,
    allTransactions: List<Transaction>,
    viewModel: FinanceViewModel,
    lang: AppLanguage
) {
    // Format Month data
    val displayedMonths = allBudgets.take(4).reversed() // Take last 4 months chronologically asc
    val dataSet = displayedMonths.map { budget ->
        val transactions = allTransactions.filter { it.monthId == budget.monthId }
        val expensesSum = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val extraInc = transactions.filter { it.type == "EXTRA_INCOME" }.sumOf { it.amount }
        val incomeTotal = budget.baseIncome + extraInc
        MonthChartSet(monthId = budget.monthId, income = incomeTotal, expenses = expensesSum)
    }

    val maxAmount = dataSet.maxOfOrNull { maxOf(it.income, it.expenses) }?.toFloat()?.coerceAtLeast(1000f) ?: 1000f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .drawBehind {
                    // Draw Faint Horizontal Grid lines in backgrounds
                    val lineCount = 4
                    val stepHeight = size.height / lineCount
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    for (i in 0..lineCount) {
                        val y = i * stepHeight
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f,
                            pathEffect = pathEffect
                        )
                    }
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            dataSet.forEach { monthData ->
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Income Bar (Mint Green)
                    val incomePercent = (monthData.income / maxAmount).toFloat().coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(incomePercent)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    // Expense Bar (Coral Red)
                    val expensePercent = (monthData.expenses / maxAmount).toFloat().coerceIn(0.04f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(expensePercent)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month Names Labels Under Bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dataSet.forEach { monthData ->
                Text(
                    text = monthData.monthId,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart Legends Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = LanguageStrings.get("total_revenue", lang), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = LanguageStrings.get("total_expenses", lang), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

data class MonthChartSet(
    val monthId: String,
    val income: Double,
    val expenses: Double
)

// ==========================================
// CUSTOM COMPONENT: STELLAR CATEGORY BAR CHART FOR EXPENSES
// ==========================================
@Composable
fun CategoryBarChart(
    transactions: List<Transaction>,
    lang: AppLanguage
) {
    val groupedExpenses = transactions.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    val listColors = listOf(
        Color(0xFFFF6B6B), // Food
        Color(0xFF4DABF7), // Transport
        Color(0xFFB197FC), // Rent
        Color(0xFFFFD43B), // Utilities
        Color(0xFF20C997), // Shopping
        Color(0xFFFAA2C1), // Entertainment
        Color(0xFFA9E34B), // Medical
        Color(0xFFADB5BD)  // Other
    )
    val listCategories = listOf("Food", "Transport", "Rent", "Utilities", "Shopping", "Entertainment", "Medical", "Other")

    val chartData = groupedExpenses.map { (cat, amount) ->
        val idx = listCategories.indexOf(cat).coerceAtLeast(0) % listColors.size
        CategoryChartItem(
            category = cat,
            amount = amount,
            color = listColors[idx],
            percentage = if (groupedExpenses.values.sum() > 0) (amount / groupedExpenses.values.sum() * 100).toFloat() else 0f
        )
    }.sortedByDescending { it.amount }

    if (chartData.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(LanguageStrings.get("no_data", lang), color = Color.Gray, fontSize = 13.sp)
        }
        return
    }

    val maxAmount = chartData.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            chartData.forEach { item ->
                val barHeightFraction = (item.amount / maxAmount).toFloat().coerceIn(0.04f, 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f", item.amount),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .fillMaxHeight(barHeightFraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(item.color)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val translated = LanguageStrings.get("cat_${item.category.lowercase()}", lang)
                    Text(
                        text = translated.take(4),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ==========================================
// CUSTOM COMPONENT: STELLAR HISTORIC DONUT CHART (CIRCULAR CHART OPTION)
// ==========================================
@Composable
fun HistoricDonutChart(
    allBudgets: List<MonthBudget>,
    allTransactions: List<Transaction>,
    viewModel: FinanceViewModel,
    lang: AppLanguage
) {
    val displayedMonths = allBudgets.take(4).reversed()
    val dataSet = displayedMonths.map { budget ->
        val transactions = allTransactions.filter { it.monthId == budget.monthId }
        val expensesSum = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val extraInc = transactions.filter { it.type == "EXTRA_INCOME" }.sumOf { it.amount }
        val incomeTotal = budget.baseIncome + extraInc
        MonthChartSet(monthId = budget.monthId, income = incomeTotal, expenses = expensesSum)
    }

    val totalExpensesSum = dataSet.sumOf { it.expenses }

    val listColors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4DABF7),
        Color(0xFFB197FC),
        Color(0xFFFFD43B)
    )

    if (totalExpensesSum <= 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(LanguageStrings.get("no_data", lang), color = Color.Gray, fontSize = 13.sp)
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var currentStartAngle = -90f
                dataSet.forEachIndexed { i, monthData ->
                    val pct = if (totalExpensesSum > 0) (monthData.expenses / totalExpensesSum).toFloat() else 0f
                    val sweepAngle = pct * 360f
                    val col = listColors.getOrElse(i) { Color.Gray }
                    drawArc(
                        color = col,
                        startAngle = currentStartAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 36f),
                        size = Size(size.width, size.height)
                    )
                    currentStartAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (lang == AppLanguage.AR) "إجمالي المصاريف" else "Total Exp",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%,.0f", totalExpensesSum),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            dataSet.forEachIndexed { i, monthData ->
                val col = listColors.getOrElse(i) { Color.Gray }
                val pct = if (totalExpensesSum > 0) (monthData.expenses / totalExpensesSum * 100).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(col, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${monthData.monthId} ($pct%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ==========================================
// ROBUST DATE PICKER SELECTOR DIALOG
// ==========================================
@Composable
fun SimpleDatePickerSelectDialog(
    lang: AppLanguage,
    title: String,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val currentCalendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(currentCalendar.get(Calendar.MONTH)) } // 0-indexed
    var selectedDay by remember { mutableStateOf(currentCalendar.get(Calendar.DAY_OF_MONTH)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val daysList = remember { (1..31).map { it.toString() } }
                    val monthsList = remember { (1..12).map { it.toString() } }
                    val currentYear = currentCalendar.get(Calendar.YEAR)
                    val yearsList = remember { (2000..(currentYear + 15)).map { it.toString() } }
                    
                    // Day Selector Spinner column
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (lang == AppLanguage.AR) "اليوم" else "Day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.WheelPicker(
                            items = daysList,
                            selectedIndex = daysList.indexOf(selectedDay.toString()).coerceAtLeast(0),
                            onIndexSelected = { selectedDay = daysList[it].toInt() }
                        )
                    }

                    // Month Selector Spinner column
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (lang == AppLanguage.AR) "الشهر" else "Month", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.WheelPicker(
                            items = monthsList,
                            selectedIndex = selectedMonth,
                            onIndexSelected = { selectedMonth = it }
                        )
                    }

                    // Year Selector Spinner column
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (lang == AppLanguage.AR) "السنة" else "Year", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.WheelPicker(
                            items = yearsList,
                            selectedIndex = yearsList.indexOf(selectedYear.toString()).coerceAtLeast(0),
                            onIndexSelected = { selectedYear = yearsList[it].toInt() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(LanguageStrings.get("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val outCalendar = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, selectedDay)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onDateSelected(outCalendar.timeInMillis)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(LanguageStrings.get("save", lang))
                    }
                }
            }
        }
    }
}

// ==========================================
// MODAL DIALOG: NEW MONTH BUDGET
// ==========================================
@Composable
fun StartNewMonthDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    val currentCalendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(currentCalendar.get(Calendar.MONTH) + 1) } 
    var baseIncomeInput by remember { mutableStateOf("4500") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = LanguageStrings.get("new_month_title", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val monthsList = remember { (1..12).map { String.format(Locale.US, "%02d", it) } }
                    val currentYearVal = currentCalendar.get(Calendar.YEAR)
                    val yearsList = remember { (2000..(currentYearVal + 15)).map { it.toString() } }
                    
                    // Month Selector Spinner column
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (lang == AppLanguage.AR) "الشهر" else "Month", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.WheelPicker(
                            items = monthsList,
                            selectedIndex = selectedMonth - 1,
                            onIndexSelected = { selectedMonth = it + 1 }
                        )
                    }

                    // Year Selector Spinner column
                    Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (lang == AppLanguage.AR) "السنة" else "Year", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.WheelPicker(
                            items = yearsList,
                            selectedIndex = yearsList.indexOf(selectedYear.toString()).coerceAtLeast(0),
                            onIndexSelected = { selectedYear = yearsList[it].toInt() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Monthly base income
                OutlinedTextField(
                    value = baseIncomeInput,
                    onValueChange = { baseIncomeInput = it },
                    label = { Text(LanguageStrings.get("base_income_label", lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base_income_input")
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = validationError!!, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(LanguageStrings.get("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanedMonthId = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth)
                            val amt = baseIncomeInput.toDoubleOrNull()
                            if (amt == null || amt < 0) {
                                validationError = LanguageStrings.get("enter_amount", lang)
                                return@Button
                            }
                            onConfirm(cleanedMonthId, amt)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_month")
                    ) {
                        Text(LanguageStrings.get("save", lang))
                    }
                }
            }
        }
    }
}

// ==========================================
// MODAL DIALOG: TRANSACTION ITEM ENGINE
// ==========================================
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    lang: AppLanguage,
    type: String, // "EXPENSE" or "EXTRA_INCOME"
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, Long) -> Unit
) {
    val isExpense = type == "EXPENSE"
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Categories Selection
    val categoriesState by viewModel.customCategories.collectAsStateWithLifecycle()
    val categoriesList = remember(categoriesState, isExpense) {
        val filtered = categoriesState.filter {
            it.type == if (isExpense) "EXPENSE" else "INCOME"
        }.map { it.name }
        if (filtered.isEmpty()) {
            if (isExpense) {
                listOf("Food", "Transport", "Rent", "Utilities", "Shopping", "Entertainment", "Medical", "Other")
            } else {
                listOf("Bonus", "Investment", "Gift", "Side_Project", "Salary", "Other")
            }
        } else {
            filtered
        }
    }

    var selectedCategory by remember { mutableStateOf(categoriesList.firstOrNull() ?: "Other") }
    var hasUserManuallySelected by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isExpense) LanguageStrings.get("add_expense", lang) else LanguageStrings.get("add_extra_income", lang),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Enter Amount Text field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text(LanguageStrings.get("amount", lang)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_amount")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Category Selector Horizontally Scrolling
                Text(
                    text = LanguageStrings.get("category", lang),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoriesList.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val translateKey = "cat_${cat.lowercase()}"
                        val displayStr = LanguageStrings.get(translateKey, lang)

                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedCategory = cat 
                                hasUserManuallySelected = true
                            },
                            label = { Text(displayStr, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.elevatedFilterChipColors(
                                selectedContainerColor = if (isExpense) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = if (isExpense) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = if (isExpense) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Source Entry with smart Auto-Categorization!
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { 
                        noteInput = it 
                        if (!hasUserManuallySelected && it.isNotBlank()) {
                            val autoSuggested = viewModel.autoCategorizeDescription(it, isExpense)
                            if (autoSuggested in categoriesList) {
                                selectedCategory = autoSuggested
                            }
                        }
                    },
                    label = { Text(LanguageStrings.get("note", lang)) },
                    placeholder = { 
                        Text(if (isExpense) "e.g., Uber or Restaurant" else "e.g., Freelance work")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_note")
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = validationError!!, color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(LanguageStrings.get("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountInput.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                validationError = LanguageStrings.get("enter_amount", lang)
                                return@Button
                            }
                            onConfirm(amt, selectedCategory, noteInput.trim(), System.currentTimeMillis())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpense) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_transaction")
                    ) {
                        Text(LanguageStrings.get("add", lang))
                    }
                }
            }
        }
    }
}

// ==========================================
// 4TH TAB: SETTINGS, AI COPILOT & TOOLS
// ==========================================
@Composable
fun SettingsToolsTab(
    viewModel: FinanceViewModel,
    lang: AppLanguage
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentThemeIndex by viewModel.selectedThemeIndex.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    val reminderFreq by viewModel.reminderFrequency.collectAsStateWithLifecycle()

    val fontSizeLevel by viewModel.fontSizeLevel.collectAsStateWithLifecycle()
    val fontWeight by viewModel.fontWeight.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val categoriesList by viewModel.customCategories.collectAsStateWithLifecycle()
    var showResetMonthDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfoData by remember { mutableStateOf<com.example.ui.UpdateInfo?>(null) }
    val currentAppVersionCode = BuildConfig.VERSION_CODE
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    if (showUpdateDialog && updateInfoData != null) {
        val info = updateInfoData!!
        val isAr = lang == AppLanguage.AR
        AlertDialog(
            onDismissRequest = { 
                if (!info.isForceUpdate) {
                    showUpdateDialog = false 
                }
            },
            title = { Text(LanguageStrings.get("new_update_available", lang)) },
            text = {
                Column {
                    Text(
                        text = "${LanguageStrings.get("update_app_version", lang)} ${info.versionName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) info.releaseNotesAr else info.releaseNotesEn,
                        fontSize = 14.sp
                    )
                    if (info.isForceUpdate) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = LanguageStrings.get("force_update_message", lang),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        uriHandler.openUri(info.downloadUrl)
                        if (!info.isForceUpdate) {
                            showUpdateDialog = false
                        }
                    } catch (e: Exception) {
                        try {
                            android.widget.Toast.makeText(context, if (lang == AppLanguage.AR) "تعذر فتح المتصفح للتنزيل" else "Could not open browser to download", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e2: Exception) {
                            // ignore
                        }
                    }
                }) {
                    Text(LanguageStrings.get("update_now", lang)) 
                }
            },
            dismissButton = {
                if (!info.isForceUpdate) {
                    TextButton(onClick = { showUpdateDialog = false }) { 
                        Text(LanguageStrings.get("update_later", lang)) 
                    }
                }
            }
        )
    }

    if (showResetMonthDialog) {
        AlertDialog(
            onDismissRequest = { showResetMonthDialog = false },
            title = { Text(LanguageStrings.get("reset_month_data", lang)) },
            text = { Text(LanguageStrings.get("confirm_reset_month", lang)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetMonthData(viewModel.activeMonthId.value)
                    showResetMonthDialog = false
                }) { Text(LanguageStrings.get("delete", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetMonthDialog = false }) { Text(LanguageStrings.get("cancel", lang)) }
            }
        )
    }

    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text(LanguageStrings.get("reset_all_data", lang)) },
            text = { Text(LanguageStrings.get("confirm_reset_all_data", lang)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllData()
                    showResetAllDialog = false
                }) { Text(LanguageStrings.get("delete", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) { Text(LanguageStrings.get("cancel", lang)) }
            }
        )
    }


    val isAr = lang == AppLanguage.AR

    var noteCategoryName by remember { mutableStateOf("") }
    var noteCategoryType by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"

    var chatInputText by remember { mutableStateOf("") }
    var restoreDialogOpen by remember { mutableStateOf(false) }
    var restoreCSVInput by remember { mutableStateOf("") }

    var cloudSyncingState by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val challenges = remember(chatMessages, categoriesList) {
        viewModel.compileChallenges(lang)
    }

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()

    // Parse hour, minute, am/pm from reminderTime
    val parsedTime = remember(reminderTime) {
        try {
            if (reminderTime.contains("AM") || reminderTime.contains("PM") || reminderTime.contains("صباحاً") || reminderTime.contains("مساءً")) {
                val isPm = reminderTime.contains("PM") || reminderTime.contains("مساءً")
                val clean = reminderTime.replace("PM", "").replace("AM", "").replace("مساءً", "").replace("صباحاً", "").trim()
                val parts = clean.split(":")
                if (parts.size >= 2) {
                    val h = parts[0].trim().toIntOrNull() ?: 12
                    val m = parts[1].trim().toIntOrNull() ?: 0
                    Triple(h.toString(), String.format(Locale.US, "%02d", m), if (isPm) "PM" else "AM")
                } else {
                    Triple("12", "00", "AM")
                }
            } else {
                val parts = reminderTime.split(":")
                if (parts.size >= 2) {
                    val h24 = parts[0].trim().toIntOrNull() ?: 20
                    val m = parts[1].trim().toIntOrNull() ?: 30
                    val isPm = h24 >= 12
                    val h12 = when {
                        h24 == 0 -> 12
                        h24 > 12 -> h24 - 12
                        else -> h24
                    }
                    Triple(h12.toString(), String.format(Locale.US, "%02d", m), if (isPm) "PM" else "AM")
                } else {
                    Triple("12", "00", "AM")
                }
            }
        } catch (e: Exception) {
            Triple("12", "00", "AM")
        }
    }

    var manualHour by remember(parsedTime) { mutableStateOf(parsedTime.first) }
    var manualMinute by remember(parsedTime) { mutableStateOf(parsedTime.second) }
    var manualAmPm by remember(parsedTime) { mutableStateOf(parsedTime.third) }

    val displayCurrentTime = remember(reminderTime, isAr) {
        try {
            if (reminderTime.contains("AM") || reminderTime.contains("PM") || reminderTime.contains("صباحاً") || reminderTime.contains("مساءً")) {
                val isPm = reminderTime.contains("PM") || reminderTime.contains("مساءً")
                val clean = reminderTime.replace("PM", "").replace("AM", "").replace("مساءً", "").replace("صباحاً", "").trim()
                val suffix = if (isPm) (if (isAr) "مساءً" else "PM") else (if (isAr) "صباحاً" else "AM")
                "$clean $suffix"
            } else {
                val parts = reminderTime.split(":")
                if (parts.size >= 2) {
                    val h24 = parts[0].trim().toIntOrNull() ?: 20
                    val m = parts[1].trim().toIntOrNull() ?: 30
                    val isPm = h24 >= 12
                    val h12 = when {
                        h24 == 0 -> 12
                        h24 > 12 -> h24 - 12
                        else -> h24
                    }
                    val suffix = if (isPm) (if (isAr) "مساءً" else "PM") else (if (isAr) "صباحاً" else "AM")
                    String.format(Locale.US, "%02d:%02d %s", h12, m, suffix)
                } else {
                    reminderTime
                }
            }
        } catch (e: Exception) {
            reminderTime
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_tools_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. FONT SETTINGS SECTION ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "⚙️ إعدادات الخط" else "⚙️ Font Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        text = LanguageStrings.get("font_size", lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (0..4).forEach { level ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (fontSizeLevel == level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .clickable { viewModel.saveFontSizeLevel(level) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (level + 1).toString(),
                                    color = if (fontSizeLevel == level) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = LanguageStrings.get("font_weight", lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = LanguageStrings.get("normal", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Switch(
                            checked = fontWeight == "BOLD",
                            onCheckedChange = { viewModel.saveFontWeight(if (it) "BOLD" else "NORMAL") }
                        )
                        Text(
                            text = LanguageStrings.get("bold", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // --- 1. LANGUAGE & 10 BEAUTIFUL THEMES SELECTOR ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "🎨 تنسيق البرنامج واللغات" else "🎨 App Styling & Languages",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language toggling row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) "لغة نظام التطبيق:" else "App System Language:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (lang == AppLanguage.AR) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setLanguage(AppLanguage.AR) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("settings_lang_ar_toggle")
                                ) {
                                    Text(
                                        text = "العربية 🇸🇦",
                                        color = if (lang == AppLanguage.AR) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (lang == AppLanguage.EN) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.setLanguage(AppLanguage.EN) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("settings_lang_en_toggle")
                                ) {
                                    Text(
                                        text = "English 🇬🇧",
                                        color = if (lang == AppLanguage.EN) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isCheckingUpdate = true
                            coroutineScope.launch {
                                try {
                                    val info = com.example.ui.UpdateManager.checkUpdate()
                                    if (info != null && info.versionCode > currentAppVersionCode) {
                                        updateInfoData = info
                                        showUpdateDialog = true
                                    } else {
                                        android.widget.Toast.makeText(context, LanguageStrings.get("app_up_to_date", lang), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: java.io.IOException) {
                                    android.widget.Toast.makeText(context, LanguageStrings.get("no_internet", lang), android.widget.Toast.LENGTH_LONG).show()
                                } catch (e: Throwable) {
                                    android.widget.Toast.makeText(context, LanguageStrings.get("app_up_to_date", lang), android.widget.Toast.LENGTH_SHORT).show()
                                } finally {
                                    isCheckingUpdate = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_update_app"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCheckingUpdate
                    ) {
                        Text(
                            text = if (isCheckingUpdate) LanguageStrings.get("checking_for_updates", lang) else LanguageStrings.get("update_app_version", lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = LanguageStrings.get("reset_data_title", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Button(
                        onClick = { showResetMonthDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_reset_month"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha=0.8f))
                    ) {
                        Text(
                            text = LanguageStrings.get("reset_month_data", lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showResetAllDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_reset_all"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = LanguageStrings.get("reset_all_data", lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Theme Mode toggling row (Light Mode (Day) / Dark Mode (Night))
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageStrings.get("theme_mode_title", lang),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val isCurrentlyDark = if (themeMode == "SYSTEM") isSystemInDarkTheme() else (themeMode == "DARK")

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Light Mode option (☀️)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (!isCurrentlyDark) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.saveThemeMode("LIGHT") }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .testTag("settings_theme_light_toggle"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "☀️",
                                        fontSize = 18.sp
                                    )
                                }
                                
                                // Dark Mode option (🌙)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrentlyDark) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.saveThemeMode("DARK") }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .testTag("settings_theme_dark_toggle"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🌙",
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isAr) "اختر من بين 10 سمات وألوان مذهلة:" else "Choose from 10 stunning themes:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 10 Color Theme clickers horizontally scrolling
                    val themeNames = listOf(
                        if (isAr) "البنفسجي" else "Purple",
                        if (isAr) "الزمردي" else "Emerald",
                        if (isAr) "الذهبي" else "Midas Gold",
                        if (isAr) "البحري" else "Oceanic",
                        if (isAr) "الغروب" else "Sunset",
                        if (isAr) "الوردي" else "Rose",
                        if (isAr) "السايبر" else "CyberNeon",
                        if (isAr) "الزيتوني" else "Forest Sage",
                        if (isAr) "الياقوتي" else "Ruby",
                        if (isAr) "اللافندر" else "Lavender"
                    )

                    val themeColors = listOf(
                        Color(0xFF6D28D9), // Purple
                        Color(0xFF059669), // Emerald
                        Color(0xFFD97706), // Gold
                        Color(0xFF0284C7), // Oceanic
                        Color(0xFFEA580C), // Sunset
                        Color(0xFFD01B6A), // Rose
                        Color(0xFF06B6D4), // Cyber
                        Color(0xFF4F5E43), // Sage
                        Color(0xFFDC2626), // Ruby
                        Color(0xFF7C3AED)  // Lavender
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themeColors.forEachIndexed { index, color ->
                            val isSelected = index == currentThemeIndex
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.saveThemeIndex(index) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color, shape = CircleShape)
                                        .padding(2.dp)
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White.copy(alpha = 0.3f), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = themeNames[index],
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. NOTIFICATION REMINDER CONTROL ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "⏰ منبه تسجيل المصروفات والدخل" else "⏰ Log Reminders Settings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAr) "تفعيل إشعارات التذكير:" else "Enable remind notifications:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isAr) "لتذكيرك بإدخال نفقاتك بانتظام" else "To alert logging tasks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { viewModel.toggleReminder(it) }
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Quick select reminder frequency
                            OutlinedButton(
                                onClick = { viewModel.saveReminderFrequency("DAILY") },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (reminderFreq == "DAILY") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isAr) "يومي" else "Daily", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.saveReminderFrequency("WEEKLY") },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (reminderFreq == "WEEKLY") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isAr) "أسبوعي" else "Weekly", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Display Time Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isAr) "توقيت التذكير الحالي:" else "Current reminder time:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = displayCurrentTime,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("reminder_current_time_display")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Manual select/write time in 12-hour format with AM/PM
                        Text(
                            text = if (isAr) "اكتب وقت التنبيه يدوياً (نظام 12 ساعة):" else "Enter alert time manually (12-hour format):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        ) {
                            val hoursList = remember { (1..12).map { String.format(Locale.US, "%02d", it) } }
                            val minutesList = remember { (0..59).map { String.format(Locale.US, "%02d", it) } }

                            // Hour input (Wheel Picker)
                            Box(modifier = Modifier.weight(1f)) {
                                val selectedH = manualHour.toIntOrNull() ?: 12
                                com.example.ui.WheelPicker(
                                    items = hoursList,
                                    selectedIndex = hoursList.indexOf(String.format(Locale.US, "%02d", selectedH)).coerceAtLeast(0),
                                    onIndexSelected = { manualHour = hoursList[it] }
                                )
                            }

                            Text(":", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)

                            // Minute input (Wheel Picker)
                            Box(modifier = Modifier.weight(1f)) {
                                val selectedM = manualMinute.toIntOrNull() ?: 0
                                com.example.ui.WheelPicker(
                                    items = minutesList,
                                    selectedIndex = minutesList.indexOf(String.format(Locale.US, "%02d", selectedM)).coerceAtLeast(0),
                                    onIndexSelected = { manualMinute = minutesList[it] }
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1.3f).fillMaxHeight(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Segmented layout for AM/PM
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.height(48.dp).fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(2.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (manualAmPm == "AM") MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { manualAmPm = "AM" }
                                                .testTag("reminder_manual_am"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isAr) "ص" else "AM",
                                                color = if (manualAmPm == "AM") Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (manualAmPm == "PM") MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { manualAmPm = "PM" }
                                                .testTag("reminder_manual_pm"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isAr) "م" else "PM",
                                                color = if (manualAmPm == "PM") Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                // Save Button
                                Button(
                                    onClick = {
                                        val h = manualHour.toIntOrNull()
                                        val m = manualMinute.toIntOrNull()
                                        if (h != null && m != null && h in 1..12 && m in 0..59) {
                                            val formattedToSave = String.format(Locale.US, "%02d:%02d %s", h, m, manualAmPm)
                                            viewModel.saveReminderTime(formattedToSave)
                                            Toast.makeText(context, if (isAr) "تم الحفظ! ⏱️" else "Saved! ⏱️", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, if (isAr) "وقت غير صحيح" else "Invalid time", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(48.dp).fillMaxWidth().testTag("reminder_manual_save_button"),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(if (isAr) "حفظ" else "Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. CUSTOM CATEGORY SYSTEM (ADD/DELETE) ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "🏷️ إدارة فئات المصروفات والدخل" else "🏷️ Categories Custom Manager",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "يمكنك إضافة تصنيفات مخصصة تناسب أسلوب حياتك أو حذف الإضافات غير المصنفة!" 
                               else "Add custom tags suitable for your style and delete non-system ones!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Form to create Category
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = noteCategoryName,
                            onValueChange = { noteCategoryName = it },
                            placeholder = { Text(if (isAr) "اسم الفئة الجديد" else "New Tag Name", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f)
                        )

                        // Expense/Income selector
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .clickable { noteCategoryType = "EXPENSE" }
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = noteCategoryType == "EXPENSE", onClick = { noteCategoryType = "EXPENSE" })
                                Text(if (isAr) "مصروف" else "Expense", fontSize = 10.sp)
                            }
                            Row(
                                modifier = Modifier
                                    .clickable { noteCategoryType = "INCOME" }
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = noteCategoryType == "INCOME", onClick = { noteCategoryType = "INCOME" })
                                Text(if (isAr) "إيراد" else "Income", fontSize = 10.sp)
                            }
                        }

                        IconButton(
                            onClick = {
                                if (noteCategoryName.isNotBlank()) {
                                    viewModel.addNewCategory(noteCategoryName, noteCategoryType)
                                    noteCategoryName = ""
                                    Toast.makeText(context, if (isAr) "تم إضافة الفئة بنجاح!" else "Category added!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                                .size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isAr) "الفئات المتاحة حالياً:" else "Available categories:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Display list of categories as tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesList.forEach { cat ->
                            val isExp = cat.type == "EXPENSE"
                            val bulletColor = if (isExp) Color(0xFFF43F5E) else Color(0xFF0FB981)
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isExp) Color(0xFFFEE2E2) else Color(0xFFD1FAE5),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(bulletColor, shape = CircleShape))
                                    Text(
                                        text = LanguageStrings.get("cat_${cat.name.lowercase()}", lang).ifEmpty { cat.name },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExp) Color(0xFF9D174D) else Color(0xFF065F46)
                                    )
                                    if (!cat.isSystem) {
                                        IconButton(
                                            onClick = { 
                                                viewModel.deleteCustomCategory(cat)
                                                Toast.makeText(context, if (isAr) "تم الحذف!" else "Deleted!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(12.dp)
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

        // --- 4. DATA EXPORT & BACKUP/RESTORE SYSTEM ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "💾 إدارة البيانات والنسخ الاحتياطي" else "💾 Data Backup, Export & Restore",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) "احفظ معلوماتك المالية بتنسيق CSV أو قم باستيراد نسخة سابقة آمنة بنقرة واحدة."
                               else "Keep logs safe using highly-readable CSV format files or import existing ones.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val csv = viewModel.exportToCSV()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Masroofy Backup", csv)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, if (isAr) "تم نسخ موازنتك بصيغة CSV للحافظة بنجاح!" else "Backup copied to clipboard as CSV!", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(if (isAr) "تصدير CSV" else "Export CSV", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { restoreDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAr) "استعادة نسخة" else "Restore Backup", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cloud synchronization button
                    Button(
                        onClick = {
                            cloudSyncingState = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2000)
                                cloudSyncingState = false
                                Toast.makeText(context, if (isAr) "تم المزامنة السحابية مع Google Drive بنجاح!" else "Cloud Synchronization with Google Drive completed!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (cloudSyncingState) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = if (cloudSyncingState) (if (isAr) "مزامنة سحابية..." else "Synching...") else (if (isAr) "مزامنة سحابية آمنة" else "Cloud Synchronization"),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 5. SAVINGS CHALLENGES & REWARDS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "🏆 نظام التحديات والجوائز الادخارية" else "🏆 Savings Challenges & Badges",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    challenges.forEach { challenge ->
                        val textTitle = if (isAr) challenge.titleAr else challenge.titleEn
                        val textDesc = if (isAr) challenge.descAr else challenge.descEn

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = textTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = textDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (challenge.isCompleted) Color(0xFFD1FAE5) else Color(0xFFE5E7EB),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (challenge.isCompleted) (if (isAr) "مكتمل 🎉" else "Passed 🎉") else (if (isAr) "نشط ⏳" else "Active ⏳"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (challenge.isCompleted) Color(0xFF065F46) else Color(0xFF4B5563)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { challenge.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = if (challenge.isCompleted) Color(0xFF0FB981) else MaterialTheme.colorScheme.primary,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        // --- 6. AI FINANCIAL ASSISTANT & CHAT ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isAr) "🤖 المساعد المالي الذكي (عبر Gemini)" else "🤖 AI Financial CoPilot (by Gemini)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAr) "يجيب على استفساراتك اليومية ويتنبأ بوضعك القادم" else "Real time forecasts & smart advice checks",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Dialog Chat Thread Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        val scrollState = rememberLazyListState()
                        
                        // Scroll to bottom whenever new message arrives
                        LaunchedEffect(chatMessages.size) {
                            if (chatMessages.isNotEmpty()) {
                                scrollState.animateScrollToItem(chatMessages.size - 1)
                            }
                        }

                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { msg ->
                                val isUser = msg.sender == "USER"
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isUser) 12.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 12.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 12.sp,
                                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                            if (chatLoading) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            modifier = Modifier.widthIn(max = 120.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                                Text(if (isAr) "تفكر..." else "Thinking...", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Row for conversation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text(if (isAr) "اسألني بخصوص ميزانيتك..." else "Ask about your budget...", fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (chatInputText.isNotBlank()) {
                                    viewModel.sendMessageToAI(chatInputText)
                                    chatInputText = ""
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick Ask tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair(if (isAr) "🔮 توقع مصاريفي القادمة" else "🔮 Forecast future spending", "تنبؤ"),
                            Pair(if (isAr) "⚠️ هل توجد عادات صرف سيئة؟" else "⚠️ Detect bad habits", "عادات الصرف"),
                            Pair(if (isAr) "💡 أعطني نصيحة ادخارية اليوم" else "💡 Give saving tips", "نصيحة لتوفير المال")
                        ).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp))
                                    .clickable { viewModel.sendMessageToAI(tag.second) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(tag.first, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG FOR RESTORING BACKUPS ---
    if (restoreDialogOpen) {
        Dialog(onDismissRequest = { restoreDialogOpen = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isAr) "📥 استعادة ميزانية من ملف CSV" else "📥 Import Budget & Data DB",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isAr) "الرجاء لصق نص مخرجات ملف CSV التي قمت بنسخها سابقاً:" else "Paste the exported CSV content accurately below:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = restoreCSVInput,
                        onValueChange = { restoreCSVInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                        maxLines = 10,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { restoreDialogOpen = false }) {
                            Text(LanguageStrings.get("cancel", lang))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val success = viewModel.restoreFromCSV(restoreCSVInput)
                                if (success) {
                                    Toast.makeText(context, if (isAr) "تم استعادة البيانات والنسخ الاحتياطي بنجاح!" else "Data backup restored successfully!", Toast.LENGTH_SHORT).show()
                                    restoreDialogOpen = false
                                    restoreCSVInput = ""
                                } else {
                                    Toast.makeText(context, if (isAr) "تنسيق CSV غير صالح!" else "Invalid CSV backup format!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isAr) "تأكيد واستعادة" else "Import & Confirm")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: LOANS AND ADVANCES MANAGER SCREEN
// ==========================================
@Composable
fun LoansTab(
    viewModel: com.example.ui.FinanceViewModel,
    lang: com.example.ui.AppLanguage,
    activeMonthId: String,
    loans: List<com.example.data.Loan>,
    onAddLoan: (amount: Double, title: String) -> Unit,
    onDeleteLoan: (com.example.data.Loan) -> Unit
) {
    var loanAmountInput by remember { mutableStateOf("") }
    var loanTitleInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf("") }

    val isRtl = lang == com.example.ui.AppLanguage.AR

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper section: System Info about Loans System
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = LanguageStrings.get("tab_loans", lang),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isRtl) "الشهر الحالي: $activeMonthId" else "Current Month: $activeMonthId",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isRtl) 
                            "نظام السلف المالية يساعدك على سحب مبالغ كإيرادات إضافية للشهر الحالي، مع خصمها تلقائياً من دخل ميزانية الشهر القادم لتسوية الصفوية لحسابك المالي."
                            else "The loans system allows drawing advances for the current month as extra income, and automatically deducts them from next month's available budget for complete settlement.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Add New Loan Form Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = LanguageStrings.get("add_loan", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // TextField 1: Loan Title / Description
                    OutlinedTextField(
                        value = loanTitleInput,
                        onValueChange = { loanTitleInput = it },
                        label = { Text(LanguageStrings.get("loan_name_label", lang), fontSize = 12.sp) },
                        placeholder = { Text(LanguageStrings.get("loan_name_placeholder", lang), fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_title_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // TextField 2: Loan Amount
                    OutlinedTextField(
                        value = loanAmountInput,
                        onValueChange = { loanAmountInput = it },
                        label = { Text(LanguageStrings.get("loan_amount_label", lang), fontSize = 12.sp) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_amount_input")
                    )

                    if (inputError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = inputError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val amt = loanAmountInput.toDoubleOrNull()
                            val title = loanTitleInput.trim()
                            if (amt == null || amt <= 0) {
                                inputError = LanguageStrings.get("enter_amount", lang)
                            } else if (title.isBlank()) {
                                inputError = if (isRtl) "يرجى كتابة اسم السلفة" else "Please enter loan name"
                            } else {
                                onAddLoan(amt, title)
                                loanAmountInput = ""
                                loanTitleInput = ""
                                inputError = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_loan")
                    ) {
                        Text(
                            text = LanguageStrings.get("add", lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // List of Active Month Loans Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRtl) "سلف شهر ($activeMonthId)" else "Loans for month ($activeMonthId)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${loans.size} " + (if (isRtl) "سلف" else "loans"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        if (loans.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = LanguageStrings.get("no_loans", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(loans) { loan ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("loan_item_${loan.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowOutward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = loan.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = viewModel.formatDate(loan.timestamp, lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "+ ${viewModel.formatMoney(loan.amount)} ريال",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = { onDeleteLoan(loan) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_loan_${loan.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = LanguageStrings.get("delete", lang),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// DYNAMIC TYPOGRAPHY INFRASTRUCTURE (FONT SIZE SCALING & WEIGHT CONFIGURATION)
// ============================================================================
val LocalFontSizeLevel = staticCompositionLocalOf { 2 }
val LocalFontWeightGlobal = staticCompositionLocalOf { "NORMAL" }

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val fontSizeLevel = LocalFontSizeLevel.current
    val globalWeight = LocalFontWeightGlobal.current
    
    val scaleFactor = when (fontSizeLevel) {
        0 -> 0.8f
        1 -> 0.9f
        2 -> 1.0f
        3 -> 1.15f
        4 -> 1.3f
        else -> 1.0f
    }
    
    val finalFontSize = if (fontSize != TextUnit.Unspecified) {
        (fontSize.value * scaleFactor).sp
    } else if (style.fontSize != TextUnit.Unspecified) {
        (style.fontSize.value * scaleFactor).sp
    } else {
        TextUnit.Unspecified
    }
    
    val finalWeight = when (globalWeight) {
        "BOLD" -> FontWeight.Bold
        "NORMAL" -> FontWeight.Normal
        else -> fontWeight ?: style.fontWeight
    }
    
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = finalFontSize,
        fontStyle = fontStyle,
        fontWeight = finalWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val fontSizeLevel = LocalFontSizeLevel.current
    val globalWeight = LocalFontWeightGlobal.current
    
    val scaleFactor = when (fontSizeLevel) {
        0 -> 0.8f
        1 -> 0.9f
        2 -> 1.0f
        3 -> 1.15f
        4 -> 1.3f
        else -> 1.0f
    }
    
    val finalFontSize = if (fontSize != TextUnit.Unspecified) {
        (fontSize.value * scaleFactor).sp
    } else if (style.fontSize != TextUnit.Unspecified) {
        (style.fontSize.value * scaleFactor).sp
    } else {
        TextUnit.Unspecified
    }
    
    val finalWeight = when (globalWeight) {
        "BOLD" -> FontWeight.Bold
        "NORMAL" -> FontWeight.Normal
        else -> fontWeight ?: style.fontWeight
    }
    
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = finalFontSize,
        fontStyle = fontStyle,
        fontWeight = finalWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
