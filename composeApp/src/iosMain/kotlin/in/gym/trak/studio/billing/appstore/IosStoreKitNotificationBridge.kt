package `in`.gym.trak.studio.billing.appstore

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSString
import platform.Foundation.NSUUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Notification names must stay in sync with [KmpStoreKitNotificationRouter.swift].
 */
internal object StoreKitNotificationNames {
    const val REQUEST_LOAD_PRODUCTS = "in.gym.trak.storekit.request.loadProducts"
    const val RESPONSE_LOAD_PRODUCTS = "in.gym.trak.storekit.response.loadProducts"

    const val REQUEST_PURCHASE = "in.gym.trak.storekit.request.purchase"
    const val RESPONSE_PURCHASE = "in.gym.trak.storekit.response.purchase"

    const val REQUEST_RESTORE = "in.gym.trak.storekit.request.restore"
    const val RESPONSE_RESTORE = "in.gym.trak.storekit.response.restore"

    const val REQUEST_STATUS = "in.gym.trak.storekit.request.status"
    const val RESPONSE_STATUS = "in.gym.trak.storekit.response.status"

    const val EVENT_TRANSACTION_UPDATE = "in.gym.trak.storekit.event.transactionUpdate"
}

private val wireJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

@OptIn(ExperimentalForeignApi::class)
internal object IosStoreKitNotificationBridge {

    fun wireJsonCodec(): Json = wireJson

    suspend fun requestLoadProducts(productIds: List<String>): String {
        val requestId = NSUUID().UUIDString()
        val idsJson = productIds.joinToString(",") { "\"${escapeJson(it)}\"" }
        val payload = """{"requestId":"${escapeJson(requestId)}","productIds":[$idsJson]}"""
        return exchange(
            requestName = StoreKitNotificationNames.REQUEST_LOAD_PRODUCTS,
            responseName = StoreKitNotificationNames.RESPONSE_LOAD_PRODUCTS,
            requestPayload = payload,
            expectedRequestId = requestId,
        )
    }

    suspend fun requestPurchase(productId: String): String {
        val requestId = NSUUID().UUIDString()
        val payload =
            """{"requestId":"${escapeJson(requestId)}","productId":"${escapeJson(productId)}"}"""
        return exchange(
            requestName = StoreKitNotificationNames.REQUEST_PURCHASE,
            responseName = StoreKitNotificationNames.RESPONSE_PURCHASE,
            requestPayload = payload,
            expectedRequestId = requestId,
        )
    }

    suspend fun requestRestore(): String {
        val requestId = NSUUID().UUIDString()
        val payload = """{"requestId":"${escapeJson(requestId)}"}"""
        return exchange(
            requestName = StoreKitNotificationNames.REQUEST_RESTORE,
            responseName = StoreKitNotificationNames.RESPONSE_RESTORE,
            requestPayload = payload,
            expectedRequestId = requestId,
        )
    }

    suspend fun requestStatus(): String {
        val requestId = NSUUID().UUIDString()
        val payload = """{"requestId":"${escapeJson(requestId)}"}"""
        return exchange(
            requestName = StoreKitNotificationNames.REQUEST_STATUS,
            responseName = StoreKitNotificationNames.RESPONSE_STATUS,
            requestPayload = payload,
            expectedRequestId = requestId,
        )
    }

    private fun escapeJson(s: String): String = buildString(s.length + 8) {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

    private suspend fun exchange(
        requestName: String,
        responseName: String,
        requestPayload: String,
        expectedRequestId: String,
    ): String = withTimeout(120_000) {
        suspendCancellableCoroutine { cont ->
            val center = NSNotificationCenter.defaultCenter
            var token: Any? = null

            fun cleanup() {
                token?.let { center.removeObserver(it) }
                token = null
            }

            cont.invokeOnCancellation { cleanup() }

            token = center.addObserverForName(
                name = responseName,
                `object` = null,
                queue = NSOperationQueue.mainQueue,
                usingBlock = { note: NSNotification? ->
                    val json = jsonFromUserInfo(note) ?: return@addObserverForName
                    val rid = runCatching {
                        wireJson.parseToJsonElement(json).jsonObject["requestId"]?.jsonPrimitive?.content
                    }.getOrNull()
                    if (rid != expectedRequestId) return@addObserverForName
                    cleanup()
                    cont.resume(json)
                },
            )

            val info = NSMutableDictionary()
            info.setObject(
                requestPayload as NSString,
                forKey = "json" as NSString,
            )
            center.postNotificationName(
                aName = requestName,
                `object` = null,
                userInfo = info as Map<Any?, *>,
            )
        }
    }

    private fun jsonFromUserInfo(note: NSNotification?): String? {
        val userInfo = note?.userInfo ?: return null
        val raw = userInfo.get("json") ?: return null
        return raw.toString()
    }
}
