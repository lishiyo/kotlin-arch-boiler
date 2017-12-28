package lishiyo.kotlin_arch.domain

import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.data.repository.CurrencyRepository
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business logic - Process actions into results
 *
 * Created by connieli on 12/26/17.
 */
@Singleton
class CurrencyActionProcessor @Inject constructor(private val currencyRepository: CurrencyRepository,
                                                  private val schedulerProvider: BaseSchedulerProvider) {

    // main processor to combine then
    val combinedProcessor: ObservableTransformer<CurrencyAction, CurrencyResult> = ObservableTransformer {
        acts -> acts.publish { shared ->
            Observable.merge<CurrencyResult>(
                    shared.ofType<CurrencyAction.Seed>(CurrencyAction.Seed::class.java).compose(seedDatabaseProcessor),
                    shared.ofType<CurrencyAction.LoadCurrencies>(CurrencyAction.LoadCurrencies::class.java).compose(loadCurrenciesProcessor)
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v -> (v !is CurrencyAction.Seed && v !is CurrencyAction.LoadCurrencies && v !is CurrencyAction.Convert)
                            }.flatMap { w -> Observable.error<CurrencyResult>(IllegalArgumentException("Unknown Action type: " + w)) }
            )
        }
    }

    // ==== individual list of processors (action -> convertedTotal) ====

    // seed the database
    private val seedDatabaseProcessor: ObservableTransformer<CurrencyAction.Seed, CurrencyResult.Seed> = ObservableTransformer {
        acts -> acts.flatMap { act -> currencyRepository.getTotalCurrencyCounts().toObservable() }
            .doOnNext { count -> Log.i("connie", "SEED: got count $count")}
            .filter { count -> isRoomEmpty(count) } // populate room if it's empty
            .doOnNext { _ -> currencyRepository.addCurrencies() }
            .map { _ -> CurrencyResult.Seed.createSuccess() }
            .onErrorReturn { err -> CurrencyResult.Seed.createError(err) }
            .startWith(CurrencyResult.Seed.createLoading())
    }

    // load the currencies
    private val loadCurrenciesProcessor: ObservableTransformer<CurrencyAction.LoadCurrencies, CurrencyResult.LoadCurrencies> =
            ObservableTransformer {
        acts -> acts.flatMap { _ -> currencyRepository.getAllCurrencies().toObservable() }
                    .doOnNext { list -> Log.i("connie", "LOAD ++ list size: ${list.size}")}
                    .filter { list -> !list.isEmpty() }
                    .map { list -> CurrencyResult.LoadCurrencies.createSuccess(list) }
                    .onErrorReturn{ err -> CurrencyResult.LoadCurrencies.createError(err) }
                    .startWith(CurrencyResult.LoadCurrencies.createLoading())
    }

    private fun isRoomEmpty(currenciesTotal: Int) = currenciesTotal == 0

}