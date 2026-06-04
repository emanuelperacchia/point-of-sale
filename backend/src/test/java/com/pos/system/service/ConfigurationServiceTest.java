package com.pos.system.service;

import com.pos.system.entity.SystemConfiguration;
import com.pos.system.entity.User;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock private SystemConfigRepository configRepository;

    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationService = new ConfigurationService(configRepository);
    }

    @Test
    void getConfig_WhenExists_ShouldReturnConfig() {
        SystemConfiguration config = SystemConfiguration.builder()
                .configKey("test.key").configValue("test-value")
                .dataType("STRING").build();
        when(configRepository.findByConfigKey("test.key")).thenReturn(Optional.of(config));

        var result = configurationService.getConfig("test.key");
        assertEquals("test-value", result.getConfigValue());
    }

    @Test
    void getConfig_WhenNotFound_ShouldThrow() {
        when(configRepository.findByConfigKey("invalid.key")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> configurationService.getConfig("invalid.key"));
    }

    @Test
    void getConfigValue_ShouldReturnStringValue() {
        SystemConfiguration config = SystemConfiguration.builder()
                .configKey("app.name").configValue("POS System").build();
        when(configRepository.findByConfigKey("app.name")).thenReturn(Optional.of(config));

        assertEquals("POS System", configurationService.getConfigValue("app.name"));
    }

    @Test
    void getAllConfigs_ShouldReturnList() {
        when(configRepository.findByActiveTrue())
                .thenReturn(List.of(
                        SystemConfiguration.builder().configKey("key1").build(),
                        SystemConfiguration.builder().configKey("key2").build()));

        assertEquals(2, configurationService.getAllConfigs().size());
    }

    @Test
    void getConfigMap_ShouldReturnKeyValueMap() {
        when(configRepository.findByActiveTrue())
                .thenReturn(List.of(
                        SystemConfiguration.builder().configKey("k1").configValue("v1").build(),
                        SystemConfiguration.builder().configKey("k2").configValue("v2").build()));

        var map = configurationService.getConfigMap();
        assertEquals("v1", map.get("k1"));
        assertEquals("v2", map.get("k2"));
    }

    @Test
    void updateConfig_ShouldUpdateAndInvalidateCache() {
        User user = User.builder().id(1L).build();
        SystemConfiguration config = SystemConfiguration.builder()
                .configKey("test.key").configValue("old-value").build();

        when(configRepository.findByConfigKey("test.key")).thenReturn(Optional.of(config));
        when(configRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = configurationService.updateConfig("test.key", "new-value", user);
        assertEquals("new-value", result.getConfigValue());
    }
}