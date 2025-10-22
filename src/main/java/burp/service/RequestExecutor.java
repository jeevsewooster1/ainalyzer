package burp.service; // Or whatever your package is

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class RequestExecutor {

  private final MontoyaApi api;

  public RequestExecutor(MontoyaApi api) {
    this.api = api;
  }

  public HttpResponse executeRequest(HttpRequest request) {
    return api.http().sendRequest(request).response();
  }
}
