package com.google.dce.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLUtils {
  private static final String TRUST_MANAGER_FACTORY_ALGORITHM = "PKIX";
  private static final String SSL_CONTEXT_PROTOCOL = "TLS";
  private static final String KEYSTORE_TYPE = "PKCS12";
  private static final String CERTIFICATE_FACTORY_TYPE = "X.509";

  public static X509Certificate createCertificate(String pemCertificate)
      throws CertificateException {
    CertificateFactory certificateFactory =
        CertificateFactory.getInstance(CERTIFICATE_FACTORY_TYPE);
    return (X509Certificate)
        certificateFactory.generateCertificate(new ByteArrayInputStream(pemCertificate.getBytes()));
  }

  public static KeyStore createKeystore(Map<String, Certificate> aliasToCertMap)
      throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

    keyStore.load(null, null);
    for (Map.Entry<String, Certificate> e : aliasToCertMap.entrySet()) {
      keyStore.setCertificateEntry(e.getKey(), e.getValue());
    }

    return keyStore;
  }

  public static SSLSocketFactory createSSLSocketFactory(TrustManager[] trustManagers)
      throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance(SSL_CONTEXT_PROTOCOL);
    sslContext.init(null, trustManagers, null);
    return sslContext.getSocketFactory();
  }

  public static X509TrustManager createTrustManager(KeyStore keyStore)
      throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory trustManagerFactory =
        TrustManagerFactory.getInstance(TRUST_MANAGER_FACTORY_ALGORITHM);
    trustManagerFactory.init(keyStore);
    return Arrays.stream(trustManagerFactory.getTrustManagers())
        .filter(tm -> tm instanceof X509TrustManager)
        .map(X509TrustManager.class::cast)
        .findFirst()
        .orElseThrow();
  }

  public static SSLSocketFactory createSSLSocketFactoryForCert(String pemCertificate)
      throws NoSuchAlgorithmException,
          KeyManagementException,
          CertificateException,
          KeyStoreException,
          IOException {
    return createSSLSocketFactory(
        new TrustManager[] {
          createTrustManager(createKeystore(Map.of("cert", createCertificate(pemCertificate))))
        });
  }

  public static SSLSocketFactory createSSLSocketFactoryForCertUnchecked(String pemCertificate) {
    try {
      return createSSLSocketFactoryForCert(pemCertificate);
    } catch (NoSuchAlgorithmException
        | KeyManagementException
        | CertificateException
        | KeyStoreException
        | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
