package br.com.autoshop.controller;

import br.com.autoshop.exception.ApprovalConflictException;
import br.com.autoshop.exception.InvalidApprovalTokenException;
import br.com.autoshop.service.WorkOrderApprovalTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/work-order-approvals")
public class WorkOrderApprovalController {

    private final WorkOrderApprovalTokenService approvalTokenService;
    private final String approvalBaseUrl;

    public WorkOrderApprovalController(
            WorkOrderApprovalTokenService approvalTokenService,
            @Value("${autoshop.approval.base-url:http://localhost:8080/autoshop/work-order-approvals}")
            String approvalBaseUrl
    ) {
        this.approvalTokenService = approvalTokenService;
        this.approvalBaseUrl = approvalBaseUrl.replaceAll("/+$", "");
    }

    @Operation(summary = "Abrir a página de confirmação do orçamento", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de confirmação apresentada"),
            @ApiResponse(responseCode = "410", description = "Link inválido ou expirado")
    })
    @GetMapping(value = "/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showConfirmation(@PathVariable String token) {
        if (!approvalTokenService.isValid(token)) {
            return htmlResponse(HttpStatus.GONE, messagePage("Este link de aprovação é inválido ou expirou."));
        }

        String action = approvalBaseUrl + "/" + token;
        String page = """
                <!doctype html>
                <html lang="pt-BR">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Aprovar ordem de serviço</title></head>
                <body>
                <main>
                  <h1>Aprovar orçamento da ordem de serviço</h1>
                  <p>Revise o orçamento recebido antes de confirmar. Esta ação autoriza o início do serviço.</p>
                  <form method="post" action="%s">
                    <button type="submit">Confirmar aprovação</button>
                  </form>
                </main>
                </body>
                </html>
                """.formatted(action);
        return htmlResponse(HttpStatus.OK, page);
    }

    @Operation(summary = "Confirmar a aprovação do orçamento", security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orçamento aprovado"),
            @ApiResponse(responseCode = "409", description = "OS já processada"),
            @ApiResponse(responseCode = "410", description = "Link inválido ou expirado")
    })
    @PostMapping(value = "/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmApproval(@PathVariable String token) {
        approvalTokenService.approve(token);
        return htmlResponse(HttpStatus.OK, messagePage("Orçamento aprovado. A oficina pode iniciar o serviço."));
    }

    @ExceptionHandler(InvalidApprovalTokenException.class)
    public ResponseEntity<String> handleInvalidToken() {
        return htmlResponse(HttpStatus.GONE, messagePage("Este link de aprovação é inválido ou expirou."));
    }

    @ExceptionHandler(ApprovalConflictException.class)
    public ResponseEntity<String> handleApprovalConflict() {
        return htmlResponse(HttpStatus.CONFLICT, messagePage("Esta ordem de serviço não está mais aguardando aprovação."));
    }

    private static ResponseEntity<String> htmlResponse(HttpStatus status, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setCacheControl("no-store");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "default-src 'none'; form-action 'self'; frame-ancestors 'none'");
        return new ResponseEntity<>(body, headers, status);
    }

    private static String messagePage(String message) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Aprovação da ordem de serviço</title></head>
                <body><main><h1>Aprovação da ordem de serviço</h1><p>%s</p></main></body>
                </html>
                """.formatted(message);
    }
}
