package br.com.autoshop.controller;

import br.com.autoshop.dto.TokenResponseDTO;
import br.com.autoshop.dto.UserDTO;
import br.com.autoshop.model.UserEntity;
import br.com.autoshop.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> post(@Valid @RequestBody UserDTO dto) {
        UserEntity user = authService.findByEmail(dto.getEmail());
        if (Objects.nonNull(user)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        authService.save(dto);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/auth")
    public ResponseEntity<TokenResponseDTO> post(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorisation) {
        TokenResponseDTO tokenResponse = authService.generateToken(authorisation);
        return ResponseEntity.ok(tokenResponse);
    }
}
