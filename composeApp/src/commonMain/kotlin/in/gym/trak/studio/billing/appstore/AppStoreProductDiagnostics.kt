package `in`.gym.trak.studio.billing.appstore

import com.multiplatform.inAppPurchase.model.Product

internal expect fun logAppStoreAvailability(available: Boolean, detail: String?)

internal expect fun logAppStoreProductLoadContext(
    requestedProductIds: List<String>,
    foundProductIds: List<String>,
    notFoundProductIds: List<String>,
)

internal expect fun logAppStoreProductDetails(product: Product)

internal fun Product.rawPriceAmount(): Double = priceAmountMicros / 1_000_000.0
