package pub.telephone.appKitApp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import pub.telephone.appKit.MyApp
import pub.telephone.appKit.dataSource.ComposableAdapter
import pub.telephone.appKit.dataSource.ComposableNode
import pub.telephone.appKit.dataSource.DataNode
import pub.telephone.appKit.dataSource.DataSource.DataSourceParameters
import pub.telephone.appKit.dataSource.DataSourceAdapter
import pub.telephone.appKit.dataSource.DataViewHolder
import pub.telephone.appKit.dataSource.DataViewHolder.DataViewHolderParameters.Inflater
import pub.telephone.appKit.dataSource.TagKey
import pub.telephone.appKit.dataSource.promised
import pub.telephone.appKitApp.databinding.ActivityCardListBinding
import pub.telephone.appKitApp.databinding.CardBinding
import pub.telephone.javahttprequest.network.http.HTTPMethod
import pub.telephone.javahttprequest.network.http.http
import pub.telephone.javapromise.async.kpromise.toKPromise
import java.lang.ref.WeakReference

private const val NAME = "cccccccc"

class CardListActivity : AppCompatActivity() {
    companion object {
        private fun DataNode<*>.buildResponseTask(x: String) = DataNode.RetrySharedTask.Simple {
            promised {
                rsp(
                    http {
                        Method = HTTPMethod.GET
                        URL = "https://guetcob.com/question"
//                        Proxy = NetworkProxy(java.net.Proxy.Type.HTTP, "192.168.1.53", 9090)
                        CustomizedHeaderList = listOf(arrayOf("idd", x))
                    }
                        .JSONArray().toKPromise()
                        .then {
                            delay(5000)
                            rsv(value)
                        }
                        .then { rsv(value.Result.getJSONArray(2).getString(1)) }
                )
            }.promise.toJavaPromise()
        }

        private fun DataNode<*>.buildSRCListTask() = DataNode.RetrySharedTask.Simple {
            promised { rsv(List(999) { it }) }.promise.toJavaPromise()
        }
    }

    class JavaState(params: DataNodeParameters<UI>) : DataNode<JavaState.UI>(params) {
        class UI(params: Inflater) :
            DataViewHolder<ActivityCardListBinding>(params, ActivityCardListBinding::class.java) {
            fun setLastDisposed(lastDisposed: Int) {
                view.lastDisposed.text = "last disposed: $lastDisposed"
            }
        }

        class ItemState(params: DataNodeParameters<UI>) : DataNode<ItemState.UI>(params) {
            class UI(params: Inflater) :
                DataViewHolder<CardBinding>(params, CardBinding::class.java)

            private val response: Binding<String> = bindTask(
                TagKey(R.id.tagKey_CardListActivityItem, R.id.tagInitKey_CardListActivityItem),
                buildResponseTask("")
            )

            override fun __Bind__(changedBindingKeys: MutableSet<Int>?) {
                response.Bind(response.BindParameters(changedBindingKeys)
                    .setInit { holder ->
                        holder.view.title.text = "hello $position"
                        null
                    }.setOnSucceed { holder, resp ->
                        holder.view.title.text = resp
                        null
                    })
            }
        }

        inner class Adapter(params: DataSourceParameters) :
            DataSourceAdapter<ItemState.UI, ItemState>(params) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemState.UI {
                return ItemState.UI(Inflater(LayoutInflater.from(parent.context), parent))
            }

            override fun beforeViewRecycled_ui(holder: ItemState.UI) {
                EmitChange_ui(mutableSetOf(lastDisposedBinding.SetResult(holder.adapterPosition)))
            }
        }

        inner class ComposeAdapter(params: DataSourceParameters) :
            DataSourceAdapter<ComposableNode.UI, State.ItemState>(params) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComposableNode.UI {
                return ComposableNode.createListViewHolder(
                    Inflater(LayoutInflater.from(parent.context), parent),
                    lifecycleOwner.get()!!,
                )
            }

            override fun beforeViewRecycled_ui(holder: ComposableNode.UI) {
                EmitChange_ui(mutableSetOf(lastDisposedBinding.SetResult(holder.adapterPosition)))
            }
        }

        private val srcList: Binding<List<Int>> = bindTask(
            TagKey(R.id.tagKey_CardListActivityItemList, R.id.tagInitKey_CardListActivityItemList),
            buildSRCListTask()
        )

        private val lastDisposedBinding: Binding<Int> = emptyBinding(
            TagKey(
                R.id.tagKey_CardListActivityLastDisposed,
                R.id.tagInitKey_CardListActivityLastDisposed
            ),
        )

        override fun __Bind__(changedBindingKeys: MutableSet<Int>?) {
            val useComposeChild = true
            srcList.Bind(srcList.BindParameters(changedBindingKeys)
                .setInit { holder ->
                    holder.view.cardList.run cardListRun@{
                        layoutManager = LinearLayoutManager(
                            context,
                            RecyclerView.VERTICAL,
                            false
                        )
                        adapter = if (useComposeChild) ComposeAdapter(
                            DataSourceParameters(this@cardListRun, lifecycleOwner)
                        ) else Adapter(
                            DataSourceParameters(this@cardListRun, lifecycleOwner)
                        )
                    }
                    null
                }.setOnSucceed { holder, list ->
                    if (useComposeChild) {
                        (holder.view.cardList.adapter as ComposeAdapter).Source.Append(list.map {
                            State.ItemState(DataNodeParameters.State(null), it)
                        })
                    } else {
                        (holder.view.cardList.adapter as Adapter).Source.Append(list.map {
                            ItemState(DataNodeParameters(null, null))
                        })
                    }
                    null
                })
            lastDisposedBinding.Bind(lastDisposedBinding.BindParameters(changedBindingKeys)
                .setInit { holder ->
                    holder.setLastDisposed(-1)
                    null
                }.setOnSucceed { holder, v ->
                    holder.setLastDisposed(v)
                    null
                })
        }
    }
    class State(params: DataNodeParameters.State) : ComposableNode(params) {
        class ItemState(
            params: DataNodeParameters.State,
            val id: Int
        ) : ComposableNode(params) {
            private val response: Binding<String> = bindTask(
                TagKey(R.id.tagKey_CardListActivityItem, R.id.tagInitKey_CardListActivityItem),
                buildResponseTask("$id")
            )

            override fun __Bind___(changedBindingKeys: MutableSet<Int>?) {
                response.Bind(response.BindParameters(changedBindingKeys))
            }

            @Composable
            override fun __Content__() {
                val resp by response.state
                Log.w(NAME, "__Content__: $id ${resp.status} ${this@ItemState}")
                Text(text = resp["hello $id"], fontSize = 20.sp)
                if (id == 2) {
                    LaunchedEffect(id) {
                        delay(8000)
                        MyApp.post {
                            Log.w(NAME, "LaunchedEffect: $id ${this@ItemState}")
                            EmitChange_ui(null)
                        }
                    }
                }
            }
        }

        inner class ListAdapter(
            params: ComposableAdapterParameters<Int>,
        ) : ComposableAdapter<Int, ItemState>(params) {
            @Composable
            override fun __Content__(list: Result<List<ItemState>>) {
                LazyColumn {
                    if (list.isSuccess) {
                        items(
                            list.value.size,
                            key = { list.value[it].id },
                            contentType = { getItemViewType(list.value[it]) }
                        ) {
                            list.value[it].ListContent()
                            DisposableEffect(true) {
                                onDispose {
                                    EmitChange_ui(mutableSetOf(lastDisposedBinding.SetResult(it)))
                                }
                            }
                        }
                    }
                }
            }

            override fun map(data: Int): ItemState {
                return ItemState(DataNodeParameters.State(lifecycleOwner), data)
            }
        }

        private val srcList: Binding<List<Int>> = bindTask(
            TagKey(R.id.tagKey_CardListActivityItemList, R.id.tagInitKey_CardListActivityItemList),
            buildSRCListTask()
        )

        private val lastDisposedBinding: Binding<Int> = emptyBinding(
            TagKey(
                R.id.tagKey_CardListActivityLastDisposed,
                R.id.tagInitKey_CardListActivityLastDisposed
            ),
        )

        override fun __Bind___(changedBindingKeys: MutableSet<Int>?) {
            srcList.Bind(srcList.BindParameters(changedBindingKeys))
            lastDisposedBinding.Bind(lastDisposedBinding.BindParameters(changedBindingKeys))
        }

        @Composable
        override fun __Content__() {
            Column {
                val src = srcList.state
                val adapter = remember {
                    ListAdapter(
                        ComposableAdapter.ComposableAdapterParameters(
                            DataSourceParameters.State(lifecycleOwner),
                            src
                        )
                    )
                }
                val lastDisposed by lastDisposedBinding.state
                Text(text = "last disposed: ${lastDisposed[-1]}", fontSize = 40.sp)
                adapter.Content()
            }
        }
    }

    private fun composeUI() {
//        val list = mutableStateOf(List(999) { it })
//        var lastDisposed by mutableIntStateOf(-1)
        setContent {
            val state = remember {
                State(DataNode.DataNodeParameters.State(WeakReference(this))).apply {
                    EmitChange_ui(
                        null
                    )
                }
            }
//            Column {
//                Text(text = "last disposed: $lastDisposed", fontSize = 40.sp)
//                LazyColumn {
//                    items(
//                        list.value.size,
//                        key = { list.value[it] },
//                        contentType = { 0 }
//                    ) {
//                        Text(text = "hello ${list.value[it]}", fontSize = 20.sp)
//                        DisposableEffect(true) {
//                            onDispose {
//                                lastDisposed = it
//                            }
//                        }
//                    }
//                }
//            }
            state.Content()
        }
    }

    private fun composeUI2() {
        State(DataNode.DataNodeParameters.State(WeakReference(this))).apply {
            setContentView(
                createViewHolder(
                    Inflater(layoutInflater, null),
                    this@CardListActivity
                ).itemView
            )
            EmitChange_ui(null)
        }
    }

    private fun xmlUI() {
        val holder = JavaState.UI(Inflater(layoutInflater, null))
        setContentView(holder.itemView)
        JavaState(
            DataNode.DataNodeParameters(WeakReference(this), holder)
        ).EmitChange_ui(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        composeUI()
        xmlUI()
//        composeUI2()
    }
}