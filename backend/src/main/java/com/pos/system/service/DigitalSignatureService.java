package com.pos.system.service;

import com.pos.system.entity.DigitalCertificate;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Firma digitalmente el XML del comprobante usando un certificado PKCS#12.
 * <p>
 * Utiliza la API de Firma Digital XML de Java (JSR 105) combinada con
 * Bouncy Castle como proveedor de seguridad para el manejo del keystore.
 * </p>
 */
@Service
public class DigitalSignatureService {

    private static final Logger log = LoggerFactory.getLogger(DigitalSignatureService.class);

    private static final String SIGNATURE_ALGORITHM = SignatureMethod.RSA_SHA256;
    private static final String DIGEST_ALGORITHM = DigestMethod.SHA256;
    private static final String KEYSTORE_TYPE = "PKCS12";

    /**
     * Firma el XML del comprobante con el certificado digital provisto.
     * <p>
     * Agrega una firma XML enveloped dentro del documento, incluyendo
     * el certificado X.509 en la KeyInfo para validación posterior.
     * </p>
     *
     * @param xmlContent  contenido XML sin firmar
     * @param certificate certificado digital (PKCS#12) con clave privada
     * @return XML firmado como String
     * @throws RuntimeException si ocurre algún error en el proceso de firma
     */
    public String signXml(String xmlContent, DigitalCertificate certificate) {
        try {
            // Registrar Bouncy Castle como provider de seguridad
            Security.addProvider(new BouncyCastleProvider());

            // Cargar keystore PKCS#12
            char[] password = certificate.getStorePassword().toCharArray();
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE, "BC");
            ks.load(new ByteArrayInputStream(certificate.getCertificateData()), password);

            // Extraer clave privada y certificado
            String alias = ks.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password);
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            // Parsear XML a firmar
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            // Configurar firma XML digital
            XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
            KeyInfoFactory kif = sigFactory.getKeyInfoFactory();

            // Referencia: firma enveloped sobre todo el documento
            Reference ref = sigFactory.newReference(
                    "",
                    sigFactory.newDigestMethod(DIGEST_ALGORITHM, null),
                    Collections.singletonList(
                            sigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null
            );

            SignedInfo signedInfo = sigFactory.newSignedInfo(
                    sigFactory.newCanonicalizationMethod(
                            CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    sigFactory.newSignatureMethod(SIGNATURE_ALGORITHM, null),
                    Collections.singletonList(ref)
            );

            // KeyInfo con certificado X.509
            X509Data x509Data = kif.newX509Data(Collections.singletonList(cert));
            KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(x509Data));

            // Firmar
            DOMSignContext signContext = new DOMSignContext(privateKey, doc.getDocumentElement());
            XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyInfo);
            signature.sign(signContext);

            // Convertir documento firmado a string
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            log.info("XML firmado digitalmente con certificado: {}", alias);
            return writer.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error al firmar XML del comprobante: " + e.getMessage(), e);
        }
    }
}
