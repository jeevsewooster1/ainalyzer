package burp.model;

import burp.api.montoya.http.message.HttpRequestResponse;

import java.util.ArrayList;
import java.util.List;

public class RequestThread {
  private final HttpRequestResponse requestResponse;
  private final List<Task> tasks;
  private final List<ThreadMessage> conversation;

  private Task selectedTask;
  private String statusMessage;
  private boolean generatingTasks;

  public RequestThread(HttpRequestResponse requestResponse) {
    this.requestResponse = requestResponse;
    this.tasks = new ArrayList<>();
    this.conversation = new ArrayList<>();
    this.statusMessage = "Analyzing endpoint and generating tasks...";
    this.generatingTasks = false;
  }

  public HttpRequestResponse getRequestResponse() {
    return requestResponse;
  }

  public List<Task> getTasks() {
    return tasks;
  }

  public void setTasks(List<Task> tasks) {
    this.tasks.clear();
    if (tasks != null) {
      this.tasks.addAll(tasks);
    }
    if (selectedTask != null && !this.tasks.contains(selectedTask)) {
      selectedTask = null;
    }
  }

  public Task getSelectedTask() {
    return selectedTask;
  }

  public void setSelectedTask(Task selectedTask) {
    this.selectedTask = selectedTask;
  }

  public List<ThreadMessage> getConversation() {
    return conversation;
  }

  public void addConversationMessage(ThreadMessage.Role role, String content) {
    if (content == null || content.isBlank()) {
      return;
    }
    conversation.add(new ThreadMessage(role, content.trim()));
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public boolean isGeneratingTasks() {
    return generatingTasks;
  }

  public void setGeneratingTasks(boolean generatingTasks) {
    this.generatingTasks = generatingTasks;
  }

  public int getTotalStepCount() {
    return tasks.stream().mapToInt(Task::getStepCount).sum();
  }

  public String getMethod() {
    return requestResponse.request().method();
  }

  public String getHost() {
    return requestResponse.httpService().host();
  }

  public String getPath() {
    return requestResponse.request().path();
  }

  public short getStatusCode() {
    return requestResponse.hasResponse() ? requestResponse.statusCode() : 0;
  }

  public String getDisplayTitle() {
    return getMethod() + " " + getHost() + getPath();
  }

  public String getDisplaySubtitle() {
    StringBuilder subtitle = new StringBuilder();
    if (requestResponse.hasResponse()) {
      subtitle.append("HTTP ").append(getStatusCode()).append(" | ");
    }
    subtitle.append(tasks.size()).append(" task");
    if (tasks.size() != 1) {
      subtitle.append("s");
    }
    subtitle.append(" | ").append(getTotalStepCount()).append(" step");
    if (getTotalStepCount() != 1) {
      subtitle.append("s");
    }
    if (generatingTasks) {
      subtitle.append(" | generating");
    }
    return subtitle.toString();
  }

  @Override
  public String toString() {
    return getDisplayTitle();
  }
}
