package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.CustomCategory
import com.example.data.FinanceRepository
import com.example.data.MonthBudget
import com.example.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Representation of a Savings Challenge
data class SavingsChallenge(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descAr: String,
    val descEn: String,
    val icon: String,
    val targetType: String, // "REDUCTION", "SAVINGS_RATE", "LOGS_COUNT", "BUDGET_SET"
    val isCompleted: Boolean = false,
    val progress: Float = 0f // 0f to 1f
)

// Representation of a Chat Message for the AI Assistant
data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(
    private val repository: FinanceRepository,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("masroofy_new_prefs", Context.MODE_PRIVATE)

    // Language state: defaults to Arabic, can switch to English
    private val _currentLanguage = MutableStateFlow(
        AppLanguage.valueOf(prefs.getString("current_language", AppLanguage.AR.name) ?: AppLanguage.AR.name)
    )
    val currentLanguage = _currentLanguage.asStateFlow()

    // --- Login & Welcome State ---
    private val _userName = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail = _userEmail.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn = _isLoggedIn.asStateFlow()

    // --- Preferred Chart Layout ("CIRCLE" or "COLUMNS") ---
    private val _preferredChartStyle = MutableStateFlow(prefs.getString("preferred_chart_style", "CIRCLE") ?: "CIRCLE")
    val preferredChartStyle = _preferredChartStyle.asStateFlow()

    // 10 Customizable beautiful themes state
    private val _selectedThemeIndex = MutableStateFlow(prefs.getInt("theme_index", 0))
    val selectedThemeIndex = _selectedThemeIndex.asStateFlow()

    // --- Font customization: 5 levels (0-4), weight (NORMAL/BOLD) ---
    private val _fontSizeLevel = MutableStateFlow(prefs.getInt("font_size_level", 2))
    val fontSizeLevel = _fontSizeLevel.asStateFlow()

    private val _fontWeight = MutableStateFlow(prefs.getString("font_weight", "NORMAL") ?: "NORMAL")
    val fontWeight = _fontWeight.asStateFlow()

    // Mode of theme: SYSTEM, LIGHT, DARK
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode = _themeMode.asStateFlow()

    // Reminder notifications customizable options
    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean("reminder_enabled", true))
    val reminderEnabled = _reminderEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(prefs.getString("reminder_time", "20:30") ?: "20:30")
    val reminderTime = _reminderTime.asStateFlow()

    private val _reminderFrequency = MutableStateFlow(prefs.getString("reminder_freq", "DAILY") ?: "DAILY")
    val reminderFrequency = _reminderFrequency.asStateFlow()

    // Active selected month for the dashboard
    val activeMonthId = MutableStateFlow("2026-05")

    // Retrieve all recorded monthly budgets
    val allMonthBudgets: StateFlow<List<MonthBudget>> = repository.allMonthBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive flow for the active month's budget details
    val activeMonthBudget: StateFlow<MonthBudget?> = activeMonthId
        .flatMapLatest { id -> repository.getMonthBudgetFlow(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Reactive flow for the active month's transactions
    val activeTransactions: StateFlow<List<Transaction>> = activeMonthId
        .flatMapLatest { id -> repository.getTransactionsForMonth(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Loan Reactive Flows ---
    val allLoans: StateFlow<List<com.example.data.Loan>> = repository.allLoans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeLoans: StateFlow<List<com.example.data.Loan>> = activeMonthId
        .flatMapLatest { id -> repository.getLoansForMonth(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val previousMonthIdFlow: StateFlow<String> = activeMonthId
        .map { id -> getPreviousMonthId(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val previousMonthLoansSum: StateFlow<Double> = previousMonthIdFlow
        .flatMapLatest { prevId ->
            if (prevId.isBlank()) flowOf(emptyList()) else repository.getLoansForMonth(prevId)
        }
        .map { loans -> loans.sumOf { it.amount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // All transactions in the system
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive custom categories table
    val customCategories: StateFlow<List<CustomCategory>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Date range inputs for search
    val selectedStartDate = MutableStateFlow<Long?>(null)
    val selectedEndDate = MutableStateFlow<Long?>(null)

    val customFilteredTransactions: StateFlow<List<Transaction>> = combine(
        selectedStartDate,
        selectedEndDate
    ) { start, end ->
        if (start != null && end != null) {
            Pair(start, end + 86399999L)
        } else {
            null
        }
    }.flatMapLatest { range ->
        if (range != null) {
            repository.getTransactionsBetweenDates(range.first, range.second)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- AI Chatbot and Assistant State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    init {
        // Pre-populate database and configuration if empty
        viewModelScope.launch {
            repository.allMonthBudgets.first().let { list ->
                if (list.isEmpty()) {
                    setupSampleData()
                }
            }
            repository.allCategories.first().let { cats ->
                if (cats.isEmpty()) {
                    setupDefaultCategories()
                }
            }
            // Add a warm welcome message from Assistant
            val initialWelcome = if (_currentLanguage.value == AppLanguage.AR) {
                "أهلاً بك! أنا مساعدك المالي الذكي مصروفي 🤖💰. اسألني عن نصائح التوفير، أو توقعات مصاريفك للشهر القادم، أو كيف تدخر بكفاءة!"
            } else {
                "Welcome! I am Masroofy, your Smart AI Financial Assistant 🤖💰. Ask me about saving tips, future spending projections, or smart budget plans!"
            }
            _chatMessages.value = listOf(ChatMessage("AI", initialWelcome))
        }
    }

    private suspend fun setupSampleData() {
        // May Budget 2026
        val mayBudget = MonthBudget("2026-05", 4500.0, "May Initial Budget")
        repository.insertMonthBudget(mayBudget)

        val mayDay = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 25, 12, 0)
        }.timeInMillis

        repository.insertTransaction(Transaction(monthId = "2026-05", amount = 350.0, type = "EXPENSE", category = "Food", note = "Healthy Groceries", timestamp = mayDay))
        repository.insertTransaction(Transaction(monthId = "2026-05", amount = 120.0, type = "EXPENSE", category = "Transport", note = "Gasoline Fuel", timestamp = mayDay + 3600000))
        repository.insertTransaction(Transaction(monthId = "2026-05", amount = 800.0, type = "EXPENSE", category = "Rent", note = "Monthly House Rent", timestamp = mayDay - 80000000))
        repository.insertTransaction(Transaction(monthId = "2026-05", amount = 200.0, type = "EXPENSE", category = "Utilities", note = "Home Internet Bill", timestamp = mayDay + 50000000))
        repository.insertTransaction(Transaction(monthId = "2026-05", amount = 150.0, type = "EXPENSE", category = "Shopping", note = "New Summer Shirt", timestamp = mayDay + 90000000))
        repository.insertTransaction(Transaction(monthId = "2026-05", amount = 750.0, type = "EXTRA_INCOME", category = "Side_Project", note = "Mobile App Design contract", timestamp = mayDay + 100000000))

        // April Budget 2026
        val aprilBudget = MonthBudget("2026-04", 4000.0, "April Budget")
        repository.insertMonthBudget(aprilBudget)
        val aprilDay = Calendar.getInstance().apply {
            set(2026, Calendar.APRIL, 18, 14, 0)
        }.timeInMillis
        repository.insertTransaction(Transaction(monthId = "2026-04", amount = 400.0, type = "EXPENSE", category = "Food", note = "Supermarket", timestamp = aprilDay))
        repository.insertTransaction(Transaction(monthId = "2026-04", amount = 1500.0, type = "EXPENSE", category = "Rent", note = "Apartment Rent", timestamp = aprilDay - 100000000))
        repository.insertTransaction(Transaction(monthId = "2026-04", amount = 220.0, type = "EXPENSE", category = "Utilities", note = "Electricity Bill", timestamp = aprilDay + 50000000))
        repository.insertTransaction(Transaction(monthId = "2026-04", amount = 500.0, type = "EXTRA_INCOME", category = "Bonus", note = "Company Quarterly Bonus", timestamp = aprilDay - 2000000))
    }

    private suspend fun setupDefaultCategories() {
        val defaultCats = listOf(
            // Expense
            CustomCategory("Food", "EXPENSE", true),
            CustomCategory("Transport", "EXPENSE", true),
            CustomCategory("Rent", "EXPENSE", true),
            CustomCategory("Utilities", "EXPENSE", true),
            CustomCategory("Shopping", "EXPENSE", true),
            CustomCategory("Entertainment", "EXPENSE", true),
            CustomCategory("Medical", "EXPENSE", true),
            CustomCategory("Other", "EXPENSE", true),
            // Income
            CustomCategory("Bonus", "INCOME", true),
            CustomCategory("Investment", "INCOME", true),
            CustomCategory("Gift", "INCOME", true),
            CustomCategory("Side_Project", "INCOME", true),
            CustomCategory("Salary", "INCOME", true),
            CustomCategory("Other", "INCOME", true)
        )
        defaultCats.forEach { repository.insertCategory(it) }
    }

    // --- Language Switches ---
    fun toggleLanguage() {
        val newLang = if (_currentLanguage.value == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
        _currentLanguage.value = newLang
        prefs.edit().putString("current_language", newLang.name).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("current_language", lang.name).apply()
    }

    // --- Onboarding & Authorization State Managers ---
    fun setLoginState(name: String, email: String, loggedIn: Boolean) {
        _userName.value = name
        _userEmail.value = email
        _isLoggedIn.value = loggedIn
        prefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .putBoolean("is_logged_in", loggedIn)
            .apply()
    }

    fun logout() {
        _userName.value = ""
        _userEmail.value = ""
        _isLoggedIn.value = false
        prefs.edit()
            .remove("user_name")
            .remove("user_email")
            .putBoolean("is_logged_in", false)
            .apply()
    }

    fun setPreferredChartStyle(style: String) {
        _preferredChartStyle.value = style
        prefs.edit().putString("preferred_chart_style", style).apply()
    }

    // --- Dynamic 10 Themes Selection ---
    fun saveThemeIndex(index: Int) {
        if (index in 0..9) {
            _selectedThemeIndex.value = index
            prefs.edit().putInt("theme_index", index).apply()
        }
    }

    fun saveThemeMode(mode: String) {
        if (mode == "SYSTEM" || mode == "LIGHT" || mode == "DARK") {
            _themeMode.value = mode
            prefs.edit().putString("theme_mode", mode).apply()
        }
    }

    fun saveFontSizeLevel(level: Int) {
        if (level in 0..4) {
            _fontSizeLevel.value = level
            prefs.edit().putInt("font_size_level", level).apply()
        }
    }

    fun saveFontWeight(weight: String) {
        if (weight == "NORMAL" || weight == "BOLD") {
            _fontWeight.value = weight
            prefs.edit().putString("font_weight", weight).apply()
        }
    }

    // --- Customizable Notification Reminders ---
    fun toggleReminder(enabled: Boolean) {
        _reminderEnabled.value = enabled
        prefs.edit().putBoolean("reminder_enabled", enabled).apply()
    }

    fun saveReminderTime(time: String) {
        _reminderTime.value = time
        prefs.edit().putString("reminder_time", time).apply()
    }

    fun saveReminderFrequency(frequency: String) {
        _reminderFrequency.value = frequency
        prefs.edit().putString("reminder_freq", frequency).apply()
    }

    // --- Custom Category Management ---
    fun addNewCategory(name: String, type: String) {
        val cleanName = name.trim().replace(" ", "_")
        if (cleanName.isNotBlank()) {
            viewModelScope.launch {
                repository.insertCategory(CustomCategory(cleanName, type, false))
            }
        }
    }

    fun deleteCustomCategory(category: CustomCategory) {
        if (!category.isSystem) {
            viewModelScope.launch {
                repository.deleteCategory(category)
            }
        }
    }

    // --- Month actions ---
    fun selectMonth(monthId: String) {
        if (monthId.isNotBlank()) {
            activeMonthId.value = monthId.trim()
        }
    }

    fun addNewMonthBudget(monthId: String, baseIncome: Double, note: String = "") {
        viewModelScope.launch {
            repository.insertMonthBudget(MonthBudget(monthId = monthId.trim(), baseIncome = baseIncome, note = note))
            activeMonthId.value = monthId.trim()
        }
    }

    fun deleteMonthBudget(monthId: String) {
        viewModelScope.launch {
            repository.deleteMonthBudget(monthId)
            val remaining = repository.allMonthBudgets.first()
            if (remaining.isNotEmpty()) {
                activeMonthId.value = remaining.first().monthId
            } else {
                setupSampleData()
                activeMonthId.value = "2026-05"
            }
        }
    }

    // --- Transaction actions ---
    fun addTransaction(amount: Double, type: String, category: String, note: String, customTimestamp: Long? = null) {
        val monthId = activeMonthId.value
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    monthId = monthId,
                    amount = amount,
                    type = type,
                    category = category,
                    note = note,
                    timestamp = customTimestamp ?: System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun resetMonthData(monthId: String) {
        viewModelScope.launch {
            repository.getTransactionsForMonth(monthId).first().forEach { transaction ->
                repository.deleteTransaction(transaction)
            }
            repository.getMonthBudget(monthId)?.let { budget ->
                repository.insertMonthBudget(budget.copy(baseIncome = 0.0))
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            // Ensure there remains at least one active month for the UI to not break
            repository.insertMonthBudget(MonthBudget(activeMonthId.value, 0.0, "New Active Month"))
        }
    }

    // --- Loan methods ---
    fun addLoan(amount: Double, title: String) {
        val monthId = activeMonthId.value
        viewModelScope.launch {
            // 1. Insert Loan entity
            repository.insertLoan(
                com.example.data.Loan(
                    monthId = monthId,
                    amount = amount,
                    title = title
                )
            )
            // 2. Automatically link as EXTRA_INCOME to the income system
            repository.insertTransaction(
                Transaction(
                    monthId = monthId,
                    amount = amount,
                    type = "EXTRA_INCOME",
                    category = "LOAN",
                    note = title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteLoan(loan: com.example.data.Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            // Automatically find and delete the associated automatic transaction
            repository.getTransactionsForMonth(loan.monthId).firstOrNull()?.let { txs ->
                val associatedTx = txs.firstOrNull { 
                    it.type == "EXTRA_INCOME" && 
                    it.category.uppercase() == "LOAN" && 
                    it.amount == loan.amount && 
                    it.note == loan.title
                }
                if (associatedTx != null) {
                    repository.deleteTransaction(associatedTx)
                }
            }
        }
    }

    fun getPreviousMonthId(monthId: String): String {
        try {
            val parts = monthId.split("-")
            if (parts.size == 2) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                var prevYear = year
                var prevMonth = month - 1
                if (prevMonth == 0) {
                    prevMonth = 12
                    prevYear -= 1
                }
                return String.format(Locale.US, "%04d-%02d", prevYear, prevMonth)
            }
        } catch (e: Exception) {
            // ignore
        }
        return ""
    }

    fun setDateFilter(startTime: Long?, endTime: Long?) {
        selectedStartDate.value = startTime
        selectedEndDate.value = endTime
    }

    fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "%,.2f", amount)
    }

    fun formatDate(timestamp: Long, lang: AppLanguage): String {
        val sdf = if (lang == AppLanguage.AR) {
            SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        }
        return sdf.format(timestamp)
    }

    // --- Smart auto-categorization algorithm based on name/note description ---
    fun autoCategorizeDescription(note: String, isExpense: Boolean): String {
        val clean = note.lowercase().trim()
        if (isExpense) {
            return when {
                clean.contains("restaurant") || clean.contains("food") || clean.contains("burger") || clean.contains("grocery") || clean.contains("market") || clean.contains("أكل") || clean.contains("مطعم") || clean.contains("خضار") || clean.contains("بقالة") || clean.contains("سوبر") || clean.contains("قهوة") || clean.contains("coffee") || clean.contains("cafe") -> "Food"
                clean.contains("uber") || clean.contains("taxi") || clean.contains("car") || clean.contains("gas") || clean.contains("fuel") || clean.contains("بنزين") || clean.contains("تكسي") || clean.contains("سيارة") || clean.contains("مواصلات") || clean.contains("سفر") -> "Transport"
                clean.contains("rent") || clean.contains("flat") || clean.contains("apartment") || clean.contains("بيت") || clean.contains("شقة") || clean.contains("إيجار") || clean.contains("سكن") -> "Rent"
                clean.contains("internet") || clean.contains("electricity") || clean.contains("water") || clean.contains("mobil") || clean.contains("bill") || clean.contains("فاتورة") || clean.contains("انترنت") || clean.contains("كهرباء") || clean.contains("شحن") -> "Utilities"
                clean.contains("mall") || clean.contains("clothes") || clean.contains("shoes") || clean.contains("amazon") || clean.contains("ملابس") || clean.contains("تسوق") || clean.contains("أمازون") || clean.contains("شراء") -> "Shopping"
                clean.contains("cinema") || clean.contains("netflix") || clean.contains("match") || clean.contains("game") || clean.contains("رحلة") || clean.contains("ترفيه") || clean.contains("سينما") -> "Entertainment"
                clean.contains("pharmacy") || clean.contains("doctor") || clean.contains("medicine") || clean.contains("hospit") || clean.contains("علاج") || clean.contains("طبيب") || clean.contains("صيدلية") || clean.contains("مستشفى") -> "Medical"
                else -> "Other"
            }
        } else {
            return when {
                clean.contains("bonus") || clean.contains("incentiv") || clean.contains("مكافأة") || clean.contains("حافز") -> "Bonus"
                clean.contains("crypto") || clean.contains("stock") || clean.contains("divid") || clean.contains("أسهم") || clean.contains("استثمار") || clean.contains("أرباح") -> "Investment"
                clean.contains("gift") || clean.contains("present") || clean.contains("هدية") -> "Gift"
                clean.contains("freelance") || clean.contains("side") || clean.contains("contract") || clean.contains("تصميم") || clean.contains("برمجة") -> "Side_Project"
                clean.contains("salary") || clean.contains("job") || clean.contains("راتب") || clean.contains("شركة") -> "Salary"
                else -> "Other"
            }
        }
    }

    // --- Dynamic Savings Challenges Engine ---
    fun compileChallenges(lang: AppLanguage): List<SavingsChallenge> {
        val transactionsList = activeTransactions.value
        val budgetVal = activeMonthBudget.value
        val baseIncome = budgetVal?.baseIncome ?: 0.0

        val totalExp = transactionsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalInc = baseIncome + transactionsList.filter { it.type == "EXTRA_INCOME" }.sumOf { it.amount }

        val savingsRate = if (totalInc > 0) (((totalInc - totalExp) / totalInc) * 100.0).toFloat() else 0f
        val foodExp = transactionsList.filter { it.category == "Food" }.sumOf { it.amount }

        return listOf(
            SavingsChallenge(
                id = "economic_hero",
                titleAr = "بطل التوفير 🎖️",
                titleEn = "Savings Hero 🎖️",
                descAr = "ادخر أكثر من 20% من إجمالي دخلك هذا الشهر.",
                descEn = "Save more than 20% of your total income this month.",
                icon = "Savings",
                targetType = "SAVINGS_RATE",
                isCompleted = savingsRate >= 20f && totalInc > 0,
                progress = (savingsRate / 20f).coerceIn(0f, 1f)
            ),
            SavingsChallenge(
                id = "fast_food_diet",
                titleAr = "حمية الوجبات السريعة 🍔",
                titleEn = "Fast Food Diet 🍔",
                descAr = "حافظ على مصاريف طعامك وغذائك تحت 1000 ريال هذا الشهر.",
                descEn = "Keep food spending below 1,000 SR this month.",
                icon = "Restaurant",
                targetType = "REDUCTION",
                isCompleted = foodExp in 0.1..1000.0,
                progress = if (foodExp == 0.0) 0f else (1f - (foodExp / 1000f).toFloat()).coerceIn(0f, 1f)
            ),
            SavingsChallenge(
                id = "consistent_logger",
                titleAr = "المدون الملتزم 📝",
                titleEn = "Consistent Logger 📝",
                descAr = "سجل ما لا يقل عن 5 معاملات في شهر واحد.",
                descEn = "Log at least 5 transactions in a single month.",
                icon = "EditNote",
                targetType = "LOGS_COUNT",
                isCompleted = transactionsList.size >= 5,
                progress = (transactionsList.size / 5f).coerceIn(0f, 1f)
            ),
            SavingsChallenge(
                id = "first_step",
                titleAr = "الخطوة الأولى 🚀",
                titleEn = "First Step 🚀",
                descAr = "أنشئ وخطط الميزانية الأساسية للشهر النشط.",
                descEn = "Establish and set your base month budget.",
                icon = "CheckCircle",
                targetType = "BUDGET_SET",
                isCompleted = baseIncome > 0,
                progress = if (baseIncome > 0) 1f else 0f
            )
        )
    }

    // --- Predictive Expenses Algorithm - Predicts next month's spending based on history ---
    fun predictFutureMonthsSpending(lang: AppLanguage): String {
        val txs = allTransactions.value
        val monthlyGroups = txs.filter { it.type == "EXPENSE" }.groupBy { it.monthId }
        
        if (monthlyGroups.isEmpty()) {
            return if (lang == AppLanguage.AR) "لا تتوفر حركات كافية للتنبؤ بالمستقبل." else "Not enough history to predict spending yet."
        }

        val totalExpensesByMonth = monthlyGroups.map { it.value.sumOf { t -> t.amount } }
        val averageSpending = totalExpensesByMonth.average()
        val latestMonthExpenses = totalExpensesByMonth.firstOrNull() ?: averageSpending

        // Simple future projection including weights (70% last month, 30% historical average)
        val projectedSpending = (latestMonthExpenses * 0.7) + (averageSpending * 0.3)

        return if (lang == AppLanguage.AR) {
            "بناءً على نشاط صرفك خلال الأشهر الستة الماضية، نتوقع أن يبلغ إجمالي مصروفاتك الشهر القادم حوالي **${formatMoney(projectedSpending)} ريال**. ننصحك بالتحكم في الصرف لمنع التجاوز."
        } else {
            "Based on your activity over the past months, we predict your expenses next month will be approximately **${formatMoney(projectedSpending)} SR**. Consider adjusting your caps."
        }
    }

    // --- Bad Habit and Overspending Detection ---
    fun detectBadHabits(lang: AppLanguage): List<String> {
        val txs = activeTransactions.value
        val budgetVal = activeMonthBudget.value
        val baseIncome = budgetVal?.baseIncome ?: 0.0
        val totalExp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val alerts = mutableListOf<String>()

        if (totalExp > 0) {
            val foodRatio = txs.filter { it.category == "Food" }.sumOf { it.amount } / totalExp
            val shoppingRatio = txs.filter { it.category == "Shopping" }.sumOf { it.amount } / totalExp
            val entertainmentRatio = txs.filter { it.category == "Entertainment" }.sumOf { it.amount } / totalExp

            if (foodRatio > 0.35) {
                alerts.add(
                    if (lang == AppLanguage.AR) "⚠️ أكثر من 35% من نفقاتك تذهب على الطعام! فكر في الطهي المنزلي بدلاً من المطاعم المكررة."
                    else "⚠️ Over 35% of expenses is on Food! Cooking home meals can save substantial cash flow."
                )
            }
            if (shoppingRatio > 0.25) {
                alerts.add(
                    if (lang == AppLanguage.AR) "⚠️ قمت بصرف 25% من ميزانيتك على المشتريات والتسوق! حدد رغباتك الأساسية فقط."
                    else "⚠️ Shopping covers 25% of your costs. Seek postponing non-essential buyouts."
                )
            }
            if (entertainmentRatio > 0.20) {
                alerts.add(
                    if (lang == AppLanguage.AR) "⚠️ نفقات الترفيه تتجاوز 20%. حاول البحث عن بدائل مجانية للترفيه المالي."
                    else "⚠️ Entertainment represents over 20% of spending. Try choosing cheaper alternatives."
                )
            }
        }

        if (baseIncome > 0 && totalExp > baseIncome) {
            alerts.add(
                if (lang == AppLanguage.AR) "🚨 خطر! لقد تجاوزت نفقاتك دخلك الأساسي المقدر لهذا الشهر بنسبة تضخم خطيرة!"
                else "🚨 Danger! Total monthly spending has exceeded your core baseline income limit!"
            )
        }

        if (alerts.isEmpty()) {
            alerts.add(
                if (lang == AppLanguage.AR) "✅ ممتاز! عاداتك المالية منضبطة هذا الشهر ولا توجد تحذيرات غير معتادة."
                else "✅ Excellent! Your budgeting habits look fully stable with zero flags."
            )
        }

        return alerts
    }

    // --- Dynamic Insights and Financial Advice Generator ---
    fun generateFinancialInsights(lang: AppLanguage): List<String> {
        val txs = activeTransactions.value
        val budgetVal = activeMonthBudget.value
        val baseIncome = budgetVal?.baseIncome ?: 0.0

        val totalExpenses = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalExtraIncome = txs.filter { it.type == "EXTRA_INCOME" }.sumOf { it.amount }
        val totalRevenue = baseIncome + totalExtraIncome

        val savings = totalRevenue - totalExpenses
        val savingsPercent = if (totalRevenue > 0) (savings / totalRevenue) * 100 else 0.0

        val insights = mutableListOf<String>()

        if (lang == AppLanguage.AR) {
            insights.add("• نسبة ادخار هذا الشهر: **${String.format(Locale.US, "%.1f", savingsPercent)}%** (${formatMoney(savings)} ريال)")
            if (savingsPercent < 10.0) {
                insights.add("• نصيحة: ادخارك منخفض حالياً! حاول ترشيد نفقات فئة الكافيهات والمقاهي.")
            } else {
                insights.add("• رائع: ادخارك في نطاق آمن ومثالي! مبروك التزامك بالخطة الادخارية.")
            }
            if (totalExtraIncome > 0) {
                insights.add("• فخر: مصدر دخلك الإضافي (${formatMoney(totalExtraIncome)} ريال) ساعد كثيراً في رفع الميزانية الإيجابية.")
            }
        } else {
            insights.add("• Monthly Savings Rate: **${String.format(Locale.US, "%.1f", savingsPercent)}%** (${formatMoney(savings)} SR)")
            if (savingsPercent < 10.0) {
                insights.add("• Tip: Savings rate is low! Reduce impulse shopping during weekend events.")
            } else {
                insights.add("• Success: You are within healthy and highly optimal savings ratios. Keeping it up!")
            }
            if (totalExtraIncome > 0) {
                insights.add("• Side Income: Your extra gigs contributed ${formatMoney(totalExtraIncome)} SR to your surplus.")
            }
        }

        return insights
    }

    // --- AI Smart Gemini Assistant Call & Conversation ---
    fun sendMessageToAI(question: String) {
        if (question.isBlank()) return

        // Append user massage
        val userMsg = ChatMessage("USER", question)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            // Context/System instructions
            val txsList = activeTransactions.value
            val budgetVal = activeMonthBudget.value
            val baseIncome = budgetVal?.baseIncome ?: 0.0
            val totalExpense = txsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val categoriesSpent = txsList.filter { it.type == "EXPENSE" }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { t -> t.amount } }

            val systemContext = """
                You are "Masroofy AI" - an expert friendly personal finance copilot integrated in an Android app.
                The user asks financial advice or questions about their current personal budget data.
                Respond concisely and professionally in the requested language (Arabic or English).
                Current user data context:
                - Target month: ${activeMonthId.value}
                - Base income: $baseIncome SR
                - Active transactions count: ${txsList.size}
                - Total expense logged: $totalExpense SR
                - Spending by category: $categoriesSpent
                Give direct actionable tips without showing engineering raw JSON variables.
            """.trimIndent()

            // 1. Call real API via OkHttp REST if key exists
            val response = callGeminiApi(question, systemContext)

            // 2. Fallback to precise local intelligence engine if API is offline or has no key
            val finalReply = if (response.startsWith("Error:") || response == "API KEY MISSING") {
                // Return fallback answers
                getFallbackAIAnswer(question, _currentLanguage.value)
            } else {
                response
            }

            _chatMessages.value = _chatMessages.value + ChatMessage("AI", finalReply)
            _isChatLoading.value = false
        }
    }

    private suspend fun callGeminiApi(prompt: String, systemInstruction: String): String {
        return withContext(Dispatchers.IO) {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
                return@withContext "API KEY MISSING"
            }
            try {
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                // Clean-up inputs safely to build clean raw JSON body
                val escapedPrompt = org.json.JSONObject.quote(prompt)
                val escapedSystem = org.json.JSONObject.quote(systemInstruction)

                val requestBodyString = """
                    {
                        "contents": [{
                            "parts": [{"text": $escapedPrompt}]
                        }],
                        "systemInstruction": {
                            "parts": [{"text": $escapedSystem}]
                        }
                    }
                """.trimIndent()

                val requestBody = okhttp3.RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    requestBodyString
                )

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key")
                    .post(requestBody)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext "Error: status code ${response.code}"
                    }
                    val bodyString = response.body?.string() ?: ""
                    val jsonObject = org.json.JSONObject(bodyString)
                    val candidates = jsonObject.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")
                    text ?: "No valid response format."
                }
            } catch (e: Exception) {
                "Error: ${e.localizedMessage}"
            }
        }
    }

    private fun getFallbackAIAnswer(question: String, lang: AppLanguage): String {
        val q = question.lowercase().trim()
        val isAr = lang == AppLanguage.AR
        return when {
            q.contains("تنبؤ") || q.contains("predict") || q.contains("توقع") || q.contains("توقعات") -> {
                predictFutureMonthsSpending(lang)
            }
            q.contains("عادة") || q.contains("عادات") || q.contains("habit") || q.contains("مشاكل") -> {
                detectBadHabits(lang).joinToString("\n")
            }
            q.contains("نصيحة") || q.contains("insight") || q.contains("نصائح") || q.contains("ادخار") || q.contains("save") -> {
                generateFinancialInsights(lang).joinToString("\n") + "\n" + 
                (if (isAr) "نصيحة سريعة: التزم بقاعدة التوفير 50-30-20!" else "Pro-Tip: Standard guidelines advise adopting the 50-30-20 rule!")
            }
            else -> {
                if (isAr) {
                    "أهلاً بك! لقد تفاعلت معي دون اتصال بالإنترنت (أو مفتاح API مفقود). ها هي بعض النقاط المفيدة حول ميزانيتك:\n\n" +
                    generateFinancialInsights(lang).joinToString("\n") + "\n\n" +
                    "يمكنك سؤالي عن 'توقعات' أو 'عادات' لنقاش مالي سريع ودقيق!"
                } else {
                    "Greetings! Currently answering in Offline/Fallback Mode with no network keys. Here are some pointers based on custom data analytics:\n\n" +
                    generateFinancialInsights(lang).joinToString("\n") + "\n\n" +
                    "Ask for 'predictions' or 'habits' to prompt deep budgeting checks!"
                }
            }
        }
    }

    // --- High Fidelity Data Export & Backup as CSV & Restore System ---
    fun exportToCSV(): String {
        val budgetVal = activeMonthBudget.value
        val txs = activeTransactions.value

        val builder = java.lang.StringBuilder()
        builder.append("DATA_TYPE,ID,AMOUNT,CATEGORY,NOTE,TIMESTAMP\n")

        // 1. Export active Budget info
        if (budgetVal != null) {
            builder.append("BUDGET,${budgetVal.monthId},${budgetVal.baseIncome},None,${budgetVal.note},0\n")
        }

        // 2. Export active transactions
        txs.forEach { t ->
            builder.append("TRANSACTION,${t.id},${t.amount},${t.category},${t.note},${t.timestamp}\n")
        }

        return builder.toString()
    }

    fun restoreFromCSV(csvData: String): Boolean {
        if (csvData.isBlank() || !csvData.contains("DATA_TYPE")) return false
        try {
            val lines = csvData.split("\n")
            viewModelScope.launch {
                lines.forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 6) {
                        val dataType = parts[0].trim()
                        if (dataType == "BUDGET") {
                            val monthId = parts[1].trim()
                            val baseIncome = parts[2].trim().toDoubleOrNull() ?: 0.0
                            val note = parts[4].trim()
                            repository.insertMonthBudget(MonthBudget(monthId, baseIncome, note))
                        } else if (dataType == "TRANSACTION") {
                            val amount = parts[2].trim().toDoubleOrNull() ?: 0.0
                            val category = parts[3].trim()
                            val note = parts[4].trim()
                            val timestamp = parts[5].trim().toLongOrNull() ?: System.currentTimeMillis()
                            repository.insertTransaction(
                                Transaction(
                                    monthId = activeMonthId.value,
                                    amount = amount,
                                    type = if (category == "Bonus" || category == "Investment" || category == "Gift" || category == "Side_Project" || category == "Salary") "EXTRA_INCOME" else "EXPENSE",
                                    category = category,
                                    note = note,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }
}

class FinanceViewModelFactory(
    private val repository: FinanceRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
