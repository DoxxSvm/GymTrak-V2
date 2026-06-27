package `in`.gym.trak.studio.billing.appstore

import com.multiplatform.inAppPurchase.model.Product

internal actual fun logAppStoreAvailability(available: Boolean, detail: String?) {
    println("[IAP][Store] isAvailable=$available ${detail?.let { "($it)" } ?: ""}")
    if (!available) {
        println("[IAP][Store] App Store purchases are unavailable — use a physical device with a signed-in Apple ID.")
    }
}

internal actual fun logAppStoreProductLoadContext(
    requestedProductIds: List<String>,
    foundProductIds: List<String>,
    notFoundProductIds: List<String>,
) {
    println("[IAP][Store] ── queryProductDetails ──")
    println("[IAP][Store] Product IDs requested: $requestedProductIds")
    println("[IAP][Store] Product IDs found: $foundProductIds")
    if (notFoundProductIds.isNotEmpty()) {
        println("[IAP][Store] Product IDs NOT found: $notFoundProductIds")
        println(
            "[IAP][Store] Ensure IDs match App Store Connect exactly (current: gold, silver) " +
                "and products are submitted with an app version if still in draft.",
        )
    }
    println(
        "[IAP][Store] Prices use the signed-in App Store account region (currencyCode on each product).",
    )
}

internal actual fun logAppStoreProductDetails(product: Product) {
    val raw = product.rawPriceAmount()
    val regionHint = storefrontHintFromCurrency(product.priceCurrencyCode)

    println("[IAP][Product] ─────────────────────────────")
    println("[IAP][Product] product.id=${product.id}")
    println("[IAP][Product] product.title=${product.title}")
    println("[IAP][Product] product.description=${product.description}")
    println("[IAP][Product] product.price=${product.price}")
    println("[IAP][Product] product.rawPrice=$raw")
    println("[IAP][Product] product.currencyCode=${product.priceCurrencyCode}")
    println("[IAP][Product] product.priceAmountMicros=${product.priceAmountMicros}")
    println("[IAP][Product] storeRegionHint=$regionHint")
    println("[IAP][Product] product.type=${product.type}")

    when {
        product.priceCurrencyCode.equals("INR", ignoreCase = true) -> {
            println("[IAP][Product] ✅ INR pricing from App Store.")
        }
        product.price.isNotBlank() -> {
            println("[IAP][Product] currency=${product.priceCurrencyCode} formatted=${product.price}")
        }
    }
}

private fun storefrontHintFromCurrency(currencyCode: String): String = when (currencyCode.uppercase()) {
    "INR" -> "India (INR)"
    "USD" -> "United States (USD)"
    else -> "currency=$currencyCode"
}
