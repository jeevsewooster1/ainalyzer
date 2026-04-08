package burp.model;

// Add these imports
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.google.gson.JsonObject;

public class Step {
  private String name;
  private String thoughtProcess;
  private String modelReasoningProcess;
  private String rawRequest;
  private String requestParseError;

  private HttpRequest request;
  private HttpResponse response;

  private String summary;

  public Step(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getThoughtProcess() {
    return thoughtProcess;
  }

  public void setThoughtProcess(String thoughtProcess) {
    this.thoughtProcess = thoughtProcess;
  }

  public HttpRequest getRequest() {
    return request;
  }

  public void setRequest(HttpRequest request) {
    this.request = request;
  }

  public HttpResponse getResponse() {
    return response;
  }

  public void setResponse(HttpResponse response) {
    this.response = response;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getModelReasoningProcess() {
    return modelReasoningProcess;
  }

  public void setModelReasoningProcess(String modelReasoningProcess) {
    this.modelReasoningProcess = modelReasoningProcess;
  }

  public String getRequestParseError() {
    return requestParseError;
  }

  public String getRawRequest() {
    return rawRequest;
  }

  public JsonObject toStepGenerationJson() {
    JsonObject stepJson = new JsonObject();
    stepJson.addProperty("name", this.name);
    stepJson.addProperty("thought_process", this.thoughtProcess);
    if (this.rawRequest != null) {
      stepJson.addProperty("request", this.rawRequest);
    } else if (this.request != null) {
      stepJson.addProperty("request", this.request.toString());
    } else {
      stepJson.addProperty("request", "No request was generated for this step.");
    }
    return stepJson;
  }

  public boolean setRequest(String request) {
    this.rawRequest = request;
    this.requestParseError = null;
    this.request = null;

    if (request == null || request.isBlank()) {
      this.requestParseError = "Request text is empty.";
      return false;
    }

    return true;
  }

  public boolean parseRequest(HttpService httpService) {
    this.requestParseError = null;
    this.request = null;

    if (rawRequest == null || rawRequest.isBlank()) {
      this.requestParseError = "Request text is empty.";
      return false;
    }
    if (httpService == null) {
      this.requestParseError = "HTTP service is not set.";
      return false;
    }

    try {
      this.request = HttpRequest.httpRequest(httpService, rawRequest);
      return true;
    } catch (Exception e) {
      this.requestParseError = e.getMessage();
      return false;
    }
  }
}
