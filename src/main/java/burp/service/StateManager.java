package burp.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.*;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StateManager {

  private MontoyaApi api;
  private AiService aiService;
  private RequestExecutor requestExecutor;
  private ExecutionState state;
  private StateManagerView view;

  public StateManager(MontoyaApi api, AiService aiService, RequestExecutor requestExecutor) {
    this.api = api;
    this.aiService = aiService;
    this.requestExecutor = requestExecutor;
    this.state = new ExecutionState();
  }

  public void setView(StateManagerView view) {
    this.view = view;
  }

  public void initializeNewEndpoint(HttpRequestResponse requestResponse) {
    RequestThread requestThread = new RequestThread(requestResponse);
    requestThread.setGeneratingTasks(true);
    state.addRequestThread(requestThread);
    view.setRequestThreads(state.getRequestThreads());
    selectRequestThread(requestThread);
    state.setCurrentState(ExecutionState.State.GENERATING_TASKS);

    generateTasksForThread(requestThread);
  }

  public void selectRequestThread(RequestThread requestThread) {
    state.setCurrentThread(requestThread);
    state.setCurrentTask(null);
    state.setCurrentStep(null);

    view.selectRequestThread(requestThread);
    view.setConversation(requestThread != null ? requestThread.getConversation() : List.of());
    view.setConversationEnabled(requestThread != null);

    if (requestThread == null) {
      view.setTasks(List.of());
      view.clearSteps();
      view.clearExecution();
      view.setNextButtonEnabled(false);
      return;
    }

    view.setTasks(requestThread.getTasks());
    view.clearSteps();
    view.clearExecution();

    Task selectedTask = requestThread.getSelectedTask();
    if (selectedTask != null && requestThread.getTasks().contains(selectedTask)) {
      view.selectTask(selectedTask);
      selectTask(selectedTask);
      return;
    }

    view.setNextButtonEnabled(false);
    view.setThoughtProcess(requestThread.getStatusMessage());
  }

  public void selectTask(Task task) {
    RequestThread currentThread = state.getCurrentThread();
    if (currentThread == null) {
      return;
    }

    currentThread.setSelectedTask(task);
    state.setCurrentTask(task);
    state.setCurrentStep(null);

    List<Step> existingSteps = task.getSteps();
    view.setSteps(existingSteps);
    view.setNextButtonEnabled(true);

    Step selectedStep = task.getSelectedStep();
    if (selectedStep != null && existingSteps.contains(selectedStep)) {
      view.selectStep(selectedStep);
      displayStepDetails(selectedStep);
    } else if (existingSteps != null && !existingSteps.isEmpty()) {
      Step lastStep = existingSteps.get(existingSteps.size() - 1);
      task.setSelectedStep(lastStep);
      view.selectStep(lastStep);
      displayStepDetails(lastStep);
    } else {
      view.clearExecution();
      view.setThoughtProcess("Task selected: " + task.getName() + ". Click 'Next' to begin.");
      view.setRequest(null);
      view.setResponse(null);
    }
  }

  public void displayStepDetails(Step step) {
    if (step == null) {
      state.setCurrentStep(null);
      view.clearExecution();
      return;
    }

    state.setCurrentStep(step);
    if (state.getCurrentTask() != null) {
      state.getCurrentTask().setSelectedStep(step);
    }

    view.setRequest(step.getRequest());
    view.setResponse(step.getResponse());
  }

  public void executeNextStep() {
    if (state.getCurrentThread() == null || state.getCurrentTask() == null) {
      return;
    }

    state.setCurrentState(ExecutionState.State.EXECUTING_STEP);
    view.setNextButtonEnabled(false);

    view.setThoughtProcess("AI is analyzing and creating next step...");

    CompletableFuture.runAsync(() -> {
      RequestThread currentThread = state.getCurrentThread();
      Task currentTask = state.getCurrentTask();
      try {
        List<Step> previousSteps = List.copyOf(currentTask.getSteps());

        Step newStep = aiService.generateStep(
            currentThread.getRequestResponse(),
            currentTask,
            previousSteps,
            currentThread.getConversation());

        state.setCurrentStep(newStep);
        currentThread.addConversationMessage(
            ThreadMessage.Role.AI,
            "Next step: " + newStep.getName() + "\n\nReasoning:\n" + newStep.getThoughtProcess());

        SwingUtilities.invokeLater(() -> {
          currentTask.addStep(newStep);
          currentTask.setSelectedStep(newStep);
          if (state.getCurrentThread() == currentThread && state.getCurrentTask() == currentTask) {
            view.addStep(newStep);
            view.setConversation(currentThread.getConversation());
          }
        });

        HttpService httpService = currentThread.getRequestResponse().httpService();
        if (!newStep.parseRequest(httpService)) {
          String requestParseError = newStep.getRequestParseError();
          String errorSummary = "Error: AI failed to generate a valid HTTP request for this step."
              + (requestParseError != null && !requestParseError.isBlank()
                  ? " Parser error: " + requestParseError
                  : "");
          newStep.setSummary(errorSummary);

          SwingUtilities.invokeLater(() -> {
            if (state.getCurrentThread() == currentThread && state.getCurrentTask() == currentTask) {
              currentThread.addConversationMessage(ThreadMessage.Role.AI, errorSummary);
              view.setConversation(currentThread.getConversation());
              view.setThoughtProcess(errorSummary);
              view.setRequest(null);
              view.setResponse(null);
              view.setNextButtonEnabled(true);
              state.setCurrentState(ExecutionState.State.IDLE);
            }
          });
          return;
        }

        SwingUtilities.invokeLater(() -> {
          if (state.getCurrentThread() == currentThread && state.getCurrentTask() == currentTask) {
            view.setThoughtProcess("Executing step: " + newStep.getName());
            view.setRequest(newStep.getRequest());
            view.setResponse(null);
          }
        });

        final HttpResponse httpResponse = requestExecutor.executeRequest(newStep.getRequest());
        newStep.setResponse(httpResponse);

        SwingUtilities.invokeLater(() -> {
          if (state.getCurrentThread() == currentThread && state.getCurrentTask() == currentTask) {
            view.setResponse(httpResponse);
          }
        });

        String summary;
        if (httpResponse == null) {
          summary = "Error: The request failed. No response was received from the server.";
          api.logging().logToError("Request execution returned null for step: " + newStep.getName());
        } else {
          summary = aiService.generateSummary(newStep);
        }
        newStep.setSummary(summary);
        currentThread.addConversationMessage(
            ThreadMessage.Role.AI,
            "Step result for '" + newStep.getName() + "':\n" + summary);

        SwingUtilities.invokeLater(() -> {

          if (state.getCurrentThread() == currentThread && state.getCurrentTask() == currentTask) {
            view.setConversation(currentThread.getConversation());
            view.setThoughtProcess(summary);
            view.setNextButtonEnabled(true);
            state.setCurrentState(ExecutionState.State.IDLE);
          }
        });

      } catch (Exception e) {
        api.logging().logToError("Error executing step: " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
          if (state.getCurrentThread() == currentThread && state.getCurrentTask() == currentTask) {
            view.setThoughtProcess("Error: " + e.getMessage());
            view.setNextButtonEnabled(true);
            state.setCurrentState(ExecutionState.State.IDLE);
          }
        });
      }
    });
  }

  public void sendCurrentThreadMessage(String content) {
    RequestThread currentThread = state.getCurrentThread();
    if (currentThread == null || content == null || content.isBlank()) {
      return;
    }

    currentThread.addConversationMessage(ThreadMessage.Role.USER, content);
    view.setConversation(currentThread.getConversation());
  }

  private void generateTasksForThread(RequestThread requestThread) {
    requestThread.setGeneratingTasks(true);
    requestThread.setStatusMessage("Analyzing endpoint and generating tasks...");

    view.setRequestThreads(state.getRequestThreads());
    if (state.getCurrentThread() == requestThread) {
      view.clearExecution();
      view.clearSteps();
      view.setTasks(List.of());
      view.setThoughtProcess(requestThread.getStatusMessage());
      view.setNextButtonEnabled(false);
    }

    CompletableFuture.runAsync(() -> {
      try {
        List<Task> tasks = aiService.generateTasks(requestThread.getRequestResponse());
        requestThread.setTasks(tasks);
        requestThread.setGeneratingTasks(false);
        requestThread.setStatusMessage("Tasks generated successfully. Select a task to begin.");
        requestThread.addConversationMessage(
            ThreadMessage.Role.AI,
            "Generated " + tasks.size() + " tasks for this request thread. Select a task and use the chat below to steer the next steps.");

        SwingUtilities.invokeLater(() -> {
          view.setRequestThreads(state.getRequestThreads());
          if (state.getCurrentThread() == requestThread) {
            view.setConversation(requestThread.getConversation());
            view.setTasks(tasks);
            view.clearSteps();
            view.clearExecution();
            view.setThoughtProcess(requestThread.getStatusMessage());
            view.setNextButtonEnabled(false);
          }
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      } catch (Exception e) {
        requestThread.setGeneratingTasks(false);
        requestThread.setStatusMessage("Error generating tasks: " + e.getMessage());
        api.logging().logToError(requestThread.getStatusMessage());

        SwingUtilities.invokeLater(() -> {
          view.setRequestThreads(state.getRequestThreads());
          if (state.getCurrentThread() == requestThread) {
            view.setConversation(requestThread.getConversation());
            view.setThoughtProcess(requestThread.getStatusMessage());
            view.setNextButtonEnabled(false);
          }
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      }
    });
  }
}
