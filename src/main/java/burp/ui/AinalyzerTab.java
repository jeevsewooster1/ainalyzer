package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
// REMOVED: import burp.api.montoya.ui.SuiteTab;
import burp.service.AiService;
import burp.service.SettingsService; // Import the SettingsService
import burp.service.StateManager;
import burp.service.StateManagerView;
import burp.service.RequestExecutor;
import burp.model.Step;
import burp.model.Task;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AinalyzerTab extends JPanel implements StateManagerView {

  private final MontoyaApi api;
  private final StateManager stateManager;
  private final AiService aiService;
  private final SettingsService settingsService;
  private final RequestExecutor requestExecutor;

  private final TasksPanel tasksPanel;
  private final StepsPanel stepsPanel;
  private final ExecutionPanel executionPanel;

  private JTextField apiEndpointField;
  private JTextField modelField;
  private JPasswordField apiKeyField;
  private JTextArea extraHeadersArea;

  public AinalyzerTab(MontoyaApi api, SettingsService settingsService, AiService aiService) {
    this.api = api;
    this.settingsService = settingsService;
    this.aiService = aiService;

    this.requestExecutor = new RequestExecutor(api);
    this.stateManager = new StateManager(api, this.aiService, requestExecutor);

    setLayout(new BorderLayout());

    tasksPanel = new TasksPanel(api, stateManager);
    stepsPanel = new StepsPanel(api, stateManager);
    executionPanel = new ExecutionPanel(api, stateManager);

    stateManager.setView(this);

    JPanel tasksTitledPanel = createPanelWithTitle("Tasks", tasksPanel);
    JPanel stepsTitledPanel = createPanelWithTitle("Steps", stepsPanel);

    JSplitPane rightSplitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        stepsTitledPanel,
        executionPanel);
    rightSplitPane.setResizeWeight(0.5);
    rightSplitPane.setBorder(null);

    JSplitPane mainSplitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        tasksTitledPanel,
        rightSplitPane);
    mainSplitPane.setResizeWeight(0.33);
    mainSplitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    add(mainSplitPane, BorderLayout.CENTER);
    add(createConfigPanel(), BorderLayout.NORTH);
  }

  private JPanel createConfigPanel() {
    JPanel configPanel = new JPanel(new GridBagLayout());
    configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4, 4, 4, 4);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    JLabel apiLabel = new JLabel("API Endpoint:");
    apiEndpointField = new JTextField(settingsService.getApiEndpoint(), 30);

    JLabel modelLabel = new JLabel("Model:");
    modelField = new JTextField(settingsService.getModelName(), 15);

    JLabel apiKeyLabel = new JLabel("API Key:");
    apiKeyField = new JPasswordField(settingsService.getApiKey(), 20);

    JLabel extraHeadersLabel = new JLabel("Extra Headers:");
    extraHeadersArea = new JTextArea(settingsService.getExtraHeaders(), 3, 30);
    extraHeadersArea.setLineWrap(true);
    extraHeadersArea.setWrapStyleWord(true);

    JButton saveButton = new JButton("Save");

    saveButton.addActionListener(e -> {
      String newEndpoint = apiEndpointField.getText().trim();
      String newModel = modelField.getText().trim();
      String newApiKey = new String(apiKeyField.getPassword()).trim();
      String newExtraHeaders = extraHeadersArea.getText().trim();

      try {
        settingsService.validateSettings(newEndpoint, newModel, newExtraHeaders);
      } catch (IllegalArgumentException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid configuration", JOptionPane.ERROR_MESSAGE);
        return;
      }

      settingsService.setApiEndpoint(newEndpoint);
      settingsService.setModelName(newModel);
      settingsService.setApiKey(newApiKey);
      settingsService.setExtraHeaders(newExtraHeaders);

      String message = "Settings saved successfully!";
      api.logging().logToOutput(message);
      JOptionPane.showMessageDialog(this, message);
    });

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    configPanel.add(apiLabel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1;
    configPanel.add(apiEndpointField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 0;
    configPanel.add(modelLabel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1;
    configPanel.add(modelField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0;
    configPanel.add(apiKeyLabel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1;
    configPanel.add(apiKeyField, gbc);

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    configPanel.add(extraHeadersLabel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.BOTH;
    configPanel.add(new JScrollPane(extraHeadersArea), gbc);

    gbc.gridx = 1;
    gbc.gridy = 4;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.EAST;
    configPanel.add(saveButton, gbc);

    return configPanel;
  }

  private JPanel createPanelWithTitle(String title, JComponent content) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(title));
    panel.add(new JScrollPane(content), BorderLayout.CENTER);
    return panel;
  }

  public void addNewEndpoint(HttpRequestResponse requestResponse) {
    stateManager.initializeNewEndpoint(requestResponse);
  }

  @Override
  public void setTasks(List<Task> tasks) {
    tasksPanel.setTasks(tasks);
  }

  @Override
  public void clearSteps() {
    stepsPanel.clearSteps();
  }

  @Override
  public void setSteps(List<Step> steps) {
    stepsPanel.setSteps(steps);
  }

  @Override
  public void addStep(Step step) {
    stepsPanel.addStep(step);
  }

  @Override
  public void selectStep(Step step) {
    stepsPanel.selectStep(step);
  }

  @Override
  public void setNextButtonEnabled(boolean enabled) {
    stepsPanel.setNextButtonEnabled(enabled);
  }

  @Override
  public void setThoughtProcess(String thought) {
    executionPanel.setThoughtProcess(thought);
  }

  @Override
  public void setRequest(HttpRequest request) {
    executionPanel.setRequest(request);
  }

  @Override
  public void setResponse(HttpResponse response) {
    executionPanel.setResponse(response);
  }

  @Override
  public void clearExecution() {
    executionPanel.clear();
  }

}
