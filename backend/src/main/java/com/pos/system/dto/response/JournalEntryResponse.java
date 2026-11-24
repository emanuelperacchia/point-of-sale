package com.pos.system.dto.response;

import java.time.LocalDateTime;

public class JournalEntryResponse {
    private Long id;
    private String descripcion;
    private LocalDateTime fecha;

    public JournalEntryResponse() {}

    public JournalEntryResponse(Long id, String descripcion, LocalDateTime fecha) {
        this.id = id;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
