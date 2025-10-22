package burp.service;

import burp.api.montoya.MontoyaApi;

public class SettingsService {

  private static final String API_ENDPOINT_KEY = "ainalyzer.api.endpoint";
  private static final String MODEL_NAME_KEY = "ainalyzer.model.name";

  private static final String DEFAULT_API_ENDPOINT = "http://100.89.179.67:1234/v1/chat/completions";
  private static final String DEFAULT_MODEL_NAME = "meta-llama-3.1-8b-instruct";

  private final MontoyaApi api;

  private String apiEndpoint;
  private String modelName;

  public SettingsService(MontoyaApi api) {
    this.api = api;
    loadSettings();
  }

  private void loadSettings() {
    this.apiEndpoint = api.persistence().preferences().getString(API_ENDPOINT_KEY);
    this.modelName = api.persistence().preferences().getString(MODEL_NAME_KEY);

    if (this.apiEndpoint == null || this.apiEndpoint.isEmpty()) {
      setApiEndpoint(DEFAULT_API_ENDPOINT);
    }
    if (this.modelName == null || this.modelName.isEmpty()) {
      setModelName(DEFAULT_MODEL_NAME);
    }
  }

  public String getApiEndpoint() {
    return apiEndpoint;
  }

  public String getModelName() {
    return modelName;
  }

  public void setApiEndpoint(String apiEndpoint) {
    this.apiEndpoint = apiEndpoint;
    api.persistence().preferences().setString(API_ENDPOINT_KEY, apiEndpoint);
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
    api.persistence().preferences().setString(MODEL_NAME_KEY, modelName);
  }
}
