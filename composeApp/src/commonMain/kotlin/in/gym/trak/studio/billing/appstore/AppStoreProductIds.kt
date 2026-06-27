package `in`.gym.trak.studio.billing.appstore

/**
 * In-App Purchase product identifiers — must match App Store Connect exactly.
 *
 * Current App Store Connect (Distribution → In-App Purchases):
 * - `gold`  (Consumable)
 * - `silver` (Consumable)
 */
object AppStoreProductIds {
    const val GOLD = "gold"
    const val SILVER = "silver"

    /** Product IDs to pass to StoreKit `getProducts` / Flutter `queryProductDetails`. */
    val appStoreConnect: List<String> = listOf(GOLD, SILVER)

    /** @deprecated Use [appStoreConnect] */
    val all: List<String> = appStoreConnect
}

/** True when the device can use Apple IAP (iOS); false on Android and other targets. */
expect fun isAppStoreSubscriptionsSupported(): Boolean

/**
 * True when the Apple App Store storefront / StoreKit billing stack should be used.
 * On Android this is false so we do not run StoreKit-only flows on the wrong platform.
 */
expect fun isAppleAppStoreBillingAvailable(): Boolean
