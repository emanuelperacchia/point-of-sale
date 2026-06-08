package com.pos.system.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstracción para almacenamiento de archivos (comprobantes, attachments).
 * Implementación local por defecto; reemplazar con S3 en producción.
 */
public interface FileStorageService {

    /**
     * Guarda un archivo y retorna la URL para accederlo.
     *
     * @param file   archivo multipart
     * @param prefix subdirectorio de agrupación (ej. "expenses")
     * @return URL relativa del archivo guardado
     */
    String save(MultipartFile file, String prefix);

    /**
     * Carga un archivo por su URL.
     *
     * @param fileUrl URL retornada por {@link #save}
     * @return Resource del archivo
     */
    Resource load(String fileUrl);

    /**
     * Elimina un archivo por su URL.
     *
     * @param fileUrl URL retornada por {@link #save}
     */
    void delete(String fileUrl);
}
