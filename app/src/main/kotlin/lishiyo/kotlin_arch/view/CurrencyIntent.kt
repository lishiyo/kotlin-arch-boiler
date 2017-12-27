package lishiyo.kotlin_arch.view

import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Intent (events) for {@link CurrencyFragment}.
 *
 * Created by connieli on 12/26/17.
 */
sealed class CurrencyIntent : MviIntent {
    // on initial open - seed the db
    class Initial : CurrencyIntent() {
        companion object {
            fun create(): Initial {
                return Initial()
            }
        }
    }

    // load currencies intent to populate currencies list
    class LoadCurrencies : CurrencyIntent() {
        companion object {
            fun create(): LoadCurrencies {
                return LoadCurrencies()
            }
        }
    }

    // refresh currencies
    class Refresh : CurrencyIntent() {
        companion object {
            fun create(): Refresh {
                return Refresh()
            }
        }
    }

    // clicked convert button
    class Convert(val currencyFrom: String, val currencyTo: String, val quantity: Double) : CurrencyIntent() {
        companion object {
            fun create(from: String, to: String, quantity: Double): Convert {
                return Convert(from, to, quantity)
            }
        }
    }
}
