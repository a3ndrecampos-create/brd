package com.tapago.feature.tracking.domain

/**
 * Contrato para persistir e consultar sessões finalizadas.
 * A implementação inicial é em memória (ver `data/InMemoryRunSessionRepository`);
 * será substituída por uma implementação com Room (seção 2.5 da especificação)
 * quando o histórico de corridas for implementado.
 */
interface RunSessionRepository {
    fun save(session: RunSession)
    fun getById(id: String): RunSession?
}
