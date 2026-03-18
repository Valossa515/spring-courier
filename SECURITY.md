# 🔒 Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.3.x   | ✅ Yes             |
| 1.2.x   | ✅ Yes             |
| < 1.2   | ❌ No              |

## Reporting a Vulnerability

The security of Spring Courier is taken seriously. If you discover a security vulnerability, please report it responsibly.

### How to Report

**⚠️ DO NOT open a public issue for security vulnerabilities.**

Instead:

1. **Send an email** to **fe.mmo515@gmail.com** with the vulnerability details
2. Or use the **[Security Advisories](https://github.com/Valossa515/spring-courier/security/advisories/new)** feature on GitHub

### What to Include in Your Report

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Affected Spring Courier version
- Suggested fix (if any)

### What to Expect

- **Acknowledgment of receipt** within 48 hours
- **Initial assessment** within 7 days
- **Fix and release** according to severity:
  - 🔴 **Critical**: Fix and release as soon as possible
  - 🟠 **High**: Fix in the next patch release
  - 🟡 **Medium**: Fix in the next minor release
  - 🟢 **Low**: Fix in a future release

### Responsible Disclosure

We ask that you:

- Give us a reasonable amount of time to fix the vulnerability before disclosing it publicly
- Do not exploit the vulnerability beyond what is necessary to demonstrate it
- Do not access or modify other users' data

### Acknowledgment

Contributors who report valid security vulnerabilities will be acknowledged (with permission) in the release notes and the project's README.

## Security Best Practices

When using Spring Courier in your project:

- Always keep the library up to date with the latest version
- Validate inputs using `ValidationBehavior` before processing
- Do not expose internal error messages in production environments
- Use `Response<T>` to safely encapsulate responses — only `CourierException` messages are propagated; other exceptions return a generic message
