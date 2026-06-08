package com.pos.system.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageService(tempDir.toString());
        fileStorageService.init();
    }

    @Test
    void init_ShouldCreateUploadDirectory() {
        assertTrue(Files.exists(tempDir));
    }

    @Test
    void save_WithValidFile_ShouldReturnUrl() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("comprobante.pdf");
        when(file.getSize()).thenReturn(1024L);
        try {
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("PDF content".getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            fail("Mock setup failed");
        }

        // When
        String url = fileStorageService.save(file, "expenses");

        // Then
        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/expenses/"));
        assertTrue(url.endsWith(".pdf"));
    }

    @Test
    void save_WithNullFile_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.save(null, "expenses"));
    }

    @Test
    void save_WithEmptyFile_ShouldThrow() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.save(file, "expenses"));
    }

    @Test
    void save_WithOversizeFile_ShouldThrow() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6L * 1024 * 1024); // 6MB > 5MB limit

        assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.save(file, "expenses"));
    }

    @Test
    void save_WithFileWithoutExtension_ShouldSaveWithoutExtension() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("noext");
        when(file.getSize()).thenReturn(512L);
        try {
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            fail("Mock setup failed");
        }

        String url = fileStorageService.save(file, "invoices");

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/invoices/"));
        assertFalse(url.contains(".")); // UUID without extension
    }

    @Test
    void load_WhenFileExists_ShouldReturnResource() throws IOException {
        // Given - save a file first
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getSize()).thenReturn(512L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        String url = fileStorageService.save(file, "expenses");

        // When
        Resource resource = fileStorageService.load(url);

        // Then
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void load_WhenFileNotExists_ShouldThrow() {
        assertThrows(RuntimeException.class,
                () -> fileStorageService.load("/uploads/expenses/nonexistent.pdf"));
    }

    @Test
    void load_WithPathTraversal_ShouldThrow() {
        assertThrows(SecurityException.class,
                () -> fileStorageService.load("/uploads/../../../etc/passwd"));
    }

    @Test
    void delete_WhenFileExists_ShouldDelete() throws IOException {
        // Given - save a file first
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("todelete.pdf");
        when(file.getSize()).thenReturn(256L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

        String url = fileStorageService.save(file, "expenses");

        // When
        fileStorageService.delete(url);

        // Then — load() throws when file doesn't exist
        assertThrows(RuntimeException.class, () -> fileStorageService.load(url));
    }

    @Test
    void delete_WithNonExistentFile_ShouldNotThrow() {
        // Should not throw when file doesn't exist
        assertDoesNotThrow(() ->
                fileStorageService.delete("/uploads/expenses/ghost.pdf"));
    }

    @Test
    void delete_WithPathTraversal_ShouldThrow() {
        assertThrows(SecurityException.class,
                () -> fileStorageService.delete("/uploads/../../../etc/shadow"));
    }
}
