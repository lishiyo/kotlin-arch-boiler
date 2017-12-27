package lishiyo.kotlin_arch.domain

import lishiyo.kotlin_arch.mvibase.MviAction

/**
 * Created by connieli on 12/26/17.
 */
sealed class CurrencyAction : MviAction {

    // seed the database with currencies
    class Seed : CurrencyAction() {
        companion object {
            fun create(): Seed {
                return Seed()
            }
        }
    }

    // load the currencies from the db
    class LoadCurrencies : CurrencyAction() {
        companion object {
            fun create(): LoadCurrencies {
                return LoadCurrencies()
            }
        }
    }

    // convert a currency to another
    class Convert(val currencyFrom: String, val currencyTo: String, val quantity: Double) : CurrencyAction() {
        companion object {
            fun create(from: String, to: String, quantity: Double): CurrencyAction.Convert {
                return CurrencyAction.Convert(from, to, quantity)
            }
        }
    }
}