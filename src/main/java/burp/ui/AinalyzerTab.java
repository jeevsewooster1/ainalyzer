package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
// REMOVED: import burp.api.montoya.ui.SuiteTab;
import burp.model.RequestThread;
import burp.service.AiService;
import burp.service.SettingsService; // Import the SettingsService
import burp.service.StateManager;
import burp.service.StateManagerView;
import burp.service.RequestExecutor;
import burp.model.Step;
import burp.model.Task;
import burp.model.ThreadMessage;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AinalyzerTab extends JPanel implements StateManagerView {

  private final MontoyaApi api;
  private final StateManager stateManager;
  private final AiService aiService;
  private final SettingsService settingsService;
  private final RequestExecutor requestExecutor;

  private final RequestThreadsPanel requestThreadsPanel;
  private final TasksPanel tasksPanel;
  private final StepsPanel stepsPanel;
  private final ExecutionPanel executionPanel;

  private JTextField apiEndpointField;
  private JComboBox<String> modelField;
  private JPasswordField apiKeyField;
  private JTextArea extraHeadersArea;
  private JComboBox<SettingsService.ProviderType> providerTypeComboBox;
  private JTextArea providerHelpLabel;

  public AinalyzerTab(MontoyaApi api, SettingsService settingsService, AiService aiService) {
    this.api = api;
    this.settingsService = settingsService;
    this.aiService = aiService;

    this.requestExecutor = new RequestExecutor(api);
    this.stateManager = new StateManager(api, this.aiService, requestExecutor);

    setLayout(new BorderLayout());

    requestThreadsPanel = new RequestThreadsPanel(api, stateManager);
    tasksPanel = new TasksPanel(api, stateManager);
    stepsPanel = new StepsPanel(api, stateManager);
    executionPanel = new ExecutionPanel(api);
    executionPanel.setConversationSendListener(stateManager::sendCurrentThreadMessage);

    stateManager.setView(this);

    JPanel configTitledPanel = createPanelWithTitle("Configuration", new JScrollPane(createConfigPanel()));
    JPanel requestsTitledPanel = createPanelWithTitle("Requests", requestThreadsPanel);
    JPanel tasksTitledPanel = createPanelWithTitle("Tasks", tasksPanel);
    JPanel stepsTitledPanel = createPanelWithTitle("Steps", stepsPanel);

    configTitledPanel.setMinimumSize(new Dimension(130, 170));
    requestsTitledPanel.setMinimumSize(new Dimension(130, 180));
    tasksTitledPanel.setMinimumSize(new Dimension(130, 180));
    configTitledPanel.setPreferredSize(new Dimension(160, 200));
    requestsTitledPanel.setPreferredSize(new Dimension(160, 220));
    tasksTitledPanel.setPreferredSize(new Dimension(160, 260));

    JSplitPane leftBottomSplitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        requestsTitledPanel,
        tasksTitledPanel);
    leftBottomSplitPane.setResizeWeight(0.45);
    leftBottomSplitPane.setDividerSize(8);
    leftBottomSplitPane.setOneTouchExpandable(true);
    leftBottomSplitPane.setBorder(null);

    JSplitPane leftSplitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        configTitledPanel,
        leftBottomSplitPane);
    leftSplitPane.setResizeWeight(0.28);
    leftSplitPane.setDividerSize(8);
    leftSplitPane.setOneTouchExpandable(true);
    leftSplitPane.setBorder(null);

    JSplitPane bottomRightSplitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        stepsTitledPanel,
        executionPanel);
    bottomRightSplitPane.setResizeWeight(0.26);
    bottomRightSplitPane.setDividerSize(8);
    bottomRightSplitPane.setOneTouchExpandable(true);
    bottomRightSplitPane.setBorder(null);

    JSplitPane mainSplitPane = new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        leftSplitPane,
        bottomRightSplitPane);
    mainSplitPane.setResizeWeight(0.12);
    mainSplitPane.setDividerSize(8);
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    mainSplitPane.setMinimumSize(new Dimension(900, 600));

    add(mainSplitPane, BorderLayout.CENTER);

    SwingUtilities.invokeLater(() -> {
      mainSplitPane.setDividerLocation(150);
      leftSplitPane.setDividerLocation(185);
      leftBottomSplitPane.setDividerLocation(230);
      bottomRightSplitPane.setDividerLocation(250);
    });
  }

  private JComponent createConfigPanel() {
    JPanel configPanel = new JPanel();
    configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
    configPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

    JLabel apiLabel = new JLabel("API Endpoint:");
    apiEndpointField = new JTextField(settingsService.getApiEndpoint(), 14);

    JLabel modelLabel = new JLabel("Model:");
    modelField = new JComboBox<>();
    modelField.setEditable(true);

    JLabel providerLabel = new JLabel("Provider:");
    providerTypeComboBox = new JComboBox<>(SettingsService.ProviderType.values());
    providerTypeComboBox.setSelectedItem(settingsService.getProviderType());

    JLabel apiKeyLabel = new JLabel("API Key:");
    apiKeyField = new JPasswordField(settingsService.getApiKey(), 14);

    JLabel extraHeadersLabel = new JLabel("Extra Headers:");
    extraHeadersArea = new JTextArea(settingsService.getExtraHeaders(), 4, 14);
    extraHeadersArea.setLineWrap(true);
    extraHeadersArea.setWrapStyleWord(true);

    providerHelpLabel = new JTextArea();
    providerHelpLabel.setColumns(14);
    providerHelpLabel.setRows(3);
    providerHelpLabel.setLineWrap(true);
    providerHelpLabel.setWrapStyleWord(true);
    providerHelpLabel.setEditable(false);
    providerHelpLabel.setOpaque(false);
    providerHelpLabel.setFocusable(false);
    providerHelpLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
    providerHelpLabel.setFont(providerHelpLabel.getFont().deriveFont(11f));
    updateProviderFields((SettingsService.ProviderType) providerTypeComboBox.getSelectedItem(), false);

    providerTypeComboBox.addActionListener(e -> updateProviderFields(
        (SettingsService.ProviderType) providerTypeComboBox.getSelectedItem(),
        true));

    JButton saveButton = new JButton("Save");

    saveButton.addActionListener(e -> {
      SettingsService.ProviderType providerType = (SettingsService.ProviderType) providerTypeComboBox.getSelectedItem();
      String newEndpoint = apiEndpointField.getText().trim();
      String newModel = selectedModel().trim();
      String newApiKey = new String(apiKeyField.getPassword()).trim();
      String newExtraHeaders = extraHeadersArea.getText().trim();

      try {
        settingsService.validateSettings(providerType, newEndpoint, newModel, newApiKey, newExtraHeaders);
      } catch (IllegalArgumentException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid configuration", JOptionPane.ERROR_MESSAGE);
        return;
      }

      settingsService.setProviderType(providerType);
      settingsService.setApiEndpoint(newEndpoint);
      settingsService.setModelName(newModel);
      settingsService.setApiKey(newApiKey);
      settingsService.setExtraHeaders(newExtraHeaders);

      String message = "Settings saved successfully!";
      api.logging().logToOutput(message);
      JOptionPane.showMessageDialog(this, message);
    });

    configPanel.add(createFormGroup(providerLabel, providerTypeComboBox));
    configPanel.add(providerHelpLabel);
    configPanel.add(Box.createVerticalStrut(8));
    configPanel.add(createFormGroup(apiLabel, apiEndpointField));
    configPanel.add(Box.createVerticalStrut(6));
    configPanel.add(createFormGroup(modelLabel, modelField));
    configPanel.add(Box.createVerticalStrut(6));
    configPanel.add(createFormGroup(apiKeyLabel, apiKeyField));
    configPanel.add(Box.createVerticalStrut(6));
    JScrollPane extraHeadersScrollPane = new JScrollPane(extraHeadersArea);
    extraHeadersScrollPane.setPreferredSize(new Dimension(120, 90));
    configPanel.add(createFormGroup(extraHeadersLabel, extraHeadersScrollPane));
    configPanel.add(Box.createVerticalStrut(8));

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    buttonPanel.add(saveButton);
    configPanel.add(buttonPanel);
    configPanel.add(Box.createVerticalGlue());

    return configPanel;
  }

  private void updateProviderFields(SettingsService.ProviderType providerType, boolean applyDefaults) {
    if (providerType == null) {
      return;
    }

    if (applyDefaults) {
      apiEndpointField.setText(settingsService.defaultApiEndpoint(providerType));
      populateModelSuggestions(providerType, settingsService.defaultModelName(providerType));
      if (providerType == SettingsService.ProviderType.LOCAL_OPENAI_COMPATIBLE
          || providerType == SettingsService.ProviderType.AGENTAPI) {
        apiKeyField.setText("");
      }
    } else {
      populateModelSuggestions(providerType, settingsService.getModelName());
    }

    boolean openAi = providerType == SettingsService.ProviderType.OPENAI;
    boolean agentApi = providerType == SettingsService.ProviderType.AGENTAPI;
    providerHelpLabel.setText(openAi
        ? "OpenAI uses bearer-token auth. Suggested models include gpt-5.2, gpt-5.2-chat-latest, and gpt-5-mini."
        : agentApi
            ? "Use this with an external AgentAPI server, usually agentapi server --type=codex -- codex."
            : "Use this for Ollama, LM Studio, and other OpenAI-compatible local endpoints.");
    providerHelpLabel.setCaretPosition(0);
    modelField.setEnabled(!agentApi);
    apiKeyField.setEnabled(openAi);
  }

  private void populateModelSuggestions(SettingsService.ProviderType providerType, String selectedModel) {
    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    for (String option : settingsService.suggestedModels(providerType)) {
      model.addElement(option);
    }
    modelField.setModel(model);
    modelField.setSelectedItem(selectedModel);
  }

  private String selectedModel() {
    Object selectedItem = modelField.getEditor().getItem();
    return selectedItem != null ? selectedItem.toString() : "";
  }

  private JPanel createPanelWithTitle(String title, JComponent content) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(title));
    panel.add(content, BorderLayout.CENTER);
    return panel;
  }

  private JPanel createFormGroup(JLabel label, JComponent input) {
    JPanel group = new JPanel(new BorderLayout(0, 4));
    group.setAlignmentX(Component.LEFT_ALIGNMENT);
    group.add(label, BorderLayout.NORTH);
    group.add(input, BorderLayout.CENTER);
    return group;
  }

  public void addNewEndpoint(HttpRequestResponse requestResponse) {
    stateManager.initializeNewEndpoint(requestResponse);
  }

  @Override
  public void setRequestThreads(List<RequestThread> requestThreads) {
    requestThreadsPanel.setRequestThreads(requestThreads);
  }

  @Override
  public void selectRequestThread(RequestThread requestThread) {
    requestThreadsPanel.selectRequestThread(requestThread);
  }

  @Override
  public void setTasks(List<Task> tasks) {
    tasksPanel.setTasks(tasks);
  }

  @Override
  public void selectTask(Task task) {
    tasksPanel.selectTask(task);
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

  @Override
  public void setConversation(List<ThreadMessage> conversation) {
    executionPanel.setConversation(conversation);
  }

  @Override
  public void setConversationEnabled(boolean enabled) {
    executionPanel.setConversationEnabled(enabled);
  }

}
