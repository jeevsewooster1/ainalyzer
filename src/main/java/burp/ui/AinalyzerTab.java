package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
// REMOVED: import burp.api.montoya.ui.SuiteTab;
import burp.service.AiService;
import burp.service.SettingsService; // Import the SettingsService
import burp.service.StateManager;
import burp.service.RequestExecutor;

import javax.swing.*;
import java.awt.*;

public class AinalyzerTab extends JPanel {

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

    stateManager.setUiComponents(tasksPanel, stepsPanel, executionPanel);

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
    JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

    JLabel apiLabel = new JLabel("API Endpoint:");
    apiEndpointField = new JTextField(settingsService.getApiEndpoint(), 30);

    JLabel modelLabel = new JLabel("Model:");
    modelField = new JTextField(settingsService.getModelName(), 15);

    JButton saveButton = new JButton("Save");

    saveButton.addActionListener(e -> {
      String newEndpoint = apiEndpointField.getText().trim();
      String newModel = modelField.getText().trim();

      settingsService.setApiEndpoint(newEndpoint);
      settingsService.setModelName(newModel);

      String message = "Settings saved successfully!";
      api.logging().logToOutput(message);
      JOptionPane.showMessageDialog(this, message);
    });

    configPanel.add(apiLabel);
    configPanel.add(apiEndpointField);
    configPanel.add(modelLabel);
    configPanel.add(modelField);
    configPanel.add(saveButton);

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

}
