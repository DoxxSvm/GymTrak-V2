package `in`.gym.trak.studio.utils

/**
 * Normalizes phone numbers for outbound API bodies (India: E.164-style `+91` + 10-digit national).
 *
 * - Blank / null → `null` (use for optional fields).
 * - Values starting with `+` but not `+91` are returned trimmed (other country codes unchanged).
 * - Local / national inputs (`9876543210`, `091…`, `9191…`) are normalized to `+91` + 10 digits.
 */
object PhoneNumberUtils {

    const val INDIA_E164_PREFIX: String = "+91"

    /**
     * Optional API fields: blank or null in → null out (omit or skip).
     */
    fun withIndiaCountryCodeForApi(phone: String?): String? {
        val trimmed = phone?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("+") && !trimmed.startsWith(INDIA_E164_PREFIX)) {
            return trimmed
        }
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val national = extractIndianNationalTenDigits(digits)
        if (national.isEmpty()) return null
        return INDIA_E164_PREFIX + national
    }

    /**
     * Required API fields: never null; best-effort from raw input.
     */
    fun withIndiaCountryCodeForApiRequired(phone: String): String {
        val normalized = withIndiaCountryCodeForApi(phone)
        if (normalized != null) return normalized
        val digits = phone.filter { it.isDigit() }
        val national = extractIndianNationalTenDigits(digits).ifEmpty { digits }
        return INDIA_E164_PREFIX + national
    }

    private fun extractIndianNationalTenDigits(digits: String): String {
        if (digits.isEmpty()) return ""
        return when {
            digits.length >= 12 && digits.startsWith("91") ->
                digits.drop(2).takeLast(10)

            digits.length == 11 && digits.startsWith("0") ->
                digits.drop(1)

            digits.length > 10 ->
                digits.takeLast(10)

            else ->
                digits
        }
    }

    /**
     * Strips country code / leading zero for display in 10-digit mobile fields (e.g. `+91…` → national digits).
     */
    fun indianNationalDigitsForInput(raw: String?): String {
        val digits = raw?.filter { it.isDigit() }.orEmpty()
        return extractIndianNationalTenDigits(digits).take(10)
    }

    /**
     * Indian mobile: exactly 10 digits, first digit [6–9] (valid cellular numbering range).
     */
    fun isValidIndianMobileTenDigits(nationalTenDigits: String): Boolean {
        if (nationalTenDigits.length != 10 ) return false
        return nationalTenDigits[0] in '6'..'9'
    }
}
