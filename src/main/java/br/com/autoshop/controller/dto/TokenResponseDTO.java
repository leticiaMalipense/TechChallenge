package br.com.autoshop.controller.dto;

import java.time.Instant;

public record TokenResponseDTO(String token, Instant expiration) {
}
