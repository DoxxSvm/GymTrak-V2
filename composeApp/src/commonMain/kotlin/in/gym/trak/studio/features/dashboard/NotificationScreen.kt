package `in`.gym.trak.studio.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.NotificationItem
import `in`.gym.trak.studio.data.model.NotificationDTO
import `in`.gym.trak.studio.features.expenses.ExpensesScreen
import `in`.gym.trak.studio.features.members.MemberDetailScreen
import `in`.gym.trak.studio.features.trainers.TrainerDetailScreen
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.viewmodel.dashboard.NotificationScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_notification
import gym.composeapp.generated.resources.ic_money
import gym.composeapp.generated.resources.ic_subscription
import gym.composeapp.generated.resources.ic_workout
import `in`.gym.trak.studio.data.model.GymStaffListRole
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.DrawableResource

/**
 * Owner notification feed — GET /notifications (cursor paginated).
 */
class NotificationScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { NotificationScreenModel() }
        val notifications by screenModel.notifications.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val loadingMore by screenModel.loadingMore.collectAsState()
        var isPullRefreshRequested by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val pullToRefreshState = rememberPullToRefreshState()

        val sections = remember(notifications) { groupNotificationsByDay(notifications) }
        val flatItems = remember(sections) {
            buildList {
                sections.forEach { section ->
                    add(NotificationListEntry.SectionHeader(section.label))
                    section.items.forEach { add(NotificationListEntry.Item(it)) }
                }
            }
        }

        LaunchedEffect(Unit) {
            screenModel.loadNotifications(refresh = true)
        }

        LaunchedEffect(isLoading) {
            if (!isLoading) isPullRefreshRequested = false
        }

        LaunchedEffect(listState, notifications.size) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .collectLatest { visibleItems ->
                    val lastVisible = visibleItems.lastOrNull()?.index ?: return@collectLatest
                    if (notifications.isNotEmpty() && lastVisible >= flatItems.size - 3) {
                        screenModel.loadMoreNotifications()
                    }
                }
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Notification",
                        onBackClick = { navigator?.pop() },
                    )
                },
                containerColor = Color.Transparent,
            ) { padding ->
                PullToRefreshBox(
                    isRefreshing = isPullRefreshRequested,
                    onRefresh = {
                        isPullRefreshRequested = true
                        screenModel.refresh()
                    },
                    state = pullToRefreshState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    when {
                        notifications.isEmpty() && isLoading -> {
                            // Initial load: LoadingScreenHandler shows the overlay (avoid duplicate spinner).
                            Box(modifier = Modifier.fillMaxSize())
                        }

                        notifications.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppEmptyStateView(
                                    image = Res.drawable.ic_notification,
                                    title = "No notifications yet",
                                    subtitle = "Updates about members, payments, and gym activity will appear here.",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp),
                                    useCardContainer = false,
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                state = listState,
                            ) {
                                item { Spacer(modifier = Modifier.height(12.dp)) }

                                itemsIndexed(
                                    items = flatItems,
                                    key = { index, entry ->
                                        when (entry) {
                                            is NotificationListEntry.SectionHeader -> "header_${entry.label}_$index"
                                            is NotificationListEntry.Item -> entry.notification.id
                                        }
                                    },
                                ) { _, entry ->
                                    when (entry) {
                                        is NotificationListEntry.SectionHeader -> {
                                            Text(
                                                text = entry.label,
                                                style = AppTextTheme.bold.copy(
                                                    fontSize = 15.sp,
                                                    color = Gray,
                                                ),
                                                modifier = Modifier.padding(vertical = 12.dp),
                                            )
                                        }

                                        is NotificationListEntry.Item -> {
                                            NotificationListItem(
                                                notification = entry.notification,
                                                onClick = {
                                                    handleNotificationClick(
                                                        notification = entry.notification,
                                                        navigator = navigator,
                                                        screenModel = screenModel,
                                                    )
                                                },
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }
                                }

                                if (loadingMore) {
                                    item(key = "loading_more") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                color = PrimaryColor,
                                                modifier = Modifier.height(32.dp),
                                            )
                                        }
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NotificationListItem(
        notification: NotificationDTO,
        onClick: () -> Unit,
    ) {
        val title = buildNotificationTitle(notification)
        val time = DateUtils.formatRelativeNotificationTime(notification.createdAt)
        val isUnread = notification.readAt.isNullOrBlank()
        val icon = notificationIcon(notification)
        val showMemberAvatar = notification.member?.gymUserId?.isNotBlank() == true ||
            notification.type.uppercase().contains("MEMBER") ||
            notification.entity?.type?.uppercase()?.contains("MEMBER") == true

        NotificationItem(
            title = title,
            time = time,
            avatar = if (showMemberAvatar) Res.drawable.gym_boy else null,
            icon = icon,
            isUnread = isUnread,
            onClick = onClick,
        )
    }

    private fun handleNotificationClick(
        notification: NotificationDTO,
        navigator: Navigator?,
        screenModel: NotificationScreenModel,
    ) {
        screenModel.markNotificationRead(notification.id) {
            navigateFromNotificationDeepLink(notification, navigator)
        }
    }

    private fun navigateFromNotificationDeepLink(
        notification: NotificationDTO,
        navigator: Navigator?,
    ) {
        val nav = navigator ?: return
        val screen = notification.metadata?.deepLink?.screen?.lowercase().orEmpty()
        val params = notification.metadata?.deepLink?.params

        when (screen) {
            "expense" -> nav.push(ExpensesScreen())
            "trainer-salary" -> {
                val entity = notification.entity
                val trainerGymUserId = params?.gymUserId?.takeIf { it.isNotBlank() }
                    ?: notification.metadata?.gymUserId?.takeIf { it.isNotBlank() }
                    ?: entity?.id?.takeIf { entity.type.equals("TRAINER", ignoreCase = true) }
                if (!trainerGymUserId.isNullOrBlank()) {
                    nav.push(TrainerDetailScreen(
                        listRole = GymStaffListRole.TRAINER,
                        trainerId = trainerGymUserId))
                }
            }
            "staff-salary" -> {
                val entity = notification.entity
                val trainerGymUserId = params?.gymUserId?.takeIf { it.isNotBlank() }
                    ?: notification.metadata?.gymUserId?.takeIf { it.isNotBlank() }
                    ?: entity?.id?.takeIf { entity.type.equals("STAFF", ignoreCase = true) }
                if (!trainerGymUserId.isNullOrBlank()) {
                    nav.push(TrainerDetailScreen(
                        listRole = GymStaffListRole.STAFF,

                        trainerId = trainerGymUserId))
                }
            }
            "payment", "subscription", "member" -> {
                val memberGymUserId = resolveMemberGymUserId(notification) ?: return
                val initialTab = when (screen) {
                    "payment" -> "Payment"
                    "subscription" -> "Subscriptions"
                    else -> "Profile"
                }
                nav.push(
                    MemberDetailScreen(
                        memberId = memberGymUserId,
                        initialTab = initialTab,
                    ),
                )
            }
            else -> {
                val memberGymUserId = resolveMemberGymUserId(notification)
                if (!memberGymUserId.isNullOrBlank()) {
                    nav.push(MemberDetailScreen(memberId = memberGymUserId))
                } else {
                    val entity = notification.entity
                    val trainerId = entity?.id?.takeIf {
                        entity.type.equals("TRAINER", ignoreCase = true) && it.isNotBlank()
                    }
                    if (trainerId != null) {
                        nav.push(TrainerDetailScreen(trainerId = trainerId))
                    }
                }
            }
        }
    }

    private fun resolveMemberGymUserId(notification: NotificationDTO): String? {
        return notification.member?.gymUserId?.takeIf { it.isNotBlank() }
            ?: notification.metadata?.deepLink?.params?.gymUserId?.takeIf { it.isNotBlank() }
            ?: notification.metadata?.gymUserId?.takeIf { it.isNotBlank() }
            ?: notification.entity?.takeIf {
                it.type.uppercase().contains("MEMBER") && it.id.isNotBlank()
            }?.id
    }

    private fun buildNotificationTitle(notification: NotificationDTO): AnnotatedString {
        val names = collectHighlightNames(notification)
        return buildAnnotatedString {
            val headline = notification.title.takeIf { it.isNotBlank() } ?: "Notification"
            appendTextWithNameHighlights(headline, names)
            val body = notification.body.takeIf { it.isNotBlank() && it != headline }
            if (body != null) {
                append("\n")
                appendTextWithNameHighlights(body, names)
            }
        }
    }

    private fun collectHighlightNames(notification: NotificationDTO): List<String> {
        val names = linkedSetOf<String>()
        notification.member?.name?.trim()?.takeIf { it.isNotBlank() }?.let { names.add(it) }
        notification.actor?.name?.trim()?.takeIf { it.isNotBlank() }?.let { names.add(it) }
        notification.metadata?.memberName?.trim()?.takeIf { it.isNotBlank() }?.let { names.add(it) }
        notification.metadata?.actorName?.trim()?.takeIf { it.isNotBlank() }?.let { names.add(it) }
        notification.title.substringAfter(" — ", "").trim().takeIf { it.isNotBlank() }?.let { names.add(it) }
        return names.sortedByDescending { it.length }
    }

    private fun AnnotatedString.Builder.appendTextWithNameHighlights(
        text: String,
        names: List<String>,
    ) {
        if (names.isEmpty()) {
            append(text)
            return
        }
        val highlightStyle = SpanStyle(color = PrimaryColor, fontWeight = FontWeight.Bold)
        var remaining = text
        while (remaining.isNotEmpty()) {
            val earliest = names.mapNotNull { name ->
                val index = remaining.indexOf(name, ignoreCase = true)
                if (index >= 0) Triple(name, index, index + name.length) else null
            }.minByOrNull { it.second }
            if (earliest == null) {
                append(remaining)
                break
            }
            val (name, start, end) = earliest
            if (start > 0) append(remaining.substring(0, start))
            withStyle(highlightStyle) {
                append(remaining.substring(start, end))
            }
            remaining = remaining.substring(end)
        }
    }

    private fun notificationIcon(notification: NotificationDTO): DrawableResource? {
        val typeKey = notification.type.uppercase()
        val entityKey = notification.entity?.type?.uppercase().orEmpty()
        val screen = notification.metadata?.deepLink?.screen?.lowercase().orEmpty()
        val category = notification.metadata?.category?.uppercase().orEmpty()
        return when {
            screen == "expense" || screen == "trainer-salary" || category == "SALARY" ->
                Res.drawable.ic_money
            typeKey.contains("PAYMENT") || entityKey.contains("PAYMENT") -> Res.drawable.ic_money
            typeKey.contains("PLAN") || typeKey.contains("SUBSCRIPTION") ||
                entityKey.contains("SUBSCRIPTION") || entityKey.contains("MEMBER_SUBSCRIPTION") ->
                Res.drawable.ic_subscription
            typeKey.contains("WORKOUT") || typeKey.contains("SESSION") -> Res.drawable.ic_workout
            entityKey.contains("TRAINER") -> Res.drawable.ic_notification
            typeKey.contains("MEMBER") || entityKey.contains("MEMBER") -> null
            else -> Res.drawable.ic_notification
        }
    }

    private sealed class NotificationListEntry {
        data class SectionHeader(val label: String) : NotificationListEntry()
        data class Item(val notification: NotificationDTO) : NotificationListEntry()
    }

    private data class NotificationDaySection(
        val label: String,
        val items: List<NotificationDTO>,
    )

    private fun groupNotificationsByDay(items: List<NotificationDTO>): List<NotificationDaySection> {
        if (items.isEmpty()) return emptyList()
        val grouped = linkedMapOf<String, MutableList<NotificationDTO>>()
        items.forEach { notification ->
            val label = DateUtils.notificationSectionLabel(notification.createdAt)
            grouped.getOrPut(label) { mutableListOf() }.add(notification)
        }
        val order = listOf("Today", "Yesterday")
        val sortedKeys = grouped.keys.sortedWith { a, b ->
            val aIndex = order.indexOf(a)
            val bIndex = order.indexOf(b)
            when {
                aIndex >= 0 && bIndex >= 0 -> aIndex.compareTo(bIndex)
                aIndex >= 0 -> -1
                bIndex >= 0 -> 1
                else -> b.compareTo(a)
            }
        }
        return sortedKeys.map { key ->
            NotificationDaySection(label = key, items = grouped[key].orEmpty())
        }
    }
}
