package lishiyo.kotlin_arch.view

import android.arch.lifecycle.MutableLiveData
import lishiyo.kotlin_arch.domain.Currency
import lishiyo.kotlin_arch.mvibase.MviViewState

/**
 * Created by connieli on 12/26/17.
 */
data class CurrencyViewState(
        var isLoading: Boolean = false,
        var error: Throwable? = null, // null if no error
        var currencyFrom: String? = null,
        var currencyTo: String? = null,
        var quantity: Double? = null, // quantity to convert
        var convertedTotal: Double? = null, // final converted convertedTotal (quantity * exchangeRate)
        val currencies: MutableLiveData<List<Currency>> = MutableLiveData() // full list of currencies
) : MviViewState {

    companion object {
        // start with this!
        @JvmField val IDLE = CurrencyViewState(isLoading = false)
    }

}