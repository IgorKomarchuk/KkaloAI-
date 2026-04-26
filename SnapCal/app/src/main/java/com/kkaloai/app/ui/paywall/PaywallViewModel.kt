package com.kkaloai.app.ui.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.kkaloai.app.data.billing.BillingManager
import com.kkaloai.app.util.FileLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val products: StateFlow<List<ProductDetails>> = billingManager.products
    val isSubscribed: StateFlow<Boolean> = billingManager.isSubscribed
    val errors: SharedFlow<BillingManager.ErrorType> = billingManager.errors

    fun purchase(activity: Activity, product: ProductDetails) {
        FileLogger.d("PaywallViewModel", "Launching billing flow for ${product.productId}")
        billingManager.launchBillingFlow(activity, product)
    }
}
