package tv.phantombot.event.ytplayer;

public class YTPlayerPromoteRequestEvent extends YTPlayerEvent {
  private final int songRequestIndex;

  public YTPlayerPromoteRequestEvent(int songRequestIndex) {
    this.songRequestIndex = songRequestIndex;
  }

  public int getSongRequestIndex() {
    return this.songRequestIndex;
  }
}
