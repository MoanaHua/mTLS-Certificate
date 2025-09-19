package com.milo.cert;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

public class CertificateInspector {
	
	public static void main(String[] args) throws Exception{
		
		// Pass the file path parameter
		String path = "src/main/resources/";
		String certificatePem = "chain_client.pem";
		
		String certPath = path + certificatePem;

		// loading certificate
		try (InputStream inStream = new FileInputStream(certPath)){
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			
		    @SuppressWarnings("unchecked")
		    Collection<X509Certificate> certs = (Collection<X509Certificate>) (Collection<?>) cf.generateCertificates(inStream);

			System.out.println("Current Running Certificate: " + certificatePem + "\n");
			System.out.println("======== Certificate Information ========\n");
			
			int idx = 0;
		    for (X509Certificate cert : certs) {
		        System.out.println("-- Cert #" + (++idx) + " --");
		        
		        // Subject / Issuer
		        System.out.println("Subject: " + cert.getSubjectX500Principal());
		        System.out.println("Issuer: " + cert.getIssuerX500Principal());
		        
		        // Serial Number
		        System.out.println("Serial Number: " + cert.getSerialNumber().toString(16).toUpperCase());
		        
		        // Validity
		        System.out.println("Valid From: " + cert.getNotBefore());
		        System.out.println("Valid Until: " + cert.getNotAfter());
		        
		        // Signature
		        System.out.println("Signature Algorithm: " + cert.getSigAlgName());

		        // SAN
		        try {
		            var altNames = cert.getSubjectAlternativeNames();
		            if (altNames != null) {
		                System.out.print("Subject Alt Names: ");
		                for (var item : altNames) System.out.print(item.get(1) + " ");
		                System.out.println();
		            } else {
		                System.out.println("Subject Alt Names: None");
		            }
		        } catch (Exception e) {
		            System.out.println("Failed to extract SAN: " + e.getMessage());
		        }

		        // SHA-256 fingerprint
		        byte[] encoded = cert.getEncoded();
		        System.out.println("SHA-256 Fingerprint: " + fingerprint("SHA-256", encoded));
		        System.out.println();
		    }
		}
}
	
	private static String fingerprint(String algorithm, byte[] data) throws Exception{
		MessageDigest md = MessageDigest.getInstance(algorithm);
		byte[] digest = md.digest(data);
		StringBuilder sb = new StringBuilder();
		for(byte b : digest) {
			sb.append(String.format("%02X: ", b));
		}
		return sb.substring(0, sb.length());
	}
}
