package `in`.gym.trak.studio.features.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.PaymentModeSelector
import `in`.gym.trak.studio.data.model.CreateExpenseRequest
import `in`.gym.trak.studio.data.model.UpdateExpenseRequest
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.gymId
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_clock
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.TrainerDTO
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private data class ExpenseFieldErrors(
    val name: String? = null,
    val category: String? = null,
    val date: String? = null,
    val amount: String? = null,
    val gst: String? = null,
    val trainer: String? = null
)

private fun validateExpenseForm(
    expenseName: String,
    selectedCategory: String,
    paymentDate: String,
    expenseAmount: String,
    gstPercentage: String,
    selectedTrainer: TrainerDTO? = null,
    isSalary: Boolean = false
): ExpenseFieldErrors {
    val amountValue = expenseAmount.toDoubleOrNull()
    val gstValue = gstPercentage.takeIf { it.isNotBlank() }?.toDoubleOrNull()

    return ExpenseFieldErrors(
        name = when {
            expenseName.isBlank() -> "Please enter expense name."
            expenseName.trim().length < 2 -> "Expense name must be at least 2 characters."
            else -> null
        },
        category = if (selectedCategory.isBlank()) "Please select a category." else null,
        date = if (paymentDate.isBlank()) "Please select expense date." else null,
        amount = when {
            expenseAmount.isBlank() -> "Please enter amount."
            amountValue == null -> "Enter a valid amount."
            amountValue <= 0.0 -> "Amount should be greater than 0."
            else -> null
        },
        gst = when {
            gstPercentage.isBlank() -> null
            gstValue == null -> "Enter a valid GST percentage."
            gstValue < 0.0 || gstValue > 100.0 -> "GST should be between 0 and 100."
            else -> null
        },
        trainer = if (isSalary && selectedTrainer == null) "Please select a trainer or staff." else null
    )
}

private val ExpenseFieldErrors.hasAnyError: Boolean
    get() = name != null || category != null || date != null || amount != null || gst != null || trainer != null

private fun apiCategoryToUi(raw: String?): String {
    val normalized = raw.orEmpty().trim().replace('_', ' ').lowercase()
    return normalized.split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        .ifBlank { "Rent" }
}

private fun uiCategoryToApi(raw: String): String = raw.trim().replace(' ', '_').uppercase()

private fun apiPaymentModeToUi(raw: String?): String {
    return when (raw.orEmpty().trim().lowercase()) {
        "upi" -> "UPI"
        "cash" -> "Cash"
        else -> raw.orEmpty().trim().replaceFirstChar { it.uppercase() }.ifBlank { "Cash" }
    }
}

private fun uiPaymentModeToApi(raw: String): String = raw.trim().lowercase()

/**
 * Screen for adding a new gym expense (Image 3).
 */
class AddExpenseScreen(val expenseId: String? = null, val onRefresh: (() -> Unit)? = null) :
    Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val focusManager = LocalFocusManager.current

        var expenseName by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("Rent") }
        var paymentDate by remember { mutableStateOf(DateUtils.getCurrentDateIso()) }
        var showDatePicker by remember { mutableStateOf(false) }
        var expenseAmount by remember { mutableStateOf("") }
        var selectedMode by remember { mutableStateOf("cash") }
        var gstPercentage by remember { mutableStateOf("0") }
        var fieldErrors by remember { mutableStateOf(ExpenseFieldErrors()) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var isEditMode = expenseId != null

        var showTrainerBottomSheet by remember { mutableStateOf(false) }
        var selectedSalaryType by remember { mutableStateOf(GymStaffListRole.TRAINER) }
        var selectedTrainer by remember { mutableStateOf<TrainerDTO?>(null) }
        val trainers by screenModel.trainers.collectAsState()
        val trainersListLoading by screenModel.trainersListLoading.collectAsState()

        LaunchedEffect(expenseId) {
            if (isEditMode) {
                screenModel.getExpenseById(expenseId!!) { expense ->
                    expense?.let {
                        expenseName = it.bill_name ?: ""
                        selectedCategory = apiCategoryToUi(it.category)
                        paymentDate = it.date ?: DateUtils.getCurrentDateIso()
                        expenseAmount = it.amount?.toString() ?: "0.0"
                        selectedMode = apiPaymentModeToUi(it.payment_mode)
                        gstPercentage = it.gst?.toString() ?: ""
                        fieldErrors = ExpenseFieldErrors()

                    }
                }
            }
        }

        LaunchedEffect(showTrainerBottomSheet, selectedSalaryType) {
            if (!showTrainerBottomSheet) return@LaunchedEffect
            screenModel.setTrainersListRole(selectedSalaryType)
            screenModel.loadTrainers(showGlobalLoader = false)
        }

        val datePickerState = rememberDatePickerState()

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val instant = Instant.fromEpochMilliseconds(it)
                            paymentDate = instant.toString()
                            fieldErrors = fieldErrors.copy(date = null)
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Expense") },
                text = { Text("Are you sure you want to delete this expense?") },
                confirmButton = {
                    TextButton(onClick = {
                        screenModel.deleteExpense(expenseId!!) {
                            onRefresh?.invoke()
                            navigator?.pop()
                        }
                    }) { Text("Delete", color = Color.Red.copy(alpha = 0.8f)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }

        Scaffold(
            topBar = {
//                CenterAlignedTopAppBar(
//                    title = {
//                        Text(
//                            text = "Add Expense",
//                            style = AppTextTheme.bold.copy(fontSize = 18.sp)
//                        )
//                    },
//                    navigationIcon = {
//                        IconButton(onClick = { navigator?.pop() }) {
//                            Icon(
//                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                contentDescription = "Back"
//                            )
//                        }
//                    },
//                    actions = {
//                        if (isEditMode) {
//                            IconButton(onClick = { showDeleteDialog = true }) {
//                                Icon(
//                                    imageVector = Icons.Default.Delete,
//                                    contentDescription = "Delete",
//                                    tint = Color.Red
//                                )
//                            }
//                        }
//                    },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = Color.Transparent
//                    )
//                )
                GymAppBar(
                    title = "Add Expense",
                    onBackClick = { navigator?.pop() },
                    actions =
                        {
                            if (isEditMode) {
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
                dismissKeyboardOnTap = true,
            ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Name",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonTextField(
                            keyboardOptions = KeyboardOptions.Default.copy(KeyboardCapitalization.Words),
                            value = expenseName,
                            onValueChange = {
                                expenseName = it
                                fieldErrors = fieldErrors.copy(name = null)
                            },
                            placeholder = "eg. Electricity Bill",
                            errorText = fieldErrors.name
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Category",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonDropdown(
                            options = listOf(
                                "Rent",
                                "Utilities",
                                "Equipment",
                                "Maintenance",
                                "Supplies",
                                "Salary",
                                "Marketing",
                                "Software",
                                "Other"
                            ),
                            selectedOption = selectedCategory,
                            onOptionSelected = {
                                selectedCategory = it
                                fieldErrors = fieldErrors.copy(category = null)
                            },
                            placeholder = "Select Category",
                            errorText = fieldErrors.category
                        )

                        if (selectedCategory == "Salary") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Trainer / Staff",
                                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.clickable { showTrainerBottomSheet = true }) {
                                CommonTextField(
                                    value = selectedTrainer?.fullName ?: "",
                                    onValueChange = {},
                                    placeholder = "Select Trainer/Staff",
                                    readOnly = true,
                                    enabled = false,
                                    errorText = fieldErrors.trainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Date",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.clickable { showDatePicker = true }) {
                            CommonTextField(
                                value = try {
                                    val instant = Instant.parse(paymentDate)
                                    val localDateTime =
                                        instant.toLocalDateTime(TimeZone.currentSystemDefault())
                                    "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}/${localDateTime.year}"
                                } catch (e: Exception) {
                                    paymentDate
                                },
                                onValueChange = {},
                                placeholder = "Select Date",
                                readOnly = true,
                                enabled = false,
                                leadingIconDrawable = Res.drawable.ic_clock,
                                errorText = fieldErrors.date
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Amount",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonTextField(
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            value = expenseAmount,
                            onValueChange = {
                                expenseAmount = it
                                fieldErrors = fieldErrors.copy(amount = null)
                            },
                            placeholder = "Amount",
                            errorText = fieldErrors.amount
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Payment Mode",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PaymentModeSelector(
                            selectedOption = selectedMode,
                            onOptionSelected = { selectedMode = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "GST%",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonTextField(
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            value = gstPercentage,
                            onValueChange = {
                                gstPercentage = it
                                fieldErrors = fieldErrors.copy(gst = null)
                            },
                            placeholder = "eg. 18",
                            errorText = fieldErrors.gst
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    if (hasPermission(
                            SessionManager.PermissionKeys.KEY_EXPENSE_UPDATE,
                            SessionManager.PermissionKeys.KEY_EXPENSE_CREATE
                        )
                    )
                        CommonButton(
                            onClick = {
                                val errors = validateExpenseForm(
                                    expenseName = expenseName,
                                    selectedCategory = selectedCategory,
                                    paymentDate = paymentDate,
                                    expenseAmount = expenseAmount,
                                    gstPercentage = gstPercentage,
                                    selectedTrainer = selectedTrainer,
                                    isSalary = selectedCategory == "Salary"
                                )
                                fieldErrors = errors
                                if (errors.hasAnyError) return@CommonButton

//                            if (selectedCategory == "Salary") {
//                                // For Salary category, only validation for now
//                                screenModel.showError("Salary payments will be implemented soon.")
//                                return@CommonButton
//                            }

                                val amountVal = expenseAmount.toDoubleOrNull() ?: 0.0

                                if (isEditMode) {
                                    val request = UpdateExpenseRequest(
                                        title = expenseName.trim(),
                                        description = expenseName.trim(),
                                        trainer_id = selectedTrainer?.gymUserId,
                                        category = uiCategoryToApi(selectedCategory),
                                        date = paymentDate,
                                        amount = amountVal,
                                        payment_mode = uiPaymentModeToApi(selectedMode)
                                    )
                                    screenModel.updateExpense(expenseId!!, request) {
                                        onRefresh?.invoke()
                                        navigator?.pop()
                                    }
                                } else {
                                    val request = CreateExpenseRequest(
                                        gymId = gymId,
                                        trainer_id = selectedTrainer?.gymUserId,
                                        bill_name = expenseName.trim(),
                                        category = uiCategoryToApi(selectedCategory),
                                        date = paymentDate,
                                        payment_mode = uiPaymentModeToApi(selectedMode),
                                        gst = gstPercentage.toDoubleOrNull() ?: 0.0,
                                        amount = amountVal,
                                    )
                                    screenModel.createExpense(request) {
                                        onRefresh?.invoke()
                                        navigator?.pop()
                                    }
                                }
                            },
                            text = if (isEditMode) "Update Expense" else "Save Expense",
                            modifier = Modifier.fillMaxWidth()
                        )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (showTrainerBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        if (!trainersListLoading) showTrainerBottomSheet = false
                    },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 8.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFE2E8F0))
                        )
                    }
                ) {
                    TrainerStaffPickerSheetContent(
                        trainers = trainers,
                        trainersListLoading = trainersListLoading,
                        selectedSalaryType = selectedSalaryType,
                        onSalaryTypeChange = { role ->
                            selectedTrainer = null
                            selectedSalaryType = role
                        },
                        onTrainerSelected = { trainer ->
                            selectedTrainer = trainer
                            fieldErrors = fieldErrors.copy(trainer = null)
                            showTrainerBottomSheet = false
                        }
                    )
                }
            }
    }

}

@Composable
private fun TrainerStaffPickerSheetContent(
    trainers: List<TrainerDTO>,
    trainersListLoading: Boolean,
    selectedSalaryType: String,
    onSalaryTypeChange: (String) -> Unit,
    onTrainerSelected: (TrainerDTO) -> Unit
) {
    val emptyMessage = if (selectedSalaryType == GymStaffListRole.STAFF) {
        "No staff found"
    } else {
        "No trainers found"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Select Trainer/Staff",
                style = AppTextTheme.semiBold.copy(fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    GymStaffListRole.TRAINER to "Trainer",
                    GymStaffListRole.STAFF to "Staff"
                ).forEach { (role, label) ->
                    val isSelected = selectedSalaryType == role
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable(enabled = !trainersListLoading) {
                                if (selectedSalaryType != role) onSalaryTypeChange(role)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryColor else Color(0xFFF1F5F9),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFE2E8F0)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = AppTextTheme.medium.copy(
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color.White else Black
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
            ) {
                when {
                    trainers.isEmpty() && !trainersListLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emptyMessage,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    trainers.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = !trainersListLoading
                        ) {
                            items(trainers, key = { it.gymUserId }) { trainer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !trainersListLoading) {
                                            onTrainerSelected(trainer)
                                        }
                                        .padding(vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trainer.fullName.ifBlank { "Unknown" },
                                        style = AppTextTheme.medium.copy(fontSize = 16.sp)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }

        if (trainersListLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedSalaryType == GymStaffListRole.STAFF) {
                            "Loading staff…"
                        } else {
                            "Loading trainers…"
                        },
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                    )
                }
            }
        }
    }
}
