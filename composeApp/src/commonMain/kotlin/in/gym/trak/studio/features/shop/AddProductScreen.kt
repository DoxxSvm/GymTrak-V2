package `in`.gym.trak.studio.features.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.data.model.CreateProductRequest
import `in`.gym.trak.studio.data.model.UpdateProductRequest
import `in`.gym.trak.studio.data.model.formatShopCurrency
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.DarkBlue
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.OffGreenColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

private val productCategories = listOf("Supplements", "Equipment", "Clothing", "Accessories")

private val productUnits = listOf("kg", "pcs")

private fun parseMoney(raw: String): Double? {
    val cleaned = raw.trim().removePrefix("$").trim().replace(",", "")
    if (cleaned.isEmpty()) return null
    return cleaned.toDoubleOrNull()
}

private fun doubleToPriceInput(amount: Double): String {
    return if (kotlin.math.abs(amount % 1.0) < 1e-9) {
        amount.toInt().toString()
    } else {
        formatShopCurrency(amount).trim().removePrefix("$").trim()
    }
}

class AddProductScreen(private val productId: String? = null) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        AddProductScreenContent(
            screenModel = screenModel,
            productId = productId,
            onNavigateBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreenContent(
    screenModel: OwnerDashboardScreenModel,
    productId: String?,
    onNavigateBack: () -> Unit
) {
    val detail by screenModel.productDetail.collectAsState()
    val isEdit = !productId.isNullOrBlank()

    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var stockQuantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    var showImagePickerBottomSheet by remember { mutableStateOf(false) }
    var launchGallery by remember { mutableStateOf(false) }
    var launchCamera by remember { mutableStateOf(false) }
    var pendingPickerAction by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }
    val bodyScroll = rememberScrollState()
    val imageStripScroll = rememberScrollState()

    var formHydrated by remember(productId) { mutableStateOf(false) }

    LaunchedEffect(productId) {
        formHydrated = false
        if (productId.isNullOrBlank()) {
            screenModel.clearProductDetail()
        } else {
            screenModel.loadProductDetail(productId)
        }
    }

    LaunchedEffect(productId, detail) {
        val pid = productId ?: return@LaunchedEffect
        val d = detail ?: return@LaunchedEffect
        if (formHydrated) return@LaunchedEffect
        if (d.id.isNotBlank() && d.id != pid) return@LaunchedEffect

        formHydrated = true
        itemName = d.name
        category = d.category
        unit = d.unit
        stockQuantity = d.stock.toString()
        price = doubleToPriceInput(d.price)
        sellingPrice = if (d.discountPrice > 0.0 && d.discountPrice < d.price) {
            doubleToPriceInput(d.discountPrice)
        } else {
            ""
        }
        description = d.description.toString()
        isActive = d.isActive
        imageUrls = d.images.filter { it.isNotBlank() }
    }

    LaunchedEffect(showImagePickerBottomSheet, pendingPickerAction) {
        if (showImagePickerBottomSheet || pendingPickerAction == null) return@LaunchedEffect
        // Wait for bottom-sheet close animation to finish before launching camera/gallery.
        delay(180)
        when (pendingPickerAction) {
            "gallery" -> launchGallery = true
            "camera" -> launchCamera = true
        }
        pendingPickerAction = null
    }

    LoadingScreenHandler(screenModel = screenModel) {
        Scaffold(
            topBar = {
                GymAppBar(
                    title = if (isEdit) "Edit Shop Item" else "Add Shop Item",
                    onBackClick = onNavigateBack
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
//                    CommonOutlineButton(
//                        onClick = { /* draft not wired to API */ },
//                        text = "Save Draft",
//                        modifier = Modifier.weight(1f),
//                        textColor = Black,
//                        borderColor = GrayBorderColor
//                    )
                    CommonButton(
                        onClick = {
                            val stock = stockQuantity.toIntOrNull()
                            val priceVal = parseMoney(price)
                            val discountVal = parseMoney(sellingPrice) ?: 0.0
                            formError = when {
                                itemName.isBlank() -> "Item name is required"
                                category.isBlank() -> "Select a category"
                                unit.isBlank() -> "Select a unit"
                                stock == null || stock < 0 -> "Enter a valid stock quantity"
                                priceVal == null || priceVal < 0 -> "Enter a valid price"
                                imageUrls.isEmpty() -> "Add at least one product image"
                                else -> null
                            }
                            if (formError != null) return@CommonButton

                            if (isEdit && !productId.isNullOrBlank()) {
                                val update = UpdateProductRequest(
                                    name = itemName.trim(),
                                    category = category,
                                    price = priceVal!!,
                                    discountPrice = discountVal,
                                    stock = stock!!,
                                    unit = unit,
                                    description = description.trim(),
                                    images = imageUrls,
                                    isActive = isActive
                                )
                                screenModel.updateProduct(
                                    productId,
                                    update,
                                    onSuccess = onNavigateBack
                                )
                            } else {
                                val request = CreateProductRequest(
                                    gymId = SessionManager.gymId,
                                    name = itemName.trim(),
                                    category = category,
                                    price = priceVal!!,
                                    discountPrice = discountVal,
                                    stock = stock!!,
                                    unit = unit,
                                    description = description.trim(),
                                    images = imageUrls,
                                    isActive = isActive
                                )
                                screenModel.createProduct(request, onSuccess = onNavigateBack)
                            }
                        },
                        text = if (isEdit) "Save changes" else "Add Item",
                        modifier = Modifier.weight(2f)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            AppScrollableScreen(
                modifier = Modifier
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp)
                    .fillMaxSize(),
                scrollState = bodyScroll,
                contentPadding = AppScrollDefaults.screenContentPadding(
                    horizontal = 16.dp,
                    bottom = 120.dp
                ),
                dismissKeyboardOnTap = true
            ) {
                SectionHeader(title = "Product Media")

                Text(
                    text = "Add one or more images (gallery supports multiple).",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (imageUrls.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(imageStripScroll)
                            .padding(bottom = 12.dp)
                    ) {
                        imageUrls.forEachIndexed { index, url ->
                            Box {
                                Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, GrayBorderColor, RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = {
                                        imageUrls = imageUrls.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .background(White)
                                .clickable { showImagePickerBottomSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PrimaryColor,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Add",
                                    style = AppTextTheme.medium.copy(
                                        fontSize = 12.sp,
                                        color = PrimaryColor
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(White, RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                Color(0xFFCBD5E1),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showImagePickerBottomSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(OffGreenColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = PrimaryColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Upload product images",
                                style = AppTextTheme.bold.copy(color = DarkBlue, fontSize = 15.sp)
                            )
                            Text(
                                "PNG, JPG, WEBP — gallery or camera",
                                style = AppTextTheme.regular.copy(color = Gray, fontSize = 12.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                formError?.let {
                    Text(
                        text = it,
                        color = RedColor,
                        style = AppTextTheme.medium.copy(fontSize = 13.sp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionHeader(title = "Item Information")

                SectionLabel(text = "Item Name")
                Spacer(modifier = Modifier.height(8.dp))
                CommonTextField(
                    value = itemName,
                    onValueChange = { itemName = it; formError = null },
                    placeholder = "e.g. Whey Protein 2kg"
                )
                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel(text = "Category")
                Spacer(modifier = Modifier.height(8.dp))
                CommonDropdown(
                    options = productCategories,
                    selectedOption = category.ifEmpty { null },
                    onOptionSelected = { category = it; formError = null },
                    placeholder = "Select category"
                )
                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel(text = "Unit")
                Spacer(modifier = Modifier.height(8.dp))
                CommonDropdown(
                    options = productUnits,
                    selectedOption = unit.ifEmpty { null },
                    onOptionSelected = { unit = it; formError = null },
                    placeholder = "Select unit (e.g. kg)"
                )
                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel(text = "Stock Quantity")
                Spacer(modifier = Modifier.height(8.dp))
                CommonTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it; formError = null },
                    placeholder = "0",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel(text = "Price ($)")
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonTextField(
                            value = price,
                            onValueChange = { price = it; formError = null },
                            placeholder = "0.00",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel(text = "Selling / discount ($)")
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it; formError = null },
                            placeholder = "Optional",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selling price is sent as discount_price to the server.",
                    style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray)
                )
                Spacer(modifier = Modifier.height(16.dp))

//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column(modifier = Modifier.weight(1f)) {
//                        SectionLabel(text = "Product active")
//                        Text(
//                            text = "Show this product in the shop when enabled.",
//                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
//                        )
//                    }
//                    Switch(
//                        checked = isActive,
//                        onCheckedChange = { isActive = it },
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = White,
//                            checkedTrackColor = PrimaryColor
//                        )
//                    )
//                }
//                Spacer(modifier = Modifier.height(16.dp))

                SectionLabel(text = "Description")
                Spacer(modifier = Modifier.height(8.dp))
                CommonTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Describe your product details, ingredients, or benefits...",
                    isMultiline = true
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showImagePickerBottomSheet) {
            ImagePickerBottomSheet(
                onDismiss = {
                    showImagePickerBottomSheet = false
                    pendingPickerAction = null
                },
                onGalleryClick = {
                    showImagePickerBottomSheet = false
                    pendingPickerAction = "gallery"
                },
                onCameraClick = {
                    showImagePickerBottomSheet = false
                    pendingPickerAction = "camera"
                },
                title = "Product images",
                subtitle = "Choose from gallery (multiple) or take a photo"
            )
        }

        if (launchGallery) {
            GalleryPickerLauncher(
                onPhotosSelected = { photos ->
                    launchGallery = false
                    if (photos.isEmpty()) return@GalleryPickerLauncher
                    val pairs = photos.mapIndexed { i, photo ->
                        photo.loadBytes() to "product_${getCurrentTimeMillis()}_$i.jpg"
                    }
                    screenModel.uploadImagesBatch(pairs) { newUrls ->
                        if (newUrls.isNotEmpty()) {
                            imageUrls = imageUrls + newUrls
                        }
                    }
                },
                onError = { launchGallery = false },
                onDismiss = { launchGallery = false }
            )
        }

        if (launchCamera) {
            ImagePickerLauncher(
                config = ImagePickerConfig(
                    onPhotoCaptured = { photo ->
                        launchCamera = false
                        screenModel.uploadImagesBatch(
                            listOf(photo.loadBytes() to "product_${getCurrentTimeMillis()}.jpg")
                        ) { newUrls ->
                            if (newUrls.isNotEmpty()) {
                                imageUrls = imageUrls + newUrls
                            }
                        }
                    },
                    onError = { launchCamera = false }
                )
            )
        }
    }
}
