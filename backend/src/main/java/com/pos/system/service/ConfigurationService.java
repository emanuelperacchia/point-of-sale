package com.pos.system.service;

import com.pos.system.entity.SystemConfiguration;
import com.pos.system.entity.User;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigurationService {

    private final SystemConfigRepository configRepository;

    /**
     * Obtiene una configuración por key (con cache)
     */
    @Cacheable(value = "systemConfig", key = "#configKey")
    public SystemConfiguration getConfig(String configKey) {
        return configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuración no encontrada: " + configKey));
    }

    /**
     * Obtiene el valor como string
     */
    @Cacheable(value = "systemConfig", key = "'value:' + #configKey")
    public String getConfigValue(String configKey) {
        SystemConfiguration config = getConfig(configKey);
        return config.getConfigValue();
    }

    /**
     * Obtiene el valor como entero
     */
    public Integer getIntegerConfig(String configKey) {
        return getConfig(configKey).getIntegerValue();
    }

    /**
     * Obtiene el valor como booleano
     */
    public Boolean getBooleanConfig(String configKey) {
        return getConfig(configKey).getBooleanValue();
    }

    /**
     * Obtiene el valor como double
     */
    public Double getDoubleConfig(String configKey) {
        return getConfig(configKey).getDoubleValue();
    }

    /**
     * Obtiene todas las configuraciones activas
     */
    public List<SystemConfiguration> getAllConfigs() {
        return configRepository.findByActiveTrue();
    }

    /**
     * Obtiene configuraciones por grupo
     */
    public List<SystemConfiguration> getConfigsByGroup(String groupName) {
        return configRepository.findByGroupName(groupName);
    }

    /**
     * Obtiene todas las configuraciones como mapa clave-valor
     */
    public Map<String, String> getConfigMap() {
        List<SystemConfiguration> configs = configRepository.findByActiveTrue();
        Map<String, String> map = new HashMap<>();
        for (SystemConfiguration config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }

    /**
     * Actualiza una configuración (invalida cache)
     */
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public SystemConfiguration updateConfig(String configKey, String configValue, User user) {
        SystemConfiguration config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuración no encontrada: " + configKey));

        validateValue(config, configValue);
        config.setConfigValue(configValue);
        config.setUpdatedBy(user);
        return configRepository.save(config);
    }

    /**
     * Crea una nueva configuración
     */
    @Transactional
    @CacheEvict(value = "systemConfig", allEntries = true)
    public SystemConfiguration createConfig(SystemConfiguration config) {
        if (configRepository.existsByConfigKey(config.getConfigKey())) {
            throw new BadRequestException(
                    "Ya existe una configuración con la key: " + config.getConfigKey());
        }
        return configRepository.save(config);
    }

    /**
     * Valida el valor contra las reglas definidas
     */
    private void validateValue(SystemConfiguration config, String value) {
        if (config.getValidationRules() != null && !config.getValidationRules().isEmpty()) {
            String[] rules = config.getValidationRules().split(";");
            for (String rule : rules) {
                String[] parts = rule.split(":");
                if (parts.length == 2) {
                    String ruleName = parts[0].trim();
                    String ruleValue = parts[1].trim();
                    switch (ruleName.toUpperCase()) {
                        case "MIN" -> {
                            if (Double.parseDouble(value) < Double.parseDouble(ruleValue)) {
                                throw new BadRequestException(
                                        "El valor mínimo permitido es: " + ruleValue);
                            }
                        }
                        case "MAX" -> {
                            if (Double.parseDouble(value) > Double.parseDouble(ruleValue)) {
                                throw new BadRequestException(
                                        "El valor máximo permitido es: " + ruleValue);
                            }
                        }
                        case "PATTERN" -> {
                            if (!value.matches(ruleValue)) {
                                throw new BadRequestException(
                                        "El valor no cumple con el formato requerido");
                            }
                        }
                    }
                }
            }
        }
    }
}