package `in`.gym.trak.studio.features.trainers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector

enum class PermissionId {
    // Access Modes
    MARK_AS_ADMIN,
    ADMIN_FOR_CLIENTS_UNDER_TRAINER,
    CUSTOM_ACCESS,

    // Dashboard
    VIEW_DASHBOARD,
    VIEW_NOTIFICATIONS,
    VIEW_PAYMENTS_WIDGET,
    VIEW_CLIENTS_WIDGET,
    VIEW_ANALYTICS,

    // Clients
    VIEW_CLIENTS,
    ADD_CLIENT,
    EDIT_CLIENT,
    DELETE_CLIENT,

    // Client Details
    VIEW_CLIENT_DETAILS,
    EDIT_CLIENT_DETAILS,
    DELETE_CLIENT_DETAILS,

    // Subscription
    VIEW_SUBSCRIPTION,
    ADD_SUBSCRIPTION,
    RENEW_SUBSCRIPTION,
    UPGRADE_PLAN,
    FREEZE_SUBSCRIPTION,

    // Payments
    VIEW_PAYMENTS,
    ADD_PAYMENT,
    EDIT_PAYMENT,
    DELETE_PAYMENT,
    GENERATE_INVOICE,
    SHARE_INVOICE,

    // Attendance / Biometrics
    VIEW_ATTENDANCE,
    ADD_BIOMETRIC,
    DELETE_BIOMETRIC,
    BLOCK_BIOMETRIC,

    // Workout / Diet
    ASSIGN_WORKOUT_PLAN,
    ASSIGN_DIET_PLAN,
    TRACK_PROGRESS,

    // Lead / Enquiry
    VIEW_LEADS,
    ADD_LEAD,
    EDIT_LEAD,
    DELETE_LEAD,
    CONVERT_TO_CLIENT,

    // Plans
    VIEW_PLANS,
    ADD_PLAN,
    EDIT_PLAN,
    DELETE_PLAN,
    VIEW_ENROLLED_CLIENTS,

    // Staff Management
    VIEW_STAFF,
    ADD_STAFF,
    EDIT_STAFF,
    DELETE_STAFF,
    MANAGE_CREDENTIALS,
    ASSIGN_PERMISSIONS,

    // Salary
    VIEW_SALARY,
    ADD_SALARY,
    EDIT_SALARY,
    DELETE_SALARY,

    // Expense Tracker
    VIEW_EXPENSES,
    ADD_EXPENSE,
    EDIT_EXPENSE,
    DELETE_EXPENSE,

    // Gym Info
    VIEW_GYM_DETAILS,
    EDIT_GYM_INFO,
    DELETE_GYM,

    // Leave Management
    VIEW_LEAVE_REQUESTS,
    APPROVE_LEAVE,
    REJECT_LEAVE,

    // Broadcast
    SEND_WHATSAPP_MESSAGE,
    SEND_BROADCAST_MESSAGE,

    // QR Settings
    VIEW_QR_CODE,
    UPDATE_QR_CODE,

    // Shop
    VIEW_PRODUCTS,
    ADD_PRODUCT,
    EDIT_PRODUCT,
    DELETE_PRODUCT
}

data class PermissionItem(
    val id: PermissionId,
    val title: String
)

data class PermissionModule(
    val title: String,
    val icon: ImageVector,
    val permissions: List<PermissionItem>
)

object TrainerPermissionCatalog {
    val modules: List<PermissionModule> = listOf(
        PermissionModule(
            title = "Dashboard",
            icon = Icons.Default.Dashboard,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_DASHBOARD, "View Dashboard"),
                PermissionItem(PermissionId.VIEW_NOTIFICATIONS, "View Notifications"),
                PermissionItem(PermissionId.VIEW_PAYMENTS_WIDGET, "View Payments Widget"),
                PermissionItem(PermissionId.VIEW_CLIENTS_WIDGET, "View Clients Widget"),
                PermissionItem(PermissionId.VIEW_ANALYTICS, "View Analytics")
            )
        ),
        PermissionModule(
            title = "Clients",
            icon = Icons.Default.Group,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_CLIENTS, "View Clients"),
                PermissionItem(PermissionId.ADD_CLIENT, "Add Client"),
                PermissionItem(PermissionId.EDIT_CLIENT, "Edit Client"),
                PermissionItem(PermissionId.DELETE_CLIENT, "Delete Client")
            )
        ),
        PermissionModule(
            title = "Client Details",
            icon = Icons.Default.Person,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_CLIENT_DETAILS, "View Client Details"),
                PermissionItem(PermissionId.EDIT_CLIENT_DETAILS, "Edit Client Details"),
                PermissionItem(PermissionId.DELETE_CLIENT_DETAILS, "Delete Client")
            )
        ),
        PermissionModule(
            title = "Subscription",
            icon = Icons.Default.CreditCard,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_SUBSCRIPTION, "View Subscription"),
                PermissionItem(PermissionId.ADD_SUBSCRIPTION, "Add Subscription"),
                PermissionItem(PermissionId.RENEW_SUBSCRIPTION, "Renew Subscription"),
                PermissionItem(PermissionId.UPGRADE_PLAN, "Upgrade Plan"),
                PermissionItem(PermissionId.FREEZE_SUBSCRIPTION, "Freeze Subscription")
            )
        ),
        PermissionModule(
            title = "Payments",
            icon = Icons.Default.Payment,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_PAYMENTS, "View Payments"),
                PermissionItem(PermissionId.ADD_PAYMENT, "Add Payment"),
                PermissionItem(PermissionId.EDIT_PAYMENT, "Edit Payment"),
                PermissionItem(PermissionId.DELETE_PAYMENT, "Delete Payment"),
                PermissionItem(PermissionId.GENERATE_INVOICE, "Generate Invoice"),
                PermissionItem(PermissionId.SHARE_INVOICE, "Share Invoice")
            )
        ),
        PermissionModule(
            title = "Attendance / Biometrics",
            icon = Icons.Default.Fingerprint,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_ATTENDANCE, "View Attendance"),
                PermissionItem(PermissionId.ADD_BIOMETRIC, "Add Biometric"),
                PermissionItem(PermissionId.DELETE_BIOMETRIC, "Delete Biometric"),
                PermissionItem(PermissionId.BLOCK_BIOMETRIC, "Block Biometric")
            )
        ),
        PermissionModule(
            title = "Workout / Diet",
            icon = Icons.Default.FitnessCenter,
            permissions = listOf(
                PermissionItem(PermissionId.ASSIGN_WORKOUT_PLAN, "Assign Workout Plan"),
                PermissionItem(PermissionId.ASSIGN_DIET_PLAN, "Assign Diet Plan"),
                PermissionItem(PermissionId.TRACK_PROGRESS, "Track Progress")
            )
        ),
        PermissionModule(
            title = "Lead / Enquiry",
            icon = Icons.Default.PersonAdd,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_LEADS, "View Leads"),
                PermissionItem(PermissionId.ADD_LEAD, "Add Lead"),
                PermissionItem(PermissionId.EDIT_LEAD, "Edit Lead"),
                PermissionItem(PermissionId.DELETE_LEAD, "Delete Lead"),
                PermissionItem(PermissionId.CONVERT_TO_CLIENT, "Convert to Client")
            )
        ),
        PermissionModule(
            title = "Plans",
            icon = Icons.AutoMirrored.Filled.List,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_PLANS, "View Plans"),
                PermissionItem(PermissionId.ADD_PLAN, "Add Plan"),
                PermissionItem(PermissionId.EDIT_PLAN, "Edit Plan"),
                PermissionItem(PermissionId.DELETE_PLAN, "Delete Plan"),
                PermissionItem(PermissionId.VIEW_ENROLLED_CLIENTS, "View Enrolled Clients")
            )
        ),
        PermissionModule(
            title = "Staff Management",
            icon = Icons.Default.Group,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_STAFF, "View Staff"),
                PermissionItem(PermissionId.ADD_STAFF, "Add Staff"),
                PermissionItem(PermissionId.EDIT_STAFF, "Edit Staff"),
                PermissionItem(PermissionId.DELETE_STAFF, "Delete Staff"),
                PermissionItem(PermissionId.MANAGE_CREDENTIALS, "Manage Credentials"),
                PermissionItem(PermissionId.ASSIGN_PERMISSIONS, "Assign Permissions")
            )
        ),
        PermissionModule(
            title = "Salary",
            icon = Icons.Default.AttachMoney,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_SALARY, "View Salary"),
                PermissionItem(PermissionId.ADD_SALARY, "Add Salary"),
                PermissionItem(PermissionId.EDIT_SALARY, "Edit Salary"),
                PermissionItem(PermissionId.DELETE_SALARY, "Delete Salary")
            )
        ),
        PermissionModule(
            title = "Expense Tracker",
            icon = Icons.Default.Receipt,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_EXPENSES, "View Expenses"),
                PermissionItem(PermissionId.ADD_EXPENSE, "Add Expense"),
                PermissionItem(PermissionId.EDIT_EXPENSE, "Edit Expense"),
                PermissionItem(PermissionId.DELETE_EXPENSE, "Delete Expense")
            )
        ),
        PermissionModule(
            title = "Gym Info",
            icon = Icons.Default.Business,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_GYM_DETAILS, "View Gym Details"),
                PermissionItem(PermissionId.EDIT_GYM_INFO, "Edit Gym Info"),
                PermissionItem(PermissionId.DELETE_GYM, "Delete Gym")
            )
        ),
        PermissionModule(
            title = "Leave Management",
            icon = Icons.AutoMirrored.Filled.EventNote,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_LEAVE_REQUESTS, "View Leave Requests"),
                PermissionItem(PermissionId.APPROVE_LEAVE, "Approve Leave"),
                PermissionItem(PermissionId.REJECT_LEAVE, "Reject Leave")
            )
        ),
        PermissionModule(
            title = "Broadcast",
            icon = Icons.Default.Campaign,
            permissions = listOf(
                PermissionItem(PermissionId.SEND_WHATSAPP_MESSAGE, "Send WhatsApp Message"),
                PermissionItem(PermissionId.SEND_BROADCAST_MESSAGE, "Send Broadcast Message")
            )
        ),
        PermissionModule(
            title = "QR Settings",
            icon = Icons.Default.QrCode,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_QR_CODE, "View QR Code"),
                PermissionItem(PermissionId.UPDATE_QR_CODE, "Update QR Code")
            )
        ),
        PermissionModule(
            title = "Shop",
            icon = Icons.Default.Store,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_PRODUCTS, "View Products"),
                PermissionItem(PermissionId.ADD_PRODUCT, "Add Product"),
                PermissionItem(PermissionId.EDIT_PRODUCT, "Edit Product"),
                PermissionItem(PermissionId.DELETE_PRODUCT, "Delete Product")
            )
        )
    )
}

/**
 * Permissions that can be granted to gym **staff** (front desk / operations).
 * Excludes trainer-only areas: workout & diet, plans, salary, expenses, gym deletion,
 * staff/trainer management, and full admin tooling.
 */
object StaffPermissionCatalog {
    val modules: List<PermissionModule> = listOf(
        PermissionModule(
            title = "Dashboard",
            icon = Icons.Default.Dashboard,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_DASHBOARD, "View Dashboard"),
                PermissionItem(PermissionId.VIEW_NOTIFICATIONS, "View Notifications")
            )
        ),
        PermissionModule(
            title = "Members",
            icon = Icons.Default.Group,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_CLIENTS, "View Members"),
                PermissionItem(PermissionId.VIEW_CLIENT_DETAILS, "View Member Details"),
                PermissionItem(PermissionId.ADD_CLIENT, "Add Member"),
                PermissionItem(PermissionId.EDIT_CLIENT, "Edit Member")
            )
        ),
        PermissionModule(
            title = "Subscription",
            icon = Icons.Default.CreditCard,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_SUBSCRIPTION, "View Subscription")
            )
        ),
        PermissionModule(
            title = "Payments",
            icon = Icons.Default.Payment,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_PAYMENTS, "View Payments"),
                PermissionItem(PermissionId.ADD_PAYMENT, "Record Payment"),
                PermissionItem(PermissionId.GENERATE_INVOICE, "Generate Invoice"),
                PermissionItem(PermissionId.SHARE_INVOICE, "Share Invoice")
            )
        ),
        PermissionModule(
            title = "Attendance",
            icon = Icons.Default.Fingerprint,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_ATTENDANCE, "View Attendance"),
                PermissionItem(PermissionId.ADD_BIOMETRIC, "Add Biometric")
            )
        ),
        PermissionModule(
            title = "QR & entry",
            icon = Icons.Default.QrCode,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_QR_CODE, "Scan / view QR")
            )
        ),
        PermissionModule(
            title = "Leads",
            icon = Icons.Default.PersonAdd,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_LEADS, "View Leads"),
                PermissionItem(PermissionId.ADD_LEAD, "Add Lead"),
                PermissionItem(PermissionId.EDIT_LEAD, "Edit Lead")
            )
        ),
        PermissionModule(
            title = "Leave",
            icon = Icons.AutoMirrored.Filled.EventNote,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_LEAVE_REQUESTS, "View Leave Requests")
            )
        ),
        PermissionModule(
            title = "Shop",
            icon = Icons.Default.Store,
            permissions = listOf(
                PermissionItem(PermissionId.VIEW_PRODUCTS, "View Products"),
                PermissionItem(PermissionId.ADD_PRODUCT, "Add Product"),
                PermissionItem(PermissionId.EDIT_PRODUCT, "Edit Product")
            )
        ),
        PermissionModule(
            title = "Broadcast",
            icon = Icons.Default.Campaign,
            permissions = listOf(
                PermissionItem(PermissionId.SEND_WHATSAPP_MESSAGE, "Send WhatsApp Message")
            )
        )
    )
}

data class PermissionKeyBinding(
    val primaryKey: String,
    val aliases: Set<String> = emptySet()
) {
    val allKeys: Set<String> = aliases + primaryKey
}

val PermissionEffectiveKeyMap: Map<PermissionId, PermissionKeyBinding> = mapOf(
    PermissionId.MARK_AS_ADMIN to PermissionKeyBinding("admin"),
    PermissionId.VIEW_DASHBOARD to PermissionKeyBinding("dashboardView", setOf("dashboard", "show_dashboard")),
    PermissionId.VIEW_NOTIFICATIONS to PermissionKeyBinding("dashboardNotifications"),
    PermissionId.VIEW_PAYMENTS_WIDGET to PermissionKeyBinding("dashboardPaymentsWidget"),
    PermissionId.VIEW_CLIENTS_WIDGET to PermissionKeyBinding("members"),
    PermissionId.VIEW_ANALYTICS to PermissionKeyBinding("dashboardAnalytics"),
    PermissionId.VIEW_CLIENTS to PermissionKeyBinding("clientRead", setOf("members")),
    PermissionId.ADD_CLIENT to PermissionKeyBinding("clientCreate", setOf("add_clients")),
    PermissionId.EDIT_CLIENT to PermissionKeyBinding("clientUpdate"),
    PermissionId.DELETE_CLIENT to PermissionKeyBinding("clientDelete"),
    PermissionId.VIEW_CLIENT_DETAILS to PermissionKeyBinding("clientDetailsRead", setOf("show_payment_in_details")),
    PermissionId.EDIT_CLIENT_DETAILS to PermissionKeyBinding("clientDetailsUpdate"),
    PermissionId.DELETE_CLIENT_DETAILS to PermissionKeyBinding("clientDetailsDelete"),
    PermissionId.VIEW_SUBSCRIPTION to PermissionKeyBinding("subscriptionRead"),
    PermissionId.ADD_SUBSCRIPTION to PermissionKeyBinding("subscriptionCreate"),
    PermissionId.RENEW_SUBSCRIPTION to PermissionKeyBinding("subscriptionRenew"),
    PermissionId.UPGRADE_PLAN to PermissionKeyBinding("subscriptionUpgrade"),
    PermissionId.FREEZE_SUBSCRIPTION to PermissionKeyBinding("subscriptionFreeze"),
    PermissionId.VIEW_PAYMENTS to PermissionKeyBinding("paymentRead", setOf("payments", "show_payments")),
    PermissionId.ADD_PAYMENT to PermissionKeyBinding("paymentCreate"),
    PermissionId.EDIT_PAYMENT to PermissionKeyBinding("paymentUpdate"),
    PermissionId.DELETE_PAYMENT to PermissionKeyBinding("paymentDelete"),
    PermissionId.GENERATE_INVOICE to PermissionKeyBinding("invoiceGenerate"),
    PermissionId.SHARE_INVOICE to PermissionKeyBinding("invoiceShare"),
    PermissionId.VIEW_ATTENDANCE to PermissionKeyBinding("attendanceRead"),
    PermissionId.ADD_BIOMETRIC to PermissionKeyBinding("biometricCreate"),
    PermissionId.DELETE_BIOMETRIC to PermissionKeyBinding("biometricDelete"),
    PermissionId.BLOCK_BIOMETRIC to PermissionKeyBinding("biometricBlock"),
    PermissionId.ASSIGN_WORKOUT_PLAN to PermissionKeyBinding("workoutAssign"),
    PermissionId.ASSIGN_DIET_PLAN to PermissionKeyBinding("dietAssign"),
    PermissionId.TRACK_PROGRESS to PermissionKeyBinding("progressTrack"),
    PermissionId.VIEW_LEADS to PermissionKeyBinding("leadRead"),
    PermissionId.ADD_LEAD to PermissionKeyBinding("leadCreate"),
    PermissionId.EDIT_LEAD to PermissionKeyBinding("leadUpdate"),
    PermissionId.DELETE_LEAD to PermissionKeyBinding("leadDelete"),
    PermissionId.CONVERT_TO_CLIENT to PermissionKeyBinding("leadConvert"),
    PermissionId.VIEW_PLANS to PermissionKeyBinding("planRead"),
    PermissionId.ADD_PLAN to PermissionKeyBinding("planCreate"),
    PermissionId.EDIT_PLAN to PermissionKeyBinding("planUpdate"),
    PermissionId.DELETE_PLAN to PermissionKeyBinding("planDelete"),
    PermissionId.VIEW_ENROLLED_CLIENTS to PermissionKeyBinding("planClientsView"),
    PermissionId.VIEW_STAFF to PermissionKeyBinding("trainerRead"),
    PermissionId.ADD_STAFF to PermissionKeyBinding("trainerCreate", setOf("add_trainer")),
    PermissionId.EDIT_STAFF to PermissionKeyBinding("trainerUpdate"),
    PermissionId.DELETE_STAFF to PermissionKeyBinding("trainerDelete"),
    PermissionId.MANAGE_CREDENTIALS to PermissionKeyBinding("trainerCredentialsManage"),
    PermissionId.ASSIGN_PERMISSIONS to PermissionKeyBinding("trainerPermissionsAssign"),
    PermissionId.VIEW_SALARY to PermissionKeyBinding("salaryRead"),
    PermissionId.ADD_SALARY to PermissionKeyBinding("salaryCreate"),
    PermissionId.EDIT_SALARY to PermissionKeyBinding("salaryUpdate"),
    PermissionId.DELETE_SALARY to PermissionKeyBinding("salaryDelete"),
    PermissionId.VIEW_EXPENSES to PermissionKeyBinding("expenseRead"),
    PermissionId.ADD_EXPENSE to PermissionKeyBinding("expenseCreate"),
    PermissionId.EDIT_EXPENSE to PermissionKeyBinding("expenseUpdate"),
    PermissionId.DELETE_EXPENSE to PermissionKeyBinding("expenseDelete"),
    PermissionId.VIEW_GYM_DETAILS to PermissionKeyBinding("gymRead"),
    PermissionId.EDIT_GYM_INFO to PermissionKeyBinding("gymUpdate"),
    PermissionId.DELETE_GYM to PermissionKeyBinding("gymDelete"),
    PermissionId.VIEW_LEAVE_REQUESTS to PermissionKeyBinding("leaveRead"),
    PermissionId.APPROVE_LEAVE to PermissionKeyBinding("leaveApprove"),
    PermissionId.REJECT_LEAVE to PermissionKeyBinding("leaveReject"),
    PermissionId.SEND_WHATSAPP_MESSAGE to PermissionKeyBinding("broadcastWhatsapp"),
    PermissionId.SEND_BROADCAST_MESSAGE to PermissionKeyBinding("broadcastMessage"),
    PermissionId.VIEW_QR_CODE to PermissionKeyBinding("qrView"),
    PermissionId.UPDATE_QR_CODE to PermissionKeyBinding("qrView"),
    PermissionId.VIEW_PRODUCTS to PermissionKeyBinding("productRead"),
    PermissionId.ADD_PRODUCT to PermissionKeyBinding("productCreate"),
    PermissionId.EDIT_PRODUCT to PermissionKeyBinding("productUpdate"),
    PermissionId.DELETE_PRODUCT to PermissionKeyBinding("productUpdate")
)

/** Same OR-semantics as [gym.trak.studio.data.repository.SessionManager.hasPermission]: any listed key grants access. */
fun trainerHasPermission(grantedKeys: List<String>, vararg keys: String): Boolean =
    keys.any { it in grantedKeys }
