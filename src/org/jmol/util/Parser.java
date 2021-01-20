/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

import java.util.BitSet;

public class Parser {

  /// general static string-parsing class ///

  // next[0] tracks the pointer within the string so these can all be static.
  // but the methods parseFloat, parseInt, parseToken, parseTrimmed, and getTokens do not require this.

  /**
   * parses a "dirty" string for floats. If there are non-float tokens, 
   * they are ignored. A bitset is used to assign values only to specific 
   * atoms in the set, not changing the values of the data array for other atoms.
   * thus, a data set can be incrementally added to in this way.
   * 
   *  @param str     the string to parse
   *  @param bs      the atom positions to assign
   *  @param data    the (sparce) array to fill
   */
  public static void parseFloatArray(String str, BitSet bs, float[] data) {
    parseFloatArray(getTokens(str), bs, data);
  }

  public static void parseFloatArray(String[] tokens, BitSet bs, float[] data) {
    int len = data.length;
    int nTokens = tokens.length;
    int n = 0;
    for (int i = 0; i < len && n < nTokens; i++) {
      if (bs != null && !bs.get(i))
        continue;
      float f;
      while (Float.isNaN(f = Parser.parseFloat(tokens[n++])) && n < nTokens) {
      }
      data[i] = f;
    }
  }

  public static float[][] parseFloatArray2d(String str) {
    str = str.replace(';', str.indexOf('\n') < 0 ? '\n' : ' ');
    str = TextFormat.trim(str, "\n \t");
    int[] lines = markLines(str, '\n');
    int nLines = lines.length;
    float[][] data = new float[nLines][];
    for (int iLine = 0, pt = 0; iLine < lines.length; pt = lines[iLine++]) {
      String[] tokens = getTokens(str.substring(pt, lines[iLine]));
      parseFloatArray(tokens, data[iLine] = new float[tokens.length]);
    }
    return data;
  }

  /**
   * 
   * @param f
   * @param bs
   * @param data
   */
  public static void setSelectedFloats(float f, BitSet bs, float[] data) {
    for (int i = 0; i < data.length; i++)
      if (bs == null || bs.get(i))
        data[i] = f;
  }
  
  public static float[] extractData(String data, int field, int nBytes,
                                    int firstLine) {
    return parseFloatArrayFromMatchAndField(data, null, 0, 0, null, field,
        nBytes, null, firstLine);
  }

  /**
   * the major lifter here.
   * 
   * @param str         string containing the data 
   * @param bs          selects specific rows of the data 
   * @param fieldMatch  a free-format field pointer, or a column pointer
   * @param fieldMatchColumnCount specifies a column count -- not free-format
   * @param matchData   an array of data to match (atom numbers)
   * @param field       a free-format field pointer, or a column pointer
   * @param fieldColumnCount specifies a column count -- not free-format
   * @param data        float array to modify or null if size unknown
   * @param firstLine   first line to parse (1 indicates all)
   * @return            data
   */
  public static float[] parseFloatArrayFromMatchAndField(
                                                         String str,
                                                         BitSet bs,
                                                         int fieldMatch,
                                                         int fieldMatchColumnCount,
                                                         int[] matchData,
                                                         int field,
                                                         int fieldColumnCount,
                                                         float[] data, int firstLine) {
    float f;
    int i = -1;
    boolean isMatch = (matchData != null);
    int[] lines = markLines(str, (str.indexOf('\n') >= 0 ? '\n' : ';'));
    int iLine = (firstLine <= 1 || firstLine >= lines.length ? 0 : firstLine - 1);
    int pt = (iLine == 0 ? 0 : lines[iLine - 1]);
    int nLines = lines.length;
    if (data == null)
      data = new float[nLines - iLine];
    int len = data.length;
    int minLen = (fieldColumnCount <= 0 ? Math.max(field, fieldMatch) : Math
        .max(field + fieldColumnCount, fieldMatch + fieldMatchColumnCount) - 1);

    for (; iLine < nLines; iLine++) {
      String line = str.substring(pt, lines[iLine]).trim();
      pt = lines[iLine];
      String[] tokens = (fieldColumnCount <= 0 ? getTokens(line) : null);
      // check for inappropriate data -- line too short or too few tokens or NaN for data
      // and parse data
      if (fieldColumnCount <= 0) {
        if (tokens.length < minLen
            || Float.isNaN(f = parseFloat(tokens[field - 1])))
          continue;
      } else {
        if (line.length() < minLen
            || Float.isNaN(f = parseFloat(line.substring(field - 1, field
                + fieldColumnCount - 1))))
          continue;
      }
      int iData;
      if (isMatch) {
        iData = parseInt(tokens == null ? line.substring(fieldMatch - 1,
            fieldMatch + fieldMatchColumnCount - 1) : tokens[fieldMatch - 1]);
        // in the fieldMatch column we have an integer pointing into matchData
        // we replace that number then with the corresponding number in matchData
        if (iData == Integer.MIN_VALUE || iData < 0 || iData >= len
            || (iData = matchData[iData]) < 0)
          continue;
        // and we set bs to indicate we are updating that value
        if (bs != null)
          bs.set(iData);
      } else {
        // no match data
        // bs here indicates the specific data elements that need filling
        while (++i < len && bs != null && !bs.get(i)) {
        }
        if (i >= len)
          return data;
        iData = i;
      }
      data[iData] = f;
      //System.out.println("data[" + iData + "] = " + data[iData]);
    }
    return data;
  }
  
  /**
   * parses a string array for floats. Returns NaN for nonfloats.
   * 
   *  @param tokens  the strings to parse
   *  @param data    the array to fill
   */
  public static void parseFloatArray(String[] tokens, float[] data) {
    parseFloatArray(tokens, data, data.length);
  }
  
  /**
   * parses a string array for floats. Returns NaN for nonfloats or missing data.
   * 
   *  @param tokens  the strings to parse
   *  @param data    the array to fill
   *  @param nData   the number of elements
   */
  public static void parseFloatArray(String[] tokens, float[] data, int nData) {
    for (int i = nData; --i >= 0;)
      data[i] = (i >= tokens.length ? Float.NaN : Parser.parseFloat(tokens[i]));
  }
 
  public static float parseFloat(String str) {
    return parseFloat(str, new int[] {0});
  }

  public static float parseFloatStrict(String str) {
    // checks trailing characters and does not allow "1E35" to be float
    int cch = str.length();
    if (cch == 0)
      return Float.NaN;
    return parseFloatChecked(str, cch, new int[] {0}, true);
  }

  public static int parseInt(String str) {
    return parseInt(str, new int[] {0});
  }

  public static String[] getTokens(String line) {
    return getTokens(line, 0);
  }

  public static String parseToken(String str) {
    return parseToken(str, new int[] {0});
  }

  public static String parseTrimmed(String str) {
    return parseTrimmed(str, 0, str.length());
  }
  
  public static String parseTrimmed(String str, int ichStart) {
    return parseTrimmed(str, ichStart, str.length());
  }
  
  public static String parseTrimmed(String str, int ichStart, int ichMax) {
    int cch = str.length();
    if (ichMax < cch)
      cch = ichMax;
    if (cch < ichStart)
      return "";
    return parseTrimmedChecked(str, ichStart, cch);
  }

  public static int[] markLines(String data, char eol) {
    int nLines = 0;
    for (int i = data.length(); --i >=0;)
      if (data.charAt(i) == eol)
        nLines++;
    int[] lines = new int[nLines + 1];
    lines[nLines--] = data.length();
    for (int i = data.length(); --i >=0;)
      if (data.charAt(i) == eol)
        lines[nLines--] = i + 1;
    return lines;
  }

  public static float parseFloat(String str, int[] next) {
    int cch = str.length();
    if (next[0] >= cch)
      return Float.NaN;
    return parseFloatChecked(str, cch, next, false);
  }

  public static float parseFloat(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] >= ichMax)
      return Float.NaN;
    return parseFloatChecked(str, ichMax, next, false);
  }

  private final static float[] decimalScale = { 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f,
      0.000001f, 0.0000001f, 0.00000001f };

  private final static float[] tensScale = { 10, 100, 1000, 10000, 100000, 1000000 };

  private static float parseFloatChecked(String str, int ichMax, int[] next, boolean isStrict) {
    boolean digitSeen = false;
    float value = 0;
    int ich = next[0];
    if (isStrict && str.indexOf('\n') != str.lastIndexOf('\n'))
        return Float.NaN;
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      ++ich;
      negative = true;
    }
    char ch = 0;
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      ++ich;
      digitSeen = true;
    }
    boolean isDecimal = false;
    if (ch == '.') {
      isDecimal = true;
      int iscale = 0;
      while (++ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
        if (iscale < decimalScale.length)
          value += (ch - '0') * decimalScale[iscale];
        ++iscale;
        digitSeen = true;
      }
    }
    boolean isExponent = false;
    if (!digitSeen)
      value = Float.NaN;
    else if (negative)
      value = -value;
    if (ich < ichMax && (ch == 'E' || ch == 'e' || ch == 'D')) {
      isExponent = true;
      if (++ich >= ichMax)
        return Float.NaN;
      ch = str.charAt(ich);
      if ((ch == '+') && (++ich >= ichMax))
        return Float.NaN;
      next[0] = ich;
      int exponent = parseIntChecked(str, ichMax, next);
      if (exponent == Integer.MIN_VALUE)
        return Float.NaN;
      if (exponent > 0)
        value *= ((exponent < tensScale.length) ? tensScale[exponent - 1]
            : Math.pow(10, exponent));
      else if (exponent < 0)
        value *= ((-exponent < decimalScale.length) ? decimalScale[-exponent - 1]
            : Math.pow(10, exponent));
    } else {
       next[0] = ich; // the exponent code finds its own ichNextParse
    }
    return (!isStrict 
        || (!isExponent || isDecimal) && checkTrailingText(str, next[0], ichMax) 
        ? value : Float.NaN);
  }

  private static boolean checkTrailingText(String str, int ich, int ichMax) {
    //number must be pure -- no additional characters other than white space or ;
    char ch;
    while (ich < ichMax && ((ch = str.charAt(ich)) == ' '
        || ch == '\t' || ch == '\n' || ch == ';'))
      ++ich;
    return (ich == ichMax);
  }
  
  public static int parseInt(String str, int[] next) {
    int cch = str.length();
    if (next[0] >= cch)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, cch, next);
  }

  public static int parseInt(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] >= ichMax)
      return Integer.MIN_VALUE;
    return parseIntChecked(str, ichMax, next);
  }

  private static int parseIntChecked(String str, int ichMax, int[] next) {
    boolean digitSeen = false;
    int value = 0;
    int ich = next[0];
    char ch;
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    boolean negative = false;
    if (ich < ichMax && str.charAt(ich) == '-') {
      negative = true;
      ++ich;
    }
    while (ich < ichMax && (ch = str.charAt(ich)) >= '0' && ch <= '9') {
      value = value * 10 + (ch - '0');
      digitSeen = true;
      ++ich;
    }
    if (!digitSeen)// || !checkTrailingText(str, ich, ichMax))
      value = Integer.MIN_VALUE;
    else if (negative)
      value = -value;
    next[0] = ich;
    return value;
  }

  public static String[] getTokens(String line, int ich) {
    if (line == null)
      return null;
    int cchLine = line.length();
    if (ich > cchLine)
      return null;
    int tokenCount = countTokens(line, ich);
    String[] tokens = new String[tokenCount];
    int[] next = new int[1];
    next[0] = ich;
    for (int i = 0; i < tokenCount; ++i)
      tokens[i] = parseTokenChecked(line, cchLine, next);
    return tokens;
  }

  public static int countTokens(String line, int ich) {
    int tokenCount = 0;
    if (line != null) {
      int ichMax = line.length();
      while (true) {
        while (ich < ichMax && isWhiteSpace(line, ich))
          ++ich;
        if (ich == ichMax)
          break;
        ++tokenCount;
        do {
          ++ich;
        } while (ich < ichMax && !isWhiteSpace(line, ich));
      }
    }
    return tokenCount;
  }

  public static String parseToken(String str, int[] next) {
    int cch = str.length();
    if (next[0] >= cch)
      return null;
    return parseTokenChecked(str, cch, next);
  }

  public static String parseToken(String str, int ichMax, int[] next) {
    int cch = str.length();
    if (ichMax > cch)
      ichMax = cch;
    if (next[0] >= ichMax)
      return null;
    return parseTokenChecked(str, ichMax, next);
  }

  private static String parseTokenChecked(String str, int ichMax, int[] next) {
    int ich = next[0];
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    int ichNonWhite = ich;
    while (ich < ichMax && !isWhiteSpace(str, ich))
      ++ich;
    next[0] = ich;
    if (ichNonWhite == ich)
      return null;
    return str.substring(ichNonWhite, ich);
  }

  private static String parseTrimmedChecked(String str, int ich, int ichMax) {
    while (ich < ichMax && isWhiteSpace(str, ich))
      ++ich;
    int ichLast = ichMax - 1;
    while (ichLast >= ich && isWhiteSpace(str, ichLast))
      --ichLast;
    if (ichLast < ich)
      return "";
    return str.substring(ich, ichLast + 1);
  }

  public static String concatTokens(String[] tokens, int iFirst, int iEnd) {
    String str = "";
    String sep = "";
    for (int i = iFirst; i < iEnd; i++) {
      if (i < tokens.length) {
        str += sep + tokens[i];
        sep = " ";
      }
    }
    return str;
  }
  
  public static String getNextQuotedString(String line, int ipt0) {
    String value = line;
    int i = value.indexOf("\"", ipt0);
    if (i < 0)
      return "";
    value = value.substring(i + 1);
    i = -1;
    while (++i < value.length() && value.charAt(i) != '"')
      if (value.charAt(i) == '\\')
        i++;
    return value.substring(0, i);
  }
  
  private static boolean isWhiteSpace(String str, int ich) {
    char ch;
    return ((ch = str.charAt(ich)) == ' ' || ch == '\t' || ch == '\n');
  }

  public static boolean isOneOf(String key, String semiList) {
    return key.indexOf(";") < 0  && (';' + semiList + ';').indexOf(';' + key + ';') >= 0;
  }

}
