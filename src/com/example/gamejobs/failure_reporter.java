package com.example.gamejobs;

@FunctionalInterface
interface failure_reporter {
    void capture(String title, String message, String jobName, String gameArea, String runId);
}
