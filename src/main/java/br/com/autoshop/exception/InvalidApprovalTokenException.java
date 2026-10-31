package br.com.autoshop.exception;

public class InvalidApprovalTokenException extends RuntimeException {

    public InvalidApprovalTokenException() {
        super("This approval link is invalid or has expired");
    }
}
