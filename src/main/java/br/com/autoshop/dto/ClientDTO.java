package br.com.autoshop.dto;

import br.com.autoshop.util.DocumentType;
import br.com.autoshop.util.EmailValidatorUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ClientDTO {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Document cannot be empty")
    private String document;

    @NotNull(message = "DocumentType cannot be empty")
    private DocumentType documentType;

    private String phone;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = EmailValidatorUtil.VALID_EMAIL_ADDRESS)
    private String email;

    public ClientDTO() {

    }
}
