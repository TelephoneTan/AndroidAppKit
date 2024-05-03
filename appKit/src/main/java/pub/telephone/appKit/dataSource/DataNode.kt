package pub.telephone.appKit.dataSource

import pub.telephone.javapromise.async.kpromise.ProcessFunc
import pub.telephone.javapromise.async.kpromise.PromiseJob
import pub.telephone.javapromise.async.kpromise.WorkFunc
import pub.telephone.javapromise.async.kpromise.process
import pub.telephone.javapromise.async.kpromise.toProcessFunc

fun <RESULT> DataNode<*>.processed(builder: ProcessFunc<RESULT>) = currentScope().process(builder)

fun DataNode<*>.worked(builder: WorkFunc) = processed(builder.toProcessFunc())
fun <RESULT> DataNode<*>.promised(job: PromiseJob<RESULT>) = processed { promise { job() } }

internal fun <T> mutableStateOf(value: T) = androidx.compose.runtime.mutableStateOf(value)