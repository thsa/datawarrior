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

import java.awt.*;

public class Application
{
    public static Application getApplication() { return null; }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public Application() {}

    public void addAppEventListener(AppEventListener listener) {}

    public void removeAppEventListener(AppEventListener listener) {}

    public void setAboutHandler(AboutHandler handler) {}

    public void setPreferencesHandler(PreferencesHandler handler) {}

    public void setOpenFileHandler(OpenFilesHandler handler) {}

    public void setPrintFileHandler(PrintFilesHandler handler) {}

    public void setOpenURIHandler(OpenURIHandler handler) {}

    public void setQuitHandler(QuitHandler handler) {}

    public void setQuitStrategy(QuitStrategy strategy) {}

    public void enableSuddenTermination() {}

    public void disableSuddenTermination() {}

    public void requestForeground(boolean b) {}

    public void requestUserAttention(boolean b) {}

    public void openHelpViewer() {}

    public void setDockMenu(java.awt.PopupMenu menu) {}

    public java.awt.PopupMenu getDockMenu() { return null; }

    public void setDockIconImage(java.awt.Image image) {}

    public java.awt.Image getDockIconImage() { return null; }

    public void setDockIconBadge(java.lang.String s) {}

    public void setDefaultMenuBar(javax.swing.JMenuBar bar) {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void addApplicationListener(ApplicationListener listener) {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void removeApplicationListener(ApplicationListener listener) {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void setEnabledPreferencesMenu(boolean b) {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void setEnabledAboutMenu(boolean b) {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public boolean getEnabledPreferencesMenu() { return false; }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public boolean getEnabledAboutMenu() { return false; }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public boolean isAboutMenuItemPresent() { return false; }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void addAboutMenuItem() {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void removeAboutMenuItem() {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public boolean isPreferencesMenuItemPresent() { return false; }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void addPreferencesMenuItem() {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void removePreferencesMenuItem() {}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static java.awt.Point getMouseLocationOnScreen() { return null; }

    public void requestToggleFullScreen(final Window window) {}
}

