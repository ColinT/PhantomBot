package tv.phantombot.event.ytplayer;

public class YTPlayerCreatePlaylistEvent extends YTPlayerEvent {
  private final String playlistName;

  public YTPlayerCreatePlaylistEvent(String playlistName) {
    this.playlistName = playlistName;
  }

  public String getPlaylistName() {
    return this.playlistName;
  }
}
