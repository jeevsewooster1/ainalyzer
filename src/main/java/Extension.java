package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.service.AiService;
import burp.service.SettingsService;
import burp.ui.AinalyzerTab; // Your tab
// ... other imports

public class Extension implements BurpExtension {

  @Override
  public void initialize(MontoyaApi api) {
    api.extension().setName("AInalyzer");

    SettingsService settingsService = new SettingsService(api);
    AiService aiService = new AiService(api, settingsService);

    AinalyzerTab ainalyzerTab = new AinalyzerTab(api, settingsService, aiService);

    api.userInterface().registerSuiteTab("AInalyzer", ainalyzerTab);

    api.userInterface().registerContextMenuItemsProvider(
        new AinalyzerContextMenuProvider(api, ainalyzerTab));

    api.logging().logToOutput("AInalyzer extension loaded successfully!");
  }
}
