package `in`.gym.trak.studio.viewmodel.billing

import cafe.adriel.voyager.core.model.screenModelScope
import `in`.gym.trak.studio.billing.appstore.AppStoreProductIds
import `in`.gym.trak.studio.billing.appstore.AppStoreSubscriptionRepository
import `in`.gym.trak.studio.billing.appstore.GoldStoreProductQueryState
import `in`.gym.trak.studio.billing.appstore.StorePurchaseResult
import `in`.gym.trak.studio.billing.appstore.StorePurchaseStreamStatus
import `in`.gym.trak.studio.billing.appstore.StoreSubscriptionProduct
import `in`.gym.trak.studio.billing.appstore.StoreSubscriptionStatus
import `in`.gym.trak.studio.billing.appstore.createAppStoreSubscriptionRepository
import `in`.gym.trak.studio.billing.appstore.isAppleAppStoreBillingAvailable
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Apple App Store in-app products (StoreKit 2 via inAppPurchase-kmp).
 * Loads [AppStoreProductIds.appStoreConnect] from App Store Connect — no API prices on iOS.
 */
class AppStoreSubscriptionScreenModel(
    private val repository: AppStoreSubscriptionRepository = createAppStoreSubscriptionRepository(),
) : BaseScreenModel() {

    private val _products = MutableStateFlow<List<StoreSubscriptionProduct>>(emptyList())
    val products: StateFlow<List<StoreSubscriptionProduct>> = _products.asStateFlow()

    private val _status = MutableStateFlow<StoreSubscriptionStatus>(StoreSubscriptionStatus.NotSubscribed)
    val status: StateFlow<StoreSubscriptionStatus> = _status.asStateFlow()

    private val _goldStoreProductQueryState =
        MutableStateFlow<GoldStoreProductQueryState>(GoldStoreProductQueryState.Idle)
    val goldStoreProductQueryState: StateFlow<GoldStoreProductQueryState> =
        _goldStoreProductQueryState.asStateFlow()

    private var lastRequestedIds: List<String> = emptyList()

    init {
        screenModelScope.launch {
            repository.transactionUpdates().collect {
                refreshStatus(showLoader = false)
            }
        }
        screenModelScope.launch {
            repository.storePurchaseUiEvents().collect { event ->
                val label = event.productId.ifBlank { AppStoreProductIds.GOLD }
                when (event.status) {
                    StorePurchaseStreamStatus.Pending ->
                        showToast("Purchase pending approval ($label)")
                    StorePurchaseStreamStatus.Purchased -> {
                        showToast("Purchase completed ($label)")
                        refreshStatus(showLoader = false)
                    }
                    StorePurchaseStreamStatus.Restored -> {
                        showToast("Purchase restored ($label)")
                        refreshStatus(showLoader = false)
                    }
                    StorePurchaseStreamStatus.Canceled ->
                        showToast("Purchase canceled")
                    StorePurchaseStreamStatus.Error ->
                        showToast(event.message ?: "Purchase error ($label)")
                }
            }
        }
    }

    /** Query App Store Connect product IDs (Flutter `queryProductDetails`). */
    fun loadAppStoreConnectProducts(showGlobalLoader: Boolean = false) {
        loadProducts(productIds = AppStoreProductIds.appStoreConnect, showGlobalLoader = showGlobalLoader)
    }

    fun loadProducts(
        productIds: List<String> = AppStoreProductIds.appStoreConnect,
        showGlobalLoader: Boolean = true,
    ) {
        screenModelScope.launch {
            lastRequestedIds = productIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

            if (!isAppleAppStoreBillingAvailable()) {
                _goldStoreProductQueryState.value = GoldStoreProductQueryState.Error(
                    "App Store purchases are available on iOS only.",
                )
                if (showGlobalLoader) hideLoader()
                return@launch
            }

            _goldStoreProductQueryState.value = GoldStoreProductQueryState.Loading
            if (showGlobalLoader) showLoader()
            clearError()

            repository.awaitStoreKitReady().onFailure {
                _goldStoreProductQueryState.value = GoldStoreProductQueryState.StoreUnavailable
                if (showGlobalLoader) hideLoader()
                return@launch
            }

            repository.loadProducts(lastRequestedIds)
                .onSuccess { list ->
                    _products.value = list
                    val loadedIds = list.map { it.productId }.toSet()
                    val notFound = lastRequestedIds.filter { it !in loadedIds }
                    _goldStoreProductQueryState.value = when {
                        list.isEmpty() -> GoldStoreProductQueryState.ProductsNotFound(
                            requestedIds = lastRequestedIds,
                            notFoundIds = notFound.ifEmpty { lastRequestedIds },
                        )
                        notFound.isNotEmpty() -> GoldStoreProductQueryState.Success(
                            products = list,
                            notFoundProductIds = notFound,
                        )
                        else -> GoldStoreProductQueryState.Success(products = list)
                    }
                }
                .onFailure { e ->
                    val msg = e.message ?: "Failed to load products from App Store"
                    _goldStoreProductQueryState.value = when {
                        msg.contains("not available", ignoreCase = true) ->
                            GoldStoreProductQueryState.StoreUnavailable
                        msg.contains("no products", ignoreCase = true) ->
                            GoldStoreProductQueryState.ProductsNotFound(
                                requestedIds = lastRequestedIds,
                                notFoundIds = lastRequestedIds,
                            )
                        else -> GoldStoreProductQueryState.QueryError(msg)
                    }
                    if (showGlobalLoader) showError(msg)
                }

            if (showGlobalLoader) hideLoader()
        }
    }

    fun refreshStatus(showLoader: Boolean = true) {
        screenModelScope.launch {
            if (!isAppleAppStoreBillingAvailable()) return@launch
            if (showLoader) showLoader()
            clearError()
            repository.awaitStoreKitReady().getOrElse {
                if (showLoader) hideLoader()
                return@launch
            }
            repository.refreshSubscriptionStatus()
                .onSuccess { _status.value = it }
                .onFailure { e -> if (showLoader) showError(e.message ?: "Failed to refresh status") }
            if (showLoader) hideLoader()
        }
    }

    fun purchase(productId: String) {
        screenModelScope.launch {
            if (!isAppleAppStoreBillingAvailable()) {
                showError("App Store purchases are available on iOS only.")
                return@launch
            }
            showLoader()
            clearError()
            repository.awaitStoreKitReady().getOrElse { e ->
                hideLoader()
                showError(e.message ?: "App Store is not available")
                return@launch
            }
            val result = repository.purchase(productId)
            hideLoader()
            result.onFailure { e ->
                showError(e.message ?: "Purchase failed")
                return@launch
            }
            when (result.getOrNull()) {
                StorePurchaseResult.Success -> refreshStatus(showLoader = false)
                StorePurchaseResult.UserCancelled -> showToast("Purchase canceled")
                StorePurchaseResult.Pending -> showToast("Purchase pending approval")
                null -> Unit
            }
        }
    }

    fun restorePurchases() {
        screenModelScope.launch {
            if (!isAppleAppStoreBillingAvailable()) {
                showError("Restore is available on iOS only.")
                return@launch
            }
            showLoader()
            clearError()
            repository.awaitStoreKitReady().getOrElse { e ->
                hideLoader()
                showError(e.message ?: "StoreKit is not ready")
                return@launch
            }
            repository.restorePurchases()
                .onSuccess {
                    showToast("Restored purchases from App Store")
                    refreshStatus(showLoader = false)
                }
                .onFailure { e -> showError(e.message ?: "Restore failed") }
            hideLoader()
        }
    }
}
