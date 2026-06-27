package `in`.gym.trak.studio.features.management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.billing.appstore.GoldStoreProductQueryState
import `in`.gym.trak.studio.billing.appstore.StoreBillingPeriod
import `in`.gym.trak.studio.billing.appstore.StorePlanTier
import `in`.gym.trak.studio.billing.appstore.StoreSubscriptionProduct
import `in`.gym.trak.studio.billing.appstore.StoreSubscriptionStatus
import `in`.gym.trak.studio.billing.appstore.isAppleAppStoreBillingAvailable
import `in`.gym.trak.studio.components.CommonProgressOverlay
import `in`.gym.trak.studio.components.CycleToggle
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.PlanCard
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.DarkBlue
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.YellowColor
import `in`.gym.trak.studio.viewmodel.billing.AppStoreSubscriptionScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_gold
import gym.composeapp.generated.resources.ic_silver
import org.jetbrains.compose.resources.DrawableResource

/**
 * iOS App Store subscription plans screen (StoreKit auto-renewable products only).
 */
class GymSubscriptionScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val billingModel = rememberScreenModel { AppStoreSubscriptionScreenModel() }

        val productQueryState by billingModel.goldStoreProductQueryState.collectAsState()
        val subscriptionStatus by billingModel.status.collectAsState()
        val isPurchasing by billingModel.isLoading.collectAsState()

        LaunchedEffect(Unit) {
            if (isAppleAppStoreBillingAvailable()) {
                billingModel.loadAppStoreConnectProducts(showGlobalLoader = false)
                billingModel.refreshStatus(showLoader = false)
            }
        }

        var billingCycle by remember { mutableStateOf("Monthly") }

        val filteredProducts = remember(productQueryState, billingCycle) {
            val products = when (val state = productQueryState) {
                is GoldStoreProductQueryState.Success -> state.products
                else -> emptyList()
            }
            products
                .filter { it.matchesBillingCycle(billingCycle) }
                .sortedBy { it.planTier.sortOrder() }
        }

        val isProductsLoading = productQueryState is GoldStoreProductQueryState.Loading ||
            productQueryState is GoldStoreProductQueryState.Idle

        val showLoaderOverlay = isProductsLoading || isPurchasing

        LoadingScreenHandler(screenModel = billingModel, showLoadingOverlay = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    topBar = {
                        GymAppBar(
                            title = "Subscription Plans",
                            onBackClick = { navigator?.pop() },
                        )
                    },
                    containerColor = Color.Transparent,
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        StaggeredEntranceItem(index = 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Upgrade Your Gym",
                                    style = AppTextTheme.bold.copy(fontSize = 28.sp),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Choose The Plan That Fits Your Business Needs\nPerfectly.",
                                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (!isAppleAppStoreBillingAvailable()) {
                            IosOnlySubscriptionMessage()
                        } else {
                            StaggeredEntranceItem(index = 1) {
                                CycleToggle(
                                    option1 = "Monthly",
                                    option2 = "Yearly",
                                    selectedOption = billingCycle,
                                    onOptionSelected = { billingCycle = it },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            when (val queryState = productQueryState) {
                                GoldStoreProductQueryState.StoreUnavailable -> {
                                    SubscriptionEmptyState(
                                        title = "App Store Unavailable",
                                        message = "In-app purchases are not available right now.\n" +
                                            "Use a physical iPhone with a signed-in Apple ID and try again.",
                                    )
                                }
                                is GoldStoreProductQueryState.ProductsNotFound -> {
                                    SubscriptionEmptyState(
                                        title = "Products Not Found",
                                        message = "Requested: ${queryState.requestedIds}\n" +
                                            "Not found: ${queryState.notFoundIds}\n\n" +
                                            "In App Store Connect, confirm product IDs are exactly " +
                                            "\"gold\" and \"silver\", then submit them with your app version.",
                                    )
                                }
                                is GoldStoreProductQueryState.QueryError -> {
                                    SubscriptionEmptyState(
                                        title = "Unable to Load Plans",
                                        message = queryState.message,
                                    )
                                }
                                is GoldStoreProductQueryState.Error -> {
                                    SubscriptionEmptyState(
                                        title = "Unable to Load Plans",
                                        message = queryState.message,
                                    )
                                }
                                else -> {
                                    if (!isProductsLoading && filteredProducts.isEmpty()) {
                                        SubscriptionEmptyState(
                                            title = "No Plans Found",
                                            message = emptyStateMessageForCycle(billingCycle),
                                        )
                                    } else if (!isProductsLoading) {
                                        filteredProducts.forEachIndexed { index, product ->
                                            val visuals = product.toPlanCardVisuals()
                                            val isActive = subscriptionStatus.isActiveProduct(product.productId)
                                            StaggeredEntranceItem(index = index + 2) {
                                                PlanCard(
                                                    tier = visuals.tierTitle +
                                                        if (isActive) " · Active" else "",
                                                    price = product.displayPrice,
                                                    icon = visuals.icon,
                                                    bgColor = visuals.bgColor,
                                                    edgeColor = visuals.edgeColor,
                                                    description = visuals.subtitle,
                                                    savingsLabel = visuals.savingsLabel,
                                                    periodSuffix = visuals.periodSuffix,
                                                    selectButtonText = "Select Plan",
                                                    features = visuals.features,
                                                    onSelect = {
                                                        billingModel.purchase(product.productId)
                                                    },
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }

                if (showLoaderOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable(enabled = false) { },
                    ) {
                        CommonProgressOverlay()
                    }
                }
            }
        }
    }
}

private data class PlanCardVisuals(
    val tierTitle: String,
    val subtitle: String,
    val icon: DrawableResource,
    val bgColor: Color,
    val edgeColor: Color,
    val savingsLabel: String,
    val periodSuffix: String,
    val features: List<String>,
)

private fun StoreSubscriptionProduct.toPlanCardVisuals(): PlanCardVisuals {
    val periodSuffix = when (period) {
        StoreBillingPeriod.Yearly -> "year"
        StoreBillingPeriod.Monthly -> "month"
        StoreBillingPeriod.Unknown -> "period"
    }
    return when (planTier) {
        StorePlanTier.Silver -> PlanCardVisuals(
            tierTitle = "Silver",
            subtitle = "For Small Studios",
            icon = Res.drawable.ic_silver,
            bgColor = Color(0xFF1A335E),
            edgeColor = Color(0xFFC0C0C0),
            savingsLabel = "15%",
            periodSuffix = periodSuffix,
            features = silverPlanFeatures(),
        )
        StorePlanTier.Gold -> PlanCardVisuals(
            tierTitle = "Gold Plan",
            subtitle = "For Small Studios",
            icon = Res.drawable.ic_gold,
            bgColor = Color(0xFF2C2F36),
            edgeColor = YellowColor,
            savingsLabel = "15%",
            periodSuffix = periodSuffix,
            features = goldPlanFeatures(),
        )
        StorePlanTier.Unknown -> PlanCardVisuals(
            tierTitle = sanitizedPlanTitle(),
            subtitle = "For Small Studios",
            icon = Res.drawable.ic_gold,
            bgColor = DarkBlue,
            edgeColor = Color(0xFFA0AEC0),
            savingsLabel = "15%",
            periodSuffix = periodSuffix,
            features = goldPlanFeatures(),
        )
    }
}

/** Hides StoreKit Configuration / App Store Connect debug copy from the UI. */
private fun StoreSubscriptionProduct.sanitizedPlanTitle(): String {
    val raw = displayName.trim()
    if (raw.isBlank() || isStoreKitDebugCopy(raw)) {
        return when (planTier) {
            StorePlanTier.Silver -> "Silver"
            StorePlanTier.Gold -> "Gold Plan"
            StorePlanTier.Unknown -> "Plan"
        }
    }
    return raw
}

private fun isStoreKitDebugCopy(text: String): Boolean {
    val lower = text.lowercase()
    return lower.contains("storekit") ||
        lower.contains("app store connect") ||
        lower.contains("local storekit") ||
        lower.contains("product id must match")
}

private fun silverPlanFeatures(): List<String> = listOf(
    "Upto 20 members",
    "No access control to the trainer",
    "TBD",
    "TBD",
    "No biometric",
)

private fun goldPlanFeatures(): List<String> = listOf(
    "Unlimited members",
    "Access control propagation",
    "TBD",
    "TBD",
    "No biometric",
)

private fun StorePlanTier.sortOrder(): Int = when (this) {
    StorePlanTier.Silver -> 0
    StorePlanTier.Gold -> 1
    StorePlanTier.Unknown -> 2
}

@Composable
private fun IosOnlySubscriptionMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Subscriptions,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Gray.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "App Store Subscriptions",
            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Subscription plans are available through the App Store on the iOS app.",
            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SubscriptionEmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Subscriptions,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Gray.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
            textAlign = TextAlign.Center,
        )
    }
}

private fun StoreSubscriptionProduct.matchesBillingCycle(cycle: String): Boolean = when (period) {
    StoreBillingPeriod.Unknown -> true
    StoreBillingPeriod.Monthly -> cycle == "Monthly"
    StoreBillingPeriod.Yearly -> cycle == "Yearly"
}

private fun emptyStateMessageForCycle(cycle: String): String = when (cycle) {
    "Monthly" ->
        "No monthly subscription plans are available from the App Store.\nCheck your App Store Connect product setup."
    "Yearly" ->
        "No yearly subscription plans are available from the App Store.\nCheck your App Store Connect product setup."
    else -> "No subscription plans are available for this billing cycle."
}

private fun StoreSubscriptionStatus.isActiveProduct(productId: String): Boolean = when (this) {
    is StoreSubscriptionStatus.Active -> this.productId == productId
    is StoreSubscriptionStatus.CancelledButActive -> this.productId == productId
    else -> false
}
