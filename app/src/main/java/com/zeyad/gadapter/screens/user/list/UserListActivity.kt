package com.zeyad.gadapter.screens.user.list

import android.app.ActivityOptions
import android.app.SearchManager
import android.content.Context
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.*
import android.widget.ImageView
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import com.zeyad.gadapter.*
import com.zeyad.gadapter.ItemInfo.Companion.SECTION_HEADER
import com.zeyad.gadapter.fastscroll.SectionTitleProvider
import com.zeyad.gadapter.screens.BaseActivity
import com.zeyad.gadapter.screens.user.User
import com.zeyad.gadapter.screens.user.detail.UserDetailActivity
import com.zeyad.gadapter.screens.user.detail.UserDetailFragment
import com.zeyad.gadapter.screens.user.detail.UserDetailState
import com.zeyad.gadapter.screens.user.list.viewHolders.EmptyViewHolder
import com.zeyad.gadapter.screens.user.list.viewHolders.SectionHeaderViewHolder
import com.zeyad.gadapter.screens.user.list.viewHolders.UserViewHolder
import com.zeyad.gadapter.stickyheaders.StickyLayoutManager
import com.zeyad.gadapter.utils.hasLollipop
import com.zeyad.rxredux.core.BaseEvent
import com.zeyad.rxredux.core.view.ErrorMessageFactory
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_user_list.*
import kotlinx.android.synthetic.main.user_list.*
import kotlinx.android.synthetic.main.view_progress.*
import org.koin.android.viewmodel.ext.android.getViewModel
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * An activity representing a list of Repos. This activity has different presentations for handset
 * and tablet-size devices. On handsets, the activity presents a list of items, which when touched,
 * lead to a [UserDetailActivity] representing item details. On tablets, the activity presents
 * the list of items and item details side-by-side using two vertical panes.
 */
class UserListActivity : BaseActivity<UserListState, UserListVM>(), OnStartDragListener, ActionMode.Callback {

    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var usersAdapter: GenericRecyclerViewAdapter
    private var actionMode: ActionMode? = null
    private var currentFragTag: String = ""
    private var twoPane: Boolean = false
    private var eventObservable: Observable<BaseEvent<*>> = Observable.empty()
    private val postOnResumeEvents = PublishSubject.create<BaseEvent<*>>()

    override fun errorMessageFactory(): ErrorMessageFactory =
            { throwable, _ -> throwable.localizedMessage }

    override fun initialize() {
        viewModel = getViewModel()
        if (viewState == null) {
            eventObservable = Single.just<BaseEvent<*>>(GetPaginatedUsersEvent(0))
                    .filter { viewState !is GetState }
                    .doOnSuccess { Log.d("GetPaginatedUsersEvent", FIRED) }.toObservable()
        }
    }

    override fun setupUI(isNew: Boolean) {
        setContentView(R.layout.activity_user_list)
        setSupportActionBar(toolbar)
        toolbar.title = title
        setupRecyclerView()
        twoPane = findViewById<View>(R.id.user_detail_container) != null
    }

    override fun initialState(): UserListState = EmptyState()

    override fun events(): Observable<BaseEvent<*>> = eventObservable.mergeWith(postOnResumeEvents())

    private fun postOnResumeEvents(): Observable<BaseEvent<*>> = postOnResumeEvents

    override fun renderSuccessState(successState: UserListState) {
        usersAdapter.setDataList(successState.list, successState.callback)
    }

    override fun toggleViews(isLoading: Boolean, event: BaseEvent<*>) {
        linear_layout_loader.bringToFront()
        linear_layout_loader.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun showError(errorMessage: String, event: BaseEvent<*>) {
        showErrorSnackBarWithAction(errorMessage, user_list, "Retry",
                View.OnClickListener { postOnResumeEvents.onNext(event) })
    }

    private fun setupRecyclerView() {
        usersAdapter = object : GenericRecyclerViewAdapter(
                getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater, ArrayList()) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder<*> =
                    when (viewType) {
                        SECTION_HEADER -> SectionHeaderViewHolder(layoutInflater
                                .inflate(R.layout.section_header_layout, parent, false))
                        R.layout.empty_view -> EmptyViewHolder(layoutInflater
                                .inflate(R.layout.empty_view, parent, false))
                        R.layout.user_item_layout -> UserViewHolder(layoutInflater
                                .inflate(R.layout.user_item_layout, parent, false))
                        else -> throw IllegalArgumentException("Could not find view of type $viewType")
                    }
        }
        usersAdapter.setAreItemsClickable(true)
        usersAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClicked(position: Int, itemInfo: ItemInfo, holder: GenericViewHolder<*>) {
                if (actionMode != null) {
                    toggleItemSelection(position)
                } else if (itemInfo.getData<Any>() is User) {
                    val userModel = itemInfo.getData<User>()
                    val userDetailState = UserDetailState.builder().setUser(userModel).setIsTwoPane(twoPane)
                            .build()
                    var pair: android.util.Pair<View, String>? = null
                    var secondPair: android.util.Pair<View, String>? = null
                    if (hasLollipop()) {
                        val userViewHolder = holder as UserViewHolder
                        val avatar = userViewHolder.getAvatar()
                        pair = android.util.Pair.create(avatar, avatar.transitionName)
                        val textViewTitle = userViewHolder.getTextViewTitle()
                        secondPair = android.util.Pair.create(textViewTitle, textViewTitle.transitionName)
                    }
                    if (twoPane) {
                        if (currentFragTag.isNotBlank()) {
                            removeFragment(currentFragTag)
                        }
                        val orderDetailFragment = UserDetailFragment.newInstance(userDetailState)
                        currentFragTag = orderDetailFragment.javaClass.simpleName + userModel.id
                        addFragment(R.id.user_detail_container, orderDetailFragment, currentFragTag,
                                pair!!, secondPair!!)
                    } else {
                        if (hasLollipop()) {
                            val options =
                                    ActivityOptions.makeSceneTransitionAnimation(this@UserListActivity,
                                            pair, secondPair)
                            startActivity(UserDetailActivity.getCallingIntent(this@UserListActivity,
                                    userDetailState), options.toBundle())
                        } else {
                            startActivity(UserDetailActivity
                                    .getCallingIntent(this@UserListActivity, userDetailState))
                        }
                    }
                }
            }
        })
        usersAdapter.setOnItemLongClickListener(object : OnItemLongClickListener {
            override fun onItemLongClicked(position: Int, itemInfo: ItemInfo, holder: GenericViewHolder<*>): Boolean {
                if (usersAdapter.isSelectionAllowed) {
                    actionMode = startSupportActionMode(this@UserListActivity)
                    toggleItemSelection(position)
                }
                return true
            }
        })
        eventObservable = eventObservable.mergeWith(usersAdapter.itemSwipeObservable
                .map { itemInfo -> DeleteUsersEvent(listOf(itemInfo.getData<User>().login)) }
                .doOnEach { Log.d("DeleteEvent", FIRED) })
//        user_list.layoutManager = LinearLayoutManager(this)
        user_list.layoutManager = StickyLayoutManager(this, RecyclerView.VERTICAL, false, usersAdapter)
        user_list.adapter = usersAdapter
        usersAdapter.setAllowSelection(true)
        usersAdapter.sectionTitleProvider = object : SectionTitleProvider {
            override fun getSectionTitle(position: Int): String {
                return when (position) {
                    0 -> "1"
                    3 -> "2"
                    7 -> "3"
                    else -> "4"
                }
            }
        }
        fastscroll.setRecyclerView(user_list)
        eventObservable = eventObservable.mergeWith(RxRecyclerView.scrollEvents(user_list)
                .map { recyclerViewScrollEvent ->
                    GetPaginatedUsersEvent(
                            if (ScrollEventCalculator.isAtScrollEnd(recyclerViewScrollEvent))
                                viewState?.lastId!!
                            else -1)
                }
                .filter { it.getPayLoad() != -1L }
                .throttleLast(200, TimeUnit.MILLISECONDS, Schedulers.computation())
                .debounce(300, TimeUnit.MILLISECONDS, Schedulers.computation())
                .doOnNext { Log.d("NextPageEvent", FIRED) })
        itemTouchHelper = ItemTouchHelper(SimpleItemTouchHelperCallback(usersAdapter))
        itemTouchHelper.attachToRecyclerView(user_list)
    }

    private fun toggleItemSelection(position: Int) {
        usersAdapter.toggleSelection(position)
        val count = usersAdapter.selectedItemCount
        if (count == 0) {
            actionMode?.finish()
        } else {
            actionMode?.title = count.toString()
            actionMode?.invalidate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.list_menu, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.menu_search).actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.setOnCloseListener {
            postOnResumeEvents.onNext(GetPaginatedUsersEvent(viewState?.lastId!!))
            false
        }
        eventObservable = eventObservable.mergeWith(RxSearchView.queryTextChanges(searchView)
                .filter { charSequence -> charSequence.toString().isNotEmpty() }
                .throttleLast(100, TimeUnit.MILLISECONDS, Schedulers.computation())
                .debounce(200, TimeUnit.MILLISECONDS, Schedulers.computation())
                .map { query -> SearchUsersEvent(query.toString()) }
                .distinctUntilChanged()
                .doOnEach { Log.d("SearchEvent", FIRED) })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.selected_list_menu, menu)
        menu.findItem(R.id.delete_item).setOnMenuItemClickListener {
            postOnResumeEvents.onNext(DeleteUsersEvent(Observable.fromIterable(usersAdapter.selectedItems)
                    .map<String> { itemInfo -> itemInfo.getData<User>().login }.toList()
                    .blockingGet()))
            true
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        menu.findItem(R.id.delete_item).setVisible(true).isEnabled = true
        toolbar.visibility = View.GONE
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = item.itemId == R.id.delete_item

    override fun onDestroyActionMode(mode: ActionMode) {
        try {
            usersAdapter.clearSelection()
        } catch (e: Exception) {
            Log.e("onDestroyActionMode", e.message, e)
        }
        actionMode = null
        toolbar.visibility = View.VISIBLE
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    fun getImageViewAvatar(): ImageView = imageView_avatar

    companion object {
        const val FIRED = "fired!"
    }
}
