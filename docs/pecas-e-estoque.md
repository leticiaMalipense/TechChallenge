# Peças, insumos e estoque

## Escopo

Cadastro administrativo de peças e insumos, consulta de saldo e operações manuais de entrada e saída. Implementado nas camadas controller, service e repository do monólito, seguindo a organização do projeto.

Esta documentação descreve a implementação atual. Não substitui o Event Storming e os demais artefatos DDD da entrega.

## Acesso e execução

Pré-requisitos: Java 21, Docker e acesso às dependências do Gradle.

Na raiz do projeto:

```bash
docker compose up -d db
./gradlew bootRun
```

O banco configurado é PostgreSQL em localhost:5432. A configuração atual usa Hibernate `ddl-auto=update`; a coluna de saldo tem valor padrão zero para cadastros existentes. Não há ferramenta de migração versionada neste módulo.

- URL base: `http://localhost:8080/autoshop`
- Swagger UI: `http://localhost:8080/autoshop/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/autoshop/v3/api-docs`

As rotas de peças exigem autenticação. A configuração de segurança aceita Basic Authentication e Bearer JWT. O botão Authorize do Swagger está configurado globalmente para `basicAuth`: utilize um usuário válido do ambiente. Para JWT, `/auth` recebe o cabeçalho Basic de um usuário válido; utilize o token retornado nas chamadas seguintes com `Authorization: Bearer <token>`. Não é necessário alterar a configuração compartilhada para testar o módulo.

## Contrato das operações

Os caminhos abaixo são relativos à URL base.

| Método | Caminho | Resultado |
| --- | --- | --- |
| POST | `/part` | 201, sem corpo; cabeçalho Location aponta para o cadastro |
| GET | `/part/{id}` | 200, cadastro ativo com ID e saldo |
| GET | `/part?page=0&size=10&sortBy=name` | 200, página de cadastros ativos; ordenação decrescente |
| PUT | `/part/{id}` | 200, sem corpo; substitui campos editáveis |
| PATCH | `/part/{id}` | 200, sem corpo; altera os campos enviados |
| DELETE | `/part/{id}` | 200, sem corpo; inativa o cadastro |
| POST | `/part/{id}/stock/entries` | 200, cadastro com saldo atualizado |
| POST | `/part/{id}/stock/exits` | 200, cadastro com saldo atualizado |

Cadastro e PUT recebem:

```json
{
  "name": "Óleo de motor",
  "code": "OIL-001",
  "description": "Óleo utilizado na manutenção",
  "category": "Lubrificantes",
  "partType": "SUPPLY",
  "unit": "LITER",
  "costPrice": 20.00,
  "salePrice": 35.00
}
```

`name` e `code` são obrigatórios e não podem conter apenas espaços. `partType` aceita `PART` e `SUPPLY`. `unit` aceita `UNIT`, `KILOGRAM`, `LITER` e `METER`. Tipo, unidade e preços são obrigatórios. Preços aceitam zero, até 17 dígitos inteiros e 2 casas decimais; valores excedentes são rejeitados, sem arredondamento.

`id` e `stockQuantity` são somente leitura. O sistema gera o ID e inicia o saldo em zero, independentemente desses campos no corpo do cadastro. PUT preserva ID, data de criação, estado e saldo. PUT, PATCH e DELETE verificam a disponibilidade dentro da transação e retornam 404 se o item estiver inativo ou ausente.

Exemplo de PATCH:

```json
{"name": "Óleo de motor 5W30", "salePrice": 39.90}
```

PATCH aceita os mesmos campos editáveis do cadastro. Campos ausentes, nulos ou desconhecidos são ignorados. Não permite limpar campos opcionais enviando null. `id`, `active`, `creationDate` e `stockQuantity` não são alterados por PATCH.

## Estoque

Entrada e saída recebem:

```json
{"quantity": 2.750}
```

- Quantidade estritamente positiva, com até 16 dígitos inteiros e 3 casas decimais.
- O saldo também é limitado a 16 dígitos inteiros e 3 casas decimais.
- Quantidades usam BigDecimal e são expressas na unidade do cadastro. Não há conversão entre unidades.
- A implementação permite frações para todas as unidades, inclusive UNIT.
- Entrada soma a quantidade; saída subtrai e rejeita saldo insuficiente.
- Uma saída pode consumir exatamente todo o saldo.
- Cada operação executa em uma transação com bloqueio pessimista da peça no PostgreSQL. Movimentações concorrentes são serializadas por cadastro.
- PUT, PATCH e exclusão usam o mesmo bloqueio para não sobrescrever o saldo durante outra operação.
- A resposta inclui o cadastro completo e seu saldo atualizado em `stockQuantity`.

Exemplo de demonstração: cadastrar um insumo, registrar entrada de 3.000, saída de 0.250 e consultar saldo de 2.750. Uma saída posterior de 3.000 deve ser rejeitada, preservando 2.750.

## Exclusão lógica e erros

A exclusão altera `active` para false e preserva o registro e seu saldo. Pode ocorrer mesmo com saldo positivo; o saldo não é baixado automaticamente. Itens inativos ficam fora da listagem e das consultas por ID ou código, e não aceitam edição ou movimentação. Não há endpoint de reativação.

O código continua reservado após a inativação, em conformidade com a restrição única do banco. As comparações não normalizam espaços ou maiúsculas/minúsculas.

| Status | Situação |
| --- | --- |
| 400 | Dados inválidos, quantidade inválida, saldo insuficiente ou saldo excedendo o limite |
| 401 | Autenticação ausente ou inválida |
| 404 | Cadastro inexistente ou inativo nas operações individuais |
| 409 | Código duplicado identificado pela validação do service |

A restrição única também impede duplicidade no banco. Violações da restrição de código da peça retornam 409, inclusive em cadastros concorrentes. O tratamento reconhece tanto o nome explícito `uk_part_code` quanto o nome gerado pelo Hibernate em bancos anteriores; outras falhas de integridade não são convertidas em conflito. As mensagens de erro seguem os handlers existentes do projeto; não há um formato único adicional neste módulo.

## Testes

Com PostgreSQL disponível:

```bash
./gradlew test
```

Somente testes unitários de peças:

```bash
./gradlew test --tests br.com.autoshop.service.PartServiceTest --tests br.com.autoshop.service.PartStockServiceTest --tests br.com.autoshop.controller.PartControllerTest
```

Persistência e concorrência com banco:

```bash
./gradlew test --tests br.com.autoshop.integration.PartStockIntegrationTest
```

Os testes de integração criam cadastros com códigos únicos e removem apenas esses cadastros ao terminar. Exercitam preservação de saldo em edições, rejeição de movimentações em inativos e duas saídas simultâneas de 7 sobre saldo 10: uma é aceita, outra rejeitada e o saldo final é 3. Não constituem teste de carga.

Os testes HTTP iniciam a aplicação em porta aleatória, enviam JSON pela rede local e usam JWT assinado pelo encoder configurado no projeto. Verificam requisições sem token e com token inválido, validação de cadastro e quantidade, campos somente leitura, inativação, mudança de unidade e cadastros concorrentes. O token de teste não substitui os testes do fluxo de login.

```bash
./gradlew test --tests br.com.autoshop.integration.PartHttpIntegrationTest
```

## Limites e integração pendente

- Não há histórico persistido de movimentações, reservas, compras, fornecedores ou alerta de estoque mínimo.
- A OS ainda não inclui peças nem insumos e seu orçamento não soma esses itens.
- O grupo precisa definir o momento da baixa e os efeitos de aprovação, rejeição e alterações na OS.
- Alterar a unidade exige saldo zero, tanto no PUT quanto no PATCH. Manter a mesma unidade é permitido com saldo. O tipo PART/SUPPLY continua editável.
- A documentação DDD deve ser alinhada aos artefatos do grupo em `docs/ddd`. A integração com OS permanece pendente.
