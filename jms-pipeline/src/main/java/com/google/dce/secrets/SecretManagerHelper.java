package com.google.dce.secrets;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.protobuf.ByteString;
import java.io.IOException;

public class SecretManagerHelper {

  public static class DefaultSecretManagerServiceClientHolder {
    public static final SecretManagerServiceClient INSTANCE;

    static {
      try {
        INSTANCE = SecretManagerServiceClient.create();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final SecretManagerServiceClient client;
  private final String project;

  public SecretManagerHelper(SecretManagerServiceClient client, String project) {
    this.client = client;
    this.project = project;
  }

  public ByteString downloadSecretData(String secretName) {
    AccessSecretVersionResponse response =
        client.accessSecretVersion(
            SecretVersionName.newBuilder()
                .setSecretVersion("latest")
                .setProject(project)
                .setSecret(secretName)
                .build());

    return response.getPayload().getData();
  }
}
