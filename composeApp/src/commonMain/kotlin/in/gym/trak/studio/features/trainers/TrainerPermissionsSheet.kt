package `in`.gym.trak.studio.features.trainers

import `in`.gym.trak.studio.components.AppScrollableSheetColumn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.data.model.TrainerPermissionsRequest
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White

@Composable
fun TrainerPermissionsSheet(
    onDismiss: () -> Unit,
    sheetTitle: String = "Trainer permissions",
    /** Full trainer catalog or [StaffPermissionCatalog.modules] for staff-only grants. */
    modules: List<PermissionModule> = TrainerPermissionCatalog.modules,
    initialPermissions: TrainerPermissionsRequest = TrainerPermissionsRequest(),
    /**
     * When non-null (e.g. edit trainer), checkboxes follow this list of granted key names.
     * When null (e.g. add trainer), [initialPermissions] legacy flags are used instead.
     */
    initialPermissionKeys: List<String>? = null,
    onComplete: (List<String>) -> Unit
) {
    val permissionController = remember(modules, initialPermissions, initialPermissionKeys) {
        PermissionController(modules).apply {
            if (initialPermissionKeys != null) {
                initializeFromPermissionKeys(initialPermissionKeys)
            } else {
                initializeFromLegacyPermissions(initialPermissions)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sheetTitle,
                style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All Permissions",
                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Black)
                    )
                    Text(
                        text = if (modules === StaffPermissionCatalog.modules) {
                            "Only the options below can be granted to staff"
                        } else {
                            "Select or clear all options"
                        },
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                    )
                }
                Switch(
                    checked = permissionController.areAllPermissionsSelected(),
                    onCheckedChange = { permissionController.toggleAllPermissions() }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        AppScrollableSheetColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            modules.forEach { module ->
                PermissionModuleTile(
                    module = module,
                    expanded = permissionController.isExpanded(module),
                    selectedCount = permissionController.selectedCount(module),
                    totalCount = module.permissions.size,
                    isAllSelected = permissionController.isModuleFullySelected(module),
                    onExpandToggle = { permissionController.toggleExpanded(module) },
                    onToggleSelectAll = { permissionController.toggleModuleSelectAll(module) },
                    isPermissionChecked = { permissionController.isChecked(it) },
                    onPermissionCheckedChange = { id, isChecked ->
                        permissionController.setChecked(id, isChecked)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        CommonButton(
            onClick = {
                onComplete(permissionController.selectedPermissionKeys())
            },
            text = "Save Permissions",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun PermissionModuleTile(
    module: PermissionModule,
    expanded: Boolean,
    selectedCount: Int,
    totalCount: Int,
    isAllSelected: Boolean,
    onExpandToggle: () -> Unit,
    onToggleSelectAll: () -> Unit,
    isPermissionChecked: (PermissionId) -> Boolean,
    onPermissionCheckedChange: (PermissionId, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(White, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = module.title,
                        tint = PrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = module.title,
                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Black)
                    )
                    Text(
                        text = "$selectedCount / $totalCount selected",
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                    )
                }

                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Gray
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(160))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    HorizontalDivider(color = Color(0xFFEDF2F7), thickness = 1.dp)

                    PermissionRow(
                        label = "Select All",
                        checked = isAllSelected,
                        icon = if (isAllSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        iconTint = if (isAllSelected) PrimaryColor else Gray,
                        onCheckedChange = { onToggleSelectAll() }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                    module.permissions.forEachIndexed { index, permission ->
                        PermissionRow(
                            label = permission.title,
                            checked = isPermissionChecked(permission.id),
                            icon = module.icon,
                            onCheckedChange = { checked ->
                                onPermissionCheckedChange(permission.id, checked)
                            }
                        )
                        if (index < module.permissions.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    checked: Boolean,
    icon: ImageVector,
    iconTint: Color = PrimaryColor,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = Black),
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
