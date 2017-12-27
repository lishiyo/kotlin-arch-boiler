package lishiyo.kotlin_arch.domain

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.schedulers.Schedulers
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



    // private list of processors (action -> result)
    private val seedDatabaseProcessor: ObservableTransformer<CurrencyAction.Seed, CurrencyResult.Seeded> = ObservableTransformer {
        acts -> acts.flatMap { act -> currencyRepository.getTotalCurrencies().subscribeOn(Schedulers.io()).toObservable()
            }.filter { count -> isRoomEmpty(count) }
            .doOnNext { _ -> currencyRepository.addCurrencies() }
            .map { _ -> CurrencyResult.Seeded.createSuccess() }
    }

    private val loadCurrenciesProcessor: ObservableTransformer<CurrencyAction.LoadCurrencies, CurrencyResult.CurrenciesLoaded> =
            ObservableTransformer {
        acts -> acts.map { _ -> CurrencyResult.CurrenciesLoaded.createError(Throwable()) }
    }

    private fun isRoomEmpty(currenciesTotal: Int) = currenciesTotal == 0


}