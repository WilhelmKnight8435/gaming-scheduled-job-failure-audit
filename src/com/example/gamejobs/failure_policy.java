package com.example.gamejobs;

final class failure_policy {
    private failure_policy() {}

    static String decision(game_job_runner.Result result) {
        return result.status() == game_job_runner.Status.HEALTHY ? "HEALTHY" : "FAILED";
    }
}
