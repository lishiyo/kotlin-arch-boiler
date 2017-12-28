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

import android.app.AlertDialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.currency_fragment.*
import lishiyo.kotlin_arch.R
import lishiyo.kotlin_arch.mvibase.MviView


class CurrencyFragment : Fragment(), MviView<CurrencyIntent, CurrencyViewState> {
  // Current list of currencies
  private val currencies = ArrayList<String>()
  private var currenciesAdapter: ArrayAdapter<String>? = null
  private var currencyFrom: String? = null
  private var currencyTo: String? = null

  private lateinit var currencyViewModel: CurrencyViewModel

  private val mConvertPublisher = PublishSubject.create<CurrencyIntent.Convert>()
  private val mLoadCurrenciesPublisher = PublishSubject.create<CurrencyIntent.LoadCurrencies>()
  private val mRefreshPublisher = PublishSubject.create<CurrencyIntent.Refresh>()

  // Stream of ALL intents that should push to ViewModel
  override fun intents(): Observable<out CurrencyIntent> {
    return Observable.merge(
            Observable.just(CurrencyIntent.Initial.create()), // send out initial intent immediately
            mLoadCurrenciesPublisher,
            mConvertPublisher,
            mRefreshPublisher
    )
  }

  override fun render(state: CurrencyViewState) {
    // render based on the current view state

    // alert with an error message
    val err = state.error
    err?.let {
      showResult("there was an error: ${err.localizedMessage}}")
    }

    if (state.isLoading) {
      // loading
      Log.i("connie", "CurrencyFrag ++ render loading...")
    } else if (state.error == null) {
      // success
      Log.i("connie", "CurrencyFrag ++ render success!")
    }

    // add currencies
    state.currencies.value?.let {
      Log.i("connie", "got currencies list!")
      it.forEach { currencies.add(it.code + "  " + it.country) }
      currenciesAdapter?.notifyDataSetChanged()
    }

    // todo: handle convert

  }

  companion object {
    fun newInstance() = CurrencyFragment()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.currency_fragment, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initViewModel()  // sends off INIT intent

    initSpinners()
    initConvertButton()
    currenciesAdapter!!.setDropDownViewResource(R.layout.item_spinner)

    // send off LOAD intent immediately
//    populateSpinnerAdapter()
    mLoadCurrenciesPublisher.onNext(CurrencyIntent.LoadCurrencies.create())
  }


  private fun initViewModel() {
    currencyViewModel = ViewModelProviders.of(this).get(CurrencyViewModel::class.java)

    // add viewmodel as an observer of this fragment lifecycle
    currencyViewModel.let { lifecycle.addObserver(it) }

    // Subscribe to the viewmodel states with LiveData, not Rx
    currencyViewModel.states().observe(this, Observer { state ->
      Log.i("connie", "CurrencyFrag ++ got new state! rendering")
      state?.let {
        this.render(state)
      }
    })

    // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
    currencyViewModel.processIntents(intents())

//    currencyViewModel.initLocalCurrencies()
  }

//  private fun populateSpinnerAdapter() {
////    currencyViewModel?.loadCurrencyList()?.observe(this, Observer { currencyList ->
////      currencyList!!.forEach {
////        currencies.add(it.code + "  " + it.country)
////      }
////      currenciesAdapter!!.setDropDownViewResource(R.layout.item_spinner)
////      currenciesAdapter!!.notifyDataSetChanged()
////    })
//  }

  private fun initSpinners() {
    currenciesAdapter = ArrayAdapter(activity, R.layout.item_spinner, currencies)
    from_currency_spinner.adapter = currenciesAdapter
    from_currency_spinner.setSelection(0)
    to_currency_spinner.adapter = currenciesAdapter
    to_currency_spinner.setSelection(0)
  }

  private fun initConvertButton() {
    convert_button.setOnClickListener {
      convert()
    }
  }

  // You can move all this logic to the view model
  private fun convert() {
    val quantity = currency_edit.text.toString()
    currencyFrom = getCurrencyCode(from_currency_spinner.selectedItem.toString())
    currencyTo = getCurrencyCode(to_currency_spinner.selectedItem.toString())
//    val currencies = currencyFrom + "," + currencyTo

    if (quantity.isNotEmpty() && currencyFrom != currencyTo) {
//      currencyViewModel
//              ?.getAvailableExchangeAsLiveData(currencies)
//              ?.observe(this, Observer { availableExchange ->
//                exchange(quantity.toDouble(), availableExchange!!.availableExchangesMap)
//              })
      mConvertPublisher.onNext(CurrencyIntent.Convert.create(currencyFrom!!, currencyTo!!, quantity.toDouble()))
    } else {
      Toast.makeText(activity, "Could not convert.", Toast.LENGTH_SHORT).show()
    }
  }

//  private fun exchange(quantity: Double, availableExchangesMap: Map<String, Double>) {
//    val exchangesKeys = availableExchangesMap.keys.toList()
//    val exchangesValues = availableExchangesMap.values.toList()
//
//    val fromCurrency = exchangesValues[0]
//    val toCurrency = exchangesValues[1]
//
//    val fromCurrencyKey = getCurrencyCodeResult(exchangesKeys[0])
//    val toCurrencyKey = getCurrencyCodeResult(exchangesKeys[1])
//
//    // process quantity /
//    val usdExchange = quantity.div(fromCurrency)
//    val exchangeResult = usdExchange.times(toCurrency)
//
//    val convertedTotal = quantity.toString() + " " + fromCurrencyKey + " = " + exchangeResult.format(4) + " " + toCurrencyKey
//    showResult(convertedTotal)
//  }

  private fun showResult(result: String) {
    val builder: AlertDialog.Builder
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder = AlertDialog.Builder(context!!, R.style.AppCompatAlertDialogStyle)
    } else {
      builder = AlertDialog.Builder(context!!)
    }

    val setMessage = TextView(activity)
    setMessage.text = result
    setMessage.gravity = Gravity.CENTER_HORIZONTAL
    builder.setView(setMessage)
    builder.setTitle(getString(R.string.currency_converter))
        .setPositiveButton(android.R.string.yes, null)
        .setIcon(R.drawable.ic_attach_money_black_24dp)
        .show()
  }

  private fun getCurrencyCode(currency: String) = currency.substring(0, 3)

  private fun getCurrencyCodeResult(currency: String) = currency.substring(3)

  private fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

}
