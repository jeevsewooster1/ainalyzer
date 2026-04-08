package burp.service;

import burp.api.montoya.MontoyaApi;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsService {

  private static final String API_ENDPOINT_KEY = "ainalyzer.api.endpoint";
  private static final String MODEL_NAME_KEY = "ainalyzer.model.name";
  private static final String API_KEY_KEY = "ainalyzer.api.key";
  private static final String EXTRA_HEADERS_KEY = "ainalyzer.extra.headers";

  private static final String DEFAULT_API_ENDPOINT = "http://localhost:1234/v1/chat/completions";
  private static final String DEFAULT_MODEL_NAME = "meta-llama-3.1-8b-instruct";

  private final MontoyaApi api;

  private String apiEndpoint;
  private String modelName;
  private String apiKey;
  private String extraHeaders;

  public SettingsService(MontoyaApi api) {
    this.api = api;
    loadSettings();
  }

  private void loadSettings() {
    this.apiEndpoint = api.persistence().preferences().getString(API_ENDPOINT_KEY);
    this.modelName = api.persistence().preferences().getString(MODEL_NAME_KEY);
    this.apiKey = api.persistence().preferences().getString(API_KEY_KEY);
    this.extraHeaders = api.persistence().preferences().getString(EXTRA_HEADERS_KEY);

    if (this.apiEndpoint == null || this.apiEndpoint.isEmpty()) {
      setApiEndpoint(DEFAULT_API_ENDPOINT);
    }
    if (this.modelName == null || this.modelName.isEmpty()) {
      setModelName(DEFAULT_MODEL_NAME);
    }
    if (this.apiKey == null) {
      this.apiKey = "";
    }
    if (this.extraHeaders == null) {
      this.extraHeaders = "";
    }
  }

  public String getApiEndpoint() {
    return apiEndpoint;
  }

  public String getModelName() {
    return modelName;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getExtraHeaders() {
    return extraHeaders;
  }

  public void setApiEndpoint(String apiEndpoint) {
    this.apiEndpoint = apiEndpoint;
    api.persistence().preferences().setString(API_ENDPOINT_KEY, apiEndpoint);
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
    api.persistence().preferences().setString(MODEL_NAME_KEY, modelName);
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
    api.persistence().preferences().setString(API_KEY_KEY, apiKey);
  }

  public void setExtraHeaders(String extraHeaders) {
    this.extraHeaders = extraHeaders;
    api.persistence().preferences().setString(EXTRA_HEADERS_KEY, extraHeaders);
  }

  public void validateSettings(String apiEndpoint, String modelName, String extraHeaders) {
    if (apiEndpoint == null || apiEndpoint.isBlank()) {
      throw new IllegalArgumentException("API endpoint is required.");
    }

    try {
      URL url = new URL(apiEndpoint);
      String protocol = url.getProtocol();
      if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
        throw new IllegalArgumentException("API endpoint must use http or https.");
      }
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("API endpoint is not a valid URL.");
    }

    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("Model name is required.");
    }

    parseExtraHeaders(extraHeaders);
  }

  public Map<String, String> parseExtraHeaders(String extraHeaders) {
    Map<String, String> headers = new LinkedHashMap<>();
    if (extraHeaders == null || extraHeaders.isBlank()) {
      return headers;
    }

    String[] lines = extraHeaders.split("\\r?\\n");
    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }

      int separatorIndex = trimmed.indexOf(':');
      if (separatorIndex <= 0 || separatorIndex == trimmed.length() - 1) {
        throw new IllegalArgumentException("Extra headers must use the format 'Header-Name: value'.");
      }

      String name = trimmed.substring(0, separatorIndex).trim();
      String value = trimmed.substring(separatorIndex + 1).trim();
      if (name.isEmpty() || value.isEmpty()) {
        throw new IllegalArgumentException("Extra headers must use the format 'Header-Name: value'.");
      }

      headers.put(name, value);
    }

    return headers;
  }
}
