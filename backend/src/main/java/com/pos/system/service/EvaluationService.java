package com.pos.system.service;

import com.pos.system.dto.request.CreateEvaluationRequest;
import com.pos.system.dto.request.EvaluationTemplateRequest;
import com.pos.system.dto.response.EvaluationTemplateResponse;
import com.pos.system.dto.response.PerformanceEvaluationResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationTemplateRepository templateRepository;
    private final EvaluationCriterionRepository criterionRepository;
    private final PerformanceEvaluationRepository evaluationRepository;
    private final EvaluationScoreRepository scoreRepository;
    private final EmployeeRepository employeeRepository;

    // ── Templates ───────────────────────────────────────────────────

    @Transactional
    public EvaluationTemplateResponse crearTemplate(EvaluationTemplateRequest request) {
        // Validate weights sum to 100
        BigDecimal sum = request.getCriterios().stream()
                .map(EvaluationTemplateRequest.CriterionRequest::getPeso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new BadRequestException("La suma de los pesos de los criterios debe ser 100% (actual: " + sum + "%)");
        }

        EvaluationTemplate template = EvaluationTemplate.builder()
                .nombre(request.getNombre())
                .periodo(request.getPeriodo())
                .activo(true)
                .build();
        template = templateRepository.save(template);

        EvaluationTemplate finalTemplate = template;
        List<EvaluationCriterion> criteria = request.getCriterios().stream()
                .map(c -> EvaluationCriterion.builder()
                        .templateId(finalTemplate.getId())
                        .nombre(c.getNombre())
                        .peso(c.getPeso())
                        .build())
                .toList();
        criterionRepository.saveAll(criteria);

        return mapToTemplateResponse(template, criteria);
    }

    @Transactional(readOnly = true)
    public List<EvaluationTemplateResponse> listarTemplates() {
        List<EvaluationTemplate> templates = templateRepository.findByActivoTrue();
        List<Long> templateIds = templates.stream().map(EvaluationTemplate::getId).toList();
        List<EvaluationCriterion> allCriteria = criterionRepository.findByTemplateIdIn(templateIds);
        Map<Long, List<EvaluationCriterion>> criteriaByTemplate = allCriteria.stream()
                .collect(Collectors.groupingBy(EvaluationCriterion::getTemplateId));

        return templates.stream()
                .map(t -> mapToTemplateResponse(t, criteriaByTemplate.getOrDefault(t.getId(), List.of())))
                .toList();
    }

    // ── Evaluations ─────────────────────────────────────────────────

    @Transactional
    public PerformanceEvaluationResponse crearEvaluacion(CreateEvaluationRequest request, Long evaluadoPor) {
        if (!employeeRepository.existsById(request.getEmployeeId())) {
            throw new ResourceNotFoundException("Empleado no encontrado: " + request.getEmployeeId());
        }

        EvaluationTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada: " + request.getTemplateId()));

        List<EvaluationCriterion> criteria = criterionRepository.findByTemplateId(template.getId());

        // Validate all criteria have scores
        Map<Long, Integer> scoreMap = request.getScores().stream()
                .collect(Collectors.toMap(CreateEvaluationRequest.ScoreRequest::getCriterionId,
                        CreateEvaluationRequest.ScoreRequest::getPuntuacion));

        for (EvaluationCriterion criterion : criteria) {
            Integer score = scoreMap.get(criterion.getId());
            if (score == null) {
                throw new BadRequestException("Falta puntuación para el criterio: " + criterion.getNombre());
            }
            if (score < 1 || score > 5) {
                throw new BadRequestException("La puntuación debe estar entre 1 y 5 (criterio: " + criterion.getNombre() + ")");
            }
        }

        PerformanceEvaluation evaluation = PerformanceEvaluation.builder()
                .employeeId(request.getEmployeeId())
                .templateId(request.getTemplateId())
                .periodo(request.getPeriodo())
                .fechaEvaluacion(request.getFechaEvaluacion())
                .observaciones(request.getObservaciones())
                .estado(PerformanceEvaluation.Estado.BORRADOR)
                .evaluadoPor(evaluadoPor)
                .build();
        evaluation = evaluationRepository.save(evaluation);

        PerformanceEvaluation finalEvaluation = evaluation;
        List<EvaluationScore> scores = criteria.stream()
                .map(c -> EvaluationScore.builder()
                        .evaluationId(finalEvaluation.getId())
                        .criterionId(c.getId())
                        .puntuacion(scoreMap.get(c.getId()))
                        .build())
                .toList();
        scoreRepository.saveAll(scores);

        // Calculate final score
        BigDecimal puntuacionFinal = calcularPuntuacion(evaluation.getId());
        evaluation.setPuntuacionFinal(puntuacionFinal);
        evaluation = evaluationRepository.save(evaluation);

        return mapToEvaluationResponse(evaluation, criteria, scores);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularPuntuacion(Long evaluationId) {
        PerformanceEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada: " + evaluationId));

        List<EvaluationCriterion> criteria = criterionRepository.findByTemplateId(evaluation.getTemplateId());
        List<EvaluationScore> scores = scoreRepository.findByEvaluationId(evaluationId);

        Map<Long, Integer> scoreMap = scores.stream()
                .collect(Collectors.toMap(EvaluationScore::getCriterionId, EvaluationScore::getPuntuacion));

        BigDecimal total = BigDecimal.ZERO;
        for (EvaluationCriterion criterion : criteria) {
            Integer score = scoreMap.get(criterion.getId());
            if (score != null) {
                BigDecimal peso = criterion.getPeso().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                total = total.add(BigDecimal.valueOf(score).multiply(peso));
            }
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public PerformanceEvaluationResponse finalizarEvaluacion(Long evaluationId) {
        PerformanceEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación no encontrada: " + evaluationId));

        if (evaluation.getEstado() == PerformanceEvaluation.Estado.FINALIZADA) {
            throw new BadRequestException("La evaluación ya está finalizada");
        }

        // Recalculate final score
        BigDecimal puntuacionFinal = calcularPuntuacion(evaluationId);
        evaluation.setPuntuacionFinal(puntuacionFinal);
        evaluation.setEstado(PerformanceEvaluation.Estado.FINALIZADA);
        evaluation = evaluationRepository.save(evaluation);

        List<EvaluationCriterion> criteria = criterionRepository.findByTemplateId(evaluation.getTemplateId());
        List<EvaluationScore> scores = scoreRepository.findByEvaluationId(evaluationId);

        return mapToEvaluationResponse(evaluation, criteria, scores);
    }

    @Transactional(readOnly = true)
    public List<PerformanceEvaluationResponse> getEvaluationsByEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Empleado no encontrado: " + employeeId);
        }
        List<PerformanceEvaluation> evaluations = evaluationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        return evaluations.stream().map(this::mapToEvaluationResponse).toList();
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private EvaluationTemplateResponse mapToTemplateResponse(EvaluationTemplate t, List<EvaluationCriterion> criteria) {
        return EvaluationTemplateResponse.builder()
                .id(t.getId()).nombre(t.getNombre())
                .periodo(t.getPeriodo()).activo(t.getActivo())
                .criterios(criteria.stream()
                        .map(c -> EvaluationTemplateResponse.CriterionResponse.builder()
                                .id(c.getId()).nombre(c.getNombre()).peso(c.getPeso())
                                .build())
                        .toList())
                .build();
    }

    private PerformanceEvaluationResponse mapToEvaluationResponse(PerformanceEvaluation e) {
        List<EvaluationCriterion> criteria = criterionRepository.findByTemplateId(e.getTemplateId());
        List<EvaluationScore> scores = scoreRepository.findByEvaluationId(e.getId());
        return mapToEvaluationResponse(e, criteria, scores);
    }

    private PerformanceEvaluationResponse mapToEvaluationResponse(
            PerformanceEvaluation e, List<EvaluationCriterion> criteria, List<EvaluationScore> scores) {

        Map<Long, String> criterionNames = criteria.stream()
                .collect(Collectors.toMap(EvaluationCriterion::getId, EvaluationCriterion::getNombre));

        return PerformanceEvaluationResponse.builder()
                .id(e.getId()).employeeId(e.getEmployeeId())
                .templateId(e.getTemplateId())
                .templateName(criteria.isEmpty() ? "" : criteria.get(0).getNombre())
                .periodo(e.getPeriodo())
                .fechaEvaluacion(e.getFechaEvaluacion())
                .puntuacionFinal(e.getPuntuacionFinal())
                .observaciones(e.getObservaciones())
                .estado(e.getEstado())
                .evaluadoPor(e.getEvaluadoPor())
                .scores(scores.stream()
                        .map(s -> PerformanceEvaluationResponse.ScoreResponse.builder()
                                .id(s.getId()).criterionId(s.getCriterionId())
                                .criterionName(criterionNames.getOrDefault(s.getCriterionId(), ""))
                                .puntuacion(s.getPuntuacion())
                                .build())
                        .toList())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
