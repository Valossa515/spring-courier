> 🌐 **Language / Idioma:** 🇧🇷 [Português](QUICK_START.pt-BR.md) | 🇺🇸 **English** (current)

# 🚀 Quick Guide to Viewing Diagrams

This guide will help you quickly view the Spring Courier diagrams.

## ⚡ Fastest Method: Online

1. Go to: http://www.plantuml.com/plantuml/uml/
2. Copy the contents of any `.puml` file from this folder
3. Paste it in the text field
4. Click "Submit" to see the rendered diagram

### 🔗 Direct Links (GitHub)

You can view the diagrams directly on GitHub using the PlantUML Proxy:

- [Architecture Overview](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/architecture-overview.puml)
- [Class Diagram](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/class-diagram.puml)
- [Sequence - Command/Query](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/sequence-diagram-command.puml)
- [Sequence - Notifications](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/sequence-diagram-notification.puml)
- [Activity Diagram](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/activity-diagram.puml)
- [Use Cases](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/use-case-diagram.puml)
- [Component Diagram](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/component-diagram.puml)
- [Deployment Diagram](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/deployment-diagram.puml)
- [State Diagram](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/state-diagram.puml)

> **Note**: After merging to the `main` branch, the links above will work automatically!

---

## 💻 Local Viewing

### Option 1: VS Code (Recommended)

1. Install the extension: [PlantUML - jebbs](https://marketplace.visualstudio.com/items?itemName=jebbs.plantuml)
2. Open any `.puml` file
3. Press `Alt+D` or use the command `PlantUML: Preview Current Diagram`
4. The diagram will be rendered in real time alongside the code

### Option 2: IntelliJ IDEA

1. Install the plugin: **PlantUML Integration**
   - `File` → `Settings` → `Plugins` → Search for "plantuml4idea"
   - Install Graphviz
   - Go to `Settings` → `Languages & Frameworks` → Click "PlantUML" → Paste the directory where Graphviz was installed.
2. Open any `.puml` file
3. The preview will appear automatically in the side panel

### Option 3: Command Line

```bash
# Install PlantUML (requires Java)

# macOS (using Homebrew)
brew install plantuml

# Ubuntu/Debian
sudo apt-get install plantuml

# Generate PNG for all diagrams
cd docs/diagrams
plantuml *.puml

# Generate SVG (vectorized, better quality)
plantuml -tsvg *.puml

# Generate PDF
plantuml -tpdf *.puml
```

Image files will be generated in the same folder as the `.puml` files.

---

## 🎨 Additional Tools

### PlantUML QEditor
- **Download**: http://qeditor.gforge.inria.fr/
- Standalone editor with real-time preview
- Cross-platform (Windows, Mac, Linux)

### draw.io / diagrams.net
- Does not natively support PlantUML, but you can import generated PNGs

### Visual Paradigm
- Supports PlantUML import
- Free Community version available

---

## 🔧 Tips

### Export for Documentation

```bash
# Generate all as high-resolution PNGs
plantuml -tpng -DPLANTUML_LIMIT_SIZE=8192 *.puml

# Generate SVG for web documentation
plantuml -tsvg *.puml

# Generate in multiple formats
plantuml -tpng -tsvg *.puml
```

### CI/CD Integration

You can add automatic diagram generation in CI:

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

### Include in Markdown

```markdown
<!-- Using public proxy -->
![Architecture](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/Valossa515/spring-courier/main/docs/diagrams/architecture-overview.puml)

<!-- Or using a generated image -->
![Architecture](./architecture-overview.png)
```

---

## 📚 Resources

- [PlantUML Official Site](https://plantuml.com/)
- [PlantUML Language Reference](https://plantuml.com/guide)
- [Real World PlantUML Examples](https://real-world-plantuml.com/)
- [PlantUML Theme Gallery](https://bschwarz.github.io/puml-themes/)

---

## ❓ Common Issues

### "Java not found"
PlantUML requires Java. Install it:
```bash
# macOS
brew install openjdk

# Ubuntu
sudo apt-get install default-jre
```

### "Graphviz not found"
Some advanced diagrams require Graphviz:
```bash
# macOS
brew install graphviz

# Ubuntu
sudo apt-get install graphviz
```

### Diagram not rendering in VS Code
1. Check that the PlantUML extension is installed
2. Check that Java is installed: `java -version`
3. Restart VS Code

---

## 🤝 Contributing

To add or modify diagrams:

1. Edit existing `.puml` files or create new ones
2. Test locally before committing
3. Keep the style and theme consistent
4. Add a description in the main `README.md`
5. Open a Pull Request

---

Made with ❤️ for the Spring Courier project
