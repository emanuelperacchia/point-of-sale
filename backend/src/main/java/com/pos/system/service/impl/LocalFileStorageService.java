package com.pos.system.service.impl;

import com.pos.system.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementación local de {@link FileStorageService}.
 * Guarda archivos en {@code file-storage.upload-dir} (default: ./uploads).
 */
@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadDir;

    public LocalFileStorageService(
            @Value("${file-storage.upload-dir:./uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
            log.info("File storage directory: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de uploads: " + uploadDir, e);
        }
    }

    @Override
    public String save(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo de 5 MB");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = prefix + "/" + UUID.randomUUID() + extension;

            Path targetPath = uploadDir.resolve(filename).normalize();
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File saved: {}", targetPath);
            return "/uploads/" + filename.replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo", e);
        }
    }

    @Override
    public Resource load(String fileUrl) {
        try {
            // fileUrl viene como /uploads/expenses/uuid.pdf
            String relativePath = fileUrl.replace("/uploads/", "");
            Path filePath = uploadDir.resolve(relativePath).normalize();

            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("Acceso denegado: " + fileUrl);
            }

            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists()) {
                throw new RuntimeException("Archivo no encontrado: " + fileUrl);
            }
            return resource;
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar el archivo: " + fileUrl, e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            String relativePath = fileUrl.replace("/uploads/", "");
            Path filePath = uploadDir.resolve(relativePath).normalize();

            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("Acceso denegado: " + fileUrl);
            }

            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filePath);
        } catch (SecurityException e) {
            throw e;
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo: {}", fileUrl, e);
        }
    }
}
