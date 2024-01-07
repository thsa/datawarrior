/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* this is stub code written based on Apple EAWT package javadoc published at
 * http://developer.apple.com.  It makes compiling code which uses Apple EAWT
 * on non-Mac platforms possible.  The compiled stub classes should never be
 * included in the final product.
 */

package com.apple.eawt;

public abstract class AppEvent extends java.util.EventObject {

  AppEvent() { super(null); }

  public static class SystemSleepEvent extends AppEvent {

    SystemSleepEvent() {}
  }

  /**
   * @since Mac OS X 10.7 Update 1
   */
  public static class FullScreenEvent extends AppEvent {
    FullScreenEvent(java.awt.Window window) { }

    public java.awt.Window getWindow() { return null; }
  }

  public static class ScreenSleepEvent extends AppEvent {

    ScreenSleepEvent() {}
  }

  public static class UserSessionEvent extends AppEvent {

    UserSessionEvent() {}
  }

  public static class AppHiddenEvent extends AppEvent {

    AppHiddenEvent() {}
  }

  public static class AppForegroundEvent extends AppEvent {

    AppForegroundEvent() {}
  }

  public static class AppReOpenedEvent extends AppEvent {

    AppReOpenedEvent() {}
  }

  public static class QuitEvent extends AppEvent {

    QuitEvent() {}
  }

  public static class PreferencesEvent extends AppEvent {

    PreferencesEvent() {}
  }

  public static class AboutEvent extends AppEvent {

    AboutEvent() {}
  }

  public static class OpenURIEvent extends AppEvent {
    OpenURIEvent(java.net.URI uri) {}

    public java.net.URI getURI() { return null; }
  }

  public static class PrintFilesEvent extends AppEvent.FilesEvent {

    PrintFilesEvent(java.util.List<java.io.File> files) { super(files); }
  }

  public static class OpenFilesEvent extends AppEvent.FilesEvent {
    OpenFilesEvent(java.util.List<java.io.File> files, java.lang.String s) { super(files); }

    public java.lang.String getSearchTerm() { return null; }
  }

  public static abstract class FilesEvent extends AppEvent {
    FilesEvent(java.util.List<java.io.File> files) {}

    public java.util.List<java.io.File> getFiles() { return null; }
  }
}