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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final AbsenceRepository absenceRepository;
    private final EmployeeRepository employeeRepository;

    /** Jornada estándar en minutos (configurable, default 8h = 480 min) */
    private int jornadaEstandarMinutos = 480;

    public void setJornadaEstandarMinutos(int minutos) {
        this.jornadaEstandarMinutos = minutos;
    }

    @Transactional
    public AttendanceResponse checkIn(CheckInRequest request) {
        validateEmployee(request.getEmployeeId());

        LocalDate fecha = request.getTimestamp().toLocalDate();
        // Check no open check-in for today
        var existing = attendanceRepository.findByEmployeeIdAndFechaAndHoraSalidaIsNull(request.getEmployeeId(), fecha);
        if (existing.isPresent()) {
            throw new BadRequestException("El empleado ya tiene un check-in abierto para hoy");
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(request.getEmployeeId())
                .fecha(fecha)
                .horaEntrada(request.getTimestamp().toLocalTime())
                .observacion(request.getObservacion())
                .estado(AttendanceRecord.Estado.COMPLETO)
                .build();

        record = attendanceRepository.save(record);
        return mapToResponse(record);
    }

    @Transactional
    public AttendanceResponse checkOut(CheckOutRequest request) {
        validateEmployee(request.getEmployeeId());

        LocalDate fecha = request.getTimestamp().toLocalDate();
        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndFechaAndHoraSalidaIsNull(request.getEmployeeId(), fecha)
                .orElseThrow(() -> new BadRequestException("No hay check-in abierto para este empleado hoy"));

        record.setHoraSalida(request.getTimestamp().toLocalTime());

        // Calculate worked minutes
        int totalMinutos = (int) Duration.between(record.getHoraEntrada(), record.getHoraSalida()).toMinutes();
        record.setHorasTrabajadasMinutos(totalMinutos);

        // Calculate overtime
        if (totalMinutos > jornadaEstandarMinutos) {
            record.setHorasExtraMinutos(totalMinutos - jornadaEstandarMinutos);
        } else {
            record.setHorasExtraMinutos(0);
        }

        record = attendanceRepository.save(record);
        return mapToResponse(record);
    }

    @Transactional
    public AbsenceResponse registrarAusencia(AbsenceRequest request, Long aprobadoPor) {
        validateEmployee(request.getEmployeeId());

        Absence absence = Absence.builder()
                .employeeId(request.getEmployeeId())
                .fecha(request.getFecha())
                .tipo(request.getTipo())
                .descripcion(request.getDescripcion())
                .aprobadoPor(aprobadoPor)
                .build();

        absence = absenceRepository.save(absence);

        // Mark attendance record as AUSENCIA if exists
        attendanceRepository.findByEmployeeIdAndFechaAndHoraSalidaIsNull(request.getEmployeeId(), request.getFecha())
                .ifPresent(record -> {
                    record.setEstado(AttendanceRecord.Estado.AUSENCIA);
                    attendanceRepository.save(record);
                });

        return mapToAbsenceResponse(absence);
    }

    /**
     * Scheduled task that runs daily at midnight to mark incomplete attendance records.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void marcarIncompletos() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        List<AttendanceRecord> sinSalida = attendanceRepository.findSinCheckOutEnFecha(ayer);
        for (AttendanceRecord record : sinSalida) {
            record.setEstado(AttendanceRecord.Estado.INCOMPLETO);
            attendanceRepository.save(record);
            log.warn("Attendance record {} marked as INCOMPLETO for employee {}", record.getId(), record.getEmployeeId());
        }
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendances(Long employeeId, LocalDate desde, LocalDate hasta) {
        if (employeeId != null) {
            validateEmployee(employeeId);
            return attendanceRepository.findByEmployeeIdAndFechaBetweenOrderByFechaAsc(employeeId, desde, hasta)
                    .stream().map(this::mapToResponse).toList();
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(Long employeeId, int mes, int anio) {
        validateEmployee(employeeId);

        int horasTotales = attendanceRepository.totalMinutosTrabajados(employeeId, mes, anio);
        int horasExtra = attendanceRepository.totalMinutosExtra(employeeId, mes, anio);
        int ausenciasJustificadas = absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.JUSTIFICADA, mes, anio);
        int ausenciasInjustificadas = absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.INJUSTIFICADA, mes, anio);
        int licencias = absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.LICENCIA, mes, anio);
        int vacaciones = absenceRepository.countByEmployeeIdAndTipoAndMes(employeeId, Absence.Tipo.VACACIONES, mes, anio);

        // Count days worked (records with check-out)
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());
        List<AttendanceRecord> registros = attendanceRepository.findByEmployeeIdAndFechaBetweenOrderByFechaAsc(employeeId, inicio, fin);
        int diasTrabajados = (int) registros.stream().filter(r -> r.getHoraSalida() != null).count();

        return AttendanceSummaryResponse.builder()
                .employeeId(employeeId)
                .mes(mes)
                .anio(anio)
                .diasTrabajados(diasTrabajados)
                .horasTotalesMinutos(horasTotales)
                .horasExtraMinutos(horasExtra)
                .ausenciasJustificadas(ausenciasJustificadas)
                .ausenciasInjustificadas(ausenciasInjustificadas)
                .licencias(licencias)
                .vacaciones(vacaciones)
                .build();
    }

    private void validateEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Empleado no encontrado: " + employeeId);
        }
    }

    private AttendanceResponse mapToResponse(AttendanceRecord r) {
        return AttendanceResponse.builder()
                .id(r.getId())
                .employeeId(r.getEmployeeId())
                .fecha(r.getFecha())
                .horaEntrada(r.getHoraEntrada())
                .horaSalida(r.getHoraSalida())
                .horasTrabajadasMinutos(r.getHorasTrabajadasMinutos())
                .horasExtraMinutos(r.getHorasExtraMinutos())
                .estado(r.getEstado())
                .observacion(r.getObservacion())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private AbsenceResponse mapToAbsenceResponse(Absence a) {
        return AbsenceResponse.builder()
                .id(a.getId())
                .employeeId(a.getEmployeeId())
                .fecha(a.getFecha())
                .tipo(a.getTipo())
                .descripcion(a.getDescripcion())
                .aprobadoPor(a.getAprobadoPor())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
