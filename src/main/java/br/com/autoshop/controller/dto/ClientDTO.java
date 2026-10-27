package br.com.autoshop.controller.dto;

import br.com.autoshop.controller.dto.validators.document.CPFOrCNPJ;
import br.com.autoshop.controller.dto.validators.document.CPFOrCNPJValidator;
import br.com.autoshop.util.DocumentType;
import br.com.autoshop.util.EmailValidatorUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private DocumentType documentType;

    private String phone;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = EmailValidatorUtil.VALID_EMAIL_ADDRESS)
    private String email;

    public DocumentType getDocumentType() {
        if (getDocument().length() == CPFOrCNPJValidator.CPF_LENGTH) {
            return DocumentType.CPF;
        } else if (getDocument().length() == CPFOrCNPJValidator.CNPJ_LENGTH) {
            return DocumentType.CNPJ;
        }
        return null;
    }

    public String getDocument() {
        return this.document.replaceAll("[^0-9]", "");
    }

    public ClientDTO() {

    }
}
