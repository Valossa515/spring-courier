> 🌐 **Language / Idioma:** 🇧🇷 [Português](README.pt-BR.md) | 🇺🇸 **English** (current)

# 📊 Spring Courier Diagrams

This folder contains UML diagrams that represent the architecture and workings of the Spring Courier library.

## 📋 Diagram List

### 0. 🎯 Architecture Overview (`architecture-overview.puml`)

Simplified diagram showing the basic flow of the library:
- Client → Controller → Courier → Pipeline → Handler → Storage
- 12-step numbered flow
- Explanatory notes about each component

**When to use**: As a starting point to quickly understand how the library works.

---

### 1. 🏗️ Class Diagram (`class-diagram.puml`)

Represents the class and interface structure of the library, showing:
- Base interfaces: `IRequest`, `ICommand`, `IQuery`, `INotification`
- Handler interfaces: `CommandHandler`, `QueryHandler`, `NotificationHandler`
- Core components: `Courier`, `Response`, registries
- Pipeline components: `PipelineBehavior`, `PipelineExecutor`, `PipelineRegistry`
- Validation components: `ValidationBehavior`, `Validator`, `ValidationResult`
- Discovery components: `HandlerDiscoveryPostProcessor`, `NotificationDiscoveryPostProcessor`, `BehaviorDiscoveryPostProcessor`
- Configuration: `CourierAutoConfiguration`

**When to use**: To understand the overall structure of the library and how classes relate to each other.

---

### 2. 🔄 Sequence Diagram - Command/Query (`sequence-diagram-command.puml`)

Shows the detailed execution flow of a Command or Query, including:
- Client sending request via Controller
- Handler lookup in the registry
- Validation pipeline execution
- Handler invocation
- Response return

**When to use**: To understand the complete processing flow of commands and queries.

---

### 3. 📢 Sequence Diagram - Notifications (`sequence-diagram-notification.puml`)

Demonstrates how notifications/events are published to multiple handlers:
- Synchronous publishing (`publish`)
- Asynchronous publishing (`publishAsync`)
- Multiple handler execution
- Error handling in individual handlers

**When to use**: To understand how to implement event-driven architecture with the library.

---

### 4. 📋 Activity Diagram (`activity-diagram.puml`)

Represents the activity flow in request processing:
- Request reception
- Pipeline validation
- Behavior execution
- Handler invocation
- Error handling
- Response return

**When to use**: To visualize the logical flow and decisions in request processing.

---

### 5. 👤 Use Case Diagram (`use-case-diagram.puml`)

Shows the main use cases of the library from the developer's perspective:
- CQRS operations (send commands, queries, notifications)
- Handler management
- Pipeline configuration
- Validation
- Spring integration

**When to use**: To understand the available features and how to use them.

---

### 6. 🧩 Component Diagram (`component-diagram.puml`)

Represents the component architecture and their interactions:
- Spring Boot application
- Spring Courier library components
- Spring Framework
- External systems (database, email, cache, etc.)
- Relationships and dependencies

**When to use**: To understand the overall architecture and how the library integrates with Spring Boot.

---

### 7. 🚀 Deployment Diagram (`deployment-diagram.puml`)

Shows how the library is deployed in a production environment:
- Application JAR structure
- Dependencies and libraries
- Runtime environment (JVM)
- Maven Central integration
- External systems

**When to use**: To understand how the library is distributed and deployed.

---

### 8. 🔀 State Diagram (`state-diagram.puml`)

Represents the lifecycle of a request through different states:
- Request creation
- Handler lookup
- Pipeline execution
- Validation
- Handler invocation
- Response creation
- Success and error states

**When to use**: To understand state transitions during request processing.

---

## 🛠️ How to View the Diagrams

The diagrams are in PlantUML format (`.puml`). To view them:

### Option 1: Online
Use the [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/):
1. Copy the contents of the `.puml` file
2. Paste it in the online editor
3. View the rendered diagram

### Option 2: VS Code
Install the [PlantUML extension for VS Code](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml):
1. Open the `.puml` file in VS Code
2. Use `Alt+D` for preview
3. Or right-click → "Preview Current Diagram"

### Option 3: IntelliJ IDEA
Install the [PlantUML Integration](https://plugins.jetbrains.com/plugin/7017-plantuml-integration) plugin:
1. Open the `.puml` file in IntelliJ
2. The preview will appear automatically beside it

### Option 4: Command Line
Install PlantUML locally:
```bash
# Install PlantUML (requires Java)
brew install plantuml  # macOS
apt-get install plantuml  # Ubuntu/Debian

# Generate PNG image
plantuml diagram.puml

# Generate SVG
plantuml -tsvg diagram.puml
```

---

## 📚 Additional Documentation

For more information about the library, see:
- [README.md](../../README.md) - Main documentation
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Contribution guide
- Source code at `src/main/java/io/github/valossa515/spring_courier/`

---

## 🤝 Contributing to Diagrams

If you wish to add or improve diagrams:

1. Use PlantUML to maintain consistency
2. Follow the naming pattern: `descriptive-name.puml`
3. Add a description in this README
4. Use simple themes and consistent colors
5. Add explanatory notes where appropriate

---

## 📝 License

The diagrams follow the same MIT license as the main project.
