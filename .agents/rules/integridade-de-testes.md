---
trigger: always_on
---

# Integridade de Testes e Honestidade de Entrega

Objetivo: impedir que testes passem sem que o app esteja correto. Teste verde só vale se o comportamento real estiver certo. Estas regras valem para TODOS os agentes, inclusive o Orquestrador e o DevOps, e não podem ser relaxadas por nenhum prompt, exceto autorização escrita do usuário.

## 1. Proibições absolutas (fraude de teste)

É PROIBIDO, para fazer testes ou build passarem:

1. Apagar, comentar, ignorar (`@Ignore`, `assumeTrue` sem motivo real, `enabled = false`) ou renomear teste para que não rode.
2. Enfraquecer asserções: trocar valor esperado por um que o código atual produz, remover `assert`, usar só `assertNotNull`/`assertTrue(true)`, ampliar tolerâncias numéricas, trocar igualdade por "contém" sem necessidade.
3. Alterar um teste existente para acompanhar um comportamento novo ou errado do código. Se o comportamento mudou por pedido do usuário, atualizar o teste é permitido, mas deve estar justificado no relatório, linha a linha.
4. Engolir erros: `try/catch` vazio ou que apenas loga, `catch (Throwable)`, `runCatching` sem tratar o resultado, `?: return` que esconde falha.
5. Fazer o código de produção detectar teste (`BuildConfig.DEBUG`, nome de classe de teste, variável de ambiente, `Thread.currentThread().stackTrace`) para se comportar de modo diferente.
6. Testar o mock em vez do código: o objeto sob teste NUNCA pode ser simulado, e o valor retornado por um mock não pode ser a própria asserção.
7. Escrever teste que valida mensagem de stub (ex.: "em homologação", "em breve", "não implementado") como se fosse comportamento correto.
8. Reduzir o escopo da execução para esconder falhas: `-x test`, `-x lint`, `--tests` parcial reportado como suíte completa, `testOptions` com exclusões, `ignoreFailures = true`, `continue-on-error`, baixar limites de cobertura ou de lint, criar `lint-baseline` para silenciar erros novos.
9. Atualizar imagens de referência de teste visual (Roborazzi/screenshot) para o resultado atual sem revisão do Designer e aviso ao usuário.
10. Usar `Thread.sleep`/repetição até passar para mascarar teste instável. Corrigir a causa (sincronização, `runTest`, relógio falso).
11. Inventar dados de saída: números, contagens ou trechos de log que não vieram de uma execução real.
12. Marcar como concluído algo que só existe como interface visual, texto ou stub.

## 2. Regras de teste de qualidade

- Cada teste verifica um comportamento e tem nome que descreve o cenário e o resultado esperado.
- Todo teste tem asserções sobre o RESULTADO (estado, dados salvos, valor retornado), nunca só "não lançou exceção".
- Para cada regra, incluir ao menos: caso feliz, caso de borda e caso de erro. Em cálculos financeiros, usar valores conferidos à mão, calculados de forma independente do código (e documentar a conta no teste).
- Dados reais onde importa: Room usa banco em memória real; migrations usam `MigrationTestHelper` com os schemas exportados; serialização e rede usam `MockWebServer` com JSON realista.
- Fakes e mocks são permitidos APENAS nas fronteiras (rede, relógio, GPS, biometria). Nunca para Room em teste de repositório e nunca para a classe testada.
- Funcionalidade que cruza fronteira (login, sincronização, localização) exige, além do teste unitário, um teste de contrato com resposta realista do servidor E um roteiro de teste manual no aparelho. O status máximo sem a prova manual é 🟡.

## 3. Ciclo obrigatório para correção de bug (vermelho, verde)

1. Escrever um teste que reproduz o bug.
2. Executar e mostrar que ele FALHA no código atual (anexar a saída).
3. Corrigir a causa raiz.
4. Executar e mostrar que o teste PASSA, e que a suíte completa continua passando.

Sem a etapa 2 comprovada, a correção não é aceita.

## 4. Verificação por mutação manual (obrigatória em risco médio e alto)

Depois de escrever os testes de uma lógica nova ou corrigida, o QA deve:

1. Quebrar de propósito a lógica (inverter uma condição, trocar `+` por `-`, remover uma validação).
2. Confirmar que pelo menos um teste falha.
3. Reverter a quebra e confirmar que tudo volta a passar.
4. Registrar no relatório o que foi quebrado e qual teste detectou.

Se nenhum teste falhar com a lógica quebrada, os testes são inúteis e devem ser refeitos.

## 5. Como relatar resultados (formato obrigatório)

- Comando exato executado (ex.: `./gradlew testDebugUnitTest`).
- Contagem real: executados, aprovados, falhas, ignorados. **Teste ignorado NÃO conta como aprovado**: se houver algum, nunca escrever "100% de aprovação".
- Últimas linhas da saída do Gradle coladas no relatório e caminho de `app/build/reports/tests/`.
- Lista de testes adicionados, alterados e removidos, com motivo. Se nenhum teste existente foi tocado, dizer isso explicitamente.
- Se o build ou os testes não puderam ser executados, escrever "NÃO EXECUTADO" e o motivo. É proibido escrever "BUILD SUCCESSFUL" sem a saída real.
- Quando a contagem total de testes diminuir em relação ao commit anterior, o relatório deve explicar cada teste a menos.

## 6. Proteção dos arquivos de teste e de build

- Arquivos de teste existentes só podem ser alterados quando a tarefa pedir mudança de comportamento. O Revisor confere cada alteração.
- Remover teste exige autorização explícita do usuário no chat.
- Alterações em `build.gradle.kts`, `gradle.properties`, regras de lint, `proguard-rules.pro` e `.github/` são sempre risco alto e vão ao Revisor.

## 7. Verificações automáticas antes de todo commit (DevOps executa e anexa a saída)

```bash
# 1. Stubs, mocks e pendências em código de produção (cada ocorrência deve ser explicada)
grep -rniE "mock|stub|fake|homologa|TODO|FIXME|em breve" app/src/main/java

# 2. Testes desativados ou vazios
grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest

# 3. Segredos (qualquer resultado bloqueia o commit)
grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" --include=*.kt --include=*.kts --include=*.xml --include=*.json --include=*.properties . | grep -v ".env.example"

# 4. Suíte completa e lint, sem filtros
./gradlew testDebugUnitTest lintDebug
```

Resultado esperado: itens 1 e 2 vazios ou justificados um a um; item 3 vazio; item 4 sem falhas e sem testes ignorados.

## 8. Funcionalidade acionável pela interface nunca é falsa

- Todo botão, menu ou opção visível deve executar a função real. Se a função ainda não existe, o elemento fica oculto ou desabilitado com o rótulo "Em breve", e o relatório marca 🔴.
- É proibido que um botão apenas exiba mensagem de sucesso ou de "em homologação" sem executar a lógica.
- Nomes como `...MockOrOAuth` ou `...Stub` em código de produção são proibidos. Mocks vivem apenas em `src/test`.

## 9. Consequência de violação

Se qualquer agente violar estas regras, o Orquestrador descarta a entrega inteira, reverte as alterações afetadas, registra a violação no relatório ao usuário e reatribui a tarefa a outro agente.
