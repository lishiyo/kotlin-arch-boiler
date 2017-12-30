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

package lishiyo.kotlin_arch.data.repository

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import lishiyo.kotlin_arch.data.remote.CurrencyResponse
import lishiyo.kotlin_arch.data.remote.RemoteCurrencyDataSource
import lishiyo.kotlin_arch.data.room.CurrencyEntity
import lishiyo.kotlin_arch.data.room.RoomCurrencyDataSource
import lishiyo.kotlin_arch.domain.AvailableExchange
import lishiyo.kotlin_arch.domain.Currency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetch data from local and remote sources.
 */
@Singleton
class CurrencyRepository @Inject constructor(
    private val roomCurrencyDataSource: RoomCurrencyDataSource,
    private val remoteCurrencyDataSource: RemoteCurrencyDataSource
) : Repository {

  val allCompositeDisposable: MutableList<Disposable> = arrayListOf()

  // Stream of total currency counts
  override fun getTotalCurrencyCounts(): Flowable<Int> = roomCurrencyDataSource.currencyDao().getCurrenciesTotal()

  override fun addCurrencies() {
    // populate with seed data
    Log.i("connie", "CurrencyRepository ++ addCurrencies!")
    val currencyEntityList = RoomCurrencyDataSource.getAllCurrencies()
    roomCurrencyDataSource.currencyDao().insertAll(currencyEntityList)
  }

  // Stream of currencies from database
  override fun getAllCurrencies(): Flowable<List<Currency>> {
    return roomCurrencyDataSource.currencyDao()
            .getAllCurrencies()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { currencyList -> transform(currencyList) }
  }

  // Hit api for the exchange rate map
  override fun getAvailableExchange(currencyString: String): Observable<AvailableExchange> {
    return remoteCurrencyDataSource.requestAvailableExchange(currencyString)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { currencyResponse -> transform(currencyResponse) }
  }

  // Map currency entities from database into domain model Currencies
  private fun transform(currencies: List<CurrencyEntity>): List<Currency> {
    val currencyList = ArrayList<Currency>()
    currencies.forEach {
      currencyList.add(Currency(it.countryCode, it.countryName))
    }
    return currencyList
  }

  // map CurrencyResponse to domain model AvailableExchange
  private fun transform(exchangeMap: CurrencyResponse): AvailableExchange {
    return AvailableExchange(exchangeMap.currencyQuotes)
  }

}
