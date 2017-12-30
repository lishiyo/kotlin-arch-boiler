package lishiyo.kotlin_arch.domain

import lishiyo.kotlin_arch.mvibase.MviResult

/**
 * Results to feed back to the ViewModel - encapsulates all data needed to render a ViewState.
 *
 * Created by connieli on 12/26/17.
 */
sealed class CurrencyResult(var status: Status = Status.IDLE, var error: Throwable? = null) : MviResult {
    enum class Status {
        LOADING, SUCCESS, FAILURE, IDLE
    }

    // seeded the database
    class Seed(status: Status, error: Throwable?) : CurrencyResult(status, error) {
        companion object {
            fun createSuccess() : Seed {
                return Seed(Status.SUCCESS, null)
            }
            fun createError(throwable: Throwable) : Seed {
                return Seed(Status.FAILURE, throwable)
            }
            fun createLoading(): Seed {
                return Seed(Status.LOADING, null)
            }
        }
    }

    // currencies retrieved from db
    class LoadCurrencies(status: Status, val currencies: List<Currency>?, error: Throwable?) : CurrencyResult(status, error) {
        companion object {
            fun createSuccess(currencies: List<Currency>) : LoadCurrencies {
                return LoadCurrencies(Status.SUCCESS, currencies, null)
            }
            fun createError(throwable: Throwable) : LoadCurrencies {
                return LoadCurrencies(Status.FAILURE, null, throwable)
            }
            fun createLoading(): LoadCurrencies {
                return LoadCurrencies(Status.LOADING, null, null)
            }
        }
    }

    // got exchange response back from api
    class Convert(status: Status,
                  val exchangeRate: Double? = null,
                  error: Throwable?,
                  val currencyFrom: String? = null,
                  val currencyTo: String? = null,
                  val quantity: Double? = null) : CurrencyResult(status, error) {
        companion object {
            fun createSuccess(exchangeRate: Double, currencyFrom: String, currencyTo: String, quantity: Double) : Convert {
                return Convert(Status.SUCCESS, exchangeRate, null, currencyFrom, currencyTo, quantity)
            }
            fun createError(throwable: Throwable, currencyFrom: String?, currencyTo: String?, quantity: Double?) : Convert {
                return Convert(Status.FAILURE, null, throwable, currencyFrom, currencyTo, quantity)
            }
            fun createLoading(currencyFrom: String?, currencyTo: String?, quantity: Double?) : Convert {
                return Convert(Status.LOADING, null, null, currencyFrom, currencyTo, quantity)
            }
        }
    }

}