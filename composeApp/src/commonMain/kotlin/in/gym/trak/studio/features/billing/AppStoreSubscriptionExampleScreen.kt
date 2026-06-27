package `in`.gym.trak.studio.features.billing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.billing.appstore.GoldStoreProductQueryState
import `in`.gym.trak.studio.billing.appstore.StoreSubscriptionStatus
import `in`.gym.trak.studio.billing.appstore.isAppleAppStoreBillingAvailable
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.viewmodel.billing.AppStoreSubscriptionScreenModel

/**
 * Example UI for Gold / Silver monthly & yearly App Store subscriptions.
 * On Android this screen shows a short notice; on iOS it drives StoreKit 2 via [AppStoreSubscriptionScreenModel].
 */
class AppStoreSubscriptionExampleScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val model = rememberScreenModel { AppStoreSubscriptionScreenModel() }
        val products by model.products.collectAsState()
        val status by model.status.collectAsState()
        val isLoading by model.isLoading.collectAsState()

        LaunchedEffect(Unit) {
            if (isAppleAppStoreBillingAvailable()) {
                model.loadAppStoreConnectProducts(showGlobalLoader = true)
                model.refreshStatus(showLoader = false)
            }
        }

        val goldQuery by model.goldStoreProductQueryState.collectAsState()

        LoadingScreenHandler(screenModel = model) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Premium Plans",
                        onBackClick = { navigator?.pop() },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (!isAppleAppStoreBillingAvailable()) {
                        Text(
                            "In-app subscriptions are implemented for iOS with StoreKit 2. " +
                                "Use the Android billing APIs separately if you offer Play subscriptions.",
                            style = AppTextTheme.regular,
                        )
                        return@Column
                    }

                    Text("Product query (gold)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val goldQueryState = goldQuery
                    when (goldQueryState) {
                        GoldStoreProductQueryState.Idle, GoldStoreProductQueryState.Loading ->
                            Text("Loading App Store catalog…", style = AppTextTheme.medium)
                        GoldStoreProductQueryState.StoreUnavailable ->
                            Text("App Store unavailable", style = AppTextTheme.medium, color = PrimaryColor)
                        is GoldStoreProductQueryState.ProductsNotFound ->
                            Text("No products found", style = AppTextTheme.medium, color = PrimaryColor)
                        is GoldStoreProductQueryState.QueryError ->
                            Text("Error: ${goldQueryState.message}", style = AppTextTheme.medium, color = PrimaryColor)
                        is GoldStoreProductQueryState.Error ->
                            Text("Error: ${goldQueryState.message}", style = AppTextTheme.medium, color = PrimaryColor)
                        is GoldStoreProductQueryState.Success ->
                            Text(
                                "Loaded ${goldQueryState.products.size} SKU(s)" +
                                    if (goldQueryState.notFoundProductIds.isNotEmpty()) {
                                        " (missing: ${goldQueryState.notFoundProductIds})"
                                    } else "",
                                style = AppTextTheme.medium,
                            )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Current status", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(statusLabel(status), style = AppTextTheme.medium)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { model.restorePurchases() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Restore purchases")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Products", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading && products.isEmpty() && goldQuery is GoldStoreProductQueryState.Loading) {
                        CircularProgressIndicator(color = PrimaryColor)
                    } else {
                        products.forEach { p ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { model.purchase(p.productId) }
                                    .padding(vertical = 12.dp),
                            ) {
                                Text("${p.planTier} · ${p.period}", style = AppTextTheme.bold)
                                Text(p.displayName, style = AppTextTheme.medium)
                                if (p.description.isNotBlank()) {
                                    Text(p.description, style = AppTextTheme.regular, color = PrimaryColor)
                                }
                                Text(p.displayPrice, color = PrimaryColor, style = AppTextTheme.bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                }
            }
        }
    }
}

private fun statusLabel(s: StoreSubscriptionStatus): String = when (s) {
    is StoreSubscriptionStatus.NotSubscribed -> "Not subscribed"
    is StoreSubscriptionStatus.Active ->
        "Active: ${s.planTier} (${s.period}) — renews: ${s.willAutoRenew}, grace: ${s.isInGracePeriod}, billing retry: ${s.isInBillingRetry}"
    is StoreSubscriptionStatus.Expired -> "Expired (last: ${s.lastKnownProductId})"
    is StoreSubscriptionStatus.Revoked -> "Revoked (product: ${s.productId})"
    is StoreSubscriptionStatus.CancelledButActive ->
        "Cancelled but active until expiry — ${s.planTier} (${s.period})"
}
