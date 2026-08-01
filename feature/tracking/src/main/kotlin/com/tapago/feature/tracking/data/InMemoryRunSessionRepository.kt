package com.tapago.feature.tracking.data

import com.tapago.feature.tracking.domain.RunSession
import com.tapago.feature.tracking.domain.RunSessionRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO: substituir por implementação com Room quando o histórico de
 * corridas (fora do escopo deste incremento) for implementado.
 */
@Singleton
class InMemoryRunSessionRepository @Inject constructor() : RunSessionRepository {
    private val sessions = ConcurrentHashMap<String, RunSession>()

    override fun save(session: RunSession) {
        sessions[session.id] = session
    }

    override fun getById(id: String): RunSession? = sessions[id]
}
