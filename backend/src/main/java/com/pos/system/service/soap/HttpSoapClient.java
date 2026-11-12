package com.pos.system.service.soap;

import com.pos.system.exception.BadRequestException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementación de {@link SoapClient} sobre {@link java.net.http.HttpClient}
 * (Java 17+, sin dependencias SOAP externas).
 */
@Component
@RequiredArgsConstructor
public class HttpSoapClient implements SoapClient {

    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;

    public HttpSoapClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .build();
    }

    HttpSoapClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String post(String endpoint, String soapAction, String envelope) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", soapAction)
                    .POST(HttpRequest.BodyPublishers.ofString(envelope))
                    .timeout(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new BadRequestException(
                        "Servicio SOAP respondió HTTP " + response.statusCode()
                                + " para " + endpoint);
            }

            return response.body();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Error llamando al servicio SOAP " + endpoint
                    + ": " + e.getMessage());
        }
    }
}
