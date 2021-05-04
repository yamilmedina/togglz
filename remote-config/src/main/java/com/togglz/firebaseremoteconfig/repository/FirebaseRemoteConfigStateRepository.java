package com.togglz.firebaseremoteconfig.repository;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.Parameter;
import com.google.firebase.remoteconfig.ParameterValue;
import com.google.firebase.remoteconfig.Template;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.cache.CachingStateRepository;
import org.togglz.core.util.Preconditions;

import java.util.concurrent.ExecutionException;

public class FirebaseRemoteConfigStateRepository implements StateRepository {

    private final FirebaseRemoteConfig firebaseRemoteConfig;

    private FirebaseRemoteConfigStateRepository(Builder builder) {
        firebaseRemoteConfig = builder.firebaseRemoteConfig;
    }

    @Override
    public FeatureState getFeatureState(Feature feature) {
        return getFeatureStateFromFirebase(feature);
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
        public static final long DEFAULT_TTL_12_HOURS_MS = 43_200_000L;

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
        private FirebaseRemoteConfigStateRepository buildStateRepository() {
            Preconditions.checkArgument(firebaseRemoteConfig != null,
                    "FirebaseRemoteConfig is null, make sure FirebaseApp is initialized");
            return new FirebaseRemoteConfigStateRepository(this);
        }

        /**
         * Creates a new {@link FirebaseRemoteConfigStateRepository} wrapped under a {@link CachingStateRepository}.
         * This enforces an efficient usage for the project avoiding throttling.
         *
         * @param ttl time to live for the cached strategy
         */
        public CachingStateRepository build(long ttl) {
            Preconditions.checkArgument(ttl > DEFAULT_TTL_12_HOURS_MS,
                    "The Firebase suggested min. time to cached the values is 12 hours");
            return new CachingStateRepository(buildStateRepository(), ttl);
        }
    }

}
