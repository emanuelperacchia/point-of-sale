package com.pos.system.service.soap;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del cliente HTTP reutilizable para llamadas SOAP.
 * <p>
 * Se mantiene en una clase {@link Configuration} propia (fuera de
 * {@link com.pos.system.service.afip.AfipAuthService}) para evitar una
 * dependencia circular: {@code AfipAuthService} inyecta {@link SoapClient},
 * {@link HttpSoapClient} inyecta {@link HttpClient}.
 * </p>
 */
@Configuration
public class SoapClientConfig {

    private static final int CONNECT_TIMEOUT_SECONDS = 10;

    @Bean
    HttpClient soapHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .build();
    }
}
