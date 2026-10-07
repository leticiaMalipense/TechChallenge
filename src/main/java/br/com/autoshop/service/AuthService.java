package br.com.autoshop.service;

import br.com.autoshop.dto.TokenResponseDTO;
import br.com.autoshop.dto.UserDTO;
import br.com.autoshop.model.UserEntity;
import br.com.autoshop.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@AllArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public TokenResponseDTO generateToken(String authorisation) {

        String[] values = getCredentials(authorisation);

        var token = new UsernamePasswordAuthenticationToken(values[0], values[1]);
        Authentication authentication = authenticationManager.authenticate(token);

        return jwtService.generateToken(authentication);
    }

    private static String @NonNull [] getCredentials(String authorisation) {
        String base64Credentials = authorisation.substring(6).trim();
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded, StandardCharsets.UTF_8);
        return credentials.split(":", 2);
    }

    public UserEntity findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElse(null);
    }

    public void save(@Valid UserDTO dto) {

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserEntity userEntity = UserEntity.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword())).build();
        userRepository.save(userEntity);
    }
}
