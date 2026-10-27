package br.com.autoshop.controller;

import br.com.autoshop.controller.dto.PartDTO;
import br.com.autoshop.controller.dto.StockMovementDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.autoshop.service.PartService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@SecurityRequirement(name = "basicAuth")
@Tag(name = "Part", description = "Cadastro de peças, insumos e controle de estoque")
@RestController
@RequestMapping("/part")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @Operation(summary = "Cadastrar peça ou insumo",
            description = "Cria um cadastro ativo com saldo inicial zero. Código único, inclusive entre inativos. ID e saldo são somente leitura.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cadastro criado; endereço retornado no cabeçalho Location"),
            @ApiResponse(responseCode = "400", description = "Dados de cadastro inválidos"),
            @ApiResponse(responseCode = "409", description = "Código já cadastrado"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @PostMapping
    public ResponseEntity<Void> postPart(@Valid @RequestBody PartDTO dto) {
        Long id = partService.save(dto);
        return ResponseEntity.created(getLocation(id)).build();
    }

    @Operation(summary = "Consultar peça ou insumo",
            description = "Retorna o cadastro ativo, incluindo ID e saldo atual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadastro encontrado"),
            @ApiResponse(responseCode = "404", description = "Cadastro inexistente ou inativo"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PartDTO> getPart(@PathVariable Long id) {
        PartDTO part = partService.getById(id);
        if (part == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(part);
    }

    @Operation(summary = "Listar peças e insumos ativos",
            description = "Lista cadastros ativos com ID e saldo. Paginação começa em zero; ordenação decrescente pelo campo sortBy.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de cadastros ativos"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @GetMapping
    public ResponseEntity<Page<PartDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(partService.getAll(pageable));
    }

    @Operation(summary = "Atualizar cadastro completo",
            description = "Substitui os campos editáveis de um cadastro ativo. Preserva ID, data de criação, estado e saldo. Mudança de unidade exige saldo zero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadastro atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Cadastro inexistente ou inativo"),
            @ApiResponse(responseCode = "409", description = "Código já cadastrado"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> put(@PathVariable Long id, @Valid @RequestBody PartDTO dto) {
        partService.put(id, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Atualizar cadastro parcialmente",
            description = "Atualiza os campos enviados. Campos nulos e desconhecidos são ignorados. ID, saldo e estado não são editáveis. Mudança de unidade exige saldo zero. Nome e código não podem ser vazios; preços devem ser não negativos, com até duas casas decimais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Campos atualizados"),
            @ApiResponse(responseCode = "400", description = "Valor de campo inválido"),
            @ApiResponse(responseCode = "404", description = "Cadastro inexistente ou inativo"),
            @ApiResponse(responseCode = "409", description = "Código já cadastrado"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<Void> patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        partService.patch(id, fields);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Inativar peça ou insumo",
            description = "Realiza exclusão lógica, preservando saldo e código. O item deixa de aceitar consultas e movimentações. Não há reativação pela API.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadastro inativado"),
            @ApiResponse(responseCode = "404", description = "Cadastro inexistente ou já inativo"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        partService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Registrar entrada de estoque",
            description = "Acrescenta uma quantidade positiva ao saldo de uma peça ou insumo ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo atualizado; retorna o cadastro completo"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida, saldo insuficiente na saída ou limite de saldo excedido na entrada"),
            @ApiResponse(responseCode = "404", description = "Cadastro inexistente ou inativo"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @PostMapping("/{id}/stock/entries")
    public ResponseEntity<PartDTO> addStock(@PathVariable Long id, @Valid @RequestBody StockMovementDTO dto) {
        PartDTO part = partService.addStock(id, dto.getQuantity());
        if (part == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(part);
    }

    @Operation(summary = "Registrar saída de estoque",
            description = "Retira uma quantidade positiva do saldo. Rejeita saldo insuficiente e itens inativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo atualizado; retorna o cadastro completo"),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida, saldo insuficiente na saída ou limite de saldo excedido na entrada"),
            @ApiResponse(responseCode = "404", description = "Cadastro inexistente ou inativo"),
            @ApiResponse(responseCode = "401", description = "Autenticação ausente ou inválida")
    })
    @PostMapping("/{id}/stock/exits")
    public ResponseEntity<PartDTO> removeStock(@PathVariable Long id, @Valid @RequestBody StockMovementDTO dto) {
        PartDTO part = partService.removeStock(id, dto.getQuantity());
        if (part == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(part);
    }

    private static URI getLocation(Long id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
