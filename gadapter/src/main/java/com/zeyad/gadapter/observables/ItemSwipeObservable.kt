package com.zeyad.gadapter.observables

import com.zeyad.gadapter.GenericAdapter
import com.zeyad.gadapter.ItemInfo
import com.zeyad.gadapter.OnSwipeListener
import com.zeyad.gadapter.checkMainThread
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable

class ItemSwipeObservable(private val genericRecyclerViewAdapter: GenericAdapter) : Observable<ItemInfo<*>>() {

    override fun subscribeActual(observer: Observer<in ItemInfo<*>>) {
        if (!observer.checkMainThread()) {
            return
        }
        val listener = Listener(observer)
        observer.onSubscribe(listener)
        genericRecyclerViewAdapter.onSwipeListener = listener.onSwipeListener
    }

    internal inner class Listener(observer: Observer<in ItemInfo<*>>) : MainThreadDisposable() {
        internal val onSwipeListener: OnSwipeListener

        init {
            this.onSwipeListener = object : OnSwipeListener {
                override fun onItemSwipe(itemInfo: ItemInfo<*>) {
                    if (!isDisposed) {
                        observer.onNext(itemInfo)
                    }
                }
            }
        }

        override fun onDispose() {}
    }
}
