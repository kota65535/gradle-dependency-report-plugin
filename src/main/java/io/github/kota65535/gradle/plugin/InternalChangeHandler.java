package io.github.kota65535.gradle.plugin;


import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.UnresolvableConfigurationResult;

public class InternalChangeHandler {

    static public UnresolvableConfigurationResult createUnresolvableConfigurationResult(Configuration configuration) {
        try {
            return (UnresolvableConfigurationResult) UnresolvableConfigurationResult.class
                    .getMethod("of", Configuration.class)
                    .invoke(null, configuration);
        } catch (Exception e) {
            // do nothing
        }
        try {
            return UnresolvableConfigurationResult.class
                    .getConstructor(Configuration.class)
                    .newInstance(configuration);
        } catch (Exception e) {
            // do nothing
        }
        throw new IllegalStateException("createUnresolvableConfigurationResult failed");
    }

    private InternalChangeHandler() {
    }
}
