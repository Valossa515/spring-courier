# 🤝 Contributing to Spring Courier

Obrigado por considerar contribuir com o Spring Courier! Este documento fornece diretrizes para contribuições e o processo de release.

## 🚀 Como Contribuir

### 1. Fork e Clone
```bash
git clone https://github.com/seu-usuario/spring-courier.git
cd spring-courier
```

### 2. Crie uma Branch
```bash
git checkout -b feature/nova-funcionalidade
```

### 3. Faça suas Alterações
- Escreva código limpo e bem documentado
- Adicione testes para novas funcionalidades
- Siga as convenções de código existentes

### 4. Teste suas Alterações
```bash
./mvnw clean test
```

### 5. Commit e Push
```bash
git commit -m "feat: adiciona nova funcionalidade"
git push origin feature/nova-funcionalidade
```

### 6. Abra um Pull Request
Descreva suas alterações e o problema que elas resolvem.

---

## 📦 Processo de Release (Para Maintainers)

### Pré-requisitos

Para publicar uma nova versão no Maven Central, você precisa configurar os seguintes **GitHub Secrets** no repositório:

| Secret | Descrição | Como Obter |
|--------|-----------|------------|
| `GPG_PRIVATE_KEY` | Chave GPG privada em formato ASCII armored | Exportar sua chave GPG (veja abaixo) |
| `GPG_PASSPHRASE` | Senha da chave GPG | A senha que você definiu ao criar a chave GPG |
| `SONATYPE_TOKEN` | Token de acesso do Sonatype Central Portal | Gerar em https://central.sonatype.com/account |

### 🔐 Configurando a Chave GPG

#### 1. Gerar uma Chave GPG (se ainda não tiver)

```bash
gpg --full-generate-key
```

Escolha:
- Tipo: RSA and RSA
- Tamanho: 4096 bits
- Validade: 0 (não expira) ou o período desejado
- Nome e e-mail: Use os mesmos do perfil Maven Central

#### 2. Listar Chaves GPG

```bash
gpg --list-secret-keys --keyid-format=long
```

Saída exemplo:
```
sec   rsa4096/ABCD1234EFGH5678 2024-01-01 [SC]
      1234567890ABCDEF1234567890ABCDEF12345678
uid                 [ultimate] Seu Nome <seu@email.com>
```

O ID da chave é `ABCD1234EFGH5678`.

#### 3. Exportar a Chave Privada

```bash
gpg --armor --export-secret-keys ABCD1234EFGH5678 > private-key.asc
```

#### 4. Publicar a Chave Pública (Necessário para Maven Central)

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EFGH5678
gpg --keyserver keys.openpgp.org --send-keys ABCD1234EFGH5678
```

#### 5. Adicionar ao GitHub Secrets

1. Vá em: `Settings` > `Secrets and variables` > `Actions`
2. Clique em `New repository secret`
3. Adicione cada secret:
   - **Nome**: `GPG_PRIVATE_KEY`
   - **Value**: Cole o conteúdo completo do arquivo `private-key.asc`

### 🎯 Configurando Sonatype Central Portal

#### 1. Criar Conta

1. Acesse https://central.sonatype.com/
2. Faça login com GitHub, Google ou crie uma conta
3. Verifique seu domínio (ex: `io.github.valossa515`)

#### 2. Gerar Token de Acesso

1. Vá em: https://central.sonatype.com/account
2. Clique em "Generate User Token"
3. Copie o Token gerado
4. Adicione ao GitHub Secrets como `SONATYPE_TOKEN`

### 📋 Fazendo uma Release

#### Opção 1: Via GitHub Release (Recomendado)

1. **Atualize a versão no `pom.xml`**:
   ```xml
   <version>1.0.0</version>
   ```

2. **Commit e Push**:
   ```bash
   git add pom.xml
   git commit -m "chore: bump version to 1.0.0"
   git push origin main
   ```

3. **Crie uma Release no GitHub**:
   - Vá em: `Releases` > `Create a new release`
   - Tag: `v1.0.0`
   - Title: `Spring Courier v1.0.0`
   - Description: Descreva as mudanças
   - Clique em `Publish release`

4. **GitHub Action será disparada automaticamente** e publicará no Maven Central.

#### Opção 2: Via Workflow Dispatch (Manual)

1. Vá em: `Actions` > `Publish to Maven Central`
2. Clique em `Run workflow`
3. Opcionalmente, especifique a versão
4. Clique em `Run workflow`

### ✅ Verificar Publicação

Após a execução do workflow:

1. Verifique o status em: https://central.sonatype.com/publishing
2. A publicação pode levar de 15 minutos a algumas horas para aparecer no Maven Central
3. Verifique em: https://central.sonatype.com/artifact/io.github.valossa515/spring-courier

### 📝 Checklist de Release

- [ ] Todas as alterações foram testadas
- [ ] A versão foi atualizada no `pom.xml`
- [ ] O CHANGELOG foi atualizado (se houver)
- [ ] Os testes estão passando
- [ ] A release foi criada no GitHub
- [ ] O workflow de publicação foi executado com sucesso
- [ ] A nova versão aparece no Maven Central

---

## 🐛 Reportando Bugs

Use as [GitHub Issues](https://github.com/Valossa515/spring-courier/issues) para reportar bugs. Inclua:

- Descrição detalhada do problema
- Passos para reproduzir
- Versão do Spring Courier
- Versão do Java e Spring Boot
- Stack trace (se aplicável)

---

## 💡 Sugestões de Funcionalidades

Sugestões são bem-vindas! Abra uma [Issue](https://github.com/Valossa515/spring-courier/issues) descrevendo:

- O problema que a funcionalidade resolve
- Como a funcionalidade deveria funcionar
- Exemplos de uso (se possível)

---

## 📜 Código de Conduta

Seja respeitoso e construtivo em todas as interações. Contribuições de todos são bem-vindas!

---

## ❤️ Obrigado!

Suas contribuições ajudam a tornar o Spring Courier melhor para todos!
