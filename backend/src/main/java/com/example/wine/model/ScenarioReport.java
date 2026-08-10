package com.example.wine.model;

import java.util.List;

public class ScenarioReport {
    private String scenario;
    private String status;
    private String severity;
    private String report;
    private List<Incident> activeIncidents;

    public ScenarioReport() {
    }

    public ScenarioReport(String scenario, String status, String severity, String report, List<Incident> activeIncidents) {
        this.scenario = scenario;
        this.status = status;
        this.severity = severity;
        this.report = report;
        this.activeIncidents = activeIncidents;
    }

    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
    public List<Incident> getActiveIncidents() { return activeIncidents; }
    public void setActiveIncidents(List<Incident> activeIncidents) { this.activeIncidents = activeIncidents; }
}
