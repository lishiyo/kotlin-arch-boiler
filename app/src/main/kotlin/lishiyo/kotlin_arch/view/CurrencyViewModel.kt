/**
 * Copyright 2017 Erik Jhordan Rey.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lishiyo.kotlin_arch.view

import android.arch.lifecycle.*
import android.arch.lifecycle.Lifecycle.Event.ON_DESTROY
import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.observables.ConnectableObservable
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.data.repository.CurrencyRepository
import lishiyo.kotlin_arch.di.CurrencyApplication
import lishiyo.kotlin_arch.domain.CurrencyAction
import lishiyo.kotlin_arch.domain.CurrencyActionProcessor
import lishiyo.kotlin_arch.domain.CurrencyResult
import lishiyo.kotlin_arch.mvibase.MviIntent
import lishiyo.kotlin_arch.mvibase.MviViewModel
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import java.util.*
import javax.inject.Inject

class CurrencyViewModel : ViewModel(), LifecycleObserver, MviViewModel<CurrencyIntent, CurrencyViewState> {
    // Dagger
    @Inject lateinit var currencyRepository: CurrencyRepository
    @Inject lateinit var actionProcessor: CurrencyActionProcessor
    @Inject lateinit var schedulerProvider: SchedulerProvider

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped list of currencies
//    private val liveCurrencyData: LiveData<List<Currency>> by lazy { MutableLiveData<List<Currency>>() }

    // LiveData-wrapped exchange model
//    private val liveAvailableExchange: MutableLiveData<AvailableExchange> by lazy { MutableLiveData<AvailableExchange>() }

    // LiveData-wrapped current ViewState
    private val liveViewState: MutableLiveData<CurrencyViewState> by lazy { MutableLiveData<CurrencyViewState>() }

    // subject to publish currency view states
    private val intentsSubject : PublishSubject<CurrencyIntent> by lazy { PublishSubject.create<CurrencyIntent>() }

    /**
     * take only the first ever InitialIntent and all intents of other types
     * to avoid reloading data on config changes
     */
    private val intentFilter: ObservableTransformer<CurrencyIntent, CurrencyIntent> = ObservableTransformer { intents ->
//        intents.filter{ intent -> intent is CurrencyIntent }
        intents.publish { shared -> shared
            Observable.merge<CurrencyIntent>(
                    shared.ofType(CurrencyIntent.Initial::class.java).take(1), // only take initial one time
                    shared.filter({ intent -> intent !is CurrencyIntent.Initial })
            )
        }
    }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<CurrencyViewState, CurrencyResult, CurrencyViewState> = BiFunction { previousState, result ->
        Log.i("connie", "reducer ++ result: ${result.javaClass.simpleName} with status: ${result.status}")
        when (result) {
            is CurrencyResult.Seed -> return@BiFunction createSeedResult(previousState, result)
            is CurrencyResult.LoadCurrencies -> return@BiFunction createLoadCurrenciesResult(previousState, result)
            is CurrencyResult.Convert -> return@BiFunction createConvertResult(previousState, result)
            else -> throw IllegalArgumentException("Don't know this convertedTotal " + result)
        }
    }

    // ===== Individual reducers ======

    private fun createSeedResult(previousState: CurrencyViewState, result: CurrencyResult.Seed): CurrencyViewState {
        val newState: CurrencyViewState = previousState.copy()
        newState.error = null

        when (result.status) {
            CurrencyResult.Status.LOADING -> {
                newState.isLoading = true
            }
            CurrencyResult.Status.SUCCESS, CurrencyResult.Status.IDLE -> {
                newState.isLoading = false
            }
            CurrencyResult.Status.FAILURE -> {
                newState.isLoading = false
                newState.error = result.error
            }
        }

        return newState
    }

    private fun createLoadCurrenciesResult(previousState: CurrencyViewState, result: CurrencyResult.LoadCurrencies): CurrencyViewState {
        val newState: CurrencyViewState = previousState.copy()
        newState.error = null

        when (result.status) {
            CurrencyResult.Status.LOADING -> {
                newState.isLoading = true
                newState.currencies.postValue(Collections.emptyList()) // clear out previous
            }
            CurrencyResult.Status.SUCCESS -> {
                newState.isLoading = false
                newState.currencies.postValue(result.currencies)
            }
            CurrencyResult.Status.FAILURE -> {
                newState.isLoading = false
                newState.error = result.error
            }
        }

        return newState
    }

    private fun createConvertResult(previousState: CurrencyViewState, result: CurrencyResult.Convert): CurrencyViewState {
        val newState: CurrencyViewState = previousState.copy()
        newState.error = null
        newState.convertedTotal = null
        newState.currencyFrom = result.currencyFrom
        newState.currencyTo = result.currencyTo
        newState.quantity = result.quantity

        when (result.status) {
            CurrencyResult.Status.LOADING -> {
                newState.isLoading = true
                newState.currencies.postValue(Collections.emptyList()) // clear out
            }
            CurrencyResult.Status.SUCCESS -> {
                newState.isLoading = false

                // calculate converted result
                val exchangeRate = result.exchangeRate
                val quantity = result.quantity
                if (quantity!! > 0 && exchangeRate!! > 0) {
                    val totalQuantity = quantity * exchangeRate
                    newState.convertedTotal = totalQuantity
                }
            }
            CurrencyResult.Status.FAILURE -> {
                newState.isLoading = false
                newState.error = result.error
            }
        }

        return newState
    }

    init {
        // inject repo, actionprocessor, scheduler
        initializeDagger()

        // create observable to push into states live data
        val observable: ConnectableObservable<CurrencyViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe{ Log.i("connie", "subscribed!") }
                .doOnDispose{ Log.i("connie", "disposed!") }
                .doOnTerminate { Log.i("connie", "terminated!") }
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .doOnNext { intent -> Log.i("Connie", "ViewModel ++ intentsSubject hitCompose: ${intent.javaClass.name}") }
                .compose(actionProcessor.combinedProcessor)
                .scan(CurrencyViewState.IDLE, reducer)
                // Emit the last one event of the stream on subscription
                // Useful when a View rebinds to the ViewModel after rotation.
                .replay(1)
                // Create the stream on creation without waiting for anyone to subscribe
                // This allows the stream to stay alive even when the UI disconnects and
                // match the stream's lifecycle to the ViewModel's one.
//                .autoConnect(0) // automatically connect

        compositeDisposable.add(
                observable.subscribe({ currencyViewState ->
                    Log.i("connie", "ViewModel ++ new currencyViewState posted: $currencyViewState")
                    liveViewState.postValue(currencyViewState) // should be on main thread (if worker, use postValue)
                }, { err ->
                    Log.i("connie", "ViewModel ++ ERROR " + err.localizedMessage)
                })
        )

        observable.autoConnect(0)
    }

    override fun processIntents(intents: Observable<out CurrencyIntent>) {
        Log.i("connie", "ViewModel ++ process intents!")
        intents.subscribe(intentsSubject)
    }

    override fun states(): LiveData<CurrencyViewState> {
       return liveViewState
    }

    private fun actionFromIntent(intent: MviIntent) : CurrencyAction {
        Log.i("connie", "actionFromIntent ${intent.javaClass.simpleName}")
        when(intent) {
            is CurrencyIntent.Initial -> return CurrencyAction.Seed.create()
            is CurrencyIntent.LoadCurrencies -> return CurrencyAction.LoadCurrencies.create()
            is CurrencyIntent.Convert -> return CurrencyAction.Convert.create(intent.currencyFrom, intent.currencyTo, intent.quantity)
            is CurrencyIntent.Refresh -> return CurrencyAction.LoadCurrencies.create() // TODO update later
        }

        throw IllegalArgumentException("do not know how to treat this intent " + intent)
    }

//    fun getAvailableExchange(currencyFrom: String, currencyTo: String): LiveData<AvailableExchange>? {
////    liveAvailableExchange = MutableLiveData<AvailableExchange>()
//        val currencies = currencyFrom + "," + currencyTo
//        liveAvailableExchange = currencyRepository.getAvailableExchangeAsLiveData(currencies)
//        return liveAvailableExchange
//    }
//
//    fun loadCurrencyList(): LiveData<List<Currency>>? {
////    if (liveCurrencyData == null) {
////      liveCurrencyData = MutableLiveData<List<Currency>>()
////      liveCurrencyData = currencyRepository.getCurrencyListAsLiveData()
////    }
//        return liveCurrencyData
//    }

//    fun initLocalCurrencies() { // TODO Load intent
//        // seed room db if necessary
//        val disposable = currencyRepository.getTotalCurrencyCounts()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe {
//                    if (isRoomEmpty(it)) {
//                        populate() // populate with seed data
//                    } else {
//                        Log.i(CurrencyRepository::class.java.simpleName, "DataSource has been already Populated")
//                    }
//                }
//        compositeDisposable.add(disposable)
//    }

    @OnLifecycleEvent(ON_DESTROY)
    fun unSubscribeViewModel() {
        Log.i("connie", "ViewModel ++ unsubscribe, clear out disposables")
        // clear out repo subscriptions
        for (disposable in currencyRepository.allCompositeDisposable) {
            compositeDisposable.addAll(disposable)
        }
        compositeDisposable.clear()
    }

//    private fun isRoomEmpty(currenciesTotal: Int) = currenciesTotal == 0

//    private fun populate() {
//        Completable.fromAction { currencyRepository.addCurrencies() }
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(object : CompletableObserver {
//                    override fun onSubscribe(@NonNull d: Disposable) {
//                        compositeDisposable.add(d)
//                    }
//
//                    override fun onComplete() {
//                        Log.i(CurrencyRepository::class.java.simpleName, "DataSource has been Populated")
//
//                    }
//
//                    override fun onError(@NonNull e: Throwable) {
//                        e.printStackTrace()
//                        Log.e(CurrencyRepository::class.java.simpleName, "DataSource hasn't been Populated")
//                    }
//                })
//    }

    override fun onCleared() {
        unSubscribeViewModel()
        super.onCleared()
    }

    private fun initializeDagger() = CurrencyApplication.appComponent.inject(this)

}


