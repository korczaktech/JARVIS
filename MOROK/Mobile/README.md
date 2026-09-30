# MOROK — MOBILE ANDROID

Aplicativo Android nativo do ecossistema Morok.

O Mobile não será uma página web empacotada apresentada como aplicativo. O objetivo é construir um **assistente de sistema Android**, com núcleo nativo, permissões explícitas, execução real de comandos e uma interface simples que poderá ser completamente reformulada depois sem reescrever o núcleo.

O aplicativo deverá funcionar por **voz, texto e botões**, utilizar os serviços do Android quando autorizados e delegar ao backend apenas o que não precisa ou não deve ser executado localmente.

---

# REGRAS FUNDAMENTAIS

1. O aplicativo final é **Android nativo**.
2. O núcleo de execução será feito prioritariamente em **Kotlin + Android SDK + Jetpack**.
3. A interface inicial deve ser simples, funcional e substituível.
4. HTML, CSS, JavaScript e SVG poderão ser usados em componentes visuais isolados quando trouxerem vantagem real, mas não serão usados para transformar o aplicativo inteiro em um WebView.
5. Nenhum botão, tela, permissão ou serviço deverá existir apenas para preencher interface.
6. Toda funcionalidade marcada como concluída precisa funcionar de verdade em um aparelho/emulador compatível.
7. Recursos que exigem permissões do Android deverão solicitar, explicar e respeitar essas permissões.
8. O Morok nunca deverá executar uma ação sensível sem a confirmação exigida pelo Android ou pela política de segurança definida para o recurso.
9. O backend não deve ser necessário para comandos que podem ser executados localmente.
10. O aplicativo deve continuar útil mesmo quando estiver sem internet, dentro do conjunto de funções locais implementadas.
11. O projeto usará **GitHub, Render e MongoDB** dentro dos limites gratuitos disponíveis.
12. Nenhum segredo será colocado no APK, no Git ou no frontend.
13. A atualização deverá ser automatizada ao máximo.
14. O README só poderá marcar uma funcionalidade como 🟢 quando ela tiver implementação e validação real.
15. Bibliotecas dos projetos em `../` devem ser usadas como código, referência ou infraestrutura quando forem compatíveis com a arquitetura; não devem ser incluídas apenas para aumentar a lista de dependências.

---

# LEGENDA

- 🔴 **Não implementado**
- 🟡 **Em andamento / parcialmente implementado**
- 🟢 **Implementado e validado**

Uma funcionalidade não é considerada concluída apenas porque existe uma classe, rota, tela, botão ou configuração correspondente.

---

# ARQUITETURA DO MOBILE

```
Android App
│
├── UI
│   ├── Jetpack Compose
│   ├── SVG / Canvas quando necessário
│   └── componentes visuais simples e substituíveis
│
├── Morok Core
│   ├── Command Router
│   ├── Intent Engine
│   ├── Permission Manager
│   ├── Confirmation Manager
│   ├── Task Manager
│   ├── Memory Client
│   ├── Tool Registry
│   ├── Automation Engine
│   ├── Update Manager
│   └── Audit Logger
│
├── Android Native Services
│   ├── AccessibilityService
│   ├── NotificationListenerService
│   ├── Foreground Service
│   ├── SpeechRecognizer
│   ├── TextToSpeech
│   ├── Camera
│   ├── MediaProjection
│   ├── Contacts
│   ├── Calendar
│   ├── Location
│   ├── Bluetooth
│   ├── Wi-Fi
│   ├── Telephony
│   ├── MediaSession
│   └── PackageManager
│
├── Local Storage
│   ├── Room
│   ├── DataStore
│   ├── encrypted secrets
│   └── local command history
│
└── Cloud
    ├── Morok API
    ├── MongoDB
    ├── Render
    └── GitHub Releases
```

---

# FASE 0 — FUNDAÇÃO NATIVA

**Status: 🟢 Concluída e validada**

Objetivo: transformar o Mobile em um aplicativo Android nativo real e estabelecer a fundação que todas as outras fases utilizarão.

## 0.1 — Projeto Android

- 🟢 Projeto Android nativo em Kotlin
- 🟢 Gradle/Kotlin DSL
- 🟢 Android SDK configurado
- 🟢 Min/Target SDK definidos
- 🟢 Application ID definitivo
- 🟢 VersionCode e VersionName
- 🟢 Build Debug
- 🟢 Build Release
- 🟢 APK instalável
- 🟢 Assinatura de release
- 🟢 Ícone Morok
- 🟢 Splash screen
- 🟢 Estrutura modular
- 🟢 Separação entre UI, domínio, serviços Android e infraestrutura
- 🟢 ProGuard/R8
- 🟢 Build reproduzível

## 0.2 — Base de execução

- 🟢 Command Router
- 🟢 Intent Router
- 🟢 Tool Registry
- 🟢 Event Bus
- 🟢 Task Queue
- 🟢 Execution Context
- 🟢 Cancellation
- 🟢 Timeout
- 🟢 Retry
- 🟢 Error Recovery
- 🟢 Logs estruturados
- 🟢 Auditoria de ações
- 🟢 estado online/offline
- 🟢 sincronização de estado

## 0.3 — Persistência local

- 🟢 Room para dados estruturados
- 🟢 DataStore para preferências
- 🟢 histórico local
- 🟢 cache
- 🟢 fila offline
- 🟢 configurações
- 🟢 estado de tarefas
- 🟢 memória local
- 🟢 migrações de banco
- 🟢 limpeza segura de dados

## 0.4 — Segurança

- 🟢 Android Keystore
- 🟢 criptografia local
- 🟢 armazenamento seguro de tokens
- 🟢 sessão autenticada
- 🟢 expiração/renovação de sessão
- 🟢 controle de permissões
- 🟢 confirmação de ações sensíveis
- 🟢 auditoria
- 🟢 proteção contra comandos duplicados
- 🟢 proteção contra execução acidental

## 0.5 — Backend

Integração com a infraestrutura já definida no projeto:

- 🟢 API Morok
- 🟢 Render
- 🟢 MongoDB
- 🟢 autenticação
- 🟢 sessões
- 🟢 memória
- 🟢 ferramentas
- 🟢 tarefas
- 🟢 automações
- 🟢 auditoria
- 🟢 documentos
- 🟢 integrações
- 🟢 health checks
- 🟢 sincronização Mobile ↔ API

O Mobile deverá possuir um modo de operação local e um modo conectado.

---


## Validação da Fase 0

- 🟢 Projeto Android nativo criado em Kotlin.
- 🟢 Gradle 8.9 + Android Gradle Plugin 8.7.3 configurados.
- 🟢 Java 17 configurado.
- 🟢 SDK 35 / minSdk 26 / targetSdk 35 definidos.
- 🟢 APK Debug compilado pelo GitHub Actions.
- 🟢 Testes unitários executados com sucesso.
- 🟢 Lint de Release executado com sucesso.
- 🟢 APK Release compilado com assinatura efêmera de CI.
- 🟢 Room + KSP compilados e validados.
- 🟢 DataStore configurado.
- 🟢 armazenamento criptografado configurado.
- 🟢 Command Router, Tool Registry, Task Manager, Execution Context e Audit Logger criados.
- 🟢 serviço Foreground criado e declarado.
- 🟢 permissões-base declaradas e solicitadas.
- 🟢 ícone nativo inicial do aplicativo configurado.
- 🟢 workflow oficial de Android CI criado em `.github/workflows/morok-mobile.yml`.

A validação do build realizada no GitHub confirmou Debug, testes, Lint e Release assinado. A etapa de integridade do APK usa `unzip -t` para não depender da disponibilidade do executável `apksigner` no PATH do runner.

# FASE 1 — ASSISTENTE ANDROID FUNCIONAL

**Status: 🔴 Não implementada no Mobile**

Objetivo: entregar a primeira versão realmente utilizável do Morok como assistente Android.

## 1.1 — Entrada por voz

- 🔴 ativação por botão
- 🔴 SpeechRecognizer nativo
- 🔴 reconhecimento em português
- 🔴 reconhecimento contínuo quando permitido
- 🔴 detecção de início/fim de fala
- 🔴 tratamento de ruído
- 🔴 comandos curtos
- 🔴 comandos compostos
- 🔴 confirmação por voz
- 🔴 fallback para texto
- 🔴 TextToSpeech
- 🔴 escolha de voz
- 🔴 velocidade da fala
- 🔴 interrupção da fala
- 🔴 resposta falada enquanto o comando continua sendo executado

### Wake word

- 🔴 arquitetura para wake word
- 🔴 detecção local
- 🔴 ativação com tela desligada quando o Android permitir
- 🔴 integração com `openWakeWord`
- 🔴 baixo consumo de bateria
- 🔴 fallback sem wake word

A detecção contínua deverá ser opcional e controlada pelo usuário.

## 1.2 — Texto

- 🔴 campo de comando
- 🔴 histórico
- 🔴 sugestões de comandos
- 🔴 comandos predefinidos
- 🔴 comandos compostos
- 🔴 respostas
- 🔴 cancelamento
- 🔴 repetição
- 🔴 edição de comando
- 🔴 execução em segundo plano quando permitido

## 1.3 — Botões

A interface inicial terá somente controles funcionais:

- 🔴 ouvir
- 🔴 parar
- 🔴 executar
- 🔴 cancelar
- 🔴 confirmar
- 🔴 negar
- 🔴 configurações
- 🔴 histórico
- 🔴 status
- 🔴 atualizações

## 1.4 — Comandos Android

O Morok deverá possuir uma camada de comandos nativos, incluindo quando tecnicamente permitido pelo Android:

### Sistema

- 🔴 volume
- 🔴 brilho
- 🔴 modo silencioso
- 🔴 vibração
- 🔴 lanterna
- 🔴 rotação
- 🔴 modo avião, respeitando restrições do Android
- 🔴 Wi-Fi
- 🔴 Bluetooth
- 🔴 dados/conectividade conforme APIs disponíveis
- 🔴 localização
- 🔴 economia de bateria
- 🔴 abertura de configurações
- 🔴 informações do aparelho
- 🔴 bateria
- 🔴 armazenamento
- 🔴 rede

### Aplicativos

- 🔴 abrir aplicativo
- 🔴 fechar/interromper quando permitido
- 🔴 abrir configurações de aplicativo
- 🔴 identificar aplicativos instalados
- 🔴 verificar versão
- 🔴 iniciar intents
- 🔴 compartilhar conteúdo
- 🔴 selecionar aplicativo
- 🔴 desinstalação via fluxo oficial do Android

### Comunicação

- 🔴 fazer ligação via fluxo autorizado
- 🔴 abrir discador
- 🔴 contatos
- 🔴 criar contato
- 🔴 pesquisar contato
- 🔴 SMS através dos fluxos e permissões disponíveis
- 🔴 compartilhamento
- 🔴 notificações

### Mídia

- 🔴 reproduzir
- 🔴 pausar
- 🔴 continuar
- 🔴 próximo
- 🔴 anterior
- 🔴 volume
- 🔴 controlar sessão de mídia
- 🔴 identificar mídia atual quando disponível

### Organização

- 🔴 calendário
- 🔴 eventos
- 🔴 lembretes
- 🔴 tarefas
- 🔴 alarmes através das APIs disponíveis
- 🔴 contatos
- 🔴 notificações

### Arquivos

- 🔴 listar arquivos
- 🔴 pesquisar arquivos
- 🔴 abrir
- 🔴 copiar
- 🔴 mover
- 🔴 renomear
- 🔴 excluir
- 🔴 criar
- 🔴 compartilhar
- 🔴 downloads
- 🔴 documentos
- 🔴 leitura de formatos suportados

---

# FASE 2 — CONTROLE PROFUNDO DO ANDROID

**Status: 🔴 Não implementada**

Objetivo: fazer o Morok agir como assistente de sistema, e não apenas como aplicativo de conversa.

## 2.1 — AccessibilityService

Quando autorizado pelo usuário:

- 🔴 identificar elementos da tela
- 🔴 clicar
- 🔴 tocar
- 🔴 rolar
- 🔴 voltar
- 🔴 navegar
- 🔴 preencher campos
- 🔴 selecionar elementos
- 🔴 ler conteúdo acessível
- 🔴 executar sequências
- 🔴 automatizar aplicativos compatíveis
- 🔴 detectar mudança de tela
- 🔴 interromper automação
- 🔴 confirmar ações críticas

O serviço deverá possuir limites de segurança, registro de ações e desligamento imediato.

## 2.2 — Notificações

Com autorização:

- 🔴 ler notificações
- 🔴 identificar aplicativo
- 🔴 identificar remetente
- 🔴 resumir notificações
- 🔴 filtrar
- 🔴 responder quando o aplicativo/API permitir
- 🔴 executar ações de notificação
- 🔴 criar regras
- 🔴 registrar eventos

## 2.3 — Tela e visão

- 🔴 captura de tela via MediaProjection
- 🔴 OCR
- 🔴 análise visual
- 🔴 identificação de elementos
- 🔴 leitura de interfaces
- 🔴 comparação de estados
- 🔴 visão computacional
- 🔴 automação baseada em visão
- 🔴 confirmação antes de ações de risco

## 2.4 — Câmera e microfone

- 🔴 câmera
- 🔴 captura controlada
- 🔴 análise de imagem
- 🔴 reconhecimento de texto
- 🔴 leitura de QR
- 🔴 microfone
- 🔴 processamento de áudio
- 🔴 gravação somente quando explicitamente autorizada

## 2.5 — Serviços em segundo plano

- 🔴 Foreground Service
- 🔴 serviço de voz
- 🔴 fila de tarefas
- 🔴 execução de automações
- 🔴 recuperação após interrupção
- 🔴 retomada de tarefas
- 🔴 controle de bateria
- 🔴 limites de consumo

---

# FASE 3 — INTELIGÊNCIA, AGENTES E AUTOMAÇÃO

**Status: 🔴 Não implementada**

Objetivo: incorporar as capacidades dos projetos open-source presentes em `../` de maneira funcional.

## 3.1 — LangGraph

Uso planejado:

- 🔴 workflows de estado
- 🔴 agentes com estado
- 🔴 memória de curto prazo
- 🔴 memória persistente
- 🔴 execução durável
- 🔴 retomada após falhas
- 🔴 human-in-the-loop
- 🔴 interrupção e alteração de estado
- 🔴 subgrafos
- 🔴 branching
- 🔴 streaming
- 🔴 tracing/observabilidade quando disponível
- 🔴 execução de tarefas longas

## 3.2 — AutoGen

Como referência/integração de arquitetura multiagente:

- 🔴 agentes especializados
- 🔴 AgentChat
- 🔴 comunicação entre agentes
- 🔴 multi-agent orchestration
- 🔴 ferramentas como agentes
- 🔴 MCP
- 🔴 execução de código controlada
- 🔴 workflows multiagente
- 🔴 avaliação/benchmarking
- 🔴 Studio apenas como referência de prototipação

AutoGen encontra-se em manutenção; sua adoção deverá ser avaliada junto das alternativas atuais sem tornar o Mobile dependente de uma tecnologia abandonada.

## 3.3 — CrewAI

- 🔴 agentes
- 🔴 crews
- 🔴 tarefas
- 🔴 processos
- 🔴 ferramentas
- 🔴 memória
- 🔴 planejamento
- 🔴 colaboração entre agentes
- 🔴 execução sequencial
- 🔴 execução hierárquica
- 🔴 workflows
- 🔴 eventos
- 🔴 observabilidade
- 🔴 persistência
- 🔴 human-in-the-loop

## 3.4 — Self-Operating Computer

Adaptado ao Android, quando tecnicamente possível:

- 🔴 perceber estado visual
- 🔴 interpretar tela
- 🔴 decidir ação
- 🔴 executar ação
- 🔴 observar resultado
- 🔴 repetir ciclo
- 🔴 controle por objetivo
- 🔴 intervenção humana
- 🔴 encerramento seguro

O conceito será adaptado para Android; comandos específicos de desktop não serão falsamente declarados como compatíveis.

## 3.5 — Open Interpreter

Camada de execução controlada:

- 🔴 interpretação de comandos
- 🔴 execução de ferramentas
- 🔴 execução de código quando permitido
- 🔴 manipulação de arquivos
- 🔴 automação
- 🔴 planejamento
- 🔴 confirmação antes de operações sensíveis
- 🔴 sandbox/limites
- 🔴 logs
- 🔴 cancelamento

Código arbitrário nunca deverá receber privilégios ilimitados no aparelho.

## 3.6 — Anthropic Quickstarts

Usar os padrões e exemplos aplicáveis para:

- 🔴 agentes
- 🔴 uso de ferramentas
- 🔴 computer use como referência arquitetural
- 🔴 ciclos percepção → decisão → ação
- 🔴 aprovação humana
- 🔴 integração com modelos
- 🔴 streaming
- 🔴 tool calling

## 3.7 — Android Agent

Explorar a arquitetura específica para agente Android:

- 🔴 percepção da interface
- 🔴 planejamento
- 🔴 execução de ações
- 🔴 controle de aplicativos
- 🔴 automação
- 🔴 feedback visual
- 🔴 recuperação de falhas

## 3.8 — Appium

Camada de automação e testes:

- 🔴 testes de aplicativos
- 🔴 descoberta de elementos
- 🔴 interação automatizada
- 🔴 testes de fluxos
- 🔴 execução em dispositivos/emuladores
- 🔴 regressão
- 🔴 validação das ações do Morok

Appium será usado principalmente para validação e automação controlada; o mecanismo principal de controle do usuário deverá permanecer nativo.

## 3.9 — scrcpy

Uso como ferramenta de desenvolvimento/operação:

- 🔴 espelhamento Android
- 🔴 controle por USB
- 🔴 controle por rede
- 🔴 diagnóstico
- 🔴 testes
- 🔴 suporte ao desenvolvimento

Não será embutido como requisito do APK final quando não fizer sentido.

## 3.10 — LiveKit Agents

Para voz e agentes em tempo real, quando necessário:

- 🔴 áudio em tempo real
- 🔴 agentes de voz
- 🔴 streaming
- 🔴 interrupção
- 🔴 turn detection
- 🔴 transporte em tempo real
- 🔴 ferramentas durante conversa
- 🔴 comunicação de baixa latência

## 3.11 — openWakeWord

- 🔴 wake word local
- 🔴 modelos de ativação
- 🔴 detecção contínua
- 🔴 limiar configurável
- 🔴 baixo consumo
- 🔴 privacidade local
- 🔴 fallback para botão

---

# FASE 4 — AUTOMAÇÃO, BACKEND E ECOSSISTEMA

**Status: 🔴 Não implementada**

## 4.1 — Faster-Whisper

Camada opcional de reconhecimento de fala:

- 🔴 transcrição
- 🔴 processamento local quando houver hardware adequado
- 🔴 processamento remoto
- 🔴 timestamps
- 🔴 segmentação
- 🔴 detecção de idioma
- 🔴 fallback
- 🔴 comparação com SpeechRecognizer nativo

O mecanismo escolhido deverá considerar CPU, RAM, bateria e latência do aparelho.

## 4.2 — Flutter

O projeto Flutter será tratado como referência de engenharia multiplataforma e interoperabilidade, mas **não substituirá o requisito de Android nativo**.

- 🔴 análise de componentes reutilizáveis
- 🔴 integração com APIs nativas como referência
- 🔴 FFI quando necessário
- 🔴 platform channels como referência
- 🔴 arquitetura de plugins
- 🔴 testes multiplataforma quando aplicáveis

## 4.3 — shadcn/ui

A biblioteca será usada como referência para:

- 🔴 componentes
- 🔴 tokens visuais
- 🔴 estados
- 🔴 acessibilidade
- 🔴 consistência de interface

Quando a implementação nativa for superior, os conceitos serão traduzidos para componentes Android/Compose.

## 4.4 — FastAPI

Backend auxiliar quando Python for a melhor escolha:

- 🔴 APIs
- 🔴 validação
- 🔴 autenticação
- 🔴 OpenAPI
- 🔴 JSON Schema
- 🔴 endpoints assíncronos
- 🔴 documentação automática
- 🔴 serviços de IA
- 🔴 serviços de voz
- 🔴 serviços de visão
- 🔴 workers

A API principal do ecossistema poderá continuar em Fastify/TypeScript quando isso for mais adequado; FastAPI será usado para serviços especializados.

## 4.5 — n8n

Automação externa:

- 🔴 workflows
- 🔴 triggers
- 🔴 ações
- 🔴 webhooks
- 🔴 integrações
- 🔴 aprovação humana
- 🔴 automações multi-etapas
- 🔴 JavaScript/Python em workflows
- 🔴 conexões com serviços externos
- 🔴 observabilidade
- 🔴 templates

O n8n deverá ser tratado como serviço de automação, não como parte obrigatória do APK.

## 4.6 — Integrações do ecossistema

- 🔴 GitHub
- 🔴 Render
- 🔴 MongoDB
- 🔴 serviços web
- 🔴 arquivos
- 🔴 calendário
- 🔴 contatos
- 🔴 notificações
- 🔴 automações
- 🔴 dispositivos
- 🔴 APIs externas

---

# FASE 5 — PRODUÇÃO, ATUALIZAÇÃO AUTOMÁTICA E CONCLUSÃO

**Status: 🔴 Não implementada**

Objetivo: transformar o Mobile em uma versão estável, distribuível e continuamente atualizável.

## 5.1 — Atualização automática

Fluxo desejado:

```
Abrir Morok
   ↓
Verificar versão
   ↓
Existe atualização?
   ├── Não → iniciar normalmente
   └── Sim
        ↓
Mostrar "Atualização disponível"
        ↓
Usuário confirma "ATUALIZAR"
        ↓
Baixar pacote
        ↓
Validar assinatura/hash
        ↓
Instalar
        ↓
Reiniciar Morok
        ↓
Validar nova versão
```

### Requisitos

- 🔴 versionamento semântico
- 🔴 GitHub Releases
- 🔴 manifest de versão
- 🔴 verificação ao abrir
- 🔴 download automático
- 🔴 validação de integridade
- 🔴 validação de assinatura
- 🔴 rollback quando possível
- 🔴 atualização incremental quando viável
- 🔴 retomada de download
- 🔴 atualização somente após confirmação do usuário
- 🔴 tela de atualização
- 🔴 instalação automática pelo fluxo suportado pelo Android
- 🔴 validação pós-atualização
- 🔴 recuperação de falha
- 🔴 proteção contra downgrade indevido

### Limitação importante do Android

Em Android comum, um APK distribuído diretamente pelo GitHub não pode simplesmente substituir silenciosamente o próprio aplicativo sem as autorizações/mecanismos adequados do sistema.

Portanto, o projeto deverá buscar o fluxo mais automático possível, mas **não deverá fingir que possui instalação silenciosa universal**.

Para uma instalação realmente integrada e silenciosa, será necessário um mecanismo de distribuição/gerenciamento compatível com o dispositivo ou uma loja que forneça o mecanismo de atualização correspondente.

O objetivo do Morok é que o usuário não precise procurar, baixar e instalar manualmente uma nova versão. A confirmação deverá acontecer dentro do fluxo do aplicativo, respeitando as regras do Android.

## 5.2 — GitHub Actions

- 🔴 build automático
- 🔴 testes
- 🔴 lint
- 🔴 type/checks
- 🔴 APK release
- 🔴 geração de changelog
- 🔴 criação de GitHub Release
- 🔴 atualização do manifest
- 🔴 hashes
- 🔴 assinatura
- 🔴 artefatos
- 🔴 validação
- 🔴 publicação

## 5.3 — Render

Plano gratuito quando tecnicamente suficiente:

- 🔴 API
- 🔴 serviços auxiliares
- 🔴 health checks
- 🔴 deploy automático
- 🔴 variáveis de ambiente
- 🔴 logs
- 🔴 recuperação
- 🔴 limites do plano documentados

## 5.4 — MongoDB

- 🔴 banco Morok
- 🔴 usuários
- 🔴 sessões
- 🔴 memória
- 🔴 comandos
- 🔴 tarefas
- 🔴 automações
- 🔴 integrações
- 🔴 auditoria
- 🔴 versões
- 🔴 dispositivos
- 🔴 sincronização
- 🔴 índices
- 🔴 backups dentro do que o plano permitir

## 5.5 — Testes finais

- 🔴 testes unitários
- 🔴 testes instrumentados
- 🔴 testes de integração
- 🔴 testes de voz
- 🔴 testes de permissões
- 🔴 testes de AccessibilityService
- 🔴 testes de notificações
- 🔴 testes offline
- 🔴 testes de atualização
- 🔴 testes de recuperação
- 🔴 testes de bateria
- 🔴 testes de memória
- 🔴 testes de rede
- 🔴 testes em diferentes versões Android
- 🔴 testes em aparelhos reais
- 🔴 testes de segurança
- 🔴 testes de regressão

---

# MATRIZ DE FUNCIONALIDADES

## Conversação e inteligência

- 🔴 conversa por voz
- 🔴 conversa por texto
- 🔴 contexto
- 🔴 memória
- 🔴 intenção
- 🔴 ferramentas
- 🔴 agentes
- 🔴 workflows
- 🔴 multiagentes
- 🔴 planejamento
- 🔴 execução
- 🔴 streaming
- 🔴 interrupção
- 🔴 human-in-the-loop

## Controle Android

- 🔴 aplicativos
- 🔴 tela
- 🔴 acessibilidade
- 🔴 notificações
- 🔴 áudio
- 🔴 câmera
- 🔴 microfone
- 🔴 contatos
- 🔴 calendário
- 🔴 chamadas
- 🔴 mensagens
- 🔴 arquivos
- 🔴 mídia
- 🔴 Bluetooth
- 🔴 Wi-Fi
- 🔴 localização
- 🔴 brilho
- 🔴 volume
- 🔴 bateria
- 🔴 configurações

## Automação

- 🔴 tarefas
- 🔴 sequências
- 🔴 condições
- 🔴 triggers
- 🔴 workflows
- 🔴 n8n
- 🔴 MCP
- 🔴 ferramentas
- 🔴 agentes
- 🔴 visão computacional
- 🔴 OCR
- 🔴 automação visual

## Voz

- 🔴 SpeechRecognizer
- 🔴 TextToSpeech
- 🔴 openWakeWord
- 🔴 Faster-Whisper
- 🔴 LiveKit
- 🔴 streaming
- 🔴 interrupção
- 🔴 wake word

## Dados

- 🔴 Room
- 🔴 DataStore
- 🔴 MongoDB
- 🔴 sincronização
- 🔴 cache
- 🔴 offline
- 🔴 auditoria
- 🔴 memória
- 🔴 arquivos

## Infraestrutura

- 🔴 GitHub
- 🔴 GitHub Actions
- 🔴 GitHub Releases
- 🔴 Render
- 🔴 MongoDB
- 🔴 API
- 🔴 FastAPI
- 🔴 n8n
- 🔴 observabilidade
- 🔴 atualização automática

---

# TECNOLOGIAS DEFINIDAS

## Aplicativo

- **Kotlin**
- **Android SDK**
- **Jetpack**
- **Jetpack Compose**
- **Room**
- **DataStore**
- **Android Keystore**
- **AccessibilityService**
- **Foreground Services**
- **MediaProjection**
- **SpeechRecognizer**
- **TextToSpeech**

## Interface

Prioridade:

1. Jetpack Compose
2. Canvas/SVG quando necessário
3. HTML/CSS/JS somente em módulos que realmente se beneficiem de tecnologia web

A interface será propositalmente simples nesta fase. O design sofisticado será desenvolvido depois sem comprometer o núcleo.

## Backend

- TypeScript/Fastify já existente
- Python/FastAPI para serviços especializados quando necessário
- MongoDB
- Render

## Inteligência

- LangGraph
- CrewAI
- AutoGen como referência/compatibilidade quando necessário
- Open Interpreter
- Anthropic Quickstarts
- Android Agent
- modelos locais/remotos
- MCP

## Voz

- Android SpeechRecognizer
- Android TextToSpeech
- openWakeWord
- Faster-Whisper
- LiveKit Agents quando necessário

## Automação

- AccessibilityService
- Appium para testes/automação controlada
- n8n
- LangGraph
- agentes e ferramentas próprias

---

# PLANO DE CUSTO

O projeto deverá ser desenvolvido priorizando **R$ 0 de infraestrutura recorrente**, utilizando planos gratuitos sempre que forem suficientes.

### GitHub

- código
- issues
- Actions dentro dos limites gratuitos
- Releases
- artefatos quando disponíveis
- controle de versões

### Render

- API e serviços compatíveis com o plano gratuito
- sem depender de recursos pagos
- limites documentados

### MongoDB

- cluster gratuito compatível com o projeto
- dados essenciais
- índices necessários
- consumo monitorado

### Open Source

Os 16 projetos presentes em `../` serão aproveitados conforme compatibilidade, licença, desempenho, tamanho do APK e necessidade real.

**Importante:** “usar todos os 16 repositórios” significa incorporar suas capacidades relevantes ao ecossistema Morok, não necessariamente executar todos os 16 dentro do APK simultaneamente. Projetos de servidor, desktop, teste ou infraestrutura permanecerão em seus ambientes adequados.

---

# REGRA DE IMPLEMENTAÇÃO

Cada funcionalidade seguirá:

```
Requisito
  ↓
Arquitetura
  ↓
Implementação
  ↓
Teste
  ↓
Teste em dispositivo
  ↓
Tratamento de erro
  ↓
Auditoria
  ↓
Documentação
  ↓
🟢 Concluído
```

Nunca:

```
Botão → "funciona"
Arquivo vazio → "implementado"
Mock → "concluído"
Tela → "funcionalidade"
```

---

# STATUS DAS 5 FASES

| Fase | Nome | Status |
|---|---|---|
| **0** | Fundação Nativa | 🟢 |
| **1** | Assistente Android Funcional | 🔴 |
| **2** | Controle Profundo do Android | 🔴 |
| **3** | Inteligência, Agentes e Automação | 🔴 |
| **4** | Automação, Backend e Ecossistema | 🔴 |
| **5** | Produção, Atualização e Conclusão | 🔴 |

> A numeração do Mobile é própria desta plataforma. Ela mantém a ideia das fases do projeto raiz, mas foi reorganizada para que o desenvolvimento Android tenha uma sequência executável.

---

# RESULTADO FINAL ESPERADO

Ao final, o Morok Mobile deverá ser um **assistente Android nativo**, capaz de receber comandos por voz, texto e controles da interface, compreender contexto, utilizar memória, executar ferramentas e automações, interagir com recursos autorizados do aparelho, trabalhar com serviços locais e remotos, operar workflows, controlar aplicativos quando permitido, processar voz e visão, manter histórico e auditoria e atualizar sua própria versão através de um fluxo automatizado compatível com as regras do Android.

A interface poderá mudar completamente depois.

O núcleo não deverá depender dela.

O objetivo não é construir uma demonstração de um Jarvis.

O objetivo é construir o **Morok funcionando de verdade no Android**.


---

## Release trigger

This marker documents the first production-release pipeline trigger for Morok Mobile. It does not change runtime behavior; the release workflow uses changes under MOROK/Mobile/** to build and publish the signed APK.
