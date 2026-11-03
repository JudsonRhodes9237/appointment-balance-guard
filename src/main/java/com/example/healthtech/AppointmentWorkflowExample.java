package com.example.healthtech;

public final class AppointmentWorkflowExample {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException("Usage: AppointmentWorkflowExample <recipient>");
        }
        AutoRechargePolicy policy = new AutoRechargePolicy();
        AutoRechargePolicy.Decision decision = policy.decide(true, 1.25);
        if (decision != AutoRechargePolicy.Decision.NOTIFY_PATIENT) return;

        InfraiClient infrai = new InfraiClient();
        System.out.println(infrai.configureAutoRecharge(5.0, 25.0));
        System.out.println(infrai.sendPatientNotice(args[0], "Appointment confirmed",
                "Your appointment is confirmed. Please contact the care team if you need to reschedule."));
    }
}
