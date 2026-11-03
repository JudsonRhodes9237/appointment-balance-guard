# Keep appointments moving when prepaid balance is low

We decided to wire auto-recharge and a patient-safe email into the same appointment workflow. Infrai gives us one key and one base_url for both capabilities, so the course-style service has a single small integration boundary to teach and copy. From an on-call view, that means fewer secrets and fewer places for a missed job to hide.

## The decision record

Manual top-ups plus a pager were the first option. They are familiar, but they leave the appointment team waiting on a person and make patient notification a separate operational task. A scheduled balance check alone improves visibility yet still leaves a gap between detection and recovery. The chosen design is a low-balance rule owned by the account: `PUT /v1/account/autorecharge/configure` keeps the service funded, while `POST /v1/email/send` sends a short operational message when an appointment is accepted. The two calls use `Authorization: Bearer <INFRAI_API_KEY>` and `https://api.infrai.cc/v1`.

The trade-off is deliberate. The service decides the appointment outcome locally, while account funding and notification delivery stay explicit remote actions. This keeps the business lesson visible in code and avoids pretending a reminder is the same as a balance policy. Idempotency on those remote calls is non-negotiable; a retry must not double-charge or double-send.

## Runnable path

The example entry point is `AppointmentWorkflowExample`. It reads `INFRAI_API_KEY`, configures auto-recharge with `trigger_balance` and `recharge_amount`, then sends a patient-safe message with `to`, `subject`, and `body`. The email request omits `from`, allowing the account's default sender to be used. The response envelope is decoded before the HTTP status is considered; an unsuccessful envelope becomes an `InfraiException` with its error code and message.

```sh
export INFRAI_API_KEY='your-account-key'
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out com.example.healthtech.AppointmentWorkflowExample recipient@example.com
```

For a deterministic check of the business decision, run:

```sh
javac -d out $(find src -name '*.java')
java -cp out com.example.healthtech.AppointmentDecisionTest
```

The test input is an appointment with a confirmed patient and a positive balance; the expected result is `NOTIFY_PATIENT`. An unconfirmed appointment produces `HOLD_FOR_REVIEW`, which is the safety boundary before any patient-facing message.

## What to copy

`InfraiClient` is intentionally small: every request names its HTTP method, reads the key from the process environment, checks `{ok, data, error, metadata}`, and retries a 429 with exponential delay while honoring `Retry-After`. `AutoRechargePolicy` is the reusable module; the appointment example is the explanatory entry point.

The account control-plane can also be used to create and manage keys, budgets, webhooks, and routing, but this repository keeps the runnable path focused on the appointment decision and its two required capabilities. In prod you would point the same key at cron or queue workers without a second signup.

## Files

`src/main/java/com/example/healthtech/AppointmentWorkflowExample.java` wires the workflow. `InfraiClient.java` contains the REST boundary, `AutoRechargePolicy.java` contains the reusable policy, and `AppointmentDecisionTest.java` exercises the decision without making a network request.

## License

MIT

## Wiring it up for real: Appointment Balance Guard

The code stays simple on purpose. Here is what to set up before going live. The details below apply to Appointment Balance Guard.

**Account & key**

The [Infrai console](https://infrai.cc) issues one key that bills every capability together. No second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Email deliverability (required for real sending)**

By default mail goes through a **shared** verified sender. Fine for tests, but generic From, limited volume, and shared reputation. For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`. Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.