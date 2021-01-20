package com.actelion.research.table.view.card.animators;


// Is this functionality somewhere in java2d?

import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates equidistant samples at distance "equidistance" from Path2D
 */
public class SampledPath{

    public Path2D mPath;

    List<Point2D> mSampledPoints;


    public SampledPath(Path2D path, double equidistance, int n_samples ) {
        this(path.getPathIterator(null),equidistance,n_samples);
    }

    /**
     * Decision whether equidistance or n_samples is used based on which one is not negative.
     *
     * @param path
     * @param equidistance
     * @param n_samples
     */
    public SampledPath(PathIterator path, double equidistance, int n_samples ) {

        //PathIterator pi = path.getPathIterator(null);
        PathIterator   pi = path;
        FlatteningPathIterator iter = new FlatteningPathIterator(pi, 0.01, 8);


        this.mSampledPoints = new ArrayList<>();
        List<Point2D> mSampledPointsPre = new ArrayList<>();

        double coords[] = new double[2];

        double totalLength = 0.0;

        Point2D p_last = null;
        while (!iter.isDone()) {
            iter.currentSegment(coords);
            int x = (int) coords[0];
            int y = (int) coords[1];
            Point2D p_next = new Point2D.Double(x, y);

            mSampledPointsPre.add(p_next);
            iter.next();
            if(p_last!=null){
                totalLength += p_last.distance(p_next) ;
            }
            p_last = p_next;
        }

        // Use n_samples definition if possible..
        if(n_samples > 0){
            equidistance = totalLength / n_samples;
        }

        // Briefly check if we actually dont move. Then we will end up in an endless loop if we don't break here..
        if(totalLength==0.0){
            mSampledPoints.addAll(mSampledPointsPre);
        }
        else
        {

            // sample equidistant:
            this.mSampledPoints = new ArrayList<>();

            double remainder_from_last = 0.0; // remainder from last segment
            p_last = null;

            // current sampling position
            Point2D position = null;

            for (Point2D p : mSampledPointsPre) {
                if (p_last != null) {
                    remainder_from_last += p.distance(p_last);
                    double v_normed_x = (p.getX() - p_last.getX()) / remainder_from_last;
                    double v_normed_y = (p.getY() - p_last.getY()) / remainder_from_last;

                    while (remainder_from_last >= equidistance) {
                        position = new Point2D.Double(position.getX() + v_normed_x * equidistance, position.getY() + v_normed_y * equidistance);
                        mSampledPoints.add(position);
                        remainder_from_last -= equidistance;
                        //System.out.println("x="+position.getX()+"y="+position.getY());
                    }

                } else {
                    mSampledPoints.add(p);
                    position = p;
                }

                p_last = p;
            }
            //System.out.println("Sampled points: "+this.getSampledPoints().size());
            // add endpoint extra..
            mSampledPoints.remove(mSampledPoints.size() - 1);
            mSampledPoints.add(p_last);
        }
    }

    public List<Point2D> getSampledPoints(){
        return this.mSampledPoints;
    }

    public Point2D at(double t){
        int pos = (int) ( t * (double) this.getSampledPoints().size() );
        pos = Math.min(mSampledPoints.size()-1,pos);
        pos = Math.max(0,pos);
        return mSampledPoints.get(pos);
    }



}