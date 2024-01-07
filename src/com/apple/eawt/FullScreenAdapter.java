package com.apple.eawt;

/**
 * @author Dennis.Ushakov
 */
public abstract class FullScreenAdapter implements FullScreenListener {
  public FullScreenAdapter() { /* compiled code */ }

  public void windowEnteringFullScreen(AppEvent.FullScreenEvent event) { }

  public void windowEnteredFullScreen(AppEvent.FullScreenEvent event) { }

  public void windowExitingFullScreen(AppEvent.FullScreenEvent event) { }

  public void windowExitedFullScreen(AppEvent.FullScreenEvent event) { }
}
