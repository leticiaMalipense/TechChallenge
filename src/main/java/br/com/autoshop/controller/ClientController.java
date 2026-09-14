package br.com.autoshop.controller;

import br.com.autoshop.dto.ClientDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.service.ClientService;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<Void> postClient(@Valid @RequestBody ClientDTO dto) {
        ClientDTO client = clientService.getByDocument(dto.getDocument());
        if (Objects.nonNull(client)) {
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

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(@PathVariable Long id) {
        ClientDTO client = clientService.getById(id);
        if (Objects.nonNull(client)) {
            return ResponseEntity.ok(client);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Page<ClientDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(defaultValue = "name") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<ClientDTO> clients = clientService.getAll(pageable);
        return ResponseEntity.ok(clients);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> put(@PathVariable Long id, @Valid @RequestBody ClientDTO dto) {
        if (Objects.isNull(clientService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        clientService.put(id, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    @ExceptionHandler(RequestInvalidException.class)
    public ResponseEntity<ClientDTO> patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        if (Objects.isNull(clientService.getById(id))) {
            return ResponseEntity.noContent().build();
        }

        clientService.patch(id, fields);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ClientDTO> deleteClient(@PathVariable Long id) {
        if (clientService.isClientExistById(id)) {
            clientService.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }

}
