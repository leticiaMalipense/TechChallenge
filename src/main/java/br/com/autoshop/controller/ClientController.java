package br.com.autoshop.controller;

import br.com.autoshop.model.ClientEntity;
import br.com.autoshop.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public void postClient() {
    }

    @GetMapping
    public ClientEntity getClient(@RequestParam(name = "id") Long id) {
        return clientService.getById(id).get();
    }

    @PatchMapping
    public String patchClient(@RequestParam(name = "id") Long id) {
        return "teste";
    }

    @DeleteMapping
    public String deleteClient(@RequestParam(name = "id") Long id) {
        return "teste";
    }

}
