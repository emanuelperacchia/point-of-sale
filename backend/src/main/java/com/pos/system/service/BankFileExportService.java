package com.pos.system.service;

import com.pos.system.entity.Payroll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Genera archivo de exportación bancaria para pago de sueldos.
 * Formato CSV con cabecera estándar compatible con transferencias masivas.
 */
@Service
@RequiredArgsConstructor
public class BankFileExportService {

    private final EmployeeService employeeService;

    /**
     * Genera un archivo CSV simple con los datos de pago.
     * Cabecera: CUIL;Apellido;Nombre;Importe;Concepto;Mes;Anio
     */
    public byte[] generateCsv(List<Payroll> payrolls) {
        StringBuilder sb = new StringBuilder();
        sb.append("CUIL;Apellido;Nombre;Importe;Concepto;Mes;Anio\n");

        for (Payroll p : payrolls) {
            var emp = employeeService.getById(p.getEmployeeId());
            String cuil = emp.getCuil() != null ? emp.getCuil() : "";
            String apellido = emp.getApellido() != null ? emp.getApellido() : "";
            String nombre = emp.getNombre() != null ? emp.getNombre() : "";
            String importe = p.getNetoApagar() != null
                    ? p.getNetoApagar().setScale(2, BigDecimal.ROUND_HALF_UP).toString()
                    : "0.00";

            sb.append(escapeCsv(cuil)).append(";")
                    .append(escapeCsv(apellido)).append(";")
                    .append(escapeCsv(nombre)).append(";")
                    .append(importe).append(";")
                    .append("Sueldo ").append(p.getMes()).append("/").append(p.getAnio()).append(";")
                    .append(p.getMes()).append(";")
                    .append(p.getAnio()).append("\n");
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
