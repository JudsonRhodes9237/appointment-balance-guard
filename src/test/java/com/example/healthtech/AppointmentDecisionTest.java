package com.example.healthtech;

public final class AppointmentDecisionTest {
    public static void main(String[] args) {
        AutoRechargePolicy policy = new AutoRechargePolicy();
        require(policy.decide(true, 1.25) == AutoRechargePolicy.Decision.NOTIFY_PATIENT, "confirmed appointment");
        require(policy.decide(false, 1.25) == AutoRechargePolicy.Decision.HOLD_FOR_REVIEW, "unconfirmed appointment");
        require(policy.decide(true, -0.01) == AutoRechargePolicy.Decision.HOLD_FOR_REVIEW, "negative balance");
        System.out.println("AppointmentDecisionTest passed");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
