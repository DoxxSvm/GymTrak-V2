package `in`.gym.trak.studio.data.repository

import `in`.gym.trak.studio.data.model.DashboardPermissions
import `in`.gym.trak.studio.data.model.RoleDefaultPermissions
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.getPlatform
import `in`.gym.trak.studio.utils.NotificationManager

object SessionManager {

    private val platform = getPlatform()
    private const val KEY_FIRST_TIME_USER = "isFirstTimeUser"
    private const val KEY_LOGGED_IN = "isLoggedIn"
    private const val KEY_GYM_ID = "gymId"
    /** Last gym id from member dashboard (used when session [gymId] is empty, e.g. payments query). */
    private const val KEY_MEMBER_DASHBOARD_GYM_ID = "memberDashboardGymId"
    /** Gym membership id from member dashboard — profile URL segment and payments `memberId` for self-serve member. */
    private const val KEY_MEMBER_GYM_USER_ID = "memberGymUserId"
    private const val KEY_USER_ID = "userId"
    private const val KEY_ACCESS_TOKEN = "accessToken"
    private const val KEY_USER_ROLE = "userRole"
    private const val KEY_REFRESH_TOKEN = "refreshToken"
    private const val KEY_FCM_DEVICE_TOKEN = "fcmDeviceToken"
    private const val KEY_LAST_REFRESH_TIMESTAMP = "lastRefreshTimestamp"
    /** From login/resend before OTP; sent with verify-otp when present. */
    private const val KEY_PENDING_OTP_TEMP = "pendingOtpTempToken"
    private const val KEY_PENDING_OTP_ACCESS = "pendingOtpAccessToken"
    private const val KEY_OWNER_PERMISSIONS_CACHED = "ownerPermissionsCached"
    private const val KEY_OWNER_PERMISSION_ROLE = "ownerPermissionRole"
    private const val KEY_OWNER_PERMISSION_PREFIX = "owner_perm_"

    object PermissionKeys {
        // Dashboard
        const val KEY_DASHBOARD = "dashboard"
        const val KEY_DASHBOARD_VIEW = "dashboardView"
        const val KEY_DASHBOARD_NOTIFICATIONS = "dashboardNotifications"
        const val KEY_DASHBOARD_PAYMENTS_WIDGET = "dashboardPaymentsWidget"
        const val KEY_DASHBOARD_ANALYTICS = "dashboardAnalytics"

        // Basic
        const val KEY_PAYMENTS = "payments"
        const val KEY_MEMBERS = "members"
        const val KEY_ADMIN = "admin"

        // Leave
        const val KEY_LEAVE_CREATE = "leaveCreate"
        const val KEY_LEAVE_READ = "leaveRead"
        const val KEY_LEAVE_UPDATE = "leaveUpdate"
        const val KEY_LEAVE_DELETE = "leaveDelete"
        const val KEY_LEAVE_APPROVE = "leaveApprove"
        const val KEY_LEAVE_REJECT = "leaveReject"

        // Product
        const val KEY_PRODUCT_CREATE = "productCreate"
        const val KEY_PRODUCT_READ = "productRead"
        const val KEY_PRODUCT_UPDATE = "productUpdate"
        const val KEY_PRODUCT_DELETE = "productDelete"

        // Client
        const val KEY_CLIENT_READ = "clientRead"
        const val KEY_CLIENT_CREATE = "clientCreate"
        const val KEY_CLIENT_UPDATE = "clientUpdate"
        const val KEY_CLIENT_DELETE = "clientDelete"

        // Client Details
        const val KEY_CLIENT_DETAILS_READ = "clientDetailsRead"
        const val KEY_CLIENT_DETAILS_UPDATE = "clientDetailsUpdate"
        const val KEY_CLIENT_DETAILS_DELETE = "clientDetailsDelete"

        // Subscription
        const val KEY_SUBSCRIPTION_READ = "subscriptionRead"
        const val KEY_SUBSCRIPTION_CREATE = "subscriptionCreate"
        const val KEY_SUBSCRIPTION_RENEW = "subscriptionRenew"
        const val KEY_SUBSCRIPTION_UPGRADE = "subscriptionUpgrade"
        const val KEY_SUBSCRIPTION_FREEZE = "subscriptionFreeze"

        // Payment
        const val KEY_PAYMENT_READ = "paymentRead"
        const val KEY_PAYMENT_CREATE = "paymentCreate"
        const val KEY_PAYMENT_UPDATE = "paymentUpdate"
        const val KEY_PAYMENT_DELETE = "paymentDelete"

        // Invoice
        const val KEY_INVOICE_GENERATE = "invoiceGenerate"
        const val KEY_INVOICE_SHARE = "invoiceShare"

        // Attendance
        const val KEY_ATTENDANCE_READ = "attendanceRead"

        // Biometric
        const val KEY_BIOMETRIC_CREATE = "biometricCreate"
        const val KEY_BIOMETRIC_DELETE = "biometricDelete"
        const val KEY_BIOMETRIC_BLOCK = "biometricBlock"

        // Workout / Diet / Progress
        const val KEY_WORKOUT_ASSIGN = "workoutAssign"
        const val KEY_DIET_ASSIGN = "dietAssign"
        const val KEY_PROGRESS_TRACK = "progressTrack"

        // Lead
        const val KEY_LEAD_READ = "leadRead"
        const val KEY_LEAD_CREATE = "leadCreate"
        const val KEY_LEAD_UPDATE = "leadUpdate"
        const val KEY_LEAD_DELETE = "leadDelete"
        const val KEY_LEAD_CONVERT = "leadConvert"

        // Plan
        const val KEY_PLAN_READ = "planRead"
        const val KEY_PLAN_CREATE = "planCreate"
        const val KEY_PLAN_UPDATE = "planUpdate"
        const val KEY_PLAN_DELETE = "planDelete"
        const val KEY_PLAN_CLIENTS_VIEW = "planClientsView"

        // Trainer
        const val KEY_TRAINER_READ = "trainerRead"
        const val KEY_TRAINER_CREATE = "trainerCreate"
        const val KEY_TRAINER_UPDATE = "trainerUpdate"
        const val KEY_TRAINER_DELETE = "trainerDelete"
        const val KEY_TRAINER_CREDENTIALS_MANAGE = "trainerCredentialsManage"
        const val KEY_TRAINER_PERMISSIONS_ASSIGN = "trainerPermissionsAssign"

        // Salary
        const val KEY_SALARY_READ = "salaryRead"
        const val KEY_SALARY_CREATE = "salaryCreate"
        const val KEY_SALARY_UPDATE = "salaryUpdate"
        const val KEY_SALARY_DELETE = "salaryDelete"

        // Expense
        const val KEY_EXPENSE_READ = "expenseRead"
        const val KEY_EXPENSE_CREATE = "expenseCreate"
        const val KEY_EXPENSE_UPDATE = "expenseUpdate"
        const val KEY_EXPENSE_DELETE = "expenseDelete"

        // Gym
        const val KEY_GYM_READ = "gymRead"
        const val KEY_GYM_UPDATE = "gymUpdate"
        const val KEY_GYM_DELETE = "gymDelete"

        // Broadcast
        const val KEY_BROADCAST_WHATSAPP = "broadcastWhatsapp"
        const val KEY_BROADCAST_MESSAGE = "broadcastMessage"

        // QR
        const val KEY_QR_VIEW = "qrView"
    }

    private val allPermissionKeys = listOf(
        PermissionKeys.KEY_DASHBOARD,
        PermissionKeys.KEY_DASHBOARD_VIEW,
        PermissionKeys.KEY_DASHBOARD_NOTIFICATIONS,
        PermissionKeys.KEY_DASHBOARD_PAYMENTS_WIDGET,
        PermissionKeys.KEY_DASHBOARD_ANALYTICS,
        PermissionKeys.KEY_PAYMENTS,
        PermissionKeys.KEY_MEMBERS,
        PermissionKeys.KEY_ADMIN,
        PermissionKeys.KEY_LEAVE_READ,
        PermissionKeys.KEY_LEAVE_CREATE,
        PermissionKeys.KEY_LEAVE_UPDATE,
        PermissionKeys.KEY_LEAVE_DELETE,
        PermissionKeys.KEY_LEAVE_APPROVE,
        PermissionKeys.KEY_LEAVE_REJECT,
        PermissionKeys.KEY_PRODUCT_CREATE,
        PermissionKeys.KEY_PRODUCT_READ,
        PermissionKeys.KEY_PRODUCT_UPDATE,
        PermissionKeys.KEY_PRODUCT_DELETE,
        PermissionKeys.KEY_CLIENT_READ,
        PermissionKeys.KEY_CLIENT_CREATE,
        PermissionKeys.KEY_CLIENT_UPDATE,
        PermissionKeys.KEY_CLIENT_DELETE,
        PermissionKeys.KEY_CLIENT_DETAILS_READ,
        PermissionKeys.KEY_CLIENT_DETAILS_UPDATE,
        PermissionKeys.KEY_CLIENT_DETAILS_DELETE,
        PermissionKeys.KEY_SUBSCRIPTION_READ,
        PermissionKeys.KEY_SUBSCRIPTION_CREATE,
        PermissionKeys.KEY_SUBSCRIPTION_RENEW,
        PermissionKeys.KEY_SUBSCRIPTION_UPGRADE,
        PermissionKeys.KEY_SUBSCRIPTION_FREEZE,
        PermissionKeys.KEY_PAYMENT_READ,
        PermissionKeys.KEY_PAYMENT_CREATE,
        PermissionKeys.KEY_PAYMENT_UPDATE,
        PermissionKeys.KEY_PAYMENT_DELETE,
        PermissionKeys.KEY_INVOICE_GENERATE,
        PermissionKeys.KEY_INVOICE_SHARE,
        PermissionKeys.KEY_ATTENDANCE_READ,
        PermissionKeys.KEY_BIOMETRIC_CREATE,
        PermissionKeys.KEY_BIOMETRIC_DELETE,
        PermissionKeys.KEY_BIOMETRIC_BLOCK,
        PermissionKeys.KEY_WORKOUT_ASSIGN,
        PermissionKeys.KEY_DIET_ASSIGN,
        PermissionKeys.KEY_PROGRESS_TRACK,
        PermissionKeys.KEY_LEAD_READ,
        PermissionKeys.KEY_LEAD_CREATE,
        PermissionKeys.KEY_LEAD_UPDATE,
        PermissionKeys.KEY_LEAD_DELETE,
        PermissionKeys.KEY_LEAD_CONVERT,
        PermissionKeys.KEY_PLAN_READ,
        PermissionKeys.KEY_PLAN_CREATE,
        PermissionKeys.KEY_PLAN_UPDATE,
        PermissionKeys.KEY_PLAN_DELETE,
        PermissionKeys.KEY_PLAN_CLIENTS_VIEW,
        PermissionKeys.KEY_TRAINER_READ,
        PermissionKeys.KEY_TRAINER_CREATE,
        PermissionKeys.KEY_TRAINER_UPDATE,
        PermissionKeys.KEY_TRAINER_DELETE,
        PermissionKeys.KEY_TRAINER_CREDENTIALS_MANAGE,
        PermissionKeys.KEY_TRAINER_PERMISSIONS_ASSIGN,
        PermissionKeys.KEY_SALARY_READ,
        PermissionKeys.KEY_SALARY_CREATE,
        PermissionKeys.KEY_SALARY_UPDATE,
        PermissionKeys.KEY_SALARY_DELETE,
        PermissionKeys.KEY_EXPENSE_READ,
        PermissionKeys.KEY_EXPENSE_CREATE,
        PermissionKeys.KEY_EXPENSE_UPDATE,
        PermissionKeys.KEY_EXPENSE_DELETE,
        PermissionKeys.KEY_GYM_READ,
        PermissionKeys.KEY_GYM_UPDATE,
        PermissionKeys.KEY_GYM_DELETE,
        PermissionKeys.KEY_BROADCAST_WHATSAPP,
        PermissionKeys.KEY_BROADCAST_MESSAGE,
        PermissionKeys.KEY_QR_VIEW
    )

    var sessionRefreshDoneForCurrentProcess: Boolean = false

    const val REFRESH_INTERVAL_MS = 10 * 60 * 1000L

    var isFirstTimeUser: Boolean
        get() = platform.getBoolean(KEY_FIRST_TIME_USER, true)
        set(value) = platform.setBoolean(KEY_FIRST_TIME_USER, value)

    var isLoggedIn: Boolean
        get() = platform.getBoolean(KEY_LOGGED_IN, false)
        set(value) = platform.setBoolean(KEY_LOGGED_IN, value)

    var gymId: String
        get() = platform.getString(KEY_GYM_ID, "")
        set(value) = platform.setString(KEY_GYM_ID, value)

    var memberDashboardGymId: String
        get() = platform.getString(KEY_MEMBER_DASHBOARD_GYM_ID, "")
        set(value) = platform.setString(KEY_MEMBER_DASHBOARD_GYM_ID, value)

    var memberGymUserId: String
        get() = platform.getString(KEY_MEMBER_GYM_USER_ID, "")
        set(value) = platform.setString(KEY_MEMBER_GYM_USER_ID, value)

    var userId: String
        get() = platform.getString(KEY_USER_ID, "")
        set(value) = platform.setString(KEY_USER_ID, value)

    var accessToken: String
        get() = platform.getString(KEY_ACCESS_TOKEN, "")
        set(value) = platform.setString(KEY_ACCESS_TOKEN, value)

    var userRole: String
        get() = platform.getString(KEY_USER_ROLE, "")
        set(value) = platform.setString(KEY_USER_ROLE, value)

    var refreshToken: String
        get() = platform.getString(KEY_REFRESH_TOKEN, "")
        set(value) = platform.setString(KEY_REFRESH_TOKEN, value)

    /** Latest FCM registration token; use empty string to unregister on the server when calling the device-token API. */
    var fcmDeviceToken: String
        get() = platform.getString(KEY_FCM_DEVICE_TOKEN, "")
        set(value) = platform.setString(KEY_FCM_DEVICE_TOKEN, value)

    var lastRefreshTimestamp: Long
        get() = platform.getLong(KEY_LAST_REFRESH_TIMESTAMP, 0L)
        set(value) = platform.setLong(KEY_LAST_REFRESH_TIMESTAMP, value)

    var pendingOtpTempToken: String
        get() = platform.getString(KEY_PENDING_OTP_TEMP, "")
        set(value) = platform.setString(KEY_PENDING_OTP_TEMP, value)

    var pendingOtpAccessToken: String
        get() = platform.getString(KEY_PENDING_OTP_ACCESS, "")
        set(value) = platform.setString(KEY_PENDING_OTP_ACCESS, value)

    fun clearPendingOtpTokens() {
        pendingOtpTempToken = ""
        pendingOtpAccessToken = ""
    }

    fun saveOwnerDashboardPermissions(permissions: DashboardPermissions) {
        platform.setBoolean(KEY_OWNER_PERMISSIONS_CACHED, true)
        platform.setString(KEY_OWNER_PERMISSION_ROLE, permissions.role)
        allPermissionKeys.forEach { permissionKey ->
            val value = permissions.effective[permissionKey] == true
            setPermission(permissionKey, value)
        }

    }

    /**
     * When the owner dashboard API omits permission payloads (common for a brand-new gym), the UI would
     * otherwise treat every [hasPermission] check as false. Gym owners get full access until the API
     * returns explicit keys/maps on a later refresh.
     */
    fun applyDefaultGymOwnerPermissions() {
        saveOwnerDashboardPermissions(
            DashboardPermissions(
                role = "gym_owner",
                effective = allPermissionKeys.associateWith { true }
            )
        )
    }

    /**
     * Persists owner permissions from the dashboard API when `permissions` is a JSON array of key names.
     * Unknown keys from the API are stored too so [getPermission] works for extras (e.g. legacy `show_dashboard`).
     */
    fun saveOwnerDashboardPermissionKeys(permissionKeys: List<String>) {
        platform.setBoolean(KEY_OWNER_PERMISSIONS_CACHED, true)
        val granted = permissionKeys.toSet()
        (allPermissionKeys + granted).forEach { key ->
            setPermission(key, key in granted)
        }
    }

    fun getOwnerDashboardPermissions(): DashboardPermissions? {
        val hasCachedPermissions = platform.getBoolean(KEY_OWNER_PERMISSIONS_CACHED, false)
        if (!hasCachedPermissions) return null
        val effective = allPermissionKeys.associateWith { permissionKey ->
            getPermission(permissionKey)
        }
        return DashboardPermissions(
            role = platform.getString(KEY_OWNER_PERMISSION_ROLE, ""),
            effective = effective,
            roleDefaults = RoleDefaultPermissions()
        )
    }

    fun getPermission(key: String): Boolean {
        val hasCachedPermissions = platform.getBoolean(KEY_OWNER_PERMISSIONS_CACHED, false)
        if (!hasCachedPermissions) return false
        return platform.getBoolean(ownerPermissionStorageKey(key), false)
    }

    fun setPermission(key: String, value: Boolean) {
        platform.setBoolean(KEY_OWNER_PERMISSIONS_CACHED, true)
        platform.setBoolean(ownerPermissionStorageKey(key), value)
    }

    fun hasOwnerPermission(permissionKey: String): Boolean {
        return getPermission(permissionKey)
    }

    fun hasAnyOwnerPermission(vararg permissionKeys: String): Boolean {
        return permissionKeys.any { hasOwnerPermission(it) }
    }


    fun hasPermission(vararg keys: String): Boolean {
        return keys.any { key -> getPermission(key) }

    }

    fun clearOwnerDashboardPermissions() {
        platform.remove(KEY_OWNER_PERMISSION_ROLE)
        platform.remove(KEY_OWNER_PERMISSIONS_CACHED)
        allPermissionKeys.forEach { permissionKey ->
            platform.remove(ownerPermissionStorageKey(permissionKey))
        }
    }

    fun shouldRefreshToken(): Boolean {
        if (!isLoggedIn || refreshToken.isEmpty()) return false
        if (!sessionRefreshDoneForCurrentProcess) return true
        val elapsed = getCurrentTimeMillis() - lastRefreshTimestamp
        return elapsed >= REFRESH_INTERVAL_MS
    }

    /**
     * Gym id for member-scoped APIs: session gym when set, else last value from member dashboard.
     */
    fun effectiveGymIdForMemberApis(): String =
        gymId.takeIf { it.isNotBlank() } ?: memberDashboardGymId

    /**
     * Id for `GET members/{id}/profile` and payments `memberId` when the caller passes the logged-in [userId]
     * or blank (member self-serve). When an owner passes another member's id, that explicit id wins.
     */
    fun effectiveMemberListingIdForApi(explicitMemberId: String): String {
        val explicit = explicitMemberId.trim()
        if (explicit.isNotBlank() && explicit != userId) return explicit
        val stored = memberGymUserId.trim()
        if (stored.isNotBlank()) return stored
        return explicit.ifBlank { userId }
    }

    fun clearSession() {
        isLoggedIn = false
        accessToken = ""
        gymId = ""
        memberDashboardGymId = ""
        memberGymUserId = ""
        userRole = ""
        refreshToken = ""
        clearPendingOtpTokens()
        fcmDeviceToken = ""
        lastRefreshTimestamp = 0L
        clearOwnerDashboardPermissions()
        sessionRefreshDoneForCurrentProcess = false
        NotificationManager.clearFcmState()
    }

    private fun ownerPermissionStorageKey(permissionKey: String): String {
        return "$KEY_OWNER_PERMISSION_PREFIX$permissionKey"
    }
}
