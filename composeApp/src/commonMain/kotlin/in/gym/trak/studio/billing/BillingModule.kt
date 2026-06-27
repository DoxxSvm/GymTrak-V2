package `in`.gym.trak.studio.billing

import com.multiplatform.inAppPurchase.IAPManager
import kotlin.getValue


object BillingModule {
    val iapManager: IAPManager by lazy { IAPManager() }
}
