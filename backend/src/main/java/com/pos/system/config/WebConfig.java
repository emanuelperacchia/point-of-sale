package com.pos.system.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuración web MVC.
 * <p>
 * El resource handler estático de {@code /uploads/**} fue eliminado a
 * propósito: los archivos ahora se sirven vía {@code FileDownloadController}
 * con autorización por rol (antes eran públicos).
 * </p>
 */
@Configuration
public class WebConfig {
}