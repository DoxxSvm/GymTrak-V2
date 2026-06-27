package `in`.gym.trak.studio.billing.appstore

import kotlinx.datetime.Instant

enum class StorePlanTier {
    Gold,
    Silver,
    Unknown,
}

enum class StoreBillingPeriod {
    Monthly,
    Yearly,
    Unknown,
}

data class StoreSubscriptionProduct(
    val productId: String,
    val planTier: StorePlanTier,
    val period: StoreBillingPeriod,
    val displayName: String,
    val description: String,
    /** Localized price string from StoreKit (user's App Store region). */
    val displayPrice: String,
    /** ISO 4217 currency from StoreKit (e.g. `INR` for India storefront). */
    val currencyCode: String,
    /** Price in millionths of the currency unit (same as Play Billing / Flutter `rawPrice`). */
    val priceAmountMicros: Long,
) {
    /** Apple-localized price string exactly as returned by StoreKit ([displayPrice]). */
    val priceForDisplay: String
        get() = displayPrice

    /** Same as Flutter `ProductDetails.rawPrice`. */
    val rawPrice: Double
        get() = priceAmountMicros / 1_000_000.0
}

/**
 * Normalized subscription state for the shared layer.
 * Map from StoreKit entitlements + renewal info on iOS.
 */
sealed class StoreSubscriptionStatus {
    data object NotSubscribed : StoreSubscriptionStatus()

    data class Active(
        val productId: String,
        val planTier: StorePlanTier,
        val period: StoreBillingPeriod,
        val expiresAt: Instant?,
        val willAutoRenew: Boolean,
        val isInBillingRetry: Boolean,
        val isInGracePeriod: Boolean,
        val originalTransactionId: String?,
    ) : StoreSubscriptionStatus()

    /** Subscription lapsed (no active entitlement). */
    data class Expired(
        val lastKnownProductId: String?,
    ) : StoreSubscriptionStatus()

    /** Family sharing / refund / other revocation. */
    data class Revoked(
        val productId: String?,
    ) : StoreSubscriptionStatus()

    /** Still entitled but auto-renew is off; user keeps access until [expiresAt]. */
    data class CancelledButActive(
        val productId: String,
        val planTier: StorePlanTier,
        val period: StoreBillingPeriod,
        val expiresAt: Instant?,
        val originalTransactionId: String?,
    ) : StoreSubscriptionStatus()
}

sealed class StorePurchaseResult {
    data object Success : StorePurchaseResult()
    data object UserCancelled : StorePurchaseResult()

    /** Ask the customer to approve (Ask to Buy, etc.). Listen to [AppStoreSubscriptionRepository.transactionUpdates]. */
    data object Pending : StorePurchaseResult()
}

enum class StoreTransactionUpdateKind {
    Purchased,
    Renewed,
    Expired,
    Revoked,
    Refunded,
    Upgraded,
    Other,
}

/**
 * High-level purchase listener states (aligned with Flutter `PurchaseDetails.status` concepts).
 */
enum class StorePurchaseStreamStatus {
    Pending,
    Purchased,
    Restored,
    Error,
    Canceled,
}

data class StorePurchaseUiEvent(
    val productId: String,
    val status: StorePurchaseStreamStatus,
    val message: String? = null,
)

/** UI state for App Store product query (Flutter `queryProductDetails` equivalent). */
sealed class GoldStoreProductQueryState {
    data object Idle : GoldStoreProductQueryState()
    data object Loading : GoldStoreProductQueryState()
    data class Success(
        val products: List<StoreSubscriptionProduct>,
        val notFoundProductIds: List<String> = emptyList(),
    ) : GoldStoreProductQueryState()

    data object StoreUnavailable : GoldStoreProductQueryState()

    data class ProductsNotFound(
        val requestedIds: List<String>,
        val notFoundIds: List<String>,
    ) : GoldStoreProductQueryState()

    data class QueryError(val message: String) : GoldStoreProductQueryState()

    /** Generic error (e.g. iOS-only guard). */
    data class Error(val message: String) : GoldStoreProductQueryState()
}

data class StoreTransactionEvent(
    val productId: String,
    val kind: StoreTransactionUpdateKind,
    val transactionId: String?,
    val originalTransactionId: String?,
)

sealed class StoreSubscriptionError : Exception() {
    class Network : StoreSubscriptionError()
    class ProductUnavailable : StoreSubscriptionError()
    class NotAllowed : StoreSubscriptionError()
    class Unknown(override val message: String) : StoreSubscriptionError()
}
