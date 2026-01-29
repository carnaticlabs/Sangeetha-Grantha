package com.sangita.grantha.backend.api.plugins

import com.sangita.grantha.backend.dal.support.QueryCounter
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.path
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.withContext
import java.util.UUID
import org.slf4j.event.Level

fun Application.configureRequestLogging() {
    install(CallId) {
        retrieveFromHeader("X-Request-ID")
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        val counter = AtomicInteger(0)
        withContext(QueryCounter.contextElement(counter)) {
            proceed()
        }
        if (!call.response.isCommitted) {
            call.response.headers.append("X-DB-Query-Count", counter.get().toString())
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            val path = call.request.path()
            path == "/health" || path.startsWith("/v1")
        }
        mdc("requestId") { call -> call.callId }
        mdc("userId") { call -> call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString() }
    }
}
