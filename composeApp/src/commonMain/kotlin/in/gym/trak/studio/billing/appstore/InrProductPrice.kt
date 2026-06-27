package `in`.gym.trak.studio.billing.appstore

import com.multiplatform.inAppPurchase.model.Product

/**
 * INR display pricing for an App Store product (Flutter `ProductDetails` equivalent).
 * Purchase still uses the original StoreKit product — this is display-only.
 */
data class InrProductPrice(
    val originalPrice: Double,
    val originalCurrency: String,
    val inrPrice: Double,
    val formattedInrPrice: String,
    /** True when [inrPrice] came from a live FX conversion (non-INR storefront). */
    val convertedFromRemote: Boolean = false,
    /** True when conversion failed and [formattedInrPrice] is a fallback. */
    val conversionFailed: Boolean = false,
)

/**
 * Resolves App Store product pricing for UI display in Indian Rupees.
 *
 * Maps Flutter `in_app_purchase` fields:
 * - [Product.priceAmountMicros] → `rawPrice` (millionths)
 * - [Product.priceCurrencyCode] → `currencyCode`
 */
object AppStoreInrPriceHelper {

    suspend fun getPriceInINR(product: Product): InrProductPrice =
        resolveInrProductPrice(
            amountMicros = product.priceAmountMicros,
            currencyCode = product.priceCurrencyCode,
            localizedDisplayPrice = product.price,
        )

    suspend fun getPriceInINR(
        amountMicros: Long,
        currencyCode: String,
        localizedDisplayPrice: String,
    ): InrProductPrice = resolveInrProductPrice(amountMicros, currencyCode, localizedDisplayPrice)
}

/** iOS: FX conversion when needed. Android: passthrough (not used for App Store UI). */
internal expect suspend fun resolveInrProductPrice(
    amountMicros: Long,
    currencyCode: String,
    localizedDisplayPrice: String,
): InrProductPrice
