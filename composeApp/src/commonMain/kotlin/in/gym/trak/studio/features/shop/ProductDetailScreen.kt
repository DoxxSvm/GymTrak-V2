package `in`.gym.trak.studio.features.shop

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.ProductDetailDTO
import `in`.gym.trak.studio.data.model.formatShopCurrency
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.DarkBlack
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.utils.ShareService
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_whatsapp
import gym.composeapp.generated.resources.ic_workout
import org.jetbrains.compose.resources.painterResource

class ProductDetailScreen(
    private val productId: String,
    private val fromMemberProfile: Boolean = false
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val detail by screenModel.productDetail.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val refreshVersion by ProductRefreshBus.version.collectAsState()
        var loadInitiated by remember(productId) { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        LaunchedEffect(productId) {
            loadInitiated = true
            screenModel.loadProductDetail(productId)
        }
        LaunchedEffect(fromMemberProfile) {
            if (fromMemberProfile) {
                screenModel.loadFavorites(showLoader = false)
            }
        }

        LaunchedEffect(productId, refreshVersion) {
            if (refreshVersion > 0) {
                screenModel.loadProductDetail(productId)
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete product?") },
                text = { Text("This will remove the product from the shop (soft delete).") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            screenModel.deleteProduct(productId) {
                                navigator.pop()
                            }
                        }
                    ) {
                        Text("Delete", color = Color(0xFFDC2828))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        LoadingScreenHandler(screenModel = screenModel) {
            ProductDetailScreenContent(
                productId = productId,
                detail = detail,
                isLoading = isLoading,
                loadInitiated = loadInitiated,
                onNavigateBack = { navigator.pop() },
                onEditItem = { navigator.push(AddProductScreen(productId = productId)) },
                onRequestDelete = { showDeleteConfirm = true },
                showManageActions = !fromMemberProfile,
                showLike = fromMemberProfile,
                onToggleLike = { id, isFavorite ->
                    screenModel.toggleFavorite(id, isFavorite)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreenContent(
    productId: String,
    detail: ProductDetailDTO?,
    isLoading: Boolean,
    loadInitiated: Boolean,
    onNavigateBack: () -> Unit,
    onEditItem: () -> Unit,
    onRequestDelete: () -> Unit,
    showManageActions: Boolean = true,
    showLike: Boolean = false,
    onToggleLike: (String, Boolean) -> Unit = { _, _ -> }
) {
    var selectedImageIndex by remember(productId) { mutableIntStateOf(0) }

    LaunchedEffect(detail?.images) {
        selectedImageIndex = 0
    }

    val imageUrls = remember(detail) {
        detail?.images.orEmpty().filter { it.isNotBlank() }
    }
    
    val pagerState = rememberPagerState(pageCount = { imageUrls.size.coerceAtLeast(1) })

    // Sync pager with selected index
    LaunchedEffect(pagerState.currentPage) {
        selectedImageIndex = pagerState.currentPage
    }
    
    LaunchedEffect(selectedImageIndex) {
        if (selectedImageIndex < pagerState.pageCount && selectedImageIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(selectedImageIndex)
        }
    }

    Scaffold(
        topBar = {
            GymAppBar(
                title = "Product Details",
                onBackClick = onNavigateBack,
                actions = {
                    if (detail != null && showManageActions) {
                        IconButton(onClick = onRequestDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete product",
                                tint = Color(0xFFDC2828)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            detail?.let { ShareService.shareProduct(it) }
                        },
                        enabled = detail != null,
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            if (detail != null && showManageActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CommonButton(
                        modifier = Modifier.weight(1f),
                        onClick = onEditItem,
                        text = "Edit item"
                    )
                    Surface(
                        onClick = { /* Contact */ },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GrayBorderColor),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_whatsapp),
                                contentDescription = "Contact",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        when {
            detail == null && !isLoading && productId.isNotBlank() && loadInitiated -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Could not load this product.",
                        style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                    )
                }
            }

            detail != null -> {
                val d = detail
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val url = imageUrls.getOrNull(page).orEmpty()
                            val painter = if (url.isNotBlank()) {
                                rememberAsyncImagePainter(url)
                            } else {
                                painterResource(Res.drawable.ic_workout)
                            }
                            Image(
                                painter = painter,
                                contentDescription = d.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (url.isBlank()) Modifier.padding(32.dp) else Modifier),
                                contentScale = if (url.isBlank()) ContentScale.Fit else ContentScale.Crop
                            )
                        }

                        if (imageUrls.size > 1) {
                            Row(
                                modifier = Modifier
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(imageUrls.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) PrimaryColor else White.copy(alpha = 0.5f)
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (imageUrls.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val stripScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(stripScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            imageUrls.forEachIndexed { index, url ->
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (index == selectedImageIndex) 2.dp else 1.dp,
                                            color = if (index == selectedImageIndex) PrimaryColor else GrayBorderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedImageIndex = index }
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(url),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!d.isActive) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFEDD5), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Inactive",
                                    style = AppTextTheme.bold.copy(color = Color(0xFFC2410C), fontSize = 12.sp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Active",
                                    style = AppTextTheme.bold.copy(color = Color(0xFF16A34A), fontSize = 12.sp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (d.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (d.isFavorite) Color(0xFFEF4444) else Gray,
                            modifier = Modifier
                                .size(28.dp)
                                .then(
                                    if (showLike) Modifier.clickable {
                                        onToggleLike(d.id, d.isFavorite)
                                    } else Modifier
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = d.name,
                        style = AppTextTheme.bold.copy(color = DarkBlack, fontSize = 22.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val hasDiscount = d.discountPrice > 0.0 && d.discountPrice < d.price
                        val effective = if (hasDiscount) d.discountPrice else d.price
                        Text(
                            text = formatShopCurrency(effective),
                            style = AppTextTheme.bold.copy(color = PrimaryColor, fontSize = 26.sp)
                        )
                        if (hasDiscount) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = formatShopCurrency(d.price),
                                style = AppTextTheme.regular.copy(
                                    color = Gray,
                                    fontSize = 16.sp,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = listOfNotNull(
                            d.category.takeIf { it.isNotBlank() }?.let { "Category: $it" },
                            d.unit.takeIf { it.isNotBlank() }?.let { "Unit: $it" },
                            "Stock: ${d.stock}"
                        ).joinToString(" · "),
                        style = AppTextTheme.regular.copy(color = Gray, fontSize = 14.sp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Description",
                        style = AppTextTheme.bold.copy(color = DarkBlack, fontSize = 16.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = d.description?.ifBlank { "No description provided." } ?: "No description provided.",
                        style = AppTextTheme.regular.copy(
                            color = DarkBlack.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            }
        }
    }
}
