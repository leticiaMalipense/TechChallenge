# Módulo de Ordem de Serviço

Este documento descreve o comportamento implementado no módulo de Ordens de Serviço (OS) e seus endpoints. No dicionário de linguagem ubíqua, uma OS é o registro do que deverá ser feito no veículo, associado a um cliente e a um veículo, com serviços previstos, status, estimativa e datas.

## Visão geral do fluxo

1. A oficina abre a OS informando o documento do cliente, a placa do veículo e, opcionalmente, serviços do catálogo.
2. O cliente precisa existir e estar ativo. Se a placa ainda não estiver cadastrada, marca, modelo e ano do veículo também são obrigatórios para cadastrá-lo no mesmo fluxo.
3. A OS começa como `RECEIVED` quando não há serviços ou como `IN_DIAGNOSIS` quando já são informados serviços. O valor estimado é a soma do custo-base dos serviços; peças e insumos ainda não são considerados.
4. Durante a avaliação, a oficina pode incluir serviços e iniciar o diagnóstico. Para submeter um orçamento, a OS precisa estar em `IN_DIAGNOSIS` e ter pelo menos um serviço; a operação muda o status para `AWAITING_APPROVAL`.
5. Após a aprovação, a OS passa por `IN_EXECUTION`, `FINISHED` e `DELIVERED`. A entrega também atualiza a data de última revisão do veículo.
6. O acompanhamento do cliente só é permitido depois da aprovação e requer o UUID da OS e os três primeiros dígitos do documento do cliente.

As transições válidas implementadas são:

| Operação | Status de origem | Status de destino |
|---|---|---|
| Adicionar serviços ou iniciar diagnóstico | `RECEIVED` | `IN_DIAGNOSIS` |
| Adicionar serviços | `IN_DIAGNOSIS` | `IN_DIAGNOSIS` |
| Enviar para aprovação | `IN_DIAGNOSIS` | `AWAITING_APPROVAL` |
| Aprovar orçamento | `AWAITING_APPROVAL` | `IN_EXECUTION` |
| Finalizar execução | `IN_EXECUTION` | `FINISHED` |
| Registrar entrega | `FINISHED` | `DELIVERED` |

## Endpoints

O controller administrativo tem base `/work-orders`; com o `server.servlet.context-path=/autoshop` configurado, o prefixo completo é `/autoshop/work-orders`. Essas rotas exigem autenticação Basic. As exceções públicas de aprovação por token estão em `/work-order-approvals/{token}` e são identificadas separadamente na tabela.

| Método e caminho | Entrada | Resultado |
|---|---|---|
| `POST /work-orders` | JSON `WorkOrderRequestDTO` | Cria a OS; retorna `201 Created`, corpo `WorkOrderResponseDTO` e `Location` para a OS criada. |
| `GET /work-orders/{id}` | `id` UUID | Retorna `200` com `WorkOrderResponseDTO` ou `404` quando não encontrada. |
| `GET /work-orders/client/{document}` | Documento do cliente no caminho | Retorna `200` com a lista de OS daquele documento. |
| `GET /work-orders?page=0&size=10&sortBy=creationDate` | Parâmetros de paginação; ordenação decrescente por padrão | Retorna `200` com uma página de `WorkOrderResponseDTO`. |
| `POST /work-orders/{id}/services` | Corpo JSON: lista de IDs de serviços (`[1, 2]`) | Adiciona serviços ativos, atualiza estimativa e status; retorna `200` e `WorkOrderResponseDTO`. |
| `PATCH /work-orders/{id}/start-diagnosis` | `id` UUID | Inicia o diagnóstico; retorna `200` e `WorkOrderResponseDTO`. |
| `PATCH /work-orders/{id}/send-for-approval` | `id` UUID | Consolida o orçamento, muda para `AWAITING_APPROVAL` e envia o PDF ao e-mail cadastrado do cliente; retorna `200` e `WorkOrderResponseDTO`, ou `502` com o estado da falha de e-mail (a OS continua aguardando aprovação). |
| `POST /work-orders/{id}/estimate-email/resend` | `id` UUID | Reenvia o orçamento da OS que ainda aguarda aprovação; retorna `200` ou `502` se o SMTP falhar. |
| `GET /work-orders/{id}/estimate-email/latest` | `id` UUID | Retorna o estado da última tentativa de envio ou `404` se ainda não houve tentativa. |
| `PATCH /work-orders/{id}/approve` | `id` UUID | Aprova o orçamento e muda para `IN_EXECUTION`; retorna `200` e `WorkOrderResponseDTO`. |
| `PATCH /work-orders/{id}/finish` | `id` UUID | Finaliza a execução e muda para `FINISHED`; retorna `200` e `WorkOrderResponseDTO`. |
| `PATCH /work-orders/{id}/deliver` | `id` UUID | Registra a entrega e muda para `DELIVERED`; retorna `200` e `WorkOrderResponseDTO`. |
| `GET /work-orders/{id}/tracking?documentPrefix=123` | UUID e prefixo do documento | Retorna `200` com `WorkOrderTrackingDTO` se a OS já foi aprovada e o prefixo confere. |
| `GET /work-order-approvals/{token}` | Token enviado por e-mail | Rota pública que apresenta a página de confirmação; não altera o status da OS. |
| `POST /work-order-approvals/{token}` | Token enviado por e-mail | Confirma explicitamente a aprovação e muda `AWAITING_APPROVAL` para `IN_EXECUTION`; retorna uma página de confirmação. |

As rotas de aprovação por token são públicas, mas as rotas administrativas de OS — incluindo `PATCH /work-orders/{id}/approve` e as operações de reenvio/consulta de envio — continuam protegidas por autenticação.

### Formatos principais

`WorkOrderRequestDTO` aceita:

```json
{
  "clientDocument": "documento do cliente",
  "vehiclePlateNumber": "ABC1D23",
  "vehicleBrand": "Marca",
  "vehicleModel": "Modelo",
  "vehicleYear": 2024,
  "serviceIds": [1, 2]
}
```

`clientDocument` e `vehiclePlateNumber` são obrigatórios. Marca, modelo e ano só são exigidos se o veículo da placa ainda não existir. `serviceIds` pode estar vazio.

`WorkOrderResponseDTO` contém UUID, nome e documento do cliente, dados do veículo, status, serviços (nome, descrição, custo-base, duração e categoria), estimativa total e datas de criação e atualização. Não inclui e-mail do cliente. `WorkOrderTrackingDTO` é mais restrito: não expõe nome/documento do cliente nem detalhes completos dos serviços.

Erros de regra de negócio tratados por `RequestInvalidException` retornam `400` com corpo `{"error":"..."}`. Isso inclui cliente inexistente/inativo, dados obrigatórios do novo veículo ausentes, serviço inexistente/inativo, transição inválida, ausência de serviços ao enviar para aprovação e falha de validação do tracking. A consulta por UUID é a exceção: responde `404` quando a OS não existe.

## PDF e envio de e-mail

- O PDF é gerado em `PATCH /work-orders/{id}/send-for-approval`, quando a OS já tem serviços e o total é recalculado. `EstimatePDFService.generatePdf(WorkOrderResponseDTO)` compila `reports/estimate_report_template.jrxml` com Jasper e retorna os bytes; não grava arquivo por OS nem há endpoint de download.
- `WorkOrderQuoteDeliveryService` obtém o e-mail de `ClientEntity`, gera o PDF e chama `QuoteEmailService` para enviar o anexo `orcamento-{id}.pdf` com o link de aprovação.
- Cada tentativa é persistida em `work_order_email_delivery` com estado `PENDING`, `SENT` ou `FAILED`, número da tentativa, datas e mensagem de erro sanitizada. Uma falha SMTP deixa a OS em `AWAITING_APPROVAL`, retorna `502` e pode ser consultada pela rota `/estimate-email/latest` e reenviada por `/estimate-email/resend`.
- O link usa a base configurada em `APPROVAL_BASE_URL` (propriedade `autoshop.approval.base-url`) e expira após o prazo definido em `APPROVAL_TOKEN_TTL_HOURS` (padrão: 72 horas).
- O token de aprovação é aleatório, de uso único e persistido somente como hash SHA-256 na tabela `work_order_approval_token`. Reenviar o orçamento invalida o token anterior e emite um novo. Ao aprovar, tokens pendentes são consumidos ou revogados.
- A visita GET ao link apenas exibe uma página de confirmação. O botão envia um POST explícito; só então o status muda para `IN_EXECUTION`. Tokens inválidos/expirados retornam `410`; conflitos com OS já processadas retornam `409`. A página não expõe dados pessoais, usa `Cache-Control: no-store` e `Referrer-Policy: no-referrer`; o token é persistido somente como hash.
- A configuração SMTP usa `spring.mail.*` e `autoshop.mail.from`: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS` e `SMTP_FROM`. Em produção, configure SMTP e `APPROVAL_BASE_URL` com valores/segredos próprios do ambiente.
- A criação (`POST /work-orders`) não envia e-mail. Isso evita enviar uma estimativa incompleta, pois uma OS pode ser criada sem serviços e receber itens depois.

## Documentação de domínio relacionada

- [Diagrama — envio de e-mail e aprovação da OS](diagrama_envio_email_aprovacao_os.md)
- [Dicionário de Linguagem Ubíqua](dicionario_linguagem_ubiqua.md)
- [Event Storming — criação de OS](event_storming_criacao_de_OS.pdf)
- [Event Storming — acompanhamento de OS](event_storming_acompanhamento_OS.pdf)
