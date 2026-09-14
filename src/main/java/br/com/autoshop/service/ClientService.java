package br.com.autoshop.service;

import br.com.autoshop.dto.ClientDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.model.ClientEntity;
import br.com.autoshop.repository.ClientRepository;
import br.com.autoshop.util.DocumentType;
import br.com.autoshop.util.EmailValidatorUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ClientService {

    public static final String DOCUMENT_TYPE_IS_INVALID = "DocumentType is invalid";
    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientDTO getById(Long id) {
        return clientRepository.findById(id)
                .map(this::convertToDto)
                .orElse(null);
    }

    public ClientDTO getByDocument(String document) {
        return clientRepository.findByDocument(document)
                .map(this::convertToDto)
                .orElse(null);
    }

    public Long save(ClientDTO dto) {
        ClientEntity clientEntity = builderClientEntity(dto).build();
        clientRepository.save(clientEntity);
        return clientEntity.getId();
    }

    public void put(Long id, ClientDTO dto) {
        ClientEntity clientEntity = builderClientEntity(dto).id(id).build();
        clientRepository.save(clientEntity);
    }

    public ClientEntity.ClientEntityBuilder builderClientEntity(ClientDTO dto) {
        return ClientEntity.builder()
                .name(dto.getName())
                .document(dto.getDocument())
                .documentType(dto.getDocumentType())
                .email(dto.getEmail())
                .phone(dto.getPhone());
    }

    public void patch(Long id, Map<String, Object> fields) throws RequestInvalidException {
        Optional<ClientEntity> clientEntityOptional = clientRepository.findById(id);

        AtomicBoolean isDifferent = new AtomicBoolean(false);
        if (clientEntityOptional.isPresent()) {
            ClientEntity client = clientEntityOptional.get();

            fields.forEach((key, value) -> {
                if (Objects.nonNull(value)) {
                    String converted = String.valueOf(value);

                    switch (key) {
                        case "name" -> {
                            client.setName(converted);
                            isDifferent.set(true);
                        }
                        case "documentType" -> {
                            if (!DocumentType.isValueOf(converted)) {
                                throw new RequestInvalidException(DOCUMENT_TYPE_IS_INVALID);
                            }
                            client.setDocumentType(DocumentType.valueOf(converted));
                            isDifferent.set(true);
                        }
                        case "document" -> {
                            client.setDocument(converted);
                            isDifferent.set(true);
                        }
                        case "email" -> {
                            if (!EmailValidatorUtil.isValidEmail(converted)) {
                                throw new RequestInvalidException(EmailValidatorUtil.VALID_EMAIL_ADDRESS);
                            }
                            client.setEmail(converted);
                            isDifferent.set(true);
                        }
                        case "phone" -> {
                            client.setPhone(converted);
                            isDifferent.set(true);
                        }
                    }
                }
            });

            if (isDifferent.get()) {
                client.setId(id);
                clientRepository.save(client);
            }
        }
    }

    public void deleteById(Long id) {
        clientRepository.updateActiveById(Boolean.FALSE, id);
    }

    public boolean isClientExistById(Long id) {
        Optional<ClientEntity> client = clientRepository.getByIdAndActiveIsTrue(id);
        return client.isPresent();
    }

    public Page<ClientDTO> getAll(Pageable pageable) {
        Page<ClientEntity> clientEntityPage = clientRepository.findAll(pageable);
        return clientEntityPage.map(this::convertToDto);
    }
    
    private ClientDTO convertToDto(ClientEntity entity) {
        return ClientDTO.builder()
                .name(entity.getName())
                .document(entity.getDocument())
                .documentType(entity.getDocumentType())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .build();
    }
}
