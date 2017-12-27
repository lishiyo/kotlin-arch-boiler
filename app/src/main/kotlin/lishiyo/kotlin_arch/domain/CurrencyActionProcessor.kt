package lishiyo.kotlin_arch.domain

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
    val combinedProcessor: ObservableTransformer<in CurrencyAction, out CurrencyResult> = ObservableTransformer {
        acts -> acts.publish<CurrencyResult> { shared: Observable<in CurrencyAction> -> Observable.merge<CurrencyResult>(
            shared.ofType<CurrencyAction.Seed>(CurrencyAction.Seed::class.java).compose(seedDatabaseProcessor),
            shared.ofType<CurrencyAction.LoadCurrencies>(CurrencyAction.LoadCurrencies::class.java).compose(loadCurrenciesProcessor)
        ).mergeWith(
            // Error for not implemented actions
            shared.filter { v ->
                (v !is CurrencyAction.Seed && v !is CurrencyAction.LoadCurrencies && v !is CurrencyAction.Convert)
            }.flatMap { w -> Observable.error<CurrencyResult>(IllegalArgumentException("Unknown Action type: " + w)) })
        }
    }


    // ==== individual list of processors (action -> result) ====

    // seed the database
    private val seedDatabaseProcessor: ObservableTransformer<CurrencyAction.Seed, CurrencyResult.Seeded> = ObservableTransformer {
        acts -> acts.flatMap { act -> currencyRepository.getTotalCurrencies().toObservable().subscribeOn(schedulerProvider.io()) }
            .filter { count -> isRoomEmpty(count) } // populate room if it's empty
            .doOnNext { _ -> currencyRepository.addCurrencies() }
            .map { _ -> CurrencyResult.Seeded.createSuccess() }
            .onErrorReturn { err -> CurrencyResult.Seeded.createError(err) }
    }

    // load the currencies
    private val loadCurrenciesProcessor: ObservableTransformer<CurrencyAction.LoadCurrencies, CurrencyResult.CurrenciesLoaded> =
            ObservableTransformer {
        acts -> acts.startWith { _ -> CurrencyResult.CurrenciesLoaded.createLoading() }
                    .flatMap { _ -> currencyRepository.getCurrenciesLocal().toObservable().subscribeOn(schedulerProvider.io()) }
                    .filter { list -> !list.isEmpty() }
                    .map { list -> CurrencyResult.CurrenciesLoaded.createSuccess(list) }
                    .onErrorReturn{ err -> CurrencyResult.CurrenciesLoaded.createError(err) }
    }

    private fun isRoomEmpty(currenciesTotal: Int) = currenciesTotal == 0

}