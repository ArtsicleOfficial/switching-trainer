package com.switching;

public enum TrainingMode {
    LIVE_FEEDBACK("Live Feedback"),
    OCCASIONAL_UPDATES("Occasional Updates"),
    BOTH("Both");

    String name;

    TrainingMode(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
