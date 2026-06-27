package `in`.gym.trak.studio.data.model

import kotlin.math.abs
import kotlin.math.round
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import `in`.gym.trak.studio.base.Constants

/** Multiplatform-safe money label for shop UI (e.g. `₹12.50`). */
fun formatShopCurrency(amount: Double): String {
    val v = if (abs(amount % 1.0) < 1e-9) {
        amount.toInt().toString()
    } else {
        val scaled = round(amount * 100.0).toLong()
        val whole = scaled / 100
        val frac = abs(scaled % 100)
        "$whole.${frac.toString().padStart(2, '0')}"
    }
    return "${Constants.RUPEE}$v"
}

@Serializable
data class CreateProductRequest(
    @SerialName("gym_id") val gymId: String,
    val name: String,
    val category: String,
    val price: Double,
    @SerialName("discount_price") val discountPrice: Double,
    val stock: Int,
    val unit: String,
    val description: String?,
    val images: List<String>,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class CreateProductApiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: CreateProductData? = null
)

@Serializable
data class CreateProductData(
    val id: String? = null
)

@Serializable
data class ShopProductDTO(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    @SerialName("discount_price") val discountPrice: Double = 0.0,
    val stock: Int = 0,
    val unit: String = "",
    val image: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_favorite") val isFavorite: Boolean = false
)

@Serializable
data class ProductListPagination(
    val page: Int = 0,
    val limit: Int = 0,
    val total: Int = 0
)

@Serializable
data class ProductListResponse(
    val success: Boolean = false,
    val data: List<ShopProductDTO> = emptyList(),
    val pagination: ProductListPagination? = null
)

@Serializable
data class ProductDetailDTO(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    @SerialName("discount_price") val discountPrice: Double = 0.0,
    val stock: Int = 0,
    val unit: String = "",
    val description: String? = "",
    val images: List<String> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_favorite") val isFavorite: Boolean = false
)

@Serializable
data class ProductDetailApiResponse(
    val success: Boolean = false,
    val data: ProductDetailDTO? = null
)

@Serializable
data class UpdateProductRequest(
    val name: String,
    val category: String,
    val price: Double,
    @SerialName("discount_price") val discountPrice: Double,
    val stock: Int,
    val unit: String,
    val description: String?,
    val images: List<String>,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class ProductMutationResponse(
    val success: Boolean = false,
    val message: String? = null
)

@Serializable
data class FavoriteProductDTO(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val image: String = ""
)

@Serializable
data class FavoritesListResponse(
    val success: Boolean = false,
    val data: List<FavoriteProductDTO> = emptyList()
)
