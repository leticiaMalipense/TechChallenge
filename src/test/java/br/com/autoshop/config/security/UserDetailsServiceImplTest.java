package br.com.autoshop.config.security;

import br.com.autoshop.repository.UserRepository;
import br.com.autoshop.repository.model.UserEntity;
import br.com.autoshop.util.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void loadsAuthenticatedUserWhenEmailExists() {
        UserEntity user = UserEntity.builder()
                .email("ana@example.com")
                .password("encoded-password")
                .role(RoleType.Client)
                .build();
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        var result = userDetailsService.loadUserByUsername("ana@example.com");

        assertInstanceOf(UserAuthenticated.class, result);
        assertEquals("ana@example.com", result.getUsername());
        verify(userRepository).findByEmail("ana@example.com");
    }

    @Test
    void throwsUsernameNotFoundWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com"));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail("missing@example.com");
    }
}
