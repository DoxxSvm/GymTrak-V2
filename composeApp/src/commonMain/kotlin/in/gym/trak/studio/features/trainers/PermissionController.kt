package `in`.gym.trak.studio.features.trainers

import androidx.compose.runtime.mutableStateMapOf
import `in`.gym.trak.studio.data.model.TrainerPermissionsRequest

class PermissionController(
    private val modules: List<PermissionModule> = TrainerPermissionCatalog.modules
) {
    private val visiblePermissionIds: Set<PermissionId> =
        modules.flatMap { m -> m.permissions.map { p -> p.id } }.toSet()

    private val permissionState = mutableStateMapOf<PermissionId, Boolean>()
    private val expandedState = mutableStateMapOf<String, Boolean>()

    init {
        modules.forEachIndexed { index, module ->
            expandedState[module.title] = index == 0
            module.permissions.forEach { permissionState[it.id] = false }
        }
    }

    fun initializeFromLegacyPermissions(initial: TrainerPermissionsRequest) {
        setChecked(PermissionId.MARK_AS_ADMIN, initial.admin)

        setChecked(PermissionId.VIEW_DASHBOARD, initial.dashboard || initial.show_dashboard)
        setChecked(PermissionId.VIEW_PAYMENTS_WIDGET, initial.show_payments)

        setChecked(PermissionId.VIEW_PAYMENTS, initial.payments || initial.show_payments)
        setChecked(PermissionId.VIEW_CLIENTS, initial.members)
        setChecked(PermissionId.VIEW_CLIENT_DETAILS, initial.show_payment_in_details || initial.members)
        setChecked(PermissionId.ADD_CLIENT, initial.add_clients)
        setChecked(PermissionId.ADD_STAFF, initial.add_trainer)
    }

    fun initializeFromEffectivePermissions(effective: Map<String, Boolean>) {
        if (effective.isEmpty()) return
        initializeFromPermissionKeys(effective.filter { it.value }.keys)
    }

    /** Select checkboxes when the API returns a list of granted permission key names (dashboard-style keys). */
    fun initializeFromPermissionKeys(grantedKeys: Collection<String>) {
        val set = grantedKeys.toSet()
        PermissionEffectiveKeyMap.forEach { (permissionId, binding) ->
            val checked = binding.allKeys.any { it in set }
            setChecked(permissionId, checked)
        }
    }

    fun isExpanded(module: PermissionModule): Boolean = expandedState[module.title] == true

    fun toggleExpanded(module: PermissionModule) {
        expandedState[module.title] = !(expandedState[module.title] ?: false)
    }

    fun isChecked(id: PermissionId): Boolean = permissionState[id] == true

    fun setChecked(id: PermissionId, checked: Boolean) {
        if (id !in visiblePermissionIds) return
        permissionState[id] = checked
    }

    fun togglePermission(id: PermissionId) {
        if (id !in visiblePermissionIds) return
        permissionState[id] = !(permissionState[id] ?: false)
    }

    fun selectedCount(module: PermissionModule): Int {
        return module.permissions.count { isChecked(it.id) }
    }

    fun isModuleFullySelected(module: PermissionModule): Boolean {
        if (module.permissions.isEmpty()) return false
        return module.permissions.all { isChecked(it.id) }
    }

    fun toggleModuleSelectAll(module: PermissionModule) {
        val shouldSelectAll = !isModuleFullySelected(module)
        module.permissions.forEach { permissionState[it.id] = shouldSelectAll }
    }

    fun areAllPermissionsSelected(): Boolean {
        if (permissionState.isEmpty()) return false
        return permissionState.values.all { it }
    }

    fun toggleAllPermissions() {
        val shouldSelectAll = !areAllPermissionsSelected()
        permissionState.keys.forEach { permissionState[it] = shouldSelectAll }
    }

    /** Primary API key for each checked permission (for create/update trainer payloads). */
    fun selectedPermissionKeys(): List<String> {
        val keys = LinkedHashSet<String>()
        PermissionEffectiveKeyMap.forEach { (permissionId, binding) ->
            if (permissionId in visiblePermissionIds && isChecked(permissionId)) {
                keys.add(binding.primaryKey)
            }
        }
        return keys.toList()
    }
}
