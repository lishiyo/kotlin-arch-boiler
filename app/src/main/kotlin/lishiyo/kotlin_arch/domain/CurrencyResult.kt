package lishiyo.kotlin_arch.domain

import lishiyo.kotlin_arch.mvibase.MviResult

/**
 * Results to feed back to the view model.
 *
 * Created by connieli on 12/26/17.
 */
sealed class CurrencyResult : MviResult {
    enum class Status {
        LOADING, SUCCESS, FAILURE
    }

    // seeded the database
    class Seeded(val status: Status, val err: Throwable?) : CurrencyResult() {
        companion object {
            fun createSuccess() : Seeded {
                return Seeded(Status.SUCCESS, null)
            }

            fun createError(throwable: Throwable) : Seeded {
                return Seeded(Status.FAILURE, throwable)
            }
        }
    }

    // currencies retrieved from db
    class CurrenciesLoaded(val status: Status, val currencies: List<Currency>?, val err: Throwable?) : CurrencyResult() {
        companion object {
            fun createSuccess(currencies: List<Currency>) : CurrenciesLoaded {
                return CurrenciesLoaded(Status.SUCCESS, currencies, null)
            }
            fun createError(throwable: Throwable) : CurrenciesLoaded {
                return CurrenciesLoaded(Status.FAILURE, null, throwable)
            }
            fun createLoading(): CurrenciesLoaded {
                return CurrenciesLoaded(Status.LOADING, null, null)
            }
        }
    }

    // got exchange response back from api
    class Converted(val status: Status, val exchangeRate: Double?, val err: Throwable?) : CurrencyResult() {
        companion object {
            fun createSuccess(exchangeRate: Double) : Converted {
                return Converted(Status.SUCCESS, exchangeRate, null)
            }

            fun createError(throwable: Throwable) : Converted {
                return Converted(Status.FAILURE, null, throwable)
            }
        }
    }

}