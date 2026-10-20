package au.com.penrithbinapp.data.billing

import android.app.Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake billing repository for local debug/testing builds only (BuildConfig.DEBUG == true).
 * Returns mock products and simulates purchase flow.
 */
class FakeBillingRepository private constructor() {

    companion object {
        @Volatile
        private var instance: FakeBillingRepository? = null

        fun getInstance(): FakeBillingRepository {
            return instance ?: synchronized(this) {
                instance ?: FakeBillingRepository().also { instance = it }
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

    suspend fun startConnection() {
        _isPurchasing.value = false
        _billingState.value = BillingState.Loading
        delay(500) // Simulate network delay

        val tiers = listOf(
            DonationTier(
                productId = "donation_tier_1_nappies",
                title = "Buy a Pack of Nappies",
                description = "Help with the essentials",
                price = "$1.99",
                isOwned = ownedProductIds.contains("donation_tier_1_nappies"),
                canPurchase = true
            ),
            DonationTier(
                productId = "donation_tier_2_clothes",
                title = "Buy Some Baby Clothes",
                description = "Keep the little ones warm",
                price = "$4.99",
                isOwned = ownedProductIds.contains("donation_tier_2_clothes"),
                canPurchase = true
            ),
            DonationTier(
                productId = "donation_tier_3_toy",
                title = "Buy a Toy",
                description = "A fun treat for the kids",
                price = "$9.99",
                isOwned = ownedProductIds.contains("donation_tier_3_toy"),
                canPurchase = true
            ),
            DonationTier(
                productId = "donation_tier_4_epic_toy",
                title = "Buy an Epic Toy",
                description = "Something they'll remember",
                price = "$24.99",
                isOwned = ownedProductIds.contains("donation_tier_4_epic_toy"),
                canPurchase = true
            ),
            DonationTier(
                productId = "donation_tier_5_braces",
                title = "Help with Braces",
                description = "Supporting their smile",
                price = "$250.00",
                isOwned = ownedProductIds.contains("donation_tier_5_braces"),
                canPurchase = true
            )
        )

        _donationTiers.value = tiers
        _billingState.value = BillingState.Ready
    }

    suspend fun launchBillingFlow(activity: Activity, tier: DonationTier) {
        _isPurchasing.value = true
        // Simulate purchase delay
        delay(1000)

        // Mark as owned (simulate successful purchase)
        ownedProductIds.add(tier.productId)

        // Update tiers
        _donationTiers.value = _donationTiers.value.map { t ->
            if (t.productId == tier.productId) t.copy(isOwned = true) else t
        }

        _isPurchasing.value = false
        _purchaseSuccess.value = true
    }

    fun resetPurchaseSuccess() {
        _purchaseSuccess.value = false
    }

    fun endConnection() {
        _isPurchasing.value = false
    }
}
