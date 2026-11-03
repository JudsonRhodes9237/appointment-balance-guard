package com.example.healthtech;

public final class AutoRechargePolicy {
    public enum Decision { NOTIFY_PATIENT, HOLD_FOR_REVIEW }

    public Decision decide(boolean patientConfirmed, double balanceUsd) {
        if (patientConfirmed && balanceUsd >= 0) {
            return Decision.NOTIFY_PATIENT;
        }
        return Decision.HOLD_FOR_REVIEW;
    }
}
