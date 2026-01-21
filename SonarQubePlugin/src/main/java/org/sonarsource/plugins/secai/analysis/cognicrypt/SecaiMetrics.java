package org.sonarsource.plugins.secai.analysis.cognicrypt;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.measures.Metric.*;

import java.util.List;

public class SecaiMetrics implements Metrics {

    public static final Metric<String> FULL_ERRORS_JSON = new Metric.Builder(
            "secai.cognicrypt.error.tree",         // Metric key
            "CogniCrypt Error Tree (JSON)",
            ValueType.STRING)
            .setDescription("Serialized full error tree produced by CogniCrypt")
            .setDomain("Security")
            .create();

    @Override
    public List<Metric> getMetrics() {
        return List.of(FULL_ERRORS_JSON);
    }
}
