> 🌐 **Language / Idioma:** 🇧🇷 [Português](CONTRIBUTING.pt-BR.md) | 🇺🇸 **English** (current)

# 🤝 Contributing to Spring Courier

Thank you for considering contributing to Spring Courier! This document provides guidelines for contributions and the release process.

## 🚀 How to Contribute

### 1. Fork and Clone
```bash
git clone https://github.com/your-username/spring-courier.git
cd spring-courier
```

### 2. Create a Branch
```bash
git checkout -b feature/new-feature
```

### 3. Make Your Changes
- Write clean, well-documented code
- Add tests for new features
- Follow existing code conventions

### 4. Test Your Changes
```bash
./mvnw clean test
```

### 5. Commit and Push
```bash
git commit -m "feat: add new feature"
git push origin feature/new-feature
```

### 6. Open a Pull Request
Describe your changes and the problem they solve.

---

## 📦 Release Process (For Maintainers)

### Prerequisites

To publish a new version to Maven Central, you need to configure the following **GitHub Secrets** in the repository:

| Secret | Description | How to Obtain |
|--------|-------------|---------------|
| `GPG_PRIVATE_KEY` | GPG private key in ASCII armored format | Export your GPG key (see below) |
| `GPG_PASSPHRASE` | GPG key passphrase | The passphrase you set when creating the GPG key |
| `SONATYPE_TOKEN` | Sonatype Central Portal access token | Generate at https://central.sonatype.com/account |

### 🔐 Setting Up the GPG Key

#### 1. Generate a GPG Key (if you don't have one yet)

```bash
gpg --full-generate-key
```

Choose:
- Type: RSA and RSA
- Size: 4096 bits
- Validity: 0 (does not expire) or your desired period
- Name and email: Use the same ones from your Maven Central profile

#### 2. List GPG Keys

```bash
gpg --list-secret-keys --keyid-format=long
```

Example output:
```
sec   rsa4096/ABCD1234EFGH5678 2024-01-01 [SC]
      1234567890ABCDEF1234567890ABCDEF12345678
uid                 [ultimate] Your Name <your@email.com>
```

The key ID is `ABCD1234EFGH5678`.

#### 3. Export the Private Key

```bash
gpg --armor --export-secret-keys ABCD1234EFGH5678 > private-key.asc
```

#### 4. Publish the Public Key (Required for Maven Central)

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys ABCD1234EFGH5678
gpg --keyserver keys.openpgp.org --send-keys ABCD1234EFGH5678
```

#### 5. Add to GitHub Secrets

1. Go to: `Settings` > `Secrets and variables` > `Actions`
2. Click `New repository secret`
3. Add each secret:
   - **Name**: `GPG_PRIVATE_KEY`
   - **Value**: Paste the full contents of the `private-key.asc` file

### 🎯 Setting Up Sonatype Central Portal

#### 1. Create an Account

1. Go to https://central.sonatype.com/
2. Log in with GitHub, Google, or create an account
3. Verify your domain (e.g., `io.github.valossa515`)

#### 2. Generate an Access Token

1. Go to: https://central.sonatype.com/account
2. Click "Generate User Token"
3. Copy the generated token
4. Add it to GitHub Secrets as `SONATYPE_TOKEN`

### 📋 Making a Release

#### Option 1: Via GitHub Release (Recommended)

1. **Bump the version across the whole reactor** (parent POM + every module).
   Use the helper script (or the Maven Versions plugin) so the parent and all
   `<module>` `<parent>` references stay in sync — do not hand-edit a single
   `pom.xml`:
   ```bash
   ./scripts/bump-version.sh 1.0.0
   # equivalent to:
   # ./mvnw versions:set -DnewVersion=1.0.0 -DprocessAllModules=true -DgenerateBackupPoms=false
   ```

2. **Commit and Push**:
   ```bash
   git add pom.xml '**/pom.xml' README.md README.pt-BR.md CLAUDE.md
   git commit -m "chore: bump version to 1.0.0"
   git push origin main
   ```

3. **Create a GitHub Release**:
   - Go to: `Releases` > `Create a new release`
   - Tag: `v1.0.0`
   - Title: `Spring Courier v1.0.0`
   - Description: Describe the changes
   - Click `Publish release`

4. **The GitHub Action will be triggered automatically** and publish to Maven Central.

#### Option 2: Via Workflow Dispatch (Manual)

1. Go to: `Actions` > `Publish to Maven Central`
2. Click `Run workflow`
3. Optionally, specify the version
4. Click `Run workflow`

### ✅ Verify Publication

After the workflow completes:

1. Check the status at: https://central.sonatype.com/publishing
2. Publication may take from 15 minutes to a few hours to appear on Maven Central
3. Verify at: https://central.sonatype.com/artifact/io.github.valossa515/spring-courier

### 📝 Release Checklist

- [ ] All changes have been tested
- [ ] The version has been updated across the reactor (parent + all modules)
- [ ] The CHANGELOG has been updated (if applicable)
- [ ] Tests are passing
- [ ] The release has been created on GitHub
- [ ] The publish workflow has been executed successfully
- [ ] The new version appears on Maven Central

---

## 🐛 Reporting Bugs

Use [GitHub Issues](https://github.com/Valossa515/spring-courier/issues) to report bugs. Include:

- Detailed description of the problem
- Steps to reproduce
- Spring Courier version
- Java and Spring Boot version
- Stack trace (if applicable)

---

## 💡 Feature Suggestions

Suggestions are welcome! Open an [Issue](https://github.com/Valossa515/spring-courier/issues) describing:

- The problem the feature solves
- How the feature should work
- Usage examples (if possible)

---

## 📜 Code of Conduct

Be respectful and constructive in all interactions. Contributions from everyone are welcome!

---

## ❤️ Thank You!

Your contributions help make Spring Courier better for everyone!
