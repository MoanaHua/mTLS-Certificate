//package com.certificate.client.controller;
//
//import io.netty.handler.ssl.SslContext;
//import io.netty.handler.ssl.SslContextBuilder;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.client.reactive.ReactorClientHttpConnector;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import reactor.core.publisher.Mono;
//import reactor.netty.http.client.HttpClient;
//import reactor.netty.resources.ConnectionProvider;
//
//import javax.net.ssl.KeyManagerFactory;
//import javax.net.ssl.SSLEngine;
//import javax.net.ssl.SSLParameters;
//import javax.net.ssl.TrustManagerFactory;
//import java.io.InputStream;
//import java.net.InetSocketAddress;
//import java.security.KeyStore;
//
//
//@RestController
//@RequestMapping("/test")
//public class ClientController {
//	
//	   @Value("${SSL_TRUSTSTORE_PASSWORD}")
//	   private String trustStorePassword;
//	   
//	   @Value("${TARGET_SERVER_HOST:localhost}")
//	   private String targetHost;
//	   
//	   @Value("${TARGET_SERVER_PORT:8443}")
//	   private int targetPort;
//	   
//	   @Value("${SSL_KEYSTORE_PASSWORD}")
//	   private String validKeystorePassword;
//	   
//	   @Value("${SSL_KEYSTORE_PASSWORD_EXPIRED}")
//	   private String expiredKeystorePassword;
//	   
//	   @Value("${SSL_KEYSTORE_PASSWORD_MISMATCH}")
//	   private String mismatchKeystorePassword;
//	   
//	   @Value("${SSL_KEYSTORE_PASSWORD_INVALID}")
//	   private String invalidKeystorePassword;
//	   
//	   @Value("${SSL_KEYSTORE_PASSWORD_UNTRUSTED}")
//	   private String untrustedKeystorePassword;
//	   
//	   @GetMapping("/valid")
//	   public Mono<ResponseEntity<String>> testValidCertificate() {
//	       return performMtlsRequest("keystore/client.keystore.p12", validKeystorePassword);
//	   }
//	   
//	   @GetMapping("/expired")
//	   public Mono<ResponseEntity<String>> testExpiredCertificate() {
//	       return performMtlsRequest("keystore/expired_client.keystore.p12", expiredKeystorePassword);
//	   }
//	   
//	   @GetMapping("/mismatch")
//	   public Mono<ResponseEntity<String>> testMismatchCertificate() {
//	       return performMtlsRequest("keystore/mismatch_client.keystore.p12", mismatchKeystorePassword);
//	   }
//	   
//	   @GetMapping("/invalid")
//	   public Mono<ResponseEntity<String>> testInvalidCertificate() {
//	       return performMtlsRequest("keystore/invalid_client.keystore.p12", invalidKeystorePassword);
//	   }
//	   
//	   @GetMapping("/untrusted")
//	   public Mono<ResponseEntity<String>> testUntrustedCertificate() {
//	       return performMtlsRequest("keystore/untrusted_client.keystore.p12", untrustedKeystorePassword);
//	   }
//	   
//	   private Mono<ResponseEntity<String>> performMtlsRequest(String keyStorePath, String keyStorePassword) {
//	       try {
//	           WebClient webClient = createMtlsWebClient(keyStorePath, keyStorePassword);
//	           String uri = "https://localhost:" + targetPort + "/";
//	           
//	           return webClient.get()
//	                   .uri(uri)
//	                   // Tell server close the connection once received the response
//	                   .headers(h -> h.set(HttpHeaders.CONNECTION, "close"))
//	                   
//	                   // Start from here, create TCP, then prepare to establish TLS
//	                   .retrieve()
//	                   .toEntity(String.class)
//	                   .doOnSuccess(r -> System.out.println("Success: " + r.getStatusCode()))
//	                   .doOnError(e -> System.err.println("Error: " + e.getMessage()));
//	       } catch (Exception e) {
//	           e.printStackTrace();
//	           return Mono.just(ResponseEntity.internalServerError().body("Failed to create WebClient: " + e.getMessage()));
//	       }
//	   }
//	   
//	   // create mTLS connection
//	   private WebClient createMtlsWebClient(String keyStorePath, String keyStorePassword) throws Exception {
//	       // Client Keystore
//	       KeyStore keyStore = KeyStore.getInstance("PKCS12");
//	       try (InputStream in = new ClassPathResource(keyStorePath).getInputStream()) {
//	           keyStore.load(in, keyStorePassword.toCharArray());
//	       }
//	       
//	       KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//	       kmf.init(keyStore, keyStorePassword.toCharArray());
//	       
//	       // Truststore
//	       KeyStore trustStore = KeyStore.getInstance("PKCS12");
//	       try (InputStream in = new ClassPathResource("client.truststore.p12").getInputStream()) {
//	           trustStore.load(in, trustStorePassword.toCharArray());
//	       }
//	       
//	       TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//	       tmf.init(trustStore);
//	       
//	       // SSL Context
//	       SslContext sslContext = SslContextBuilder.forClient()
//	               .keyManager(kmf)
//	               .trustManager(tmf)
//	               .protocols("TLSv1.3")
//	               .build();
//	       
//	       // Reactor Netty HttpClient
//	       // New connection every time
//	       ConnectionProvider provider = ConnectionProvider.newConnection();
//	       
//	       // Specify the target address (prepare to establish a TCP Socket Connection; no data is transmitted at the time)
//	       HttpClient httpClient = HttpClient.create(provider)
//	    		   .keepAlive(false)
//	               .secure(ssl -> ssl
//	                       .sslContext(sslContext)
//	                       .handlerConfigurator(h -> {
//	                           SSLEngine engine = h.engine();
//	                           SSLParameters params = engine.getSSLParameters();
//	                           params.setEndpointIdentificationAlgorithm("HTTPS");
//	                           engine.setSSLParameters(params);
//	                       })
//	               )
//	               .headers(h -> h.set(HttpHeaders.HOST, targetHost + ":" + targetPort));
//	       
//	       return WebClient.builder()
//	               .clientConnector(new ReactorClientHttpConnector(httpClient))
//	               .build();
//	   }
//}
