/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

/**
 * @author Richard Littin
 */

final class Curve {
  private Curve() {
  }

  private static final float[][] SPREAD_DIST_AS = {
    {},
    {},
    {},
    {},
    {},
    {0.00000F, 5.45377F, 5.06931F, 4.63694F, 4.17619F, 3.70629F, 3.24352F, 2.80060F, 2.38675F, 2.00802F, 1.66772F, 1.36696F},
    {0.00000F, 4.00306F, 3.65757F, 3.30358F, 2.95032F, 2.60637F, 2.27854F, 1.97183F, 1.68955F, 1.43356F, 1.20450F, 1.00209F, 0.82529F, 0.67254F},
    {0.00000F, 3.10329F, 2.81124F, 2.52444F, 2.24750F, 1.98435F, 1.73792F, 1.51015F, 1.30214F, 1.11425F, 0.94625F, 0.79748F, 0.66690F, 0.55330F, 0.45530F, 0.37144F},
    {0.00000F, 2.49880F, 2.25458F, 2.02012F, 1.79779F, 1.58943F, 1.39624F, 1.21887F, 1.05750F, 0.91192F, 0.78165F, 0.66593F, 0.56388F, 0.47451F, 0.39679F, 0.32963F, 0.27198F, 0.22280F},
    {0.00000F, 2.06902F, 1.86409F, 1.66991F, 1.48771F, 1.31831F, 1.16212F, 1.01922F, 0.88942F, 0.77230F, 0.66732F, 0.57375F, 0.49090F, 0.41788F, 0.35395F, 0.29823F, 0.24996F, 0.20833F, 0.17265F, 0.14221F},
    {0.00000F, 1.75022F, 1.57694F, 1.41400F, 1.26206F, 1.12144F, 0.99218F, 0.87410F, 0.76687F, 0.67003F, 0.58303F, 0.50526F, 0.43607F, 0.37481F, 0.32082F, 0.27345F, 0.23206F, 0.19607F, 0.16490F, 0.13804F, 0.11498F, 0.09527F},
    {0.00000F, 1.50587F, 1.35804F, 1.21967F, 1.09109F, 0.97240F, 0.86343F, 0.76395F, 0.67354F, 0.59177F, 0.51814F, 0.45211F, 0.39314F, 0.34068F, 0.29420F, 0.25316F, 0.21708F, 0.18544F, 0.15784F, 0.13382F, 0.11302F, 0.09505F, 0.07960F, 0.06635F},
    {0.00000F, 1.31360F, 1.18637F, 1.06759F, 0.95743F, 0.85584F, 0.76262F, 0.67747F, 0.60002F, 0.52985F, 0.46650F, 0.40952F, 0.35843F, 0.31280F, 0.27218F, 0.23613F, 0.20424F, 0.17611F, 0.15137F, 0.12970F, 0.11076F, 0.09428F, 0.07997F, 0.06758F, 0.05690F, 0.04771F},
    {0.00000F, 1.15902F, 1.04861F, 0.94567F, 0.85027F, 0.76232F, 0.68159F, 0.60780F, 0.54058F, 0.47955F, 0.42432F, 0.37449F, 0.32967F, 0.28950F, 0.25358F, 0.22154F, 0.19304F, 0.16777F, 0.14542F, 0.12571F, 0.10837F, 0.09316F, 0.07985F, 0.06824F, 0.05813F, 0.04936F, 0.04178F, 0.03524F},
    {0.00000F, 1.03253F, 0.93595F, 0.84598F, 0.76261F, 0.68572F, 0.61510F, 0.55045F, 0.49148F, 0.43780F, 0.38915F, 0.34512F, 0.30540F, 0.26965F, 0.23756F, 0.20883F, 0.18315F, 0.16028F, 0.13992F, 0.12189F, 0.10591F, 0.09182F, 0.07940F, 0.06848F, 0.05892F, 0.05055F, 0.04325F, 0.03690F, 0.03139F, 0.02662F},
    {0.00000F, 0.92742F, 0.84237F, 0.76312F, 0.68968F, 0.62189F, 0.55955F, 0.50242F, 0.45021F, 0.40260F, 0.35933F, 0.32008F, 0.28455F, 0.25248F, 0.22359F, 0.19762F, 0.17432F, 0.15347F, 0.13483F, 0.11822F, 0.10345F, 0.09033F, 0.07870F, 0.06843F, 0.05936F, 0.05137F, 0.04437F, 0.03821F, 0.03283F, 0.02814F, 0.02405F, 0.02050F},
    {0.00000F, 0.83896F, 0.76357F, 0.69330F, 0.62813F, 0.56793F, 0.51250F, 0.46161F, 0.41501F, 0.37246F, 0.33368F, 0.29843F, 0.26643F, 0.23745F, 0.21126F, 0.18763F, 0.16635F, 0.14723F, 0.13008F, 0.11472F, 0.10100F, 0.08875F, 0.07784F, 0.06815F, 0.05954F, 0.05192F, 0.04518F, 0.03924F, 0.03401F, 0.02941F, 0.02537F, 0.02184F, 0.01875F, 0.01606F},
  };
  private static final float[][] SPREAD_DIST_BS = {
    {},
    {},
    {},
    {},
    {},
    {0.00000F, 0.89506F, 1.00301F, 1.11225F, 1.22256F, 1.33417F, 1.44746F, 1.56295F, 1.68123F, 1.80304F, 1.92924F, 2.06090F},
    {0.00000F, 0.87728F, 0.96682F, 1.05750F, 1.14889F, 1.24105F, 1.33417F, 1.42845F, 1.52416F, 1.62170F, 1.72144F, 1.82373F, 1.92923F, 2.03858F},
    {0.00000F, 0.86464F, 0.94109F, 1.01853F, 1.09656F, 1.17513F, 1.25429F, 1.33417F, 1.41491F, 1.49668F, 1.57967F, 1.66414F, 1.75035F, 1.83859F, 1.92923F, 2.02267F},
    {0.00000F, 0.85520F, 0.92186F, 0.98941F, 1.05750F, 1.12598F, 1.19486F, 1.26423F, 1.33417F, 1.40477F, 1.47610F, 1.54836F, 1.62170F, 1.69629F, 1.77222F, 1.84979F, 1.92924F, 2.01085F},
    {0.00000F, 0.84788F, 0.90695F, 0.96682F, 1.02719F, 1.08787F, 1.14889F, 1.21024F, 1.27197F, 1.33417F, 1.39686F, 1.46019F, 1.52416F, 1.58901F, 1.65466F, 1.72144F, 1.78930F, 1.85855F, 1.92924F, 2.00165F},
    {0.00000F, 0.84206F, 0.89506F, 0.94882F, 1.00301F, 1.05750F, 1.11225F, 1.16727F, 1.22256F, 1.27818F, 1.33417F, 1.39057F, 1.44746F, 1.50490F, 1.56295F, 1.62170F, 1.68123F, 1.74165F, 1.80304F, 1.86553F, 1.92924F, 1.99431F},
    {0.00000F, 0.83729F, 0.88535F, 0.93409F, 0.98326F, 1.03268F, 1.08235F, 1.13221F, 1.18231F, 1.23264F, 1.28324F, 1.33417F, 1.38541F, 1.43709F, 1.48916F, 1.54179F, 1.59489F, 1.64870F, 1.70309F, 1.75832F, 1.81431F, 1.87129F, 1.92923F, 1.98835F},
    {0.00000F, 0.83333F, 0.87728F, 0.92186F, 0.96682F, 1.01206F, 1.05750F, 1.10311F, 1.14889F, 1.19486F, 1.24105F, 1.28747F, 1.33417F, 1.38116F, 1.42845F, 1.47610F, 1.52416F, 1.57268F, 1.62170F, 1.67128F, 1.72144F, 1.77222F, 1.82373F, 1.87604F, 1.92923F, 1.98338F},
    {0.00000F, 0.82999F, 0.87046F, 0.91153F, 0.95296F, 0.99464F, 1.03652F, 1.07852F, 1.12068F, 1.16301F, 1.20550F, 1.24819F, 1.29109F, 1.33417F, 1.37750F, 1.42113F, 1.46506F, 1.50934F, 1.55401F, 1.59903F, 1.64450F, 1.69046F, 1.73696F, 1.78405F, 1.83178F, 1.88014F, 1.92924F, 1.97915F},
    {0.00000F, 0.82713F, 0.86464F, 0.90269F, 0.94109F, 0.97973F, 1.01853F, 1.05750F, 1.09656F, 1.13581F, 1.17513F, 1.21464F, 1.25429F, 1.29413F, 1.33417F, 1.37440F, 1.41491F, 1.45562F, 1.49668F, 1.53798F, 1.57967F, 1.62170F, 1.66414F, 1.70703F, 1.75035F, 1.79425F, 1.83859F, 1.88363F, 1.92924F, 1.97558F},
    {0.00000F, 0.82465F, 0.85960F, 0.89506F, 0.93083F, 0.96682F, 1.00301F, 1.03929F, 1.07570F, 1.11225F, 1.14889F, 1.18565F, 1.22256F, 1.25960F, 1.29678F, 1.33417F, 1.37172F, 1.40946F, 1.44746F, 1.48570F, 1.52416F, 1.56295F, 1.60206F, 1.64145F, 1.68124F, 1.72144F, 1.76198F, 1.80304F, 1.84462F, 1.88662F, 1.92923F, 1.97251F},
    {0.00000F, 0.82249F, 0.85520F, 0.88837F, 0.92186F, 0.95555F, 0.98941F, 1.02340F, 1.05750F, 1.09170F, 1.12598F, 1.16036F, 1.19486F, 1.22948F, 1.26423F, 1.29912F, 1.33417F, 1.36938F, 1.40477F, 1.44033F, 1.47610F, 1.51211F, 1.54836F, 1.58489F, 1.62170F, 1.65883F, 1.69629F, 1.73407F, 1.77222F, 1.81079F, 1.84979F, 1.88926F, 1.92923F, 1.96975F},
  };

  private static float findValue(float[][] spreadDist, int spreadIndex, int distIndex, float spreadDelta, float distDelta) {
    final float start = spreadDist[spreadIndex][distIndex] + distDelta * (spreadDist[spreadIndex][distIndex + 1] - spreadDist[spreadIndex][distIndex]);
    final float end = spreadDist[spreadIndex + 1][distIndex] + distDelta * (spreadDist[spreadIndex + 1][distIndex + 1] - spreadDist[spreadIndex + 1][distIndex]);
    return start + spreadDelta * (end - start);
  }

  // look up table base curve fitting
  // averages values for locations between known spread/minDist pairs
  public static float[] curveFit(float spread, float minDist) {
    if (spread < 0.5F || spread > 1.5F) {
      throw new IllegalArgumentException("Spread must be in the range 0.5 < spread <= 1.5, got : " + spread);
    }
    if (minDist < 0 || minDist > spread) {
      throw new IllegalArgumentException("Expecting 0 < minDist < " + spread + ", got : " + minDist);
    }
    final int spreadIndex = (int) (10 * spread);
    final float spreadDelta = (10 * spread - spreadIndex) / 10.0F;
    final int distIndex = (int) (20 * minDist);
    final float distDelta = (20 * minDist - distIndex) / 20.0F;
    final float a = findValue(SPREAD_DIST_AS, spreadIndex, distIndex, spreadDelta, distDelta);
    final float b = findValue(SPREAD_DIST_BS, spreadIndex, distIndex, spreadDelta, distDelta);
    return new float[] {a, b};
  }


  private static double curve(final float x, final float a, final float b) {
    return 1.0 / (1.0 + a * Math.pow(x, 2 * b));
  }

  private static float[] wrapCurve(final float[] x, final float[] y, final float a, final float b) {
    final float[] res = new float[x.length];
    for (int i = 0; i < x.length; i++) {
      res[i] = (float) (curve(x[i], a, b) - y[i]);
    }
    return res;
  }


  //        def curve_fit(f, xdata, ydata, p0=None, sigma=None, absolute_sigma=False,
//                check_finite=True, bounds=(-np.inf, np.inf), method=None,
//                jac=None, **kwargs):
//        """
//    Use non-linear least squares to fit a function, f, to data.
//
//    Assumes ``ydata = f(xdata, *params) + eps``
//
//    Parameters
//    ----------
//    f : callable
//        The model function, f(x, ...).  It must take the independent
//        variable as the first argument and the parameters to fit as
//        separate remaining arguments.
//    xdata : array_like or object
//        The independent variable where the data is measured.
//        Should usually be an M-length sequence or an (k,M)-shaped array for
//        functions with k predictors, but can actually be any object.
//    ydata : array_like
//        The dependent data, a length M array - nominally ``f(xdata, ...)``.
//    p0 : array_like, optional
//        Initial guess for the parameters (length N).  If None, then the
//        initial values will all be 1 (if the number of parameters for the
//        function can be determined using introspection, otherwise a
//        ValueError is raised).
//    sigma : None or M-length sequence or MxM array, optional
//        Determines the uncertainty in `ydata`. If we define residuals as
//        ``r = ydata - f(xdata, *popt)``, then the interpretation of `sigma`
//        depends on its number of dimensions:
//
//            - A 1-d `sigma` should contain values of standard deviations of
//              errors in `ydata`. In this case, the optimized function is
//              ``chisq = sum((r / sigma) ** 2)``.
//
//            - A 2-d `sigma` should contain the covariance matrix of
//              errors in `ydata`. In this case, the optimized function is
//              ``chisq = r.T @ inv(sigma) @ r``.
//
//              .. versionadded:: 0.19
//
//        None (default) is equivalent of 1-d `sigma` filled with ones.
//    absolute_sigma : bool, optional
//        If True, `sigma` is used in an absolute sense and the estimated parameter
//        covariance `pcov` reflects these absolute values.
//
//        If False, only the relative magnitudes of the `sigma` values matter.
//        The returned parameter covariance matrix `pcov` is based on scaling
//        `sigma` by a constant factor. This constant is set by demanding that the
//        reduced `chisq` for the optimal parameters `popt` when using the
//        *scaled* `sigma` equals unity. In other words, `sigma` is scaled to
//        match the sample variance of the residuals after the fit.
//        Mathematically,
//        ``pcov(absolute_sigma=False) = pcov(absolute_sigma=True) * chisq(popt)/(M-N)``
//    check_finite : bool, optional
//        If True, check that the input arrays do not contain nans of infs,
//        and raise a ValueError if they do. Setting this parameter to
//        False may silently produce nonsensical results if the input arrays
//        do contain nans. Default is True.
//    bounds : 2-tuple of array_like, optional
//        Lower and upper bounds on parameters. Defaults to no bounds.
//        Each element of the tuple must be either an array with the length equal
//        to the number of parameters, or a scalar (in which case the bound is
//        taken to be the same for all parameters.) Use ``np.inf`` with an
//        appropriate sign to disable bounds on all or some parameters.
//
//        .. versionadded:: 0.17
//    method : {'lm', 'trf', 'dogbox'}, optional
//        Method to use for optimization.  See `least_squares` for more details.
//        Default is 'lm' for unconstrained problems and 'trf' if `bounds` are
//        provided. The method 'lm' won't work when the number of observations
//        is less than the number of variables, use 'trf' or 'dogbox' in this
//        case.
//
//        .. versionadded:: 0.17
//    jac : callable, string or None, optional
//        Function with signature ``jac(x, ...)`` which computes the Jacobian
//        matrix of the model function with respect to parameters as a dense
//        array_like structure. It will be scaled according to provided `sigma`.
//        If None (default), the Jacobian will be estimated numerically.
//        String keywords for 'trf' and 'dogbox' methods can be used to select
//        a finite difference scheme, see `least_squares`.
//
//        .. versionadded:: 0.18
//    kwargs
//        Keyword arguments passed to `leastsq` for ``method='lm'`` or
//        `least_squares` otherwise.
//
//    Returns
//    -------
//    popt : array
//        Optimal values for the parameters so that the sum of the squared
//        residuals of ``f(xdata, *popt) - ydata`` is minimized
//    pcov : 2d array
//        The estimated covariance of popt. The diagonals provide the variance
//        of the parameter estimate. To compute one standard deviation errors
//        on the parameters use ``perr = np.sqrt(np.diag(pcov))``.
//
//        How the `sigma` parameter affects the estimated covariance
//        depends on `absolute_sigma` argument, as described above.
//
//        If the Jacobian matrix at the solution doesn't have a full rank, then
//        'lm' method returns a matrix filled with ``np.inf``, on the other hand
//        'trf'  and 'dogbox' methods use Moore-Penrose pseudoinverse to compute
//        the covariance matrix.
//
//    Raises
//    ------
//    ValueError
//        if either `ydata` or `xdata` contain NaNs, or if incompatible options
//        are used.
//
//    RuntimeError
//        if the least-squares minimization fails.
//
//    OptimizeWarning
//        if covariance of the parameters can not be estimated.
//
//    See Also
//    --------
//    least_squares : Minimize the sum of squares of nonlinear functions.
//    scipy.stats.linregress : Calculate a linear least squares regression for
//                             two sets of measurements.
//
//    Notes
//    -----
//    With ``method='lm'``, the algorithm uses the Levenberg-Marquardt algorithm
//    through `leastsq`. Note that this algorithm can only deal with
//    unconstrained problems.
//
//    Box constraints can be handled by methods 'trf' and 'dogbox'. Refer to
//    the docstring of `least_squares` for more information.
//
//    Examples
//    --------
//    >>> import matplotlib.pyplot as plt
//    >>> from scipy.optimize import curve_fit
//
//    >>> def func(x, a, b, c):
//    ...     return a * np.exp(-b * x) + c
//
//    Define the data to be fit with some noise:
//
//    >>> xdata = np.linspace(0, 4, 50)
//    >>> y = func(xdata, 2.5, 1.3, 0.5)
//    >>> np.random.seed(1729)
//    >>> y_noise = 0.2 * np.random.normal(size=xdata.size)
//    >>> ydata = y + y_noise
//    >>> plt.plot(xdata, ydata, 'b-', label='data')
//
//    Fit for the parameters a, b, c of the function `func`:
//
//    >>> popt, pcov = curve_fit(func, xdata, ydata)
//    >>> popt
//    array([ 2.55423706,  1.35190947,  0.47450618])
//    >>> plt.plot(xdata, func(xdata, *popt), 'r-',
//    ...          label='fit: a=%5.3f, b=%5.3f, c=%5.3f' % tuple(popt))
//
//    Constrain the optimization to the region of ``0 <= a <= 3``,
//    ``0 <= b <= 1`` and ``0 <= c <= 0.5``:
//
//    >>> popt, pcov = curve_fit(func, xdata, ydata, bounds=(0, [3., 1., 0.5]))
//    >>> popt
//    array([ 2.43708906,  1.        ,  0.35015434])
//    >>> plt.plot(xdata, func(xdata, *popt), 'g--',
//    ...          label='fit: a=%5.3f, b=%5.3f, c=%5.3f' % tuple(popt))
//
//    >>> plt.xlabel('x')
//    >>> plt.ylabel('y')
//    >>> plt.legend()
//    >>> plt.show()
//
//    """
//  public static float[] curveFit(float[] xdata, float[] ydata) {
    // Uses curve method above
    /*
    final int n = 2;  // number of fit parameters fixed to 2 (a and b)

    //lb, ub = prepare_bounds(bounds, n)
    final float[] lb = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
    final float[] ub = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
    float[] p0 = {1.0F, 1.0F};

    // only need lm method due to constraints above
    //final String method = "lm";

    checkValues(xdata);
    checkValues(ydata);

    //func = _wrap_func(f, xdata, ydata, transform)
    // TODO want func(xdata, *params) - ydata - see wrap_curve above
    // following is ripped out of leastsq function
    float[] wrapped_curve = wrap_curve(xdata, ydata, p0[0], p0[1]);
    final int m = wrapped_curve.length;
    assert n > m;

    final float epsfcn = 2.220446049250313e-16F; // smallest representable float
    assert epsfcn + 1.0f != 1.0f;
    final int maxfev = 200 * (n + 1);

    int col_deriv = 0;
    float ftol = 1.49012e-8F;
    float xtol = 1.49012e-8F;
    float gtol = 0.0F;
    int factor = 100;
*/
    //       if method == 'lm':
    //
    //res = leastsq(func, p0, full_output=1)
//
//        popt, pcov, infodict, errmsg, ier = res
//        cost = np.sum(infodict['fvec'] ** 2)
//        if ier not in [1, 2, 3, 4]:
//        raise RuntimeError("Optimal parameters not found: " + errmsg)


//        warn_cov = False
//        if pcov is None:
//        //# indeterminate covariance
//        pcov = zeros((len(popt), len(popt)), dtype=float)
//        pcov.fill(inf)
//        warn_cov = True
//        elif not absolute_sigma:
//        if ydata.size > p0.size:
//        s_sq = cost / (ydata.size - p0.size)
//        pcov = pcov * s_sq
//        else:
//        pcov.fill(inf)
//        warn_cov = True
//
//        if warn_cov:
//        warnings.warn('Covariance of the parameters could not be estimated',
//                category=OptimizeWarning)
//
//        if return_full:
//        return popt, pcov, infodict, errmsg, ier
//    else:
//        return popt, pcov
//    return null;
//  }
//
//
//    static float[] leastsq(func, x0, args=(), Dfun=None, full_output=0,
//    col_deriv=0, ftol=1.49012e-8, xtol=1.49012e-8,
//    gtol=0.0, maxfev=0, epsfcn=None, factor=100, diag=None):
//            """
//    Minimize the sum of squares of a set of equations.
//
//    ::
//
//        x = arg min(sum(func(y)**2,axis=0))
//                 y
//
//    Parameters
//    ----------
//    func : callable
//        should take at least one (possibly length N vector) argument and
//        returns M floating point numbers. It must not return NaNs or
//        fitting might fail.
//    x0 : ndarray
//        The starting estimate for the minimization.
//    args : tuple, optional
//        Any extra arguments to func are placed in this tuple.
//    Dfun : callable, optional
//        A function or method to compute the Jacobian of func with derivatives
//        across the rows. If this is None, the Jacobian will be estimated.
//    full_output : bool, optional
//        non-zero to return all optional outputs.
//    col_deriv : bool, optional
//        non-zero to specify that the Jacobian function computes derivatives
//        down the columns (faster, because there is no transpose operation).
//    ftol : float, optional
//        Relative error desired in the sum of squares.
//    xtol : float, optional
//        Relative error desired in the approximate solution.
//    gtol : float, optional
//        Orthogonality desired between the function vector and the columns of
//        the Jacobian.
//    maxfev : int, optional
//        The maximum number of calls to the function. If `Dfun` is provided
//        then the default `maxfev` is 100*(N+1) where N is the number of elements
//        in x0, otherwise the default `maxfev` is 200*(N+1).
//    epsfcn : float, optional
//        A variable used in determining a suitable step length for the forward-
//        difference approximation of the Jacobian (for Dfun=None).
//        Normally the actual step length will be sqrt(epsfcn)*x
//        If epsfcn is less than the machine precision, it is assumed that the
//        relative errors are of the order of the machine precision.
//    factor : float, optional
//        A parameter determining the initial step bound
//        (``factor * || diag * x||``). Should be in interval ``(0.1, 100)``.
//    diag : sequence, optional
//        N positive entries that serve as a scale factors for the variables.
//
//    Returns
//    -------
//    x : ndarray
//        The solution (or the result of the last iteration for an unsuccessful
//        call).
//    cov_x : ndarray
//        The inverse of the Hessian. `fjac` and `ipvt` are used to construct an
//        estimate of the Hessian. A value of None indicates a singular matrix,
//        which means the curvature in parameters `x` is numerically flat. To
//        obtain the covariance matrix of the parameters `x`, `cov_x` must be
//        multiplied by the variance of the residuals -- see curve_fit.
//    infodict : dict
//        a dictionary of optional outputs with the keys:
//
//        ``nfev``
//            The number of function calls
//        ``fvec``
//            The function evaluated at the output
//        ``fjac``
//            A permutation of the R matrix of a QR
//            factorization of the final approximate
//            Jacobian matrix, stored column wise.
//            Together with ipvt, the covariance of the
//            estimate can be approximated.
//        ``ipvt``
//            An integer array of length N which defines
//            a permutation matrix, p, such that
//            fjac*p = q*r, where r is upper triangular
//            with diagonal elements of nonincreasing
//            magnitude. Column j of p is column ipvt(j)
//            of the identity matrix.
//        ``qtf``
//            The vector (transpose(q) * fvec).
//
//    mesg : str
//        A string message giving information about the cause of failure.
//    ier : int
//        An integer flag.  If it is equal to 1, 2, 3 or 4, the solution was
//        found.  Otherwise, the solution was not found. In either case, the
//        optional output variable 'mesg' gives more information.
//
//    Notes
//    -----
//    "leastsq" is a wrapper around MINPACK's lmdif and lmder algorithms.
//
//    cov_x is a Jacobian approximation to the Hessian of the least squares
//    objective function.
//    This approximation assumes that the objective function is based on the
//    difference between some observed target data (ydata) and a (non-linear)
//    function of the parameters `f(xdata, params)` ::
//
//           func(params) = ydata - f(xdata, params)
//
//    so that the objective function is ::
//
//           min   sum((ydata - f(xdata, params))**2, axis=0)
//         params
//
//    The solution, `x`, is always a 1D array, regardless of the shape of `x0`,
//    or whether `x0` is a scalar.
//    """
//    x0 = asarray(x0).flatten()
//    n = len(x0)
//    if not isinstance(args, tuple):
//    args = (args,)
//    shape, dtype = _check_func('leastsq', 'func', func, x0, args, n)
//    m = shape[0]
//
//            if n > m:
//    raise TypeError('Improper input: N=%s must not exceed M=%s' % (n, m))
//
//            if epsfcn is None:
//    epsfcn = finfo(dtype).eps
//
//    if Dfun is None:
//            if maxfev == 0:
//    maxfev = 200*(n + 1)
//    retval = _minpack._lmdif(func, x0, args, full_output, ftol, xtol,
//    gtol, maxfev, epsfcn, factor, diag)
//            else:
//            if col_deriv:
//    _check_func('leastsq', 'Dfun', Dfun, x0, args, n, (n, m))
//            else:
//    _check_func('leastsq', 'Dfun', Dfun, x0, args, n, (m, n))
//            if maxfev == 0:
//    maxfev = 100 * (n + 1)
//    retval = _minpack._lmder(func, Dfun, x0, args, full_output,
//    col_deriv, ftol, xtol, gtol, maxfev,
//    factor, diag)
//
//    errors = {0: ["Improper input parameters.", TypeError],
//        1: ["Both actual and predicted relative reductions "
//        "in the sum of squares\n  are at most %f" % ftol, None],
//        2: ["The relative error between two consecutive "
//        "iterates is at most %f" % xtol, None],
//        3: ["Both actual and predicted relative reductions in "
//        "the sum of squares\n  are at most %f and the "
//        "relative error between two consecutive "
//        "iterates is at \n  most %f" % (ftol, xtol), None],
//        4: ["The cosine of the angle between func(x) and any "
//        "column of the\n  Jacobian is at most %f in "
//        "absolute value" % gtol, None],
//        5: ["Number of calls to function has reached "
//        "maxfev = %d." % maxfev, ValueError],
//        6: ["ftol=%f is too small, no further reduction "
//        "in the sum of squares\n  is possible.""" % ftol,
//                ValueError],
//        7: ["xtol=%f is too small, no further improvement in "
//        "the approximate\n  solution is possible." % xtol,
//                ValueError],
//        8: ["gtol=%f is too small, func(x) is orthogonal to the "
//        "columns of\n  the Jacobian to machine "
//        "precision." % gtol, ValueError]}
//
//    # The FORTRAN return value (possible return values are >= 0 and <= 8)
//    info = retval[-1]
//
//            if full_output:
//    cov_x = None
//        if info in LEASTSQ_SUCCESS:
//    from numpy.dual import inv
//            perm = take(eye(n), retval[1]['ipvt'] - 1, 0)
//    r = triu(transpose(retval[1]['fjac'])[:n, :])
//    R = dot(r, perm)
//            try:
//    cov_x = inv(dot(transpose(R), R))
//    except (LinAlgError, ValueError):
//    pass
//        return (retval[0], cov_x) + retval[1:-1] + (errors[info][0], info)
//            else:
//            if info in LEASTSQ_FAILURE:
//            warnings.warn(errors[info][0], RuntimeWarning)
//    elif info == 0:
//    raise errors[info][1](errors[info][0])
//            return retval[0], info
//      return null;
//    }

  private static void checkValues(float[] data) {
    if (data.length == 0) {
      throw new IllegalArgumentException("Array must not be empty.");
    }
    for (final float value : data) {
      if (Float.isNaN(value) || Float.isInfinite(value)) {
        throw new IllegalArgumentException("Array cannot contain NaN or Infinity.");
      }
    }
  }
}

