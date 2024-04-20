package pub.telephone.appKit.dataSource

import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

interface Config<T> {
    fun copy(): T
}

open class Selector<K, V, R>(
    private val fallback: K,
    private val m: Map<K, Any?>,
    private val t: (V) -> R
) : Config<Selector<K, V, R>> {
    private fun select(k: K): R {
        var key = k
        if (!m.containsKey(key)) {
            key = fallback
        }
        return when (val res = m[key]) {
            is Selector<*, *, *> -> @Suppress("UNCHECKED_CAST") (res as Selector<K, *, R>).select(k)
            else -> @Suppress("UNCHECKED_CAST") t(res as V)
        }
    }

    companion object {
        internal fun <R> transform(c: Config<*>, k: Any?): R {
            @Suppress("UNCHECKED_CAST")
            return when (c) {
                is Selector<*, *, *> ->
                    @Suppress("UNCHECKED_CAST")
                    (c as Selector<Any?, *, Any?>).select(k)

                else -> c.apply configApply@{
                    this@configApply::class.declaredMemberProperties.forEach pLoop@{ p ->
                        if (
                            p !is KMutableProperty<*> ||
                            p.visibility !in setOf(
                                KVisibility.PUBLIC,
                                KVisibility.INTERNAL
                            )
                        ) {
                            return@pLoop
                        }
                        p.setter.call(
                            this@configApply,
                            transform(p.getter.call(this@configApply).let {
                                if (it !is Config<*>) {
                                    return@pLoop
                                }
                                @Suppress("UNCHECKED_CAST")
                                (it as Config<Config<*>>).copy()
                            }, k)
                        )
                    }
                }
            } as R
        }
    }

    override fun copy(): Selector<K, V, R> {
        return this
    }
}