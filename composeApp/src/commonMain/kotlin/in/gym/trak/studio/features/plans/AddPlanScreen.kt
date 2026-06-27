package `in`.gym.trak.studio.features.plans

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.DaySelector
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.format24hStringToAmPm
import `in`.gym.trak.studio.components.normalizeTimeTo24hString
import `in`.gym.trak.studio.data.model.BatchDetailsRequest
import `in`.gym.trak.studio.data.model.BatchShiftTime
import `in`.gym.trak.studio.data.model.CreatePlanCompatRequest
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.features.trainers.AddShiftDaySelection
import `in`.gym.trak.studio.features.trainers.AddShiftDialog
import `in`.gym.trak.studio.features.trainers.ShiftData
import `in`.gym.trak.studio.features.trainers.ShiftListItem
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_female
import gym.composeapp.generated.resources.ic_free_trial
import gym.composeapp.generated.resources.ic_male
import gym.composeapp.generated.resources.ic_other
import gym.composeapp.generated.resources.ic_pt_plan
import gym.composeapp.generated.resources.ic_subscriber
import gym.composeapp.generated.resources.ic_total_active_plan
import `in`.gym.trak.studio.features.trainers.AddTrainerScreen
import org.jetbrains.compose.resources.painterResource

/**
 * Creates or edits a gym plan: [GYM_MEMBERSHIP] (includes free trial), [PT_PLAN], or [BATCH_PLAN].
 * When [existingPlanId] is set, the form is filled from [OwnerDashboardScreenModel.getPlanDetail] and save calls the update API.
 * Batch timing uses the same shift dialog / list pattern as [gym.trak.studio.features.trainers.EditTrainerScreen].
 */
import kotlin.jvm.Transient

class AddPlanScreen(
    private val existingPlanId: String? = null,
    private val preselectedTrainerId: String? = null,
    private val limitPlanTypes: Boolean = false,
    @Transient private val onPlanSaved: (() -> Unit)? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val planDetail by screenModel.planDetail.collectAsState()

        var planName by remember { mutableStateOf("") }
        var planDuration by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var selectedPlanType by remember { mutableStateOf(if (limitPlanTypes) "PT Plan" else "Gym Member") }

        val focusManager = LocalFocusManager.current

        val trainers by screenModel.trainers.collectAsState()
        var selectedTrainerId by remember(preselectedTrainerId) { mutableStateOf(preselectedTrainerId) }

        data class FormErrors(
            val planName: String? = null,
            val planDuration: String? = null,
            val price: String? = null,
            val trainer: String? = null,
            val workingDays: String? = null,
            val batchShift: String? = null
        )
        var fieldErrors by remember { mutableStateOf(FormErrors()) }

        var selectedGender by remember { mutableStateOf("Unisex") }
        var selectedDays by remember { mutableStateOf(setOf<String>()) }

        var batchShifts by remember { mutableStateOf(listOf<ShiftData>()) }
        var editingBatchShiftIndex by remember { mutableIntStateOf(-1) }
        var showAddBatchShiftSheet by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            screenModel.loadTrainers()
        }

        val editingPlanId = existingPlanId?.takeIf { it.isNotBlank() }
        var didApplyRemotePlan by remember(editingPlanId) { mutableStateOf(false) }

        LaunchedEffect(editingPlanId) {
            didApplyRemotePlan = false
            if (editingPlanId != null) {
                screenModel.getPlanDetail(editingPlanId)
            }
        }

//        val dayKeys = listOf("M0", "T1", "W2", "T3", "F4", "S5", "S6")
//        fun workingDaysInts(): List<Int> =
//            selectedDays.mapNotNull { key -> dayKeys.indexOf(key).takeIf { it >= 0 }?.plus(1) }.sorted()

        val dayKeys = listOf("M0", "T1", "W2", "T3", "F4", "S5", "S6")

        fun workingDaysInts(): List<Int> =
            selectedDays.mapNotNull { key ->
                dayKeys.indexOf(key).takeIf { it >= 0 }
            }.sorted()

        fun planTypeApi(): String = when (selectedPlanType) {
            "Gym Member", "Free Trial" -> "GYM_MEMBERSHIP"
            "PT Plan" -> "PT_PLAN"
            "Batch Plan" -> "BATCH_PLAN"
            else -> "GYM_MEMBERSHIP"
        }

        fun genderApi(): String = when (selectedGender) {
            "Male" -> "male"
            "Female" -> "female"
            else -> "unisex"
        }

        fun uiPlanTypeFromApi(type: String): String = when (type) {
            "PT_PLAN" -> "PT Plan"
            "BATCH_PLAN" -> "Batch Plan"
            "FREE_TRIAL" -> "Free Trial"
            else -> "Gym Member"
        }

        fun validateForm(): FormErrors {
            var errors = FormErrors()
            if (planName.trim().length < 2) {
                errors = errors.copy(planName = "Please enter a valid plan name.")
            }
            val duration = planDuration.toIntOrNull()
            if (duration == null || duration <= 0) {
                errors = errors.copy(planDuration = "Please enter a valid plan duration in days.")
            }
            val priceVal = price.toDoubleOrNull()
            if (priceVal == null || priceVal < 0.0) {
                errors = errors.copy(price = "Please enter a valid plan price.")
            }
            val needsTrainer = selectedPlanType == "PT Plan" || selectedPlanType == "Batch Plan"
            if (needsTrainer && selectedTrainerId.isNullOrBlank()) {
                errors = errors.copy(trainer = "Please assign a trainer for this plan.")
            }
            if (selectedPlanType == "Batch Plan") {
                if (workingDaysInts().isEmpty()) {
                    errors = errors.copy(workingDays = "Please select at least one working day for the batch.")
                }
                if (batchShifts.isEmpty()) {
                    errors = errors.copy(batchShift = "Please add at least one batch shift.")
                }
                val hasInvalidShift = batchShifts.any { shift ->
                    val parts = shift.time.split(" to ").map { it.trim() }
                    val start24 = parts.getOrNull(0)
                    val end24 = parts.getOrNull(1)
                    start24.isNullOrBlank() || end24.isNullOrBlank()
                }
                if (hasInvalidShift) {
                    errors = errors.copy(batchShift = "One or more shifts are invalid. Please review batch timings.")
                }
            }
            return errors
        }

        LaunchedEffect(planDetail?.id, editingPlanId, trainers.size, didApplyRemotePlan) {
            val id = editingPlanId ?: return@LaunchedEffect
            val p = planDetail ?: return@LaunchedEffect
            if (p.id != id || didApplyRemotePlan) return@LaunchedEffect
            if (p.trainer != null && trainers.isEmpty()) return@LaunchedEffect

            planName = p.name
            planDuration = p.durationDays.toString()
            val priceDouble = p.priceCents / 100.0
            price = if (priceDouble % 1.0 == 0.0) priceDouble.toInt().toString() else priceDouble.toString()
            selectedPlanType = uiPlanTypeFromApi(p.type)
            selectedTrainerId = p.trainer?.gymUserId

            p.batch?.let { batch ->
                selectedGender = when (batch.gender.lowercase()) {
                    "male" -> "Male"
                    "female" -> "Female"
                    else -> "Unisex"
                }
                selectedDays = batch.daysOfWeek.mapNotNull { d ->
                    when {
                        d in 0..6 -> dayKeys.getOrNull(d)
                        d in 1..7 -> dayKeys.getOrNull(d - 1)
                        else -> null
                    }
                }.toSet()
                batchShifts = batch.shifts.sortedBy { it.sortOrder }.mapIndexed { idx, s ->
                    val start = normalizeTimeTo24hString(s.startTime)
                    val end = normalizeTimeTo24hString(s.endTime)
                    ShiftData(
                        title = "Shift ${idx + 1}",
                        time = "$start to $end",
                        dayOfWeek = 1
                    )
                }
            }

            didApplyRemotePlan = true
        }

        val showBatchShiftDialog = showAddBatchShiftSheet || editingBatchShiftIndex >= 0
        if (showBatchShiftDialog && selectedPlanType == "Batch Plan") {
            val shiftToEdit = editingBatchShiftIndex.takeIf { it >= 0 }?.let { batchShifts.getOrNull(it) }
            val initialStart = shiftToEdit?.time?.split(" to ")?.getOrNull(0)?.trim() ?: "08:00"
            val initialEnd = shiftToEdit?.time?.split(" to ")?.getOrNull(1)?.trim() ?: "09:00"
            AddShiftDialog(
                onDismiss = {
                    showAddBatchShiftSheet = false
                    editingBatchShiftIndex = -1
                },
                initialDays = emptyList(),
                initialStartTime = initialStart,
                initialEndTime = initialEnd,
                isEdit = shiftToEdit != null,
                daySelection = AddShiftDaySelection.None,
                onAddShift = { title, start, end, _ ->
                    val label = title.ifBlank { "Shift" }
                    val newShift = ShiftData(label, "$start to $end", 1)
                    if (editingBatchShiftIndex >= 0) {
                        val idx = editingBatchShiftIndex
                        batchShifts = batchShifts.toMutableList().apply { this[idx] = newShift }
                    } else {
                        batchShifts = batchShifts + newShift
                    }
                    fieldErrors = fieldErrors.copy(batchShift = null)
                    showAddBatchShiftSheet = false
                    editingBatchShiftIndex = -1
                }
            )
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = if (editingPlanId != null) "Edit Plan" else "Add New Plan",
                                style = AppTextTheme.bold.copy(fontSize = 18.sp)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navigator?.pop() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    dismissKeyboardOnTap = true,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Plan Type",
                        style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val availableTypes = buildList {
                        if (!limitPlanTypes) add("Gym Member" to Res.drawable.ic_total_active_plan)
                        add("PT Plan" to Res.drawable.ic_pt_plan)
                        add("Batch Plan" to Res.drawable.ic_subscriber)
                        if (!limitPlanTypes) add("Free Trial" to Res.drawable.ic_free_trial)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (i in availableTypes.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val first = availableTypes[i]
                                PlanTypeCard(
                                    first.first,
                                    first.second,
                                    selectedPlanType == first.first,
                                    {
                                        selectedPlanType = first.first
                                        fieldErrors = fieldErrors.copy(trainer = null, workingDays = null, batchShift = null)
                                    },
                                    Modifier.weight(1f),
                                    enabled = editingPlanId == null
                                )
                                if (i + 1 < availableTypes.size) {
                                    val second = availableTypes[i + 1]
                                    PlanTypeCard(
                                        second.first,
                                        second.second,
                                        selectedPlanType == second.first,
                                        {
                                            selectedPlanType = second.first
                                            fieldErrors = fieldErrors.copy(trainer = null, workingDays = null, batchShift = null)
                                        },
                                        Modifier.weight(1f),
                                        enabled = editingPlanId == null
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Plan Details",
                        style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Plan name",
                        style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CommonTextField(
                        value = planName,
                        onValueChange = {
                            planName = it
                            fieldErrors = fieldErrors.copy(planName = null)
                        },
                        placeholder = "Enter Your Plan Name",
                        errorText = fieldErrors.planName
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plan Duration",
                                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CommonTextField(
                                value = planDuration,
                                onValueChange = {
                                    planDuration = it
                                    fieldErrors = fieldErrors.copy(planDuration = null)
                                },
                                placeholder = "Duration (Days)",
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                ),
                                errorText = fieldErrors.planDuration
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Price",
                                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CommonTextField(
                                value = price,
                                onValueChange = {
                                    price = it
                                    fieldErrors = fieldErrors.copy(price = null)
                                },
                                placeholder = "${Constants.RUPEE} Price",
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                errorText = fieldErrors.price
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (selectedPlanType == "PT Plan" || selectedPlanType == "Batch Plan") {
                        Text(
                            text = "Select Trainer",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (trainers.isEmpty()) {
                            CommonOutlineButton(
                                onClick = {
                                    navigator?.push(
                                        AddTrainerScreen(
                                            onTrainerAdded = {
                                                screenModel.loadTrainers()
                                            }
                                        )
                                    )
                                },
                                text = "Add Trainer",
                                modifier = Modifier.fillMaxWidth(),
                                textColor = Black
                            )
                            fieldErrors.trainer?.let {
                                Text(
                                    text = it,
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Color.Red),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            val trainerNames = trainers.map { it.fullName }
                            val selectedTrainerName =
                                trainers.find { it.gymUserId == selectedTrainerId }?.fullName ?: ""

                            CommonDropdown(
                                options = trainerNames,
                                selectedOption = selectedTrainerName,
                                onOptionSelected = { name ->
                                    selectedTrainerId = trainers.find { it.fullName == name }?.gymUserId
                                    fieldErrors = fieldErrors.copy(trainer = null)
                                },
                                placeholder = "Assign Trainer",
                                errorText = fieldErrors.trainer
                            )
                        }
                    }

                    if (selectedPlanType == "Batch Plan") {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Batch Plan",
                            style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            GenderSelectionCard(
                                label = "Male",
                                icon = Res.drawable.ic_male,
                                isSelected = selectedGender == "Male",
                                onClick = { selectedGender = "Male" },
                                modifier = Modifier.weight(1f)
                            )
                            GenderSelectionCard(
                                label = "Female",
                                icon = Res.drawable.ic_female,
                                isSelected = selectedGender == "Female",
                                onClick = { selectedGender = "Female" },
                                modifier = Modifier.weight(1f)
                            )
                            GenderSelectionCard(
                                label = "Unisex",
                                icon = Res.drawable.ic_other,
                                isSelected = selectedGender == "Unisex",
                                onClick = { selectedGender = "Unisex" },
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Working Days",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DaySelector(
                            selectedDays = selectedDays,
                            onDayToggle = { day ->
                                selectedDays =
                                    if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                                fieldErrors = fieldErrors.copy(workingDays = null)
                            }
                        )
                        fieldErrors.workingDays?.let { dayError ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayError,
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Color.Red)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Batch timing",
                                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        batchShifts.forEachIndexed { index, shift ->
                            ShiftListItem(
                                shift = shift,
                                suppressDayLabel = true,
                                onDelete = {
                                    batchShifts = batchShifts.toMutableList().apply { removeAt(index) }
                                    if (batchShifts.isNotEmpty()) {
                                        fieldErrors = fieldErrors.copy(batchShift = null)
                                    }
                                },
                                onEdit = { editingBatchShiftIndex = index }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .drawBehind {
                                    drawRoundRect(
                                        color = Gray.copy(alpha = 0.5f),
                                        style = Stroke(
                                            width = 3f,
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(15f, 15f),
                                                0f
                                            )
                                        ),
                                        cornerRadius = CornerRadius(100.dp.toPx())
                                    )
                                }
                                .clickable { showAddBatchShiftSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add Shift",
                                style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                            )
                        }
                        fieldErrors.batchShift?.let { shiftError ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = shiftError,
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Color.Red)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CommonOutlineButton(
                            onClick = { navigator?.pop() },
                            text = "Cancel",
                            modifier = Modifier.weight(1f),
                            textColor = Black
                        )
                        if (hasPermission(
                                SessionManager.PermissionKeys.KEY_PLAN_CREATE,
                                SessionManager.PermissionKeys.KEY_PLAN_UPDATE
                            )
                        ) {
                            CommonButton(
                                onClick = {
                                    fieldErrors = validateForm()
                                    if (
                                        fieldErrors.planName != null ||
                                        fieldErrors.planDuration != null ||
                                        fieldErrors.price != null ||
                                        fieldErrors.trainer != null ||
                                        fieldErrors.workingDays != null ||
                                        fieldErrors.batchShift != null
                                    ) return@CommonButton
                                    val duration = planDuration.toIntOrNull() ?: return@CommonButton
                                    val priceVal = price.toDoubleOrNull() ?: return@CommonButton
                                    val needsTrainer = selectedPlanType == "PT Plan" || selectedPlanType == "Batch Plan"

                                    val gymId = SessionManager.gymId
                                    val batchDetails = if (selectedPlanType == "Batch Plan") {
                                        BatchDetailsRequest(
                                            working_days = workingDaysInts(),
                                            gender = genderApi(),
                                            shifts = batchShifts.map { s ->
                                                val parts = s.time.split(" to ").map { it.trim() }
                                                val start24 = parts.getOrNull(0) ?: "08:00"
                                                val end24 = parts.getOrNull(1) ?: "09:00"
                                                BatchShiftTime(
                                                    startTime = format24hStringToAmPm(start24),
                                                    endTime = format24hStringToAmPm(end24)
                                                )
                                            }
                                        )
                                    } else null

                                    val request = CreatePlanCompatRequest(
                                        gymId = gymId,
                                        planType = planTypeApi(),
                                        planName = planName.trim(),
                                        durationDays = duration,
                                        price = priceVal,
                                        trainerId = if (needsTrainer) selectedTrainerId else null,
                                        batchDetails = batchDetails
                                    )

                                    if (editingPlanId != null) {
                                        screenModel.updatePlanCompat(editingPlanId, request) {
                                            onPlanSaved?.invoke()
                                            navigator?.pop()
                                        }
                                    } else {
                                        screenModel.createPlanCompat(request) {
                                            onPlanSaved?.invoke()
                                            navigator?.pop()
                                        }
                                    }
                                },
                                text = if (editingPlanId != null) "Update Plan" else "Save Plan",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
