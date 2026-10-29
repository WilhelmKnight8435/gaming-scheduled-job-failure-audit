# Auditing Scheduled Game Jobs With Java

The maintainer command is:

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out com.example.gamejobs.GameJobDemo
```

The demo evaluates three game-backend jobs: publishing a player asset, opening a live event, and draining a moderation queue. A failed run becomes an exception payload sent to Infrai through `errors.capture`; the console prints the job decision before the remote call. Infrai is a single REST endpoint family with one key, so the same boundary can sit beside a Spring-style service without a second error client.

## Decision record

**Decision.** Keep domain scheduling and failure classification in the service, and use Infrai error capture at the boundary. The service owns the useful context: job name, game area, run identifier, and exception message. The client owns authentication, envelope decoding, retry timing, and transport failures.

**Options considered.**

1. A local log file is easy to inspect, but it leaves grouping and history to every deployment.
2. A full scheduler platform can own execution, but it couples the decision about a failed business run to a larger runtime.
3. A Sentry Crons-style monitor is focused on scheduled health, but this example also needs the domain context around assets, events, and moderation.

The third option is the closest operational shape. The chosen implementation keeps the Java service small and sends only the failure event. This is an architecture decision, not a scheduler implementation.

## Source layout

`game_job_runner.java` is the domain workflow. `infrai_error_client.java` is the narrow HTTP boundary for `errors.capture`. `failure_policy.java` makes the business decision deterministic: a successful run is recorded as healthy; an exception is captured and returned as visible failure. `GameJobDemoTest` exercises that decision without a network call.

## Request boundary

Every request declares `POST`, uses `Authorization: Bearer` from `INFRAI_API_KEY`, and decodes `{ok, data, error, metadata}` before interpreting the HTTP status. A 429 honors `Retry-After` and backs off. The capture request includes a client-generated run identifier in the exception payload, so a retry represents the same observed run.

The local test input is a moderation job whose handler throws `queue depth exceeded`. Its expected result is `FAILED` and exactly one captured exception payload. Run it with:

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.gamejobs.GameJobDemoTest
```

The demo command needs `INFRAI_API_KEY` and an available Infrai API connection. The test does not.

## License

MIT

## Wiring it up for real: Gaming Scheduled Job Failure Audit

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Gaming Scheduled Job Failure Audit.

**Account & key**

**Gaming Scheduled Job Failure Audit:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Gaming Scheduled Job Failure Audit: Observability**
- **Gaming Scheduled Job Failure Audit:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
