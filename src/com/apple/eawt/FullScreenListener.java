package com.apple.eawt;

/**
 * @author Dennis.Ushakov
 */
public interface FullScreenListener {
  public void windowEnteringFullScreen(AppEvent.FullScreenEvent event);

  public void windowEnteredFullScreen(AppEvent.FullScreenEvent event);

  public void windowExitingFullScreen(AppEvent.FullScreenEvent event);

  public void windowExitedFullScreen(AppEvent.FullScreenEvent event);
}
