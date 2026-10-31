# AutoShop

Sistema de gestão de oficina mecânica desenvolvido como Tech Challenge. A API permite cadastrar clientes, veículos e
serviços, acompanhar ordens de serviço (OS), enviar orçamentos em PDF por e-mail e registrar a aprovação do cliente.

## Objetivo e solução

O projeto organiza o fluxo de atendimento de uma oficina: abertura da OS, diagnóstico, composição do orçamento,
aprovação, execução, finalização e entrega do veículo. O orçamento é enviado como PDF; o cliente pode aprová-lo por um
link de uso único com prazo de validade. A API também oferece acompanhamento da OS após a aprovação.

## Arquitetura

A aplicação segue uma arquitetura em camadas dentro de um monólito Spring Boot:

- **Controller** recebe requisições HTTP, valida entradas e define respostas e códigos HTTP.
- **Service** implementa regras de negócio e coordena os casos de uso.
- **Repository** usa Spring Data JPA para persistir e consultar entidades no PostgreSQL.
- **Model/Entity** representa os dados persistidos; DTOs definem os contratos da API.
- **Config** reúne configuração de segurança, JWT e documentação OpenAPI.

O domínio é documentado com conceitos de DDD, linguagem ubíqua e fluxos de Event Storming. Os links estão na
seção [Documentação DDD](#documentação-ddd).

## Tecnologias

- Java 21 e Spring Boot
- Spring MVC, Spring Security, OAuth2 Resource Server e JWT
- Spring Data JPA / Hibernate e PostgreSQL 15
- Gradle Wrapper
- JasperReports para geração de PDF
- Spring Mail e Mailpit para desenvolvimento local
- Swagger UI / OpenAPI
- Docker Compose
- JUnit e JaCoCo para testes e cobertura

## Banco de dados

O PostgreSQL foi escolhido por ser um banco relacional adequado às relações entre clientes, veículos, ordens de serviço,
serviços e registros de envio. Transações, chaves estrangeiras e restrições de unicidade ajudam a preservar a
consistência dos dados do fluxo da oficina. No ambiente local, o Compose inicia PostgreSQL 15.

## Pré-requisitos

- JDK 21
- Docker e Docker Compose (para executar o banco e, opcionalmente, a aplicação e ferramentas auxiliares)

O Gradle é executado pelo wrapper incluído (`./gradlew`), sem instalação separada.

## Executar localmente

### Aplicação no host

1. Inicie com docker composer:

   ```bash
   docker compose up -d 
   ```

O Compose aguarda a verificação de saúde do banco antes de iniciar a aplicação. Acesse `http://localhost:8080/autoshop`.

## Acesso à API e autenticação

- Swagger
  UI: [http://localhost:8080/autoshop/swagger-ui/index.html](http://localhost:8080/autoshop/swagger-ui/index.html)
- Especificação OpenAPI: [http://localhost:8080/autoshop/v3/api-docs](http://localhost:8080/autoshop/v3/api-docs)

As rotas protegidas usam **Bearer JWT**. Para obter um token, envie as credenciais do usuário no endpoint
`POST /autoshop/auth` com `Authorization: Basic` (email e senha), por exemplo:

```bash
curl -u 'admin@admin:123' -X POST http://localhost:8080/autoshop/auth
```

Copie o campo `token` da resposta e, no Swagger UI, selecione **Authorize** no esquema Bearer e informe o JWT. O
endpoint `/auth` aceita Basic apenas para autenticar e emitir o JWT.

Usuário administrador de demonstração, inserido por `src/main/resources/data.sql`:

| E-mail        | Senha | Papel   |
|---------------|-------|---------|
| `admin@admin` | `123` | `ADMIN` |

O cadastro público é feito por `POST /autoshop/signup`; contas cadastradas recebem papel `Client`. Para operações
protegidas, use um token JWT válido.

## Testes

Execute todos os testes:

```bash
./gradlew test
```

Para executar os testes e gerar o relatório de cobertura JaCoCo:

```bash
./gradlew clean test jacocoTestReport
```

Abra `build/reports/jacoco/test/html/index.html` no navegador para consultar o relatório detalhado. O relatório XML, útil para ferramentas de análise contínua, fica em `build/reports/jacoco/test/jacocoTestReport.xml`.

Para compilar, executar os testes e gerar os relatórios de build:

```bash
./gradlew clean build
```

## Estrutura dos diretórios principais

```text
src/main/java/br/com/autoshop/
  config/       Configuração de segurança e Swagger/OpenAPI
  controller/   Endpoints REST e DTOs
  exception/    Exceções e tratamento global de erros
  repository/   Repositórios e entidades JPA
  service/      Casos de uso e regras de negócio
  util/         Tipos e utilitários compartilhados
src/main/resources/
  application.properties  Configurações da aplicação
  data.sql                Dados iniciais de demonstração
  reports/                Template e recursos de relatório PDF
src/test/java/             Testes unitários e de integração
docs/ddd/                  Documentação de domínio e DDD
docs/c4/                   Diagramas de contexto e containers
```

## Documentação DDD

- [Módulo de Ordem de Serviço](docs/ddd/modulo_ordem_de_servico.md)
- [Dicionário de Linguagem Ubíqua](docs/ddd/dicionario_linguagem_ubiqua.md)
- [Diagrama do envio de e-mail e aprovação da OS](docs/ddd/diagrama_envio_email_aprovacao_os.md)
- [Event Storming: criação de OS](docs/ddd/event_storming_criacao_de_OS.pdf)
- [Event Storming: acompanhamento de OS](docs/ddd/event_storming_acompanhamento_OS.pdf)
- [Diagramas C4](docs/c4/)

## Vídeo da entrega

> **Link pendente:** adicione aqui o link do vídeo de apresentação/entrega.

## Serviços auxiliares

| Serviço                 | Endereço                | Credenciais locais                    |
|-------------------------|-------------------------|---------------------------------------|
| PostgreSQL da aplicação | `localhost:5432`        | `postgres` / `postgres`               |
| Mailpit (interface)     | `http://localhost:8025` | Sem autenticação                      |
| Mailpit (SMTP)          | `localhost:1025`        | Sem autenticação                      |
| SonarQube, se iniciado  | `http://localhost:9000` | No primeiro acesso, `admin` / `admin` |

Para iniciar SonarQube e seu banco auxiliar, use `docker compose up -d sonarqube db-sonar`. Dentro da rede do Compose, o
host do PostgreSQL é `db`; a partir do host, use `localhost`.

As configurações de SMTP aceitam `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`,
`SMTP_STARTTLS` e `SMTP_FROM`. A URL pública dos links de aprovação pode ser definida por `APPROVAL_BASE_URL`; o prazo
do token pode ser definido por `APPROVAL_TOKEN_TTL_HOURS` (padrão de 72 horas). O Mailpit captura e-mails localmente e
não os envia a destinatários externos.

## Encerrar os serviços

```bash
docker compose down
```

Os dados são mantidos nos volumes do Compose. Para removê-los também:

```bash
docker compose down -v
```
