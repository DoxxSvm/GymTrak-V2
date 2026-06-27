package `in`.gym.trak.studio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GymTrak owner subscription plan from backend — [apple_product_id] is queried via StoreKit on iOS.
 */
@Serializable
data class GymTrakSubscriptionPlanDto(
    val id: String = "",
    val name: String = "",
    @SerialName("billing_cycle")
    val billingCycle: String? = null,
    @SerialName("apple_product_id")
    val appleProductId: String? = null,
)

@Serializable
data class GymTrakSubscriptionPlanListResponse(
    val items: List<GymTrakSubscriptionPlanDto> = emptyList(),
)

fun List<GymTrakSubscriptionPlanDto>.appleProductIds(): List<String> =
    mapNotNull { plan -> plan.appleProductId?.trim()?.takeIf { it.isNotEmpty() } }.distinct()
