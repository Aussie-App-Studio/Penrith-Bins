package au.com.penrithbinapp.ui.screens

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.penrithbinapp.BuildConfig
import au.com.penrithbinapp.data.billing.BillingRepository
import au.com.penrithbinapp.data.billing.BillingState
import au.com.penrithbinapp.data.billing.DonationTier
import au.com.penrithbinapp.data.billing.FakeBillingRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DonationViewModel(application: Application) : AndroidViewModel(application) {

    private val billingRepository = BillingRepository.getInstance(application.applicationContext)
    private val fakeBillingRepository = FakeBillingRepository.getInstance()

    // Use fake billing in debug builds (same as Auto Context)
    private val useDebugBilling = BuildConfig.DEBUG

    val billingState: StateFlow<BillingState>
        get() = if (useDebugBilling) fakeBillingRepository.billingState else billingRepository.billingState

    val donationTiers: StateFlow<List<DonationTier>>
        get() = if (useDebugBilling) fakeBillingRepository.donationTiers else billingRepository.donationTiers

    val purchaseSuccess: StateFlow<Boolean>
        get() = if (useDebugBilling) fakeBillingRepository.purchaseSuccess else billingRepository.purchaseSuccess

    val isPurchasing: StateFlow<Boolean>
        get() = if (useDebugBilling) fakeBillingRepository.isPurchasing else billingRepository.isPurchasing

    fun initBilling() {
        if (useDebugBilling) {
            viewModelScope.launch {
                fakeBillingRepository.startConnection()
            }
        } else {
            billingRepository.startConnection()
        }
    }

    fun launchPurchase(activity: Activity, tier: DonationTier) {
        if (useDebugBilling) {
            viewModelScope.launch {
                fakeBillingRepository.launchBillingFlow(activity, tier)
            }
        } else {
            billingRepository.launchBillingFlow(activity, tier)
        }
    }

    fun resetPurchaseSuccess() {
        if (useDebugBilling) {
            fakeBillingRepository.resetPurchaseSuccess()
        } else {
            billingRepository.resetPurchaseSuccess()
        }
    }

    fun endBilling() {
        if (useDebugBilling) {
            fakeBillingRepository.endConnection()
        } else {
            billingRepository.endConnection()
        }
    }
}
