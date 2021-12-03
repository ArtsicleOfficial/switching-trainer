package com.switching;

public class SwitchData {

    private int numberWaySwitch;
    private int ticksTaken;
    private float switchesPerTick;
    public int timestamp;

    private void updateSwitchesPerTick() {
        switchesPerTick = (float)numberWaySwitch / (float)ticksTaken;
    }

    public SwitchData(int numberWaySwitch, int ticksTaken, int timestamp) {
        this.numberWaySwitch = numberWaySwitch;
        this.ticksTaken = ticksTaken;
        this.timestamp = timestamp;
        updateSwitchesPerTick();
    }

    public int getNumberWaySwitch() {
        return numberWaySwitch;
    }

    public void setNumberWaySwitch(int numberWaySwitch) {
        this.numberWaySwitch = numberWaySwitch;
        updateSwitchesPerTick();
    }

    public int getTicksTaken() {
        return ticksTaken;
    }

    public void setTicksTaken(int ticksTaken) {
        this.ticksTaken = ticksTaken;
        updateSwitchesPerTick();
    }

    public String toString() {
        return numberWaySwitch + " way switch in " + ticksTaken + " (" + Math.round(switchesPerTick*100.0f)/100.0f + " switches per tick)";
    }

    public float getSwitchesPerTick() {
        return switchesPerTick;
    }
}
