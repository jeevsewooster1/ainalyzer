package burp.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.*;

import javax.swing.*;
import java.util.ArrayList;
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
    state.setBaseRequestResponse(requestResponse);
    state.setCurrentTask(null);
    state.setCurrentStep(null);
    state.setCurrentState(ExecutionState.State.GENERATING_TASKS);

    view.clearExecution();
    view.clearSteps();
    view.setNextButtonEnabled(false);

    view.setThoughtProcess("Analyzing endpoint and generating tasks...");

    CompletableFuture.runAsync(() -> {
      try {
        List<Task> tasks = aiService.generateTasks(requestResponse);

        SwingUtilities.invokeLater(() -> {
          view.setTasks(tasks);
          view.setThoughtProcess("Tasks generated successfully. Select a task to begin.");
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      } catch (Exception e) {
        api.logging().logToError("Error generating tasks: " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
          view.setThoughtProcess("Error generating tasks: " + e.getMessage());
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      }
    });
  }

  public void selectTask(Task task) {
    state.setCurrentTask(task);
    state.setCurrentStep(null);
    view.clearExecution();

    List<Step> existingSteps = task.getSteps();
    view.setSteps(existingSteps);

    if (existingSteps != null && !existingSteps.isEmpty()) {
      Step lastStep = existingSteps.get(existingSteps.size() - 1);
      state.setCurrentStep(lastStep);
      view.selectStep(lastStep);
      displayStepDetails(lastStep);
      view.setThoughtProcess("Resumed task: " + task.getName() + ". Showing last step.");
    } else {
      view.setThoughtProcess("Task selected: " + task.getName() + ". Click 'Next' to begin.");
    }

    view.setNextButtonEnabled(true);
  }

  public void displayStepDetails(Step step) {
    if (step == null) {
      state.setCurrentStep(null);
      view.clearExecution();
      return;
    }

    state.setCurrentStep(step);

    String thought = step.getThoughtProcess() != null ? step.getThoughtProcess() : "";
    String summary = step.getSummary() != null ? step.getSummary() : "No summary available.";

    view.setThoughtProcess(thought + "\n\n--- Summary ---\n" + summary);
    view.setRequest(step.getRequest());
    view.setResponse(step.getResponse());
  }

  public void executeNextStep() {
    if (state.getCurrentTask() == null) {
      return;
    }

    state.setCurrentState(ExecutionState.State.EXECUTING_STEP);
    view.setNextButtonEnabled(false);

    view.setThoughtProcess("AI is analyzing and creating next step...");

    CompletableFuture.runAsync(() -> {
      try {
        List<Step> previousSteps = new ArrayList<>(state.getCurrentTask().getSteps());

        Step newStep = aiService.generateStep(
            state.getBaseRequestResponse(),
            state.getCurrentTask(),
            previousSteps);

        state.setCurrentStep(newStep);

        SwingUtilities.invokeLater(() -> {
          state.getCurrentTask().addStep(newStep);
          view.addStep(newStep);
        });

        HttpService httpService = state.getBaseRequestResponse().httpService();
        if (!newStep.parseRequest(httpService)) {
          String requestParseError = newStep.getRequestParseError();
          String errorSummary = "Error: AI failed to generate a valid HTTP request for this step."
              + (requestParseError != null && !requestParseError.isBlank()
                  ? " Parser error: " + requestParseError
                  : "");
          newStep.setSummary(errorSummary);

          SwingUtilities.invokeLater(() -> {
            view.setThoughtProcess(newStep.getThoughtProcess() +
                "\n\nSummary: " + errorSummary);
            view.setRequest(null);
            view.setResponse(null);
            view.setNextButtonEnabled(true);
            state.setCurrentState(ExecutionState.State.IDLE);
          });
          return;
        }

        SwingUtilities.invokeLater(() -> {
          view.setThoughtProcess(newStep.getThoughtProcess());
          view.setRequest(newStep.getRequest());
          view.setResponse(null);
        });

        final HttpResponse httpResponse = requestExecutor.executeRequest(newStep.getRequest());
        newStep.setResponse(httpResponse);

        SwingUtilities.invokeLater(() -> {
          view.setResponse(httpResponse);
        });

        String summary;
        if (httpResponse == null) {
          summary = "Error: The request failed. No response was received from the server.";
          api.logging().logToError("Request execution returned null for step: " + newStep.getName());
        } else {
          summary = aiService.generateSummary(newStep);
        }
        newStep.setSummary(summary);

        SwingUtilities.invokeLater(() -> {

          view.setThoughtProcess(newStep.getThoughtProcess() +
              "\n\nSummary: " + summary);
          view.setNextButtonEnabled(true);
          state.setCurrentState(ExecutionState.State.IDLE);
        });

      } catch (Exception e) {
        api.logging().logToError("Error executing step: " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
          view.setThoughtProcess("Error: " + e.getMessage());
          view.setNextButtonEnabled(true);
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      }
    });
  }
}
