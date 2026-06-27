package `in`.gym.trak.studio.billing.appstore

import `in`.gym.trak.studio.getCurrentTimeMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Converts non-INR App Store prices to INR for display using Frankfurter (free, no API key).
 * Purchase flow is unchanged — only UI amounts are converted.
 */
internal actual suspend fun resolveInrProductPrice(
    amountMicros: Long,
    currencyCode: String,
    localizedDisplayPrice: String,
): InrProductPrice {
    val originalPrice = amountMicros / 1_000_000.0
    val currency = currencyCode.trim().uppercase()

    if (currency == "INR" || originalPrice <= 0.0) {
        val inr = if (originalPrice > 0.0) originalPrice else 0.0
        return InrProductPrice(
            originalPrice = originalPrice,
            originalCurrency = currency.ifBlank { "INR" },
            inrPrice = inr,
            formattedInrPrice = StoreProductPriceFormatter.formatInrFromAmount(inr),
            convertedFromRemote = false,
        )
    }

    return try {
        val inrAmount = AppStoreCurrencyToInrConverter.convert(originalPrice, currency)
        InrProductPrice(
            originalPrice = originalPrice,
            originalCurrency = currency,
            inrPrice = inrAmount,
            formattedInrPrice = StoreProductPriceFormatter.formatInrFromAmount(inrAmount),
            convertedFromRemote = true,
        )
    } catch (e: Exception) {
        println("[IAP][INR] Conversion failed ($currency $originalPrice): ${e.message}")
        val fallbackInr = AppStoreCurrencyToInrConverter.convertWithStaleCache(originalPrice, currency)
        if (fallbackInr != null) {
            InrProductPrice(
                originalPrice = originalPrice,
                originalCurrency = currency,
                inrPrice = fallbackInr,
                formattedInrPrice = StoreProductPriceFormatter.formatInrFromAmount(fallbackInr),
                convertedFromRemote = true,
                conversionFailed = true,
            )
        } else {
            InrProductPrice(
                originalPrice = originalPrice,
                originalCurrency = currency,
                inrPrice = originalPrice,
                formattedInrPrice = localizedDisplayPrice.ifBlank {
                    StoreProductPriceFormatter.formatForInr(
                        amountMicros = amountMicros,
                        currencyCode = currencyCode,
                        localizedDisplayPrice = localizedDisplayPrice,
                    )
                },
                conversionFailed = true,
            )
        }
    }
}

@Serializable
private data class FrankfurterConversionResponse(
    val amount: Double = 0.0,
    val base: String = "",
    val date: String = "",
    val rates: Map<String, Double> = emptyMap(),
)

private object AppStoreCurrencyToInrConverter {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient by lazy {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
        }
    }

    /** `from` currency → INR per 1 unit of `from`, and fetch timestamp. */
    private val rateCache = mutableMapOf<String, Pair<Double, Long>>()
    private const val CACHE_TTL_MS = 60 * 60 * 1000L

    suspend fun convert(amount: Double, fromCurrency: String): Double {
        val key = fromCurrency.uppercase()
        val now = currentTimeMillis()
        rateCache[key]?.let { (ratePerUnit, fetchedAt) ->
            if (now - fetchedAt < CACHE_TTL_MS) {
                return amount * ratePerUnit
            }
        }
        val responseText: String = httpClient.get(
            "https://api.frankfurter.app/latest?from=$key&to=INR&amount=$amount",
        ).body()
        val response = json.decodeFromString<FrankfurterConversionResponse>(responseText)
        val inrTotal = response.rates["INR"]
            ?: throw IllegalStateException("INR amount missing for $key")

        if (amount > 0.0) {
            rateCache[key] = (inrTotal / amount) to currentTimeMillis()
        }
        return inrTotal
    }

    fun convertWithStaleCache(amount: Double, fromCurrency: String): Double? {
        val cached = rateCache[fromCurrency.uppercase()]?.first
        return cached?.let { amount * it }
    }

    private fun currentTimeMillis(): Long = getCurrentTimeMillis()
}
