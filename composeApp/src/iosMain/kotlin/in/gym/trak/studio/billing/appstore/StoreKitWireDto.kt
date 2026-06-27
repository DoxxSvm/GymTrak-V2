package `in`.gym.trak.studio.billing.appstore

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WireError(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
internal data class WireProduct(
    val productId: String,
    val displayName: String,
    val description: String,
    val displayPrice: String,
    val planTier: String,
    val period: String,
)

@Serializable
internal data class LoadProductsResponse(
    val requestId: String,
    val ok: Boolean,
    val products: List<WireProduct>? = null,
    val error: WireError? = null,
)

@Serializable
internal data class PurchaseResponse(
    val requestId: String,
    val ok: Boolean,
    val result: String? = null,
    val error: WireError? = null,
)

@Serializable
internal data class SimpleOkResponse(
    val requestId: String,
    val ok: Boolean,
    val error: WireError? = null,
)

@Serializable
internal data class WireSubscription(
    val productId: String,
    val expirationDateMillis: Long? = null,
    val originalTransactionId: String? = null,
    val willAutoRenew: Boolean = true,
    val renewalState: String,
    val isActive: Boolean,
    val isInGracePeriod: Boolean = false,
    val isInBillingRetryPeriod: Boolean = false,
)

@Serializable
internal data class StatusResponse(
    val requestId: String,
    val ok: Boolean,
    val subscriptions: List<WireSubscription>? = null,
    val error: WireError? = null,
)

@Serializable
internal data class TransactionEventPayload(
    val productId: String,
    val kind: String,
    val transactionId: String? = null,
    val originalTransactionId: String? = null,
)
