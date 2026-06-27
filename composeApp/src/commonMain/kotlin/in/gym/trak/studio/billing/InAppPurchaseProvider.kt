package `in`.gym.trak.studio.billing

import com.multiplatform.inAppPurchase.IAPManager
import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import com.multiplatform.inAppPurchase.model.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class InAppPurchaseProvider(
    private val inAppPurchaseManager: IAPManager = BillingModule.iapManager
) {
    init {
        observePurchaseUpdates()
    }

    suspend fun initialize() {
        inAppPurchaseManager.initialize()
    }

    private fun observePurchaseUpdates() {
        CoroutineScope(Dispatchers.Main).launch {
            inAppPurchaseManager.getPurchaseUpdates().collect { purchase ->
                handlePurchase(purchase)
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        // Here you would verify the purchase on your backend if needed
        // For simplicity, we acknowledge it directly if it's not already processed
        acknowledgePurchaseIfNeeded(purchase)
    }

    private suspend fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        val acknowledgeResult = inAppPurchaseManager.acknowledgePurchase(purchase)
        when (acknowledgeResult) {
            is IAPResult.Success -> {
                println("Purchase successful! Product ID: ${purchase.productId}")
            }
            is IAPResult.Error -> {
            }

            else -> {}
        }
    }

    suspend fun fetchProducts(productIds: List<String>): IAPResult<List<Product>> {
        return inAppPurchaseManager.getProducts(productIds)
    }

    suspend fun purchase(product: Product): IAPResult<Unit> {
        return inAppPurchaseManager.launchPurchaseFlow(product)
    }
}
