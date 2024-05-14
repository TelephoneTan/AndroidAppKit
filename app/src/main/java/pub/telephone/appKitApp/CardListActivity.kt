package pub.telephone.appKitApp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import pub.telephone.appKit.dataSource.ComposableAdapter
import pub.telephone.appKit.dataSource.ComposableNode
import pub.telephone.appKit.dataSource.DataNode
import pub.telephone.appKit.dataSource.DataSource
import pub.telephone.appKit.dataSource.TagKey
import pub.telephone.appKit.dataSource.promised
import pub.telephone.javahttprequest.network.http.HTTPMethod
import pub.telephone.javahttprequest.network.http.http
import pub.telephone.javapromise.async.kpromise.toKPromise
import java.lang.ref.WeakReference

class CardListActivity : AppCompatActivity() {
    class State(params: DataNodeParameters.State) : ComposableNode(params) {
        class ItemState(
            params: DataNodeParameters.State,
            val id: Int
        ) : ComposableNode(params) {
            private val response: Binding<String> = bindTask(
                TagKey(R.id.tagKey_CardListActivityItem, R.id.tagInitKey_CardListActivityItem),
                RetrySharedTask.Simple {
                    promised {
                        rsp(
                            http {
                                Method = HTTPMethod.GET
                                URL = "https://guetcob.com/question"
                            }
                                .JSONArray().toKPromise()
                                .then { rsv(value.Result.getJSONArray(2).getString(1)) }
                        )
                    }.promise.toJavaPromise()
                }
            )

            override fun __Bind__(changedBindingKeys: MutableSet<Int>?) {
                response.Bind(response.BindParameters(changedBindingKeys))
            }

            @Composable
            override fun __Content__() {
                val resp by response.state
                Text(text = resp["hello $id"], fontSize = 20.sp)
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
            RetrySharedTask.Simple {
                promised { rsv(List(999) { it }) }.promise.toJavaPromise()
            }
        )

        private val lastDisposedBinding: Binding<Int> = emptyBinding(
            TagKey(
                R.id.tagKey_CardListActivityLastDisposed,
                R.id.tagInitKey_CardListActivityLastDisposed
            ),
        )

        override fun __Bind__(changedBindingKeys: MutableSet<Int>?) {
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
                            DataSource.DataSourceParameters.State(lifecycleOwner),
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = State(DataNode.DataNodeParameters.State(WeakReference(this)))
//        val list = mutableStateOf(List(999) { it })
//        var lastDisposed by mutableIntStateOf(-1)
        setContent {
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
        state.EmitChange_ui(null)
    }
}