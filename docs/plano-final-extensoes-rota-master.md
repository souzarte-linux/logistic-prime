# 📋 Plano de Implementação Final — Extensões da Rota Master (Bipagem, Plataforma, Handoff Financeiro e Atribuição Cruzada)

> **Projeto:** Central do Motorista (logistic-prime)
> **Base:** repositório auditado diretamente (não é greenfield — a funcionalidade "Rota" do Master já existe parcialmente implementada)
> **Status:** Todas as decisões de escopo confirmadas com o usuário nesta conversa

---

## 0. O que já existe no repositório (levantado por auditoria direta, não por suposição)

Antes de qualquer prompt, é essencial que os subagentes saibam que **não estão partindo do zero**:

| Arquivo | O que já faz |
|---|---|
| `data/model/MasterRouteModels.kt` | `MasterDeliveryRoute` (cabeçalho) e `MasterRouteStop` (parada) — **sem** `platformId`/`packageType`/`photoUrl`/`assignedPartnerId` por parada ainda |
| `data/repository/MasterRouteRepository.kt` | `createRoute`, `addStop`, `addStopsBatch`, `getRouteStops`, `updateStopStatus`, `finishRoute` |
| `data/remote/dto/MasterRouteDto.kt` + `data/remote/api/MasterRouteApi.kt` | Camada de rede/Supabase para a Rota Master |
| `ui/screens/routes/master/RouteHomeScreen.kt` + `RouteHomeViewModel.kt` | Hub de rotas: cria rota nova (**plataforma escolhida uma vez, no cabeçalho**), lista rotas recentes |
| `ui/screens/routes/master/RouteScannerScreen.kt` + `RouteScannerViewModel.kt` | Bipagem com barcode+OCR (`onPackageScanned(context, barcode, parsedAddress)`), beep/vibração, detecção de duplicata, **auto-avanço sempre dispara após 2s, sem checagem de confiança do parsing** |
| `ui/screens/routes/master/RouteCockpitScreen.kt` + `RouteCockpitViewModel.kt` + `components/StopDeliveryCard.kt` | Lista de paradas (cards de layout fixo, **sem expandir/contrair**), navegação via `NavigationIntentHelper.launchNavigation(address, preference=ALWAYS_ASK)` (parada-a-parada, já correto), `finishRoute(...)` |
| `ui/screens/routes/NewRouteViewModel.kt` (`applyMasterRouteHandOff`) + `navigation/NavGraph.kt` | Handoff **já existente**: ao concluir a rota, navega para "Lançar Ganhos por Rota" pré-preenchendo `platformId`, `deliveredPackages` (agregado), `origin`, `destination`, `masterRouteId`. **Não** passa split pacotinho/volumoso, data, horários nem KM |
| `ui/screens/relatorios/RelatoriosViewModel.kt` | **Confirmado por leitura direta**: soma `distanceKm` de todas as rotas para o total geral (linha ~253) e agrupa `distanceKm`/tempo por `platformId` no recurso "Rentabilidade por Plataforma" (linhas ~538-566). Isso significa que duplicar KM/tempo entre rotas financeiras sintéticas quebra esse relatório — daí a regra de rateio da Seção 3 |
| Geocodificação (ORS), mapa embutido (osmdroid), ordenação de paradas | **Não existem ainda** — precisam ser criados do zero (Seção 6) |

---

## 1. Decisões de escopo confirmadas nesta conversa

1. **Handoff financeiro mantém a confirmação do usuário** (padrão atual: pré-preenche e navega para "Lançar Ganhos por Rota", usuário revisa e salva — não vira um salvamento 100% silencioso).
2. **Split de Pacotinhos e Volumosos**: nova propriedade `packageType` por parada (`PACOTINHO` | `VOLUMOSO`, padrão `PACOTINHO`), contagens separadas no handoff.
3. **Plataforma por bipagem com seleção "sticky"**: campo de plataforma na tela de bipagem, mantendo a última selecionada; só muda quando o usuário tocar em outra. Indicador **grande e persistente** no topo da tela do scanner (não um dropdown discreto) para reduzir o risco de esquecimento. A mesma rota física pode ter paradas de plataformas diferentes.
   - **Na conclusão da rota ("Lançar Ganhos por Rota")**: gerar **um registro financeiro por plataforma** presente nas paradas daquele dia (não um registro único).
   - **Regra de rateio de KM e tempo trabalhado** (para não duplicar valores no Relatório — ver Seção 3): proporcional à quantidade de pacotes de cada plataforma dentro da rota física.
4. **Atribuição cruzada Master ↔ Parceiro** (já detalhada em rodada anterior, agora referenciando o modelo real): dois estágios — intenção (`assigned_partner_id` setado, status `atribuido_pendente`) e confirmação física (bipagem duplicada na sessão do parceiro, status `confirmado`), com ícone de origem ao fechar a sessão do parceiro.
5. **Cards expansíveis/contraíveis** no Cockpit: contraídos por padrão (nome completo + código visíveis), botão "Expandir todos"/"Contrair todos" no topo, contador de pacotes bipados no cabeçalho da tela.
6. **Foto de backup da etiqueta**: opcional na bipagem, com **política crítica de expiração** — apagar a foto após a rota estar concluída há mais de 15 dias (ou config equivalente), para não estourar o 1 GB gratuito do Supabase Storage.

---

## 2. Modelagem de dados — extensões (não recriação)

### 2.1 `master_route_stops` — novas colunas
```sql
alter table master_route_stops
  add column platform_id uuid references platforms(id),
  add column package_type text not null default 'pacotinho', -- 'pacotinho' | 'volumoso'
  add column photo_url text,
  add column photo_expires_at timestamptz, -- calculado no momento da conclusão da rota (finished_at + 15 dias)
  add column assigned_partner_id uuid references delivery_partners(id),
  add column transfer_status text, -- null | 'atribuido_pendente' | 'confirmado'
  add column transferred_via text, -- 'manual_master' | 'scan_parceiro'
  add column transferred_at timestamptz;

create index idx_master_route_stops_barcode_active
  on master_route_stops (barcode)
  where transfer_status is distinct from 'confirmado' and status <> 'devolvido';
```

- `platform_id` da parada é **independente** do `platform_id` do cabeçalho da rota (`master_delivery_routes.platform_id` continua existindo, mas passa a ser só um "padrão inicial" sugerido na criação da rota — a fonte de verdade pro handoff financeiro é o `platform_id` de cada parada).
- **Lista ativa do Master** = `WHERE assigned_partner_id IS NULL OR transfer_status = 'atribuido_pendente'`; assim que `transfer_status = 'confirmado'`, some da navegação ativa do Master.

### 2.2 Atualizar `data/model/MasterRouteModels.kt`
```kotlin
enum class PackageType(val value: String) {
    PACOTINHO("pacotinho"),
    VOLUMOSO("volumoso");

    companion object {
        fun fromValue(value: String?): PackageType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PACOTINHO
    }
}

enum class TransferStatus(val value: String) {
    ATRIBUIDO_PENDENTE("atribuido_pendente"),
    CONFIRMADO("confirmado");

    companion object {
        fun fromValue(value: String?): TransferStatus? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

// MasterRouteStop ganha:
//   val platformId: String? = null,
//   val packageType: PackageType = PackageType.PACOTINHO,
//   val photoUrl: String? = null,
//   val photoExpiresAt: OffsetDateTime? = null,
//   val assignedPartnerId: String? = null,
//   val transferStatus: TransferStatus? = null,
//   val transferredVia: String? = null,
//   val transferredAt: OffsetDateTime? = null
```
Atualizar `MasterRouteStopDto` (`data/remote/dto/MasterRouteDto.kt`) e as funções `toDomain()`/`toDto()` correspondentes com os mesmos campos (`@SerializedName` em snake_case).

### 2.3 `partner_session_packages` (pré-requisito ainda pendente de auditoria)
> ⚠️ Mantém-se a mesma ressalva de antes: confirmar primeiro se `delivery_partner_sessions` já tem granularidade por pacote. Se não tiver, criar esta tabela é bloqueante para a Seção 5.
```sql
create table partner_session_packages (
  id uuid primary key default gen_random_uuid(),
  session_id uuid not null references delivery_partner_sessions(id) on delete cascade,
  barcode text not null,
  origin text not null default 'novo', -- 'novo' | 'importado_master'
  master_stop_id uuid references master_route_stops(id),
  scanned_at timestamptz not null default now()
);
```

---

## 3. Regra de rateio de KM e tempo trabalhado por plataforma (crítico)

Ao concluir a rota física (`finishRoute` em `RouteCockpitViewModel.kt`), antes de disparar o handoff:

1. Agrupar `stops` por `platformId` (ignorando as que já foram transferidas para parceiro — essas não entram no cálculo do Master).
2. Para cada grupo de plataforma:
   ```kotlin
   val totalPackagesInPhysicalRoute = allMasterStops.size
   val packagesThisPlatform = stopsForThisPlatform.size
   val shareRatio = packagesThisPlatform.toBigDecimal()
       .divide(totalPackagesInPhysicalRoute.toBigDecimal(), 6, RoundingMode.HALF_UP)

   val proratedKm = totalRouteDistanceKm.multiply(shareRatio)
       .setScale(1, RoundingMode.HALF_UP)
   val proratedWorkedMinutes = (totalRouteWorkedMinutes * shareRatio.toDouble()).roundToInt()
   ```
3. Cada registro financeiro sintético recebe: `platformId` do grupo, `smallPackagesCount`/`largePackagesCount` (contados só dentro do grupo), `startKm`/`endKm` calculados a partir do KM inicial real + `proratedKm` acumulado (não o range cheio da rota física), `startTime`/`endTime` proporcionais dentro da janela real de 4h, e a mesma observação `"Encerrada via Central do Motorista (Rota Master #<id>)"` **acrescida do nome da plataforma**, para rastreabilidade.
4. **Verificação obrigatória de soma**: a soma dos `proratedKm` de todos os grupos deve bater exatamente com o KM total da rota física (ajustar arredondamento no último grupo para fechar a soma, evitando sobra/falta de centésimos).

Isso preserva a integridade de `RelatoriosViewModel.totalKm` (soma sem duplicar) e torna "Rentabilidade por Plataforma" (já existente, linhas ~538-566) coerente com a realidade, em vez de zerado ou duplicado.

---

## 4. Prompts de implementação por subagente

### 🔍 Prompt 0 — Auditoria do modelo de bipagem de parceiro (Arquiteto)
*(mantido da rodada anterior — ainda pendente)*
```json
{
  "to_agent": "Arquiteto",
  "title": "Confirmar se delivery_partner_sessions já guarda pacotes individualmente",
  "context": "Checar código-fonte e schema Supabase antes de criar partner_session_packages.",
  "acceptance_criteria": [
    "Relatório claro sobre existência ou não de estrutura granular",
    "Decisão: criar partner_session_packages do zero ou reaproveitar estrutura existente"
  ]
}
```

### 🗄️ Prompt 1 — Migrations e Modelos (Backend)
```json
{
  "to_agent": "Backend",
  "title": "Estender master_route_stops com platform_id, package_type, photo_url e campos de transferência",
  "context": "Aplicar as migrations da Seção 2.1 em Supabase. Atualizar MasterRouteModels.kt (novos enums PackageType e TransferStatus), MasterRouteStopDto.kt e as funções toDomain()/toDto() em MasterRouteDto.kt. Atualizar assinatura de MasterRouteRepository.addStop(...) para aceitar platformId, packageType e photoUrl opcionais. Criar índice de busca por barcode ativo (idx_master_route_stops_barcode_active) e a função findActiveMasterStopByBarcode(barcode) usada no Prompt 6.",
  "acceptance_criteria": [
    "Migrations aplicadas e documentadas em docs/backend/",
    "MasterRouteStop/MasterRouteStopDto com os novos campos, testes de serialização passando",
    "addStop aceita os 3 novos parâmetros com valores padrão retrocompatíveis (packageType default PACOTINHO)",
    "findActiveMasterStopByBarcode usando o índice, sem full scan"
  ],
  "depends_on": ["Auditoria do modelo de bipagem de parceiro"]
}
```

### 📱 Prompt 2 — Seletor de Plataforma "Sticky" + Tipo de Produto no Scanner (Android/Kotlin)
```json
{
  "to_agent": "Android/Kotlin",
  "title": "RouteScannerScreen: seletor persistente de Plataforma e Tipo de Produto por bipagem",
  "context": "Adicionar a RouteScannerUiState (RouteScannerViewModel.kt): currentPlatformId (mantém o último valor entre bipagens) e currentPackageType (padrão PACOTINHO, alternável a cada bipagem, não sticky). Adicionar indicador visual grande e persistente no topo da RouteScannerScreen mostrando a plataforma ativa (ex.: faixa colorida com nome da plataforma), não apenas um dropdown discreto — para reduzir risco de bipar pacote com plataforma errada por esquecimento. Passar platformId e packageType para masterRouteRepository.addStop(...) em onPackageScanned.",
  "acceptance_criteria": [
    "Plataforma permanece selecionada entre bipagens consecutivas até o usuário trocar manualmente",
    "Tipo de Produto (Pacotinho/Volumoso) com Pacotinho pré-marcado a cada bipagem",
    "Indicador de plataforma ativa visível sem precisar abrir o seletor",
    "Testes: trocar plataforma no meio da sessão gera stops com platformId correto antes e depois da troca"
  ],
  "depends_on": ["Estender master_route_stops com platform_id, package_type, photo_url e campos de transferência"]
}
```

### 📱 Prompt 3 — Confiança Mínima no Auto-Avanço + Foto de Backup (Android/Kotlin)
```json
{
  "to_agent": "Android/Kotlin",
  "title": "Gate de confiança no auto-avanço do scanner e captura opcional de foto da etiqueta",
  "context": "Em RouteScannerViewModel.startAutoAdvanceTimer(), hoje o timer sempre dispara após a bipagem, sem checar a qualidade do parsedAddress. Adicionar checagem: só iniciar o auto-avanço automaticamente se parsedAddress.cep != null e parsedAddress.fullFormattedAddress não estiver vazio; caso contrário, manter o card aberto aguardando edição manual (sem countdown). Adicionar captura opcional de foto do frame congelado da etiqueta (mesmo frame já usado pelo OCR), upload para bucket do Supabase Storage, salvando a URL em photo_url. Calcular photo_expires_at = data de conclusão da rota + 15 dias no momento do finishRoute (Prompt 4).",
  "acceptance_criteria": [
    "Auto-avanço não dispara quando CEP ou endereço vieram vazios do OCR — força edição manual",
    "Botão opcional 'Tirar foto da etiqueta' no card de confirmação, não obrigatório",
    "Foto comprimida antes do upload (baixa resolução, o suficiente para consulta, não para arquivo)",
    "Teste: parsedAddress incompleto não inicia countdown; parsedAddress completo inicia normalmente"
  ],
  "depends_on": ["RouteScannerScreen: seletor persistente de Plataforma e Tipo de Produto por bipagem"]
}
```

### 🧹 Prompt 4 — Job de Expiração das Fotos (Backend/DevOps)
```json
{
  "to_agent": "Backend",
  "title": "Rotina de limpeza de fotos de etiqueta expiradas",
  "context": "Criar uma Supabase Edge Function (ou job agendado) que roda periodicamente: seleciona master_route_stops onde photo_expires_at < now() e photo_url is not null, apaga o arquivo do bucket de Storage e limpa photo_url/photo_expires_at no registro. Documentar a política (15 dias após conclusão da rota) em docs/backend/.",
  "acceptance_criteria": [
    "Job testável manualmente (endpoint ou comando local) antes de agendar",
    "Não apaga fotos de rotas ainda em_andamento (só as já concluídas há mais de 15 dias)",
    "Log de quantas fotos foram removidas por execução, para acompanhar consumo de cota"
  ],
  "depends_on": ["Gate de confiança no auto-avanço do scanner e captura opcional de foto da etiqueta"]
}
```

### 📱 Prompt 5 — Cards Expansíveis/Contraíveis no Cockpit (Android/Kotlin)
```json
{
  "to_agent": "Android/Kotlin",
  "title": "StopDeliveryCard com estado expandido/contraído e contador no cabeçalho",
  "context": "Adicionar estado local de expandido/contraído por card em StopDeliveryCard.kt (padrão contraído: mostra só recipientName completo + barcode). Adicionar em RouteCockpitScreen.kt: botão 'Expandir todos'/'Contrair todos' no topo da lista, controlando um Set<String> de IDs expandidos no RouteCockpitViewModel; e um contador 'X pacotes bipados' no cabeçalho da tela, somando uiState.stops.size.",
  "acceptance_criteria": [
    "Todos os cards contraídos por padrão ao abrir o Cockpit",
    "Toque individual expande/contrai um card sem afetar os demais",
    "'Expandir todos'/'Contrair todos' sincroniza o estado de todos de uma vez",
    "Contador no topo reflete o total de paradas em tempo real, inclusive após importação/transferência (Prompt 6)"
  ],
  "depends_on": []
}
```

### 📱 Prompt 6 — Atribuição Cruzada Master ↔ Parceiro (Android/Kotlin)
```json
{
  "to_agent": "Android/Kotlin",
  "title": "Atribuição no momento da bipagem, reatribuição na lista, e importação por bipagem duplicada no Parceiro",
  "context": "No card de confirmação do RouteScannerScreen, adicionar seletor 'Fica com: Master / [Parceiro X] / [Parceiro Y]', gravando assignedPartnerId + transferStatus=ATRIBUIDO_PENDENTE quando != Master. No RouteCockpitScreen, ação de swipe/toque-longo em qualquer parada pendente para 'Enviar para parceiro', mesmo efeito. Na tela de bipagem já existente do Entregador Parceiro, antes de tratar um código como pacote novo, consultar findActiveMasterStopByBarcode(barcode) (Prompt 1); se encontrar, gravar em partner_session_packages com origin=IMPORTADO_MASTER e atualizar o master_route_stops correspondente para transferStatus=CONFIRMADO, transferredVia=SCAN_PARCEIRO.",
  "acceptance_criteria": [
    "Paradas com transferStatus=ATRIBUIDO_PENDENTE somem da lista de navegação ativa do Master, mas aparecem em seção 'aguardando confirmação'",
    "Bipagem duplicada no parceiro é atômica (não duplica registro em falha de rede)",
    "Pacote nunca pré-atribuído também é importado corretamente se bipado no parceiro (dedup por barcode, não só por atribuição prévia)"
  ],
  "depends_on": ["Estender master_route_stops com platform_id, package_type, photo_url e campos de transferência"]
}
```

### 📱 Prompt 7 — Ícone de Origem no Fechamento da Sessão do Parceiro (Android/Kotlin)
```json
{
  "to_agent": "Android/Kotlin",
  "title": "Sinalizar pacotes importados do Master na tela de resumo do parceiro",
  "context": "Na tela de fechamento de sessão do parceiro, exibir ícone + tooltip nos itens com origin=IMPORTADO_MASTER, e contagem separada 'próprios vs. importados' no resumo.",
  "acceptance_criteria": [
    "Ícone visível e diferenciado",
    "Contagem separada no resumo numérico",
    "Sem regressão na tela de fechamento existente"
  ],
  "depends_on": ["Atribuição no momento da bipagem, reatribuição na lista, e importação por bipagem duplicada no Parceiro"]
}
```

### 📱 Prompt 8 — Handoff Financeiro Multi-Plataforma com Rateio de KM/Tempo (Android/Kotlin)
```json
{
  "to_agent": "Android/Kotlin",
  "title": "Estender applyMasterRouteHandOff e RouteCockpitViewModel.finishRoute para múltiplas plataformas com rateio proporcional",
  "context": "Em RouteCockpitViewModel.finishRoute, antes do handoff, agrupar as paradas ativas do Master por platformId (Seção 3) e calcular, por grupo: smallPackagesCount, largePackagesCount, proratedKm, proratedWorkedMinutes, startTime/endTime derivados (hora do 1º pacote bipado do grupo até +4h, respeitando a proporção). Estender applyMasterRouteHandOff(...) em NewRouteViewModel.kt para aceitar smallPackagesCount, largePackagesCount, routeDate, startTime, endTime, startKm, endKm (além dos parâmetros já existentes). Atualizar NavGraph.kt para navegar sequencialmente (ou em fila) para uma instância de 'Lançar Ganhos por Rota' por grupo de plataforma, cada uma pré-preenchida com seus próprios valores.",
  "acceptance_criteria": [
    "Rota física com 1 única plataforma continua gerando exatamente 1 registro financeiro (sem regressão do comportamento atual)",
    "Rota física com N plataformas gera N registros financeiros, cada um com contagens e valores só daquela plataforma",
    "Soma dos proratedKm de todos os grupos bate exatamente com o KM total da rota física (sem sobra/falta por arredondamento)",
    "RelatoriosViewModel.totalKm não fica inflado após a divisão (teste de regressão comparando total antes/depois da feature)",
    "KM Inicial de cada registro sintético usa o histórico corretamente (sem quebrar o auto-preenchimento já existente via lastOdometerKm)"
  ],
  "depends_on": ["Estender master_route_stops com platform_id, package_type, photo_url e campos de transferência", "RouteScannerScreen: seletor persistente de Plataforma e Tipo de Produto por bipagem"]
}
```

### 🗺️ Prompt 9 — Geocodificação (ORS), Ordenação e Mapa Embutido (Android/Kotlin)
```json
{
  "to_agent": "Android/Kotlin",
  "title": "Geocodificação via OpenRouteService, ordenação por vizinho mais próximo e mapa osmdroid na aba Rota",
  "context": "Nenhuma dessas 3 peças existe ainda no repositório. Implementar: (1) geocodificação em background de cada parada nova via OpenRouteService Geocoding (chave gratuita, cota 1.000/dia confirmada), preenchendo latitude/longitude; (2) ordenação v1 local via heurístico de vizinho mais próximo usando os lat/lng já obtidos, com ponto de extensão para trocar por ORS Optimization (lotes de até 50 localizações, cota 500/dia) depois; (3) mapa osmdroid embutido no Cockpit, um pin por parada, colorido por status.",
  "acceptance_criteria": [
    "Geocodificação não excede a cota diária mesmo em dia de 130 pacotes (cache local, nunca geocodifica o mesmo endereço duas vezes)",
    "Ordenação local funciona offline, sem chamada de rede",
    "Mapa degrada graciosamente quando uma parada ainda não tem lat/lng"
  ],
  "depends_on": ["Cards expansíveis/contraíveis no Cockpit"]
}
```

### 🧪 Prompt 10 — Testes (QA)
```json
{
  "to_agent": "QA",
  "title": "Cobertura de testes das extensões",
  "context": "Cobrir: parsing/gate de confiança do OCR; máquina de estados de transferência; dedup por barcode; rateio proporcional de KM/tempo (incluindo teste de que a soma bate com o total e que RelatoriosViewModel.totalKm não duplica); geração de N registros financeiros para N plataformas; sticky selector mantendo platformId entre bipagens; expand/collapse dos cards; expiração de fotos.",
  "acceptance_criteria": [
    "testAutoAdvanceDoesNotTriggerWithIncompleteParsing",
    "testAssignPartnerAtScanTimeSetsPendingStatus",
    "testPartnerScanOfExistingMasterBarcodeImportsAndConfirms",
    "testProratedKmSumsExactlyToPhysicalRouteTotal",
    "testRelatoriosTotalKmNotInflatedByMultiPlatformHandoff",
    "testHandOffGeneratesOneFinancialRoutePerPlatform",
    "testStickyPlatformSelectorPersistsBetweenScans",
    "testExpandCollapseAllTogglesEveryCard",
    "testPhotoExpiryJobDeletesOnlyStopsPast15Days"
  ],
  "depends_on": ["Estender applyMasterRouteHandOff e RouteCockpitViewModel.finishRoute para múltiplas plataformas com rateio proporcional", "Sinalizar pacotes importados do Master na tela de resumo do parceiro", "Rotina de limpeza de fotos de etiqueta expiradas"]
}
```

---

## 5. Verificação final

```powershell
./gradlew testDebugUnitTest --tests "com.fernando.centraldomotorista.delivery.*"
./gradlew testDebugUnitTest --tests "com.fernando.centraldomotorista.relatorios.*"
./gradlew compileDebugKotlin
./gradlew installDebug
```

Validação manual sugerida:
1. Bipar uma rota só com Shopee — conferir que o handoff continua gerando 1 registro (sem regressão).
2. Bipar uma rota misturando Shopee e Mercado Livre — trocar a plataforma no meio da bipagem, concluir e conferir 2 registros financeiros com contagens e KM proporcionais corretos.
3. Comparar o "Total de KM" no Relatório antes e depois — não pode ter dobrado.
4. Testar atribuição a parceiro na bipagem e reatribuição depois na lista; bipar o mesmo código na sessão do parceiro e confirmar importação + ícone no fechamento.
5. Deixar o OCR falhar de propósito (etiqueta borrada) e confirmar que o auto-avanço não dispara sozinho.
6. Tirar foto de uma etiqueta, concluir a rota, e (manualmente, sem esperar 15 dias) rodar o job de limpeza para confirmar que ele não apaga fotos de rota recente.
7. Expandir/contrair cards individualmente e via botão geral; conferir contador no topo.

```powershell
git add -A
git commit -m "feat: plataforma por bipagem com rateio de KM, split pacotinho/volumoso, atribuicao cruzada Master-Parceiro, cards expansiveis e foto de etiqueta com expiracao"
git push origin main
```
