# 📊 Diagramas do Spring Courier

Esta pasta contém os diagramas UML que representam a arquitetura e o funcionamento da biblioteca Spring Courier.

## 📋 Lista de Diagramas

### 0. 🎯 Visão Geral da Arquitetura (`architecture-overview.puml`)

Diagrama simplificado mostrando o fluxo básico da biblioteca:
- Cliente → Controller → Courier → Pipeline → Handler → Storage
- Fluxo numerado de 12 passos
- Notas explicativas sobre cada componente

**Quando usar**: Como ponto de partida para entender rapidamente como a biblioteca funciona.

---

### 1. 🏗️ Diagrama de Classes (`class-diagram.puml`)

Representa a estrutura de classes e interfaces da biblioteca, mostrando:
- Interfaces base: `IRequest`, `ICommand`, `IQuery`, `INotification`
- Interfaces de handlers: `CommandHandler`, `QueryHandler`, `NotificationHandler`
- Componentes core: `Courier`, `Response`, registries
- Componentes de pipeline: `PipelineBehavior`, `PipelineExecutor`, `PipelineRegistry`
- Componentes de validação: `ValidationBehavior`, `Validator`, `ValidationResult`
- Componentes de descoberta: `HandlerDiscoveryPostProcessor`, `NotificationDiscoveryPostProcessor`, `BehaviorDiscoveryPostProcessor`
- Configuração: `CourierAutoConfiguration`

**Quando usar**: Para entender a estrutura geral da biblioteca e como as classes se relacionam.

---

### 2. 🔄 Diagrama de Sequência - Command/Query (`sequence-diagram-command.puml`)

Mostra o fluxo detalhado de execução de um Command ou Query, incluindo:
- Cliente enviando request via Controller
- Busca do handler no registry
- Execução do pipeline de validação
- Invocação do handler
- Retorno da resposta

**Quando usar**: Para entender o fluxo completo de processamento de commands e queries.

---

### 3. 📢 Diagrama de Sequência - Notificações (`sequence-diagram-notification.puml`)

Demonstra como as notificações/eventos são publicados para múltiplos handlers:
- Publicação síncrona (`publish`)
- Publicação assíncrona (`publishAsync`)
- Execução de múltiplos handlers
- Tratamento de erros em handlers individuais

**Quando usar**: Para entender como implementar event-driven architecture com a biblioteca.

---

### 4. 📋 Diagrama de Atividades (`activity-diagram.puml`)

Representa o fluxo de atividades no processamento de um request:
- Recebimento do request
- Validação no pipeline
- Execução de behaviors
- Invocação do handler
- Tratamento de erros
- Retorno da resposta

**Quando usar**: Para visualizar o fluxo lógico e decisões no processamento de requests.

---

### 5. 👤 Diagrama de Casos de Uso (`use-case-diagram.puml`)

Mostra os principais casos de uso da biblioteca do ponto de vista do desenvolvedor:
- Operações CQRS (enviar commands, queries, notificações)
- Gerenciamento de handlers
- Configuração de pipeline
- Validação
- Integração com Spring

**Quando usar**: Para entender as funcionalidades disponíveis e como utilizá-las.

---

### 6. 🧩 Diagrama de Componentes (`component-diagram.puml`)

Representa a arquitetura de componentes e suas interações:
- Aplicação Spring Boot
- Componentes da biblioteca Spring Courier
- Spring Framework
- Sistemas externos (database, email, cache, etc.)
- Relacionamentos e dependências

**Quando usar**: Para entender a arquitetura geral e como a biblioteca se integra com Spring Boot.

---

### 7. 🚀 Diagrama de Implantação (`deployment-diagram.puml`)

Mostra como a biblioteca é implantada em um ambiente de produção:
- Estrutura do JAR da aplicação
- Dependências e bibliotecas
- Runtime environment (JVM)
- Integração com Maven Central
- Sistemas externos

**Quando usar**: Para entender como a biblioteca é distribuída e implantada.

---

### 8. 🔀 Diagrama de Estados (`state-diagram.puml`)

Representa o ciclo de vida de um request através dos diferentes estados:
- Criação do request
- Busca do handler
- Execução do pipeline
- Validação
- Invocação do handler
- Criação da resposta
- Estados de sucesso e erro

**Quando usar**: Para entender as transições de estado durante o processamento de requests.

---

## 🛠️ Como Visualizar os Diagramas

Os diagramas estão em formato PlantUML (`.puml`). Para visualizá-los:

### Opção 1: Online
Use o [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/):
1. Copie o conteúdo do arquivo `.puml`
2. Cole no editor online
3. Visualize o diagrama renderizado

### Opção 2: VS Code
Instale a extensão [PlantUML para VS Code](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml):
1. Abra o arquivo `.puml` no VS Code
2. Use `Alt+D` para preview
3. Ou clique com botão direito → "Preview Current Diagram"

### Opção 3: IntelliJ IDEA
Instale o plugin [PlantUML Integration](https://plugins.jetbrains.com/plugin/7017-plantuml-integration):
1. Abra o arquivo `.puml` no IntelliJ
2. O preview aparecerá automaticamente ao lado

### Opção 4: Linha de Comando
Instale o PlantUML localmente:
```bash
# Instalar PlantUML (requer Java)
brew install plantuml  # macOS
apt-get install plantuml  # Ubuntu/Debian

# Gerar imagem PNG
plantuml diagram.puml

# Gerar SVG
plantuml -tsvg diagram.puml
```

---

## 📚 Documentação Adicional

Para mais informações sobre a biblioteca, consulte:
- [README.md](../../README.md) - Documentação principal
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Guia de contribuição
- Código fonte em `src/main/java/io/github/valossa515/spring_courier/`

---

## 🤝 Contribuindo com Diagramas

Se você deseja adicionar ou melhorar os diagramas:

1. Use PlantUML para manter consistência
2. Siga o padrão de nomenclatura: `nome-descritivo.puml`
3. Adicione uma descrição neste README
4. Use temas simples e cores consistentes
5. Adicione notas explicativas quando apropriado

---

## 📝 Licença

Os diagramas seguem a mesma licença MIT do projeto principal.
