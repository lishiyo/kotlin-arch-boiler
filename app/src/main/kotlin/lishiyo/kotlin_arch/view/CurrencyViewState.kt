package lishiyo.kotlin_arch.view

import android.arch.lifecycle.LiveData
import lishiyo.kotlin_arch.domain.Currency
import lishiyo.kotlin_arch.mvibase.MviViewState

/**
 * Created by connieli on 12/26/17.
 */
class CurrencyViewState : MviViewState {
    // whether in process of converting
    var isLoading: Boolean = false

    // full currency name
    var currencyFrom: String = ""

    var currencyTo: String = ""

    // quantity to convert
    var quantity: Double = 0.0

    // final converted result (quantity * exchangeRate)
    var result: Double = 0.0

    // full list of currencies
    var currencies: LiveData<List<Currency>>? = null
}