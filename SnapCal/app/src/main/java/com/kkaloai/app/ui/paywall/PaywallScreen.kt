package com.kkaloai.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.kkaloai.app.R
import com.kkaloai.app.data.billing.BillingManager

private const val MONTHLY_ID = "kkaloai_monthly"
private const val ANNUAL_ID = "kkaloai_annual"

@Composable
fun PaywallScreen(
    onUnlocked: () -> Unit,
    viewModel: PaywallViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(ANNUAL_ID) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isSubscribed) {
        if (isSubscribed) onUnlocked()
    }

    val errServiceMsg = stringResource(R.string.pw_err_service)
    val errOfferMsg = stringResource(R.string.pw_err_offer)
    val errPurchaseMsg = stringResource(R.string.pw_err_purchase)
    val errCancelledMsg = stringResource(R.string.pw_err_cancelled)

    LaunchedEffect(Unit) {
        viewModel.errors.collect { err ->
            val msg = when (err) {
                BillingManager.ErrorType.SERVICE_UNAVAILABLE -> errServiceMsg
                BillingManager.ErrorType.OFFER_UNAVAILABLE -> errOfferMsg
                BillingManager.ErrorType.PURCHASE_FAILED -> errPurchaseMsg
                BillingManager.ErrorType.USER_CANCELLED -> errCancelledMsg
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    val monthly = products.firstOrNull { it.productId == MONTHLY_ID }
    val annual = products.firstOrNull { it.productId == ANNUAL_ID }
    val selectedProduct = if (selectedId == ANNUAL_ID) annual else monthly

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F0F), Color(0xFF1E1E1E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = stringResource(R.string.pw_unlock_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pw_trial_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            FeatureRow(stringResource(R.string.pw_feature_ai))
            FeatureRow(stringResource(R.string.pw_feature_barcode))
            FeatureRow(stringResource(R.string.pw_feature_health))
            FeatureRow(stringResource(R.string.pw_feature_backup))
            FeatureRow(stringResource(R.string.pw_feature_macros))

            Spacer(modifier = Modifier.height(24.dp))

            PlanCard(
                title = stringResource(R.string.pw_plan_annual),
                priceLine = annual?.priceLine() ?: stringResource(R.string.pw_plan_annual_price_default),
                sub = stringResource(R.string.pw_plan_annual_sub),
                selected = selectedId == ANNUAL_ID,
                badge = stringResource(R.string.pw_badge_best_value),
                onClick = { selectedId = ANNUAL_ID }
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlanCard(
                title = stringResource(R.string.pw_plan_monthly),
                priceLine = monthly?.priceLine() ?: stringResource(R.string.pw_plan_monthly_price_default),
                sub = stringResource(R.string.pw_plan_monthly_sub),
                selected = selectedId == MONTHLY_ID,
                badge = null,
                onClick = { selectedId = MONTHLY_ID }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val activity = context as? Activity ?: return@Button
                    selectedProduct?.let { viewModel.purchase(activity, it) }
                },
                enabled = selectedProduct != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.pw_cta),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pw_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.pw_continue_free),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUnlocked() }
                    .padding(vertical = 12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        IconButton(
            onClick = { onUnlocked() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.pw_close_cd),
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("✓ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanCard(
    title: String,
    priceLine: String,
    sub: String,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(sub, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = priceLine,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

private fun ProductDetails.priceLine(): String {
    val phases = subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList
    val paid = phases?.lastOrNull { it.priceAmountMicros > 0 } ?: phases?.firstOrNull()
    return paid?.formattedPrice ?: ""
}
