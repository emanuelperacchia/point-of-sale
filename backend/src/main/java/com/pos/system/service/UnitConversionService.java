package com.pos.system.service;

import com.pos.system.entity.UnitOfMeasure;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.UnitOfMeasureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitConversionService {

    private final UnitOfMeasureRepository unitOfMeasureRepository;

    /**
     * Convierte una cantidad de una unidad a otra
     */
    public BigDecimal convert(BigDecimal quantity, Long fromUnitId, Long toUnitId) {
        if (fromUnitId.equals(toUnitId)) {
            return quantity;
        }

        UnitOfMeasure fromUnit = unitOfMeasureRepository.findById(fromUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad origen no encontrada: " + fromUnitId));
        UnitOfMeasure toUnit = unitOfMeasureRepository.findById(toUnitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad destino no encontrada: " + toUnitId));

        // Validar que sean de la misma categoría
        if (fromUnit.getCategory() != toUnit.getCategory()) {
            throw new BadRequestException(String.format(
                    "No se puede convertir entre categorías diferentes: %s → %s",
                    fromUnit.getCategory(), toUnit.getCategory()));
        }

        // Convertir ambas a la unidad base y luego al destino
        BigDecimal baseQuantity = convertToBase(quantity, fromUnit);
        return convertFromBase(baseQuantity, toUnit);
    }

    /**
     * Convierte una cantidad a la unidad base de su categoría
     */
    private BigDecimal convertToBase(BigDecimal quantity, UnitOfMeasure unit) {
        if (unit.getBaseUnit()) {
            return quantity;
        }
        // Si tiene referencia a unidad base, usar el factor de conversión
        if (unit.getBaseUnitRef() != null) {
            return quantity.multiply(unit.getConversionFactor());
        }
        return quantity;
    }

    /**
     * Convierte desde la unidad base a la unidad destino
     */
    private BigDecimal convertFromBase(BigDecimal baseQuantity, UnitOfMeasure unit) {
        if (unit.getBaseUnit()) {
            return baseQuantity;
        }
        if (unit.getBaseUnitRef() != null) {
            return baseQuantity.divide(unit.getConversionFactor(), unit.getDecimalPlaces(), RoundingMode.HALF_UP);
        }
        return baseQuantity;
    }

    /**
     * Obtiene todas las unidades activas
     */
    public List<UnitOfMeasure> getAllActiveUnits() {
        return unitOfMeasureRepository.findByActiveTrue();
    }

    /**
     * Obtiene unidades por categoría
     */
    public List<UnitOfMeasure> getUnitsByCategory(UnitOfMeasure.UnitCategory category) {
        return unitOfMeasureRepository.findByCategory(category);
    }

    /**
     * CRUD - Crear unidad
     */
    @Transactional
    public UnitOfMeasure createUnit(UnitOfMeasure unit) {
        if (unitOfMeasureRepository.existsBySymbol(unit.getSymbol())) {
            throw new BadRequestException("Ya existe una unidad con el símbolo: " + unit.getSymbol());
        }
        if (unitOfMeasureRepository.existsByName(unit.getName())) {
            throw new BadRequestException("Ya existe una unidad con el nombre: " + unit.getName());
        }
        return unitOfMeasureRepository.save(unit);
    }

    /**
     * CRUD - Actualizar unidad
     */
    @Transactional
    public UnitOfMeasure updateUnit(Long id, UnitOfMeasure unitData) {
        UnitOfMeasure unit = unitOfMeasureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada: " + id));
        unit.setName(unitData.getName());
        unit.setSymbol(unitData.getSymbol());
        unit.setDescription(unitData.getDescription());
        unit.setCategory(unitData.getCategory());
        unit.setConversionFactor(unitData.getConversionFactor());
        unit.setDecimalPlaces(unitData.getDecimalPlaces());
        unit.setActive(unitData.getActive());
        return unitOfMeasureRepository.save(unit);
    }

    /**
     * CRUD - Eliminar (desactivar) unidad
     */
    @Transactional
    public void deactivateUnit(Long id) {
        UnitOfMeasure unit = unitOfMeasureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada: " + id));
        unit.setActive(false);
        unitOfMeasureRepository.save(unit);
    }
}