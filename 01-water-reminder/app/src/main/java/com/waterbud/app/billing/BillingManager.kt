package com.waterbud.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.waterbud.app.data.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Google Play Billing wrapper for the one-time "Premium" unlock (remove ads +
 * all themes + unlimited cups + export). Create the product id below in the
 * Play Console before testing a real purchase.
 */
class BillingManager(
    private val appContext: Context,
    private val onPremiumChanged: (Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object { const val PREMIUM_PRODUCT_ID = "waterbud_premium" }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var premiumDetails: ProductDetails? = null

    private val client = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    queryExistingPurchases()
                }
            }
            override fun onBillingServiceDisconnected() { /* client auto-retries on next call */ }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PREMIUM_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        ).build()
        client.queryProductDetailsAsync(params) { _, list ->
            premiumDetails = list.firstOrNull()
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = premiumDetails ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details).build()
                )
            ).build()
        client.launchBillingFlow(activity, params)
    }

    private fun queryExistingPurchases() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { _, purchases ->
            val owns = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    it.products.contains(PREMIUM_PRODUCT_ID)
            }
            // Reflect real ownership: this also REVOKES premium after a refund/chargeback.
            if (owns) purchases.forEach(::handlePurchase) else setPremium(false)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach(::handlePurchase)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            ) {}
        }
        setPremium(true)
    }

    private fun setPremium(premium: Boolean) {
        onPremiumChanged(premium)
        scope.launch(Dispatchers.IO) { PrefsRepository(appContext).setPremium(premium) }
    }
}
