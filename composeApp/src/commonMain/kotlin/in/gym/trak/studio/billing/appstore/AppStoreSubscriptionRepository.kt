package `in`.gym.trak.studio.billing.appstore

import kotlinx.coroutines.flow.Flow

/**
 * Apple App Store subscriptions (StoreKit 2 on iOS). On Android this implementation is a stub.
 */
interface AppStoreSubscriptionRepository {
    /** Wait until StoreKit / IAPManager has finished [initialize]; required before querying products. */
    suspend fun awaitStoreKitReady(): Result<Unit>

    suspend fun loadProducts(productIds: List<String> = AppStoreProductIds.all): Result<List<StoreSubscriptionProduct>>

    /**
     * Loads App Store products by [appleProductIds] from your API (`apple_product_id` field).
     * Equivalent to Flutter `InAppPurchase.instance.queryProductDetails(ids.toSet())`.
     */
    suspend fun loadProductsByAppleProductIds(
        appleProductIds: List<String>,
    ): Result<List<StoreSubscriptionProduct>> = loadProducts(
        productIds = appleProductIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
    )

    suspend fun purchase(productId: String): Result<StorePurchaseResult>

    /** StoreKit 2: re-sync with App Store (restore). */
    suspend fun restorePurchases(): Result<Unit>

    /** Current entitlements + renewal metadata reflected as [StoreSubscriptionStatus]. */
    suspend fun refreshSubscriptionStatus(): Result<StoreSubscriptionStatus>

    /**
     * Live transaction updates (renewals, refunds, etc.). On iOS this is backed by
     * [Transaction.updates]. Collect from a long-lived scope (e.g. ViewModel).
     */
    fun transactionUpdates(): Flow<StoreTransactionEvent>

    /**
     * Purchase stream for UI / analytics (mirrors Flutter `purchaseStream` listener).
     * Emits after StoreKit updates are processed where applicable.
     */
    fun storePurchaseUiEvents(): Flow<StorePurchaseUiEvent>
}

private val unifiedRepo by lazy { UnifiedAppStoreSubscriptionRepository() }

fun createAppStoreSubscriptionRepository(): AppStoreSubscriptionRepository = unifiedRepo

