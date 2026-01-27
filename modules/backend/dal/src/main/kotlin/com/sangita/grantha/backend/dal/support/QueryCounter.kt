package com.sangita.grantha.backend.dal.support

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext

object QueryCounter {
    private val counterThreadLocal = ThreadLocal<AtomicInteger?>()

    fun contextElement(counter: AtomicInteger = AtomicInteger(0)): ThreadContextElement<AtomicInteger?> =
        counterThreadLocal.asContextElement(counter)

    fun isActive(): Boolean = counterThreadLocal.get() != null

    fun increment() {
        counterThreadLocal.get()?.incrementAndGet()
    }
}

object QueryCounterLogger : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        QueryCounter.increment()
    }
}
