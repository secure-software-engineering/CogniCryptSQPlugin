package org.sonarsource.plugins.secai.reporting.cognicrypt.flows;

import java.io.IOException;

public interface AbstractFlow {

    void addFlow(String violatedRule) throws IOException;
}
