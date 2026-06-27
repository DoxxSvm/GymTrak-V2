package `in`.gym.trak.studio.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Accepts JSON number or string for nested gym location coordinates (GET /gyms list). */
private object GymNestedCoordinateSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("GymNestedCoordinate", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive) return null
        if (!element.isString) {
            return element.doubleOrNull
                ?: element.longOrNull?.toDouble()
                ?: element.intOrNull?.toDouble()
        }
        val s = element.content.trim()
        if (s.isEmpty()) return null
        return s.toDoubleOrNull()
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeDouble(value)
    }
}

@Serializable
data class GymLocationNested(
    @Serializable(with = GymNestedCoordinateSerializer::class)
    val latitude: Double? = null,
    @Serializable(with = GymNestedCoordinateSerializer::class)
    val longitude: Double? = null,
)

/** GET /gyms — typical body `{ "gyms": [ ... ] }`; some deployments may use `{ "data": [ ... ] }`. */
@Serializable
data class UserGymsListResponse(
    val gyms: List<UserOwnedGymDTO> = emptyList(),
    val data: List<UserOwnedGymDTO>? = null,
    val success: Boolean = true,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserOwnedGymDTO(
    val id: String = "",
    val name: String = "",
    val address: String? = null,
    @JsonNames("logoUrl", "logo_url", "gymLogo")
    val logoUrl: String? = null,
    /** Flat coordinates (some endpoints). */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** List/detail shape: `"location": { "latitude", "longitude" }`. */
    val location: GymLocationNested? = null,
    val gstin: String? = null,
    val slug: String? = null,
    val timezone: String? = null,
    val status: String? = null,
    val role: String? = null,
)

fun UserOwnedGymDTO.resolvedLatitude(): Double? =
    latitude ?: location?.latitude

fun UserOwnedGymDTO.resolvedLongitude(): Double? =
    longitude ?: location?.longitude

@Serializable
data class CreateOwnedGymRequest(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val gstin: String = "",
    @JsonNames("logoUrl", "logo_url")
    val logoUrl: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateOwnedGymNestedPayload(
    @JsonNames("access_token", "accessToken")
    val access_token: String? = null,
    @JsonNames("refresh_token", "refreshToken")
    val refresh_token: String? = null,
    @JsonNames("temp_token", "tempToken", "signup_token", "tempSignupToken")
    val temp_token: String? = null,
    @JsonNames("gym_id", "gymId")
    val gym_id: String? = null,
    val gym: UserOwnedGymDTO? = null,
)

/** Response from PATCH /gyms/{id}. */
@Serializable
data class UpdateGymResponse(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val status: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CreateOwnedGymResponse(
    val success: Boolean = false,
    val message: String? = null,
    @JsonNames("access_token", "accessToken")
    val access_token: String? = null,
    @JsonNames("refresh_token", "refreshToken")
    val refresh_token: String? = null,
    @JsonNames("temp_token", "tempToken", "signup_token", "tempSignupToken")
    val temp_token: String? = null,
    @JsonNames("gym_id", "gymId")
    val gym_id: String? = null,
    val data: CreateOwnedGymNestedPayload? = null,
    val gym: UserOwnedGymDTO? = null,
)
