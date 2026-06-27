package `in`.gym.trak.studio.billing.appstore

/** Android stub — App Store INR display is iOS-only; purchase uses Play Billing separately. */
internal actual suspend fun resolveInrProductPrice(
    amountMicros: Long,
    currencyCode: String,
    localizedDisplayPrice: String,
): InrProductPrice {
    val originalPrice = amountMicros / 1_000_000.0
    return InrProductPrice(
        originalPrice = originalPrice,
        originalCurrency = currencyCode,
        inrPrice = originalPrice,
        formattedInrPrice = localizedDisplayPrice,
    )
}
