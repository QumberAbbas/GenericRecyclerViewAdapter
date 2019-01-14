package com.zeyad.gadapter.Observables;

import com.zeyad.gadapter.GenericRecyclerViewAdapter;
import com.zeyad.gadapter.GenericViewHolder;
import com.zeyad.gadapter.ItemInfo;
import com.zeyad.gadapter.OnItemClickListener;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;

import static com.zeyad.gadapter.Utils.checkMainThread;

public final class ItemClickObservable extends Observable<ClickEvent> {
    private final GenericRecyclerViewAdapter genericRecyclerViewAdapter;

    public ItemClickObservable(GenericRecyclerViewAdapter genericRecyclerViewAdapter) {
        this.genericRecyclerViewAdapter = genericRecyclerViewAdapter;
    }

    @Override
    protected void subscribeActual(Observer<? super ClickEvent> observer) {
        if (!checkMainThread(observer)) {
            return;
        }
        Listener listener = new Listener(observer);
        observer.onSubscribe(listener);
        genericRecyclerViewAdapter.setOnItemClickListener(listener.onItemClickListener);
    }

    final class Listener extends MainThreadDisposable {
        private final OnItemClickListener onItemClickListener;

        Listener(final Observer<? super ClickEvent> observer) {
            this.onItemClickListener = new OnItemClickListener() {
                @Override
                public void onItemClicked(int position, ItemInfo itemInfo, GenericViewHolder holder) {
                    if (!isDisposed()) {
                        observer.onNext(new ClickEvent(position, itemInfo, holder));
                    }
                }
            };
        }

        @Override
        protected void onDispose() {
        }
    }
}
