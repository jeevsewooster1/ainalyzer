package burp.service;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.RequestThread;
import burp.model.Step;
import burp.model.Task;
import burp.model.ThreadMessage;

import java.util.List;

public interface StateManagerView {

  void setRequestThreads(List<RequestThread> requestThreads);

  void selectRequestThread(RequestThread requestThread);

  void setTasks(List<Task> tasks);

  void selectTask(Task task);

  void clearSteps();

  void setSteps(List<Step> steps);

  void addStep(Step step);

  void selectStep(Step step);

  void setNextButtonEnabled(boolean enabled);

  void setThoughtProcess(String thought);

  void setRequest(HttpRequest request);

  void setResponse(HttpResponse response);

  void clearExecution();

  void setConversation(List<ThreadMessage> conversation);

  void setConversationEnabled(boolean enabled);
}
