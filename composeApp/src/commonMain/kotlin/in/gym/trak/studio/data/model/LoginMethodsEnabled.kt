package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginMethodsEnabled(
    val apple: Boolean = false,
    val email: Boolean = false,
    val google: Boolean = false,
    val phone: Boolean = true
)
