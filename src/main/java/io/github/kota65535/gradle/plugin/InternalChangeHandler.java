package io.github.kota65535.gradle.plugin;


import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.UnresolvableConfigurationResult;
import org.gradle.internal.deprecation.DeprecatableConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.NoSuchElementException;

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

    static public boolean configurationInternalIsDeclarableByExtension(ConfigurationInternal configuration) {
        Method method = null;
        List<String> methodNames = List.of("isDeclarableByExtension", "isDeclarableAgainstByExtension");
        for (String name: methodNames) {
            try {
                method = ConfigurationInternal.class.getMethod(name);
            } catch (NoSuchMethodException e) {
                // do nothing
            }
        }
        if (method == null) {
            throw new NoSuchElementException("None of method found %s".formatted(methodNames));
        }
        try {
            return (boolean) method.invoke(configuration);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalArgumentException("failed to call method %s".formatted(method.getName()));
        }
    }

    static public boolean DeprecatableConfigurationIsDeprecatedForResolution(DeprecatableConfiguration configuration) {
        try {
            return (boolean) DeprecatableConfiguration.class
                    .getMethod("isDeprecatedForResolution")
                    .invoke(configuration);
        } catch (Exception e) {
            // do nothing
        }
        return configuration.getResolutionAlternatives() != null;
    }

    private InternalChangeHandler() {
    }
}
