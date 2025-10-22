package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.ui.AinalyzerTab;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AinalyzerContextMenuProvider implements ContextMenuItemsProvider {

  private final MontoyaApi api;
  private final AinalyzerTab ainalyzerTab;

  public AinalyzerContextMenuProvider(MontoyaApi api, AinalyzerTab ainalyzerTab) {
    this.api = api;
    this.ainalyzerTab = ainalyzerTab;
  }

  @Override
  public List<Component> provideMenuItems(ContextMenuEvent event) {
    if (event.isFromTool(ToolType.PROXY, ToolType.TARGET, ToolType.REPEATER,
        ToolType.INTRUDER, ToolType.LOGGER)) {

      HttpRequestResponse requestResponse = null;

      if (event.messageEditorRequestResponse().isPresent()) {
        requestResponse = event.messageEditorRequestResponse().get().requestResponse();
      } else if (!event.selectedRequestResponses().isEmpty()) {
        requestResponse = event.selectedRequestResponses().get(0);
      }

      if (requestResponse != null) {
        List<Component> menuItems = new ArrayList<>();

        JMenuItem sendToAinalyzer = new JMenuItem("Send to AInalyzer");

        final HttpRequestResponse finalReqResp = requestResponse;

        sendToAinalyzer.addActionListener(e -> {
          ainalyzerTab.addNewEndpoint(finalReqResp);
          api.logging().logToOutput("Request sent to AInalyzer: " +
              finalReqResp.request().url());
        });

        menuItems.add(sendToAinalyzer);
        return menuItems;
      }
    }

    return null;
  }
}
