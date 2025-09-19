package com.milo.cert;

import java.io.InputStream;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collections;

import java.security.cert.*;
import java.util.Arrays;
import java.util.EnumSet;

public class CertificateValidatorWithCRL {
	private static final String Path = "revoked/";
    private static final String ROOT_CA_PEM = Path + "rootCA.pem";
    private static final String TARGET_CERT = Path + "demo_client3.pem";
    private static final String CRL_PEM = Path + "crl.pem";
	
    public static void main(String[] args) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // load root CA
        X509Certificate root = (X509Certificate) cf.generateCertificate(reqResource(ROOT_CA_PEM));

        // load certificate
        String targetPath = (args != null && args.length > 0) ? args[0] : TARGET_CERT;
        X509Certificate leaf = (X509Certificate) cf.generateCertificate(reqResource(targetPath));

        // load CRL
        X509CRL crl = (X509CRL) cf.generateCRL(reqResource(CRL_PEM));

        // create path
        CertPath certPath = cf.generateCertPath(Collections.singletonList(leaf));

        CertStore store = CertStore.getInstance(
                "Collection",
                new CollectionCertStoreParameters(Arrays.asList(crl, root))
        );

        TrustAnchor anchor = new TrustAnchor(root, null);
        PKIXParameters params = new PKIXParameters(Collections.singleton(anchor));
        params.setRevocationEnabled(true);
        params.addCertStore(store);

        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        PKIXRevocationChecker rc = (PKIXRevocationChecker) validator.getRevocationChecker();
        rc.setOptions(EnumSet.of(
                PKIXRevocationChecker.Option.PREFER_CRLS,
                PKIXRevocationChecker.Option.NO_FALLBACK
        ));
        params.addCertPathChecker(rc);

        try {
            validator.validate(certPath, params);
            System.out.println("This certificate is VALID");
        } catch (CertPathValidatorException e) {
            System.out.println("This certificate is INVALID: " + e.getReason());
            if (e.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                System.out.println("This Certificate has been REVOKED!!!");
            }
            throw e;
        }
    }

    private static InputStream reqResource(String path) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Resource not found on classpath: " + path);
        }
        return is;
    }
}

