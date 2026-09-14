package br.com.autoshop.service;

import br.com.autoshop.dto.ClientDTO;
import br.com.autoshop.model.ClientEntity;
import br.com.autoshop.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientDTO getById(Long id) {
        Optional<ClientEntity> client = clientRepository.findById(id);

        if (client.isPresent()) {
            ClientEntity clientEntity = client.get();
            return buildClientDTO(clientEntity);
        }
        return null;
    }

    public ClientDTO getByDocument(String document) {
        Optional<ClientEntity> client = clientRepository.findByDocument(document);

        if (client.isPresent()) {
            ClientEntity clientEntity = client.get();
            return buildClientDTO(clientEntity);
        }
        return null;
    }

    private static ClientDTO buildClientDTO(ClientEntity clientEntity) {
        return ClientDTO.builder()
                .name(clientEntity.getName())
                .document(clientEntity.getDocument())
                .documentType(clientEntity.getDocumentType())
                .email(clientEntity.getEmail())
                .phone(clientEntity.getPhone()).build();
    }


    public Long save(ClientDTO dto) {
        ClientEntity clientEntity = builderClientEntity(dto).build();
        clientRepository.save(clientEntity);
        return clientEntity.getId();
    }

    public void put(Long id, ClientDTO dto) {
        ClientEntity clientEntity = builderClientEntity(dto)
                .id(id).build();
        clientRepository.save(clientEntity);
    }

    public void patch(Long id, Map<String, Object> fields) {

    }

    public ClientEntity.ClientEntityBuilder builderClientEntity(ClientDTO dto) {
        return ClientEntity.builder()
                .name(dto.getName())
                .document(dto.getDocument())
                .documentType(dto.getDocumentType())
                .email(dto.getEmail())
                .phone(dto.getPhone());
    }

    public void deleteById(Long id) {
        clientRepository.updateActiveById(Boolean.FALSE, id);
    }

    public boolean isClientExistById(Long id) {
        Optional<ClientEntity> client = clientRepository.getByIdAndActiveIsTrue(id);
        return client.isPresent();
    }
}
