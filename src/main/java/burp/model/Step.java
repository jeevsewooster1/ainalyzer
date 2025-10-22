package burp.model;

// Add these imports
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.google.gson.JsonObject;

public class Step {
  private String name;
  private String thoughtProcess;
  private String modelReasoningProcess;

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

  public JsonObject toStepGenerationJson() {
    JsonObject stepJson = new JsonObject();
    stepJson.addProperty("name", this.name);
    stepJson.addProperty("thought_process", this.thoughtProcess);
    if (this.request != null) {
      stepJson.addProperty("request", this.request.toString());
    } else {
      stepJson.addProperty("request", "No request was generated for this step.");
    }
    return stepJson;
  }

  public void setRequest(String request) {
    try {
      this.request = HttpRequest.httpRequest(request);
    } catch (Exception e) {
      this.request = HttpRequest.httpRequest("GET /ai-parse-error HTTP/1.1\r\nHost: error.com\r\n\r\n" + request);
    }
  }
}
