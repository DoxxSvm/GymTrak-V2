package `in`.gym.trak.studio.billing.appstore

import com.multiplatform.inAppPurchase.model.Product

internal actual fun logAppStoreAvailability(available: Boolean, detail: String?) = Unit

internal actual fun logAppStoreProductLoadContext(
    requestedProductIds: List<String>,
    foundProductIds: List<String>,
    notFoundProductIds: List<String>,
) = Unit

internal actual fun logAppStoreProductDetails(product: Product) = Unit
