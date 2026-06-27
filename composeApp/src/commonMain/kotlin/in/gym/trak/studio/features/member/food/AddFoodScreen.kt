package `in`.gym.trak.studio.features.member.food

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.rememberAsyncImagePainter
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_thunder
import `in`.gym.trak.studio.components.AppScrollableSheetColumn
import `in`.gym.trak.studio.components.CommonBottomSheetPicker
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.data.model.CreateDietCatalogFoodRequest
import `in`.gym.trak.studio.data.model.DietCatalogFoodDTO
import `in`.gym.trak.studio.data.model.formatDietFoodAmount
import `in`.gym.trak.studio.data.model.formatDietFoodWeightLong
import `in`.gym.trak.studio.data.model.formatDietMealFoodSummary
import `in`.gym.trak.studio.data.model.normalizeDietUnitType
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.features.member.LocalMealFood
import `in`.gym.trak.studio.features.trainers.LabeledField
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/**
 * Picks a food from the gym catalog ([GET /diet/food]) and returns it to the caller.
 */
class AddFoodScreen(
    val currentFoods: List<LocalMealFood> = emptyList(),
    val onFoodsSelected: (List<DietCatalogFoodDTO>) -> Unit
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val catalog: List<DietCatalogFoodDTO> by screenModel.dietCatalogFoods.collectAsState()
        val catalogLoading: Boolean by screenModel.dietCatalogLoading.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        val selectedFoods = remember {
            mutableStateMapOf<String, DietCatalogFoodDTO>().apply {
                currentFoods.forEach { local ->
                    local.diet_food_id?.let { id ->
                        put(id, local.toCatalogFoodDto())
                    }
                }
            }
        }
        var sheetFood by remember { mutableStateOf<DietCatalogFoodDTO?>(null) }
        var showFoodSheet by remember { mutableStateOf(false) }

        // States for image picking shared with bottom sheet
        var imageUrlForSheet by remember { mutableStateOf<String?>(null) }
        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }
        var reopenFoodSheetAfterPicker by remember { mutableStateOf(false) }

        LaunchedEffect(showFoodSheet) {
            if (showFoodSheet) {
                focusManager.clearFocus()
            }
        }

        LaunchedEffect(Unit) {
            screenModel.loadDietFoodCatalog(null)
        }

        LaunchedEffect(searchQuery) {
            delay(350)
            val query = if (searchQuery.isBlank()) null else searchQuery
            screenModel.loadDietFoodCatalog(query)
        }

        LaunchedEffect(catalog) {
            if (catalog.isEmpty()) return@LaunchedEffect
            val catalogById = catalog.associateBy { it.id }
            selectedFoods.keys.toList().forEach { id ->
                catalogById[id]?.let { catalogItem ->
                    selectedFoods[id] = mergeCatalogFoodWithSelection(
                        catalog = catalogItem,
                        selected = selectedFoods[id],
                    )
                }
            }
        }

        LaunchedEffect(showFoodSheet, pendingPickerAction) {
            if (showFoodSheet || pendingPickerAction == null) return@LaunchedEffect
            // Wait for sheet dismiss animation before opening camera/gallery.
            delay(180)
            when (pendingPickerAction) {
                "gallery" -> launchGallery = true
                "camera" -> launchCamera = true
            }
            pendingPickerAction = null
        }

        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GymAppBar(
                    "Add Food",
                    onBackClick = { navigator.pop() },
                    actions = {
                        TextButton(
                            onClick = {
                                if (selectedFoods.isNotEmpty()) {
                                    val ordered = buildList {
                                        currentFoods.forEach { local ->
                                            local.diet_food_id?.let { id ->
                                                selectedFoods[id]?.let { add(it) }
                                            }
                                        }
                                        selectedFoods.values.forEach { food ->
                                            if (none { it.id == food.id }) add(food)
                                        }
                                    }
                                    onFoodsSelected(ordered)
                                    navigator.pop()
                                }
                            },
                            modifier = Modifier.Companion.padding(end = 8.dp)
                        ) {
                            Text(
                                "Done",
                                style = AppTextTheme.bold.copy(
                                    fontSize = 16.sp,
                                    color = PrimaryColor
                                )
                            )
                        }
                    },
                )
            },
            containerColor = Color.Companion.Transparent
        ) { padding ->
            Column(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(modifier = Modifier.Companion.padding(20.dp)) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search foods"
                    )
                }

                Box(modifier = Modifier.Companion.weight(1f).fillMaxWidth()) {
                    if (catalogLoading && catalog.isEmpty()) {
                        Box(
                            modifier = Modifier.Companion.fillMaxSize(),
                            contentAlignment = Alignment.Companion.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryColor)
                        }
                    } else if (catalog.isEmpty()) {
                        Box(
                            modifier = Modifier.Companion.fillMaxSize(),
                            contentAlignment = Alignment.Companion.Center
                        ) {
                            Text(
                                "No foods match your search.",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.Companion.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(catalog, key = { it.id }) { food ->
                                val isSelected = selectedFoods.containsKey(food.id)
                                val displayFood = mergeCatalogFoodWithSelection(
                                    catalog = food,
                                    selected = selectedFoods[food.id],
                                )
                                CatalogFoodRow(
                                    food = displayFood,
                                    isSelected = isSelected,
                                    onToggle = {
                                        if (isSelected) {
                                            selectedFoods.remove(food.id)
                                        } else {
                                            selectedFoods[food.id] = mergeCatalogFoodWithSelection(
                                                catalog = food,
                                                selected = null,
                                            )
                                        }
                                    },
                                    onEdit = {
                                        focusManager.clearFocus()
                                        sheetFood = displayFood
                                        imageUrlForSheet = displayFood.image_url ?: food.image_url
                                        showFoodSheet = true
                                    }
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.Companion.padding(20.dp)) {
                    CommonButton(
                        text = "Create New Food",
                        onClick = {
                            focusManager.clearFocus()
                            sheetFood = null
                            imageUrlForSheet = null
                            showFoodSheet = true
                        },
                        modifier = Modifier.Companion.fillMaxWidth()
                    )
                }
            }
        }

        if (launchGallery) {
            GalleryPickerLauncher(
                onPhotosSelected = { photos ->
                    launchGallery = false
                    photos.firstOrNull()?.let { photo ->
                        screenModel.uploadImage(
                            photo.loadBytes(),
                            "food_${getCurrentTimeMillis()}.jpg"
                        ) { url ->
                            imageUrlForSheet = url
                        }
                    }
                    if (reopenFoodSheetAfterPicker) {
                        showFoodSheet = true
                        reopenFoodSheetAfterPicker = false
                    }
                },
                onDismiss = {
                    launchGallery = false
                    if (reopenFoodSheetAfterPicker) {
                        showFoodSheet = true
                        reopenFoodSheetAfterPicker = false
                    }
                },
                onError = {
                    launchGallery = false
                    if (reopenFoodSheetAfterPicker) {
                        showFoodSheet = true
                        reopenFoodSheetAfterPicker = false
                    }
                }
            )
        }

        if (launchCamera) {
            ImagePickerLauncher(
                config = ImagePickerConfig(
                    onPhotoCaptured = { photo ->
                        launchCamera = false
                        screenModel.uploadImage(
                            photo.loadBytes(),
                            "food_${getCurrentTimeMillis()}.jpg"
                        ) { url ->
                            imageUrlForSheet = url
                        }
                        if (reopenFoodSheetAfterPicker) {
                            showFoodSheet = true
                            reopenFoodSheetAfterPicker = false
                        }
                    },
                    onError = {
                        launchCamera = false
                        if (reopenFoodSheetAfterPicker) {
                            showFoodSheet = true
                            reopenFoodSheetAfterPicker = false
                        }
                    }
                )
            )
        }
        }

        if (showFoodSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showFoodSheet = false
                    sheetFood = null
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.Companion.White,
                dragHandle = null
            ) {
                key(sheetFood?.id, sheetFood?.updated_at, imageUrlForSheet) {
                CreateFoodBottomSheet(
                    existingFood = sheetFood,
                    imageUrl = imageUrlForSheet ?: sheetFood?.image_url,
                    onLaunchGallery = {
                        pendingPickerAction = "gallery"
                        reopenFoodSheetAfterPicker = true
                        showFoodSheet = false
                    },
                    onLaunchCamera = {
                        pendingPickerAction = "camera"
                        reopenFoodSheetAfterPicker = true
                        showFoodSheet = false
                    },
                    screenModel = screenModel,
                    onDismiss = {
                        showFoodSheet = false
                        sheetFood = null
                    },
                    onUpdated = { updatedFood ->
                        selectedFoods[updatedFood.id] = updatedFood
                        showFoodSheet = false
                        sheetFood = null
                    },
                    onCreated = {
                        showFoodSheet = false
                        sheetFood = null
                        screenModel.loadDietFoodCatalog(searchQuery.takeIf { it.isNotBlank() })
                    },
                )
                }
            }
        }
    }

    @Composable
    private fun CatalogFoodRow(
        food: DietCatalogFoodDTO,
        isSelected: Boolean,
        onToggle: () -> Unit,
        onEdit: () -> Unit
    ) {
        val foodImage =food.image_url
        val painter = if (!foodImage.isNullOrEmpty())
            rememberAsyncImagePainter(foodImage)
        else
            painterResource(Res.drawable.ic_thunder)
        CommonCard(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .clickable { onToggle() },
            shape = RoundedCornerShape(16.dp),
            elevation = 2.dp,
            borderColor = if (isSelected) PrimaryColor else null,
            content = {
                Row(
                    modifier = Modifier.Companion.padding(16.dp),
                    verticalAlignment = Alignment.Companion.CenterVertically
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.Companion.size(48.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.Companion.width(16.dp))
                    Column(modifier = Modifier.Companion.weight(1f)) {
                        Text(food.name, style = AppTextTheme.bold.copy(fontSize = 16.sp))
                        Text(
                            foodNutritionSummary(food),
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                    }

                    if (isSelected) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = PrimaryColor,
                                modifier = Modifier.Companion.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onToggle,
                        shape = CircleShape,
                        color = if (isSelected) PrimaryColor else Color.Companion.White,
                        border = if (isSelected) null else BorderStroke(
                            1.dp,
                            Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.Companion.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Companion.Center) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isSelected) White else PrimaryColor,
                                modifier = Modifier.Companion.size(18.dp)
                            )
                        }
                    }
                }
            }
        )
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { onToggle() },
//            shape = RoundedCornerShape(16.dp),
////            colors = CardDefaults.cardColors(
////                containerColor = if (isSelected) PrimaryColor.copy(alpha = 0.08f) else White
////            ),
//            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//            border = if (isSelected) BorderStroke(1.dp, PrimaryColor) else null
//        ) {
//            Row(
//                modifier = Modifier.padding(16.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Image(
//                    painter = painter,
//                    contentDescription = null,
//                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(food.name, style = AppTextTheme.bold.copy(fontSize = 16.sp))
//                    val w = food.weight_kg?.let { "Kg: $it  " } ?: ""
//                    val c = food.calories?.let { "Kcal: $it  " } ?: ""
//                    Text(
//                        "$w$c",
//                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
//                    )
//                }
//
//                if (isSelected) {
//                    IconButton(onClick = onEdit) {
//                        Icon(
//                            imageVector = Icons.Default.Edit,
//                            contentDescription = "Edit",
//                            tint = PrimaryColor,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    }
//                }
//
//                Surface(
//                    onClick = onToggle,
//                    shape = CircleShape,
//                    color = if (isSelected) PrimaryColor else Color.White,
//                    border = if (isSelected) null else BorderStroke(1.dp, Gray.copy(alpha = 0.5f)),
//                    modifier = Modifier.size(28.dp)
//                ) {
//                    Box(contentAlignment = Alignment.Center) {
//                        Icon(
//                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
//                            contentDescription = null,
//                            tint = if (isSelected) White else PrimaryColor,
//                            modifier = Modifier.size(18.dp)
//                        )
//                    }
//                }
//            }
//        }
    }

    @Composable
    private fun CreateFoodBottomSheet(
        existingFood: DietCatalogFoodDTO?,
        imageUrl: String?,
        onLaunchGallery: () -> Unit,
        onLaunchCamera: () -> Unit,
        screenModel: OwnerDashboardScreenModel,
        onDismiss: () -> Unit,
        onUpdated: (DietCatalogFoodDTO) -> Unit,
        onCreated: () -> Unit,
    ) {
        val isEditMode = existingFood != null
        val sheetKey = existingFood?.id ?: "create"
        val initialWeightUnit = foodWeightUnits.find {
            it.value == normalizeDietUnitType(existingFood?.unit_type)
        } ?: foodWeightUnits.first()

        var name by remember(sheetKey) { mutableStateOf(existingFood?.name ?: "") }
        var weight by remember(sheetKey) {
            mutableStateOf(weightDisplayValue(existingFood?.weight_kg, existingFood?.unit_type))
        }
        var selectedWeightUnit by remember(sheetKey) { mutableStateOf(initialWeightUnit) }
        var calories by remember(sheetKey) { mutableStateOf(existingFood?.calories?.toString() ?: "") }
        var protein by remember(sheetKey) { mutableStateOf(formatMacroInput(existingFood?.protein)) }
        var carbs by remember(sheetKey) { mutableStateOf(formatMacroInput(existingFood?.carbs)) }
        var fat by remember(sheetKey) { mutableStateOf(formatMacroInput(existingFood?.fat)) }
        var quantity by remember(sheetKey) { mutableStateOf(existingFood?.quantity?.toString() ?: "1") }
        var nameError by remember { mutableStateOf<String?>(null) }
        var weightError by remember { mutableStateOf<String?>(null) }
        var caloriesError by remember { mutableStateOf<String?>(null) }
        var proteinError by remember { mutableStateOf<String?>(null) }
        var carbsError by remember { mutableStateOf<String?>(null) }
        var fatError by remember { mutableStateOf<String?>(null) }
        var quantityError by remember { mutableStateOf<String?>(null) }

        var showImagePicker by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current

        LaunchedEffect(showImagePicker) {
            if (showImagePicker) {
                focusManager.clearFocus()
            }
        }

        if (showImagePicker) {
            ImagePickerBottomSheet(
                onDismiss = {
                    showImagePicker = false
                },
                onGalleryClick = {
                    showImagePicker = false
                    onLaunchGallery()
                },
                onCameraClick = {
                    showImagePicker = false
                    onLaunchCamera()
                }
            )
        }

        AppScrollableSheetColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(
                modifier = Modifier.Companion.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Edit Food" else "Create New Food",
                    style = AppTextTheme.bold.copy(fontSize = 18.sp)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Close",
                        modifier = Modifier.Companion.rotate(45f)
                    )
                }
            }

            Spacer(modifier = Modifier.Companion.height(20.dp))

            // Image selection
            Box(
                modifier = Modifier.Companion
                    .size(100.dp)
                    .align(Alignment.Companion.CenterHorizontally)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .background(Gray.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        GrayBorderColor,
                        androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        focusManager.clearFocus()
                        showImagePicker = true
                    },
                contentAlignment = Alignment.Companion.Center
            ) {
                if (imageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = null,
                        modifier = Modifier.Companion.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_thunder),
                            contentDescription = null,
                            tint = Gray,
                            modifier = Modifier.Companion.size(32.dp)
                        )
                        Text(
                            "Add Image",
                            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.Companion.height(24.dp))

            LabeledField(label = "Food Name") {
                CommonTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    placeholder = "eg. Oats",
                    errorText = nameError
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LabeledField(label = "Weight Unit") {
                CommonBottomSheetPicker(
                    options = foodWeightUnits,
                    selectedOption = selectedWeightUnit,
                    onOptionSelected = {
                        selectedWeightUnit = it
                        weightError = null
                    },
                    placeholder = "Select unit",
                    title = "Select Weight Unit",
                    optionToString = { it.label },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LabeledField(label = "Weight Value") {
                CommonTextField(
                    value = weight,
                    onValueChange = {
                        weight = it
                        weightError = null
                    },
                    placeholder = weightValuePlaceholder(selectedWeightUnit),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    errorText = weightError,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LabeledField(label = "Calories (Kcal)") {
                CommonTextField(
                    value = calories,
                    onValueChange = {
                        calories = it
                        caloriesError = null
                    },
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    errorText = caloriesError,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField(label = "Protein (g)", modifier = Modifier.weight(1f)) {
                    CommonTextField(
                        value = protein,
                        onValueChange = {
                            protein = it
                            proteinError = null
                        },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorText = proteinError,
                    )
                }
                LabeledField(label = "Carbs (g)", modifier = Modifier.weight(1f)) {
                    CommonTextField(
                        value = carbs,
                        onValueChange = {
                            carbs = it
                            carbsError = null
                        },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorText = carbsError,
                    )
                }
                LabeledField(label = "Fat (g)", modifier = Modifier.weight(1f)) {
                    CommonTextField(
                        value = fat,
                        onValueChange = {
                            fat = it
                            fatError = null
                        },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorText = fatError,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LabeledField(label = "Default Quantity") {
                CommonTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        quantityError = null
                    },
                    placeholder = "1",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Number),
                    errorText = quantityError
                )
            }

            Spacer(modifier = Modifier.Companion.height(32.dp))

            CommonButton(
                text = if (isEditMode) "Save Changes" else "Create Food",
                onClick = {
                    val normalizedName = name.trim()
                    val weightValue = weight.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    val caloriesValue = calories.toIntOrNull()
                    val proteinValue = protein.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    val carbsValue = carbs.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    val fatValue = fat.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                    val quantityValue = quantity.toIntOrNull()

                    nameError = when {
                        normalizedName.isBlank() -> "Food name is required."
                        normalizedName.length < 2 -> "Food name must be at least 2 characters."
                        else -> null
                    }
                    weightError = when {
                        weight.isBlank() -> "Enter weight value."
                        weightValue == null -> "Enter valid weight."
                        weightValue < 0.0 -> "Weight cannot be negative."
                        else -> null
                    }
                    caloriesError = when {
                        calories.isBlank() -> "Calories are required."
                        caloriesValue == null -> "Enter valid calories."
                        caloriesValue < 0 -> "Calories cannot be negative."
                        else -> null
                    }
                    proteinError = macroFieldError(protein, proteinValue)
                    carbsError = macroFieldError(carbs, carbsValue)
                    fatError = macroFieldError(fat, fatValue)
                    quantityError = when {
                        quantity.isBlank() -> "Quantity is required."
                        quantityValue == null -> "Enter valid quantity."
                        quantityValue < 1 -> "Quantity must be at least 1."
                        else -> null
                    }

                    if (
                        nameError != null ||
                        weightError != null ||
                        caloriesError != null ||
                        proteinError != null ||
                        carbsError != null ||
                        fatError != null ||
                        quantityError != null
                    ) {
                        return@CommonButton
                    }

                    val weightAmount = weightValue ?: 0.0
                    val proteinInt = proteinValue?.toInt() ?: 0
                    val carbsInt = carbsValue?.toInt() ?: 0
                    val fatInt = fatValue?.toInt() ?: 0

                    if (isEditMode) {
                        onUpdated(
                            existingFood!!.copy(
                                name = normalizedName,
                                weight_kg = weightAmount,
                                calories = caloriesValue,
                                quantity = quantityValue,
                                image_url = imageUrl,
                                protein = proteinValue,
                                carbs = carbsValue,
                                fat = fatValue,
                                unit_type = selectedWeightUnit.value,
                            )
                        )
                    } else {
                        screenModel.createDietFood(
                            CreateDietCatalogFoodRequest(
                                name = normalizedName,
                                weight_kg = weightAmount,
                                calories = caloriesValue ?: 0,
                                protein = proteinInt,
                                carbs = carbsInt,
                                fat = fatInt,
                                unit_type = selectedWeightUnit.value,
                                quantity = quantityValue ?: 1,
                                image_url = imageUrl,
                            )
                        ) {
                            onCreated()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun LocalMealFood.toCatalogFoodDto(): DietCatalogFoodDTO {
    val foodId = diet_food_id ?: key
    return DietCatalogFoodDTO(
        id = foodId,
        name = name,
        calories = calories,
        weight_kg = weight_kg,
        quantity = quantity,
        protein = protein,
        carbs = carbs,
        fat = fat,
        unit_type = unit_type?.let { normalizeDietUnitType(it) },
        image_url = image_url,
    )
}

/** Merges catalog API row with any meal-selection overrides (weight, qty, etc.). */
private fun mergeCatalogFoodWithSelection(
    catalog: DietCatalogFoodDTO,
    selected: DietCatalogFoodDTO?,
): DietCatalogFoodDTO {
    if (selected == null) return catalog
    return catalog.copy(
        name = selected.name.ifBlank { catalog.name },
        weight_kg = selected.weight_kg ?: catalog.weight_kg,
        calories = selected.calories ?: catalog.calories,
        quantity = selected.quantity ?: catalog.quantity,
        protein = selected.protein ?: catalog.protein,
        carbs = selected.carbs ?: catalog.carbs,
        fat = selected.fat ?: catalog.fat,
        unit_type = selected.unit_type?.let { normalizeDietUnitType(it) } ?: catalog.unit_type,
        image_url = selected.image_url ?: catalog.image_url,
    )
}

private fun formatMacroInput(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}

private data class FoodWeightUnit(
    val label: String,
    val value: String,
)

private val foodWeightUnits = listOf(
    FoodWeightUnit("Gram", "GRAM"),
    FoodWeightUnit("Kg", "KG"),
    FoodWeightUnit("Liter", "LITER"),
)

private fun weightDisplayValue(weightAmount: Double?, unitType: String?): String {
    if (weightAmount == null) return ""
    return formatDietFoodAmount(weightAmount)
}

private fun weightValuePlaceholder(unit: FoodWeightUnit): String = when (unit.value) {
    "GRAM" -> "e.g. 100"
    "KG" -> "e.g. 1.5"
    else -> "e.g. 0.5"
}

private fun macroFieldError(raw: String, parsed: Double?): String? = when {
    raw.isBlank() -> null
    parsed == null -> "Enter a valid value."
    parsed < 0.0 -> "Cannot be negative."
    else -> null
}

private fun foodNutritionSummary(food: DietCatalogFoodDTO): String {
    val weightText = formatDietFoodWeightLong(food.weight_kg, food.unit_type)
        .takeIf { it.isNotBlank() }
        ?.let { "Weight: $it" }
    val parts = buildList {
        weightText?.let { add(it) }
        food.calories?.let { add("Kcal: $it") }
        food.protein?.let { add("P: ${it.toInt()}g") }
        food.carbs?.let { add("C: ${it.toInt()}g") }
        food.fat?.let { add("F: ${it.toInt()}g") }
    }
    return parts.joinToString("  ")
}