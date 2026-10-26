package br.com.autoshop.controller;

import br.com.autoshop.dto.TokenResponseDTO;
import br.com.autoshop.dto.UserDTO;
import br.com.autoshop.model.UserEntity;
import br.com.autoshop.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController authController;

    @Test
    void signupCreatesUserWhenEmailIsNotRegistered() {
        UserDTO dto = userDto();
        when(authService.findByEmail(dto.getEmail())).thenReturn(null);

        var response = authController.post(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).save(dto);
    }

    @Test
    void signupReturnsConflictWhenEmailAlreadyExists() {
        UserDTO dto = userDto();
        when(authService.findByEmail(dto.getEmail())).thenReturn(
                UserEntity.builder().email(dto.getEmail()).password("hash").build());

        var response = authController.post(dto);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(authService, never()).save(any());
    }

    @Test
    void authenticateReturnsTokenResponseFromService() {
        String authorization = "Basic YW5hQGV4YW1wbGUuY29tOnNlY3JldA==";
        TokenResponseDTO token = new TokenResponseDTO("jwt-token", Instant.now().plusSeconds(3600));
        when(authService.generateToken(authorization)).thenReturn(token);

        var response = authController.post(authorization);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(token, response.getBody());
        verify(authService).generateToken(authorization);
    }

    private static UserDTO userDto() {
        return UserDTO.builder()
                .email("ana@example.com")
                .password("secret")
                .role("MECHANIC")
                .build();
    }
}
