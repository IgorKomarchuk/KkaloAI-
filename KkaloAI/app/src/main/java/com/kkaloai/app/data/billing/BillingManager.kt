package com.kkaloai.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.kkaloai.app.util.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    enum class ErrorType { SERVICE_UNAVAILABLE, OFFER_UNAVAILABLE, PURCHASE_FAILED, USER_CANCELLED }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed

    private val _isPremiumPlus = MutableStateFlow(false)
    val isPremiumPlus: StateFlow<Boolean> = _isPremiumPlus

    private val _isLifetime = MutableStateFlow(false)
    val isLifetime: StateFlow<Boolean> = _isLifetime

    private val _errors = MutableSharedFlow<ErrorType>(extraBufferCapacity = 4)
    val errors: SharedFlow<ErrorType> = _errors.asSharedFlow()

    private var reconnectAttempt = 0

    init {
        startConnection()
    }

    private fun startConnection() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempt = 0
                    queryProducts()
                    queryPurchases()
                } else {
                    FileLogger.e("BillingManager", "Setup failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                    scheduleReconnect()
                }
            }

            override fun onBillingServiceDisconnected() {
                FileLogger.d("BillingManager", "Service disconnected, scheduling reconnect")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        val delaySec = minOf(1L shl reconnectAttempt.coerceAtMost(6), 60L)
        reconnectAttempt++
        scope.launch {
            delay(delaySec * 1000L)
            startConnection()
        }
    }

    private fun queryProducts() {
        // MVP launches with 2-plan paywall (Annual + Monthly). Premium+/Family/Lifetime
        // SKUs are infrastructure-ready in code but disabled here until corresponding SKUs
        // exist in Play Console — otherwise Billing logs OFFER_UNAVAILABLE on each query.
        val subsList = listOf(
            "kkaloai_monthly",
            "kkaloai_annual"
            // TODO(premium-plus): create SKUs $9.99/mo + $69.99/yr in Play Console, then uncomment.
            // , "kkaloai_premium_plus_monthly"
            // , "kkaloai_premium_plus_annual"
            // TODO(family): re-enable post-MVP after validating demand (M3 review).
            //  SKU + Play Console toggle "Family sharing eligibility" must exist before uncomment.
            // , "kkaloai_family_monthly"
        ).map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        // TODO(lifetime): create kkaloai_lifetime $59 INAPP in Play Console, then uncomment list below.
        val inappList = emptyList<QueryProductDetailsParams.Product>()
        // val inappList = listOf("kkaloai_lifetime").map {
        //     QueryProductDetailsParams.Product.newBuilder()
        //         .setProductId(it)
        //         .setProductType(BillingClient.ProductType.INAPP)
        //         .build()
        // }

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(subsList).build()
        ) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = (_products.value + list).distinctBy { it.productId }
            } else FileLogger.e("BillingManager", "queryProducts SUBS failed: ${result.responseCode}")
        }
        if (inappList.isNotEmpty()) {
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(inappList).build()
            ) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _products.value = (_products.value + list).distinctBy { it.productId }
                } else FileLogger.e("BillingManager", "queryProducts INAPP failed: ${result.responseCode}")
            }
        }
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val acked = purchases.filter { it.isAcknowledged }
                val anySub = acked.any { it.products.any { id -> id in PREMIUM_SKUS || id in PREMIUM_PLUS_SKUS || id == FAMILY_SKU } }
                val anyPlus = acked.any { it.products.any { id -> id in PREMIUM_PLUS_SKUS } }
                _isSubscribed.value = anySub
                _isPremiumPlus.value = anyPlus
            }
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _isLifetime.value = purchases.any { it.isAcknowledged && it.products.contains(LIFETIME_SKU) }
            }
        }
    }

    companion object {
        val PREMIUM_SKUS = setOf("kkaloai_monthly", "kkaloai_annual")
        val PREMIUM_PLUS_SKUS = setOf("kkaloai_premium_plus_monthly", "kkaloai_premium_plus_annual")
        const val FAMILY_SKU = "kkaloai_family_monthly"
        const val LIFETIME_SKU = "kkaloai_lifetime"
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        if (!billingClient.isReady) {
            FileLogger.e("BillingManager", "launchBillingFlow called but client not ready")
            _errors.tryEmit(ErrorType.SERVICE_UNAVAILABLE)
            startConnection()
            return
        }
        val isInapp = productDetails.productType == BillingClient.ProductType.INAPP
        val builder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        if (!isInapp) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                FileLogger.e("BillingManager", "No offer token for product ${productDetails.productId}")
                _errors.tryEmit(ErrorType.OFFER_UNAVAILABLE)
                return
            }
            builder.setOfferToken(offerToken)
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(builder.build()))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            FileLogger.e("BillingManager", "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
            _errors.tryEmit(ErrorType.PURCHASE_FAILED)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _errors.tryEmit(ErrorType.USER_CANCELLED)
            }
            else -> {
                FileLogger.e("BillingManager", "onPurchasesUpdated error: ${billingResult.responseCode} ${billingResult.debugMessage}")
                _errors.tryEmit(ErrorType.PURCHASE_FAILED)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    applyPurchaseFlags(purchase)
                } else {
                    FileLogger.e("BillingManager", "acknowledgePurchase failed: ${billingResult.responseCode}")
                }
            }
        } else if (purchase.isAcknowledged) {
            applyPurchaseFlags(purchase)
        }
    }

    private fun applyPurchaseFlags(purchase: Purchase) {
        purchase.products.forEach { id ->
            when {
                id == LIFETIME_SKU -> { _isLifetime.value = true; _isSubscribed.value = true; _isPremiumPlus.value = true }
                id in PREMIUM_PLUS_SKUS -> { _isPremiumPlus.value = true; _isSubscribed.value = true }
                id in PREMIUM_SKUS || id == FAMILY_SKU -> { _isSubscribed.value = true }
            }
        }
    }
}
