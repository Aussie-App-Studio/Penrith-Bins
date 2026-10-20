package au.com.penrithbinapp.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DonationTier(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val isOwned: Boolean = false,
    val productDetails: ProductDetails? = null,
    val canPurchase: Boolean = productDetails != null
)

sealed class BillingState {
    data object Loading : BillingState()
    data object Ready : BillingState()
    data class Error(val message: String) : BillingState()
}

class BillingRepository private constructor(
    context: Context
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "BillingRepository"

        // Product IDs - must match Play Console In-App Products (One-time products)
        val PRODUCT_IDS = listOf(
            "donation_tier_1_nappies",
            "donation_tier_2_clothes",
            "donation_tier_3_toy",
            "donation_tier_4_epic_toy",
            "donation_tier_5_braces"
        )

        // Tier display info (fallback if Play Console data unavailable)
        val TIER_INFO = mapOf(
            "donation_tier_1_nappies" to Pair("Buy a Pack of Nappies", "Help with the essentials"),
            "donation_tier_2_clothes" to Pair("Buy Some Baby Clothes", "Keep the little ones warm"),
            "donation_tier_3_toy" to Pair("Buy a Toy", "A fun treat for the kids"),
            "donation_tier_4_epic_toy" to Pair("Buy an Epic Toy", "Something they'll remember"),
            "donation_tier_5_braces" to Pair("Help with Braces", "Supporting their smile")
        )

        @Volatile
        private var instance: BillingRepository? = null

        fun getInstance(context: Context): BillingRepository {
            return instance ?: synchronized(this) {
                instance ?: BillingRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Loading)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _donationTiers = MutableStateFlow<List<DonationTier>>(emptyList())
    val donationTiers: StateFlow<List<DonationTier>> = _donationTiers.asStateFlow()

    private val _purchaseSuccess = MutableStateFlow(false)
    val purchaseSuccess: StateFlow<Boolean> = _purchaseSuccess.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val ownedProductIds = mutableSetOf<String>()

    private var billingClient: BillingClient = createBillingClient()

    private fun createBillingClient(): BillingClient {
        return BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()
    }

    fun startConnection() {
        _isPurchasing.value = false
        if (billingClient.isReady) {
            queryOwnedPurchases()
            queryProductDetails()
            return
        }

        _billingState.value = BillingState.Loading

        if (billingClient.connectionState == BillingClient.ConnectionState.CLOSED) {
            billingClient = createBillingClient()
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryOwnedPurchases()
                    queryProductDetails()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _billingState.value = BillingState.Error("Could not connect to Play Store")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
                _billingState.value = BillingState.Error("Play Store connection lost")
            }
        })
    }

    private fun queryOwnedPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                ownedProductIds.clear()
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        purchase.products.forEach { productId ->
                            ownedProductIds.add(productId)
                            Log.d(TAG, "Owned: $productId")
                        }

                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                }
                updateTiersWithOwnership()
            }
        }
    }

    private fun queryProductDetails() {
        val productList = PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = queryResult.productDetailsList.orEmpty()
                if (productDetailsList.isNotEmpty()) {
                    val tiers = productDetailsList.map { details ->
                        val info = TIER_INFO[details.productId] ?: Pair("Donation", "Support the developer")
                        DonationTier(
                            productId = details.productId,
                            title = details.name.ifBlank { info.first },
                            description = info.second,
                            price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A",
                            isOwned = ownedProductIds.contains(details.productId),
                            productDetails = details,
                            canPurchase = true
                        )
                    }.sortedBy { PRODUCT_IDS.indexOf(it.productId) }

                    _donationTiers.value = tiers
                    _billingState.value = BillingState.Ready
                    Log.d(TAG, "Loaded ${tiers.size} products")
                } else {
                    loadFallbackTiers()
                }
            } else {
                Log.e(TAG, "Failed to load products: ${billingResult.debugMessage}")
                loadFallbackTiers()
            }
        }
    }

    private fun loadFallbackTiers() {
        val tiers = PRODUCT_IDS.mapIndexed { index, productId ->
            val info = TIER_INFO[productId] ?: Pair("Donation", "Support the developer")
            DonationTier(
                productId = productId,
                title = info.first,
                description = info.second,
                price = listOf("$1.99", "$4.99", "$9.99", "$24.99", "$250.00")[index],
                isOwned = ownedProductIds.contains(productId),
                productDetails = null,
                canPurchase = false
            )
        }
        _donationTiers.value = tiers
        _billingState.value = BillingState.Ready
    }

    private fun updateTiersWithOwnership() {
        _donationTiers.value = _donationTiers.value.map { tier ->
            tier.copy(isOwned = ownedProductIds.contains(tier.productId))
        }
    }

    fun launchBillingFlow(activity: Activity, tier: DonationTier) {
        val productDetails = tier.productDetails ?: run {
            Log.e(TAG, "No product details for ${tier.productId}")
            _isPurchasing.value = false
            return
        }

        _isPurchasing.value = true

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "launchBillingFlow failed: ${result.debugMessage}")
            _isPurchasing.value = false
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _isPurchasing.value = false
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    Log.d(TAG, "Purchase successful: ${purchase.products}")
                    purchase.products.forEach { ownedProductIds.add(it) }

                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }

                    updateTiersWithOwnership()
                    _purchaseSuccess.value = true
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase canceled")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            }
        }
    }

    fun resetPurchaseSuccess() {
        _purchaseSuccess.value = false
    }

    fun endConnection() {
        _isPurchasing.value = false
    }
}
