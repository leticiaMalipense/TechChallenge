package br.com.autoshop.config.security;

import br.com.autoshop.repository.model.UserEntity;
import br.com.autoshop.util.RoleType;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuthenticatedTest {

    @Test
    void exposesEmailPasswordAndRoleAuthorityFromUser() {
        UserEntity user = UserEntity.builder()
                .email("ana@example.com")
                .password("encoded-password")
                .role(RoleType.Mechanic)
                .build();

        UserAuthenticated authenticated = new UserAuthenticated(user);

        assertEquals("ana@example.com", authenticated.getUsername());
        assertEquals("encoded-password", authenticated.getPassword());
        assertEquals(1, authenticated.getAuthorities().size());
        assertTrue(authenticated.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_Mechanic")));
    }
}
