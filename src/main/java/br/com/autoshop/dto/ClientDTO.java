package br.com.autoshop.dto;

import br.com.autoshop.dto.validator.document.CPFOrCNPJ;
import br.com.autoshop.dto.validator.document.CPFOrCNPJValidator;
import br.com.autoshop.util.DocumentType;
import br.com.autoshop.util.EmailValidatorUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    @CPFOrCNPJ
    private String document;

    private String phone;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = EmailValidatorUtil.VALID_EMAIL_ADDRESS)
    private String email;

    public DocumentType getDocumentType() {
        if (this.document.length() == CPFOrCNPJValidator.CPF_LENGTH) {
            return DocumentType.CPF;
        } else if (this.document.length() == CPFOrCNPJValidator.CNPJ_LENGTH) {
            return DocumentType.CNPJ;
        }
        return null;
    }

    public ClientDTO() {

    }
}
