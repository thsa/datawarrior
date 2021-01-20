package org.jmol.api;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;

import javax.vecmath.*;
import java.awt.*;
import java.util.BitSet;

public interface JmolRendererInterface {

  // these methods are implmented in Export3D and Graphics3D
  
  public abstract boolean initializeExporter(String type, Graphics3D g3d, Object output);

  public abstract boolean isAntialiased();
  
  public abstract boolean checkTranslucent(boolean isAlphaTranslucent);

  public abstract boolean haveTranslucentObjects();

  /**
   * gets g3d width
   *
   * @return width pixel count;
   */
  public abstract int getRenderWidth();

  /**
   * gets g3d height
   *
   * @return height pixel count
   */
  public abstract int getRenderHeight();

  /**
   * gets g3d slab
   *
   * @return slab
   */
  public abstract int getSlab();

  public abstract void setSlab(int slabValue);

  /**
   * gets g3d depth
   *
   * @return depth
   */
  public abstract int getDepth();

  /**
   * sets current color from colix color index
   * @param colix the color index
   * @return true or false if this is the right pass
   */
  public abstract boolean setColix(short colix);

  public abstract void renderBackground();
  
  /**
   * draws a screened circle ... every other dot is turned on
   *
   * @param colix the color index
   * @param diameter the pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   * @param doFill fill or not
   */
  public abstract void drawCircleCentered(short colix, int diameter, int x, int y, int z,
                                          boolean doFill);


  /**
   * draws a screened circle ... every other dot is turned on
   *
   * @param colixFill the color index
   * @param diameter the pixel diameter
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public abstract void fillScreenedCircleCentered(short colixFill,
                                                  int diameter, int x, int y,
                                                  int z);

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param x center x
   * @param y center y
   * @param z center z
   */
  public abstract void fillSphereCentered(int diameter, int x, int y, int z);

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param center javax.vecmath.Point3i defining the center
   */

  public abstract void fillSphereCentered(int diameter, Point3i center);

  /**
   * fills a solid sphere
   *
   * @param diameter pixel count
   * @param center a javax.vecmath.Point3f ... floats are casted to ints
   */
  public abstract void fillSphereCentered(int diameter, Point3f center);

  /**
   * draws a rectangle
   *
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param zSlab z for slab check (for set labelsFront)
   * @param rWidth pixel count
   * @param rHeight pixel count
   */
  public abstract void drawRect(int x, int y, int z, int zSlab, int rWidth,
                                int rHeight);

  /**
   * fills background rectangle for label
   *<p>
   *
   * @param x upper left x
   * @param y upper left y
   * @param z upper left z
   * @param zSlab  z value for slabbing
   * @param widthFill pixel count
   * @param heightFill pixel count
   */
  public abstract void fillRect(int x, int y, int z, int zSlab, int widthFill,
                                int heightFill);

  /**
   * draws the specified string in the current font.
   * no line wrapping -- axis, labels, measures
   *
   * @param str the String
   * @param font3d the Font3D
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   * @param zSlab z for slab calculation
   */

  public abstract void drawString(String str, Font3D font3d, int xBaseline,
                                  int yBaseline, int z, int zSlab);

  public abstract void plotPixelClippedNoSlab(int argb, int x, int y, int z);
    
  /**
   * draws the specified string in the current font.
   * no line wrapping -- echo, frank, hover, molecularOrbital, uccage
   *
   * @param str the String
   * @param font3d the Font3D
   * @param xBaseline baseline x
   * @param yBaseline baseline y
   * @param z baseline z
   */

  public abstract void drawStringNoSlab(String str, Font3D font3d,
                                        int xBaseline, int yBaseline, int z);

  public abstract void setFont(byte fid);

  public abstract Font3D getFont3DCurrent();

  public abstract void drawPixel(int x, int y, int z);

  public abstract void plotPixelClipped(Point3i a);

  public abstract void drawPoints(int count, int[] coordinates);

  public abstract void drawDashedLine(int run, int rise, Point3i pointA,
                                      Point3i pointB);

  public abstract void drawDottedLine(Point3i pointA, Point3i pointB);

  public abstract void drawLine(int x1, int y1, int z1, int x2, int y2, int z2);

  public abstract void drawLine(Point3i pointA, Point3i pointB);

  public abstract void drawLine(short colixA, short colixB, int x1, int y1,
                                int z1, int x2, int y2, int z2);

  public abstract void fillCylinder(short colixA, short colixB, byte endcaps,
                                    int diameter, int xA, int yA, int zA,
                                    int xB, int yB, int zB);

  public abstract void fillCylinder(byte endcaps, int diameter, int xA, int yA,
                                    int zA, int xB, int yB, int zB);

  public abstract void fillCylinder(byte endcaps, int diameter,
                                    Point3i screenA, Point3i screenB);

  public abstract void fillCylinderBits(byte endcaps, int diameter,
                                        Point3f screenA, Point3f screenB);

  public abstract void fillCone(byte endcap, int diameter, Point3i screenBase,
                                Point3i screenTip);

  public abstract void fillCone(byte endcap, int diameter, Point3f screenBase,
                                Point3f screenTip);

  public abstract void drawHermite(int tension, Point3i s0, Point3i s1,
                                   Point3i s2, Point3i s3);

  public abstract void drawHermite(boolean fill, boolean border, int tension,
                                   Point3i s0, Point3i s1, Point3i s2,
                                   Point3i s3, Point3i s4, Point3i s5,
                                   Point3i s6, Point3i s7, int aspectRatio);

  public abstract void fillHermite(int tension, int diameterBeg,
                                   int diameterMid, int diameterEnd,
                                   Point3i s0, Point3i s1, Point3i s2,
                                   Point3i s3);

  public abstract void drawTriangle(Point3i screenA, short colixA,
                                    Point3i screenB, short colixB,
                                    Point3i screenC, short colixC, int check);

  public abstract void drawTriangle(Point3i screenA, Point3i screenB,
                                    Point3i screenC, int check);

  public abstract void drawCylinderTriangle(int xA, int yA, int zA, int xB,
                                            int yB, int zB, int xC, int yC,
                                            int zC, int diameter);

  public abstract void drawfillTriangle(int xA, int yA, int zA, int xB, int yB,
                                        int zB, int xC, int yC, int zC);

  public abstract void fillTriangle(Point3i screenA, short colixA,
                                    short normixA, Point3i screenB,
                                    short colixB, short normixB,
                                    Point3i screenC, short colixC, short normixC);

  public abstract void fillTriangle(short normix, int xScreenA, int yScreenA,
                                    int zScreenA, int xScreenB, int yScreenB,
                                    int zScreenB, int xScreenC, int yScreenC,
                                    int zScreenC);

  public abstract void fillTriangle(Point3f screenA, Point3f screenB,
                                    Point3f screenC);

  public abstract void fillTriangle(Point3i screenA, Point3i screenB,
                                    Point3i screenC);

  public void fillTriangle(Point3i screenA, int intensityA,
                           Point3i screenB, int intensityB,
                           Point3i screenC, int intensityC);
  
  public abstract void fillTriangle(Point3i screenA, short colixA,
                                    short normixA, Point3i screenB,
                                    short colixB, short normixB,
                                    Point3i screenC, short colixC,
                                    short normixC, float factor);

  public abstract void drawQuadrilateral(short colix, Point3i screenA,
                                         Point3i screenB, Point3i screenC,
                                         Point3i screenD);

  public abstract void fillQuadrilateral(Point3f screenA, Point3f screenB,
                                         Point3f screenC, Point3f screenD);

  public abstract void fillQuadrilateral(Point3i screenA, short colixA,
                                         short normixA, Point3i screenB,
                                         short colixB, short normixB,
                                         Point3i screenC, short colixC,
                                         short normixC, Point3i screenD,
                                         short colixD, short normixD);

  public abstract void renderIsosurface(Point3f[] vertices, short colix,
                                        short[] colixes, Vector3f[] normals,
                                        int[][] indices, BitSet bsFaces, int nVertices,
                                        int faceVertexMax, short[] polygonColixes, int nPolygons);

  public abstract boolean isInDisplayRange(int x, int y);

  public abstract boolean isClippedZ(int z);

  public abstract boolean isClippedXY(int i, int screenX, int screenY);

  public abstract int getColixArgb(short colix);

  public abstract String getHexColorFromIndex(short colix);

  public abstract int calcSurfaceShade(Point3i screenA, Point3i screenB,
                                        Point3i screenC);

  public abstract byte getFontFid(String fontFace, float fontSize);

  public abstract short getNormix(Vector3f vector);
  
  public abstract short getInverseNormix(short normix);

  public abstract boolean isDirectedTowardsCamera(short normix);

  public abstract Vector3f[] getTransformedVertexVectors();

  public abstract Vector3f getNormixVector(short normix);

  public abstract Font3D getFont3DScaled(Font3D font3d, float imageFontScaling);

  public abstract byte getFontFid(float fontSize);

  public abstract void renderEllipsoid(Point3f center, Point3f[] points, int x, int y, int z, 
      int diameter, Matrix3f mToEllipsoidal, double[] coef, Matrix4f mDeriv, int selectedOctant, Point3i[] octantPoints);

  public abstract void drawImage(Image image, int x, int y, int z, int zslab, short bgcolix, int width, int height);

  public abstract void startShapeBuffer(int iShape);

  public abstract void endShapeBuffer();

  public abstract boolean canDoTriangles();

  public abstract boolean isCartesianExport();
  
  public abstract String finalizeOutput();

  public abstract short[] getBgColixes(short[] bgcolixes);

}
