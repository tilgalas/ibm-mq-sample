package com.google.dce;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MessageSizeTool {

  private static final String HOST = "localhost";
  private static final int PORT = 1414;
  private static final String CHANNEL = "DEV.APP.SVRCONN";
  private static final String QMGR = "QM1";
  private static final String APP_USER = "app";
  private static final String APP_PASSWORD = "password";
  private static final String TOPIC_NAME = "dev/";
  private static final String TRUST_MANAGER_FACTORY_ALGORITHM = "PKIX";
  private static final String SSL_CONTEXT_PROTOCOL = "TLS";
  private static final String KEYSTORE_TYPE = "PKCS12";
  private static final String CERTIFICATE_FACTORY_TYPE = "X.509";

  private static final String SSL_CERT_FILENAME = "/etc/ssl/certs/qmgr.pem";

  public static void main(String[] args) throws Exception {
    JmsConnectionFactory jmsConnectionFactory = getJmsConnectionFactory();
    try (JMSContext jmsContext = jmsConnectionFactory.createContext()) {
      jmsContext.setExceptionListener(JMSException::printStackTrace);
      Topic t = jmsContext.createTopic("topic://" + TOPIC_NAME);
      if ("-p".equals(args[0])) {
        TextMessage msg = jmsContext.createTextMessage(args[1]);
        msg.setIntProperty(JmsConstants.JMS_IBM_RETAIN, JmsConstants.RETAIN_PUBLICATION);
        JMSProducer producer = jmsContext.createProducer();
        producer.send(t, msg);
      } else if ("-c".equals(args[0])) {
        JMSConsumer consumer = jmsContext.createConsumer(t);
        TextMessage message = (TextMessage) consumer.receive();
        System.out.println(message.getText());
      } else {
        throw new IllegalArgumentException("unrecognized mode");
      }
    }
  }

  private static SSLSocketFactory createSslSocketFactory()
      throws NoSuchAlgorithmException,
          KeyStoreException,
          CertificateException,
          IOException,
          KeyManagementException {
    try (BufferedInputStream bufferedInputStream =
        new BufferedInputStream(new FileInputStream(SSL_CERT_FILENAME))) {
      CertificateFactory certificateFactory =
          CertificateFactory.getInstance(CERTIFICATE_FACTORY_TYPE);
      X509Certificate certificate =
          (X509Certificate) certificateFactory.generateCertificate(bufferedInputStream);
      KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
      keyStore.load(null, null);
      keyStore.setCertificateEntry("cert", certificate);
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TRUST_MANAGER_FACTORY_ALGORITHM);
      trustManagerFactory.init(keyStore);
      SSLContext sslContext = SSLContext.getInstance(SSL_CONTEXT_PROTOCOL);
      sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
      return sslContext.getSocketFactory();
    }
  }

  private static JmsConnectionFactory getJmsConnectionFactory()
      throws CertificateException,
          NoSuchAlgorithmException,
          KeyStoreException,
          IOException,
          KeyManagementException {
    try {
      JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
      JmsConnectionFactory cf = ff.createConnectionFactory();

      // Set the properties
      cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
      cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
      cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
      cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
      cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
      cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "MessageSizeTool");
      cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
      cf.setStringProperty(WMQConstants.USERID, APP_USER);
      cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
      cf.setObjectProperty(WMQConstants.WMQ_SSL_SOCKET_FACTORY, createSslSocketFactory());
      cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "*TLS12ORHIGHER");
      return cf;
    } catch (JMSException e) {
      throw new RuntimeException(e);
    }
  }
}
