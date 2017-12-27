package lishiyo.kotlin_arch.utils.schedulers

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Provides different types of schedulers.
 */
object SchedulerProvider : BaseSchedulerProvider {

    override fun computation(): Scheduler {
        return Schedulers.computation()
    }

    override fun io(): Scheduler {
        return Schedulers.io()
    }

    override fun ui(): Scheduler {
        return AndroidSchedulers.mainThread()
    }

//    companion object {
//
//
//
//        private var INSTANCE: SchedulerProvider? = null
//
//        val instance: SchedulerProvider
//            @Synchronized get() {
//                if (INSTANCE == null) {
//                    INSTANCE = SchedulerProvider()
//                }
//                return INSTANCE
//            }
//    }
}
