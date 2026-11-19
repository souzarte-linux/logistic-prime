# Plano de Implementação — Centro de Custos (custos fixos, reserva e caixinhas) · versão final (v3)

**Projeto:** logistic-prime (`com.fernando.centraldomotorista`) · **Base analisada:** commit `3d901f1` · **Data:** 16/11/2026
**Executor:** Antigravity · **Método:** 12 fases sequenciais (F0 a F10, incluindo a F6B), 1 prompt por fase (F9 é opcional)

> Análise estática do código. Não compilei o app nem rodei os testes, e não tive acesso ao schema vivo do Supabase — por isso os Prompts F0 e F1 mandam medir a linha de base e inspecionar o banco real antes de escrever qualquer código ou DDL.

**O que esta versão final contém**
1. **Valores dos custos fixos são do usuário.** Nenhum valor monetário existe no código. Há tela de cadastro, lista pré-cadastrada (só nome/categoria/periodicidade) e todo item pode ser excluído.
2. **Room, Hilt e WorkManager** fazem parte do plano (Fase F0), pois o projeto não tinha nenhum dos três.
3. **Segundo plano comparado e integrado** (seção 3): razão imutável de caixinhas, estornos, idempotência, modo "dias trabalhados", "km não informado", branch + PR por fase e matriz de testes.
4. **Pagamento dividido:** o pagamento sai da caixinha (até o saldo) e o restante de uma conta bancária informada. **A caixinha nunca fica negativa** por pagamento; retirada e transferência manual também não passam do saldo.
5. **Aporte automático na baixa da fatura da plataforma** (tela Faturas / Contas a Receber), com prévia editável, divisão proporcional por plataforma e **desfazer baixa**. Nova tela de consulta **"Reserva a separar"** (Fase F6B). O valor da fatura nunca é alterado.
6. **Cartão de débito** gera **extrato mensal** (não fatura), com conta bancária obrigatória vinculada ao cartão.
7. **Modo "dias trabalhados"** começa com domingo a sábado.

---

## 1. Como usar

1. **Passo 0:** copie este arquivo para `docs/plano-centro-de-custos.md` dentro do repositório e faça commit. Todos os prompts mandam o Antigravity ler as seções 4 a 8 desse arquivo.
2. Execute os prompts na ordem **F0 → F1 → F2 → F3 → F4 → F5 → F6 → F6B → F7 → F8 → F9 → F10**, um por sessão. Só avance quando os critérios de aceite e os testes da fase passarem.
3. Cada prompt é um bloco de texto para colar inteiro no Antigravity.
4. Cada fase vira **uma branch e um PR próprios** (regra G12). Você revisa e faz o merge; o Antigravity nunca faz.
5. Revise o relatório de cada fase (arquivos alterados, decisões, saída dos testes) antes de liberar a próxima.

---

## 2. Diagnóstico do repositório (verificado no código)

| Tema | Situação atual | Consequência para o plano |
|---|---|---|
| Stack | Kotlin 2.1.0, AGP 8.8.2, Compose BOM 2024.12.01, Material 3, Navigation Compose 2.8.8, Retrofit 2.11 + Gson, supabase-kt 3.1.1 (só **Auth**), DataStore 1.1.2, coroutines 1.10.1, minSdk 26 / target 35 | Dados novos seguem o padrão Retrofit/PostgREST para falar com o Supabase |
| Dependências ausentes | **Não existe Room, Hilt, WorkManager nem KSP.** Não existe classe `Application` (o `<application>` do manifesto não tem `android:name`). Testes: só `libs.junit` (sem coroutines-test, Robolectric, Turbine) | **F0** instala tudo e cria a classe `Application`. O nome `CentralDoMotoristaApp` já é um composable: não reutilize |
| Padrão atual | Repositórios usam parâmetro default (`RetrofitClient.xApi`); ViewModels têm hooks de teste (`externalScope`, `loadOnInit`, `userIdProvider`) | Código legado **não é migrado**. Só o módulo novo usa Hilt/Room |
| Segurança | App fala direto com o PostgREST com chave anon → **o RLS é a única barreira** | F1 exige RLS em toda tabela nova e teste de isolamento |
| Histórico | `HistoricoRepository.loadHistoricoData` monta `TransactionItem` (GANHO/DESPESA). `HistoricoViewModel.calculatePeriodMetrics`: saldo = entradas − saídas, despesa contada na data da compra (inclusive cartão) | Saldo atual vira "regime de caixa" e fica intacto; nasce o "Lucro real" ao lado |
| Manutenção | `LancarManutencaoScreen` (FORMA DE PAGAMENTO, ~linhas 653–800: PIX / Cartão / Dinheiro). `PartMaintenanceViewModel.savePartMaintenance` cria `Expense` (`manutencao`) e depois `PartMaintenance` — 2 requests não atômicos. `ExpenseDto.id` é `String?` e `toDto()` já envia o `id` quando preenchido | Dá para **gerar o UUID da despesa no cliente** e montar uma saga idempotente (F4) sem alterar `expenses` |
| Exclusão de despesa | `ExpenseRepository.deleteExpense` (linha ~31) é o ponto central; chamado em `PartMaintenanceViewModel` (~635, ~729), `PartnerRoutesViewModel` (~311) e `HistoricoRepository` (~245) | Um único gancho em `deleteExpense` cobre todos os caminhos (F2) |
| Faturas de plataformas (baixa) | `FaturasViewModel.confirmPayment()` (botão "Liquidar / Baixar Repasse") marca o ciclo como `pago` e grava `payment_received_date` via `BillingCycleRepository.updateStatus`. `BillingCycle` tem `periodStart`, `periodEnd` e `netTotalAmount` (só descontos e créditos das próprias plataformas). **Não existe "desfazer baixa"**; `updateStatus` usa `Map<String, Any>` e não consegue gravar `null` | Ponto único para o aporte automático (F6B). O desfazer baixa será implementado na F6B, com corpo JSON de `null` explícito |
| Cartões | `CreditCard` tem `closingDay`/`dueDay`. `CardPaymentModal` define a 1ª parcela por `purchaseDay < closingDay`. **Parcelado grava UMA despesa** (`installmentTotal`, `installmentGroupId`). `FaturasScreen` é "Contas a Receber" de plataformas, **não** fatura de cartão | Fatura de cartão não existe: é derivada das despesas |
| Desgaste de peças | `PartMaintenance.usagePercentage`, alertas ≥90% no `PainelViewModel`. Odômetro atual = maior entre último odômetro de despesa e último `endKm` de rota. Peça é atualizada **no mesmo registro** a cada troca | Custo por km = despesa vinculada ÷ `lifeKm` |
| Km do dia | `RelatoriosViewModel` (~linha 271): `totalKm = routeKm + dailyKm` | Reutilizar a mesma regra |
| Notificações locais | Inexistentes (a tabela `notifications` é só de recebíveis) | F8 cria canal, permissão e worker |
| Banco | Tabelas antigas `user_id TEXT`; novas `UUID` + RLS `auth.uid() = user_id`. Migrações em `docs/backend/*.sql` | Tabelas novas em UUID; junções por `id`, nunca por `user_id` entre tabelas novas e antigas |
| Gson | Omite `null` por padrão | PATCH não limpa coluna; o outbox (F0/F2) serializa com `JsonObject` e `null` explícito |
| Testes/CI | JUnit4 em `app/src/test`; CI roda `testDebugUnitTest testReleaseUnitTest`; workflow `.agents/workflows/commit.md` (manda sincronizar com o remoto) | Cada fase termina com testes verdes. O workflow de commit conflita com "PR sem merge": ver G12 |

---

## 3. Comparação com o segundo plano

Avaliei o segundo plano (`plano-logistic-prime-centro-custos-prompts`) ponto a ponto. Ele é forte em **arquitetura financeira e rigor transacional**; o meu era mais forte em **detalhe executável** (arquivos, linhas, fórmulas com números, testes com valores).

| Item do segundo plano | Decisão | Onde entra / por quê |
|---|---|---|
| Fase 0 de diagnóstico/baseline (somente leitura) | **Integrado** | Primeiro passo da F0: registrar commit, testes e achados em `docs/financeiro/00-diagnostico-baseline.md` |
| Razão imutável `vault_ledger` com `operation_id` e estornos | **Integrado (substitui minha view derivada)** | Meu saldo derivado quebrava o offline e deixava edições mexerem no passado. Agora: razão só de inserção, `UNIQUE (operation_id, leg)`, estorno por linha nova (F1/F2) |
| `expense_settlements` (liquidação de despesa) | **Integrado** | Tabela que guarda conta/caixinha de origem do PIX. **Substitui o `ALTER TABLE expenses` da v1**: a tabela `expenses` não é mais tocada |
| Operação atômica no servidor (RPC) para manutenção | **Adaptado** | RPC exigiria duplicar no SQL todo o payload legado de `expenses`/`part_maintenance`. Uso **saga idempotente**: UUID da despesa gerado no cliente, razão local primeiro, conciliador compensa falhas (F4) |
| Room + outbox, sem simular offline | **Integrado** | Você pediu Room/Hilt/WorkManager. Módulo novo é Room-first com outbox e sync (F0/F2) |
| Custos com vigência/versionamento | **Integrado** | `fixed_cost_versions`: mudar o valor cria nova versão e **não reescreve o passado** (F1/F5). Também sustenta o requisito de valores digitados pelo usuário |
| Modo "dias trabalhados" para sugerir reserva | **Integrado** | Configuração do usuário; Lucro real continua em dias corridos. Divisor zero → sem sugestão, mostra pendência (R5/F6) |
| "Km não informado" em vez de custo zero | **Integrado** | Dia sem km mostra aviso e não provisiona desgaste (R2/F6) |
| Resto de centavos | **Integrado** | Último dia do ciclo absorve o resíduo; totais de período arredondados uma única vez (R1) |
| Estorno ao editar/excluir despesa PIX e ao trocar PIX→Cartão | **Integrado** | Delta ou estorno na razão (R9/F4); gancho em `deleteExpense` (F2) |
| FK composta `(id, user_id)`, `search_path` seguro, sem `service_role` | **Integrado** | F1 e regra G9 |
| Política de lembretes 7/3/1/0 dias | **Integrado como configuração** | Padrão: fechamento + 3 dias + vencimento; usuário pode ligar 7 e 1 (F8) |
| Alerta dentro do app quando falta permissão de notificação | **Integrado** | Banner/lista de faturas a vencer na aba Cartões (F8) |
| Itens extras do catálogo: depreciação (analítica) e reserva de imprevistos | **Integrado** | `depreciacao_veiculo` (não entra no Lucro real) e `reserva_imprevistos` (seção 7) |
| Duas métricas rotuladas (resultado estimado × fluxo de caixa) + memória de cálculo | **Integrado em parte** | Saldo (caixa) e Lucro real (estimado) lado a lado, com sheet "?" explicando o cálculo (F6) |
| Branch por fase, PR sem merge, `main` intocada | **Integrado** | Regra G12 (substitui o "sync" do workflow `/commit`) |
| Matriz de testes de aceite | **Integrado** | Casos do segundo plano que faltavam foram incorporados nas fases e consolidados na F10 |
| Recomendação de reserva separada da transferência confirmada | **Integrado** | O saldo da caixinha só sobe por aporte confirmado: manual ou **automático na baixa da fatura** (D22, F6B). Nunca por provisão |
| Tabelas genéricas `cost_centers`, `cost_rules`, `cost_accruals`, `financial_operations` | **Não integrado** | Excesso para o app: gravar rateio diário por regra explode o volume e cria divergência. Rateio é **calculado** (função pura testada), não armazenado |
| `card_installments` e `card_statements` materializados | **Não integrado** | Fatura derivada das despesas não diverge e muda sozinha se a despesa for editada. Materializar só se o desempenho exigir |
| Financiamento com juros separados do principal | **Fora do escopo agora** | Exige que o usuário informe a divisão juros/amortização. Cadastra-se a parcela inteira como reserva de caixa. Entra na seção 12 |
| Provisão corretiva por km/mês/% e custo de oportunidade | **Não integrado** | Provisão corretiva = cadastrar `reserva_imprevistos` como custo fixo. Custo de oportunidade é métrica analítica sem pagamento |
| Pagamento dividido em várias origens | **Integrado (versão reduzida)** | Pedido seu: o pagamento sai da caixinha (até o saldo) + conta bancária para o restante. A caixinha nunca fica negativa (D8, R13, F4) |
| Prompts que invocam "Orquestrador/subagentes" | **Descartado** | Conflita com a regra G2 (execução direta), pois a regra *always-on* do repositório tenta forçar saída em JSON |
| Peça vencida continua provisionando | **Não integrado** | Mantive o teto no valor da despesa e um aviso "peça vencida — atualize" (R2) |

O que o segundo plano **não tinha** e o meu mantém: regras com números conferíveis, mapa de arquivos e linhas do código, tratamento do Gson/PATCH, fatura derivada com regra de fechamento, exemplo de conferência e testes com valores exatos.

---

## 4. Decisões de design

Itens marcados **(confirme)** são suposições minhas. O resto vem do seu pedido.

| # | Decisão |
|---|---|
| D1 | **(confirme)** O **Saldo** atual do Histórico (regime de caixa) não muda. Nasce o **Lucro real (estimado)** = ganho líquido − custos variáveis realizados − custos alocados (fixos rateados + desgaste de peças). |
| D2 | **(confirme)** Custo fixo é rateado **por dia corrido**: valor do ciclo ÷ dias do ciclo. Ciclos de calendário ancorados no mês de `start_date`. |
| D3 | **(confirme)** Desgaste: custo/km = valor da despesa vinculada à peça ÷ `lifeKm`; só conta km **após** `lastChangeAt`; o acumulado nunca passa do valor da despesa. Despesas de categoria `manutencao` **saem** do custo realizado do Lucro real (senão contariam duas vezes), mas continuam no Saldo de caixa. |
| D4 | Pagamento de custo fixo e de fatura de cartão são **liquidações**: debitam a caixinha na razão e **não** aparecem como despesa no Histórico. |
| D5 | Compra no cartão continua sendo despesa na data da compra. A **fatura é derivada** das despesas (parcelas calculadas, sem criar linhas novas). |
| D6 | O saldo da caixinha = saldo inicial + soma da razão `vault_ledger` (linhas **imutáveis**). Não existe coluna de saldo mutável. Correções são linhas novas (estorno/ajuste). |
| D7 | A conta principal não é gerenciada: o app **não mantém saldo dela**. Quando parte do pagamento não sai de caixinha, o usuário informa **de qual conta bancária saiu** e o app registra só a origem. |
| D8 | **Pagamento dividido, caixinha nunca negativa.** Todo pagamento (PIX da Manutenção, custo fixo, fatura, compra no débito) usa até duas origens: a **caixinha**, pelo valor escolhido (começa com todo o saldo disponível, editável, nunca acima do saldo) e a **conta bancária** para o restante. Saldo insuficiente abre o pop-up: "Não, voltar" retorna para trocar conta/caixinha; "Sim, continuar" abre a escolha da conta de origem do restante. |
| D9 | **(confirme)** Sem nenhuma conta cadastrada, o PIX segue como hoje ("Continuar sem vincular"). Com 1+ conta ativa, a origem é obrigatória: caixinha, conta, ou as duas. |
| D10 | A reserva é **sugestão** até a baixa da fatura da plataforma. O saldo da caixinha só sobe por aporte confirmado: aporte manual ou **aporte automático na baixa** (D22). |
| D11 | Cartão `credito`/`multiplo` gera **fatura** (fecha, vence, paga, lembra). Cartão `debito` gera **extrato mensal** (sem fechamento, vencimento, pagamento nem lembrete) e exige **conta bancária vinculada** ao cartão; o app pergunta sempre que o vínculo não existe. `voucher` não gera nenhum dos dois. |
| D12 | **(confirme)** Lembretes de fatura por WorkManager diário: no fechamento e nos dias configurados antes do vencimento (padrão 3 e 0). Sem alarme exato. |
| D13 | **Room-first com outbox** no módulo novo: toda escrita grava no Room e entra na fila de sincronização; o app funciona offline para caixinhas e custos. O legado (despesas, rotas, peças) continua online como hoje. |
| D14 | PIX com conta/caixinha entra em **Manutenção** agora. Combustível/Alimentação ficam na F9 (recomendada). |
| D15 | A reserva só é provisionada até **hoje**. Dias futuros do período não entram. |
| D16 | **Valores de custos fixos são sempre digitados pelo usuário.** O código e o catálogo (JSON) não contêm nenhum valor monetário. Item sem valor fica "VALOR PENDENTE" e não entra em nenhum cálculo. |
| D17 | **Lista pré-cadastrada, 100% removível.** Na primeira abertura, o app cria alguns itens essenciais (sem valor): seguro do veículo, rastreador, IPVA, licenciamento, garagem, internet e celular/dados **(confirme a lista)**. O usuário informa o valor, ou exclui o que não tem. Item excluído **não volta** sozinho. O catálogo completo continua disponível em "Adicionar do catálogo". |
| D18 | Mudar o valor de um custo cria **nova versão com data de vigência**; o passado não é reescrito. Existe também "Corrigir valor" (erro de digitação) que edita a versão atual. |
| D19 | **(confirme)** Dados do módulo novo são isolados por usuário no aparelho: ao trocar de conta, o banco Room local é limpo e recarregado do Supabase. |
| D20 | Reserva sugerida tem dois modos: **dias corridos** (padrão) e **dias trabalhados** (usuário marca os dias da semana e as folgas; **começa com domingo a sábado**). O Lucro real usa sempre dias corridos. |
| D21 | **Retirada e transferência manual nunca passam do saldo** da caixinha: mensagem "O valor máximo para retirada é o saldo existente (R$ X)". "Ajustar saldo" não aceita saldo negativo. Única exceção: o estorno de um aporte (D23) pode deixar saldo negativo, com aviso. |
| D22 | **Aporte automático na baixa da fatura da plataforma** (Faturas / Contas a Receber, "Liquidar / Baixar Repasse"): o app registra os aportes nas caixinhas, com **prévia editável** no modal de baixa, divisão **proporcional por plataforma** (R12) e teto no líquido recebido. O valor da fatura **nunca** é alterado por custo fixo ou reserva: só descontos e créditos das próprias plataformas. |
| D23 | **Desfazer baixa** (aba "Pago" de Faturas): a fatura volta para "A Vencer", a data de recebimento é apagada e os aportes automáticos daquela baixa são estornados. |
| D24 | **Tela "Reserva a separar"** (drawer, Financeiro & Operacional), somente consulta, no padrão do Histórico: linha do tempo, mesmos filtros, subtotais por dia/semana/mês. Estados: Previsto, Aportado, Pendente. Sem botão manual de transferência. |
| D25 | O app registra o aporte, mas **não move dinheiro no banco**: após a baixa ele lembra o usuário de transferir na conta real. Divergência se corrige com "Ajustar saldo". |
| D26 | Só valem baixas e dias **a partir da data de ativação** do Centro de Custos (`cost_center_started_on`); histórico antigo não gera aporte. |

---

## 5. Modelo de dados

**Supabase (F1)** — todas com `id UUID PK`, `user_id UUID → auth.users ON DELETE CASCADE`, `created_at/updated_at`, RLS `auth.uid() = user_id`, FK composta `(id, user_id)` nas referências entre tabelas novas. **`expenses`, `credit_cards` e `part_maintenance` NÃO são alteradas.**

| Tabela | Papel | Colunas-chave |
|---|---|---|
| `bank_accounts` | Conta bancária (dados opcionais) | `name`, `institution`, `agency`, `account_last_digits` (só últimos dígitos), `account_type`, `informed_balance` + `informed_balance_at` (**informativo, não gerenciado**), `active`, `deleted_at` |
| `vaults` | Caixinha/cofre (único saldo gerenciado) | `account_id`, `name`, `purpose`, `goal_amount`, `initial_balance`, `tracking_start_date`, `active`, `deleted_at` |
| `vault_ledger` | **Razão imutável** de caixinhas | `vault_id`, `operation_id`, `leg`, `entry_type`, `amount` (**com sinal**), `occurred_at`, `ref_type`, `ref_id`, `contra_vault_id`, `reverses_entry_id`, `note`. `UNIQUE (operation_id, leg)`. Sem UPDATE/DELETE |
| `expense_settlements` | Origem de pagamento de uma despesa (PIX ou débito) | `expense_id` (único), `kind` (`pix`, `debito`), `account_id`, `vault_id`, `amount`, `vault_amount`, `account_amount` (soma = `amount`), `status` (`PENDENTE_DESPESA`, `CONFIRMADA`, `ABORTADA`), `operation_id` |
| `fixed_costs` | Custos fixos **sem valor** | `name`, `category`, `catalog_key`, `periodicity`, `due_day`, `due_month`, `start_date` (nulo até o usuário informar o valor), `end_date`, `vault_id`, `is_vehicle_related`, `analytical`, `seeded`, `notes`, `active`, `deleted_at` |
| `fixed_cost_versions` | **Valores digitados pelo usuário**, por vigência | `fixed_cost_id`, `amount > 0`, `effective_from`. `UNIQUE (fixed_cost_id, effective_from)` |
| `fixed_cost_payments` | Liquidação de custo fixo | `fixed_cost_id`, `competence_date`, `paid_at`, `amount`, `vault_amount`, `account_amount`, `source_account_id`, `source_vault_id`, `operation_id`. `UNIQUE (fixed_cost_id, competence_date)` |
| `card_invoice_payments` | Liquidação de fatura de cartão de crédito | `card_id` (TEXT/UUID conforme `credit_cards.id` real), `reference_month`, `amount`, `vault_amount`, `account_amount`, `paid_at`, `source_account_id`, `source_vault_id`, `operation_id` |
| `cost_center_settings` | 1 linha por usuário | `maintenance_vault_id`, `wear_provision_enabled`, `fixed_cost_provision_enabled`, `reserve_mode`, `planned_weekdays` (padrão 127 = domingo a sábado), `days_off`, `seeded_catalog_version`, `dismissed_seed_keys`, `reminder_enabled`, `reminder_offsets`, `cost_center_started_on` |
| `reserve_allocations` | Aporte automático de uma baixa de fatura, por caixinha | `billing_cycle_id`, `vault_id`, `suggested_amount`, `amount` (final, após edição), `net_cap_applied`, `status` (`ATIVA`, `DESFEITA`, `DISPENSADA`), `operation_id`, `undone_at` |
| `reserve_allocation_days` | Dias cobertos por cada alocação | `allocation_id`, `day`, `platform_id` (nulo = dia sem ganho), `suggested_amount` (escala 6) |
| `card_account_links` | Conta bancária de cada cartão de débito (sem alterar `credit_cards`) | `card_id` (único), `account_id` |
| `vault_balances` (view) | Saldo para **auditoria/conciliação** | inicial + Σ razão |

**Android (F0/F2)** — Room espelha todas as tabelas acima (com `sync_state`) e acrescenta tabelas só locais: `outbox`, `sync_cursor`, `notified_events` (F8).

**Asset do app (F5):** `app/src/main/assets/cost_center/fixed_cost_templates.json` — catálogo da seção 7 com `key`, nome, categoria, periodicidade sugerida, propósito de caixinha sugerido, dica, `preseed`, `analytical`. **Proibido qualquer campo de valor monetário.**

---

## 6. Regras de cálculo

- **R1 — Quota diária de custo fixo.** Só conta se o custo está `active`, não excluído, tem versão vigente e `start_date ≤ dia ≤ end_date`. O valor usado é o da **versão com maior `effective_from ≤ dia`**. Quota = `valor ÷ diasDoCiclo`. Semanal: ÷ 7. Mensal ou mais: ciclo de N meses de calendário ancorado no mês de `start_date`. Escala 6 internamente. O **último dia do ciclo absorve o resíduo**, de modo que a soma das quotas de um ciclo completo (sem troca de versão) é exatamente o valor. Totais de período são somados em escala 6 e **arredondados uma só vez** para 2 casas.
- **R2 — Provisão de desgaste.** Por peça: `custoPorKm = despesa vinculada ÷ lifeKm` (ignorar peça sem despesa de valor > 0 ou com `lifeKm ≤ 0`). Provisão do dia = `custoPorKm × kmDoDia`, só para dias ≥ data de `lastChangeAt`, com teto no valor da despesa. `kmDoDia` = soma de `distanceKm` das rotas + totais diários do dia local (regra do Relatórios). **Dia sem nenhum km informado** → provisão ZERO **e** o dia recebe a marca `kmNaoInformado = true` (a tela mostra "km não informado", nunca "R$ 0,00 calculado"). Peça que passou de `lifeKm` para de provisionar (teto) e exibe "peça vencida — atualize".
- **R3 — Custo variável realizado do dia.** Soma das despesas do dia **exceto** categoria `manutencao`.
- **R4 — Lucro real (estimado) do dia.** `ganho líquido do dia` (via `EarningsCalculator`) − R3 − quotas de custos fixos **não analíticos** (R1, sempre em dias corridos) − R2. Pode ser negativo. Custos `analytical` (ex.: depreciação) ficam **fora** do Lucro real e do aporte; aparecem só na memória de cálculo como informação.
- **R5 — Reserva sugerida.**
  - Modo `CALENDAR`: sugestão do dia = custos alocados do dia (R1 + R2).
  - Modo `WORKING_DAYS`: para cada custo fixo, `equivalente mensal do mês ÷ dias de trabalho planejados do mês`, aplicado só nos dias planejados (dias da semana marcados menos `days_off`). Equivalente mensal: mensal = valor; N meses = valor ÷ N; semanal = valor × 52 ÷ 12. **Zero dias planejados → nenhuma sugestão e aviso de pendência** (nunca dividir por zero). O desgaste (R2) continua por km nos dois modos.
  - Agrupamento por caixinha: custos com `vault_id = v`, mais o desgaste para `maintenance_vault_id`. O restante vai para "Sem caixinha".
- **R6 — Falta aportar (por caixinha).** `Σ sugestões (R5) desde max(tracking_start_date, início do custo, cost_center_started_on) até hoje − Σ aportes líquidos no mesmo intervalo` (aportes manuais + aportes automáticos de baixa + transferências recebidas + seus estornos, lidos da razão). Com o aporte automático, a "falta" mostra só o que ainda não foi baixado ou não coube no teto.
- **R7 — Fatura de cartão.** Mês da fatura = `purchaseDay < min(closingDay, tamanho do mês) ? mês da compra : mês seguinte` (regra do `CardPaymentModal`). Parcela *k* cai em +*k−1* meses. Parcela = `total ÷ N` (escala 2, HALF_UP); a última absorve a diferença (soma exata = total; ex.: 3× R$ 100,01 → 33,34 + 33,34 + 33,33). Fechamento = `closingDay` do mês da fatura, limitado ao tamanho do mês. Vencimento = mesmo mês se `dueDay > closingDay`, senão mês seguinte (limitado ao tamanho do mês).
  Status: `PAGA` (pagamentos ≥ total) · `ABERTA` (hoje < fechamento) · `FECHADA` (fechamento ≤ hoje ≤ vencimento) · `VENCIDA` (hoje > vencimento e não paga).
  **Cartão de débito (extrato):** sem fechamento, vencimento, pagamento ou lembrete. O extrato é por **mês de calendário** da compra, lista cada gasto com a conta/caixinha de origem e mostra o total do mês. Exige conta vinculada em `card_account_links`.
- **R8 — Liquidações** (custo fixo, fatura) não são despesa (D4).
- **R9 — Razão (`vault_ledger`).** `amount` tem sinal (+ entra, − sai). Tipos: `aporte` (inclui o aporte automático de baixa, com `ref_type = baixa_fatura`), `retirada`, `transferencia` (duas linhas, `leg` 0 e 1, mesma `operation_id`), `ajuste`, `pagamento_despesa`, `pagamento_custo_fixo`, `pagamento_fatura`, `estorno`. **Linhas nunca são editadas nem apagadas.** Estorno = nova linha de sinal oposto com `reverses_entry_id`. Reenvio com a mesma `operation_id` não duplica. Saldo = `initial_balance` + Σ `amount`. **Retirada, transferência e pagamento nunca excedem o saldo (D21)**; só o estorno pode deixar saldo negativo.
- **R10 — Edição de despesa PIX (Manutenção).** Mesma caixinha, valor 100 → 80: o app reduz **primeiro a parte da conta** e só depois a da caixinha (linha `ajuste` positiva). Valor maior: o acréscimo usa R13 (caixinha até o saldo, resto na conta). Caixinha diferente: estorno da linha antiga + nova linha. PIX → Cartão/Dinheiro: estorno do líquido atual. Exclusão da despesa: estorno do líquido atual.
- **R11 — Arredondamento.** `BigDecimal` sempre; escala 6 em intermediários; HALF_UP; 2 casas só na exibição e na persistência.

- **R12 — Alocação proporcional na baixa de fatura.** Na baixa da fatura da plataforma P com período [início, fim] (limitado a `cost_center_started_on ≤ dia ≤ hoje`), para cada dia *d* e caixinha *v*: `aporte(d, v) = reserva_do_dia(d, v) × ganho_líquido(P, d) ÷ Σ ganho_líquido(todas as plataformas, d)`. Dia **sem nenhum ganho** e ainda não coberto entra integralmente na **primeira baixa cujo fim ≥ d**. Dia/plataforma já coberto por alocação `ATIVA` não entra de novo. Soma em escala 6, arredondamento uma vez por caixinha. O total de todas as caixinhas é **limitado ao `netTotalAmount`** da fatura (redução proporcional entre caixinhas); o que não couber continua em "falta aportar". Na prévia, o usuário pode editar os valores ou desmarcar caixinhas (a desmarcada fica `DISPENSADA`, sem aporte, e seus dias continuam pendentes). Exemplo: dia 10, ganho R$ 150 na plataforma A e R$ 50 na B, reserva do dia R$ 8 → baixa de A = 6,00; baixa de B = 2,00.
- **R13 — Pagamento dividido.** `parteCaixinha = min(valorEscolhido, saldoDaCaixinha, valorDoPagamento)`, nunca negativa; `parteConta = valorDoPagamento − parteCaixinha`. `parteConta > 0` exige conta bancária. Só a `parteCaixinha` gera linha na razão; as duas partes ficam no registro do pagamento. Valor escolhido começa em `min(saldo, pagamento)` e é editável.
- **R14 — Desfazer baixa.** Estorna (linhas `estorno` com `reverses_entry_id`) os aportes da baixa, marca as alocações como `DESFEITA` e libera os dias para a próxima baixa. Se uma caixinha já gastou o valor, o estorno é aplicado e o saldo pode ficar negativo, com aviso (única exceção de D21).

**Exemplo de conferência (15/06/2026, modo CALENDAR):** ganho R$ 200,00; combustível R$ 30,00; internet R$ 120,00 mensal (junho = 30 dias → 4,00); seguro R$ 600,00 anual com início em 01/01/2026 (365 dias → 1,643836); óleo R$ 60,00 / 3.000 km e pneu R$ 400,00 / 20.000 km, ambos já trocados, 100 km no dia → 2,00 + 2,00. Reserva = 9,643836 → **R$ 9,64**. Lucro real = 200 − 30 − 9,643836 = **R$ 160,36**. (Os valores 120, 600 etc. existem só nos **testes**; no app, o usuário os digita.)

---

## 7. Catálogo de custos fixos (modelo da lista — **sem valores**)

Este catálogo vira o arquivo JSON da F5. Ele define só **nome, categoria, periodicidade sugerida e caixinha sugerida**. Os valores variam por UF, município, veículo e usuário: **quem digita é o usuário**. A coluna **Pré** marca os itens criados automaticamente na primeira abertura (D17); todos podem ser excluídos. Para ciclos maiores que 1 ano (CNH, curso), o usuário cadastra o **equivalente anual** (total ÷ anos de validade). Para equipamentos, o rateio é `valor ÷ meses de vida útil`.

**Categorias (enum):** `veiculo_tributo`, `veiculo_protecao`, `garagem`, `financiamento`, `comunicacao`, `trabalho_fiscal`, `documentos`, `protecao_pessoal`, `equipamento`, `outro`.
**Propósitos de caixinha (enum):** `manutencao`, `impostos`, `garagem`, `seguro`, `custos_fixos`, `viagem`, `emergencia`, `outro`.

| `key` | Item | Categoria | Periodicidade sugerida | Caixinha sugerida | Pré |
|---|---|---|---|---|---|
| `ipva` | IPVA | veiculo_tributo | anual | impostos | ✔ |
| `licenciamento` | Licenciamento anual (CRLV-e) | veiculo_tributo | anual | impostos | ✔ |
| `seguro_obrigatorio` | Seguro obrigatório (DPVAT/SPVAT, só se estiver sendo cobrado) | veiculo_tributo | anual | impostos | |
| `vistoria` | Vistoria/inspeção veicular (quando exigida) | veiculo_tributo | anual | impostos | |
| `debitos_veiculo` | Parcelamento de débitos do veículo (IPVA/multas) | veiculo_tributo | mensal | impostos | |
| `seguro_veiculo` | Seguro do veículo | veiculo_protecao | mensal | seguro | ✔ |
| `protecao_veicular` | Proteção veicular/associação | veiculo_protecao | mensal | seguro | |
| `rastreador` | Rastreador (mensalidade) | veiculo_protecao | mensal | seguro | ✔ |
| `assistencia_24h` | Assistência 24h/guincho | veiculo_protecao | mensal | seguro | |
| `seguro_rc_carga` | Seguro de carga/responsabilidade civil | veiculo_protecao | mensal | seguro | |
| `garagem` | Aluguel de garagem/estacionamento | garagem | mensal | garagem | ✔ |
| `financiamento` | Parcela de financiamento/consórcio do veículo | financiamento | mensal | custos_fixos | |
| `locacao_veiculo` | Aluguel/locação do veículo | financiamento | semanal | custos_fixos | |
| `internet` | Plano de internet | comunicacao | mensal | custos_fixos | ✔ |
| `celular_dados` | Plano de celular/dados | comunicacao | mensal | custos_fixos | ✔ |
| `apps_assinaturas` | Assinaturas de apps (navegação, rotas, OCR) | comunicacao | mensal | custos_fixos | |
| `das_mei` | DAS-MEI | trabalho_fiscal | mensal | impostos | |
| `inss_autonomo` | INSS/previdência (autônomo) | trabalho_fiscal | mensal | impostos | |
| `contador` | Contador/contabilidade | trabalho_fiscal | mensal | custos_fixos | |
| `alvara_licenca` | Alvará/licença municipal | trabalho_fiscal | anual | impostos | |
| `tarifa_bancaria` | Tarifa de conta/maquininha | trabalho_fiscal | mensal | custos_fixos | |
| `sindicato` | Sindicato/associação da categoria | trabalho_fiscal | mensal | custos_fixos | |
| `renovacao_cnh` | Renovação de CNH/exames (equivalente anual) | documentos | anual | impostos | |
| `curso_motofrete` | Curso especializado/reciclagem (equivalente anual) | documentos | anual | impostos | |
| `plano_saude` | Plano de saúde | protecao_pessoal | mensal | custos_fixos | |
| `seguro_vida_acidentes` | Seguro de vida/acidentes pessoais | protecao_pessoal | mensal | seguro | |
| `equipamento_bau` | Baú/top case (rateio) | equipamento | mensal | custos_fixos | |
| `equipamento_epi` | Capacete, jaqueta, luvas (rateio) | equipamento | mensal | custos_fixos | |
| `equipamento_bag` | Bag térmica/mochila (rateio) | equipamento | mensal | custos_fixos | |
| `acessorios_celular` | Suporte de celular/carregador (rateio) | equipamento | mensal | custos_fixos | |
| `reserva_imprevistos` | Reserva para imprevistos/manutenção corretiva | outro | mensal | manutencao | |
| `depreciacao_veiculo` | Depreciação do veículo (**analítico**: não entra no Lucro real nem no aporte) | outro | mensal | custos_fixos | |
| `outro` | Outro custo fixo (nome livre) | outro | mensal | custos_fixos | |

**Custos variáveis (como ficam):** combustível, alimentação e equipe já são despesas e entram no custo realizado do dia. Peças (óleo, pneus, relação, pastilhas…) entram pelo desgaste (R2). Lavagem avulsa, pedágio, estacionamento avulso e multas não têm categoria própria hoje; ficam fora de escopo (seção 12).

---

## 8. Regras globais (valem para todas as fases)

- **G1.** Leia `docs/plano-centro-de-custos.md` (seções 4 a 8) antes de escrever código. Se o arquivo não existir, PARE e informe.
- **G2.** Execute diretamente. NÃO responda no formato JSON de tarefas do Orquestrador descrito em `.agents/rules/equipe-agentes.md` e NÃO delegue a subagentes.
- **G3.** Dinheiro **somente em `BigDecimal`**. É PROIBIDO `Double`/`Float` em valores, quotas e custo por km. Toda divisão informa `scale` e `RoundingMode.HALF_UP`. Intermediário com scale 6; 2 casas só na exibição e na persistência. Exibição: reutilize a extensão pública `BigDecimal.formatCurrency()` de `ui/screens/relatorios/RelatoriosComponents.kt` (várias telas têm cópias `private` — NÃO crie mais uma). Entrada de valores: reutilize `cleanCurrencyInput` e `parseCurrency` de `ui/utils/InputMasks.kt`.
- **G4.** Arquitetura do módulo novo (permitido e obrigatório a partir da F0): **Hilt** para injeção, **Room** como fonte de verdade local, **outbox + sync** para o Supabase (Retrofit/PostgREST), **WorkManager** (`@HiltWorker`) para sincronização, conciliação e notificações. O **código legado NÃO é migrado**: nenhum repositório, ViewModel ou tela antiga passa a usar Hilt/Room, exceto os ganchos mínimos descritos nas fases. É PROIBIDO Koin, `fallbackToDestructiveMigration`, backend intermediário e dependência não listada na F0.
- **G5.** ViewModels novos: `@HiltViewModel` + `MutableStateFlow<UiState>`; user id via `supabase.auth.currentUserOrNull()?.id` encapsulado em um `UserSession` injetável. Toda escrita grava no Room, enfileira no outbox e chama `AppDataSync.notifyDataChanged()`. ViewModels legados que precisarem de dados novos recebem o provider como **parâmetro opcional com default** (sem quebrar os testes existentes).
- **G6.** UI: Compose Material 3 com a paleta de `ui/theme/Color.kt` (`OrangeNeon`, `GreenNeon`, `RedAlert`, `BlueInfo`, `YellowGold`, `PurpleTeam`). Código novo vai em **arquivos novos**. NÃO acrescente lógica a `HomeScreen.kt` (2254 linhas), `HistoricoScreen.kt` (1538) ou `FaturasScreen.kt` (2544) além do mínimo de integração (rota, parâmetro, chamada de composable).
- **G7.** NÃO altere a semântica do Saldo atual do Histórico nem a assinatura pública de classes existentes. Se for inevitável, atualize todos os chamadores e testes na mesma fase.
- **G8.** Testes JUnit4 em `app/src/test/java/com/fernando/centraldomotorista/<area>/`. Repositórios legados: padrão Fake API (ver `delivery/DeliveryPartnerSessionRepositoryTest.kt`). Room: banco em memória com Robolectric. ViewModels: `kotlinx-coroutines-test` (`StandardTestDispatcher`/`UnconfinedTestDispatcher`) e Turbine para fluxos. Toda regra de cálculo tem teste com os números do prompt.
- **G9.** Segurança: nada de credencial no código; nada de dado bancário em log; conta bancária só com **últimos dígitos**, exibida mascarada (`•••• 1234`); **nunca** `service_role` no app; funções SQL com `SET search_path = public` e objetos qualificados; `SECURITY DEFINER` só se imprescindível; tabelas novas com GRANT mínimo ao papel `authenticated`.
- **G10.** Validação obrigatória: `./gradlew testDebugUnitTest testReleaseUnitTest assembleDebug` (Windows: `gradlew.bat`). Não declare a fase concluída com teste falhando nem invente teste executado.
- **G11.** Ambiguidade: aplique a decisão do plano; se não houver, escolha a opção mais conservadora (não destrutiva) e registre no relatório. NÃO pergunte antes de começar. Se faltar acesso ao Supabase real, NÃO diga que a migração foi aplicada: entregue o SQL e marque "EXECUÇÃO PENDENTE (manual)".
- **G12.** Git: trabalhe na branch `feature/cc-f<N>-<slug>` criada a partir da branch da fase anterior (a F0 parte da `main` atualizada). Faça commits em português descritivo (siga o formato de `.agents/workflows/commit.md`) e gere o relatório com o hash do commit. **IGNORE a etapa de sync do workflow que envie para `main`.** Envie SOMENTE a branch da fase (`git push -u origin <branch>`) e abra o PR com `gh pr create` apontando para a branch da fase anterior (ou `main`, na F0); se `gh` não estiver disponível, registre o comando no relatório. **NUNCA** faça merge, **NUNCA** faça push em `main`/`master`, **NUNCA** use `--force`.
- **G13.** Resposta final obrigatória: (a) arquivos criados/alterados, (b) decisões tomadas, (c) saída resumida dos testes, (d) pendências e riscos, (e) hash do commit e link/comando do PR.
- **G14.** Room: `exportSchema = true`, esquemas versionados em `app/schemas/`, toda mudança de versão acompanha `Migration` testada com `MigrationTestHelper`. Entidades sincronizadas têm `id` UUID gerado no cliente, `updated_at`, `deleted_at` (exclusão lógica, exceto a razão) e `sync_state`.
- **G15.** **Nenhum valor monetário de custo fixo, tributo, tarifa ou mensalidade pode existir no código-fonte de produção nem no JSON do catálogo.** Valores só nascem de digitação do usuário. Valores literais são permitidos apenas em testes.
- **G16.** Ganchos mínimos no código legado (permitidos, sem mudar assinaturas nem comportamento): exclusão de despesa em `ExpenseRepository.deleteExpense` (F2) e baixa/desfazer baixa em `FaturasViewModel`/`BillingCycleRepository` (F6B). Todo gancho roda em `try/catch`: falha no módulo novo nunca quebra a operação legada.

---

## 9. Roteiro das fases

| Fase | Entrega | Depende de |
|---|---|---|
| F0 | Baseline + Fundação: Hilt, Room, WorkManager, KSP, `Application`, motor de outbox/sync, infraestrutura de testes | — |
| F1 | Banco Supabase: tabelas, RLS, razão imutável, versões de custo, alocações de reserva (sem tocar em `expenses`, `credit_cards`, `billing_cycles`) | F0 |
| F2 | Camada de dados: entidades Room, DTOs, APIs, repositórios, razão, sync, gancho de exclusão | F1 |
| F3 | Hub "Centro de Custos" + aba Contas & Caixinhas | F2 |
| F4 | Lançar Manutenção: PIX com **pagamento dividido** (caixinha + conta), pop-up de saldo, saga idempotente, conciliador | F3 |
| F5 | **Cadastro de Custos Fixos pelo usuário**: lista pré-cadastrada, valores digitados, exclusão, versões, pagamento | F4 |
| F6 | Motor de reserva (2 modos) + Lucro real no Histórico + "falta aportar" | F5 |
| F6B | **Aporte automático na baixa da fatura**, prévia editável, **desfazer baixa**, tela "Reserva a separar" | F6 |
| F7 | Cartões: faturas de crédito, extrato de débito, vínculo de conta, pagamento de fatura | F6B |
| F8 | Notificações de fatura (WorkManager + Hilt) | F7 |
| F9 | Opcional: PIX/débito em Combustível/Alimentação, aba Resumo, chip na Home | F7 (F8 recomendada) |
| F10 | Consolidação, auditoria e relatório final | todas |

---

## 10. Prompts

### PROMPT F0 — Baseline e Fundação (Hilt, Room, WorkManager)

````markdown
# FASE 0 — Baseline e Fundação técnica: Hilt, Room, WorkManager, KSP

PAPEL: Engenheiro Android Sênior (Kotlin/Gradle). Execute você mesmo, sem delegar.
BRANCH: `feature/cc-f0-fundacao`, a partir da `main` atualizada (regra G12).

CONTEXTO: o projeto NÃO tem Room, Hilt, WorkManager nem KSP, e NÃO tem classe Application (o <application> do AndroidManifest.xml não tem android:name). O nome `CentralDoMotoristaApp` já existe como composable: NÃO o reutilize. Código legado não será migrado (G4): esta fase só instala a base para o módulo novo.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 2, 4, 5 e 8), gradle/libs.versions.toml, build.gradle.kts (raiz), app/build.gradle.kts, app/src/main/AndroidManifest.xml, MainActivity.kt, data/remote/RetrofitClient.kt, data/remote/SupabaseClient.kt e .agents/workflows/commit.md.
2. BASELINE (somente leitura, ANTES de qualquer alteração): registre o hash do commit e rode `./gradlew testDebugUnitTest testReleaseUnitTest assembleDebug`. Crie docs/financeiro/00-diagnostico-baseline.md com: hash, saída resumida dos testes (liste testes que já falham ANTES da sua mudança, se houver), versões do toolchain, mapa arquivo → função → tabela das telas de Manutenção, Histórico e Cartões, e achados classificados CRÍTICO/ALTO/MÉDIO. Se a baseline estiver quebrada, registre e continue só se a quebra não for causada por dependências; não esconda falhas.
3. DEPENDÊNCIAS: adicione ao catálogo gradle/libs.versions.toml e use em app/build.gradle.kts: plugin KSP (versão compatível com Kotlin 2.1.0), Hilt (`hilt-android`, compiler via ksp, plugin `com.google.dagger.hilt.android`), `androidx.hilt:hilt-navigation-compose`, `androidx.hilt:hilt-work` + `androidx.hilt:hilt-compiler`, Room (`room-runtime`, `room-ktx`, compiler via ksp, `room-testing`), WorkManager (`androidx.work:work-runtime-ktx`, `androidx.work:work-testing`), e para testes `kotlinx-coroutines-test` (mesma versão de coroutines do projeto), `app.cash.turbine`, `robolectric`. Use SEMPRE a versão ESTÁVEL mais recente compatível com Kotlin 2.1.0, AGP 8.8.2 e compileSdk 35 (confirme no Maven do Google/Central; nunca alpha/beta/rc; se a mais nova for incompatível, use a mais recente compatível e registre). Configure `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, `testOptions { unitTests { isIncludeAndroidResources = true } }` e `sourceSets` para exportar app/schemas.
4. APPLICATION: crie `LogisticPrimeApplication` com `@HiltAndroidApp`, implementando `Configuration.Provider` com `HiltWorkerFactory` injetada. Registre em AndroidManifest.xml com `android:name=".LogisticPrimeApplication"` e DESABILITE o inicializador padrão do WorkManager (`<provider android:name="androidx.startup.InitializationProvider" ... tools:node="merge">` com `<meta-data android:name="androidx.work.WorkManagerInitializer" tools:node="remove" />`). Anote `MainActivity` com `@AndroidEntryPoint`.
5. MÓDULOS HILT (pacote di/): `NetworkModule` expõe os objetos JÁ existentes de RetrofitClient e SupabaseClient (NÃO recrie Retrofit, Gson nem o cliente Supabase); `DispatchersModule` (qualificadores IO/Default); `DatabaseModule` (Room); `UserSession` (interface + implementação) que expõe `userIdFlow` e `currentUserId()` a partir do Supabase Auth.
6. ROOM BASE: crie `CostCenterDatabase` (versão 1, exportSchema = true) contendo, por enquanto, apenas `OutboxEntity` (id, table_name, row_id, operation, payload_json, attempts, last_error, created_at) e `SyncCursorEntity` (table_name, last_pulled_at). Nome do banco inclui o user id; crie `UserDatabaseGuard`: ao detectar troca de usuário ou logout, feche e apague o banco local (D19). Payload do outbox é um JsonObject serializado com `null` EXPLÍCITO (Gson omite null por padrão).
7. MOTOR DE SINCRONIZAÇÃO (pacote sync/): interface `TableSyncHandler { val table: String; suspend fun push(item: OutboxItem): PushResult; suspend fun pull(since: Instant?): PullResult }`; `SyncEngine` que (a) processa o outbox em ordem de criação, com retry e backoff exponencial, descartando para "falha permanente" após 8 tentativas (registrando o erro, nunca apagando em silêncio); (b) trata resposta 409/duplicado como SUCESSO idempotente; (c) puxa mudanças por `updated_at` > cursor; (d) resolve conflito de tabelas mutáveis por último `updated_at` e NUNCA altera linhas da razão. `SyncScheduler` agenda `OneTimeWork` único (`ExistingWorkPolicy.APPEND_OR_REPLACE`, restrição NetworkType.CONNECTED) após cada escrita e `PeriodicWork` único de 6 h. `SyncWorker` é `@HiltWorker` (`@AssistedInject`), exige sessão válida (`Result.success()` sem sessão) e usa um registro de handlers que está VAZIO nesta fase.
8. TESTES (Robolectric + coroutines-test + work-testing): outbox preserva a ordem; retry incrementa `attempts` e respeita o backoff; 409 conta como sucesso; 8 falhas viram falha permanente visível; payload com `null` explícito é preservado; troca de usuário apaga o banco local; `SyncWorker` sem sessão retorna success sem chamar handlers; scheduler não duplica trabalho único. Teste de migração: `MigrationTestHelper` carrega o esquema 1 exportado.
9. REGRESSÃO: nenhuma tela, ViewModel ou repositório legado pode ter o comportamento alterado; compare com a baseline. Confirme que o app abre (assembleDebug) e que a regra G2 foi respeitada.

É PROIBIDO: migrar código legado para Hilt/Room; `fallbackToDestructiveMigration`; Koin; criar tabelas de domínio (vêm na F2); versões alpha/beta; alterar `expenses`, rotas ou qualquer lógica de negócio.

CRITÉRIOS DE ACEITE: baseline documentada; `./gradlew testDebugUnitTest testReleaseUnitTest assembleDebug` verde com os testes novos; Application registrada; esquema Room exportado em app/schemas; WorkManager inicializado via Hilt (sem inicializador duplicado); PR aberto SEM merge (G12).
````

---

### PROMPT F1 — Banco de dados (Supabase)

````markdown
# FASE 1 — Banco de dados (Supabase): Centro de Custos

PAPEL: Engenheiro de Banco de Dados Sênior (PostgreSQL/Supabase). Execute você mesmo, sem delegar.
BRANCH: `feature/cc-f1-banco`, a partir da branch da F0.
PRÉ-REQUISITO: Fase 0 concluída.

CONTEXTO: o app acessa o Supabase direto pelo PostgREST com chave anon; a ÚNICA barreira de segurança é o RLS. Tabelas antigas usam user_id TEXT; as novas usam UUID + RLS `auth.uid() = user_id`. As tabelas `expenses`, `credit_cards`, `part_maintenance` e `billing_cycles` já têm dados reais e NÃO serão alteradas nesta fase nem em nenhuma outra deste plano.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8), docs/backend/migracao-rotas-e-paradas-master.sql (convenções de PK, timestamps, trigger de updated_at e RLS) e docs/backend/auditoria-banco-e-apis.md (pode estar defasado: o schema real prevalece).
2. Inspecione o schema REAL antes de escrever DDL: tipos de expenses.id, expenses.occurred_at, expenses.category, expenses.amount, credit_cards.id, credit_cards.user_id, billing_cycles.id e billing_cycles.platform_id; execute `SELECT version();`. Registre os resultados no relatório e ajuste os tipos das colunas que referenciam essas tabelas (`ref_id`, `expense_id`, `card_id`, `billing_cycle_id`, `platform_id`).
3. Crie docs/backend/migracao-centro-de-custos.sql, IDEMPOTENTE, em uma única transação (BEGIN; ... COMMIT;): CREATE TABLE IF NOT EXISTS, ADD COLUMN IF NOT EXISTS, CREATE INDEX IF NOT EXISTS, DROP POLICY IF EXISTS antes de CREATE POLICY, CREATE OR REPLACE VIEW/FUNCTION.
4. Em TODAS as tabelas novas: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()` (o app envia o id gerado no cliente), `user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE`, `created_at` e `updated_at` TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()), trigger de updated_at (reutilize public.handle_master_updated_at() se existir; senão crie public.handle_updated_at() com `SET search_path = public`), RLS habilitado e policies para `authenticated` com `auth.uid() = user_id` (USING e WITH CHECK). Valores monetários: NUMERIC(12,2). Em tabelas pai referenciadas por FK composta, crie `UNIQUE (id, user_id)`; nas filhas use `FOREIGN KEY (<pai>_id, user_id) REFERENCES <pai>(id, user_id)` para impedir referência cruzada entre usuários.
5. Crie estas tabelas:
   - bank_accounts: name TEXT NOT NULL; institution; agency; account_last_digits TEXT CHECK (account_last_digits IS NULL OR length(account_last_digits) <= 6); account_type TEXT CHECK IN ('corrente','poupanca','pagamento','outra'); informed_balance NUMERIC(12,2); informed_balance_at TIMESTAMPTZ; active BOOLEAN NOT NULL DEFAULT TRUE; sort_order INT NOT NULL DEFAULT 0; deleted_at TIMESTAMPTZ.
   - vaults: account_id (FK composta → bank_accounts); name TEXT NOT NULL; purpose TEXT NOT NULL DEFAULT 'outro' CHECK IN ('manutencao','impostos','garagem','seguro','custos_fixos','viagem','emergencia','outro'); goal_amount NUMERIC(12,2) CHECK (goal_amount IS NULL OR goal_amount > 0); initial_balance NUMERIC(12,2) NOT NULL DEFAULT 0; tracking_start_date DATE NOT NULL DEFAULT CURRENT_DATE; active BOOLEAN NOT NULL DEFAULT TRUE; sort_order INT NOT NULL DEFAULT 0; deleted_at TIMESTAMPTZ; índice único parcial (account_id, lower(name)) WHERE deleted_at IS NULL.
   - vault_ledger (RAZÃO IMUTÁVEL): vault_id (FK composta → vaults); operation_id UUID NOT NULL; leg SMALLINT NOT NULL DEFAULT 0 CHECK (leg IN (0,1)); entry_type TEXT NOT NULL CHECK IN ('aporte','retirada','transferencia','ajuste','pagamento_despesa','pagamento_custo_fixo','pagamento_fatura','estorno'); amount NUMERIC(12,2) NOT NULL CHECK (amount <> 0) (COM SINAL: + entra, − sai); occurred_at TIMESTAMPTZ NOT NULL; ref_type TEXT CHECK (ref_type IS NULL OR ref_type IN ('despesa','custo_fixo','fatura','manual','baixa_fatura')); ref_id TEXT; contra_vault_id UUID; reverses_entry_id UUID REFERENCES vault_ledger(id); note TEXT; UNIQUE (operation_id, leg); CHECK (entry_type <> 'estorno' OR reverses_entry_id IS NOT NULL); CHECK (entry_type <> 'transferencia' OR contra_vault_id IS NOT NULL). SEM coluna updated_at/deleted_at. Policies SOMENTE de SELECT e INSERT (nenhuma de UPDATE/DELETE) e um trigger BEFORE UPDATE OR DELETE que lança exceção (`RAISE EXCEPTION 'vault_ledger é imutável'`). GRANT apenas SELECT e INSERT.
   - expense_settlements: expense_id <tipo real de expenses.id> NOT NULL UNIQUE; kind TEXT NOT NULL DEFAULT 'pix' CHECK (kind IN ('pix','debito')); account_id (FK composta → bank_accounts); vault_id (FK composta → vaults; NULL = sem caixinha); amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0); vault_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (vault_amount >= 0); account_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (account_amount >= 0); CHECK (vault_amount + account_amount = amount); CHECK (account_amount = 0 OR account_id IS NOT NULL); CHECK (vault_amount = 0 OR vault_id IS NOT NULL); status TEXT NOT NULL DEFAULT 'PENDENTE_DESPESA' CHECK IN ('PENDENTE_DESPESA','CONFIRMADA','ABORTADA'); operation_id UUID NOT NULL UNIQUE.
   - fixed_costs (SEM COLUNA DE VALOR): name TEXT NOT NULL; category TEXT NOT NULL CHECK IN ('veiculo_tributo','veiculo_protecao','garagem','financiamento','comunicacao','trabalho_fiscal','documentos','protecao_pessoal','equipamento','outro'); catalog_key TEXT; periodicity TEXT NOT NULL CHECK IN ('semanal','mensal','bimestral','trimestral','semestral','anual'); due_day SMALLINT CHECK (due_day BETWEEN 1 AND 31); due_month SMALLINT CHECK (due_month BETWEEN 1 AND 12); start_date DATE (NULO até o usuário informar o valor); end_date DATE; vault_id (FK composta → vaults, ON DELETE da lógica: use SET NULL via coluna e trate no app); is_vehicle_related BOOLEAN NOT NULL DEFAULT FALSE; analytical BOOLEAN NOT NULL DEFAULT FALSE; seeded BOOLEAN NOT NULL DEFAULT FALSE; notes TEXT; active BOOLEAN NOT NULL DEFAULT TRUE; deleted_at TIMESTAMPTZ; CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date). Índice único parcial (user_id, catalog_key) WHERE catalog_key IS NOT NULL AND catalog_key <> 'outro' AND deleted_at IS NULL (impede duplicar o mesmo item do catálogo).
   - fixed_cost_versions: fixed_cost_id (FK composta → fixed_costs); amount NUMERIC(12,2) NOT NULL CHECK (amount > 0); effective_from DATE NOT NULL; UNIQUE (fixed_cost_id, effective_from).
   - fixed_cost_payments: fixed_cost_id (FK composta); competence_date DATE NOT NULL; paid_at TIMESTAMPTZ NOT NULL; amount NUMERIC(12,2) NOT NULL CHECK (amount > 0); source_account_id; source_vault_id (FKs compostas); vault_amount NUMERIC(12,2) NOT NULL DEFAULT 0; account_amount NUMERIC(12,2) NOT NULL DEFAULT 0; CHECK (vault_amount >= 0 AND account_amount >= 0 AND vault_amount + account_amount = amount); CHECK (account_amount = 0 OR source_account_id IS NOT NULL); CHECK (vault_amount = 0 OR source_vault_id IS NOT NULL); operation_id UUID NOT NULL UNIQUE; notes; deleted_at; UNIQUE (fixed_cost_id, competence_date) WHERE deleted_at IS NULL.
   - card_invoice_payments: card_id <tipo real de credit_cards.id> NOT NULL (sem FK se user_id de credit_cards for TEXT); reference_month DATE NOT NULL CHECK (EXTRACT(DAY FROM reference_month) = 1); amount NUMERIC(12,2) NOT NULL CHECK (amount > 0); paid_at TIMESTAMPTZ NOT NULL; source_account_id; source_vault_id; vault_amount, account_amount NUMERIC(12,2) NOT NULL DEFAULT 0 (mesmos CHECKs de fixed_cost_payments); operation_id UUID NOT NULL UNIQUE; notes; deleted_at. Permita vários pagamentos por fatura (pagamento parcial).
   - cost_center_settings: `user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE` (sem coluna id); maintenance_vault_id UUID; wear_provision_enabled BOOLEAN NOT NULL DEFAULT TRUE; fixed_cost_provision_enabled BOOLEAN NOT NULL DEFAULT TRUE; reserve_mode TEXT NOT NULL DEFAULT 'CALENDAR' CHECK (reserve_mode IN ('CALENDAR','WORKING_DAYS')); planned_weekdays SMALLINT NOT NULL DEFAULT 127 CHECK (planned_weekdays BETWEEN 0 AND 127) (bit0 = segunda … bit6 = domingo; 127 = domingo a sábado, todos os dias); days_off JSONB NOT NULL DEFAULT '[]'; seeded_catalog_version INT NOT NULL DEFAULT 0; dismissed_seed_keys JSONB NOT NULL DEFAULT '[]'; reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE; reminder_offsets JSONB NOT NULL DEFAULT '[3,0]'; cost_center_started_on DATE (nulo até a primeira abertura do Centro de Custos; o app grava hoje); updated_at.
   - reserve_allocations: billing_cycle_id <tipo real de billing_cycles.id> NOT NULL; vault_id (FK composta → vaults) NOT NULL; suggested_amount NUMERIC(12,2) NOT NULL CHECK (suggested_amount >= 0); amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0) (valor final após edição na prévia); net_cap_applied BOOLEAN NOT NULL DEFAULT FALSE; status TEXT NOT NULL DEFAULT 'ATIVA' CHECK IN ('ATIVA','DESFEITA','DISPENSADA'); operation_id UUID NOT NULL UNIQUE; undone_at TIMESTAMPTZ; deleted_at; índice único parcial (billing_cycle_id, vault_id) WHERE status IN ('ATIVA','DISPENSADA').
   - reserve_allocation_days: allocation_id (FK composta → reserve_allocations); day DATE NOT NULL; platform_id TEXT (NULL = dia sem nenhum ganho); suggested_amount NUMERIC(14,6) NOT NULL CHECK (suggested_amount >= 0); índice único (allocation_id, day, COALESCE(platform_id, '')).
   - card_account_links: card_id <tipo real de credit_cards.id> NOT NULL; account_id (FK composta → bank_accounts) NOT NULL; deleted_at; índice único parcial (card_id) WHERE deleted_at IS NULL.
6. NÃO altere `expenses`, `credit_cards`, `part_maintenance`, `billing_cycles` nem as policies delas.
7. Índices: vaults(user_id, account_id); vault_ledger(user_id, occurred_at DESC), (vault_id, occurred_at), (ref_type, ref_id); fixed_costs(user_id, active); fixed_cost_versions(fixed_cost_id, effective_from DESC); fixed_cost_payments(source_vault_id); card_invoice_payments(card_id, reference_month); expense_settlements(vault_id); reserve_allocations(billing_cycle_id); reserve_allocation_days(allocation_id, day); card_account_links(account_id).
8. View `public.vault_balances WITH (security_invoker = true)`: `vault_id, user_id, account_id, balance` = vaults.initial_balance + COALESCE(SUM(vault_ledger.amount), 0), juntando SOMENTE por vault_id. Se o Postgres for < 15, crie a função SQL `get_vault_balances()` SECURITY INVOKER com `SET search_path = public` e registre. A view serve para AUDITORIA e conciliação; o app calcula o saldo no Room.
9. GRANT SELECT na view e GRANT SELECT, INSERT, UPDATE, DELETE nas tabelas mutáveis para `authenticated` (razão: só SELECT, INSERT). REVOKE de tudo para `anon`.
10. ANTES de executar: registre a contagem de linhas de expenses, credit_cards e part_maintenance. Execute a migração no Supabase pela ferramenta MCP de SQL (execute_sql) SOMENTE se estiver disponível e apontando para este projeto, de preferência primeiro em uma branch/ambiente de teste. Se não estiver, NÃO improvise: finalize o arquivo, marque "EXECUÇÃO PENDENTE (manual)" no relatório e PARE.
11. Valide (e cole as evidências no relatório):
    a) information_schema.columns, pg_policies, pg_indexes e constraints das tabelas novas;
    b) contagens e a estrutura de expenses, credit_cards, part_maintenance e billing_cycles idênticas às de antes;
    c) em transação com ROLLBACK: cofre com initial_balance 100,00 + aporte +50,00 + pagamento_despesa −30,00 + pagamento_custo_fixo −10,00 + pagamento_fatura −5,00 => balance 105,00; transferência de 20,00 (duas linhas, leg 0 = −20,00 e leg 1 = +20,00) => 85,00 na origem e +20,00 no destino;
    d) UPDATE e DELETE em vault_ledger DEVEM falhar (trigger e ausência de policy);
    e) INSERT repetido com o mesmo (operation_id, leg) DEVE falhar com violação de unicidade (o app trata como sucesso idempotente);
    f) estorno sem reverses_entry_id DEVE falhar;
    g) inserir fixed_cost_versions com amount ≤ 0 DEVE falhar; fixed_costs aceita start_date nulo;
    h) isolamento com dois user_id distintos (SET LOCAL ROLE authenticated + set_config('request.jwt.claims', ...)): B enxerga 0 linhas de todas as tabelas novas de A e NÃO consegue inserir vault_ledger apontando para a caixinha de A (FK composta);
    i) a segunda execução do arquivo SQL não falha (idempotência).
    j) pagamento com vault_amount + account_amount ≠ amount DEVE falhar; account_amount > 0 sem conta DEVE falhar;
    k) reaplicar o mesmo operation_id em reserve_allocations DEVE falhar por unicidade; duas alocações ATIVA para o mesmo (billing_cycle_id, vault_id) DEVEM falhar;
    l) dois vínculos ativos para o mesmo card_id em card_account_links DEVEM falhar.
12. Gere docs/backend/relatorio-centro-de-custos-fase-1.md com as evidências, a versão do Postgres e os tipos reais encontrados.

É PROIBIDO: DROP TABLE/COLUMN, TRUNCATE, DELETE ou UPDATE em dados existentes, alterar tipos ou policies de tabelas existentes, `service_role`, colocar chave/senha no SQL, inserir qualquer valor monetário de custo como dado padrão (seed de custos é feito pelo app, sem valor).

CRITÉRIOS DE ACEITE: SQL idempotente; RLS em todas as tabelas; testes (c) a (l) comprovados; contagens preservadas; relatório gerado; PR aberto SEM merge.
````

---

### PROMPT F2 — Camada de dados Android (Room, DTOs, sync, razão)

````markdown
# FASE 2 — Camada de dados Android: Room, DTOs, APIs, repositórios, razão e sincronização

PAPEL: Engenheiro Android Sênior (Kotlin). Execute você mesmo.
BRANCH: `feature/cc-f2-dados`, a partir da branch da F1.
PRÉ-REQUISITO: Fases 0 e 1 concluídas e migração executada no Supabase.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8). Leia como modelo: data/model/Models.kt, data/remote/dto/ExpenseDto.kt, data/remote/api/ExpenseApi.kt, data/repository/ExpenseRepository.kt, data/repository/CreditCardRepository.kt e o código de sync criado na F0 (sync/).
2. Domínio (data/model/CostCenterModels.kt): BankAccount, Vault, VaultBalance, LedgerEntry, ExpenseSettlement, FixedCost, FixedCostVersion, FixedCostPayment, CardInvoicePayment, ReserveAllocation, ReserveAllocationDay, CardAccountLink, CostCenterSettings e os enums AllocationStatus, SettlementKind (PIX, DEBITO), VaultPurpose, LedgerEntryType, FixedCostCategory, FixedCostPeriodicity (SEMANAL, MENSAL, BIMESTRAL, TRIMESTRAL, SEMESTRAL, ANUAL), ReserveMode (CALENDAR, WORKING_DAYS), SettlementStatus. Cada enum tem `key` (valor exato gravado no banco), `label` em português e `fromKey()` seguro. Dinheiro em BigDecimal; datas em LocalDate/Instant.
3. Room (pacote data/local/costcenter/): uma `@Entity` por tabela (campos espelham o banco + `sync_state` PENDENTE/SINCRONIZADO/ERRO), `TypeConverters` para BigDecimal (String), LocalDate e Instant, e um DAO por entidade com `Flow` para leitura. Suba o `CostCenterDatabase` para a versão 2 com `Migration(1, 2)` TESTADA. NÃO use `fallbackToDestructiveMigration`. Saldo da caixinha: query `initial_balance + COALESCE(SUM(amount), 0)` exposta como `Flow<BigDecimal>`.
4. Rede: DTO por entidade em data/remote/dto/ (@SerializedName snake_case, toDomain()/toDto()) e APIs em data/remote/api/ (padrão ExpenseApi): upsert por `id` com `Prefer: resolution=merge-duplicates,return=representation` para tabelas mutáveis; para vault_ledger use INSERT com `Prefer: resolution=ignore-duplicates` e `on_conflict=operation_id,leg`; leitura incremental com filtro `updated_at=gt.<cursor>` (razão: `created_at`). Registre em RetrofitClient como `val xApi by lazy { ... }` e exponha via Hilt (NetworkModule).
5. Sync: implemente um `TableSyncHandler` por tabela, registrado no SyncEngine da F0. Mutáveis: LWW por updated_at. Razão: somente inserção; um pull nunca sobrescreve linha existente.
6. Repositórios `@Singleton` com `@Inject constructor` (pacote data/repository/costcenter/): BankAccountRepository, VaultRepository (caixinhas com saldo), LedgerRepository, FixedCostRepository (custos + versões + pagamentos), CardInvoicePaymentRepository, SettlementRepository, ReserveAllocationRepository, CardAccountLinkRepository e CostCenterSettingsRepository. REGRA DE ESCRITA: toda operação grava no Room e no outbox NA MESMA TRANSAÇÃO (`withTransaction`) e agenda o sync.
7. LedgerRepository — operações (todas com `operationId: UUID` recebido do chamador para idempotência): `aportar`, `retirar`, `transferir` (duas linhas, leg 0 = saída, leg 1 = entrada, mesma operation_id), `ajustarSaldo(saldoCorreto)` (grava a diferença; diferença zero não grava), `registrarPagamento(refType, refId, vaultId, valorDaCaixinha, operationId)` (linha negativa só da parte da caixinha; nunca acima do saldo), `estornar(entryId)` e `estornarPorReferencia(refType, refId)` (estorna o LÍQUIDO atual da referência, em uma linha). Validações: valor > 0; transferência exige origem ≠ destino; **retirada, transferência e pagamento acima do saldo são rejeitados** (`InsufficientVaultBalance`, com o saldo no erro); `ajustarSaldo` rejeita saldo negativo; **o estorno é a única operação que pode deixar saldo negativo**; estornar linha já estornada é no-op; reaplicar a mesma operation_id é no-op. Crie também `PaymentSplitCalculator` (puro, R13): `split(total, saldoCaixinha, valorPedido?) → (parteCaixinha, parteConta)`. Nunca atualize nem apague linha da razão.
8. Exclusão lógica: conta/caixinha/custo com qualquer referência só pode ser arquivado (`active = false`); sem referência, recebe `deleted_at`. Custo fixo EXCLUÍDO nunca remove versões nem pagamentos (preserva o histórico).
9. GANCHO DE EXCLUSÃO DE DESPESA (alteração mínima no legado): crie a interface `ExpenseDeletionListener { suspend fun onExpenseDeleted(expenseId: String) }` e um registro estático `ExpenseDeletionHooks` (lista de listeners). Em `ExpenseRepository.deleteExpense`, chame os listeners APÓS a exclusão bem-sucedida, dentro de try/catch (falha do listener não pode quebrar a exclusão). NÃO mude assinaturas. Implemente `SettlementReverter` (módulo novo) que estorna a liquidação da despesa na razão, marca `expense_settlements` como ABORTADA e é registrado no `LogisticPrimeApplication`. Isso cobre PartMaintenanceViewModel, PartnerRoutesViewModel e HistoricoRepository de uma vez.
10. Testes (Robolectric + fakes de API): (a) round-trip DTO↔domínio↔entidade preservando BigDecimal com 2 casas; (b) fromKey() desconhecido não quebra; (c) transferir grava 2 linhas que somam zero; (d) mesma operation_id duas vezes não duplica; (e) ajustarSaldo grava o sinal certo e não grava com diferença zero; (f) estornar duas vezes grava uma só vez; (g) escrita + outbox atômicos (falha no outbox desfaz a escrita); (h) pull não sobrescreve a razão e aplica LWW nas mutáveis; (i) o gancho chama o estorno após `deleteExpense` e uma falha do listener não impede a exclusão; (j) migração 1→2; (k) `ExpenseDto` antigo continua desserializando; (l) `PaymentSplitCalculator`: total 100 e saldo 60 → 60 + 40; saldo 150 → 100 + 0; valor pedido 30 → 30 + 70; valor pedido acima do saldo é limitado ao saldo; saldo 0 → 0 + 100; (m) retirada e transferência acima do saldo rejeitadas; ajuste para saldo negativo rejeitado; estorno pode deixar saldo negativo.

É PROIBIDO: editar/apagar linha da razão; `Double`/`Float`; alterar assinatura de método existente do legado; tocar em `expenses` no banco; qualquer tela nesta fase.

CRITÉRIOS DE ACEITE: compila; testes antigos e novos verdes (G10); razão idempotente comprovada; nenhuma tela alterada; PR aberto SEM merge.
````

---

### PROMPT F3 — Hub "Centro de Custos" + Contas & Caixinhas

````markdown
# FASE 3 — Hub "Centro de Custos" e aba Contas & Caixinhas

PAPEL: Engenheiro Android Sênior (Jetpack Compose). Execute você mesmo.
BRANCH: `feature/cc-f3-hub-contas`, a partir da branch da F2.
PRÉ-REQUISITO: Fases 0 a 2 concluídas.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8). Leia como modelo visual: ui/screens/cards/CreditCardsScreen.kt, ui/screens/expenses/CardPaymentModal.kt (ModalBottomSheet e dropdowns) e ui/common/cards/CollapsibleSectionCard.kt.
2. Crie o pacote ui/screens/costcenter/ com: CentroCustosScreen.kt (hub com TabRow dirigido por `enum class CentroCustosTab`, inicialmente só CONTAS — fases futuras acrescentam valores), AccountsVaultsViewModel.kt (`@HiltViewModel`) e AccountsVaultsTab.kt. Componentes em ui/screens/costcenter/components/: AccountFormSheet, VaultFormSheet, VaultMovementSheet, VaultStatementSheet, SyncStatusChip.
3. Navegação em navigation/NavGraph.kt: acrescente `object CentroCustos : Screen("centro_custos", "Centro de Custos", Icons.Default.AccountBalance)` e um composable com a rota `"${Screen.CentroCustos.route}?tab={tab}&aporteVaultId={aporteVaultId}&aporteAmount={aporteAmount}"`, com os três argumentos nullable e default null (padrão da rota Historico); use `hiltViewModel()`. Se `aporteVaultId` vier preenchido, abra o VaultMovementSheet em modo APORTE já com o valor.
4. Menu: em ui/screens/home/HomeScreen.kt, na seção "Financeiro & Operacional" do drawer, acrescente um `DrawerCadastroItem` "Centro de Custos" (subtítulo "Contas, caixinhas, custos fixos e cartões") que chama `onNavigateToRoute("centro_custos")`. Nada além disso em HomeScreen.
5. Aba Contas & Caixinhas: no topo, card "TOTAL NAS CAIXINHAS" (soma dos saldos de caixinhas ativas, vindos do Room). Abaixo, cartões expansíveis por conta: apelido, instituição, `•••• últimos dígitos` e, se existir, "Saldo informado: R$ X em dd/MM (não gerenciado pelo app)". Dentro da conta, cada caixinha mostra nome, propósito, saldo (negativo em RedAlert), barra de progresso se houver meta e ações: Aportar, Retirar, Transferir, Ajustar saldo, Extrato, Editar, Arquivar.
6. Formulários: conta (apelido obrigatório; instituição, agência, tipo, saldo informado opcionais; campo de conta aceita SOMENTE até 6 dígitos, dica "informe apenas os últimos dígitos") e caixinha (nome obrigatório, propósito, saldo inicial, meta opcional, data de início do acompanhamento = hoje no fuso local). Switch "Usar como caixinha de desgaste de peças": grava cost_center_settings.maintenance_vault_id; só uma caixinha por vez.
7. Ações via LedgerRepository, SEMPRE gerando um `operationId` novo ao abrir o sheet e reutilizando-o no reenvio do mesmo formulário (toque duplo não duplica). Retirar ou transferir acima do saldo é **bloqueado**, com a mensagem "O valor máximo para retirada é o saldo existente (R$ X)" (campo já limitado ao saldo). Ajustar saldo: usuário informa o saldo correto (não aceita negativo); diferença zero => "Saldo já está correto", sem gravar.
8. Extrato (VaultStatementSheet): lista as linhas da razão agrupadas por dia, positivos em GreenNeon e negativos em RedAlert, ícone por tipo; linhas de estorno aparecem ligadas à original ("estorno de …"). Nunca ofereça "editar" ou "excluir" uma linha: correção é sempre nova linha.
9. SyncStatusChip: mostra "Sincronizado", "N alterações pendentes" ou "Falha ao sincronizar — tentar de novo" (lendo o outbox). O app funciona offline.
10. Estados obrigatórios: carregando, vazio (CTA "Cadastrar primeira conta"), erro com "Tentar novamente".
11. Testes de AccountsVaultsViewModel (coroutines-test + Turbine + repositórios fake): conta sem apelido rejeitada; conta com mais de 6 dígitos rejeitada; só uma caixinha de desgaste; transferência para a mesma caixinha rejeitada; ajuste (saldo 105,00 → informado 100,00 grava −5,00); retirada acima do saldo rejeitada com a mensagem do saldo máximo; transferência acima do saldo rejeitada; ajuste para saldo negativo rejeitado; arquivar em vez de excluir quando há referência; reenvio com o mesmo operationId não duplica; chip de sync reflete o outbox.

É PROIBIDO: gerenciar o saldo da conta principal; exibir número de conta completo; log de dados bancários; editar/apagar linha da razão.

CRITÉRIOS DE ACEITE: Centro de Custos acessível pelo drawer; CRUD de conta e caixinha; aporte/retirada/transferência/ajuste refletem no saldo na hora (Room) e sincronizam depois; testes verdes; PR aberto SEM merge.
````

---

### PROMPT F4 — Lançar Manutenção: PIX com pagamento dividido (caixinha + conta)

````markdown
# FASE 4 — Lançar Manutenção: PIX com conta de origem, caixinha e pagamento dividido

PAPEL: Engenheiro Android Sênior (Jetpack Compose). Execute você mesmo.
BRANCH: `feature/cc-f4-manutencao-pix`, a partir da branch da F3.
PRÉ-REQUISITO: Fases 0 a 3 concluídas.

CONTEXTO: hoje `savePartMaintenance` cria a despesa e depois a peça em 2 requests não atômicos. Esta fase NÃO altera a tabela `expenses`. A origem do PIX vive em `expense_settlements` e o débito vive na razão. REGRA DO PRODUTO (D8, R13): o pagamento sai da CAIXINHA (até o saldo) e o RESTANTE de uma CONTA BANCÁRIA informada; a caixinha NUNCA fica negativa. O app não gerencia o saldo da conta. Para não deixar débito órfão nem despesa sem débito, use uma SAGA IDEMPOTENTE com UUIDs gerados no cliente.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8) e, no código: ui/screens/pecas/LancarManutencaoScreen.kt (FORMA DE PAGAMENTO, ~653–800), ui/screens/pecas/PartMaintenanceViewModel.kt (`savePartMaintenance`, `startEditing`, `initOrLoad`, `deletePartMaintenance`), ui/screens/expenses/CardPaymentModal.kt, data/remote/dto/ExpenseDto.kt (`toDto()` já envia o `id` quando preenchido) e a rota LancarManutencao em navigation/NavGraph.kt (~704).
2. Crie em ui/screens/costcenter/components/ (reutilizáveis em F5, F7 e F9):
   - `data class PaymentSource(val accountId: String?, val vaultId: String?, val vaultAmount: BigDecimal, val accountAmount: BigDecimal)` — accountId e vaultId nulos só quando o usuário não tem conta ativa ("Continuar sem vincular").
   - `PaymentSplitCalculator` (da F2) para calcular `vaultAmount`/`accountAmount` (R13).
   - `PaymentSourceSheet`: ModalBottomSheet no padrão do CardPaymentModal, título "ORIGEM DO PAGAMENTO", dropdown de Conta, dropdown de Caixinha (ativas da conta, com saldo; primeira opção "Nenhuma caixinha — pagar só com a conta"), campo "Valor da caixinha" (preenchido com min(saldo, valor do pagamento), editável, máximo = min(saldo, valor)), linha "Restante na conta: R$ X" e Confirmar em OrangeNeon. Sem conta cadastrada: estado vazio com "+" que navega ao Centro de Custos e botão "Continuar sem vincular".
   - `InsufficientBalanceDialog`: título "Saldo insuficiente", texto "A caixinha '<nome>' tem R$ <saldo> e o pagamento é de R$ <valor>. Faltam R$ <diferença>. Deseja continuar? O restante será pago com uma conta bancária." e botões "Sim, continuar" e "Não, voltar".
   - `RemainderAccountSheet`: abre ao tocar em "Sim, continuar"; título "DE QUAL CONTA SAIU O RESTANTE?"; escolha da conta (padrão = conta da própria caixinha), exibe "R$ <restante> sairão da conta <nome> (o app não controla o saldo da conta)" e Confirmar.
   - `PaymentSourceValidator.check(amount: BigDecimal?, vaultBalance: BigDecimal?, creditBack: BigDecimal = ZERO): BalanceCheck` (Ok | Insufficient(balance, required, shortfall)). Ok se amount nulo ou ≤ 0, se vaultBalance nulo (sem caixinha) ou se vaultBalance + creditBack ≥ amount.
3. `MaintenancePaymentSaga` (módulo novo, injetável):
   a) Ao confirmar o PIX, o ViewModel gera `expenseId = UUID` e `operationId = UUID` e os mantém no estado até o fim do fluxo (reabrir o formulário reaproveita os mesmos).
   b) Passo 1 (local, durável): em UMA transação Room grave `expense_settlements` (status PENDENTE_DESPESA, com `vault_amount` e `account_amount`) e, se `vault_amount > 0`, a linha `pagamento_despesa` (ref_type 'despesa', ref_id = expenseId, valor negativo = vault_amount, operation_id). Se o saldo mudou e já não cobre `vault_amount`, a transação FALHA e o fluxo reabre o pop-up com o saldo atual.
   c) Passo 2: o ViewModel monta a `Expense` COM `id = expenseId` e chama o `savePartMaintenance` existente. Falha por duplicidade (a despesa já existe) = sucesso após `getExpenseById`.
   d) Passo 3: sucesso → settlement CONFIRMADA. Falha definitiva ou cancelamento depois do passo 1 → estorno da linha (`estornarPorReferencia`) e settlement ABORTADA. Falha de rede → mantenha PENDENTE_DESPESA, mostre "Não foi possível salvar. Tente novamente" e permita retentar com os MESMOS ids.
   e) `SettlementReconcilerWorker` (`@HiltWorker`, periódico 1 h + disparo no início do app): settlements PENDENTE_DESPESA com mais de 10 min → consulte `getExpenseById`: existe → CONFIRMADA; não existe → estorna e ABORTADA. Idempotente.
4. No ViewModel: carregue contas e caixinhas com saldo do Room (`availableAccounts`, `availableVaults` no UiState); `paymentSource: PaymentSource?`; leia o saldo do Room ao abrir o sheet E antes de salvar (nunca valide contra saldo velho). Obtenha as dependências novas por parâmetro com default via `EntryPoint` do Hilt (o ViewModel legado não vira Hilt).
5. Em LancarManutencaoScreen: ao tocar em PIX, chame `onPaymentMethodSelected("pix")` e abra o PaymentSourceSheet (estado local `showPixSheet`, como `showCardModal`). Abaixo dos botões, resumo clicável "🏦 <Conta> • Caixinha <nome> R$ X + conta R$ Y — Alterar"; com PIX selecionado e origem não confirmada: "Origem do PIX pendente — toque para escolher" em RedAlert. Novo parâmetro `onNavigateToManageAccounts` ligado em NavGraph a `Screen.CentroCustos.route`.
6. Fluxo do pop-up: ao confirmar o sheet, se amount > 0 e o valor da caixinha escolhido é menor que o pagamento por falta de saldo, mostre o InsufficientBalanceDialog. "Sim, continuar" => abre o RemainderAccountSheet; confirmado, grava `paymentSource` (vaultAmount = saldo disponível ou o valor editado; accountAmount = restante) e fecha. "Não, voltar" => fecha dialog e sheet e VOLTA à janela Lançar Manutenção com PIX ainda selecionado e `paymentSource = null` (origem pendente), para trocar a conta/caixinha (tocando no resumo) ou mudar a forma de pagamento. NADA é gravado em "Não". Se o usuário escolher "Nenhuma caixinha", o pagamento inteiro é parte da conta (sem pop-up).
7. Em `savePartMaintenance`: (a) PIX com valor > 0 e 1+ conta ativa EXIGE `paymentSource` (erro "Confirme a conta de origem do PIX."); sem conta ativa, siga como hoje; (b) revalide o saldo ao salvar: se a caixinha já não cobre `vaultAmount`, reabra o pop-up com o saldo atual (nunca grave com saldo velho); (c) execute a saga; (d) forma de pagamento diferente de PIX não cria settlement.
8. Edição (regra R10 + R13): `startEditing` e `initOrLoad` carregam o settlement da despesa. Valor MENOR: reduza primeiro a parte da conta e só depois a da caixinha (linha `ajuste` positiva). Valor MAIOR: o acréscimo usa R13 (caixinha até o saldo, restante na conta, com o mesmo pop-up). Caixinha mudou: estorno + nova linha. PIX → Cartão/Dinheiro: `estornarPorReferencia` e settlement ABORTADA. Cartão/Dinheiro → PIX: fluxo de criação para a despesa existente. Use `creditBack` = parte original da caixinha ao validar saldo na mesma caixinha. OperationIds novos e determinísticos por edição.
9. Excluir a manutenção continua apagando a despesa; o gancho `ExpenseDeletionListener` (F2) estorna a parte da caixinha. Não crie outra lógica de estorno aqui.
10. Testes (estilo PartMaintenanceFlowTest + coroutines-test): validator (nulo, suficiente, exato, insuficiente com diferença correta, creditBack); PaymentSplit: PIX R$ 100 com caixinha de R$ 60 e "Sim" + conta do restante → razão −60,00, `account_amount` 40,00, caixinha em ZERO (nunca negativa); valor da caixinha editado para R$ 30 → 30 + 70; valor acima do saldo rejeitado; "Nenhuma caixinha" → 100% conta e sem linha na razão; PIX sem origem e com conta ativa bloqueia o salvamento; "Não" não grava nada e deixa `paymentSource = null` com PIX selecionado; trocar para Dinheiro zera a origem; edição 100 → 80 reduz primeiro a parte da conta (40 → 20) sem mexer na caixinha; edição 100 → 130 acrescenta 30 pelo R13; PIX → Cartão estorna e não cria obrigação dupla; falha na criação da despesa + conciliador => estorno e ABORTADA; reenvio com os mesmos ids não duplica despesa nem débito; falha ao salvar a peça não deixa débito órfão; saldo mudou entre o sheet e o salvar reabre o pop-up; sem conta ativa o PIX salva como hoje; excluir a manutenção estorna.

É PROIBIDO: alterar o fluxo de Cartão e de Dinheiro; criar coluna/tabela em `expenses`; deixar a caixinha negativa; validar saldo com valor em cache antigo; salvar a despesa antes de a pergunta de saldo insuficiente ser respondida; apagar linha da razão.

CRITÉRIOS DE ACEITE: PIX abre o sheet; os caminhos do pop-up (Sim, Não, saldo mudou) funcionam; o pagamento dividido grava caixinha + conta corretamente; a caixinha nunca fica negativa; a saga não deixa débito nem despesa órfãos nos testes de falha; testes verdes; PR aberto SEM merge.
````

---

### PROMPT F5 — Cadastro de Custos Fixos (valores digitados pelo usuário)

````markdown
# FASE 5 — Cadastro de Custos Fixos: lista pré-cadastrada, valores do usuário, exclusão, versões e pagamento

PAPEL: Engenheiro Android Sênior (Kotlin/Compose). Execute você mesmo.
BRANCH: `feature/cc-f5-custos-fixos`, a partir da branch da F4.
PRÉ-REQUISITO: Fases 0 a 4 concluídas (PaymentSourceSheet e InsufficientBalanceDialog já existem).

REGRA-MESTRA (G15): NENHUM valor monetário de custo fixo existe no código nem no JSON. O usuário digita todos os valores. Item sem valor = "VALOR PENDENTE" e fica fora de TODOS os cálculos.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8; a seção 7 é o catálogo oficial).
2. CATÁLOGO COMO ASSET: crie app/src/main/assets/cost_center/fixed_cost_templates.json com `{ "catalogVersion": 1, "items": [ ... ] }`, contendo TODAS as linhas da tabela da seção 7 (sem omitir nenhuma) com os campos `key`, `name`, `category`, `periodicity`, `suggestedVaultPurpose`, `hint`, `preseed` (✔ da coluna "Pré"), `analytical` (true só em `depreciacao_veiculo`). Dicas: itens de equivalente anual → "Cadastre o total ÷ anos de validade"; equipamentos → "Valor ÷ meses de vida útil"; financiamento → "Informe a parcela inteira (reserva de caixa)". É PROIBIDO qualquer campo numérico monetário. Crie `FixedCostTemplateRepository` que lê o asset com Gson e valida o esquema.
3. CALCULADORA (`util/FixedCostCalculator.kt`, objeto puro, BigDecimal, sem Android) AGORA COM VERSÕES:
   - `activeVersion(versions, date): FixedCostVersion?` (maior `effective_from ≤ date`; nulo se não houver);
   - `cycleBounds(cost, date): Pair<LocalDate, LocalDate>` (início inclusivo, fim exclusivo) — ciclos de N meses de calendário ancorados no mês de start_date (N = 1, 2, 3, 6, 12);
   - `dailyQuota(cost, versions, date): BigDecimal` (scale 6, HALF_UP): ZERO se inativo, excluído, sem versão vigente, fora de start_date..end_date ou sem start_date; semanal = valor ÷ 7; demais = valor ÷ dias do ciclo; o ÚLTIMO dia do ciclo absorve o resíduo (soma do ciclo completo sem troca de versão = valor exato);
   - `monthlyEquivalent(cost, versions, date)` (semanal = valor × 52 ÷ 12; demais = valor ÷ N);
   - `nextDueDate(cost, today): LocalDate?` (mensal: due_day limitado ao tamanho do mês; anual: due_month/due_day, padrão = mês/dia de start_date; bimestral/trimestral/semestral ancorados em start_date; semanal: dia da semana de start_date; null se end_date já passou ou sem start_date).
4. SEMEADURA IDEMPOTENTE (`FixedCostSeeder`): na primeira abertura do Centro de Custos (e quando `catalogVersion` do asset > `seeded_catalog_version`), crie no Room, SEM valor e SEM versão, um `fixed_costs` para cada item `preseed = true` que (a) ainda não exista (`catalog_key`) e (b) NÃO esteja em `dismissed_seed_keys`; marque `seeded = true`; depois atualize `seeded_catalog_version`. Rodar duas vezes não duplica. Use `operationId`/ids determinísticos (UUID v5 do user id + key) para que dois aparelhos não dupliquem. A semeadura NÃO apaga nem altera itens existentes. Na primeira execução, grave também `cost_center_started_on` = hoje (fuso local) em `cost_center_settings`, se estiver nulo (D26).
5. ABA FIXOS do hub (acrescente `FIXOS` a CentroCustosTab; arquivos novos: FixedCostsTab.kt, FixedCostsViewModel.kt `@HiltViewModel`, FixedCostFormSheet.kt, FixedCostValueSheet.kt, FixedCostPaymentSheet.kt, FixedCostCatalogSheet.kt):
   - Topo: "Custo fixo mensal equivalente" e "Quota diária de hoje" (somente itens COM valor). Se houver itens pendentes: faixa em YellowGold "N itens aguardando seu valor".
   - Lista agrupada por categoria. Item com valor: nome, valor atual, periodicidade, próximo vencimento com status (em dia / vence em até 7 dias / atrasado), caixinha de destino e quota diária. Item SEM valor: chip "VALOR PENDENTE" e botão "Informar valor".
   - Cada item tem menu: Informar/Alterar valor, Editar, Pausar/Reativar, **Excluir**.
6. INFORMAR VALOR (FixedCostValueSheet): campos valor (> 0, máscara de G3), periodicidade (editável), due_day (1–31), due_month (obrigatório só em anual), "Em vigor a partir de" (padrão = hoje; rótulo da data de início: "Início do acúmulo — ex.: mês seguinte ao último pagamento"), caixinha de destino opcional (sugerida: primeira ativa do mesmo propósito; senão em branco). Primeiro valor: define start_date e cria a primeira versão. **Alterar valor depois** oferece duas opções claras: "Novo valor a partir de [data]" (cria NOVA versão; o passado não muda) e "Corrigir valor atual (erro de digitação)" (edita a versão vigente). Mostre o histórico de versões do item.
7. EXCLUIR: confirme com "Excluir '<nome>'? Você poderá adicioná-lo de novo pelo catálogo." Exclusão lógica (`deleted_at`) — mantém versões e pagamentos para o histórico. Se o item era pré-cadastrado (`seeded`), registre o `catalog_key` em `dismissed_seed_keys` para NUNCA reaparecer sozinho. Item com pagamentos só pode ser desativado OU excluído logicamente (nunca apagado fisicamente).
8. ADICIONAR: botão "+ Adicionar custo" abre FixedCostCatalogSheet (chips por categoria) listando os itens do catálogo que NÃO estão ativos na lista; escolher um cria o item (sem valor) e abre "Informar valor"; remova o `catalog_key` de `dismissed_seed_keys` ao readicionar. Também permite "Custo personalizado" (nome livre, categoria, periodicidade). Itens `analytical` mostram o selo "Analítico — não entra no Lucro real".
9. TELA/FORMULÁRIO DE CADASTRO: FixedCostFormSheet edita nome, categoria, periodicidade, due_day/due_month, caixinha, relacionado ao veículo, observações, end_date. Valide tudo antes de gravar. O valor só é editado pelo FixedCostValueSheet (versões).
10. REGISTRAR PAGAMENTO: sheet com competência (padrão = próximo vencimento), valor (padrão = valor vigente), data/hora (padrão = agora) e origem via PaymentSourceSheet; MESMO fluxo da F4 (pagamento dividido: caixinha até o saldo + conta para o restante; "Não, voltar" retorna ao sheet). Grave `fixed_cost_payments` (com `vault_amount` e `account_amount`) + linha `pagamento_custo_fixo` só da parte da caixinha na razão (ref_type 'custo_fixo') em UMA transação Room, com operation_id. Impeça pagamento duplicado da mesma competência (UNIQUE) com mensagem clara. Pagamento NÃO cria Expense (D4). Item sem valor não permite registrar pagamento ("Informe o valor primeiro").
11. TESTES:
    - FixedCostCalculator: R$ 120,00 mensal com start 2026-01-01 em 2026-06-15 → 4,000000 (junho = 30 dias); em 2026-07-15 → 3,870968 (31 dias); R$ 120,00 em fevereiro de 2027 (28 dias) → soma das quotas de fevereiro = 120,00 EXATOS; em fevereiro de 2028 (29 dias) idem; R$ 600,00 anual start 2026-01-01 em 2026-06-15 → 1,643836; R$ 366,00 anual start 2028-01-01 em 2028-06-15 → 1,000000 (366 dias); R$ 70,00 semanal → 10,000000; dia antes de start_date e dia depois de end_date → ZERO; custo inativo, excluído ou SEM VERSÃO → ZERO; bimestral R$ 240,00 start 2026-01-01 em 2026-02-10 → 240 ÷ 59 = 4,067797; duas versões (120,00 até 2026-06-14 e 150,00 a partir de 2026-06-15): dias antes usam 120, dias depois usam 150, e dias passados NÃO mudam; nextDueDate com due_day 31 em fevereiro → último dia do mês; anual vencendo 10/12 consultado em 11/12/2026 → 10/12/2027; end_date passada → null.
    - Seeder: primeira execução cria exatamente os itens `preseed` sem valor e sem versão; segunda execução não duplica; item excluído (em dismissed) NÃO reaparece; readicionar pelo catálogo funciona; novo `catalogVersion` adiciona só itens novos.
    - FixedCostsViewModel: valor ≤ 0 rejeitado; due_month ausente em anual rejeitado; item sem valor não entra no total mensal; informar valor cria start_date + versão 1; alterar valor cria versão 2 sem mudar a 1; "Corrigir" edita a versão vigente; excluir pré-cadastrado grava dismissed; pagamento duplicado rejeitado; saldo insuficiente + "Sim" divide o pagamento (caixinha zera, restante na conta), + "Não" não grava; caixinha nunca fica negativa; pagamento debita a caixinha na razão.
    - GUARDA ANTI-VALOR (G15): teste que carrega o JSON e FALHA se algum item tiver campo numérico além de `catalogVersion`, ou se `FixedCostTemplate` tiver propriedade `BigDecimal`/`Double`/`Int` de dinheiro; teste que valida que a contagem de itens do JSON é igual ao número de linhas da seção 7 do plano (leia `docs/plano-centro-de-custos.md` no teste e compare as chaves).

É PROIBIDO: Double/Float; qualquer valor monetário de custo no código ou no JSON; criar despesa ao pagar custo fixo; apagar fisicamente custo com versões/pagamentos; recriar sozinho um item que o usuário excluiu.

CRITÉRIOS DE ACEITE: lista pré-cadastrada aparece sem valores; usuário informa valores e exclui o que não tem; itens do catálogo podem ser adicionados depois; versões preservam o passado; quotas e vencimentos corretos nos testes; pagamento debita a caixinha; guarda anti-valor verde; testes verdes; PR aberto SEM merge.
````

---

### PROMPT F6 — Motor de reserva + Histórico

````markdown
# FASE 6 — Motor de reserva (2 modos), Lucro real no Histórico e "falta aportar"

PAPEL: Engenheiro Android Sênior (Kotlin/Compose). Execute você mesmo.
BRANCH: `feature/cc-f6-reserva-historico`, a partir da branch da F5.
PRÉ-REQUISITO: Fases 0 a 5 concluídas.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8 — regras R1 a R6, R11 e decisões D1 a D4, D15 e D20). Leia no código: data/repository/HistoricoRepository.kt, ui/screens/historico/HistoricoViewModel.kt (`loadData`, `rebuildGroupsAndMetrics`, `calculatePeriodMetrics`), ui/screens/historico/HistoricoScreen.kt, util/EarningsCalculator.kt, data/model/Transaction.kt, ui/common/period/PeriodFilter.kt (`resolveRange`), ui/screens/relatorios/RelatoriosViewModel.kt (~271, regra de km) e ui/screens/painel/PainelViewModel.kt (desgaste).
2. Crie util/CostCenterCalculator.kt (objeto puro, BigDecimal) com:
   - `kmForDay(routes, dailyTotals, date, zone): KmDay(km: BigDecimal, informed: Boolean)` — soma de distanceKm das rotas + totais diários do dia local (mesma regra do Relatórios); `informed = false` se NÃO existir nenhum registro de km no dia;
   - `wearCostPerKm(part, linkedExpense)` = linkedExpense.amount ÷ part.lifeKm (scale 6); ZERO sem despesa > 0 ou com lifeKm ≤ 0;
   - `wearProvisionForDay(...)` — só dias ≥ data local de part.lastChangeAt; o acumulado da peça nunca passa do valor da despesa; dia com `informed = false` → provisão ZERO e flag `kmNaoInformado`; peça que passou de lifeKm → flag `pecaVencida`;
   - `economicCostForDay(date, fixedCosts, versionsByCost, settings, parts, expensesById, routes, dailyTotals, zone): DayCost` — custos fixos NÃO analíticos (R1, sempre dias corridos) + desgaste (R2), respeitando `fixed_cost_provision_enabled` e `wear_provision_enabled`; informa também `analyticalInfo` (soma dos analíticos, só informativa);
   - `computeRealProfit(date, items): DayProfit` (ganho líquido via EarningsCalculator, custos variáveis = despesas do dia exceto `manutencao`, custo alocado, lucro real).
3. Crie util/ReserveSuggestionCalculator.kt (objeto puro) aplicando R5: modo CALENDAR (= custo alocado do dia) e modo WORKING_DAYS (equivalente mensal ÷ dias planejados do mês, só em dias planejados; `planned_weekdays` bit0 = segunda; `days_off` remove dias). ZERO dias planejados no mês → retorna `Pendente(motivo)` e NUNCA divide por zero. Agrupa por caixinha (custos com vault_id; desgaste → `maintenance_vault_id`; senão chave null = "Sem caixinha").
4. Fonte de dados: crie `CostCenterSnapshot` (custos, versões, settings, peças, despesas por id) e `CostCenterSnapshotProvider` (Hilt) que lê custos/versões/settings do ROOM e peças/despesas pelos repositórios legados, carregando em paralelo (async). NÃO altere a assinatura nem o retorno de `HistoricoRepository.loadHistoricoData`. O `HistoricoViewModel` recebe o provider por parâmetro OPCIONAL com default nulo (sem quebrar functional/Historico*Test.kt); a rota do Histórico em NavGraph obtém o provider por `EntryPointAccessors.fromApplication` e injeta na fábrica. Falha ao carregar o snapshot NÃO quebra o Histórico: snapshot vazio e log sem dado sensível.
5. Em HistoricoViewModel, para o período selecionado, calcule SOMENTE até min(fim do período, hoje) (D15) e exponha: `ganhoLiquidoPeriodo`, `custosVariaveisPeriodo`, `custosAlocadosPeriodo`, `reservaSugeridaPeriodo`, `reservaPorCaixinha`, `lucroRealPeriodo`, `diasSemKm` e `dayReserveByDate`. Totais de período: some em escala 6 e arredonde UMA vez (R11). `saldoPeriodo`, `entradasPeriodo` e `saidasPeriodo` continuam IDÊNTICOS aos de hoje (D1).
6. Em HistoricoScreen, extraia a UI nova para ui/screens/historico/components/ (arquivos novos) e acrescente em HistoricoScreen só a chamada: (a) abaixo do card de saldo, o card "LUCRO REAL (ESTIMADO)" com Ganho líquido − Custos variáveis − Custos alocados = Lucro real (negativo em RedAlert), rótulo "Saldo (caixa)" no card antigo para não confundir, e ícone "?" que abre sheet de MEMÓRIA DE CÁLCULO (cada rubrica, regra usada, dias sem km, peças vencidas, analíticos informativos e o aviso "valores estimados"); (b) tocar em "Reserva sugerida" expande a lista por caixinha com botão "Aportar" que navega para `centro_custos?tab=CONTAS&aporteVaultId=<id>&aporteAmount=<valor>`; (c) no cabeçalho de cada dia, "Separar: R$ X" a partir de `dayReserveByDate`, ou "km não informado" quando aplicável; (d) sem nenhum custo fixo COM valor nem peça com custo base, o card mostra reserva 0 e o botão "Cadastrar custos fixos" (Centro de Custos). NÃO altere PeriodSelector.
7. CONFIGURAÇÃO DA RESERVA (aba FIXOS, seção "Como sugerir reserva", arquivos novos): escolha Dias corridos × Dias trabalhados; chips dos dias da semana (padrão domingo a sábado, todos marcados); seletor de folgas (datas); texto explicando que o Lucro real sempre usa dias corridos. Grava em cost_center_settings via repositório.
8. `ReserveProgressRepository.computeProgress(): List<VaultReserveProgress>` (vaultId, due, deposited, missing = due − deposited) aplicando R6 com sugestões e aportes líquidos da razão (aportes manuais + aportes automáticos de baixa + transferências recebidas + estornos), respeitando `cost_center_started_on`. Mostre na aba Contas: "Reserva devida R$ X · Aportado R$ Y · Falta R$ Z" (Falta > 0 em YellowGold, ≤ 0 em GreenNeon), só em caixinhas que recebem reserva.
9. TESTES com os números do exemplo da seção 6 (data 2026-06-15, modo CALENDAR; esses valores existem SÓ nos testes): ganho R$ 200,00; combustível R$ 30,00; internet 120,00 mensal (4,000000); seguro 600,00 anual com start 2026-01-01 (1,643836); óleo R$ 60,00 / 3.000 km e pneu R$ 400,00 / 20.000 km, 100 km → 2,00 + 2,00. Esperado: custos alocados 9,643836 (R$ 9,64) e lucro real 160,356164 (R$ 160,36). Teste ainda: despesa `manutencao` de R$ 300,00 NÃO altera o lucro real mas ALTERA o saldo de caixa; custo SEM versão não entra; custo analítico não entra no lucro real nem na reserva; peça sem despesa vinculada não provisiona; peça trocada depois do dia não provisiona; teto da peça; dia sem km → provisão zero com `kmNaoInformado` (e não "calculado"); peça vencida sinalizada; caixinha de desgaste nula → "Sem caixinha"; período com dias futuros não provisiona o futuro; flags desligadas zeram a parcela; modo WORKING_DAYS: R$ 600,00 mensal com 25 dias planejados → 24,00 por dia planejado e 0 em folga; ZERO dias planejados → `Pendente` sem divisão por zero; ReserveProgress com due 120,00 e aportes 50,00 → missing 70,00; aporte +50 não altera lucro real; soma das quotas de fevereiro fecha o valor exato. Rode os testes existentes de Historico (functional/) e confirme que continuam verdes.

É PROIBIDO: mudar a fórmula do Saldo; provisionar dias futuros; usar Double/Float; fazer o Histórico depender do snapshot para renderizar; duplicar a regra de km do Relatórios (reutilize); contar custo SEM valor; mostrar "R$ 0,00 calculado" quando o km não foi informado.

CRITÉRIOS DE ACEITE: card Lucro real e memória de cálculo funcionando e reagindo ao filtro; "Separar" por dia; Aportar abre o sheet com o valor; modo dias trabalhados configurável; "Falta aportar" correto; testes antigos e novos verdes; PR aberto SEM merge.
````

---

### PROMPT F6B — Aporte automático na baixa da fatura, desfazer baixa e "Reserva a separar"

````markdown
# FASE 6B — Aporte automático na baixa da fatura da plataforma, desfazer baixa e tela "Reserva a separar"

PAPEL: Engenheiro Android Sênior (Kotlin/Compose). Execute você mesmo.
BRANCH: `feature/cc-f6b-baixa-aporte`, a partir da branch da F6.
PRÉ-REQUISITO: Fases 0 a 6 concluídas (`ReserveSuggestionCalculator` e a razão já existem).

CONTEXTO: a baixa de uma fatura de plataforma acontece em `FaturasViewModel.confirmPayment()` (botão "Liquidar / Baixar Repasse"): marca o ciclo como `pago` e grava `payment_received_date`. `BillingCycle` tem `periodStart`, `periodEnd` e `netTotalAmount` (só descontos e créditos das PLATAFORMAS). REGRAS DO PRODUTO: (1) o valor e os itens da fatura NUNCA são alterados por custo fixo ou reserva (a fatura é documento de conferência do que a empresa paga); (2) o aporte nas caixinhas nasce na baixa, nunca antes (D22); (3) desfazer baixa existe (D23); (4) o app registra o aporte mas não move dinheiro no banco (D25).

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 2, 4 a 8 — D10, D21 a D26, R6, R12 e R14) e, no código: ui/screens/faturas/FaturasViewModel.kt (`confirmPayment`, `openPayModal`, `closePayModal`, `closeCycleToPending`, `loadData`), ui/screens/faturas/FaturasScreen.kt (modal "Liquidar / Baixar Pagamento" ~552, aba "Pago", cartão da fatura ~760–830), data/repository/BillingCycleRepository.kt (`updateStatus`, `createBillingCycle`, `linkCycleTransactions`), a API e o DTO de billing_cycles, data/model/Models.kt (`BillingCycle`), util/EarningsCalculator.kt e ui/screens/relatorios/RelatoriosViewModel.kt. CONFIRME no código que rotas e totais diários têm `platformId` e permitem somar o ganho líquido por plataforma e por dia; se não tiverem, PARE e registre no relatório, sem inventar.
2. Crie util/ReserveAllocationCalculator.kt (objeto puro, BigDecimal) aplicando R12: `preview(cycle, platformNetByDay, allPlatformsNetByDay, reserveByDayAndVault, coveredDays, startedOn, today, netCap): List<VaultAllocation>` com, por caixinha, `suggested`, `amount`, `capApplied` e as linhas por dia (`day`, `platformId?`, `suggestedAmount` em escala 6). Regras: só dias com `startedOn ≤ dia ≤ min(periodEnd, hoje)`; proporção `ganhoDaPlataforma ÷ ganhoTotalDoDia`; dia sem nenhum ganho e ainda não coberto vai inteiro para a primeira baixa cujo `periodEnd ≥ dia`; dia/plataforma já coberto por alocação ATIVA não entra de novo; soma escala 6 e arredondamento UMA vez por caixinha; total limitado a `netTotalAmount` com redução proporcional entre caixinhas; sem caixinhas ou sem custos com valor → lista vazia.
3. Crie `ReserveAllocationService` (`@Singleton`, módulo novo) com:
   - `buildPreview(cycle)` — usa o `ReserveSuggestionCalculator` (respeita o modo CALENDAR/WORKING_DAYS) e o calculator acima;
   - `confirm(cycle, editedPreview)` — em UMA transação Room: grava `reserve_allocations` (ATIVA com amount > 0; DISPENSADA, amount 0, para caixinhas desmarcadas), as linhas de `reserve_allocation_days` e a linha `aporte` na razão (ref_type 'baixa_fatura', ref_id = id do ciclo, operation_id determinístico = UUID v5 de `cycleId|vaultId|v1`); o total editado não pode passar do líquido da fatura;
   - `undo(cycleId)` — estorna por referência as linhas do ciclo, marca as alocações DESFEITA com `undone_at` e libera os dias; se alguma caixinha ficar negativa, devolve o aviso (única exceção de D21);
   - `reconcile()` — vide item 6.
4. GANCHO NA BAIXA (G16, mudança mínima no legado): `FaturasViewModel` recebe `reservePort: ReserveAllocationPort? = null` (default nulo, sem quebrar os testes atuais; a rota de Faturas em NavGraph injeta o serviço por `EntryPointAccessors.fromApplication`). Então:
   - `openPayModal` carrega a prévia em segundo plano; falha ou porta nula → o modal funciona exatamente como hoje;
   - no modal de baixa (FaturasScreen ~552) acrescente SÓ a chamada de um composable novo `ReserveAllocationPreviewSection` (arquivo novo): lista de caixinhas com checkbox, valor editável (máscara de G3, `cleanCurrencyInput`/`parseCurrency`), total, aviso quando o teto do líquido foi aplicado e o texto fixo "O valor da fatura não muda."; por padrão tudo marcado;
   - em `confirmPayment`, após o sucesso remoto (inclusive o ramo de ciclos `dynamic_`/`unbilled_`, depois de `linkCycleTransactions`), chame `reservePort.confirm(...)` em try/catch; se falhar, mostre "Baixa registrada. O aporte automático será refeito em segundo plano." (o conciliador cuida);
   - após o aporte, mostre o lembrete (diálogo ou snackbar com ação) "Transfira no seu banco: R$ X para a conta <conta> / caixinha <caixinha>" listando cada caixinha com aporte.
5. DESFAZER BAIXA:
   - `BillingCycleRepository.undoPayment(cycleId): Boolean` NOVO — PATCH com corpo `JsonObject` serializado com `status = "a_vencer"` e `payment_received_date = null` EXPLÍCITO (Gson/`Map<String, Any>` omitem null; NÃO reaproveite `updateStatus`). Crie a função correspondente na API do ciclo (`@Body RequestBody`).
   - Em Faturas, aba "Pago": botão "Desfazer baixa" no cartão da fatura (composable em arquivo novo; mínimo em FaturasScreen) com confirmação "A fatura volta para A Vencer e os aportes de R$ X serão estornados das caixinhas. Continuar?". Ordem: remoto primeiro; sucesso → `reservePort.undo`; falha remota → nada muda. Exiba o aviso de saldo negativo se houver.
   - Faturas criadas dinamicamente na baixa também podem ser desfeitas (voltam para `a_vencer`).
6. Conciliador `ReserveAllocationReconcilerWorker` (`@HiltWorker`, periódico 1 h + ao abrir o app, exige sessão): (a) ciclos `pago` com `paymentReceivedDate ≥ cost_center_started_on` SEM nenhuma linha em reserve_allocations → cria a alocação padrão (sem prévia) pelo cálculo R12; (b) alocações ATIVA cujo ciclo já não está `pago` → estorna e marca DESFEITA. Idempotente.
7. Tela "Reserva a separar": rota `reserva_a_separar` em NavGraph e item no drawer (seção "Financeiro & Operacional") "Reserva a separar" — subtítulo "Quanto transferir para cada conta e caixinha" (apenas a linha do item em HomeScreen). Pacote ui/screens/costcenter/reserve/: ReserveToSeparateScreen, ReserveToSeparateViewModel (`@HiltViewModel`) e componentes. A ESTRUTURA é a do Histórico: reutilize `PeriodFilter`/`PeriodPreset`/`PeriodSelector` (ui.common.period) com os mesmos filtros; linha do tempo do mais recente para o mais antigo; um card por dia com uma linha por caixinha ("Seguro — Nubank: R$ 4,20") e subtotais por dia, semana e mês no mesmo estilo do HistoricoScreen (copie o padrão de agrupamento em arquivo novo; NÃO altere HistoricoScreen). Estados por linha: **Previsto** (dia/ganho sem fatura baixada), **Aportado** (chip com data da baixa e a fatura de origem) e **Pendente** (dias sem ganho aguardando a próxima baixa e valores acima do teto). No topo do período: Total previsto, Aportado e Pendente. Somente leitura: botão "Ver fatura" (abre Faturas) e "Aporte manual" (abre Contas & Caixinhas). Estados de carregando, vazio ("Nenhum custo com valor cadastrado" + botão "Cadastrar custos fixos") e erro.
8. TESTES: ReserveAllocationCalculator — dia 10 com R$ 150 na plataforma A e R$ 50 na B e reserva R$ 8 → baixa de A = 6,00 e baixa de B = 2,00; uma só plataforma → 100%; dia sem ganho entra na primeira baixa cujo fim ≥ dia e NÃO se repete na segunda; teto no líquido (reserva 30,00 e líquido 20,00 → total 20,00 proporcional entre caixinhas, resto pendente); dias anteriores a `cost_center_started_on` e dias futuros ignorados; modo WORKING_DAYS respeitado; valores editados; caixinha DISPENSADA não gera razão e seus dias continuam pendentes; sem caixinhas → nada. Service — confirm grava alocação + dias + razão na mesma transação; baixa repetida (mesma operation_id) não duplica; falha do gancho não quebra a baixa; undo estorna e libera os dias; undo com caixinha que já gastou o valor → saldo negativo com aviso; re-baixa após undo cria nova alocação; conciliador cria a alocação faltante e estorna a órfã. **A fatura permanece idêntica**: `netTotalAmount` e itens iguais antes e depois da baixa. Repositório — `undoPayment` envia `"payment_received_date":null` e `"status":"a_vencer"`. ViewModel da tela — agrupamento por dia/semana/mês, filtros de período e os três estados.

É PROIBIDO: alterar valores ou itens da fatura; Double/Float; aporte sem baixa; aporte de dias anteriores a `cost_center_started_on`; apagar linha da razão; mexer na semântica dos status de fatura além do retorno a `a_vencer` no desfazer; alterar HistoricoScreen.

CRITÉRIOS DE ACEITE: baixa gera aportes proporcionais com prévia editável; desfazer baixa restaura a fatura e estorna os aportes; tela Reserva a separar funcionando com os filtros do Histórico; conciliador comprovado nos testes; fatura inalterada; testes verdes; PR aberto SEM merge.
````

---

### PROMPT F7 — Cartões: fatura de crédito, extrato de débito e pagamento de fatura

````markdown
# FASE 7 — Cartões: faturas de crédito, extrato mensal de débito, vínculo de conta e pagamento de fatura

PAPEL: Engenheiro Android Sênior (Kotlin/Compose). Execute você mesmo.
BRANCH: `feature/cc-f7-cartoes-faturas`, a partir da branch da F6B.
PRÉ-REQUISITO: Fases 0 a 6B concluídas.

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8 — R7, R8, R13 e decisões D4, D5, D11). Leia no código: ui/screens/expenses/CardPaymentModal.kt (`computedFirstInstallmentMonth`), ui/screens/cards/CreditCardsScreen.kt, ui/screens/cards/CreditCardsViewModel.kt, ui/screens/cards/CardManagementScreen.kt, data/repository/CreditCardRepository.kt, data/model/Models.kt (`CreditCard`, `Expense`). CONFIRME que `FaturasScreen` é contas a RECEBER de plataformas: NÃO a use para faturas de cartão.
2. Crie util/CardInvoiceCalculator.kt (objeto puro, BigDecimal), com data classes CardInvoice, InvoiceItem, DebitStatement e enum InvoiceStatus (ABERTA, FECHADA, VENCIDA, PAGA):
   - `invoiceMonthFor(purchaseDate, closingDay): YearMonth` — `purchaseDay < min(closingDay, tamanho do mês) ? mês da compra : mês seguinte`;
   - `closingDate(refMonth, closingDay)` e `dueDate(refMonth, closingDay, dueDay)` limitados ao tamanho do mês;
   - `installmentSchedule(expense)` — N = installmentTotal ?: 1; parcela = total ÷ N (scale 2, HALF_UP); a última absorve a diferença; parcela k em +(k−1) meses;
   - `buildInvoices(card, expenses, payments, today)` — só cartões `credito` e `multiplo`; pagamentos somados por (cardId, referenceMonth), ignorando excluídos logicamente;
   - `buildDebitStatements(card, expenses, settlements)` — só cartões `debito` (D11): um extrato por MÊS DE CALENDÁRIO da compra, com total do mês e a origem (conta/caixinha) de cada gasto; sem fechamento, vencimento, status de pagamento nem lembrete. `voucher` e despesas de PIX/dinheiro são ignorados.
3. Refatore CardPaymentModal para usar `invoiceMonthFor` ao calcular `computedFirstInstallmentMonth`, com o MESMO resultado (teste de regressão).
4. `CardInvoiceRepository` (Hilt): carrega cartões (`CreditCardRepository`), despesas com cartão (`getExpenses(userIdFilter, filters = mapOf("card_id" to "not.is.null"))`), pagamentos e settlements do ROOM, e devolve faturas de crédito e extratos de débito. Não cria nem altera despesas.
5. VÍNCULO DE CONTA DO CARTÃO DE DÉBITO (D11): `CardAccountLinkRepository` (F2) e tela/sheet "Conta bancária do cartão" na aba Cartões (arquivo novo): cartão `debito` sem vínculo aparece com o aviso "Informe a conta bancária deste cartão" e o extrato fica bloqueado até vincular. NÃO altere a tabela nem o cadastro `credit_cards`.
6. Aba CARTOES no hub (acrescente `CARTOES` a CentroCustosTab; arquivos novos: CardsOverviewTab.kt, CardsOverviewViewModel.kt `@HiltViewModel`): "A pagar nos cartões neste mês" (só crédito); lista dos cartões (apelido, •••• últimos 4, bandeira/emissor): crédito mostra fatura aberta com fechamento e vencimento e faturas fechadas a pagar com chip de status; débito mostra a conta vinculada e o total gasto no mês com o botão "Ver extrato". Botão "Gerenciar cadastro de cartões" abre a tela existente. Em CreditCardsScreen acrescente apenas "Ver fatura" (crédito) ou "Ver extrato" (débito) por cartão.
7. Tela do cartão: rota `card_detail/{cardId}` em NavGraph; CardDetailScreen.kt e CardDetailViewModel.kt novos. Crédito: seções "A pagar", "Fatura aberta", "Próximas" e "Pagas", cada fatura expande os lançamentos (data, título, parcela "2/5", valor) com subtotal. Débito: lista de meses (mais recente primeiro) com os lançamentos, a origem de cada um e o total do mês; SEM "Pagar fatura". TODO gasto no cartão — Manutenção, Combustível, Alimentação — aparece automaticamente (derivado das despesas).
8. "Pagar fatura" (só crédito): sheet com valor (padrão = saldo restante; editável para pagamento parcial; nunca acima do restante), data e origem via PaymentSourceSheet com o MESMO fluxo de pagamento dividido da F4 (R13: caixinha até o saldo + conta para o restante; "Não, voltar" retorna). Em UMA transação Room grave `card_invoice_payments` (com `vault_amount`/`account_amount`) + linha `pagamento_fatura` só da parte da caixinha na razão (ref_type 'fatura', ref_id = cardId|AAAA-MM), com operation_id (toque duplo não paga duas vezes). NUNCA crie Expense (D4). Fatura totalmente paga vira PAGA. Pagar fatura NÃO altera o Lucro real.
9. COMPRA NO DÉBITO (Manutenção): quando o cartão escolhido em Lançar Manutenção for `debito`, abra o PaymentSourceSheet com a CONTA já preenchida pelo vínculo do cartão (travada); se o cartão ainda não tem vínculo, o sheet pergunta a conta e oferece "Vincular este cartão a esta conta" (marcado por padrão). A caixinha é opcional e vale o pagamento dividido (R13). Reaproveite a `MaintenancePaymentSaga` da F4 com `kind = 'debito'`. O fluxo de crédito permanece como está.
10. Testes de CardInvoiceCalculator: fecha dia 3, vence dia 10 → compra em 2026-11-02 cai na fatura 11/2026 (fecha 03/11, vence 10/11); compra em 2026-11-03 cai em 12/2026 (fecha 03/12, vence 10/12); fecha dia 25, vence dia 5 → compra 2026-11-10 cai em 11/2026 (fecha 25/11, vence 05/12); compra 2026-11-25 cai em 12/2026 (fecha 25/12, vence 05/01/2027); R$ 100,00 em 3x em 2026-11-16 com fechamento dia 3 → 33,33, 33,33 e 33,34 nas faturas 12/2026, 01/2027 e 02/2027; **R$ 100,01 em 3x → 33,34 + 33,34 + 33,33** (soma 100,01); closingDay 31 em fevereiro de 2027 → 28; compra EXATAMENTE no dia do fechamento e em mês curto sem datas inválidas; status ABERTA/FECHADA/VENCIDA/PAGA; pagamento parcial mantém FECHADA/VENCIDA com restante correto; PIX/dinheiro ignorado. **Débito:** gera extrato por mês de calendário, sem status nem pagamento; gastos de dois meses ficam em extratos separados; cartão débito sem vínculo bloqueia o extrato e pede a conta; compra no débito com conta vinculada abre o sheet com a conta travada; "Vincular este cartão" grava o vínculo; compra no débito com caixinha sem saldo suficiente divide (caixinha zera, restante na conta). ViewModel — pagar fatura integral e parcial debita a caixinha pelo R13, não cria despesa, reenvio com o mesmo operation_id não duplica, pagamento acima do restante rejeitado. Regressão do CardPaymentModal.

É PROIBIDO: materializar parcelas como novas despesas; contar pagamento de fatura como despesa no Histórico; criar fatura, vencimento ou lembrete para cartão de débito; alterar `credit_cards` ou o cadastro de cartões além de "Ver fatura/extrato"; Double/Float.

CRITÉRIOS DE ACEITE: aba Cartões e tela do cartão funcionando para crédito (fatura) e débito (extrato mensal com conta vinculada); gastos de Manutenção/Combustível/Alimentação aparecem no lugar certo; pagamento (total e parcial) debita a caixinha pelo R13; testes verdes; PR aberto SEM merge.
````

---

### PROMPT F8 — Notificações de fatura

````markdown
# FASE 8 — Notificações de fatura de cartão (WorkManager + Hilt)

PAPEL: Engenheiro Android Sênior (Kotlin). Execute você mesmo.
BRANCH: `feature/cc-f8-notificacoes`, a partir da branch da F7.
PRÉ-REQUISITO: Fase 7 concluída (WorkManager e Hilt já existem desde a F0 — NÃO adicione dependências).

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8 — D11 e D12). Leia: MainActivity.kt, AndroidManifest.xml, LogisticPrimeApplication.kt, o `SyncWorker` da F0 (modelo de `@HiltWorker`), util/CardInvoiceCalculator.kt e CostCenterSettingsRepository.
2. AndroidManifest.xml: acrescente `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
3. Room: suba o banco para a próxima versão com `Migration` TESTADA adicionando a tabela local `notified_events` (chave `cardId|AAAA-MM|EVENTO`, notified_at), que NÃO é sincronizada.
4. Pacote notification/:
   - `CardInvoiceReminderPlanner` (objeto puro): `plan(today, invoices, notifiedKeys, offsets): List<ReminderEvent>`. Eventos: FECHAMENTO (janela [fechamento, vencimento]) e `D_MENOS_n` para cada n em `reminder_offsets` do usuário (padrão [3, 0]; permitidos 7, 3, 1, 0), com janela [vencimento−n, vencimento] (n = 0: [vencimento, vencimento+3]). Só faturas com restante > 0 e cartão credito/multiplo. Idempotência por chave; a janela recupera o aparelho desligado no dia exato.
   - `InvoiceNotifier`: cria o canal "faturas_cartao" (IMPORTANCE_DEFAULT) na primeira notificação e publica: FECHAMENTO → "Fatura do cartão <apelido> fechou: R$ <restante>. Vence em <dd/MM>."; D_MENOS_n (n > 0) → "Fatura do <apelido> vence em <n> dia(s): R$ <restante>."; D_MENOS_0 → "Fatura do <apelido> vence hoje: R$ <restante>.". Nada de dado sensível além de apelido e valor.
   - `CardInvoiceReminderWorker` (`@HiltWorker`): aguarde a sessão do Supabase Auth; sem sessão → `Result.success()` sem notificar; carregue faturas via CardInvoiceRepository, execute o planner, notifique, grave as chaves em `notified_events` e descarte as com mais de 60 dias.
   - Agendamento (`ReminderScheduler`, chamado no `LogisticPrimeApplication`): `enqueueUniquePeriodicWork("card_invoice_reminders", ExistingPeriodicWorkPolicy.KEEP, ...)` a cada 24 h, com atraso inicial até o próximo 08:00 local e NetworkType.CONNECTED. `reminder_enabled` desligado → cancele.
5. Permissão e fallback: na aba Cartões, com SDK ≥ 33 e permissão ausente, mostre o banner "Ativar lembretes de fatura" e peça POST_NOTIFICATIONS com `rememberLauncherForActivityResult(RequestPermission())`. Negada: o app funciona sem insistir, mostra "Lembretes desativados no sistema" **e exibe dentro do app a lista de faturas que fecham ou vencem em até 7 dias** (alerta interno, sem depender de notificação). Acrescente na aba um Switch "Lembretes de fatura" e chips para ligar os avisos de 7 e 1 dia antes.
6. Toque na notificação: PendingIntent (FLAG_IMMUTABLE) para MainActivity com extra `open_route=card_detail/<cardId>`. Se tratar isso exigir mudança invasiva no login/biometria do NavGraph, NÃO implemente: abra só o app e registre a pendência.
7. Testes do planner: no dia do fechamento gera FECHAMENTO; no dia seguinte sem notificação anterior ainda gera; fatura PAGA ou restante 0 não gera; chave já notificada não repete; vencimento−3 gera D_MENOS_3; com offsets [7,3,1,0] gera cada evento na data certa; vencimento gera D_MENOS_0; 4 dias após o vencimento nada; cartão débito não gera; dois cartões geram eventos independentes. Worker: sem sessão não notifica; sem permissão não quebra e o alerta interno aparece. Migração do Room testada.

É PROIBIDO: AlarmManager exato; pedir permissão de alarme exato; notificar sem sessão; dado sensível no texto; nova dependência; confundir com a tabela `notifications` de recebíveis.

CRITÉRIOS DE ACEITE: compila e testes verdes; worker agendado uma única vez (KEEP); canal criado; banner correto em API 33+; alerta interno funciona sem permissão; PR aberto SEM merge.
````

---

### PROMPT F9 — Extensões (opcional, recomendado)

````markdown
# FASE 9 (OPCIONAL) — PIX em Combustível/Alimentação, aba Resumo e chip na Home

PAPEL: Engenheiro Android Sênior (Kotlin/Compose). Execute você mesmo.
BRANCH: `feature/cc-f9-extensoes`, a partir da branch da F8 (ou F7, se F8 não existir).
PRÉ-REQUISITO: Fases 0 a 7 concluídas (F8 recomendada).

COMANDOS:
1. Leia docs/plano-centro-de-custos.md (seções 4 a 8 — D7 a D9 e D14). Leia ui/screens/expenses/FuelExpenseViewModel.kt, FuelExpenseScreen.kt, MealExpenseViewModel.kt, MealExpenseScreen.kt e a `MaintenancePaymentSaga` da F4.
2. Estenda a origem do PIX para Combustível e Alimentação reutilizando EXATAMENTE PaymentSourceSheet, PaymentSource, PaymentSourceValidator, InsufficientBalanceDialog e a saga da F4 (generalize a saga para qualquer despesa; não duplique). Nessas telas o `paymentMethod` padrão já é "pix": NÃO abra o sheet ao entrar; abra ao tocar em PIX e, ao salvar com PIX, valor > 0 e 1+ conta ativa sem origem confirmada, abra o sheet nesse momento. Sem conta ativa, siga como hoje. Aplique as mesmas regras R10 (edição, estorno, troca de forma de pagamento) e a revalidação ao salvar. Preserve o campo "card" legado. Aplique o mesmo pagamento dividido (R13) e estenda o cartão de DÉBITO a essas telas (conta pelo vínculo do cartão, D11), reutilizando a saga da F4 com `kind = 'debito'`.
3. Aba RESUMO (primeira aba do hub; acrescente `RESUMO` a CentroCustosTab): total nas caixinhas; faturas a pagar nos próximos 7 dias; custos fixos com vencimento nos próximos 7 dias; itens aguardando valor; alertas (caixinha negativa, "falta aportar" > 0, fatura vencida, falha de sincronização), cada um levando à aba correspondente.
4. Em HomeViewModel/HomeScreen, chip "Reserva de hoje: R$ X" no card de lucro, calculado com `ReserveSuggestionCalculator` (mesma regra da F6, só até hoje). Falha no cálculo oculta o chip sem afetar a Home. Em HomeScreen, apenas a chamada de um composable novo em arquivo novo.
5. Testes: Fuel e Meal — PIX sem origem bloqueia quando há conta ativa; saldo insuficiente + "Sim"/"Não"; sem conta ativa salva como hoje; PIX → Cartão estorna; edição aplica ajuste. Resumo: alertas montados a partir de dados de exemplo. Home: chip some quando não há custo com valor nem peça com custo base.

É PROIBIDO: duplicar componentes da F4; alterar o fluxo de Cartão; mudar a semântica do lucro já exibido na Home.

CRITÉRIOS DE ACEITE: PIX de Combustível e Alimentação debita a caixinha; aba Resumo mostra os alertas; chip aparece e some corretamente; testes verdes; PR aberto SEM merge.
````

---

### PROMPT F10 — Consolidação, auditoria e relatório final

````markdown
# FASE 10 — Consolidação, auditoria e relatório final

PAPEL: Engenheiro de QA/Auditoria Sênior (Android + PostgreSQL). Execute você mesmo. Você NÃO é o autor das fases anteriores: audite, não presuma.
BRANCH: `feature/cc-f10-auditoria`, a partir da última branch de fase.
PRÉ-REQUISITO: Fases 0 a 8 concluídas (F9 se existir).

COMANDOS:
1. Leia docs/plano-centro-de-custos.md inteiro e os relatórios de cada fase. Rode `./gradlew testDebugUnitTest testReleaseUnitTest assembleDebug` e inspecione os workflows do GitHub Actions. NÃO invente teste executado.
2. Monte a MATRIZ requisito → arquivo → teste → resultado para TODOS os itens abaixo. Para cada um, aponte o teste existente; se não existir, ESCREVA o teste:
   - Sem custos cadastrados: Histórico e Home iguais à baseline, indicador "estimado" sinalizado.
   - Lista pré-cadastrada sem valores; itens sem valor fora dos cálculos; usuário informa valores; exclusão de pré-cadastrado não volta; readicionar pelo catálogo; **guarda anti-valor (G15) verde**; nenhuma constante monetária de custo em `app/src/main` (busque por literais em código de custo).
   - R$ 120/mês em fevereiro (28 e 29 dias) soma exata R$ 120; IPVA anual rateado em 365/366 sem cobrar de novo no pagamento; seguro mensal em dias sem entrega continua provisionando.
   - Peça R$ 300 / 10.000 km com 100 km → R$ 3,00; dia sem km → "km não informado".
   - Abastecimento de R$ 30 pago em qualquer forma → uma única despesa.
   - Aporte de R$ 50 → saldo +50, lucro inalterado.
   - PIX R$ 100 com caixinha de R$ 60 → aviso; "Não" não grava; "Sim" abre a conta do restante: razão −60, R$ 40 na conta, caixinha em ZERO (nunca negativa); valor da caixinha editável.
   - Queda de rede / reenvio com a mesma operation_id → sem duplicar despesa, débito ou parcela.
   - Falha ao salvar a peça → sem despesa órfã nem débito órfão (conciliador).
   - Editar PIX 100 → 80 → ajuste de +20; PIX → Cartão → estorno, sem dois débitos; excluir manutenção → estorno.
   - Cartão de débito gera extrato mensal (sem fatura, vencimento nem lembrete) e exige conta vinculada; 3× R$ 100,01 soma exata; compra no dia do fechamento e dia 31 em mês curto; pagar fatura total/parcial sem segunda despesa e sem alterar Lucro real.
   - Dois usuários: acesso cruzado negado (RLS e FK composta); troca de conta limpa o Room local.
   - Offline: escrever sem rede, reabrir o app, reconectar e sincronizar sem perda nem duplicação; `outbox` com falha permanente visível ao usuário.
   - Sem permissão de notificação: app funciona e mostra alerta interno.
   - Retirada, transferência e ajuste acima do saldo bloqueados com a mensagem do saldo máximo; só estorno deixa saldo negativo, com aviso.
   - Pagamento dividido em Manutenção, custo fixo, fatura de cartão e compra no débito.
   - Baixa de fatura da plataforma: aportes proporcionais por plataforma (R$ 150/R$ 50, reserva R$ 8 → 6,00 e 2,00); dia sem ganho entra na primeira baixa e não se repete; teto no líquido recebido; prévia editável; caixinha desmarcada fica DISPENSADA; baixas e dias anteriores a `cost_center_started_on` ignorados.
   - **Fatura da plataforma inalterada** (valor e itens iguais antes e depois da baixa).
   - Desfazer baixa: fatura volta para A Vencer com data de recebimento nula, aportes estornados, dias liberados; re-baixa cria nova alocação; baixa repetida e app fechado entre baixa e aporte não duplicam nem perdem (conciliador).
   - Tela "Reserva a separar": mesmos filtros do Histórico, subtotais por dia/semana/mês e estados Previsto/Aportado/Pendente.
   - Modo "dias trabalhados" começa com domingo a sábado.
3. SEGURANÇA: revise RLS e GRANTs de todas as tabelas novas; confirme que `vault_ledger` não aceita UPDATE/DELETE; confirme ausência de `service_role`, de credenciais e de dados bancários em logs; confirme que `expenses`, `credit_cards` e `part_maintenance` não foram alteradas no banco.
4. REGRESSÃO MANUAL documentada (roteiro + resultado): Lançar Manutenção (PIX, Cartão, Dinheiro), Combustível, Alimentação, Histórico (filtros de período, saldo idêntico), Home, Cartões (crédito e débito), Faturas (baixa e desfazer baixa), Reserva a separar, Contas & Caixinhas, Custos Fixos.
5. Gere docs/financeiro/05-relatorio-auditoria-final.md com: matriz, lista de migrações (SQL e Room), versão das dependências, pendências e o status final **APROVADO PARA REVISÃO DO PROPRIETÁRIO** somente se 100% dos itens verificáveis passarem; caso contrário **NÃO APROVADO**, com bloqueios explícitos. Itens não verificáveis sem o app em execução (capturas de tela, testes instrumentados) vão para a lista "verificação manual pendente".

É PROIBIDO: declarar aprovação sem evidência; corrigir bug fora do escopo sem registrar; merge, push em `main` ou `--force`.

CRITÉRIOS DE ACEITE: matriz completa; testes verdes; relatório final entregue; PR aberto SEM merge.
````

---

## 11. Riscos e pontos de atenção

1. **Migração em produção.** O banco tem dados reais (ex.: 74 despesas, 127 rotas no último inventário do repositório). A migração só cria tabelas novas e não toca em `expenses`; mesmo assim, faça backup/branch no Supabase antes de executar a F1.
2. **RLS é a única barreira.** O app usa chave anon direto no PostgREST; por isso a F1 exige policy em toda tabela, FK composta e teste de isolamento entre dois usuários.
3. **F0 é a fase de maior risco técnico.** Hilt + KSP + Room + WorkManager em um projeto sem nenhum deles pode esbarrar em incompatibilidade de versões (Kotlin 2.1.0/AGP 8.8.2). O prompt manda escolher versões estáveis compatíveis e registrar. Se a build quebrar, resolva na F0 antes de seguir.
4. **Duas fontes de dados.** Módulo novo = Room (offline-first); legado = online. A saga da F4 existe para cobrir a fronteira entre eles. Despesa legada criada **offline** continua não sendo possível (como hoje); o app não promete offline para o legado.
5. **Saga não é transação.** Entre o passo 1 (razão local) e o passo 2 (despesa remota) pode haver janela de inconsistência; ela é fechada pelo conciliador (até ~1 h de atraso). Se quiser atomicidade real no servidor, a alternativa é uma RPC PostgreSQL, ao custo de duplicar no SQL o payload de `expenses`/`part_maintenance`.
6. **Valores do usuário, risco de esquecimento.** Itens pré-cadastrados sem valor ficam fora dos cálculos; se o usuário não preencher, o Lucro real não desconta nada. Por isso a aba Fixos e o Resumo avisam "N itens aguardando seu valor".
7. **Versões de custo.** Trocar o valor no meio de um ciclo muda a quota só a partir da data de vigência; a soma exata do ciclo (resíduo no último dia) só vale quando não há troca de versão dentro dele.
8. **Fatura é derivada.** Mudar `closingDay`/`dueDay` recalcula faturas abertas e futuras; pagamentos ficam presos ao mês de referência. Editar ou excluir despesa ajusta a fatura sozinho.
9. **Base de custo do desgaste.** O custo/km usa a despesa **atualmente** vinculada à peça (o registro é reescrito a cada troca). Peça sem despesa de valor > 0 não provisiona e o card deve deixar isso visível.
10. **Km do dia.** Segue a soma rotas + totais diários do Relatórios; se um dia tiver os dois lançados, o km pode contar em dobro (limitação já existente).
11. **Dados bancários.** Só últimos dígitos, mascarados e fora dos logs.
12. **Notificações não são exatas.** WorkManager respeita Doze e economia de bateria; a janela do planner reduz perda, mas não elimina atraso. O alerta interno cobre o caso sem permissão.
13. **Sessão em segundo plano.** `SupabaseClient` cai para `MemorySessionManager` se o armazenamento persistente falhar; nesse caso workers não veem sessão e não sincronizam nem notificam.
14. **Arquivos grandes.** `HomeScreen`, `HistoricoScreen` e `FaturasScreen` têm milhares de linhas; o plano manda criar arquivos novos e tocar o mínimo.
15. **Regra "always on" do repositório.** `.agents/rules/equipe-agentes.md` descreve um Orquestrador com saída em JSON. A regra G2 neutraliza isso; se o Antigravity responder em JSON de tarefas, reenvie o prompt reforçando G2.
16. **Workflow `/commit` vs. PR.** O workflow manda sincronizar com o remoto. A regra G12 troca isso por branch + PR sem merge. Se o Antigravity empurrar para `main`, interrompa e reverta pelo histórico.
17. **PRs encadeados.** Cada fase parte da branch anterior; faça o merge **em ordem**. Alterar uma fase já aprovada exige atualizar as seguintes.

18. **Aporte automático não é transferência real.** O app registra o aporte na baixa, mas quem move o dinheiro no banco é o usuário. Se ele esquecer, o saldo do app fica maior que o real. Mitigação: lembrete após a baixa e "Ajustar saldo".
19. **A divisão proporcional depende do ganho por plataforma e por dia.** Se rotas ou totais diários não identificarem a plataforma, a F6B manda parar e registrar, sem inventar a proporção.
20. **Edição posterior de fatura já baixada** (rotas, ajustes) não recalcula os aportes. O caminho é desfazer a baixa e baixar de novo.
21. **Desfazer baixa pode deixar a caixinha negativa** se o dinheiro já foi gasto. É a única exceção à regra de saldo não negativo e vem com aviso.
22. **Cartão de débito sem conta vinculada** (inclusive os já cadastrados) fica com o extrato bloqueado até o usuário informar a conta.
23. **Pagamento dividido não valida o saldo real da conta.** O app confia no que o usuário informa; o valor da conta é só registro de origem.

---

## 12. Fora de escopo (sugestões futuras)

- Migrar o legado (despesas, rotas, peças) para Room/Hilt e offline-first completo (ADR-001).
- Financiamento com juros separados do principal; custo de oportunidade.
- Divisão de um pagamento em mais de duas origens (hoje: caixinha + conta).
- Categorias dedicadas para pedágio, estacionamento avulso, lavagem e multas.
- Lembretes de vencimento de custo fixo (reaproveitando o planner da F8).
- Integração bancária (Open Finance) para conciliar aportes automaticamente.
- Exportação do extrato das caixinhas e da fatura.
- Materializar `card_statements`/`card_installments` caso o desempenho exija.
