package `in`.gym.trak.studio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceQrResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: AttendanceQrData? = null,
    val gymId: String? = null,
    val name: String? = null,
    val slug: String? = null,
    val role: String? = null,
    val hint: String? = null,
    val token: String? = null,
    val payload: String? = null,
    @SerialName("staticPayload")
    val staticPayload: String? = null,
    val qrCode: String? = null,
    @SerialName("qr_code")
    val qrCodeSnakeCase: String? = null,
    @SerialName("qrCodeUrl")
    val qrCodeUrlCamelCase: String? = null,
    @SerialName("qr_code_url")
    val qrCodeUrlSnakeCase: String? = null,
    @SerialName("qrImage")
    val qrImageCamelCase: String? = null,
    @SerialName("qr_image")
    val qrImageSnakeCase: String? = null,
    @SerialName("qrBase64")
    val qrBase64: String? = null,
    @SerialName("staticPayloadQrBase64")
    val staticPayloadQrBase64: String? = null
)

@Serializable
data class AttendanceQrData(
    val token: String? = null,
    val payload: String? = null,
    @SerialName("staticPayload")
    val staticPayload: String? = null,
    val qrCode: String? = null,
    @SerialName("qr_code")
    val qrCodeSnakeCase: String? = null,
    @SerialName("qrCodeUrl")
    val qrCodeUrlCamelCase: String? = null,
    @SerialName("qr_code_url")
    val qrCodeUrlSnakeCase: String? = null,
    @SerialName("qrImage")
    val qrImageCamelCase: String? = null,
    @SerialName("qr_image")
    val qrImageSnakeCase: String? = null,
    @SerialName("qrBase64")
    val qrBase64: String? = null,
    @SerialName("staticPayloadQrBase64")
    val staticPayloadQrBase64: String? = null
)

fun AttendanceQrResponse.resolvePayload(): String? {
    return data?.token
        ?: data?.payload
        ?: data?.staticPayload
        ?: token
        ?: payload
        ?: staticPayload
}

fun AttendanceQrResponse.resolveQrImage(): String? {
    val rawImage =
        data?.staticPayloadQrBase64
            ?: data?.qrBase64
            ?: data?.qrImageCamelCase
            ?: data?.qrImageSnakeCase
            ?: data?.qrCodeUrlCamelCase
            ?: data?.qrCodeUrlSnakeCase
            ?: data?.qrCode
            ?: data?.qrCodeSnakeCase
            ?: staticPayloadQrBase64
            ?: qrBase64
            ?: qrImageCamelCase
            ?: qrImageSnakeCase
            ?: qrCodeUrlCamelCase
            ?: qrCodeUrlSnakeCase
            ?: qrCode
            ?: qrCodeSnakeCase

    if (rawImage.isNullOrBlank()) return null

    // If backend returns raw base64, normalize to a data URL for image loading.
    return if (!rawImage.startsWith("http") && !rawImage.startsWith("data:image")) {
        "data:image/png;base64,$rawImage"
    } else {
        rawImage
    }
}

fun AttendanceQrResponse.hasUsableQrData(): Boolean {
    return !resolveQrImage().isNullOrBlank() || !resolvePayload().isNullOrBlank()
}

@Serializable
data class AttendancePunchRequest(
    val token: String
)

@Serializable
data class AttendancePunchResponse(
    val ok: Boolean? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val error: String? = null,
    val statusCode: Int? = null,
    val action: String? = null,
    @SerialName("attendedOn")
    val attendedOn: String? = null,
    val gymId: String? = null,
    @SerialName("checkedInAt")
    val checkedInAt: String? = null,
    @SerialName("checkedOutAt")
    val checkedOutAt: String? = null,
    val data: AttendancePunchData? = null,
)

@Serializable
data class AttendancePunchData(
    val action: String? = null,
    val status: String? = null,
    val memberName: String? = null,
    val timestamp: String? = null,
    @SerialName("checkedInAt")
    val checkedInAt: String? = null,
    @SerialName("checkedOutAt")
    val checkedOutAt: String? = null,
    @SerialName("attendedOn")
    val attendedOn: String? = null,
)

fun AttendancePunchResponse.resolveSuccess(): Boolean = when {
    ok == true -> true
    ok == false -> false
    success == false -> false
    else -> success != false
}

fun AttendancePunchResponse.effectiveAction(): String? =
    action?.takeIf { it.isNotBlank() } ?: data?.action?.takeIf { it.isNotBlank() }

fun AttendancePunchResponse.effectiveAttendedOn(): String? =
    attendedOn?.takeIf { it.isNotBlank() } ?: data?.attendedOn?.takeIf { it.isNotBlank() }

fun AttendancePunchResponse.effectiveCheckedInAt(): String? =
    checkedInAt?.takeIf { it.isNotBlank() }
        ?: data?.checkedInAt?.takeIf { it.isNotBlank() }
        ?: data?.timestamp?.takeIf { it.isNotBlank() }

fun AttendancePunchResponse.effectiveCheckedOutAt(): String? =
    checkedOutAt?.takeIf { it.isNotBlank() } ?: data?.checkedOutAt?.takeIf { it.isNotBlank() }

fun AttendancePunchResponse.resolveActionTitle(): String = when (effectiveAction()?.lowercase()) {
    "clock_in" -> "Checked In"
    "clock_out" -> "Checked Out"
    else -> "Attendance Recorded"
}

fun AttendancePunchResponse.resolveMessage(): String {
    message?.takeIf { it.isNotBlank() }?.let { return it }
    return when (effectiveAction()?.lowercase()) {
        "clock_in" -> "You have been checked in successfully."
        "clock_out" -> "You have been checked out successfully."
        else -> error?.takeIf { it.isNotBlank() }
            ?: data?.status?.takeIf { it.isNotBlank() }
            ?: "Attendance updated successfully."
    }
}
