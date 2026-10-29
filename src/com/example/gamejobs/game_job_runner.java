package com.example.gamejobs;

import java.util.Objects;

final class game_job_runner {
    enum Status { HEALTHY, FAILED }
    record Job(String name, String gameArea, Runnable action) {}
    record Result(Status status, String jobName, String message) {}

    private final failure_reporter reporter;

    game_job_runner(failure_reporter reporter) { this.reporter = Objects.requireNonNull(reporter); }

    Result run(Job job) {
        String runId = InfraiErrorClient.newRunId();
        try {
            job.action().run();
            return new Result(Status.HEALTHY, job.name(), "completed");
        } catch (RuntimeException failure) {
            reporter.capture(job.name() + " failed", failure.getMessage(), job.name(), job.gameArea(), runId);
            return new Result(Status.FAILED, job.name(), failure.getMessage());
        }
    }
}
