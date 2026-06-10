package com.pos.system.service;

import com.pos.system.dto.request.AbsenceRequest;
import com.pos.system.dto.request.CheckInRequest;
import com.pos.system.dto.request.CheckOutRequest;
import com.pos.system.dto.response.AbsenceResponse;
import com.pos.system.dto.response.AttendanceResponse;
import com.pos.system.dto.response.AttendanceSummaryResponse;
import com.pos.system.entity.Absence;
import com.pos.system.entity.AttendanceRecord;
import com.pos.system.exception.BadRequestException;
import com.pos.system.exception.ResourceNotFoundException;
import com.pos.system.repository.AbsenceRepository;
import com.pos.system.repository.AttendanceRecordRepository;
import com.pos.system.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRecordRepository attendanceRepository;
    @Mock private AbsenceRepository absenceRepository;
    @Mock private EmployeeRepository employeeRepository;

    private AttendanceService attendanceService;

    private final Long employeeId = 1L;
    private final Long aprobadoPor = 10L;
    private final LocalDate today = LocalDate.of(2024, 6, 10);
    private final LocalTime entrada = LocalTime.of(8, 0);
    private final LocalTime salida = LocalTime.of(17, 0);

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(attendanceRepository, absenceRepository, employeeRepository);
    }

    @Test
    void checkIn_ShouldCreateRecord() {
        CheckInRequest request = CheckInRequest.builder()
                .employeeId(employeeId)
                .timestamp(LocalDateTime.of(today, entrada))
                .observacion("Llego temprano")
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(attendanceRepository.findByEmployeeIdAndFechaAndHoraSalidaIsNull(employeeId, today))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> {
                    AttendanceRecord saved = invocation.getArgument(0);
                    saved.setId(100L);
                    return saved;
                });

        AttendanceResponse response = attendanceService.checkIn(request);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(employeeId, response.getEmployeeId()),
                () -> assertEquals(today, response.getFecha()),
                () -> assertEquals(entrada, response.getHoraEntrada()),
                () -> assertEquals(AttendanceRecord.Estado.COMPLETO, response.getEstado()),
                () -> assertEquals("Llego temprano", response.getObservacion())
        );

        verify(attendanceRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void checkIn_WhenAlreadyOpen_ShouldThrowBadRequest() {
        CheckInRequest request = CheckInRequest.builder()
                .employeeId(employeeId)
                .timestamp(LocalDateTime.of(today, entrada))
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(attendanceRepository.findByEmployeeIdAndFechaAndHoraSalidaIsNull(employeeId, today))
                .thenReturn(Optional.of(new AttendanceRecord()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkIn(request));
        assertTrue(ex.getMessage().contains("check-in abierto"));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_WhenEmployeeNotFound_ShouldThrowResourceNotFound() {
        CheckInRequest request = CheckInRequest.builder()
                .employeeId(999L)
                .timestamp(LocalDateTime.of(today, entrada))
                .build();

        when(employeeRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> attendanceService.checkIn(request));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkOut_ShouldCalculateHours() {
        CheckOutRequest request = CheckOutRequest.builder()
                .employeeId(employeeId)
                .timestamp(LocalDateTime.of(today, salida))
                .build();

        AttendanceRecord openRecord = AttendanceRecord.builder()
                .id(200L)
                .employeeId(employeeId)
                .fecha(today)
                .horaEntrada(entrada)
                .estado(AttendanceRecord.Estado.COMPLETO)
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(attendanceRepository.findByEmployeeIdAndFechaAndHoraSalidaIsNull(employeeId, today))
                .thenReturn(Optional.of(openRecord));
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AttendanceResponse response = attendanceService.checkOut(request);

        assertAll(
                () -> assertEquals(Integer.valueOf(540), response.getHorasTrabajadasMinutos()),
                () -> assertEquals(Integer.valueOf(60), response.getHorasExtraMinutos()),
                () -> assertEquals(salida, response.getHoraSalida())
        );
        verify(attendanceRepository).save(openRecord);
    }

    @Test
    void checkOut_WhenNoOpenCheckIn_ShouldThrowBadRequest() {
        CheckOutRequest request = CheckOutRequest.builder()
                .employeeId(employeeId)
                .timestamp(LocalDateTime.of(today, salida))
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(attendanceRepository.findByEmployeeIdAndFechaAndHoraSalidaIsNull(employeeId, today))
                .thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> attendanceService.checkOut(request));
        assertTrue(ex.getMessage().contains("No hay check-in"));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void registrarAusencia_ShouldCreateAbsenceAndMarkAttendance() {
        AbsenceRequest request = AbsenceRequest.builder()
                .employeeId(employeeId)
                .fecha(today)
                .tipo(Absence.Tipo.JUSTIFICADA)
                .descripcion("Consulta medica")
                .build();

        AttendanceRecord openRecord = AttendanceRecord.builder()
                .id(300L)
                .employeeId(employeeId)
                .fecha(today)
                .horaEntrada(entrada)
                .estado(AttendanceRecord.Estado.COMPLETO)
                .build();

        Absence savedAbsence = Absence.builder()
                .id(400L)
                .employeeId(employeeId)
                .fecha(today)
                .tipo(Absence.Tipo.JUSTIFICADA)
                .descripcion("Consulta medica")
                .aprobadoPor(aprobadoPor)
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(attendanceRepository.findByEmployeeIdAndFechaAndHoraSalidaIsNull(employeeId, today))
                .thenReturn(Optional.of(openRecord));
        when(absenceRepository.save(any(Absence.class))).thenReturn(savedAbsence);

        AbsenceResponse response = attendanceService.registrarAusencia(request, aprobadoPor);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(employeeId, response.getEmployeeId()),
                () -> assertEquals(today, response.getFecha()),
                () -> assertEquals(Absence.Tipo.JUSTIFICADA, response.getTipo()),
                () -> assertEquals("Consulta medica", response.getDescripcion()),
                () -> assertEquals(aprobadoPor, response.getAprobadoPor())
        );

        verify(absenceRepository).save(any(Absence.class));
        verify(attendanceRepository).save(openRecord);
        assertEquals(AttendanceRecord.Estado.AUSENCIA, openRecord.getEstado());
    }

    @Test
    void marcarIncompletos_ShouldMarkOpenRecordsAsIncompleto() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        AttendanceRecord record1 = AttendanceRecord.builder()
                .id(500L)
                .employeeId(employeeId)
                .fecha(ayer)
                .horaEntrada(entrada)
                .estado(AttendanceRecord.Estado.COMPLETO)
                .build();
        AttendanceRecord record2 = AttendanceRecord.builder()
                .id(501L)
                .employeeId(2L)
                .fecha(ayer)
                .horaEntrada(LocalTime.of(9, 0))
                .estado(AttendanceRecord.Estado.COMPLETO)
                .build();

        when(attendanceRepository.findSinCheckOutEnFecha(ayer))
                .thenReturn(List.of(record1, record2));
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        attendanceService.marcarIncompletos();

        assertAll(
                () -> assertEquals(AttendanceRecord.Estado.INCOMPLETO, record1.getEstado()),
                () -> assertEquals(AttendanceRecord.Estado.INCOMPLETO, record2.getEstado())
        );
        verify(attendanceRepository, times(2)).save(any(AttendanceRecord.class));
    }

    @Test
    void getAttendances_ShouldReturnList() {
        LocalDate desde = today.withDayOfMonth(1);
        LocalDate hasta = today;

        AttendanceRecord record = AttendanceRecord.builder()
                .id(600L)
                .employeeId(employeeId)
                .fecha(today)
                .horaEntrada(entrada)
                .horaSalida(salida)
                .horasTrabajadasMinutos(540)
                .horasExtraMinutos(60)
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(attendanceRepository.findByEmployeeIdAndFechaBetweenOrderByFechaAsc(employeeId, desde, hasta))
                .thenReturn(List.of(record));

        List<AttendanceResponse> results = attendanceService.getAttendances(employeeId, desde, hasta);

        assertEquals(1, results.size());
        assertEquals(employeeId, results.get(0).getEmployeeId());
        assertEquals(entrada, results.get(0).getHoraEntrada());
        assertEquals(salida, results.get(0).getHoraSalida());
        assertEquals(Integer.valueOf(540), results.get(0).getHorasTrabajadasMinutos());
    }

    @Test
    void getSummary_ShouldReturnAggregatedData() {
        int mes = 6;
        int anio = 2024;
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());

        AttendanceRecord r1 = AttendanceRecord.builder()
                .employeeId(employeeId).fecha(inicio.plusDays(1))
                .horaEntrada(entrada).horaSalida(salida)
                .build();
        AttendanceRecord r2 = AttendanceRecord.builder()
                .employeeId(employeeId).fecha(inicio.plusDays(2))
                .horaEntrada(entrada).horaSalida(salida)
                .build();
        AttendanceRecord r3 = AttendanceRecord.builder()
                .employeeId(employeeId).fecha(inicio.plusDays(3))
                .horaEntrada(entrada).horaSalida(null)
                .build();

        when(employeeRepository.existsById(employeeId)).thenReturn(true);
        when(attendanceRepository.totalMinutosTrabajados(employeeId, mes, anio)).thenReturn(4800);
        when(attendanceRepository.totalMinutosExtra(employeeId, mes, anio)).thenReturn(600);
        when(absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.JUSTIFICADA, mes, anio))
                .thenReturn(1);
        when(absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.INJUSTIFICADA, mes, anio))
                .thenReturn(2);
        when(absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.LICENCIA, mes, anio))
                .thenReturn(1);
        when(absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.VACACIONES, mes, anio))
                .thenReturn(3);
        when(attendanceRepository.findByEmployeeIdAndFechaBetweenOrderByFechaAsc(employeeId, inicio, fin))
                .thenReturn(List.of(r1, r2, r3));

        AttendanceSummaryResponse summary = attendanceService.getSummary(employeeId, mes, anio);

        assertAll(
                () -> assertNotNull(summary),
                () -> assertEquals(employeeId, summary.getEmployeeId()),
                () -> assertEquals(mes, summary.getMes()),
                () -> assertEquals(anio, summary.getAnio()),
                () -> assertEquals(2, summary.getDiasTrabajados()),
                () -> assertEquals(4800, summary.getHorasTotalesMinutos()),
                () -> assertEquals(600, summary.getHorasExtraMinutos()),
                () -> assertEquals(1, summary.getAusenciasJustificadas()),
                () -> assertEquals(2, summary.getAusenciasInjustificadas()),
                () -> assertEquals(1, summary.getLicencias()),
                () -> assertEquals(3, summary.getVacaciones())
        );

        verify(attendanceRepository).totalMinutosTrabajados(employeeId, mes, anio);
        verify(attendanceRepository).totalMinutosExtra(employeeId, mes, anio);
        verify(absenceRepository, times(4)).countByEmployeeIdAndTipoAndMes(any(), any(), anyInt(), anyInt());
    }
}
