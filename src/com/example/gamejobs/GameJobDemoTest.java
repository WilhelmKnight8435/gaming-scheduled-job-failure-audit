package com.example.gamejobs;

import java.net.http.HttpClient;

public final class GameJobDemoTest {
    public static void main(String[] args) {
        int[] captures = {0};
        game_job_runner runner = new game_job_runner((title, message, jobName, gameArea, runId) -> captures[0]++);
        game_job_runner.Result result = failure_policyResult(runner);
        if (result.status() != game_job_runner.Status.FAILED) throw new AssertionError(result);
        if (!"drain-moderation-queue".equals(result.jobName())) throw new AssertionError(result);
        if (captures[0] != 1) throw new AssertionError("expected one errors.capture decision");
        System.out.println("business decision: FAILED");
    }

    private static game_job_runner.Result failure_policyResult(game_job_runner runner) {
        return runner.run(new game_job_runner.Job("drain-moderation-queue", "moderation", () -> {
            throw new IllegalStateException("queue depth exceeded");
        }));
    }
}
