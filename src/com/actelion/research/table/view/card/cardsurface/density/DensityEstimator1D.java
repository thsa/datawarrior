package com.actelion.research.table.view.card.cardsurface.density;

import java.util.List;

public interface DensityEstimator1D {

    public void setData(double[] data);
    public void setBandwidth(double bw);
    public double getBandwidth();

    public List<Double> getSamples();

    public void setInterval(double a, double b);
    public double[] getInterval();

    public void setNormalization(double normalization);

    public double[] estimate(double x[]);

}
