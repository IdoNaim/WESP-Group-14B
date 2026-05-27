package com.ticketpurchasingsystem.project.domain.tickets;

public class PolicyValidationResult {
    private final boolean valid;
    private final String rejectionMessage;

    private PolicyValidationResult(boolean valid, String rejectionMessage) {
        this.valid = valid;
        this.rejectionMessage = rejectionMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public String getRejectionMessage() {
        return rejectionMessage;
    }

    public static PolicyValidationResult success() {
        return new PolicyValidationResult(true, null);
    }

    public static PolicyValidationResult fail(String msg) {
        return new PolicyValidationResult(false, msg);
    }
}
