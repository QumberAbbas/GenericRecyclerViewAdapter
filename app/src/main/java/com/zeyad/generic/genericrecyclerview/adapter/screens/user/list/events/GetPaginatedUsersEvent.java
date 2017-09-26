package com.zeyad.generic.genericrecyclerview.adapter.screens.user.list.events;

import com.zeyad.rxredux.core.redux.BaseEvent;

/** @author by ZIaDo on 4/19/17. */
public class GetPaginatedUsersEvent implements BaseEvent {

    private final long lastId;

    public GetPaginatedUsersEvent(long lastId) {
        this.lastId = lastId;
    }

    public long getLastId() {
        return lastId;
    }
}