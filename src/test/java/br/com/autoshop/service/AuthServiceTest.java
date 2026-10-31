package br.com.autoshop.service;

import br.com.autoshop.controller.dto.TokenResponseDTO;
import br.com.autoshop.controller.dto.UserDTO;
import br.com.autoshop.repository.UserRepository;
import br.com.autoshop.repository.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtService, userRepository);
    }

    @Test
    void generateTokenDecodesBasicCredentialsAuthenticatesAndReturnsJwtResponse() {
        String authorization = basicAuthorization("ana@example.com", "secret");
        Authentication authentication = mock(Authentication.class);
        TokenResponseDTO expected = new TokenResponseDTO("jwt-token", Instant.now().plusSeconds(3600));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(authentication)).thenReturn(expected);

        TokenResponseDTO result = authService.generateToken(authorization);

        assertSame(expected, result);
        verify(authenticationManager).authenticate(argThat(token ->
                token != null
                        && "ana@example.com".equals(token.getPrincipal())
                        && "secret".equals(token.getCredentials())));
        verify(jwtService).generateToken(authentication);
    }

    @Test
    void generateTokenPropagatesAuthenticationFailureAndDoesNotGenerateJwt() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.generateToken(basicAuthorization("ana@example.com", "wrong")));

        verifyNoInteractions(jwtService);
    }

    @Test
    void findByEmailReturnsUserWhenRepositoryFindsOne() {
        UserEntity user = UserEntity.builder().email("ana@example.com").password("hash").build();
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        assertSame(user, authService.findByEmail("ana@example.com"));
    }

    @Test
    void findByEmailReturnsNullWhenRepositoryHasNoMatch() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertNull(authService.findByEmail("missing@example.com"));
    }

    @Test
    void savePersistsEmailAndBcryptEncodedPassword() {
        UserDTO dto = UserDTO.builder()
                .email("ana@example.com")
                .password("secret")
                .role("MECHANIC")
                .build();

        authService.save(dto);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();
        assertEquals("ana@example.com", saved.getEmail());
        assertNotEquals("secret", saved.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("secret", saved.getPassword()));
    }

    private static String basicAuthorization(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
