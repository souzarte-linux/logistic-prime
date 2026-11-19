# Relatório de Entrega — FASE 0.1: Correções da Fundação (Hilt/Room/Sync)

## 1. Itens Realizados

### ITEM 1 — Banco por Usuário, Resolvido a Cada Uso
- Criado `data/local/LocalDatabaseProvider.kt` com interface `LocalDatabaseProvider` e implementação `GuardedDatabaseProvider` injetada via Hilt.
- `UserDatabaseGuard`: removido qualquer fallback para "anonymous". Lança `IllegalStateException("Sem sessão ativa")` quando o `userId` for nulo ou em branco. Ao trocar de usuário, fecha o banco anterior via `close()` sem apagar o arquivo.
- `DatabaseModule`: removidos os providers singleton obsoletos `provideCostCenterDatabase`, `provideOutboxDao` e `provideSyncCursorDao`. Adicionado `@Provides @Singleton provideLocalDatabaseProvider`.
- `SyncEngine`: agora recebe `(provider: LocalDatabaseProvider, registry: SyncHandlerRegistry)`. Resolve o banco via `val db = provider.current()` uma vez no início de `processOutbox()` e `pullChanges()`. Nenhuma propriedade da classe armazena DAOs.
- Adicionado `sweepOtherUsers(currentUserId: String)` no `UserDatabaseGuard`: varre arquivos em `context.databaseList()`, inspeciona o outbox via `OPEN_READONLY`; apaga bancos de outros usuários apenas se outbox estiver vazio (0 registros). Preserva bancos com registros pendentes (> 0) e registra aviso via `Log.w` sem expor payloads.
- Em `LogisticPrimeApplication`: injetados `UserSession` e `UserDatabaseGuard`. Em `onCreate()`, coleta de `userIdFlow` em escopo coroutine que dispara `sweepOtherUsers` quando autenticado. Logout não apaga dados.

### ITEM 2 — Tratamento de Erros e 409
- Criado `sync/PostgrestErrorClassifier.kt` com constantes `UNIQUE_VIOLATION` ("23505"), `FOREIGN_KEY_VIOLATION` ("23503") e método `isIdempotentDuplicate`.
- Em `SyncResults.kt`: substituído `PushResult.Conflict409` por `data object AlreadyApplied : PushResult()`.
- Em `SyncEngine`: removida função `isConflictException`. Tanto `Success` quanto `AlreadyApplied` deletam o item do outbox. Qualquer exceção lançada pelo handler é convertida em `PushResult.Failure(t)` sem inspeção de texto. Violação 23503 nunca é sucesso e mantém o item no outbox.

### ITEM 3 — FIFO Estrito
- Em `processOutbox`: ao iterar pelos itens pendentes ordenados por `created_at ASC`, qualquer ocorrência de:
  1. Item em backoff;
  2. Item sem handler registrado;
  3. Falha transitória (`PushResult.Failure`) com tentativas < 8;
  interrompe imediatamente o laço (`break`), garantindo que itens posteriores não sejam processados fora de ordem.
- Itens que atingem 8 tentativas viram falhas permanentes (descartados da fila pendente) e a iteração prossegue.

### ITEM 4 — Backoff a Partir da Última Tentativa (Room v2)
- Adicionado campo `lastAttemptAt: Long? = null` em `OutboxEntity` e `OutboxItem`, mapeado para coluna `last_attempt_at INTEGER`.
- `CostCenterDatabase` incrementado para versão 2. Criada e registrada a migração `MIGRATION_1_2` com `ALTER TABLE outbox ADD COLUMN last_attempt_at INTEGER`. Esquema exportado e versionado em `app/schemas/.../2.json`.
- `OutboxDao.updateAttempts` atualizado para gravar `attempts`, `last_error` e `last_attempt_at`.
- `SyncEngine.isBackoffElapsed` calcula o intervalo de espera a partir de `(item.lastAttemptAt ?: item.createdAt)`.

### ITEM 5 — Agendamento Periódico
- Em `LogisticPrimeApplication.onCreate()`: injetado `SyncScheduler` e chamada a rotina `schedulePeriodicSync()`.

### ITEM 6 — SyncWorker
- Em `SyncWorker.doWork()`: captura e relança `CancellationException`.
- Ao concluir `syncEngine.syncAll()`: se `push.errorCount > 0 || pull.failedTablesCount > 0`, retorna `Result.retry()` para `runAttemptCount < 3`, senão `Result.success()`.
- Para outras exceções: `Result.retry()` para `runAttemptCount < 3`, senão `Result.failure()`.

### ITEM 7 — Suíte de Testes
- Atualizados `SyncEngineTest`, `SyncWorkerTest` e `UserDatabaseGuardTest`.
- Teste `resposta409ViaExcecaoTambemContaComoSucesso` reescrito para comprovar que exceção com "409" conta como falha e preserva o item no outbox.
- Novos testes implementados e verificados:
  - (a) `alreadyAppliedApagaOItem`: `PushResult.AlreadyApplied` remove o item do outbox.
  - (b) `PostgrestErrorClassifierTest`: valida "23505" como duplicidade idempotente, "23503" e nulo como não-duplicidade.
  - (c) `failureDoItem1ImpedeOEnvioDoItem2NaMesmaExecucao`: falha do primeiro item impede envio do segundo na mesma execução.
  - (d) `itemEmBackoffImpedeOsPosteriores`: item em espera de backoff bloqueia itens seguintes.
  - (e) `backoffContaAPartirDeLastAttemptAt`: item antigo com tentativa recente não é reenviado antes do tempo de backoff.
  - (f) `sweepOtherUsersApagaBancoComOutboxVazioEPreservaComItemPendente`: apaga banco vazio de outro usuário e preserva o que possui 1 item pendente.
  - (g) `getOrCreateDatabaseSemUsuarioLancaIllegalStateException`: lança `IllegalStateException` na ausência de sessão.
  - (h) `trocarDeUsuarioEChamarProviderCurrentDevolveBancoDoNovoUsuario`: alternar usuário retorna o banco correspondente ao novo usuário e fecha o anterior.
  - (i) `migration1Para2PreservaLinhaPreExistenteEAdicionaColunaLastAttemptAt`: migração 1→2 via `MigrationTestHelper` validando integridade de dados pré-existentes.
  - (j) `syncWorkerComFalhasDePushRetornaRetryNa1aTentativaESuccessNa3a`: valida comportamento de retentativas do `SyncWorker`.

---

## 2. Arquivos Modificados e Criados

### Código de Produção
- `app/src/main/java/com/fernando/centraldomotorista/data/local/LocalDatabaseProvider.kt` (Criado)
- `app/src/main/java/com/fernando/centraldomotorista/data/local/CostCenterDatabase.kt` (Modificado — v2 + Migration 1->2)
- `app/src/main/java/com/fernando/centraldomotorista/data/local/UserDatabaseGuard.kt` (Modificado — sem anonymous, sweep, close sem delete na troca)
- `app/src/main/java/com/fernando/centraldomotorista/data/local/dao/OutboxDao.kt` (Modificado — updateAttempts com last_attempt_at)
- `app/src/main/java/com/fernando/centraldomotorista/data/local/entity/OutboxEntity.kt` (Modificado — campo last_attempt_at)
- `app/src/main/java/com/fernando/centraldomotorista/di/DatabaseModule.kt` (Modificado — LocalDatabaseProvider)
- `app/src/main/java/com/fernando/centraldomotorista/sync/PostgrestErrorClassifier.kt` (Criado)
- `app/src/main/java/com/fernando/centraldomotorista/sync/OutboxItem.kt` (Modificado — campo lastAttemptAt)
- `app/src/main/java/com/fernando/centraldomotorista/sync/SyncResults.kt` (Modificado — PushResult.AlreadyApplied)
- `app/src/main/java/com/fernando/centraldomotorista/sync/SyncEngine.kt` (Modificado — LocalDatabaseProvider, FIFO estrito, backoff relativo)
- `app/src/main/java/com/fernando/centraldomotorista/sync/SyncWorker.kt` (Modificado — doWork com retry limite e CancellationException)
- `app/src/main/java/com/fernando/centraldomotorista/LogisticPrimeApplication.kt` (Modificado — sweep e scheduler)
- `app/schemas/com.fernando.centraldomotorista.data.local.CostCenterDatabase/2.json` (Criado — exportSchema Room)

### Testes
- `app/src/test/java/com/fernando/centraldomotorista/sync/PostgrestErrorClassifierTest.kt` (Criado)
- `app/src/test/java/com/fernando/centraldomotorista/sync/SyncEngineTest.kt` (Modificado)
- `app/src/test/java/com/fernando/centraldomotorista/sync/SyncWorkerTest.kt` (Modificado)
- `app/src/test/java/com/fernando/centraldomotorista/sync/SyncSchedulerTest.kt` (Modificado)
- `app/src/test/java/com/fernando/centraldomotorista/data/local/CostCenterDatabaseMigrationTest.kt` (Modificado)
- `app/src/test/java/com/fernando/centraldomotorista/data/local/UserDatabaseGuardTest.kt` (Modificado)

---

## 3. Evidências de Testes

### Execução de Testes Unitários (`testDebugUnitTest` e `testReleaseUnitTest`)
- Comando: `./gradlew testDebugUnitTest`
  - Total executados: 324 testes
  - Aprovados: 324 (100%)
  - Falhas: 0
  - Ignorados: 0
  - Status: BUILD SUCCESSFUL (32s)
- Comando: `./gradlew testReleaseUnitTest assembleDebug`
  - Total executados: 324 testes
  - Aprovados: 324 (100%)
  - Falhas: 0
  - Ignorados: 0
  - Status: BUILD SUCCESSFUL (4m 3s)

---

## 4. Pendências

- Nenhuma pendência identificada no escopo dos 7 itens da Fase 0.1.
