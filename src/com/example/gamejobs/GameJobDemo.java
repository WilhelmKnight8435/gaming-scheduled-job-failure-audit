package com.example.gamejobs;

import java.net.http.HttpClient;

public final class GameJobDemo {
    public static void main(String[] args) {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        InfraiErrorClient client = new InfraiErrorClient(HttpClient.newHttpClient(), key);
        game_job_runner runner = new game_job_runner(client::captureReported);
        game_job_runner.Job[] jobs = {
                new game_job_runner.Job("publish-player-asset", "player-assets", () -> {}),
                new game_job_runner.Job("open-live-event", "live-events", () -> {}),
                new game_job_runner.Job("drain-moderation-queue", "moderation", () -> {
                    throw new IllegalStateException("queue depth exceeded");
                })
        };
        for (game_job_runner.Job job : jobs) {
            game_job_runner.Result result = runner.run(job);
            System.out.println(job.name() + " -> " + failure_policy.decision(result));
        }
    }
}
