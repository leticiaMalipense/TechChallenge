package br.com.autoshop.service;

import br.com.autoshop.dto.ClientDTO;
import br.com.autoshop.exception.RequestInvalidException;
import br.com.autoshop.model.ClientEntity;
import br.com.autoshop.repository.ClientRepository;
import br.com.autoshop.util.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository);
    }

    @Test
    void getByIdReturnsMappedClientWhenFound() {
        ClientEntity entity = client(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(entity));

        ClientDTO result = clientService.getById(1L);

        assertClientDto(result);
    }

    @Test
    void getByIdReturnsNullWhenMissing() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(clientService.getById(99L));
    }

    @Test
    void getByDocumentReturnsMappedClientWhenFoundAndNullWhenMissing() {
        when(clientRepository.findByDocument("123")).thenReturn(Optional.of(client(1L)));
        when(clientRepository.findByDocument("missing")).thenReturn(Optional.empty());

        assertClientDto(clientService.getByDocument("123"));
        assertNull(clientService.getByDocument("missing"));
    }

    @Test
    void savePersistsMappedEntityAndReturnsGeneratedId() {
        ClientEntity entity = client(42L);
        when(clientRepository.save(any(ClientEntity.class))).thenReturn(entity);

        Long id = clientService.save(dto());

        assertEquals(42L, id);
        verify(clientRepository).save(argThat(saved -> saved.getName().equals("Ana Silva")
                && saved.getDocument().equals("123")
                && saved.getDocumentType() == DocumentType.CPF
                && saved.getEmail().equals("ana@example.com")
                && saved.getPhone().equals("11999999999")));
    }

    @Test
    void putPersistsEntityWithProvidedId() {
        clientService.put(7L, dto());

        verify(clientRepository).save(argThat(saved -> saved.getId().equals(7L)
                && saved.getName().equals("Ana Silva")));
    }

    @Test
    void patchUpdatesRecognizedNonNullFieldsAndSaves() {
        ClientEntity entity = client(1L);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(entity));

        clientService.patch(1L, Map.of("name", "Maria", "email", "maria@example.com", "phone", "1234"));

        assertEquals("Maria", entity.getName());
        assertEquals("maria@example.com", entity.getEmail());
        assertEquals("1234", entity.getPhone());
        verify(clientRepository).save(entity);
    }

    @Test
    void patchWithNoRecognizedNonNullChangesDoesNotSave() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L)));

        assertDoesNotThrow(() -> clientService.patch(1L, Map.of("unknown", "value")));

        verify(clientRepository, never()).save(any());
    }

    @Test
    void patchWithInvalidDocumentTypeThrowsAndDoesNotSave() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L)));

        RequestInvalidException exception = assertThrows(RequestInvalidException.class,
                () -> clientService.patch(1L, Map.of("documentType", "INVALID")));

        assertEquals(ClientService.DOCUMENT_TYPE_IS_INVALID, exception.getMessage());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void patchWithInvalidEmailThrowsAndDoesNotSave() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L)));

        assertThrows(RequestInvalidException.class,
                () -> clientService.patch(1L, Map.of("email", "invalid-email")));

        verify(clientRepository, never()).save(any());
    }

    @Test
    void patchDoesNothingWhenClientDoesNotExist() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> clientService.patch(99L, Map.of("name", "Maria")));

        verify(clientRepository, never()).save(any());
    }

    @Test
    void deleteByIdMarksClientInactive() {
        clientService.deleteById(3L);

        verify(clientRepository).updateActiveById(false, 3L);
    }

    @Test
    void isClientExistByIdReflectsRepositoryResult() {
        when(clientRepository.getByIdAndActiveIsTrue(1L)).thenReturn(Optional.of(client(1L)));
        when(clientRepository.getByIdAndActiveIsTrue(2L)).thenReturn(Optional.empty());

        assertTrue(clientService.isClientExistById(1L));
        assertFalse(clientService.isClientExistById(2L));
    }

    @Test
    void getAllMapsPageContentAndPreservesPageMetadata() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ClientEntity> entities = new PageImpl<>(List.of(client(1L)), pageable, 1);
        when(clientRepository.findAll(pageable)).thenReturn(entities);

        Page<ClientDTO> result = clientService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertClientDto(result.getContent().get(0));
    }

    private static ClientDTO dto() {
        return ClientDTO.builder().name("Ana Silva").document("123").documentType(DocumentType.CPF)
                .email("ana@example.com").phone("11999999999").build();
    }

    private static ClientEntity client(Long id) {
        return ClientEntity.builder().id(id).name("Ana Silva").document("123").documentType(DocumentType.CPF)
                .email("ana@example.com").phone("11999999999").build();
    }

    private static void assertClientDto(ClientDTO result) {
        assertNotNull(result);
        assertEquals("Ana Silva", result.getName());
        assertEquals("123", result.getDocument());
        assertEquals(DocumentType.CPF, result.getDocumentType());
        assertEquals("ana@example.com", result.getEmail());
        assertEquals("11999999999", result.getPhone());
    }
}
