package br.com.autoshop.controller;

import br.com.autoshop.controller.dto.ClientDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/client")
@Tag(name = "Client")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @Operation(summary = "Cadastrar cliente", description = "Cadastra um cliente com nome, CPF ou CNPJ, e-mail e telefone opcional. Retorna o endereço do recurso criado.")
    @PostMapping
    public ResponseEntity<Void> postClient(@Valid @RequestBody ClientDTO dto) {
        if (clientService.isClientExistByDocumentOrEmail(dto.getDocument(), dto.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Long id = clientService.save(dto);
        return ResponseEntity.created(getLocation(id)).build();
    }

    private static @NonNull URI getLocation(Long id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }

    @Operation(summary = "Consultar cliente por ID", description = "Busca os dados cadastrais de um cliente pelo identificador. Retorna 404 quando não encontrado.")
    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(@PathVariable Long id) {
        ClientDTO client = clientService.getById(id);
        if (Objects.nonNull(client)) {
            return ResponseEntity.ok(client);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Listar clientes", description = "Retorna uma página de clientes. Permite informar página, tamanho e campo de ordenação.")
    @GetMapping
    public ResponseEntity<Page<ClientDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(defaultValue = "name") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<ClientDTO> clients = clientService.getAll(pageable);
        return ResponseEntity.ok(clients);
    }

    @Operation(summary = "Substituir dados do cliente", description = "Atualiza os dados cadastrais do cliente pelo ID usando os campos enviados no corpo da requisição.")
    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> put(@PathVariable Long id, @Valid @RequestBody ClientDTO dto) {
        if (Objects.isNull(clientService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        clientService.put(id, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Atualizar parcialmente cliente", description = "Atualiza os campos enviados para o cliente. Campos aceitos: name, document, email e phone.")
    @PatchMapping("/{id}")
    @ExceptionHandler(RequestInvalidException.class)
    public ResponseEntity<ClientDTO> patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        if (Objects.isNull(clientService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        clientService.patch(id, fields);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Desativar cliente", description = "Desativa o cadastro do cliente pelo ID. O cadastro não é removido fisicamente.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ClientDTO> deleteClient(@PathVariable Long id) {
        if (clientService.isClientExistById(id)) {
            clientService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }

}
