package app.downify.billing

import android.app.Activity
import android.content.Context
import app.downify.data.ApiService
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing (v7). Digital upgrades MUST use Play Billing — an
 * external web checkout for digital goods violates Play policy (the Android
 * equivalent of App Store Guideline 3.1.1). Purchases are verified
 * server-side via [ApiService.recordPlayPurchase] which syncs the user tier.
 */
data class StoreProduct(
    val key: String,            // unique per offer (productId[:basePlan])
    val productId: String,
    val title: String,
    val price: String,
    val period: String,         // "/ay", "/yıl", "tek seferlik"
    val isSubscription: Boolean,
    val details: ProductDetails,
    val offerToken: String?
)

class BillingManager(
    context: Context,
    private val api: ApiService,
    private val scope: CoroutineScope,
    private val onEntitlementChanged: () -> Unit
) : PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        // Must match the product IDs created in Google Play Console.
        val INAPP_IDS = listOf("downify_pro")
        const val SUB_ID = "downify_full"   // base plans: monthly, yearly
    }

    private val _products = MutableStateFlow<List<StoreProduct>>(emptyList())
    val products: StateFlow<List<StoreProduct>> = _products.asStateFlow()

    private val _purchasing = MutableStateFlow<String?>(null)
    val purchasing: StateFlow<String?> = _purchasing.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun start() {
        if (client.connectionState == BillingClient.ConnectionState.CONNECTED) {
            queryAll(); return
        }
        client.startConnection(this)
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            queryAll()
            restore()
        } else {
            _status.value = result.debugMessage
        }
    }

    override fun onBillingServiceDisconnected() { /* retried on next start() */ }

    private fun queryAll() {
        scope.launch {
            val subs = queryDetails(BillingClient.ProductType.SUBS, listOf(SUB_ID))
            val inapp = queryDetails(BillingClient.ProductType.INAPP, INAPP_IDS)
            val list = mutableListOf<StoreProduct>()
            for (pd in subs) {
                pd.subscriptionOfferDetails?.forEach { offer ->
                    val phase = offer.pricingPhases.pricingPhaseList.lastOrNull()
                    list += StoreProduct(
                        key = pd.productId + ":" + offer.basePlanId,
                        productId = pd.productId,
                        title = pd.title.removeSuffix(" (Downify)"),
                        price = phase?.formattedPrice ?: "",
                        period = humanPeriod(phase?.billingPeriod ?: ""),
                        isSubscription = true,
                        details = pd,
                        offerToken = offer.offerToken
                    )
                }
            }
            for (pd in inapp) {
                list += StoreProduct(
                    key = pd.productId,
                    productId = pd.productId,
                    title = pd.title.removeSuffix(" (Downify)"),
                    price = pd.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
                    period = "tek seferlik",
                    isSubscription = false,
                    details = pd,
                    offerToken = null
                )
            }
            _products.value = list
        }
    }

    private suspend fun queryDetails(type: String, ids: List<String>): List<ProductDetails> =
        suspendCancellableCoroutine { cont ->
            val params = QueryProductDetailsParams.newBuilder().setProductList(
                ids.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it).setProductType(type).build()
                }
            ).build()
            client.queryProductDetailsAsync(params) { _, list -> cont.resume(list) }
        }

    fun purchase(activity: Activity, product: StoreProduct) {
        val pdParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.details)
            .apply { product.offerToken?.let { setOfferToken(it) } }
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(pdParams))
            .build()
        _purchasing.value = product.key
        client.launchBillingFlow(activity, flow)
    }

    fun restore() {
        scope.launch {
            handlePurchases(queryPurchases(BillingClient.ProductType.SUBS))
            handlePurchases(queryPurchases(BillingClient.ProductType.INAPP))
        }
    }

    private suspend fun queryPurchases(type: String): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(type).build()
            ) { _, purchases -> cont.resume(purchases) }
        }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _purchasing.value = null
        when {
            result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null ->
                scope.launch { handlePurchases(purchases) }
            result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED ->
                _status.value = result.debugMessage
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        for (p in purchases) {
            if (p.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            val productId = p.products.firstOrNull() ?: continue
            runCatching { api.recordPlayPurchase(productId, p.purchaseToken) }
                .onSuccess { onEntitlementChanged() }
            if (!p.isAcknowledged) {
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(p.purchaseToken).build()
                ) { /* result */ }
            }
        }
    }

    private fun humanPeriod(iso: String): String = when (iso) {
        "P1W" -> "/hafta"
        "P1M" -> "/ay"
        "P3M" -> "/3 ay"
        "P6M" -> "/6 ay"
        "P1Y" -> "/yıl"
        else -> ""
    }

    fun release() = client.endConnection()
}
