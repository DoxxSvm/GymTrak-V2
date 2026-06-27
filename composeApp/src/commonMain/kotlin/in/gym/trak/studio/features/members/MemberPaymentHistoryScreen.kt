package `in`.gym.trak.studio.features.members

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_gold
import gym.composeapp.generated.resources.ic_silver
import gym.composeapp.generated.resources.img_no_payment
import gym.composeapp.generated.resources.login_bg
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.model.PaymentItemDTO
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Lists gym payments for a single member (GET `/payments?gymId=…&memberId=…`).
 */
class MemberPaymentHistoryScreen(
    private val memberId: String,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        MemberPaymentHistoryContent(
            memberId = memberId,
            screenModel = screenModel,
            onBackClick = { navigator?.pop() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPaymentHistoryContent(
    memberId: String,
    screenModel: OwnerDashboardScreenModel,
    onBackClick: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val items by screenModel.memberPaymentHistoryItems.collectAsState()
    val listLoading by screenModel.memberPaymentHistoryLoading.collectAsState()

    LaunchedEffect(memberId, searchQuery) {
        val resolvedMemberId =
            memberId.ifBlank { SessionManager.effectiveMemberListingIdForApi("") }
        if (resolvedMemberId.isBlank()) return@LaunchedEffect
        if (searchQuery.isNotBlank()) delay(400)
        screenModel.loadMemberPaymentHistory(
            memberId = resolvedMemberId,
            search = searchQuery.takeIf { it.isNotBlank() },
            reset = true,
        )
    }

    LoadingScreenHandler(screenModel = screenModel) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(Res.drawable.login_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    GymAppBar(
                        title = "Payment History",
                        onBackClick = onBackClick
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search reference, status, or notes"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Recent Transactions",
                        style = AppTextTheme.bold.copy(fontSize = 16.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        listLoading && items.isEmpty() -> {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryColor)
                            }
                        }

                        !listLoading && items.isEmpty() -> {
                            AppEmptyStateView(
                                image = Res.drawable.img_no_payment,
                                title = "No Payment Records Found",
                                subtitle = if (searchQuery.isNotBlank()) {
                                    "No payments match your search."
                                } else {
                                    "Collect or extend a plan to see transactions here."
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(items) { index, payment ->
                                    MemberPaymentHistoryRow(
                                        payment = payment,
                                        icon = if (index % 2 == 0) Res.drawable.ic_gold else Res.drawable.ic_silver,
                                    )
                                    if (index >= items.size - 4 && !listLoading) {
                                        LaunchedEffect(items.size, memberId) {
                                            val resolved =
                                                memberId.ifBlank { SessionManager.effectiveMemberListingIdForApi("") }
                                            if (resolved.isBlank()) return@LaunchedEffect
                                            screenModel.loadMemberPaymentHistory(
                                                memberId = resolved,
                                                search = searchQuery.takeIf { it.isNotBlank() },
                                                reset = false,
                                            )
                                        }
                                    }
                                }
                                if (listLoading && items.isNotEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(28.dp),
                                                color = PrimaryColor
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
    }
}

@Composable
private fun MemberPaymentHistoryRow(
    payment: PaymentItemDTO,
    icon: DrawableResource,
) {
    val title = payment.description?.takeIf { it.isNotBlank() } ?: "Membership payment"
    val dateLine = DateUtils.formatShortDateTime(payment.completedAt ?: payment.createdAt)
    val mode = payment.method?.uppercase()?.takeIf { it.isNotBlank() } ?: "—"
    val amountLabel = formatPaymentAmountMinor(payment.amountCents, payment.currency)

    CommonCard(content = {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTextTheme.semiBold.copy(fontSize = 14.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateLine,
                    style = AppTextTheme.regular.copy(
                        fontSize = 12.sp,
                        color = Black.copy(alpha = 0.60f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                val statusLine = payment.status?.takeIf { it.isNotBlank() }?.let { "Status · $it" } ?: ""
                if (statusLine.isNotBlank()) {
                    Text(
                        text = statusLine,
                        style = AppTextTheme.regular.copy(
                            fontSize = 11.sp,
                            color = Black.copy(alpha = 0.45f)
                        ),
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountLabel,
                    style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = PrimaryColor),
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = if (mode == "UPI") Color(0xFFFFF0F0) else Color(0xFFE7F7F2),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = mode,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black)
                    )
                }
            }
        }
    })
}

private fun formatPaymentAmountMinor(amountCents: Int, currency: String): String {
    val whole = amountCents
    val frac = kotlin.math.abs(amountCents % 100)
    val fracStr = frac.toString().padStart(2, '0')
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        else -> Constants.RUPEE
    }
    return "$symbol$amountCents"
}
