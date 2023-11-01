/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.gui.form;

import java.awt.image.BufferedImage;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

/**
 * A transcoder that generates a BufferedImage.
 */
public class BufferedImageTranscoder extends ImageTranscoder {
    /**
     * The BufferedImage generated from the SVG document.
     */
    private BufferedImage mImage;

    /**
     * Creates a new ARGB image with the specified dimension.
     * @param width
     * @param height
     */
    @Override
    public BufferedImage createImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

    /**
     * Writes the specified image to the specified output.
     * 
     * @param image
     * @param output
     * @param TranscoderException
     */

    @Override
    public void writeImage(BufferedImage image, TranscoderOutput output) throws TranscoderException {
        mImage = image;
        }

    /**
     * Returns the {@link BufferedImage} generated from the SVG document.
     * 
     * @return {@link BufferedImage} generated from the SVG document.
     */
    public BufferedImage getBufferedImage() {
        return mImage;
        }

    /**
     * Set the dimensions to be used for the image.
     * @param width.
     * @param height.
     */
    public void setDimensions(int width, int height) {
        hints.put(KEY_WIDTH, Float.valueOf(width));
        hints.put(KEY_HEIGHT, Float.valueOf(height));
        }
    }
