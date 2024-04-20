package pub.telephone.appKit.dataSource

import pub.telephone.javapromise.async.kpromise.ProcessFunc
import pub.telephone.javapromise.async.kpromise.PromiseCancelledBroadcast
import pub.telephone.javapromise.async.kpromise.PromiseJob
import pub.telephone.javapromise.async.kpromise.PromiseScope
import pub.telephone.javapromise.async.kpromise.WorkFunc
import pub.telephone.javapromise.async.kpromise.process
import pub.telephone.javapromise.async.kpromise.toProcessFunc

fun <RESULT> DataNode<*>.processed(builder: ProcessFunc<RESULT>) = object : PromiseScope {
    override val scopeCancelledBroadcast: PromiseCancelledBroadcast?
        get() = null
}.process(builder)

fun DataNode<*>.worked(builder: WorkFunc) = process(builder.toProcessFunc())
fun <RESULT> DataNode<*>.promised(job: PromiseJob<RESULT>) = process { promise { job() } }