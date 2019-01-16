package com.zeyad.gadapter.screens.user.detail

import com.zeyad.gadapter.ItemInfo
import com.zeyad.gadapter.R
import com.zeyad.gadapter.utils.Constants.URLS.REPOSITORIES
import com.zeyad.rxredux.core.BaseEvent
import com.zeyad.rxredux.core.viewmodel.BaseViewModel
import com.zeyad.usecases.api.IDataService
import com.zeyad.usecases.db.RealmQueryProvider
import com.zeyad.usecases.requests.GetRequest
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.realm.Realm
import io.realm.RealmQuery

class UserDetailVM(private val dataUseCase: IDataService) : BaseViewModel<UserDetailState>() {

    override fun stateReducer():
            (newResult: Any, event: BaseEvent<*>, currentStateBundle: UserDetailState) -> UserDetailState =
            { newResult, _, currentStateBundle ->
                if (newResult is List<*>)
                    UserDetailState.builder()
                            .setRepos(Observable.fromIterable(newResult as List<Repository>)
                                    .map { repository -> ItemInfo(repository, R.layout.repo_item_layout) }
                                    .toList(newResult.size).blockingGet())
                            .setUser(currentStateBundle.user)
                            .setIsTwoPane(currentStateBundle.isTwoPane).build()
                else throw IllegalStateException("Can not reduce GetState with this result: $newResult!")
            }


    override fun mapEventsToActions(): Function<BaseEvent<*>, Flowable<*>> =
            Function { event -> getRepositories((event as GetReposEvent).getPayLoad()) }

    private fun getRepositories(userLogin: String): Flowable<List<Repository>> =
            dataUseCase.queryDisk(object : RealmQueryProvider<Repository> {
                override fun create(realm: Realm): RealmQuery<Repository> =
                        realm.where(Repository::class.java).equalTo("owner.login", userLogin)
            }).flatMap { list ->
                if (list.isNotEmpty())
                    Flowable.just(list)
                else
                    dataUseCase.getList(GetRequest.Builder(Repository::class.java, true)
                            .url(String.format(REPOSITORIES, userLogin)).build())
            }
}
