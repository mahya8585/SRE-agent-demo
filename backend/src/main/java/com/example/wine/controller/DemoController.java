package com.example.wine.controller;

import com.example.wine.model.Incident;
import com.example.wine.model.ScenarioReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/incidents")
    public List<Incident> incidents() {
        return Arrays.asList(
                new Incident("INC-1001", "Latency spike in inventory lookup", "Inventory queries started returning slowly after the overnight upload.", "Investigating"),
                new Incident("INC-1002", "DB connectivity issue", "A wrong connection secret caused temporary database failures.", "Resolved")
        );
    }

    @PostMapping("/scenarios/{scenario}")
    public ScenarioReport injectScenario(@PathVariable String scenario) {
        if ("latency".equalsIgnoreCase(scenario)) {
            return new ScenarioReport(
                    "latency",
                    "Investigating",
                    "High",
                    "The inventory service is experiencing elevated latency after a batch import burst. The SRE agent should validate cache health and retry budgets.",
                    Arrays.asList(
                            new Incident("INC-1003", "Latency spike injected into inventory lookups", "Response times climbed above 1.8s for high-volume lookup requests.", "Investigating")
                    )
            );
        }

        if ("db".equalsIgnoreCase(scenario)) {
            return new ScenarioReport(
                    "db",
                    "Mitigating",
                    "Critical",
                    "The primary database connection pool is saturated after a credential rotation. The SRE agent should fail over to the read replica and reset the secret rotation plan.",
                    Arrays.asList(
                            new Incident("INC-1004", "Database connectivity fault", "Connection pool exhaustion is blocking order fulfillment and stock reconciliation.", "Mitigating")
                    )
            );
        }

        return new ScenarioReport(
                scenario,
                "Unknown",
                "Low",
                "No matching scenario was found. Use latency or db.",
                Arrays.asList()
        );
    }
}
