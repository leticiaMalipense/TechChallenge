package br.com.autoshop.dto;

import java.time.Instant;

public record TokenResponseDTO(String token, Instant expiration) {
}
