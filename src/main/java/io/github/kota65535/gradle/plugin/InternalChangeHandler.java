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

    static public <R, T> R handleMethodNameChange(T object, List<String> methodNames) {
        Method method = null;
        for (String name: methodNames) {
            try {
                method = ConfigurationInternal.class.getMethod(name);
            } catch (NoSuchMethodException e) {
                // do nothing
            }
        }
        if (method == null) {
            throw new NoSuchElementException("method not found %s".formatted(methodNames));
        }
        try {
            return (R) method.invoke(object);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalArgumentException("failed to call method %s".formatted(method.getName()));
        }
    }

    private InternalChangeHandler() {
    }
}
