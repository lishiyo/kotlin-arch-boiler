package lishiyo.kotlin_arch.domain

import lishiyo.kotlin_arch.mvibase.MviResult

/**
 * Created by connieli on 12/26/17.
 */
sealed class CurrencyResult : MviResult {
    enum class Status {
        LOADING, SUCCESS, FAILURE
    }

    // seeded the database
    class Seeded(val status: Status) : CurrencyResult() {
        companion object {
            fun createSuccess() : Seeded {
                return Seeded(Status.SUCCESS)
            }

            fun createError() : Seeded {
                return Seeded(Status.FAILURE)
            }
        }
    }

    // currencies retrieved from db
    class CurrenciesLoaded(val status: Status, val currencies: List<Currency>?) : CurrencyResult() {
        companion object {
            fun createSuccess(currencies: List<Currency>) : CurrenciesLoaded {
                return CurrenciesLoaded(Status.SUCCESS, currencies)
            }
            fun createError(throwable: Throwable) : CurrenciesLoaded {
                return CurrenciesLoaded(Status.FAILURE, null)
            }
            fun createLoading(): CurrenciesLoaded {
                return CurrenciesLoaded(Status.LOADING, null)
            }
        }
    }

    // got exchange response back from api
    class Converted(val status: Status, val exchangeRate: Double?) : CurrencyResult() {
        companion object {
            fun createSuccess(exchangeRate: Double) : Converted {
                return Converted(Status.SUCCESS, exchangeRate)
            }

            fun createError(throwable: Throwable) : Converted {
                return Converted(Status.FAILURE, null)
            }
        }
    }

}