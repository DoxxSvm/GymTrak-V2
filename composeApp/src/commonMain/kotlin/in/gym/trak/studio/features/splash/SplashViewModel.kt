package `in`.gym.trak.studio.features.splash

import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.data.model.AppConfigModel
import `in`.gym.trak.studio.network.ApiClient
import `in`.gym.trak.studio.network.ApiEndpoints
import `in`.gym.trak.studio.network.BaseScreenModel
import `in`.gym.trak.studio.data.repository.SessionManager
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SplashViewModel : BaseScreenModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()


    fun checkAppVersion() {
        executeApi(
            apiCall = {
                ApiClient.safeApiCall<AppConfigModel> {

                    get(ApiEndpoints.App.CONFIG)
                }
            },
            onSuccess = { config ->
                validateVersion(config)
            },
            onError = { error ->
                val target = when {
                    !SessionManager.isLoggedIn -> NavigationTarget.Login
                    SessionManager.userRole.equals("gym_owner", ignoreCase = true) || SessionManager.userRole.equals("trainer", ignoreCase = true) -> NavigationTarget.OwnerDashboard

                    else -> NavigationTarget.MemberDashboard
                }
                _updateState.value = UpdateState.NoUpdateRequired(target)
            }
        )
    }

    private fun validateVersion(config: AppConfigModel) {
        val serverVersion = config.app_version
        val currentVersion = Constants.APP_VERSION

        if (isUpdateAvailable(currentVersion, serverVersion)) {
            _updateState.value = UpdateState.UpdateRequired(
                isForce = config.force_update,
                serverVersion = serverVersion
            )
        } else {
            val user =  SessionManager.userRole
            print("user roleeeee => ${user}")
            val target = when {
                SessionManager.isFirstTimeUser -> NavigationTarget.Intro
                !SessionManager.isLoggedIn -> NavigationTarget.Login
                SessionManager.userRole.equals("gym_owner", ignoreCase = true) ||SessionManager.userRole.equals("staff", ignoreCase = true) || SessionManager.userRole.equals("trainer", ignoreCase = true) -> NavigationTarget.OwnerDashboard
                else -> NavigationTarget.MemberDashboard
            }
            _updateState.value = UpdateState.NoUpdateRequired(target)
        }
    }

    private fun isUpdateAvailable(current: String, server: String): Boolean {
        return try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val serverParts = server.split(".").map { it.toIntOrNull() ?: 0 }
            
            for (i in 0 until maxOf(currentParts.size, serverParts.size)) {
                val c = currentParts.getOrElse(i) { 0 }
                val s = serverParts.getOrElse(i) { 0 }
                if (c < s) return true
                if (c > s) return false
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}

sealed class UpdateState {
    object Idle : UpdateState()
    data class NoUpdateRequired(val target: NavigationTarget) : UpdateState()
    data class UpdateRequired(val isForce: Boolean, val serverVersion: String) : UpdateState()
}

sealed class NavigationTarget {
    object Intro : NavigationTarget()
    object Login : NavigationTarget()
    object OwnerDashboard : NavigationTarget()
    object TrainerOwnerDashboard : NavigationTarget()
    object MemberDashboard : NavigationTarget()
}
