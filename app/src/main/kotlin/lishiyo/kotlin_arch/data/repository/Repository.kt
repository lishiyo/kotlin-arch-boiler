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

import android.arch.lifecycle.LiveData
import io.reactivex.Flowable
import lishiyo.kotlin_arch.domain.AvailableExchange
import lishiyo.kotlin_arch.domain.Currency

// Data source for currencies
interface Repository {

  fun getTotalCurrencies(): Flowable<Int>

  fun addCurrencies()

  fun getCurrencyListAsLiveData(): LiveData<List<Currency>>

  fun getAvailableExchangeAsLiveData(currencies: String): LiveData<AvailableExchange>

}
