package `in`.gym.trak.studio.features.location

/**
 * Result of picking a location on the map ([SelectGymLocationScreen], [SelectAddressScreen]).
 * Optional confirmation UI: [ConfirmLocationScreen] when used standalone.
 */
data class PickedGymLocation(
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
