package `in`.gym.trak.studio.billing.appstore

actual fun isAppStoreSubscriptionsSupported(): Boolean = true

/**
 * StoreKit is available on iOS. In Xcode, enable the **In-App Purchase** capability for the
 * iOS application target (Signing & Capabilities). The Xcode project already links StoreKit.framework.
 */
actual fun isAppleAppStoreBillingAvailable(): Boolean = true
