package burp.service;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.model.*;
import burp.ui.*;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StateManager {

  private MontoyaApi api;
  private AiService aiService;
  private RequestExecutor requestExecutor;
  private ExecutionState state;

  private TasksPanel tasksPanel;
  private StepsPanel stepsPanel;
  private ExecutionPanel executionPanel;

  public StateManager(MontoyaApi api, AiService aiService, RequestExecutor requestExecutor) {
    this.api = api;
    this.aiService = aiService;
    this.requestExecutor = requestExecutor;
    this.state = new ExecutionState();
  }

  public void setUiComponents(TasksPanel tasksPanel, StepsPanel stepsPanel, ExecutionPanel executionPanel) {
    this.tasksPanel = tasksPanel;
    this.stepsPanel = stepsPanel;
    this.executionPanel = executionPanel;
  }

  public void initializeNewEndpoint(HttpRequestResponse requestResponse) {
    state.setBaseRequestResponse(requestResponse);
    state.setCurrentTask(null);
    state.setCurrentStep(null);
    state.setCurrentState(ExecutionState.State.GENERATING_TASKS);

    executionPanel.clear();
    stepsPanel.clearSteps();
    stepsPanel.setNextButtonEnabled(false);

    executionPanel.setThoughtProcess("Analyzing endpoint and generating tasks...");

    CompletableFuture.runAsync(() -> {
      try {
        List<Task> tasks = aiService.generateTasks(requestResponse);

        SwingUtilities.invokeLater(() -> {
          tasksPanel.setTasks(tasks);
          executionPanel.setThoughtProcess("Tasks generated successfully. Select a task to begin.");
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      } catch (Exception e) {
        api.logging().logToError("Error generating tasks: " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
          executionPanel.setThoughtProcess("Error generating tasks: " + e.getMessage());
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      }
    });
  }

  public void selectTask(Task task) {
    state.setCurrentTask(task);
    state.setCurrentStep(null);
    executionPanel.clear();

    List<Step> existingSteps = task.getSteps();
    stepsPanel.setSteps(existingSteps);

    if (existingSteps != null && !existingSteps.isEmpty()) {
      Step lastStep = existingSteps.get(existingSteps.size() - 1);
      state.setCurrentStep(lastStep);
      stepsPanel.selectStep(lastStep);
      displayStepDetails(lastStep);
      executionPanel.setThoughtProcess("Resumed task: " + task.getName() + ". Showing last step.");
    } else {
      executionPanel.setThoughtProcess("Task selected: " + task.getName() + ". Click 'Next' to begin.");
    }

    stepsPanel.setNextButtonEnabled(true);
  }

  public void displayStepDetails(Step step) {
    if (step == null) {
      state.setCurrentStep(null);
      executionPanel.clear();
      return;
    }

    state.setCurrentStep(step);

    String thought = step.getThoughtProcess() != null ? step.getThoughtProcess() : "";
    String summary = step.getSummary() != null ? step.getSummary() : "No summary available.";

    executionPanel.setThoughtProcess(thought + "\n\n--- Summary ---\n" + summary);
    executionPanel.setRequest(step.getRequest());
    executionPanel.setResponse(step.getResponse());
  }

  public void executeNextStep() {
    if (state.getCurrentTask() == null) {
      return;
    }

    state.setCurrentState(ExecutionState.State.EXECUTING_STEP);
    stepsPanel.setNextButtonEnabled(false);

    executionPanel.setThoughtProcess("AI is analyzing and creating next step...");

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
          stepsPanel.addStep(newStep);
        });

        HttpService httpService = state.getBaseRequestResponse().httpService();
        HttpRequest aiRequest = newStep.getRequest();

        if (aiRequest == null) {
          String errorSummary = "Error: AI failed to generate a valid HTTP request for this step.";
          newStep.setSummary(errorSummary);

          SwingUtilities.invokeLater(() -> {
            executionPanel.setThoughtProcess(newStep.getThoughtProcess() +
                "\n\nSummary: " + errorSummary);
            executionPanel.setRequest(null); // No request to show
            executionPanel.setResponse(null); // No response
            stepsPanel.setNextButtonEnabled(true);
            state.setCurrentState(ExecutionState.State.IDLE);
          });
          return;
        }

        HttpRequest connectedRequest = aiRequest.withService(httpService);
        newStep.setRequest(connectedRequest);

        SwingUtilities.invokeLater(() -> {
          executionPanel.setThoughtProcess(newStep.getThoughtProcess());
          executionPanel.setRequest(newStep.getRequest());
          executionPanel.setResponse(null);
        });

        final HttpResponse httpResponse = requestExecutor.executeRequest(newStep.getRequest());
        newStep.setResponse(httpResponse);

        SwingUtilities.invokeLater(() -> {
          executionPanel.setResponse(httpResponse);
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

          executionPanel.setThoughtProcess(newStep.getThoughtProcess() +
              "\n\nSummary: " + summary);
          stepsPanel.setNextButtonEnabled(true);
          state.setCurrentState(ExecutionState.State.IDLE);
        });

      } catch (Exception e) {
        api.logging().logToError("Error executing step: " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
          executionPanel.setThoughtProcess("Error: " + e.getMessage());
          stepsPanel.setNextButtonEnabled(true);
          state.setCurrentState(ExecutionState.State.IDLE);
        });
      }
    });
  }
}
