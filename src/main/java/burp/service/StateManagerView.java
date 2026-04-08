package burp.service;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.Step;
import burp.model.Task;

import java.util.List;

public interface StateManagerView {

  void setTasks(List<Task> tasks);

  void clearSteps();

  void setSteps(List<Step> steps);

  void addStep(Step step);

  void selectStep(Step step);

  void setNextButtonEnabled(boolean enabled);

  void setThoughtProcess(String thought);

  void setRequest(HttpRequest request);

  void setResponse(HttpResponse response);

  void clearExecution();
}
