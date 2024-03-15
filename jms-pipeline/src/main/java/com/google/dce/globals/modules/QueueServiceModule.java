package com.google.dce.globals.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.dce.mq.rest.QueueService;
import com.google.dce.mq.rest.QueueServiceImpl;
import com.google.dce.options.BasicOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.QueueApi;

@SuppressWarnings("unused")
public class QueueServiceModule extends AbstractModule {

  @Provides
  @Singleton
  public QueueService queueService(QueueApi queueApi) {
    return new QueueServiceImpl(queueApi);
  }

  @Provides
  @Singleton
  public QueueApi queueApi(ApiClient apiClient) {
    return apiClient.queueApi();
  }

  @Provides
  @Singleton
  public ApiClient apiClient(
      @Named("baseApiPath") String baseApiPath,
      HttpTransport httpTransport,
      HttpRequestInitializer httpRequestInitializer,
      ObjectMapper objectMapper) {
    return new ApiClient(baseApiPath, httpTransport, httpRequestInitializer, objectMapper);
  }

  @Provides
  @Named("baseApiPath")
  public String baseApiPath(BasicOptions pipelineOptions) {
    return pipelineOptions.getMqApiBasePath();
  }

  @Provides
  @Singleton
  public HttpTransport httpTransport(@Named("mqApiX509Certificate") String x509Certificate)
      throws GeneralSecurityException, IOException {
    return new NetHttpTransport.Builder()
        .trustCertificatesFromStream(
            new ByteArrayInputStream(x509Certificate.getBytes(StandardCharsets.US_ASCII)))
        .build();
  }

  @Provides
  @Named("mqApiX509Certificate")
  public String mqApiX509Certificate(BasicOptions pipelineOptions) {
    return pipelineOptions.getMqApiX509Certificate();
  }

  @Provides
  @Singleton
  public HttpRequestInitializer httpRequestInitializer(
      @Named("mqApiBasicAuthUser") String mqApiBasicAuthUser,
      @Named("mqApiBasicAuthPassword") String mqApiBasicAuthPassword) {
    return new BasicAuthentication(mqApiBasicAuthUser, mqApiBasicAuthPassword);
  }

  @Provides
  @Named("mqApiBasicAuthUser")
  public String mqApiBasicAuthUser(BasicOptions pipelineOptions) {
    return pipelineOptions.getMqApiBasicAuthUser();
  }

  @Provides
  @Named("mqApiBasicAuthPassword")
  public String mqApiBasicAuthPassword(BasicOptions pipelineOptions) {
    return pipelineOptions.getMqApiBasicAuthPassword();
  }
}
