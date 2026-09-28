package com.example.grievance.entity;

public enum Priority {
    LOW(120), MEDIUM(72), HIGH(48), CRITICAL(24);

    private final int slaHours;
    Priority(int slaHours) { this.slaHours = slaHours; }
    public int getSlaHours() { return slaHours; }
}
