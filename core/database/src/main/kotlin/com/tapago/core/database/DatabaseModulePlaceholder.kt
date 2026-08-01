package com.tapago.core.database

/**
 * Módulo reservado para o banco de dados Room (histórico de corridas,
 * cache de assinatura, etc.). Ainda não implementado neste incremento —
 * o `feature:tracking` usa uma implementação em memória
 * (`InMemoryRunSessionRepository`) como placeholder.
 *
 * Próximo passo: criar `TaPagoDatabase`, `RunSessionEntity`,
 * `RunSessionDao` e migrar `InMemoryRunSessionRepository` para uma
 * implementação com Room, com testes via `MigrationTestHelper`.
 */
internal object DatabaseModulePlaceholder
