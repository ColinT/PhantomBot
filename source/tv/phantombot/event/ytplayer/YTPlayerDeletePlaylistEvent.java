package tv.phantombot.event.ytplayer;

public class YTPlayerDeletePlaylistEvent extends YTPlayerEvent {
  private final String playlistName;

  public YTPlayerDeletePlaylistEvent(String playlistName) {
    this.playlistName = playlistName;
  }

  public String getPlaylistName() {
    return this.playlistName;
  }
}
