# 🚀 Guia Rápido de Visualização dos Diagramas

Este guia ajudará você a visualizar rapidamente os diagramas do Spring Courier.

## ⚡ Método Mais Rápido: Online

1. Acesse: http://www.plantuml.com/plantuml/uml/
2. Copie o conteúdo de qualquer arquivo `.puml` desta pasta
3. Cole no campo de texto
4. Clique em "Submit" para ver o diagrama renderizado

### 🔗 Links Diretos (GitHub)

Você pode visualizar os diagramas diretamente no GitHub usando o PlantUML Proxy:

- [Visão Geral](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/architecture-overview.puml)
- [Diagrama de Classes](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/class-diagram.puml)
- [Sequência - Command/Query](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/sequence-diagram-command.puml)
- [Sequência - Notificações](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/sequence-diagram-notification.puml)
- [Diagrama de Atividades](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/activity-diagram.puml)
- [Casos de Uso](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/use-case-diagram.puml)
- [Diagrama de Componentes](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/component-diagram.puml)
- [Diagrama de Implantação](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/deployment-diagram.puml)
- [Diagrama de Estados](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/state-diagram.puml)

> **Nota**: Após o merge para a branch `main`, os links acima funcionarão automaticamente!

---

## 💻 Visualização Local

### Opção 1: VS Code (Recomendado)

1. Instale a extensão: [PlantUML - jebbs](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml)
2. Abra qualquer arquivo `.puml`
3. Pressione `Alt+D` ou use o comando `PlantUML: Preview Current Diagram`
4. O diagrama será renderizado em tempo real ao lado do código

### Opção 2: IntelliJ IDEA

1. Instale o plugin: **PlantUML Integration**
   - `File` → `Settings` → `Plugins` → Busque "plantuml4idea"
   - instale o graphviz
   - vá ate `Settings` → `Languages & Frameworks` → Clique em "PlantUML" → Cole o diretório onde o graphviz foi instalado.
2. Abra qualquer arquivo `.puml`
3. O preview aparecerá automaticamente no painel lateral

### Opção 3: Linha de Comando

```bash
# Instalar PlantUML (requer Java instalado)

# macOS (usando Homebrew)
brew install plantuml

# Ubuntu/Debian
sudo apt-get install plantuml

# Gerar PNG de todos os diagramas
cd docs/diagrams
plantuml *.puml

# Gerar SVG (vetorizado, melhor qualidade)
plantuml -tsvg *.puml

# Gerar PDF
plantuml -tpdf *.puml
```

Os arquivos de imagem serão gerados na mesma pasta dos `.puml`.

---

## 🎨 Ferramentas Adicionais

### PlantUML QEditor
- **Download**: http://qeditor.gforge.inria.fr/
- Editor standalone com preview em tempo real
- Multiplataforma (Windows, Mac, Linux)

### draw.io / diagrams.net
- Não suporta PlantUML nativamente, mas você pode importar PNGs gerados

### Visual Paradigm
- Suporta importação de PlantUML
- Versão Community gratuita disponível

---

## 🔧 Dicas

### Exportar para Documentação

```bash
# Gerar todos como PNG em alta resolução
plantuml -tpng -DPLANTUML_LIMIT_SIZE=8192 *.puml

# Gerar SVG para documentação web
plantuml -tsvg *.puml

# Gerar em múltiplos formatos
plantuml -tpng -tsvg *.puml
```

### Integração com CI/CD

Você pode adicionar geração automática de diagramas no CI:

```yaml
# .github/workflows/generate-diagrams.yml
name: Generate Diagrams
on: [push]
jobs:
  diagrams:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Generate PlantUML Diagrams
        uses: cloudbees/plantuml-github-action@master
        with:
          args: -v -tsvg docs/diagrams/*.puml
```

### Incluir em Markdown

```markdown
<!-- Usando proxy público -->
![Architecture](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/architecture-overview.puml)

<!-- Ou usando imagem gerada -->
![Architecture](./architecture-overview.png)
```

---

## 📚 Recursos

- [PlantUML Official Site](https://plantuml.com/)
- [PlantUML Language Reference](https://plantuml.com/guide)
- [Real World PlantUML Examples](https://real-world-plantuml.com/)
- [PlantUML Theme Gallery](https://bschwarz.github.io/puml-themes/)

---

## ❓ Problemas Comuns

### "Java not found"
PlantUML requer Java. Instale:
```bash
# macOS
brew install openjdk

# Ubuntu
sudo apt-get install default-jre
```

### "Graphviz not found"
Alguns diagramas avançados precisam do Graphviz:
```bash
# macOS
brew install graphviz

# Ubuntu
sudo apt-get install graphviz
```

### Diagrama não renderiza no VS Code
1. Verifique se a extensão PlantUML está instalada
2. Verifique se Java está instalado: `java -version`
3. Reinicie o VS Code

---

## 🤝 Contribuindo

Para adicionar ou modificar diagramas:

1. Edite os arquivos `.puml` existentes ou crie novos
2. Teste localmente antes de commitar
3. Mantenha o estilo e tema consistentes
4. Adicione descrição no `README.md` principal
5. Abra um Pull Request

---

Desenvolvido com ❤️ para o projeto Spring Courier
