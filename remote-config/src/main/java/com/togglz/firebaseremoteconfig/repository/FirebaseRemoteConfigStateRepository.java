package com.togglz.firebaseremoteconfig.repository;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.Parameter;
import com.google.firebase.remoteconfig.ParameterValue;
import com.google.firebase.remoteconfig.Template;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.util.Preconditions;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class FirebaseRemoteConfigStateRepository implements StateRepository {

    private final FirebaseRemoteConfig firebaseRemoteConfig;
    private final ConcurrentMap<String, FeatureState> states;

    private FirebaseRemoteConfigStateRepository(Builder builder) {
        states = new ConcurrentHashMap<>();
        firebaseRemoteConfig = builder.firebaseRemoteConfig;
    }

    @Override
    public FeatureState getFeatureState(Feature feature) {
        if (states.get(feature.name()) != null) {
            return states.get(feature.name());
        }

        FeatureState featureState = getFeatureStateFromFirebase(feature);
        if (featureState != null) {
            states.put(feature.name(), featureState);
        }
        return featureState;
    }

    private FeatureState getFeatureStateFromFirebase(Feature feature) {
        FeatureState featureState = null;
        try {
            Template template = firebaseRemoteConfig.getTemplateAsync().get();
            Parameter parameter = template.getParameters().get(feature.name());
            if (parameter != null) {
                ParameterValue parameterValue = parameter.getConditionalValues()
                        .values().stream()
                        .findFirst()
                        .orElse(parameter.getDefaultValue());
                ParameterValue.Explicit theValue = ParameterValue.Explicit.of(parameterValue.toString());
                featureState = new FeatureState(feature, isTrue(theValue.getValue()));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to get remote configuration from Firebase", e);
        }
        return featureState;
    }

    private static boolean isTrue(String s) {
        return s != null
                && ("true".equalsIgnoreCase(s.trim()) || "yes".equalsIgnoreCase(s.trim())
                || "enabled".equalsIgnoreCase(s.trim()) || "enable".equalsIgnoreCase(s.trim()));
    }

    @Override
    public void setFeatureState(FeatureState featureState) {
        throw new UnsupportedOperationException("Firebase RemoteConfig StateRepository only supports reading from");
    }

    /**
     * Creates a new builder for a {@link FirebaseRemoteConfigStateRepository}.
     *
     * @param firebaseRemoteConfig the initialized instance of {@link FirebaseRemoteConfig}.
     * @return A Builder instance
     */
    public static Builder newBuilder(FirebaseRemoteConfig firebaseRemoteConfig) {
        return new Builder(firebaseRemoteConfig);
    }

    /**
     * Builder for a {@link FirebaseRemoteConfigStateRepository}.
     */
    public static class Builder {

        private final FirebaseRemoteConfig firebaseRemoteConfig;

        /**
         * Creates a new builder for a {@link FirebaseRemoteConfigStateRepository}.
         *
         * @param firebaseRemoteConfig the initialized instance of {@link FirebaseRemoteConfig}.
         */
        public Builder(FirebaseRemoteConfig firebaseRemoteConfig) {
            this.firebaseRemoteConfig = firebaseRemoteConfig;
        }

        /**
         * Creates a new {@link FirebaseRemoteConfigStateRepository} using the current pre-initialized Firebase client.
         */
        public FirebaseRemoteConfigStateRepository build() {
            Preconditions.checkArgument(firebaseRemoteConfig != null,
                    "FirebaseRemoteConfig is null, make sure FirebaseApp is initialized");
            return new FirebaseRemoteConfigStateRepository(this);
        }
    }

    /**
     * This class represents a cached repository lookup
     * <p>
     * Note: based on {@link org.togglz.core.repository.cache.CachingStateRepository}
     * TODO: consider use the class above and delegate through this impl
     */
    private static class CacheEntry implements Serializable {

        private final FeatureState state;

        public CacheEntry(FeatureState state) {
            this.state = state;
        }

        public FeatureState getState() {
            return state;
        }

    }

}
