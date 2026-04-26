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
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("kkaloai_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("kkaloai_annual")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = productDetailsList
            } else {
                FileLogger.e("BillingManager", "queryProducts failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isSubscribed.value = purchasesList.any { it.isAcknowledged }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        if (!billingClient.isReady) {
            FileLogger.e("BillingManager", "launchBillingFlow called but client not ready")
            _errors.tryEmit(ErrorType.SERVICE_UNAVAILABLE)
            startConnection()
            return
        }
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            FileLogger.e("BillingManager", "No offer token for product ${productDetails.productId}")
            _errors.tryEmit(ErrorType.OFFER_UNAVAILABLE)
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
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
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isSubscribed.value = true
                } else {
                    FileLogger.e("BillingManager", "acknowledgePurchase failed: ${billingResult.responseCode}")
                }
            }
        } else if (purchase.isAcknowledged) {
            _isSubscribed.value = true
        }
    }
}
