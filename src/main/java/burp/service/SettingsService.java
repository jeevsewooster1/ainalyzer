package burp.service;

import burp.api.montoya.MontoyaApi;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsService {

  public enum ProviderType {
    LOCAL_OPENAI_COMPATIBLE("Local / OpenAI-compatible"),
    OPENAI("OpenAI");

    private final String displayName;

    ProviderType(String displayName) {
      this.displayName = displayName;
    }

    public String displayName() {
      return displayName;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  private static final String PROVIDER_TYPE_KEY = "ainalyzer.provider.type";
  private static final String API_ENDPOINT_KEY = "ainalyzer.api.endpoint";
  private static final String MODEL_NAME_KEY = "ainalyzer.model.name";
  private static final String API_KEY_KEY = "ainalyzer.api.key";
  private static final String EXTRA_HEADERS_KEY = "ainalyzer.extra.headers";

  private static final String DEFAULT_LOCAL_API_ENDPOINT = "http://localhost:11434/v1/chat/completions";
  private static final String DEFAULT_OPENAI_API_ENDPOINT = "https://api.openai.com/v1/chat/completions";
  private static final String DEFAULT_LOCAL_MODEL_NAME = "llama3.1";
  private static final String DEFAULT_OPENAI_MODEL_NAME = "gpt-5.2";

  private final MontoyaApi api;

  private ProviderType providerType;
  private String apiEndpoint;
  private String modelName;
  private String apiKey;
  private String extraHeaders;

  public SettingsService(MontoyaApi api) {
    this.api = api;
    loadSettings();
  }

  private void loadSettings() {
    this.providerType = parseProviderType(api.persistence().preferences().getString(PROVIDER_TYPE_KEY));
    this.apiEndpoint = api.persistence().preferences().getString(API_ENDPOINT_KEY);
    this.modelName = api.persistence().preferences().getString(MODEL_NAME_KEY);
    this.apiKey = api.persistence().preferences().getString(API_KEY_KEY);
    this.extraHeaders = api.persistence().preferences().getString(EXTRA_HEADERS_KEY);

    if (this.apiEndpoint == null || this.apiEndpoint.isEmpty()) {
      setApiEndpoint(defaultApiEndpoint(providerType));
    }
    if (this.modelName == null || this.modelName.isEmpty()) {
      setModelName(defaultModelName(providerType));
    }
    if (this.apiKey == null) {
      this.apiKey = "";
    }
    if (this.extraHeaders == null) {
      this.extraHeaders = "";
    }
  }

  public ProviderType getProviderType() {
    return providerType;
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

  public void setProviderType(ProviderType providerType) {
    this.providerType = providerType;
    api.persistence().preferences().setString(PROVIDER_TYPE_KEY, providerType.name());
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

  public void validateSettings(ProviderType providerType, String apiEndpoint, String modelName, String apiKey,
      String extraHeaders) {
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

    if (providerType == ProviderType.OPENAI && (apiKey == null || apiKey.isBlank())) {
      throw new IllegalArgumentException("API key is required for OpenAI.");
    }

    parseExtraHeaders(extraHeaders);
  }

  public String defaultApiEndpoint(ProviderType providerType) {
    return providerType == ProviderType.OPENAI ? DEFAULT_OPENAI_API_ENDPOINT : DEFAULT_LOCAL_API_ENDPOINT;
  }

  public String defaultModelName(ProviderType providerType) {
    return providerType == ProviderType.OPENAI ? DEFAULT_OPENAI_MODEL_NAME : DEFAULT_LOCAL_MODEL_NAME;
  }

  public List<String> suggestedModels(ProviderType providerType) {
    if (providerType == ProviderType.OPENAI) {
      return Arrays.asList(
          "gpt-5.2",
          "gpt-5.2-chat-latest",
          "gpt-5-mini",
          "gpt-4.1");
    }

    return Arrays.asList(
        "llama3.1",
        "qwen2.5",
        "mistral-small",
        "deepseek-r1");
  }

  private ProviderType parseProviderType(String persistedValue) {
    if (persistedValue == null || persistedValue.isBlank()) {
      return ProviderType.LOCAL_OPENAI_COMPATIBLE;
    }

    try {
      return ProviderType.valueOf(persistedValue);
    } catch (IllegalArgumentException e) {
      return ProviderType.LOCAL_OPENAI_COMPATIBLE;
    }
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
