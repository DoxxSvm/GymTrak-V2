package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.data.model.ExerciseRowDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse
import `in`.gym.trak.studio.data.model.WorkoutExerciseDTO
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.features.dashboard.AttendanceScannerScreen
import `in`.gym.trak.studio.data.model.MemberDashboardAttendance
import `in`.gym.trak.studio.data.model.MemberDashboardMembership
import `in`.gym.trak.studio.data.model.MemberDashboardNutrition
import `in`.gym.trak.studio.data.model.MemberDashboardResponse
import `in`.gym.trak.studio.data.model.MemberDashboardStats
import `in`.gym.trak.studio.data.model.MemberDashboardTodayWorkout
import `in`.gym.trak.studio.data.model.MemberDashboardUser
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.data.repository.SessionManager.memberGymUserId
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private fun WorkoutDetailResponse.toSheetExerciseRows(): List<WorkoutExerciseDTO> =
    exercises.map { ex ->
        WorkoutExerciseDTO(
            exercise = ExerciseRowDTO(
                id = ex.exercise_id.ifBlank { ex.name },
                name = ex.name,
                asset_url = ex.asset_url,
                equipment = null,
                primary_muscle = null,
                secondary_muscles = emptyList(),
                exercise_type = ex.exercise_type,
                is_active = true,
                created_at = null
            ),
            sets = ex.sets
        )
    }

@Composable
fun MemberHomeScreen(
    dashboard: MemberDashboardResponse? = null,
    memberWorkoutScreenModel: MemberWorkoutScreenModel? = null,
    onAfterWorkoutSheetFinished: () -> Unit = {},
) {
    val navigator = LocalNavigator.currentOrThrow
    val workoutSheetModel = memberWorkoutScreenModel ?: remember { MemberWorkoutScreenModel() }

    var showWorkoutSheet by remember { mutableStateOf(false) }
    var selectedWorkoutId by remember { mutableStateOf("") }
    var selectedWorkoutTitle by remember { mutableStateOf("") }
    var selectedWorkoutCategory by remember { mutableStateOf("") }
    var selectedWorkoutImage by remember { mutableStateOf(Res.drawable.img_workout) }
    var selectedWorkoutExercises by remember { mutableStateOf<List<WorkoutExerciseDTO>>(emptyList()) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StaggeredEntranceItem(index = 0) {
                HeaderSection(user = dashboard?.user)
            }
        }
        dashboard?.membership?.takeIf { it.shouldDisplayMembershipSection() }?.let { membership ->
            item {
                StaggeredEntranceItem(index = 1) {
                    MembershipStatusCard(membership = membership)
                }
            }
        }
        dashboard?.todayWorkout?.let { workout ->
            item {
                StaggeredEntranceItem(index = 2) {
                    SectionTitle("Today's Workout")
                }
            }
            item {
                StaggeredEntranceItem(index = 3) {
                    TodayWorkoutCard(
                        workout = workout,
                        onPlayClick = {
                            workout.id?.takeIf { it.isNotBlank() }?.let { wid ->
                                workoutSheetModel.loadWorkoutDetail(wid, showGlobalLoader = true) { detail ->
                                    selectedWorkoutId = workout.id
                                    selectedWorkoutTitle =
                                        detail.title.ifBlank { workout.title ?: "Workout" }
                                    selectedWorkoutCategory =
                                        workout.tags.firstOrNull()?.ifBlank { null } ?: "Today's workout"
                                    selectedWorkoutImage = Res.drawable.img_workout
                                    selectedWorkoutExercises = detail.toSheetExerciseRows()
                                    showWorkoutSheet = true
                                }
                            }
                        }
                    )
                }
            }
        }
        item {
            StaggeredEntranceItem(index = 4) {
                WorkoutStatsRow(stats = dashboard?.stats)
            }
        }
        item {
            StaggeredEntranceItem(index = 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NutritionCard(
                        modifier = Modifier.weight(1f),
                        nutrition = dashboard?.nutrition
                    )
                }
            }
        }
        item {
            StaggeredEntranceItem(index = 6) {
                AttendanceCard(attendance = dashboard?.attendance)
            }
        }
        item {
            StaggeredEntranceItem(index = 7) {
                SectionTitle("Quick Actions")
            }
        }
        item {
            StaggeredEntranceItem(index = 8) {
                QuickActionsGrid(
                    onStartWorkout = { navigator.push(ActiveWorkoutScreen()) },
                    onScanQrCode = {
                        navigator.push(
                            AttendanceScannerScreen(
                                autoLoadQrOnOpen = false,
                                openScannerDirectly = true
                            )
                        )
                    }
                )
            }
        }
        item {
            StaggeredEntranceItem(index = 9) {
                DailyMotivationCard(tagline = dashboard?.user?.tagline)
            }
        }
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showWorkoutSheet) {
        WorkoutDetailBottomSheet(
            title = selectedWorkoutTitle,
            category = selectedWorkoutCategory,
            image = selectedWorkoutImage,
            workoutId = selectedWorkoutId.takeIf { it.isNotBlank() },
            exercises = selectedWorkoutExercises,
            screenModel = workoutSheetModel,
            onWorkoutFinished = onAfterWorkoutSheetFinished,
            onDismiss = { showWorkoutSheet = false }
        )
    }
}

@Preview
@Composable
fun MemberHomeScreenPreview() {
    MemberHomeScreen()
}

/**
 * Hide membership when expired (0 days left), absent, or an empty shell from the API.
 */
private fun MemberDashboardMembership.shouldDisplayMembershipSection(): Boolean {
    if (daysRemaining != null && daysRemaining <= 0) return false
    if (!statusLabel.isNullOrBlank()) return true
    if (!progressLabel.isNullOrBlank()) return true
    if (!expiresAt.isNullOrBlank()) return true
    if (daysRemaining != null && daysRemaining > 0) return true
    if (totalDays != null && totalDays > 0) return true
    if (percentRemaining != null && percentRemaining > 0) return true
    return false
}

@Composable
fun HeaderSection(user: MemberDashboardUser? = null) {
    val greetingLine = when {
        user?.greeting != null && user.displayName != null -> "${user.greeting}, ${user.displayName}"
        user?.displayName != null -> user.displayName
        user?.firstName != null -> "Hello, ${user.firstName}"
        else -> "Good Morning"
    }
    val subtitle = user?.tagline ?: "Ready for today's workout?"
    val unread = user?.unreadNotifications ?: 0
    val avatarUrl = user?.avatarUrl

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = if (!avatarUrl.isNullOrBlank()) {
                rememberAsyncImagePainter(avatarUrl)
            } else {
                painterResource(Res.drawable.gym_boy)
            },
            contentDescription = "Profile",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greetingLine,
                style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Color.Black)
            )
            Text(
                text = subtitle,
                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
            )
        }
//        Box {
//            IconButton(
//                onClick = { },
//                modifier = Modifier
//                    .size(40.dp)
//                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
//            ) {
//                androidx.compose.material3.Icon(
//                    painter = painterResource(Res.drawable.ic_notification),
//                    contentDescription = "Notification",
//                    tint = Black,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
//            if (unread > 0) {
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.TopEnd)
//                        .size(10.dp)
//                        .background(Color(0xFFEF4444), CircleShape)
//                )
//            }
//        }
    }
}

@Composable
fun MembershipStatusCard(membership: MemberDashboardMembership? = null) {
    val headline = membership?.statusLabel
        ?: membership?.progressLabel
        ?: if (membership == null) "No active membership" else "Membership"
    val sub = membership?.daysRemaining?.let { "Expires in $it days" }
        ?: membership?.expiresAt?.let { "Renews $it" }
        ?: "Join a gym to track membership"
    val progress = ((membership?.percentRemaining ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)

    CommonCard(
        backgroundColor = Color.Transparent,
        elevation = 0.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF4F46E5), Color(0xFF3B82F6))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MEMBERSHIP STATUS",
                            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = White.copy(alpha = 0.7f))
                        )
                        Text(
                            text = headline,
                            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = White)
                        )
                        if (sub != headline) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sub,
                                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = White.copy(alpha = 0.85f))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { if (membership != null) progress else 0.15f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = White,
                    trackColor = White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Color.Black)
    )
}

@Composable
fun TodayWorkoutCard(
    workout: MemberDashboardTodayWorkout? = null,
    onPlayClick: () -> Unit = {},
) {
    val title = workout?.title ?: "Upper Body Strength"
    val exerciseCount = workout?.exerciseCount ?: 0
    val tags = workout?.tags?.takeIf { it.isNotEmpty() }
        ?: listOf("TRAINER PLAN", "RECOMMENDED")
    val completed = workout?.completed == true

    CommonCard(
        shape = RoundedCornerShape(24.dp),
        elevation = 4.dp
    ) {
        Column {
            Box {
                Image(
                    painter = workout?.imageUrl?.takeIf { it.isNotBlank() }?.let { rememberAsyncImagePainter(it) }
                        ?: painterResource(Res.drawable.img_workout),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.take(2).forEachIndexed { index, tag ->
                        val (bg, fg) = if (index == 0) Color.White to Black else PrimaryBlue to White
                        Badge(tag.uppercase(), bg, fg)
                    }
                }
            }
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Color.Black)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                            painterResource(Res.drawable.ic_workout),
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildString {
                                append(exerciseCount)
                                append(" Exercises")
                                if (completed) append(" · Done")
                            },
                            color = Gray,
                            style = AppTextTheme.regular.copy(fontSize = 14.sp)
                        )
                    }
                }
                IconButton(
                    onClick = onPlayClick,
                    enabled = !workout?.id.isNullOrBlank(),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            clip = false
                        )
                ) {
                    androidx.compose.material3.Icon(
                        painterResource(Res.drawable.ic_play),
                        null,
                        tint = White
                    )
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, backgroundColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(backgroundColor.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = AppTextTheme.bold.copy(fontSize = 10.sp, color = textColor))
    }
}

@Composable
fun WorkoutStatsRow(stats: MemberDashboardStats? = null) {
    val sessions = (stats?.sessionsThisWeek ?: 0).toString()
    val streak = (stats?.streakDays ?: 0).toString()
    val hours = stats?.totalHours?.let { h ->
        if (h % 1.0 == 0.0) "${h.toInt()}h" else "${((h * 10).toInt() / 10.0)}h"
    } ?: "0h"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox(modifier = Modifier.weight(1f), label = "SESSIONS", value = sessions, subtitle = "This Week", valueColor = PrimaryBlue)
        StatBox(modifier = Modifier.weight(1f), label = "STREAK", value = streak, subtitle = "Days", valueColor = OrangeColor)
        StatBox(modifier = Modifier.weight(1f), label = "TIME", value = hours, subtitle = "Total", valueColor = PrimaryDarkColor)
    }
}

@Composable
fun StatBox(modifier: Modifier, label: String, value: String, subtitle: String, valueColor: Color) {
    CommonCard(
        modifier = modifier
            .fillMaxHeight(),
        elevation = 0.dp,
        borderColor = Color(0xFFF3F4F6),
        backgroundColor = White
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
                .fillMaxSize()
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = AppTextTheme.semiBold.copy(fontSize = 15.sp, color = DarkBlue))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = AppTextTheme.bold.copy(fontSize = 20.sp, color = valueColor))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = AppTextTheme.semiBold.copy(fontSize = 10.sp, color = Gray))
        }
    }
}

@Composable
fun NutritionCard(modifier: Modifier, nutrition: MemberDashboardNutrition? = null) {

    val navigator = LocalNavigator.currentOrThrow

    val consumed = nutrition?.caloriesConsumed ?: 0
    val goal = nutrition?.calorieGoal ?: 2200
    val ratio = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val protein = nutrition?.proteinKcal?.let { "$it kcal" } ?: "Pending"
    val carbs = nutrition?.carbsKcal?.let { "$it kcal" } ?: "Pending"
    val fatLabel = if (nutrition?.fatPending == true) {
        "Pending"
    } else {
        nutrition?.fatKcal?.let { "$it kcal" } ?: "Pending"
    }

    CommonCard(modifier = modifier, elevation = 1.dp, borderColor = Color(0xFFF3F4F6)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Today's Nutrition", style = AppTextTheme.bold.copy(fontSize = 18.sp))
                Text(
                    "+ Log Meal",
                    style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = PrimaryBlue),
                    modifier = Modifier.clickable {

                        navigator.push(
                            CreateMealScreen(
                                readOnly = false,
                                memberGymUserId = memberGymUserId,
                                hideRepeat = true,
                                onMealCreated = {
//                                                if (memberGymUserId.isNotBlank()) {
//                                                    screenModel.loadMemberDiet(
//                                                        memberGymUserId,
//                                                        dietFilterLabelToCreatedBy(selectedFilter),
//                                                    )
//                                                }
                                }
                            )
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    MemberHomeCircularProgress(progress = ratio, size = 100.dp, color = PrimaryDarkColor)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$consumed", style = AppTextTheme.bold.copy(fontSize = 18.sp))
                        Text("/$goal kcal", style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray))
                    }
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NutritionItem(Color(0xFFEF4444), "Protein", protein)
                    NutritionItem(Color(0xFF10B981), "Carbs", carbs)
                    NutritionItem(Color(0xFFF97316), "Fat", fatLabel)
                }
            }
        }
    }
}

@Composable
fun NutritionItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Color.Black), modifier = Modifier.weight(1f))
        Text(value, style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray))
    }
}

@Composable
fun AttendanceCard(attendance: MemberDashboardAttendance? = null) {
    val attended = attendance?.daysAttended ?: 0
    val period = attendance?.periodDays ?: 0
    val pct = attendance?.percentileAmongMembers?.let { it.toInt().coerceIn(0, 100) }
    val ringProgress = if (period > 0) (attended.toFloat() / period).coerceIn(0f, 1f) else 0f
    val insight = attendance?.insightLabel
        ?: pct?.let { "Better than ${it}% of members!" }
        ?: "Attendance insights will appear as you check in."

    CommonCard(elevation = 1.dp, backgroundColor = DarkBlack) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Attendance", color = White.copy(0.7f), style = AppTextTheme.medium.copy(fontSize = 14.sp))
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$attended Days", color = White, style = AppTextTheme.bold.copy(fontSize = 24.sp))
                    Text(
                        " / $period",
                        color = White.copy(0.5f),
                        style = AppTextTheme.regular.copy(fontSize = 14.sp),
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(insight, color = Color(0xFF818CF8), style = AppTextTheme.regular.copy(fontSize = 12.sp))
            }
            Box(contentAlignment = Alignment.Center) {
                MemberHomeCircularProgress(
                    progress = ringProgress,
                    size = 70.dp,
                    color = PrimaryBlue,
                    trackColor = White.copy(0.1f)
                )
                Text("${(ringProgress * 100).toInt()}%", color = White, style = AppTextTheme.bold.copy(fontSize = 12.sp))
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onStartWorkout: () -> Unit = {},
    onScanQrCode: () -> Unit = {}
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionItem(Modifier.weight(1f), Res.drawable.ic_workout, "Start Workout", Color(0xFF6366F1), onClick = onStartWorkout)
            ActionItem(Modifier.weight(1f), Res.drawable.ic_log_meal, "Log Meal", Color(0xFF10B981))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionItem(Modifier.weight(1f), Res.drawable.ic_statistics, "View Statistics", Color(0xFF3B82F6))
            ActionItem(Modifier.weight(1f), Res.drawable.ic_qr, "Scan QR", Color(0xFF8B5CF6), onClick = onScanQrCode)
        }
    }
}

@Composable
fun ActionItem(modifier: Modifier, icon: DrawableResource, label: String, iconTint: Color, onClick: () -> Unit = {}) {
    CommonCard(modifier = modifier.clickable { onClick() }, elevation = 0.dp, borderColor = Color(0xFFF3F4F6)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = AppTextTheme.bold.copy(fontSize = 13.sp))
        }
    }
}

@Composable
fun DailyMotivationCard(tagline: String? = null) {
    val quote = tagline?.takeIf { it.isNotBlank() }
        ?: "\"Consistency is the key to transformation.\""
    CommonCard(
        backgroundColor = Color(0xFFFFF7ED), elevation = 0.dp,
        borderColor = Color(0xFFFFEDD5)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DAILY MOTIVATION", style = AppTextTheme.bold.copy(fontSize = 12.sp, color = OrangeDarkColor))
                Spacer(modifier = Modifier.height(4.dp))
                Text(quote, style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black))
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    painterResource(Res.drawable.ic_thunder),
                    null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MemberHomeCircularProgress(progress: Float, size: androidx.compose.ui.unit.Dp, color: Color, trackColor: Color = Color(0xFFF3F4F6)) {
    Canvas(modifier = Modifier.size(size)) {
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
