package br.com.autoshop.controller;

import br.com.autoshop.dto.PartDTO;
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

@RestController
@RequestMapping("/part")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @PostMapping
    public ResponseEntity<Void> postPart(@Valid @RequestBody PartDTO dto) {
        Long id = partService.save(dto);
        return ResponseEntity.created(getLocation(id)).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartDTO> getPart(@PathVariable Long id) {
        PartDTO part = partService.getById(id);
        if (part == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(part);
    }

    @GetMapping
    public ResponseEntity<Page<PartDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return ResponseEntity.ok(partService.getAll(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> put(@PathVariable Long id, @Valid @RequestBody PartDTO dto) {
        if (partService.getById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        partService.put(id, dto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        if (partService.getById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        partService.patch(id, fields);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePart(@PathVariable Long id) {
        if (!partService.isPartExistById(id)) {
            return ResponseEntity.notFound().build();
        }

        partService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private static URI getLocation(Long id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
