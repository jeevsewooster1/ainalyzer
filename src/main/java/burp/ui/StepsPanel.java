package burp.ui;

import burp.api.montoya.MontoyaApi;
import burp.model.Step;
import burp.service.StateManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StepsPanel extends JPanel {

  private MontoyaApi api;
  private StateManager stateManager;
  private DefaultListModel<Step> stepListModel;
  private JList<Step> stepList;
  private JButton nextButton;
  private List<Step> steps;

  public StepsPanel(MontoyaApi api, StateManager stateManager) {
    this.api = api;
    this.stateManager = stateManager;
    this.steps = new ArrayList<>();

    setLayout(new BorderLayout());

    stepListModel = new DefaultListModel<>();
    stepList = new JList<>(stepListModel);
    stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    stepList.setCellRenderer(new StepCellRenderer()); // Uses the updated renderer

    stepList.addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        Step selectedStep = stepList.getSelectedValue();
        if (selectedStep != null) {
          stateManager.displayStepDetails(selectedStep);
        }
      }
    });

    add(new JScrollPane(stepList), BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    nextButton = new JButton("Next");
    nextButton.setEnabled(false);
    nextButton.addActionListener(e -> stateManager.executeNextStep());
    buttonPanel.add(nextButton);
    add(buttonPanel, BorderLayout.SOUTH);
  }

  public void addStep(Step step) {
    steps.add(step);

    stepListModel.addElement(step);

    int newIndex = stepListModel.getSize() - 1;
    if (newIndex >= 0) {
      stepList.setSelectedIndex(newIndex);
      stepList.ensureIndexIsVisible(newIndex);
    }
  }

  public void clearSteps() {
    steps.clear();
    stepListModel.clear();
  }

  public void setNextButtonEnabled(boolean enabled) {
    nextButton.setEnabled(enabled);
  }

  public List<Step> getSteps() {
    return new ArrayList<>(steps);
  }

  public void setSteps(List<Step> steps) {
    stepListModel.clear();
    if (steps != null) {
      stepListModel.addAll(steps); // Or a for-loop with addElement
    }
  }

  public void selectStep(Step step) {
    stepList.setSelectedValue(step, true); // This scrolls to and highlights the step
  }

  private static class StepCellRenderer extends JPanel implements ListCellRenderer<Step> {

    private JTextArea nameArea;

    public StepCellRenderer() {
      setLayout(new BorderLayout(5, 2));
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      nameArea = new JTextArea();
      nameArea.setLineWrap(true);
      nameArea.setWrapStyleWord(true);
      nameArea.setEditable(false);
      nameArea.setFont(nameArea.getFont().deriveFont(Font.BOLD));
      add(nameArea, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends Step> list,
        Step step,
        int index,
        boolean isSelected,
        boolean cellHasFocus) {

      String stepName = step.getName();
      if (stepName == null || stepName.trim().isEmpty()) {
        stepName = "Pending...";
      }

      nameArea.setText("Step " + (index + 1) + " " + stepName);

      Insets listInsets = list.getInsets();
      Insets panelInsets = getInsets();
      int availableWidth = list.getWidth() - listInsets.left - listInsets.right - panelInsets.left - panelInsets.right;

      if (availableWidth > 0) {
        nameArea.setSize(availableWidth, Short.MAX_VALUE);
      }

      if (isSelected) {
        setBackground(new Color(184, 207, 229));
        nameArea.setForeground(Color.BLACK);
        nameArea.setBackground(new Color(184, 207, 229));
      } else {
        setBackground(list.getBackground());
        nameArea.setForeground(list.getForeground());
        nameArea.setBackground(list.getBackground());
      }

      return this;
    }
  }
}
