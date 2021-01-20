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

package com.actelion.research.table.category;

import com.actelion.research.util.DateAnalysis;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class DateCategoryNormalizer implements CategoryNormalizer<Float> {

    private DateFormat mDateFormat;
    private DateAnalysis mDateAnalysis;

	public DateCategoryNormalizer(DateAnalysis dateAnalysis) {
		mDateAnalysis = dateAnalysis;
		mDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
		}

	public String normalize(String s) {
		return normalizeOut(normalizeIn(s));
		}

	@Override
	public Float normalizeIn(String s) {
       	long millis = mDateAnalysis.getDateMillis(s);
        return (millis == -1) ? Float.NaN : (float)((millis+43200000)/86400000);
        }

	@Override
	public String normalizeOut(Float v) {
		return DateFormat.getDateInstance().format(new Date(86400000*(long)v.floatValue()+43200000));
		}
	}
