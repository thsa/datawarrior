package com.actelion.research.table.view;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.hidpi.HiDPIHelper;

import java.awt.*;

public class CellDecorationPainter {
	public static void paintFlagTriangle(Graphics g, int flags, Rectangle cellRect) {
		if (flags != 0) {
			int mask = 1;
			for (int i=1; i<CompoundTableConstants.cFlagColor.length; i++)
				mask = (mask << 1) | 1;
			Color[] color = new Color[Integer.bitCount(flags & mask)];
			mask = 1;
			int index = 0;
			for (int i = 0; i<CompoundTableConstants.cFlagColor.length; i++) {
				if ((flags & mask) != 0)
					color[index++] = CompoundTableConstants.cFlagColor[i];
				mask = (mask << 1);
				}
			int[] x = new int[3];
			int[] y = new int[3];
			x[0] = cellRect.x + cellRect.width - 1;
			y[0] = cellRect.y;
			x[1] = x[0];
			y[2] = y[0];
			int maxSize = HiDPIHelper.scale(20);
			maxSize = Math.min(maxSize, Math.min(cellRect.width, cellRect.height));
			maxSize = Math.min(maxSize, Math.max(cellRect.width, cellRect.height) / 5);
			for (int i=color.length; i>0; i--) {
				int size = (int)Math.round(maxSize * Math.pow((double)i/color.length, 0.7));
				y[1] = y[0] + size;
				x[2] = x[0] - size;
				g.setColor(color[i-1]);
				g.fillPolygon(x, y, 3);
				}
			}
		}
	}
