package com.actelion.research.table.category;

/**
 * Created by sandert on 11/02/16.
 */
public class MoleculeCategoryNormalizer implements CategoryNormalizer<CategoryMolecule> {

	@Override
	public CategoryMolecule normalizeIn(String s) {
		return new CategoryMolecule(s, null);
		}

	@Override
	public String normalizeOut(CategoryMolecule cm) {
		return cm.getIDCode();
		}
	}
