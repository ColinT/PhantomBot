package tv.phantombot.event.ytplayer;

public class YTPlayerCopyPlaylistEvent extends YTPlayerEvent {
  private final String playlistName;

  public YTPlayerCopyPlaylistEvent(String playlistName) {
    this.playlistName = playlistName;
  }

  public String getPlaylistName() {
    return this.playlistName;
  }
}
