# 🔒 Política de Segurança

## Versões Suportadas

| Versão | Suportada          |
| ------ | ------------------ |
| 1.3.x  | ✅ Sim             |
| 1.2.x  | ✅ Sim             |
| < 1.2  | ❌ Não             |

## Reportando uma Vulnerabilidade

A segurança do Spring Courier é levada a sério. Se você descobrir uma vulnerabilidade de segurança, 
por favor reporte de forma responsável.

### Como Reportar

**⚠️ NÃO abra uma issue pública para vulnerabilidades de segurança.**

Em vez disso:

1. **Envie um e-mail** para **valossa515@github.com** com os detalhes da vulnerabilidade
2. Ou use a funcionalidade **[Security Advisories](https://github.com/Valossa515/spring-courier/security/advisories/new)** do GitHub

### O que incluir no relatório

- Descrição da vulnerabilidade
- Passos para reprodução
- Impacto potencial
- Versão afetada do Spring Courier
- Sugestão de correção (se tiver)

### O que esperar

- **Confirmação de recebimento** em até 48 horas
- **Avaliação inicial** em até 7 dias
- **Correção e release** de acordo com a severidade:
  - 🔴 **Crítica**: Correção e release o mais rápido possível
  - 🟠 **Alta**: Correção na próxima release de patch
  - 🟡 **Média**: Correção na próxima release minor
  - 🟢 **Baixa**: Correção em release futura

### Divulgação Responsável

Pedimos que:

- Nos dê um prazo razoável para corrigir a vulnerabilidade antes de divulgá-la publicamente
- Não explore a vulnerabilidade além do necessário para demonstrá-la
- Não acesse ou modifique dados de outros usuários

### Reconhecimento

Contribuidores que reportarem vulnerabilidades de segurança válidas serão reconhecidos
(com permissão) nos release notes e no README do projeto.

## Boas Práticas de Segurança

Ao usar o Spring Courier no seu projeto:

- Mantenha sempre a versão mais recente da biblioteca
- Valide inputs usando `ValidationBehavior` antes do processamento
- Não exponha mensagens de erro internas em ambientes de produção
- Use o `Response<T>` para encapsular respostas de forma segura — apenas exceções do tipo `CourierException` têm suas mensagens propagadas; demais exceções retornam mensagem genérica
