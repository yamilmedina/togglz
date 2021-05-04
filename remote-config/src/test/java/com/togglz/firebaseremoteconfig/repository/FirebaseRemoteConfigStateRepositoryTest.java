package com.togglz.firebaseremoteconfig.repository;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.togglz.core.repository.cache.CachingStateRepository;

import static com.togglz.firebaseremoteconfig.repository.FirebaseRemoteConfigStateRepository.Builder.DEFAULT_TTL_12_HOURS_MS;
import static org.mockito.Mockito.mock;

class FirebaseRemoteConfigStateRepositoryTest {

    private CachingStateRepository configStateRepository;

    @BeforeEach
    void setUp() {
        configStateRepository = FirebaseRemoteConfigStateRepository
                .newBuilder(mock(FirebaseRemoteConfig.class))
                .build(DEFAULT_TTL_12_HOURS_MS);
    }


}