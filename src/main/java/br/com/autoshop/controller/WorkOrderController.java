package br.com.autoshop.controller;

import br.com.autoshop.dto.WorkOrderRequestDTO;
import br.com.autoshop.dto.WorkOrderResponseDTO;
import br.com.autoshop.dto.WorkOrderTrackingDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SecurityRequirement
@RestController
@RequestMapping("/work-orders")
@Tag(name = "Work Order", description = "Gerenciamento completo do ciclo de vida das Ordens de Serviço (OS), desde a recepção do veículo até a entrega e acompanhamento pelo cliente.")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @Operation(
            summary = "Abrir uma nova Ordem de Serviço",
            description = "Registra a entrada de um veículo na oficina vinculando-o ao cliente pelo documento (CPF/CNPJ). " +
                    "Existe para centralizar o atendimento inicial: caso a placa do veículo ainda não conste no sistema, " +
                    "o veículo é cadastrado automaticamente no mesmo fluxo, podendo já receber serviços iniciais e calcular o orçamento prévio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordem de Serviço criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Cliente não encontrado, dados de novo veículo ausentes ou serviço inválido")
    })
    @PostMapping
    public ResponseEntity<WorkOrderResponseDTO> create(@Valid @RequestBody WorkOrderRequestDTO dto) {
        WorkOrderResponseDTO created = workOrderService.create(dto);
        return ResponseEntity.created(getLocation(created.getId())).body(created);
    }

    private static @NonNull URI getLocation(UUID id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    @Operation(
            summary = "Consultar Ordem de Serviço por ID (UUID)",
            description = "Retorna todos os detalhes internos de uma Ordem de Serviço específica a partir do seu identificador único (UUID). " +
                    "Existe para permitir que atendentes e mecânicos visualizem dados completos do cliente, veículo, serviços incluídos, orçamento total e status atual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de Serviço encontrada"),
            @ApiResponse(responseCode = "404", description = "Ordem de Serviço não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponseDTO> getById(@PathVariable UUID id) {
        WorkOrderResponseDTO workOrder = workOrderService.getById(id);
        if (Objects.nonNull(workOrder)) {
            return ResponseEntity.ok(workOrder);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "Listar Ordens de Serviço pelo documento do cliente",
            description = "Busca todas as Ordens de Serviço vinculadas ao CPF ou CNPJ informado. " +
                    "Existe para agilizar o atendimento no balcão ou suporte quando o cliente informa apenas seu documento, permitindo localizar rapidamente seu histórico e OS ativas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de Ordens de Serviço do cliente retornada com sucesso")
    })
    @GetMapping("/client/{document}")
    public ResponseEntity<List<WorkOrderResponseDTO>> getByClientDocument(@PathVariable String document) {
        List<WorkOrderResponseDTO> workOrders = workOrderService.getByClientDocument(document);
        return ResponseEntity.ok(workOrders);
    }

    @Operation(
            summary = "Listar todas as Ordens de Serviço (paginado)",
            description = "Retorna uma listagem paginada e ordenada de todas as Ordens de Serviço da oficina. " +
                    "Existe para prover visão gerencial e operacional do pátio, permitindo acompanhar o volume de atendimentos em andamento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de Ordens de Serviço retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<Page<WorkOrderResponseDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(workOrderService.getAll(pageable));
    }

    @Operation(
            summary = "Adicionar serviços à Ordem de Serviço",
            description = "Inclui um ou mais serviços do catálogo (ServiceOffering) na Ordem de Serviço, avança automaticamente o status para EM DIAGNÓSTICO (IN_DIAGNOSIS) " +
                    "e recalcula o orçamento total (totalEstimate). Existe para permitir que o mecânico componha a lista de intervenções necessárias durante a avaliação técnica do veículo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviços adicionados e orçamento recalculado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Ordem de Serviço em status que não permite edição ou serviço inexistente/inativo")
    })
    @PostMapping("/{id}/services")
    public ResponseEntity<WorkOrderResponseDTO> addServices(
            @PathVariable UUID id,
            @RequestBody List<Long> serviceIds
    ) {
        return ResponseEntity.ok(workOrderService.addServices(id, serviceIds));
    }

    @Operation(
            summary = "Iniciar diagnóstico técnico da Ordem de Serviço",
            description = "Transiciona automaticamente o status da Ordem de Serviço de RECEBIDA (RECEIVED) para EM DIAGNÓSTICO (IN_DIAGNOSIS). " +
                    "Existe para registrar o momento exato em que o veículo deixa a fila de recepção e passa a ser inspecionado pela equipe técnica."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado para IN_DIAGNOSIS"),
            @ApiResponse(responseCode = "400", description = "Transição de status inválida para o estado atual da OS")
    })
    @PatchMapping("/{id}/start-diagnosis")
    public ResponseEntity<WorkOrderResponseDTO> startDiagnosis(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.startDiagnosis(id));
    }

    @Operation(
            summary = "Fechar diagnóstico e enviar orçamento para aprovação do cliente",
            description = "Consolida o valor total do orçamento (serviços e futuros insumos), altera o status de EM DIAGNÓSTICO (IN_DIAGNOSIS) para AGUARDANDO APROVAÇÃO (AWAITING_APPROVAL) " +
                    "e dispara o fluxo de envio ao cliente. Existe para garantir que nenhuma execução mecânica seja iniciada sem que o orçamento esteja fechado e submetido ao cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento consolidado e OS aguardando aprovação (AWAITING_APPROVAL)"),
            @ApiResponse(responseCode = "400", description = "Transição inválida ou nenhum serviço vinculado para compor o orçamento")
    })
    @PatchMapping("/{id}/send-for-approval")
    public ResponseEntity<WorkOrderResponseDTO> sendForApproval(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.sendForApproval(id));
    }

    @Operation(
            summary = "Aprovar o orçamento da Ordem de Serviço",
            description = "Registra o aceite do cliente sobre o orçamento enviado, avançando automaticamente o status de AGUARDANDO APROVAÇÃO (AWAITING_APPROVAL) " +
                    "para EM EXECUÇÃO (IN_EXECUTION). Existe para autorizar formalmente o início dos reparos e habilitar o acompanhamento de progresso pelo cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento aprovado e OS em execução (IN_EXECUTION)"),
            @ApiResponse(responseCode = "400", description = "A Ordem de Serviço não está no status AWAITING_APPROVAL")
    })
    @PatchMapping("/{id}/approve")
    public ResponseEntity<WorkOrderResponseDTO> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.approve(id));
    }

    @Operation(
            summary = "Finalizar a execução técnica da Ordem de Serviço",
            description = "Transiciona automaticamente o status de EM EXECUÇÃO (IN_EXECUTION) para FINALIZADA (FINISHED). " +
                    "Existe para sinalizar que todos os serviços mecânicos contratados foram concluídos e o veículo está pronto aguardando a retirada pelo cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviços concluídos e status alterado para FINISHED"),
            @ApiResponse(responseCode = "400", description = "A Ordem de Serviço não está no status IN_EXECUTION")
    })
    @PatchMapping("/{id}/finish")
    public ResponseEntity<WorkOrderResponseDTO> finish(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.finish(id));
    }

    @Operation(
            summary = "Registrar a entrega do veículo ao cliente",
            description = "Encerra o ciclo da Ordem de Serviço alterando o status de FINALIZADA (FINISHED) para ENTREGUE (DELIVERED) " +
                    "e atualiza automaticamente a data da última revisão (lastReview) no cadastro do veículo. Existe para formalizar a devolução do veículo e manter o histórico de revisão atualizado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Veículo entregue e status alterado para DELIVERED"),
            @ApiResponse(responseCode = "400", description = "A Ordem de Serviço não está no status FINISHED")
    })
    @PatchMapping("/{id}/deliver")
    public ResponseEntity<WorkOrderResponseDTO> deliver(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.deliver(id));
    }

    @Operation(
            summary = "Acompanhar progresso da Ordem de Serviço pelo cliente (Tracking)",
            description = "Permite que o cliente consulte o andamento da sua Ordem de Serviço após a aprovação do orçamento. " +
                    "Existe para oferecer transparência ao cliente sem expor dados sensíveis: utiliza UUID contra enumeração sequencial " +
                    "e exige os 3 primeiros dígitos do documento (CPF/CNPJ) do titular da OS para autorizar a visualização."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progresso da Ordem de Serviço retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Prefixo do documento inválido/divergente, OS não encontrada ou ainda não aprovada")
    })
    @GetMapping("/{id}/tracking")
    public ResponseEntity<WorkOrderTrackingDTO> trackWorkOrder(
            @PathVariable UUID id,
            @RequestParam String documentPrefix
    ) {
        return ResponseEntity.ok(workOrderService.trackWorkOrder(id, documentPrefix));
    }

    @ExceptionHandler(RequestInvalidException.class)
    public ResponseEntity<Map<String, String>> handleRequestInvalidException(RequestInvalidException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
