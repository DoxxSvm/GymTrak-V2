package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.data.model.ActiveExercise
import `in`.gym.trak.studio.data.model.ExerciseType
import `in`.gym.trak.studio.data.model.WorkoutSet
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.BlueLightColor
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_duration
import gym.composeapp.generated.resources.ic_play
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private fun parseSeconds(value: String): Int = value.toIntOrNull() ?: 0

/** Default first set, or next set copied from the most recent set's current KG/Reps (or duration). */
fun workoutSetFromPrevious(
    setNumber: Int,
    previousSet: WorkoutSet?,
    isTime: Boolean,
): WorkoutSet {
    if (previousSet == null) {
        return WorkoutSet(
            setNumber = setNumber,
            prev = "—",
            kg = "0",
            reps = if (isTime) "" else "0",
        )
    }
    return WorkoutSet(
        setNumber = setNumber,
        prev = "—",
        kg = previousSet.kg.ifBlank { "0" },
        reps = if (isTime) previousSet.reps else previousSet.reps.ifBlank { "0" },
    )
}

private fun formatSecondsLabel(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    fun twoDigits(value: Int): String = value.toString().padStart(2, '0')
    return if (hours > 0) {
        "${twoDigits(hours)}:${twoDigits(minutes)}:${twoDigits(seconds)}"
    } else {
        "${twoDigits(minutes)}:${twoDigits(seconds)}"
    }
}

@Composable
private fun DurationPickerDialog(
    initialTotalSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours by remember { mutableStateOf((initialTotalSeconds / 3600).coerceAtLeast(0)) }
    var minutes by remember { mutableStateOf(((initialTotalSeconds % 3600) / 60).coerceIn(0, 59)) }
    var seconds by remember { mutableStateOf((initialTotalSeconds % 60).coerceIn(0, 59)) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Duration", style = AppTextTheme.semiBold.copy(fontSize = 16.sp)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DurationStepColumn(
                    label = "Hours",
                    value = hours,
                    onIncrease = { if (hours < 99) hours++ },
                    onDecrease = { if (hours > 0) hours-- },
                    modifier = Modifier.weight(1f)
                )
                DurationStepColumn(
                    label = "Minutes",
                    value = minutes,
                    onIncrease = { minutes = (minutes + 1) % 60 },
                    onDecrease = { minutes = if (minutes == 0) 59 else minutes - 1 },
                    allowManualInput = true,
                    onManualValueChange = { minutes = it.coerceIn(0, 59) },
                    modifier = Modifier.weight(1f)
                )
                DurationStepColumn(
                    label = "Seconds",
                    value = seconds,
                    onIncrease = { seconds = (seconds + 1) % 60 },
                    onDecrease = { seconds = if (seconds == 0) 59 else seconds - 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hours * 3600 + minutes * 60 + seconds) }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DurationStepColumn(
    label: String,
    value: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    allowManualInput: Boolean = false,
    onManualValueChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember(value) { mutableStateOf(value.toString().padStart(2, '0')) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
        )
        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onIncrease) { Text("+") }
        if (allowManualInput) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() }.take(2)
                        inputText = filtered
                        val parsed = filtered.toIntOrNull() ?: 0
                        onManualValueChange(parsed)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    textStyle = AppTextTheme.semiBold.copy(
                        fontSize = 18.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(PrimaryColor),
                    singleLine = true
                )
            }
        } else {
            Text(
                text = value.toString().padStart(2, '0'),
                style = AppTextTheme.semiBold.copy(fontSize = 18.sp, color = Color.Black)
            )
        }
        TextButton(onClick = onDecrease) { Text("-") }
    }
}

@Composable
fun ExerciseCard(
    exercise: ActiveExercise,
    isCreateMode: Boolean = false,
    readOnly: Boolean = false,
    onSetValueChanged: () -> Unit = {},
    onSetCompleted: ((ActiveExercise, WorkoutSet, Boolean) -> Unit)? = null,
    onSetUpdated: ((ActiveExercise, WorkoutSet) -> Unit)? = null,
    onSetAdded: ((ActiveExercise, WorkoutSet) -> Unit)? = null,
    onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var setValuesRevision by remember { mutableIntStateOf(0) }
    val sets = remember { exercise.sets.toMutableStateList() }

    CommonCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        borderColor = Color(0xFFE5E7EB),
        backgroundColor = White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueLightColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!exercise.assetUrl.isNullOrEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(exercise.assetUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(exercise.icon),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Color.Black)
                    )
                }
                if (!readOnly) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Gray
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector =Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TYPE",
                    style = AppTextTheme.medium.copy(fontSize = 10.sp, color = Gray)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (exercise.type == ExerciseType.TIME) "Time" else "Reps",
                    style = AppTextTheme.bold.copy(fontSize = 10.sp, color = PrimaryColor)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isTime = exercise.type == ExerciseType.TIME
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SetColumnHeader("SET", Modifier.weight(0.8f))
                SetColumnHeader("PREV", Modifier.weight(1.5f))
                SetColumnHeader(if (isTime) "TIME (SEC)" else "KG", Modifier.weight(1.2f))
                SetColumnHeader(if (isTime) "" else "REPS", Modifier.weight(1.2f))
                SetColumnHeader(if (isCreateMode) "DEL" else "DONE", Modifier.weight(0.8f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            sets.forEachIndexed { index, set ->
                val previous = sets.getOrNull(index - 1)
                key(set.setNumber) {
                    SetRow(
                        set = set,
                        showPreviousSet = index > 0,
                        previousSetKg = previous?.kg.orEmpty(),
                        previousSetReps = previous?.reps.orEmpty(),
                        setValuesRevision = setValuesRevision,
                        isTime = isTime,
                        isCreateMode = isCreateMode,
                        readOnly = readOnly,
                        onDoneToggle = {
                            val current = sets[index]
                            current.isDone = !current.isDone
                            exercise.sets[index].isDone = current.isDone
                            onSetCompleted?.invoke(exercise, current, current.isDone)
                            setValuesRevision++
                            onSetValueChanged()
                        },
                        onDeleteSet = {
                            sets.removeAt(index)
                            exercise.sets.removeAt(index)
                            // Renumber sets
                            sets.forEachIndexed { i, s ->
                                val renumb = s.copy(setNumber = i + 1)
                                sets[i] = renumb
                                exercise.sets[i] = renumb
                            }
                            setValuesRevision++
                            onSetValueChanged()
                        },
                        onSetValueChanged = {
                            setValuesRevision++
                            onSetValueChanged()
                        },
                        onSetUpdated = {
                            onSetUpdated?.invoke(exercise, sets[index])
                        },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (!readOnly) {
                DottedBorderButton(
                    text = "+ Add Set",
                    onClick = {
                        val newNum = sets.size + 1
                        val newSet = workoutSetFromPrevious(
                            setNumber = newNum,
                            previousSet = sets.lastOrNull(),
                            isTime = isTime,
                        )
                        sets.add(newSet)
                        exercise.sets.add(newSet)
                        setValuesRevision++
                        onSetAdded?.invoke(exercise, newSet)
                        onSetValueChanged()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

        }
    }
}

@Composable
fun DottedBorderButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = 1.5.dp.toPx()
            val stroke = Stroke(
                width = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), 0f)
            )
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = strokePx / 2,
                        top = strokePx / 2,
                        right = size.width - strokePx / 2,
                        bottom = size.height - strokePx / 2,
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                )
            }
            drawPath(path, color = Color(0xFFB0B0B0), style = stroke)
        }
        Text(
            text,
            style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = Gray)
        )
    }
}

private fun formatCurrentSetValueLabel(kg: String, reps: String, isTime: Boolean): String {
    if (isTime) {
        val seconds = parseSeconds(kg.trim())
        return if (seconds > 0) formatSecondsLabel(seconds) else ""
    }
    val currentKg = kg.trim()
    val currentReps = reps.trim()
    return when {
        currentKg.isNotEmpty() && currentReps.isNotEmpty() -> "$currentKg × $currentReps"
        currentKg.isNotEmpty() -> currentKg
        currentReps.isNotEmpty() -> currentReps
        else -> ""
    }
}

private fun formatPreviousSetLabel(
    showPreviousSet: Boolean,
    previousKg: String,
    previousReps: String,
    isTime: Boolean,
): String {
    if (!showPreviousSet) return "—"
    return formatCurrentSetValueLabel(
        kg = previousKg,
        reps = previousReps,
        isTime = isTime,
    ).ifBlank { "—" }
}

@Composable
fun SetRow(
    set: WorkoutSet,
    showPreviousSet: Boolean = false,
    previousSetKg: String = "",
    previousSetReps: String = "",
    setValuesRevision: Int = 0,
    isTime: Boolean,
    isCreateMode: Boolean = false,
    readOnly: Boolean = false,
    onDoneToggle: () -> Unit,
    onSetValueChanged: () -> Unit = {},
    onSetUpdated: () -> Unit = {},
    onDeleteSet: () -> Unit = {}
) {
    var kg by remember(set.setNumber) { mutableStateOf(set.kg) }
    var reps by remember(set.setNumber) { mutableStateOf(set.reps) }
    var showDurationPicker by remember { mutableStateOf(false) }

    LaunchedEffect(set.kg, set.reps) {
        if (kg != set.kg) kg = set.kg
        if (reps != set.reps) reps = set.reps
    }

    val currentValueLabel = remember(setValuesRevision, kg, reps, isTime) {
        formatCurrentSetValueLabel(kg = kg, reps = reps, isTime = isTime)
    }
    val previousLabel = remember(setValuesRevision, showPreviousSet, previousSetKg, previousSetReps, isTime) {
        formatPreviousSetLabel(
            showPreviousSet = showPreviousSet,
            previousKg = previousSetKg,
            previousReps = previousSetReps,
            isTime = isTime,
        )
    }

    if (isTime && !readOnly && showDurationPicker) {
        val initialSeconds = parseSeconds(kg)
        DurationPickerDialog(
            initialTotalSeconds = initialSeconds,
            onDismiss = { showDurationPicker = false },
            onConfirm = { totalSeconds ->
                kg = totalSeconds.toString()
                set.kg = kg
                onSetValueChanged()
                if (!isCreateMode) {
                    onSetUpdated()
                }
                showDurationPicker = false
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${set.setNumber}",
            style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.8f)
        )

        Text(
            text = if (showPreviousSet) previousLabel else currentValueLabel.ifBlank { "—" },
            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.5f),
        )

        if (isTime) {
            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!readOnly) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(50.dp))
                            .background(PrimaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_play),
                            null,
                            tint = White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (readOnly) {
                    Text(
                        formatSecondsLabel(parseSeconds(kg)),
                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDurationPicker = true }
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = formatSecondsLabel(parseSeconds(kg)),
                            style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = Color.Black)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1.2f))
        } else {
            Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                if (readOnly) {
                    ReadOnlyCellBox(kg)
                } else {
                    EditableCellBox(value = kg, numericOnly = true, onValueChange = {
                        kg = it
                        set.kg = it
                        onSetValueChanged()
                        if (!isCreateMode) {
                            onSetUpdated()
                        }
                    })
                }
            }
            Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                if (readOnly) {
                    ReadOnlyCellBox(reps)
                } else {
                    EditableCellBox(value = reps, numericOnly = true, onValueChange = {
                        reps = it
                        set.reps = it
                        onSetValueChanged()
                        if (!isCreateMode) {
                            onSetUpdated()
                        }
                    })
                }
            }
        }

        Box(modifier = Modifier.weight(0.8f), contentAlignment = Alignment.Center) {
            val actionModifier = if (readOnly) {
                Modifier
            } else {
                Modifier.clickable {
                    if (isCreateMode) onDeleteSet() else onDoneToggle()
                }
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isCreateMode && !readOnly -> Color(0xFFFFEBEE)
                            set.isDone -> PrimaryColor
                            else -> Color(0xFFF3F4F6)
                        }
                    )
                    .then(actionModifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCreateMode && !readOnly) Icons.Default.Delete else Icons.Default.Check,
                    contentDescription = if (isCreateMode && !readOnly) "Delete" else "Done",
                    tint = when {
                        isCreateMode && !readOnly -> Color.Red
                        set.isDone -> White
                        else -> Color(0xFFB0B0B0)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SetColumnHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTextTheme.semiBold.copy(fontSize = 10.sp, color = Gray),
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun ReadOnlyCellBox(value: String) {
    Box(
        modifier = Modifier.width(56.dp).height(34.dp).clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF3F4F6)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black, textAlign = TextAlign.Center)
        )
    }
}

@Composable
fun EditableCellBox(
    value: String,
    numericOnly: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier.width(56.dp).height(34.dp).clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF3F4F6)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                if (!numericOnly || raw.all { it.isDigit() }) {
                    onValueChange(raw)
                }
            },
            textStyle = AppTextTheme.semiBold.copy(
                fontSize = 14.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(PrimaryColor),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = if (numericOnly) {
                KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            }
        )
    }
}

@Composable
fun EditableSetCell(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number
        ),
        textStyle = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black),
        cursorBrush = SolidColor(PrimaryColor),
        singleLine = true
    )
}

@Composable
fun WorkoutDetailStat(
    modifier: Modifier,
    icon: DrawableResource,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray))
        Text(value, style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = valueColor))
    }
}

@Composable
fun EditableWorkoutDurationStat(
    modifier: Modifier,
    minutes: String,
    onMinutesChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painterResource(Res.drawable.ic_duration),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text("Duration", style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray))
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            BasicTextField(
                value = minutes,
                onValueChange = { raw ->
                    if (enabled) onMinutesChange(raw.filter { it.isDigit() }.take(4))
                },
                enabled = enabled,
                modifier = Modifier.width(36.dp),
                textStyle = AppTextTheme.semiBold.copy(
                    fontSize = 13.sp,
                    color = PrimaryColor,
                    textAlign = TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(PrimaryColor),
            )
            Text(
                text = " min",
                style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = PrimaryColor),
            )
        }
    }
}
