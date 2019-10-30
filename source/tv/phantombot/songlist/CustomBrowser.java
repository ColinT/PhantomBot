package tv.phantombot.songlist;

import java.io.IOException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;

public class CustomBrowser implements AuthorizationCodeInstalledApp.Browser {

  @Override
  public void browse(String url) throws IOException {
    com.gmt2001.Console.out.println("Open the following url:");
    com.gmt2001.Console.out.println(url);
  }

}
