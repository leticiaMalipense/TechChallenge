package br.com.autoshop.integrationTests;

import br.com.autoshop.controller.ClientController;
import br.com.autoshop.config.security.SpringSecurityConfig;
import br.com.autoshop.repository.ClientRepository;
import br.com.autoshop.repository.model.ClientEntity;
import br.com.autoshop.service.ClientService;
import br.com.autoshop.util.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ClientController.class)
@Import({ClientService.class, SpringSecurityConfig.class})
@WithMockUser(roles = "ADMIN")
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "Mechanic")
    void deniesClientEndpointToMechanicRole() throws Exception {
        mockMvc.perform(get("/client/42"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(clientRepository);
    }

    @Test
    void getsClientByIdThroughHttpEndpoint() throws Exception {
        ClientEntity client = ClientEntity.builder()
                .id(42L)
                .name("Ana Silva")
                .document("52998224725")
                .documentType(DocumentType.CPF)
                .email("ana@example.com")
                .phone("11999999999")
                .build();
        when(clientRepository.findById(42L)).thenReturn(java.util.Optional.of(client));

        mockMvc.perform(get("/client/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Silva"))
                .andExpect(jsonPath("$.document").value("52998224725"))
                .andExpect(jsonPath("$.documentType").value("CPF"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.phone").value("11999999999"));

        verify(clientRepository).findById(42L);
    }

    @Test
    void createsClientThroughHttpEndpoint() throws Exception {
        when(clientRepository.getByDocumentOrEmail("52998224725", "ana@example.com"))
                .thenReturn(java.util.Optional.empty());
        when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> {
            ClientEntity saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        mockMvc.perform(post("/client").with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ana Silva",
                                  "document": "529.982.247-25",
                                  "email": "ana@example.com",
                                  "phone": "11999999999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/client/42")));

        verify(clientRepository).save(any(ClientEntity.class));
    }

    @Test
    void rejectsInvalidEmailBeforeCallingServicePersistence() throws Exception {
        mockMvc.perform(post("/client").with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ana Silva",
                                  "document": "52998224725",
                                  "email": "invalid-email",
                                  "phone": "11999999999"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(clientRepository, never()).save(any(ClientEntity.class));
    }
}
