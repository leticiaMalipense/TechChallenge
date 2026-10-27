# AutoShop

Aplicação de gestão de oficina desenvolvida com Java 21, Spring Boot, Gradle e PostgreSQL.

## Pré-requisitos

- Java 21 (JDK)
- Docker e Docker Compose

O Gradle é executado pelo wrapper incluído no projeto (`./gradlew`), portanto não é necessário instalá-lo separadamente.

## Executar localmente

1. Inicie o PostgreSQL e as ferramentas de apoio definidas no Compose:

   ```bash
   docker compose up -d db pgadmin sonarqube db-sonar
   ```

   Para iniciar somente o PostgreSQL usado pela aplicação:

   ```bash
   docker compose up -d db
   ```

2. Na raiz do projeto, inicie a aplicação:

   ```bash
   ./gradlew bootRun
   ```

   No Windows, use `gradlew.bat bootRun`.

3. A aplicação ficará disponível em `http://localhost:8080/autoshop`. A interface Swagger pode ser acessada em
   `http://localhost:8080/autoshop/swagger-ui/index.html`.

As configurações locais do banco estão em `src/main/resources/application.properties`:

| Propriedade | Valor            |
|-------------|------------------|
| Host/porta  | `localhost:5432` |
| Banco       | `autoshop`       |
| Usuário     | `postgres`       |
| Senha       | `postgres`       |

Esses valores correspondem ao serviço `db` do `docker-compose.yml`. A aplicação cria/atualiza o esquema ao iniciar.

## Serviços auxiliares

Quando iniciados pelo Compose, os serviços ficam disponíveis em:

| Serviço                 | Endereço                | Credenciais locais                    |
|-------------------------|-------------------------|---------------------------------------|
| PostgreSQL da aplicação | `localhost:5432`        | `postgres` / `postgres`               |
| pgAdmin                 | `http://localhost:5050` | `admin` / `admin`                     |
| SonarQube               | `http://localhost:9000` | No primeiro acesso, `admin` / `admin` |

No pgAdmin, para conectar ao banco do Compose, use `db` como host quando o pgAdmin estiver no container; a porta é
`5432`, o banco é `autoshop` e o usuário/senha são `postgres`.

## Compilar e executar testes

```bash
./gradlew clean build
```

Para executar apenas os testes:

```bash
./gradlew test
```

## Parar os serviços

```bash
docker compose down
```

Os dados dos bancos e do SonarQube são mantidos nos volumes do Compose. Para removê-los junto com os containers, use
`docker compose down -v`.

## Peças, insumos e estoque

Regras de cadastro e estoque, exemplos de requisições e testes estão em [Peças e estoque](docs/pecas-e-estoque.md).
