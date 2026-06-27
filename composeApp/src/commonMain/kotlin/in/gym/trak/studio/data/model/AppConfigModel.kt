package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppConfigModel(
    val app_name: String,
    val app_version: String,
    val default_country_code: String,
    val feature_flags: FeatureFlags? = null,
    val force_update: Boolean,
    val login_methods_enabled: LoginMethodsEnabled,
    val maintenance_mode: Boolean,
    val splash_assets: SplashAssets? = null,
    val support_contact: String
)
