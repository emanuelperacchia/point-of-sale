package com.pos.system.service.soap;

/**
 * Cliente SOAP mínimo y testeable.
 * <p>
 * Permite inyectar implementaciones alternativas (stubs en tests,
 * cliente HTTP real en producción) sin acoplar a una librería SOAP pesada.
 * </p>
 */
public interface SoapClient {

    /**
     * Envía un sobre SOAP y devuelve el cuerpo de la respuesta como String.
     *
     * @param endpoint   URL del servicio SOAP
     * @param soapAction valor de la cabecera SOAPAction
     * @param envelope   sobre SOAP completo (XML)
     * @return cuerpo de la respuesta SOAP (XML)
     */
    String post(String endpoint, String soapAction, String envelope);
}
