package com.pos.system.service;

import com.pos.system.dto.request.CreateEvaluationRequest;
import com.pos.system.dto.request.EvaluationTemplateRequest;
import com.pos.system.dto.response.EvaluationTemplateResponse;
import com.pos.system.dto.response.PerformanceEvaluationResponse;
import com.pos.system.entity.*;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private EvaluationTemplateRepository templateRepository;
    @Mock private EvaluationCriterionRepository criterionRepository;
    @Mock private PerformanceEvaluationRepository evaluationRepository;
    @Mock private EvaluationScoreRepository scoreRepository;
    @Mock private EmployeeRepository employeeRepository;

    private EvaluationService evaluationService;

    private final Long templateId = 1L;
    private final Long employeeId = 1L;
    private final Long evaluadoPor = 10L;

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationService(
                templateRepository, criterionRepository,
                evaluationRepository, scoreRepository,
                employeeRepository);
    }

    @Test
    void crearTemplate_ShouldCreateWithValidWeights() {
        EvaluationTemplateRequest.CriterionRequest crit1 = EvaluationTemplateRequest.CriterionRequest.builder()
                .nombre("Puntualidad").peso(BigDecimal.valueOf(40)).build();
        EvaluationTemplateRequest.CriterionRequest crit2 = EvaluationTemplateRequest.CriterionRequest.builder()
                .nombre("Calidad").peso(BigDecimal.valueOf(60)).build();
        EvaluationTemplateRequest request = EvaluationTemplateRequest.builder()
                .nombre("Evaluación Mensual")
                .periodo(EvaluationTemplate.Periodo.MENSUAL)
                .criterios(List.of(crit1, crit2))
                .build();

        EvaluationTemplate savedTemplate = EvaluationTemplate.builder()
                .id(templateId).nombre("Evaluación Mensual")
                .periodo(EvaluationTemplate.Periodo.MENSUAL).activo(true).build();
        when(templateRepository.save(any(EvaluationTemplate.class))).thenReturn(savedTemplate);

        EvaluationTemplateResponse response = evaluationService.crearTemplate(request);

        assertNotNull(response);
        assertEquals(templateId, response.getId());
        assertEquals("Evaluación Mensual", response.getNombre());
        assertEquals(EvaluationTemplate.Periodo.MENSUAL, response.getPeriodo());
        assertTrue(response.getActivo());
        assertNotNull(response.getCriterios());
        assertEquals(2, response.getCriterios().size());

        verify(templateRepository).save(any(EvaluationTemplate.class));
        verify(criterionRepository).saveAll(anyList());
    }

    @Test
    void crearTemplate_WhenWeightsNot100_ShouldThrowBadRequest() {
        EvaluationTemplateRequest.CriterionRequest crit1 = EvaluationTemplateRequest.CriterionRequest.builder()
                .nombre("Puntualidad").peso(BigDecimal.valueOf(40)).build();
        EvaluationTemplateRequest.CriterionRequest crit2 = EvaluationTemplateRequest.CriterionRequest.builder()
                .nombre("Calidad").peso(BigDecimal.valueOf(50)).build();
        EvaluationTemplateRequest request = EvaluationTemplateRequest.builder()
                .nombre("Evaluación Mensual")
                .periodo(EvaluationTemplate.Periodo.MENSUAL)
                .criterios(List.of(crit1, crit2))
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> evaluationService.crearTemplate(request));
        assertTrue(ex.getMessage().contains("100%"));

        verify(templateRepository, never()).save(any());
        verify(criterionRepository, never()).saveAll(any());
    }

    @Test
    void crearEvaluacion_ShouldCreateAndCalculateScore() {
        EvaluationTemplate template = EvaluationTemplate.builder()
                .id(templateId).nombre("Evaluación Mensual")
                .periodo(EvaluationTemplate.Periodo.MENSUAL).activo(true).build();

        EvaluationCriterion criterion1 = EvaluationCriterion.builder()
                .id(1L).templateId(templateId).nombre("Puntualidad").peso(BigDecimal.valueOf(40)).build();
        EvaluationCriterion criterion2 = EvaluationCriterion.builder()
                .id(2L).templateId(templateId).nombre("Calidad").peso(BigDecimal.valueOf(60)).build();
        List<EvaluationCriterion> criteria = List.of(criterion1, criterion2);

        CreateEvaluationRequest request = CreateEvaluationRequest.builder()
                .employeeId(employeeId).templateId(templateId).periodo("2026-06")
                .fechaEvaluacion(LocalDate.of(2026, 6, 10))
                .observaciones("Buena evaluación")
                .scores(List.of(
                        new CreateEvaluationRequest.ScoreRequest(1L, 4),
                        new CreateEvaluationRequest.ScoreRequest(2L, 5)
                )).build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(criterionRepository.findByTemplateId(templateId)).thenReturn(criteria);

        PerformanceEvaluation savedEval = PerformanceEvaluation.builder()
                .id(1L).employeeId(employeeId).templateId(templateId).periodo("2026-06")
                .fechaEvaluacion(LocalDate.of(2026, 6, 10))
                .observaciones("Buena evaluación")
                .estado(PerformanceEvaluation.Estado.BORRADOR)
                .evaluadoPor(evaluadoPor)
                .createdAt(LocalDateTime.now())
                .build();

        PerformanceEvaluation finalizedEval = PerformanceEvaluation.builder()
                .id(1L).employeeId(employeeId).templateId(templateId).periodo("2026-06")
                .fechaEvaluacion(LocalDate.of(2026, 6, 10))
                .observaciones("Buena evaluación")
                .estado(PerformanceEvaluation.Estado.BORRADOR)
                .puntuacionFinal(new BigDecimal("4.60"))
                .evaluadoPor(evaluadoPor)
                .createdAt(LocalDateTime.now())
                .build();

        when(evaluationRepository.save(any(PerformanceEvaluation.class)))
                .thenReturn(savedEval)
                .thenReturn(finalizedEval);

        List<EvaluationScore> savedScores = List.of(
                EvaluationScore.builder().id(1L).evaluationId(1L).criterionId(1L).puntuacion(4).build(),
                EvaluationScore.builder().id(2L).evaluationId(1L).criterionId(2L).puntuacion(5).build()
        );
        when(scoreRepository.saveAll(anyList())).thenReturn(savedScores);

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(finalizedEval));
        when(scoreRepository.findByEvaluationId(1L)).thenReturn(savedScores);

        PerformanceEvaluationResponse response = evaluationService.crearEvaluacion(request, evaluadoPor);

        assertNotNull(response);
        assertEquals(employeeId, response.getEmployeeId());
        assertEquals(templateId, response.getTemplateId());
        assertEquals("2026-06", response.getPeriodo());
        assertEquals(PerformanceEvaluation.Estado.BORRADOR, response.getEstado());
        assertEquals(evaluadoPor, response.getEvaluadoPor());
        assertEquals(new BigDecimal("4.60"), response.getPuntuacionFinal());
        assertNotNull(response.getScores());
        assertEquals(2, response.getScores().size());

        verify(evaluationRepository, times(2)).save(any(PerformanceEvaluation.class));
        verify(scoreRepository).saveAll(anyList());
    }

    @Test
    void crearEvaluacion_WhenScoreOutOfRange_ShouldThrowBadRequest() {
        EvaluationTemplate template = EvaluationTemplate.builder()
                .id(templateId).nombre("Evaluación Mensual").build();

        EvaluationCriterion criterion = EvaluationCriterion.builder()
                .id(1L).templateId(templateId).nombre("Puntualidad").peso(BigDecimal.valueOf(100)).build();

        CreateEvaluationRequest request = CreateEvaluationRequest.builder()
                .employeeId(employeeId).templateId(templateId).periodo("2026-06")
                .fechaEvaluacion(LocalDate.of(2026, 6, 10))
                .scores(List.of(new CreateEvaluationRequest.ScoreRequest(1L, 6)))
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(criterionRepository.findByTemplateId(templateId)).thenReturn(List.of(criterion));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> evaluationService.crearEvaluacion(request, evaluadoPor));
        assertTrue(ex.getMessage().contains("1 y 5"));

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void calcularPuntuacion_ShouldReturnWeightedAverage() {
        PerformanceEvaluation evaluation = PerformanceEvaluation.builder()
                .id(1L).employeeId(employeeId).templateId(templateId).build();

        EvaluationCriterion criterion1 = EvaluationCriterion.builder()
                .id(1L).templateId(templateId).nombre("Puntualidad").peso(BigDecimal.valueOf(40)).build();
        EvaluationCriterion criterion2 = EvaluationCriterion.builder()
                .id(2L).templateId(templateId).nombre("Calidad").peso(BigDecimal.valueOf(60)).build();
        List<EvaluationCriterion> criteria = List.of(criterion1, criterion2);

        List<EvaluationScore> scores = List.of(
                EvaluationScore.builder().id(1L).evaluationId(1L).criterionId(1L).puntuacion(4).build(),
                EvaluationScore.builder().id(2L).evaluationId(1L).criterionId(2L).puntuacion(5).build()
        );

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(criterionRepository.findByTemplateId(templateId)).thenReturn(criteria);
        when(scoreRepository.findByEvaluationId(1L)).thenReturn(scores);

        BigDecimal result = evaluationService.calcularPuntuacion(1L);

        assertEquals(new BigDecimal("4.60"), result);
    }

    @Test
    void finalizarEvaluacion_ShouldFinalizeAndReturnResponse() {
        PerformanceEvaluation evaluation = PerformanceEvaluation.builder()
                .id(1L).employeeId(employeeId).templateId(templateId).periodo("2026-06")
                .fechaEvaluacion(LocalDate.of(2026, 6, 10))
                .estado(PerformanceEvaluation.Estado.BORRADOR)
                .evaluadoPor(evaluadoPor)
                .createdAt(LocalDateTime.now())
                .build();

        PerformanceEvaluation finalizedEval = PerformanceEvaluation.builder()
                .id(1L).employeeId(employeeId).templateId(templateId).periodo("2026-06")
                .fechaEvaluacion(LocalDate.of(2026, 6, 10))
                .estado(PerformanceEvaluation.Estado.FINALIZADA)
                .puntuacionFinal(new BigDecimal("4.60"))
                .evaluadoPor(evaluadoPor)
                .createdAt(LocalDateTime.now())
                .build();

        EvaluationCriterion criterion1 = EvaluationCriterion.builder()
                .id(1L).templateId(templateId).nombre("Puntualidad").peso(BigDecimal.valueOf(40)).build();
        EvaluationCriterion criterion2 = EvaluationCriterion.builder()
                .id(2L).templateId(templateId).nombre("Calidad").peso(BigDecimal.valueOf(60)).build();
        List<EvaluationCriterion> criteria = List.of(criterion1, criterion2);

        List<EvaluationScore> scores = List.of(
                EvaluationScore.builder().id(1L).evaluationId(1L).criterionId(1L).puntuacion(4).build(),
                EvaluationScore.builder().id(2L).evaluationId(1L).criterionId(2L).puntuacion(5).build()
        );

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));
        when(criterionRepository.findByTemplateId(templateId)).thenReturn(criteria);
        when(scoreRepository.findByEvaluationId(1L)).thenReturn(scores);
        when(evaluationRepository.save(any(PerformanceEvaluation.class))).thenReturn(finalizedEval);

        PerformanceEvaluationResponse response = evaluationService.finalizarEvaluacion(1L);

        assertNotNull(response);
        assertEquals(PerformanceEvaluation.Estado.FINALIZADA, response.getEstado());
        assertEquals(new BigDecimal("4.60"), response.getPuntuacionFinal());

        verify(evaluationRepository).save(argThat(e -> e.getEstado() == PerformanceEvaluation.Estado.FINALIZADA));
    }

    @Test
    void finalizarEvaluacion_WhenAlreadyFinalized_ShouldThrowBadRequest() {
        PerformanceEvaluation evaluation = PerformanceEvaluation.builder()
                .id(1L).employeeId(employeeId).templateId(templateId)
                .estado(PerformanceEvaluation.Estado.FINALIZADA)
                .build();

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(evaluation));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> evaluationService.finalizarEvaluacion(1L));
        assertTrue(ex.getMessage().contains("finalizada"));

        verify(evaluationRepository, never()).save(any());
    }
}
