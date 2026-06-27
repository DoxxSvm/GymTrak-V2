package `in`.gym.trak.studio.features.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.ProductCard
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.ShopProductDTO
import `in`.gym.trak.studio.data.model.formatShopCurrency
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import kotlinx.coroutines.flow.distinctUntilChanged

data class Product(
    val id: String = "",
    val name: String,
    val price: String,
    val imageUrl: String = "",
    val category: String = "",
    val isFavorite: Boolean = false,
    val strikethroughPrice: String? = null,
    val unit: String = ""
)

fun ShopProductDTO.toProduct(): Product {
    val hasDiscount = discountPrice > 0.0 && discountPrice < price
    val effective = if (hasDiscount) discountPrice else price
    val strike = if (hasDiscount) formatShopCurrency(price) else null
    return Product(
        id = id,
        name = name,
        price = formatShopCurrency(effective),
        imageUrl = image,
        category = category,
        isFavorite = isFavorite,
        strikethroughPrice = strike,
        unit = unit
    )
}

class ShopScreen(
    private val fromMemberProfile: Boolean = false
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        ShopScreenContent(
            screenModel = screenModel,
            onNavigateBack = { navigator.pop() },
            onNavigateToDetail = { product ->
                if (product.id.isNotBlank()) {
                    navigator.push(ProductDetailScreen(product.id, fromMemberProfile = fromMemberProfile))
                }
            },
            onNavigateToAddProduct = { navigator.push(AddProductScreen()) },
            showAddProduct = !fromMemberProfile,
            showLike = fromMemberProfile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreenContent(
    screenModel: OwnerDashboardScreenModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Product) -> Unit,
    onNavigateToAddProduct: () -> Unit,
    showAddProduct: Boolean = true,
    showLike: Boolean = false
) {
    val productsDto by screenModel.shopProducts.collectAsState()
    val searchQuery by screenModel.shopSearchQuery.collectAsState()
    val isLoading by screenModel.isLoading.collectAsState()
    val loadingMore by screenModel.shopLoadingMore.collectAsState()
    val isRefreshing by screenModel.shopRefreshing.collectAsState()
    var isPullRefreshRequested by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val products = productsDto.map { it.toProduct() }

    val refreshVersion by ProductRefreshBus.version.collectAsState()

    LaunchedEffect(refreshVersion) {
        screenModel.loadShopProducts(isRefresh = true)
    }
    LaunchedEffect(showLike) {
        if (showLike) {
            screenModel.loadFavorites(showLoader = false)
        }
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) isPullRefreshRequested = false
    }

    LaunchedEffect(gridState, products.size, loadingMore) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (products.isEmpty() || loadingMore || isLoading) return@collect
                if (lastVisibleIndex < 0) return@collect
                if (lastVisibleIndex >= products.size - 1) {
                    screenModel.loadMoreShopProducts()
                }
            }
    }

    LoadingScreenHandler(
        screenModel = screenModel,
        showLoadingOverlay = !isPullRefreshRequested
    ) {
        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Shop",
                    onBackClick = onNavigateBack
                )
            },
            floatingActionButton = {
                if (showAddProduct) {
                    FloatingActionButton(
                        onClick = onNavigateToAddProduct,
                        containerColor = PrimaryColor,
                        contentColor = White,
                        shape = CircleShape,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Product")
                    }
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { screenModel.onShopSearchQueryChanged(it) },
                    placeholder = "Search Products...",
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                PullToRefreshBox(
                    isRefreshing = isPullRefreshRequested && isRefreshing,
                    onRefresh = {
                        isPullRefreshRequested = true
                        screenModel.refreshShopProducts()
                    },
                    state = rememberPullToRefreshState(),
                    indicator = {},
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!isLoading && products.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No products yet.",
                                style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = products,
                                key = { it.id.ifEmpty { it.name + it.price } }
                            ) { product ->
                                ProductCard(
                                    name = product.name,
                                    price = product.price,
                                    strikethroughPrice = product.strikethroughPrice,
                                    imageUrl = product.imageUrl,
                                    isFavorite = product.isFavorite,
                                    showLike = showLike,
                                    onLikeClick = {
                                        screenModel.toggleFavorite(product.id, product.isFavorite)
                                    },
                                    onClick = { onNavigateToDetail(product) }
                                )
                            }
                            if (loadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = PrimaryColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
