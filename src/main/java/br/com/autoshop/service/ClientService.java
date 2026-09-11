package br.com.autoshop.service;

import br.com.autoshop.model.ClientEntity;
import br.com.autoshop.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Optional<ClientEntity> getById(Long id){
        return clientRepository.findById(id);
    }
}
