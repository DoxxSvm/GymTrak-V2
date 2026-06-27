package `in`.gym.trak.studio.billing.appstore

import `in`.gym.trak.studio.base.Constants
import kotlin.math.roundToInt

/**
 * Formats App Store product prices for display in the app (INR-first for GymTrak).
 *
 * StoreKit returns localized [localizedDisplayPrice] and numeric [amountMicros] with [currencyCode].
 * When the storefront is India, [currencyCode] is `INR` and we format with [Constants.RUPEE].
 */
object StoreProductPriceFormatter {

    fun formatForInr(
        amountMicros: Long,
        currencyCode: String,
        localizedDisplayPrice: String,
    ): String {
        if (currencyCode.equals("INR", ignoreCase = true) && amountMicros > 0L) {
            return formatInrFromMicros(amountMicros)
        }
        if (localizedDisplayPrice.isNotBlank()) {
            return localizedDisplayPrice
        }
        if (amountMicros > 0L && currencyCode.isNotBlank()) {
            return formatGeneric(amountMicros, currencyCode)
        }
        return localizedDisplayPrice
    }

    fun formatInrFromMicros(amountMicros: Long): String =
        formatInrFromAmount(amountMicros / 1_000_000.0)

    fun formatInrFromAmount(amountInr: Double): String {
        if (amountInr <= 0.0) return "${Constants.RUPEE}0"
        val whole = amountInr.toLong()
        val cents = ((amountInr - whole) * 100).roundToInt()
        return if (cents == 0) {
            "${Constants.RUPEE}$whole"
        } else {
            "${Constants.RUPEE}$whole.${cents.toString().padStart(2, '0')}"
        }
    }

    private fun formatGeneric(amountMicros: Long, currencyCode: String): String {
        val amount = amountMicros / 1_000_000.0
        val whole = amount.toLong()
        val cents = ((amount - whole) * 100).roundToInt()
        val numeric = if (cents == 0) whole.toString() else "$whole.${cents.toString().padStart(2, '0')}"
        return "$currencyCode $numeric"
    }
}
