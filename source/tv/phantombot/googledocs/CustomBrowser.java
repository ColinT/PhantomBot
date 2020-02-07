/**
 * The standard Browser supplied by Google does not
 * work inside the PhantomBot environment, so we have
 * to provide our own implementation.
 */

package tv.phantombot.googledocs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;

public class CustomBrowser implements AuthorizationCodeInstalledApp.Browser {

  @Override
  public void browse(String url) throws IOException {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      try {
        Desktop.getDesktop().browse(new URI(url));
      } catch (URISyntaxException uriSyntaxException) {
        com.gmt2001.Console.out.println("Failed to open browser.");
      }
    } else {
      com.gmt2001.Console.out.println("Browser is not supported in this environment.");
    }

    com.gmt2001.Console.out.println("Open the following url:");
    com.gmt2001.Console.out.println(url);
  }

}
