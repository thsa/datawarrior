/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.apple.eawt.event;
import java.awt.*;
import javax.swing.*;

final class GestureHandler {
    private static final String CLIENT_PROPERTY = "com.apple.eawt.event.internalGestureHandler";

    static final int PHASE = 1;
    static final int ROTATE = 2;
    static final int MAGNIFY = 3;
    static final int SWIPE = 4;
    static final int PRESSURE = 5;

    static void addGestureListenerTo(final JComponent component, final GestureListener listener) {
    }

    static void removeGestureListenerFrom(final JComponent component, final GestureListener listener) {}

    static void handleGestureFromNative(final Window window, final int type, final double x, final double y, final double a, final double b) {}

    GestureHandler() { }

    void addListener(final GestureListener listener) {}

    void removeListener(final GestureListener listener) {}

    static class PerComponentNotifier {}

    static GestureHandler getHandlerForComponent(final Component c) {
        return null;
    }

    static PerComponentNotifier getNextNotifierForComponent(final Component c) {
        return null;
    }
}
