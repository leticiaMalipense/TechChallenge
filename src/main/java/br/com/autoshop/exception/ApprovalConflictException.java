package br.com.autoshop.exception;

public class ApprovalConflictException extends RuntimeException {

    public ApprovalConflictException() {
        super("This work order is no longer awaiting approval");
    }
}
