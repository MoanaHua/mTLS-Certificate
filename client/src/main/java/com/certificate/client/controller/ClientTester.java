package com.certificate.client.controller;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Component
public class ClientTester {

    @Value("${SSL_TRUSTSTORE_PASSWORD}")
    private String trustStorePassword;

    @Value("${TARGET_SERVER_HOST:localhost}")
    private String targetHost;

    @Value("${TARGET_SERVER_PORT:8443}")
    private int targetPort;

    @Value("${SSL_KEYSTORE_PASSWORD}")
    private String validKeystorePassword;

    @Value("${SSL_KEYSTORE_PASSWORD_EXPIRED}")
    private String expiredKeystorePassword;

    @Value("${SSL_KEYSTORE_PASSWORD_MISMATCH}")
    private String mismatchKeystorePassword;

    @Value("${SSL_KEYSTORE_PASSWORD_INVALID}")
    private String invalidKeystorePassword;

    @Value("${SSL_KEYSTORE_PASSWORD_UNTRUSTED}")
    private String untrustedKeystorePassword;

    @EventListener(ApplicationReadyEvent.class)
    public void runTests() {
        test("Valid", "keystore/client.keystore.p12", validKeystorePassword);
        test("Expired", "keystore/expired_client.keystore.p12", expiredKeystorePassword);
        test("Mismatch", "keystore/mismatch_client.keystore.p12", mismatchKeystorePassword);
        test("Invalid", "keystore/invalid_client.keystore.p12", invalidKeystorePassword);
        test("Untrusted", "keystore/untrusted_client.keystore.p12", untrustedKeystorePassword);
    }

    private void test(String certi, String keystorePath, String password) {
    	System.out.println("\n======  START TEST - " + certi + " ======");
        try {
            WebClient webClient = createMtlsWebClient(keystorePath, password);
            String uri = "https://" + targetHost + ":" + targetPort + "/";

            webClient.get()
                    .uri(uri)
                    // once received response, will close the connection
                    .headers(h -> h.set(HttpHeaders.CONNECTION, "close"))
                    .retrieve()
                    .toEntity(String.class)
                    .doOnSuccess(r -> System.out.println(certi + " Success: " + r.getStatusCode()))
                    .doOnError(e -> System.err.println(certi + " Error: " + e.getMessage()))
                    .block(); // Wait for result
        } catch (Exception e) {
            System.err.println(certi + " Initialization Error: " + e.getMessage());
        }
        System.out.println("====== END TEST ======\n");
    }

    private WebClient createMtlsWebClient(String keyStorePath, String keyStorePassword) throws Exception {
        // Keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = new ClassPathResource(keyStorePath).getInputStream()) {
            keyStore.load(in, keyStorePassword.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword.toCharArray());

        // Truststore
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = new ClassPathResource("client.truststore.p12").getInputStream()) {
            trustStore.load(in, trustStorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(kmf)
                .trustManager(tmf)
                .protocols("TLSv1.3")
                .build();

        ConnectionProvider provider = ConnectionProvider.newConnection();
        HttpClient httpClient = HttpClient.create(provider)
                .keepAlive(false)
                .secure(ssl -> ssl
                        .sslContext(sslContext)
                        .handlerConfigurator(h -> {
                            SSLEngine engine = h.engine();
                            SSLParameters params = engine.getSSLParameters();
                            params.setEndpointIdentificationAlgorithm("HTTPS");
                            engine.setSSLParameters(params);
                        })
                )
                .headers(h -> h.set(HttpHeaders.HOST, targetHost + ":" + targetPort));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

