---
trigger: always_on
---

# Padrões de Segurança, UX/UI e Boas Práticas (GiroCusto)

Estes padrões valem para todo código novo e para todo trecho alterado. Dívidas já existentes devem virar tarefas próprias, pequenas e testadas, e não ser corrigidas de uma vez no meio de outra tarefa.

Antes de adotar qualquer biblioteca, API ou prática citada aqui, verificar a documentação oficial atual (developer.android.com, supabase.com/docs). Se algo estiver descontinuado, seguir a recomendação vigente e registrar no relatório.

## 1. Segurança

### 1.1 Segredos e configuração
- Chaves e IDs ficam no `.env` local (plugin Secrets, via BuildConfig) e nunca no repositório. O `.env.example` contém apenas valores de exemplo.
- No app só existe a chave pública (anon) do Supabase. A chave `service_role` e qualquer secret OAuth jamais entram no app, no repositório ou em prompts.
- Keystore (`*.jks`, `*.keystore`) e `.env` ficam no `.gitignore`. Se um segredo vazar no histórico do git, avisar o usuário para revogá-lo, pois apagar o arquivo não basta.

### 1.2 Sessão e dados locais
- Tokens de sessão (access e refresh) NÃO podem ficar em `SharedPreferences` em texto puro. Proteger com Android Keystore (criptografia AES-GCM) ou solução equivalente atual. A biblioteca `androidx.security:security-crypto` foi descontinuada, então confirmar o estado atual antes de adotar qualquer opção.
- O manifesto está com `allowBackup=true` e regras de backup vazias: por padrão isso pode copiar preferências com credenciais para o backup na nuvem. Excluir sessão e preferências sensíveis do backup (`data_extraction_rules.xml` e `backup_rules.xml`) ou desligar o backup.
- Bloqueio por biometria: usar `BiometricPrompt` com `CryptoObject` quando proteger dados sensíveis, e nunca guardar PIN ou senha em texto puro.

### 1.3 Rede
- Somente HTTPS; sem tráfego em texto puro (`usesCleartextTraffic` falso).
- `HttpLoggingInterceptor` apenas em debug, com `redactHeader("Authorization")` e `redactHeader("apikey")`. Nenhum log (Logcat, arquivo, crash report) pode conter token, senha, e-mail, CPF, localização ou dados financeiros do usuário.
- Timeouts de conexão e leitura explícitos; erros tratados com mensagem clara ao usuário.

### 1.4 Autenticação
- Login com Google: usar Credential Manager (`GetGoogleIdOption`) com nonce (hash SHA-256 enviado ao Google, valor original enviado ao Supabase). Login e cadastro com Google são o mesmo fluxo.
- Nunca simular autenticação, sessão ou resposta de servidor em código de produção.
- Mensagens de erro de login não revelam se o e-mail existe.

### 1.5 Supabase
- Todas as tabelas com dados de usuário têm RLS ativa e políticas que restringem acesso ao dono (`auth.uid() = user_id`). Tabela nova sem RLS é reprovada.
- Alterações de banco versionadas em `supabase/migrations`, com data e descrição, e documentadas no README.

### 1.6 Release e dependências
- Release com minificação e ofuscação R8 ativas (`isMinifyEnabled = true`) e regras de manutenção para Moshi, Retrofit e Room testadas em aparelho real. Atualmente está desligada: tarefa própria de risco alto.
- Dependências com versão fixa e catálogo `libs.versions.toml`. Verificar vulnerabilidades conhecidas a cada release e a cada nova dependência.
- Permissões mínimas. Localização: pedir somente quando a função for usada, com explicação prévia em português, preferir aproximada quando a precisa não for necessária, parar as atualizações ao sair da tela.

### 1.7 LGPD
- Coletar o mínimo necessário. Informar o que é coletado e por quê.
- O usuário deve poder exportar e excluir seus dados locais e da nuvem, e excluir a conta.
- Política de privacidade acessível no app antes de publicação em loja.

## 2. UX/UI para entregadores

Contexto: uso rápido, na rua, com uma mão, sol forte, interrupções frequentes, internet ruim.

- **Rapidez:** as tarefas mais frequentes (lançar corrida, abastecimento, despesa) em no máximo 2 toques a partir da tela inicial, com valores padrão inteligentes e último valor lembrado.
- **Alcance do polegar:** ações principais na metade inferior da tela. Botões grandes, espaçados e com rótulo de texto.
- **Entrada de dados:** tipo de teclado correto (`KeyboardType.Decimal` / `Number`), formatação pt-BR (R$ 1.234,56, vírgula decimal, dd/MM/aaaa), validação no campo com mensagem específica, nunca perder o que foi digitado.
- **Estados obrigatórios em toda tela:** carregando, vazio (com orientação do que fazer), erro (com ação "tentar de novo"), sem internet (o app deve continuar útil offline) e sucesso.
- **Ações destrutivas:** pedir confirmação e oferecer "Desfazer" (Snackbar) sempre que possível.
- **Feedback:** toda ação tem resposta visível imediata. Mensagens em português simples, dizendo o que aconteceu e o que fazer.
- **Consistência:** componentes Material 3, tema central (`ui/theme`), sem cores ou tamanhos soltos no código das telas. Respeitar tema claro/escuro do sistema.
- **Sem bloqueios:** não pedir permissão nem login logo na abertura sem explicar o benefício. O app deve funcionar localmente sem conta.
- **Animações:** curtas e úteis, respeitando a configuração do sistema de reduzir animações.

## 3. Acessibilidade (critérios de aceite de toda tela)

1. Área de toque mínima de 48dp em todo elemento interativo.
2. Contraste mínimo de 4,5:1 para texto normal e 3:1 para texto grande e ícones (WCAG AA), verificando também sob sol forte (preferir contraste alto).
3. `contentDescription` descritivo em todo ícone com função. `null` somente em elemento puramente decorativo (hoje há muitos `null` a auditar).
4. Navegação por TalkBack com ordem lógica, agrupamento (`semantics`) de cartões e leitura de valores completos ("R$ 45,00", não "45").
5. Informação nunca transmitida apenas por cor (acrescentar ícone ou texto).
6. Interface íntegra com fonte em 200% e sem corte de texto.
7. Campos com rótulo permanente (não apenas placeholder) e erro associado ao campo.

## 4. Boas práticas Android/Compose atuais

- Edge-to-edge com `WindowInsets` tratados (barras do sistema, teclado com `imePadding`, recorte). Com `targetSdk` 36 isso é o padrão do sistema.
- Suporte a voltar preditivo (predictive back) e a telas grandes/dobráveis (layout adaptável, sem moldura fixa). O `SmartphoneSimulator` (moldura de celular desenhada em telas largas) é recurso de pré-visualização: não deve afetar o comportamento em aparelhos reais nem em tablets.
- `collectAsStateWithLifecycle`, `rememberSaveable`, `remember` com chaves corretas, listas com `key` estável, nada de trabalho pesado dentro de Composable.
- Strings em `strings.xml` (`stringResource`) com plurais corretos (`pluralStringResource`). Hoje há centenas de textos fixos: migrar em tarefas pequenas por tela.
- Corrotinas estruturadas: `viewModelScope`, dispatchers injetáveis para teste, cancelamento correto, sem `GlobalScope`.
- Room: consultas `suspend` ou `Flow`, índices, transações, migrations testadas, schema exportado versionado no git.
- Kotlin: sem `!!` em código novo, sem `catch` vazio, `sealed` para estados e erros, funções curtas e nomes em inglês ou português de forma consistente com o arquivo.
- Arquivos novos até 800 linhas; telas e ViewModel muito grandes são divididos em tarefas de refatoração separadas.
- Dinheiro: código novo usa centavos (`Long`) ou `BigDecimal` com arredondamento explícito. `Double` não é permitido para novo valor monetário.
- Cada alteração de interface vem com `@Preview` (claro e escuro) e teste de UI com `testTag` semântico.
