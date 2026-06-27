package `in`.gym.trak.studio.billing.appstore

import com.multiplatform.inAppPurchase.model.IAPResult
import com.multiplatform.inAppPurchase.model.Product
import `in`.gym.trak.studio.billing.BillingModule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class UnifiedAppStoreSubscriptionRepository : AppStoreSubscriptionRepository {

    private val iapManager = BillingModule.iapManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _updates = MutableSharedFlow<StoreTransactionEvent>(extraBufferCapacity = 32)
    private val _purchaseUi = MutableSharedFlow<StorePurchaseUiEvent>(extraBufferCapacity = 64)

    /** True once iapManager.initialize() has completed successfully. */
    private val _initialized = MutableStateFlow(false)

    private val storeKitReady = CompletableDeferred<Unit>()

    init {
        scope.launch {
            when (val result = iapManager.initialize()) {
                is IAPResult.Success -> {
                    println("[IAP] StoreKit 2 initialized successfully")
                    _initialized.value = true
                    storeKitReady.complete(Unit)
                    observePurchasePipeline()
                }
                is IAPResult.Error -> {
                    println("[IAP] StoreKit 2 initialization failed: ${result.message}")
                    storeKitReady.completeExceptionally(
                        Exception(result.message ?: "StoreKit initialization failed"),
                    )
                }
            }
        }
    }

    override suspend fun awaitStoreKitReady(): Result<Unit> = runCatching { storeKitReady.await() }

    private fun observePurchasePipeline() {
        // Mirrors Flutter `purchaseStream`: finish transactions (acknowledge) and surface UI events.
        scope.launch {
            iapManager.getPurchaseUpdates().collect { purchase ->
                val pid = purchase.productId
                if (pid.isBlank()) return@collect
                when (val ack = iapManager.acknowledgePurchase(purchase)) {
                    is IAPResult.Success -> {
                        _purchaseUi.emit(
                            StorePurchaseUiEvent(pid, StorePurchaseStreamStatus.Purchased),
                        )
                        _updates.emit(
                            StoreTransactionEvent(
                                productId = pid,
                                kind = StoreTransactionUpdateKind.Purchased,
                                transactionId = purchase.orderId.ifBlank { null },
                                originalTransactionId = purchase.orderId.ifBlank { null },
                            ),
                        )
                    }
                    is IAPResult.Error -> {
                        println("[IAP] acknowledgePurchase failed for $pid: ${ack.message}")
                        _purchaseUi.emit(
                            StorePurchaseUiEvent(
                                pid,
                                StorePurchaseStreamStatus.Error,
                                ack.message,
                            ),
                        )
                    }
                }
            }
        }
    }

    override suspend fun loadProducts(productIds: List<String>): Result<List<StoreSubscriptionProduct>> {
        val requested = productIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (requested.isEmpty()) {
            return Result.failure(Exception("No App Store product IDs to query."))
        }

        if (!isAppleAppStoreBillingAvailable()) {
            logAppStoreAvailability(available = false, detail = "not iOS")
            return Result.failure(Exception("App Store is only available on iOS."))
        }

        val ready = awaitStoreKitReady()
        if (ready.isFailure) {
            logAppStoreAvailability(available = false, detail = ready.exceptionOrNull()?.message)
            return Result.failure(ready.exceptionOrNull() ?: Exception("App Store is not available."))
        }
        logAppStoreAvailability(available = true, detail = "StoreKit ready")

        return when (val result = iapManager.getProducts(requested)) {
            is IAPResult.Success -> {
                val requestedSet = requested.toSet()
                val loadedIds = result.data.map { it.id }.toSet()
                val notFound = (requestedSet - loadedIds).toList()
                if (isAppleAppStoreBillingAvailable()) {
                    logAppStoreProductLoadContext(requested, loadedIds.toList(), notFound)
                }
                result.data.forEach { p ->
                    if (isAppleAppStoreBillingAvailable()) {
                        logAppStoreProductDetails(p)
                    }
                }
                val products = result.data.map { p -> p.toStoreSubscriptionProduct() }
                println(
                    "[IAP] Loaded ${products.size}/${requestedSet.size} product(s): " +
                        products.joinToString { "${it.productId} @ ${it.displayPrice} (${it.currencyCode})" },
                )
                if (products.isEmpty()) {
                    Result.failure(
                        Exception(
                            buildProductsNotFoundMessage(requested, notFound),
                        ),
                    )
                } else {
                    Result.success(products)
                }
            }
            is IAPResult.Error -> {
                println("[IAP] queryProductDetails failed: ${result.message}")
                Result.failure(Exception(result.message ?: "Failed to load products from App Store"))
            }
        }
    }

    private fun buildProductsNotFoundMessage(requested: List<String>, notFound: List<String>): String =
        buildString {
            appendLine("App Store returned no products.")
            appendLine("Requested: $requested")
            if (notFound.isNotEmpty()) appendLine("Not found: $notFound")
            append(
                "Confirm product IDs in App Store Connect (gold, silver) match exactly, " +
                    "products are added to this app version, and use an App Store sandbox account.",
            )
        }

    override suspend fun purchase(productId: String): Result<StorePurchaseResult> {
        awaitStoreKitReady().getOrElse { return Result.failure(it) }
        val productsResult = iapManager.getProducts(listOf(productId))
        if (productsResult is IAPResult.Error) {
            return Result.failure(Exception(productsResult.message ?: "Could not fetch product"))
        }

        val product = (productsResult as IAPResult.Success).data.firstOrNull()
            ?: return Result.failure(Exception("Product '$productId' not found in App Store"))

        return when (val result = iapManager.launchPurchaseFlow(product)) {
            is IAPResult.Success -> Result.success(StorePurchaseResult.Success)
            is IAPResult.Error -> {
                when (result.code) {
                    2 -> Result.success(StorePurchaseResult.UserCancelled)
                    3 -> Result.success(StorePurchaseResult.Pending)
                    else -> Result.failure(Exception(result.message ?: "Purchase failed"))
                }
            }
        }
    }

    override suspend fun restorePurchases(): Result<Unit> {
        awaitStoreKitReady().getOrElse { return Result.failure(it) }
        return when (val result = iapManager.restorePurchases()) {
            is IAPResult.Success -> {
                println("[IAP] Restored ${result.data.size} purchase(s)")
                result.data.forEach { p ->
                    val pid = p.productId
                    if (pid.isBlank()) return@forEach
                    _purchaseUi.emit(
                        StorePurchaseUiEvent(pid, StorePurchaseStreamStatus.Restored),
                    )
                    when (val ack = iapManager.acknowledgePurchase(p)) {
                        is IAPResult.Error ->
                            println("[IAP] acknowledge after restore failed for $pid: ${ack.message}")
                        else -> Unit
                    }
                }
                Result.success(Unit)
            }
            is IAPResult.Error -> {
                println("[IAP] restorePurchases failed: ${result.message}")
                Result.failure(Exception(result.message ?: "Restore failed"))
            }
        }
    }

    override suspend fun refreshSubscriptionStatus(): Result<StoreSubscriptionStatus> {
        awaitStoreKitReady().getOrElse {
            return Result.success(StoreSubscriptionStatus.NotSubscribed)
        }
        return when (val result = iapManager.getPurchases()) {
            is IAPResult.Success -> {
                val activePurchase = result.data.firstOrNull()
                val status = if (activePurchase != null) {
                    StoreSubscriptionStatus.Active(
                        productId = activePurchase.productId,
                        planTier = parseTierFromProductId(activePurchase.productId),
                        period = parsePeriodFromProductId(activePurchase.productId),
                        expiresAt = null,
                        willAutoRenew = true,
                        isInGracePeriod = false,
                        isInBillingRetry = false,
                        originalTransactionId = activePurchase.orderId.ifBlank { null },
                    )
                } else {
                    StoreSubscriptionStatus.NotSubscribed
                }
                Result.success(status)
            }
            is IAPResult.Error -> {
                println("[IAP] refreshSubscriptionStatus failed: ${result.message}")
                Result.success(StoreSubscriptionStatus.NotSubscribed)
            }
        }
    }

    override fun transactionUpdates(): Flow<StoreTransactionEvent> = _updates.asSharedFlow()

    override fun storePurchaseUiEvents(): Flow<StorePurchaseUiEvent> = _purchaseUi.asSharedFlow()

    private fun Product.toStoreSubscriptionProduct(): StoreSubscriptionProduct =
        StoreSubscriptionProduct(
            productId = id,
            planTier = parseTierFromProductId(id),
            period = parsePeriodFromProductId(id),
            displayName = title,
            description = description,
            displayPrice = price,
            currencyCode = priceCurrencyCode,
            priceAmountMicros = priceAmountMicros,
        )

    private fun parseTierFromProductId(id: String): StorePlanTier = when {
        id.equals(AppStoreProductIds.GOLD, ignoreCase = true) -> StorePlanTier.Gold
        id.equals(AppStoreProductIds.SILVER, ignoreCase = true) -> StorePlanTier.Silver
        id.contains("gold", ignoreCase = true) -> StorePlanTier.Gold
        id.contains("silver", ignoreCase = true) -> StorePlanTier.Silver
        else -> StorePlanTier.Unknown
    }

    private fun parsePeriodFromProductId(id: String): StoreBillingPeriod = when {
        id.contains("year", ignoreCase = true) -> StoreBillingPeriod.Yearly
        id.contains("month", ignoreCase = true) -> StoreBillingPeriod.Monthly
        else -> StoreBillingPeriod.Unknown
    }
}
