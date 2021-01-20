package com.actelion.research.table.category;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.table.CompoundTableChemistryCellRenderer;

/**
 * Keeps idcode with id-coordinates, while providing comparison on the idcode alone
 */
public class CategoryMolecule implements Comparable<CategoryMolecule> {
	private String idcode;
	private byte[] coords;

	public CategoryMolecule(String idcode, byte[] coords) {
		this.idcode = (idcode == null) ? "" : idcode;
		this.coords = coords;
	}

	@Override
	public int compareTo(CategoryMolecule cm) {
		return idcode.compareTo(cm.idcode);
	}

	public String getIDCode() {
		return idcode;
	}

	public StereoMolecule getMolecule() {
		if (idcode.length() == 0)
			return null;
		byte[] bytes = idcode.getBytes();
		if (new IDCodeParser().getAtomCount(bytes, 0) > CompoundTableChemistryCellRenderer.ON_THE_FLY_COORD_MAX_ATOMS)
			return null;
		return new IDCodeParser(true).getCompactMolecule(idcode.getBytes(), coords);
	}
}
