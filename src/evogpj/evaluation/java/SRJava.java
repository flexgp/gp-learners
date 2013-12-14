/**
 * Copyright (c) 2011-2013 Evolutionary Design and Optimization Group
 * 
 * Licensed under the MIT License.
 * 
 * See the "LICENSE" file for a copy of the license.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.  
 *
 */
package evogpj.evaluation.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


import evogpj.genotype.Tree;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import evogpj.math.Function;
import evogpj.math.means.ArithmeticMean;
import evogpj.math.means.Maximum;
import evogpj.math.means.Mean;
import evogpj.math.means.PowerMean;
import evogpj.algorithm.Parameters;
import evogpj.evaluation.FitnessFunction;

/**
 * Implements fitness evaluation for symbolic regression.
 * 
 * @author Owen Derby
 */
public class SRJava extends FitnessFunction {

    private final DataJava data;
    private final Mean MEAN_FUNC;
    private final boolean USE_INT;
    public static String FITNESS_KEY = Parameters.Operators.SR_JAVA_FITNESS;
    public Boolean isMaximizingFunction = true;
    public Boolean discreteFitness = false;
	
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There are two
     * parameters for this fitness evaluation:
     * <p>
     * <ul>
     * <li>The power <i>p</i> to use in computing the mean of the errors. See
     * {@link #getMeanFromP(int)} for recognized values and more information.
     * This parameter is set to the value specified by the key
     * {@value algorithm.Parameters.Names#MEAN_POW} in the properties file. The
     * default value is {@value algorithm.Parameters.Defaults#MEAN_POW}.
     * <li>A boolean flag specifying if the predictions should be forced to
     * integers because the output variable is integer-valued (ie you might want
     * to set this to true if your output is integer-valued). This parameter is
     * set to the boolean at the key
     * {@value algorithm.Parameters.Names#COERCE_TO_INT}. If there is no value
     * specified, the default is
     * {@value algorithm.Parameters.Defaults#COERCE_TO_INT}.
     * </ul>
     * 
     * @param data
     *            The dataset (training cases, output variable) to use in
     *            computing the fitness of individuals.
     * @param props
     *            system properties object.
     */
    public SRJava(DataJava data) {
        this(data, 2, false);
    }

    private SRJava(DataJava aData, int pow, boolean is_double) {
        this.data = aData;
        MEAN_FUNC = getMeanFromP(pow);
        USE_INT = is_double;
    }
	
    public SRJava(DataJava aData, Properties props) {
        this.data = aData;
        int power = Parameters.Defaults.MEAN_POW;
        if (props.containsKey(Parameters.Names.MEAN_POW))
            power = Integer.valueOf(props.getProperty(Parameters.Names.MEAN_POW));
        MEAN_FUNC = getMeanFromP(power);
        if (props.containsKey(Parameters.Names.COERCE_TO_INT))
            USE_INT = Boolean.valueOf(props.getProperty(Parameters.Names.COERCE_TO_INT));
        else
            USE_INT = Parameters.Defaults.COERCE_TO_INT;
    }

    /**
     * Set this instance's FITNESS_KEY to FITNESS_KEY_NEW
     * 
     * @param data
     * @param props
     * @param FITNESS_KEY_SUFFIX
     */
    public SRJava(DataJava aData, Properties props,String FITNESS_KEY_NEW) {
        this(aData, props);
        this.FITNESS_KEY = FITNESS_KEY_NEW;
    }

    /**
     * Should this fitness function be minimized (i.e. mean squared error) or
     * maximized?
     */
    public Boolean isMaximizingFunction() {
        return this.isMaximizingFunction;
    }

    /**
     * Simple "factory"-like method for returning the proper generalized mean
     * object to use, given the parameter p (see
     * http://en.wikipedia.org/wiki/Generalized_mean). Since we are only
     * concerned with means of errors, we don't utilize all the valid values of
     * p defined for generalized means.
     * 
     * @param p
     *            power to use in computing a mean
     * @return an instance of {@link PowerMean} if p > 1; an instance of
     *         {@link ArithmeticMean} if p == 1; otherwise, an instance of
     *         {@link Maximum}.
     */
    public static Mean getMeanFromP(int p) {
        if (p == 1) {
            return new ArithmeticMean();
        } else if (p > 1) {
            return new PowerMean(p);
        } else {
            return new Maximum();
        }
    }

    /**
     * @see Function
     */
    public void eval(Individual ind) {
        if (ind.getFitness(this.FITNESS_KEY) != null) {
                return;
        }
        SRPhenotype phenotype = new SRPhenotype();
        Tree genotype = (Tree) ind.getGenotype();
        MEAN_FUNC.reset();
        SRPhenotype phenotype_tmp = new SRPhenotype();
        Function func = genotype.generate();
        // This is the most time-consuming part of any GP run - evaluation of
        // individuals!

        // for (List<Double> d : this.getFitnessCases()) {
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
                d = new ArrayList<Double>();
                for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                        d.add(j, inputValuesAux[i][j]);
                }
                Double val = func.eval(d);
                phenotype_tmp.addNewDataValue(val);
                d.clear();
        }

        // scale predictions to [0,1] range (same as scaled_data) and then compute error
        final ScaledTransform scalePrediction = new ScaledTransform(phenotype_tmp.min, phenotype_tmp.max);
        final ScaledTransform scaleTarget = new ScaledTransform(this.data.getTargetMin(), this.data.getTargetMax());
        double[] targetAux = this.getTarget();
        for (int i = 0; i < this.getTarget().length; i++) {
            Double scaled_val = scalePrediction.scaleValue(phenotype_tmp.getDataValue(i));
            if (this.USE_INT) {
                // If we're working with integer output space, we need to perform rounding in the output space.
                int rounded_val = scaleTarget.unScaleValue(scaled_val).intValue();
                scaled_val = scaleTarget.scaleValue((double) rounded_val);
            }
            MEAN_FUNC.addValue(Math.abs(targetAux[i] - scaled_val));
        }
        Double error = MEAN_FUNC.getMean();
        // Because of scaling and normalization (done automatically by MEAN_FUNC), we ensure that error is always in range [0,1].
        ind.setFitness(this.FITNESS_KEY, errorToFitness(error));
        phenotype_tmp = null;
        func = null;
}
    
    /**
     * Transform errors to fitness values. For errors, smaller values are
     * better, while for fitness, values closer to 1 are better. This particular
     * transformation also places a greater emphasis on small changes close to
     * the solution (fitness == 1.0 represents a complete solution). However,
     * this transformation also assumes that the error is in the range [0,1].
     * 
     * @param error
     *            Error on training set; value in range [0,1].
     * @return 0 if error is not a number (NaN) or outside of the range [0,1].
     *         Otherwise, return the fitness.
     */
    private Double errorToFitness(Double error) {
        if (error.isNaN() || error < 0.0 || error >= 1.0) {
            return 0.0;
        } else {
            return (1.0 - error) / (1 + error);
        }
    }

    @Override
    public void evalPop(Population pop) {
        for (Individual individual : pop) {
            this.eval(individual);
        }
    }

    /**
     * @return the fitnessCases
     */
    public double[][] getFitnessCases() {
        return data.getInputValues();
    }

    /**
     * @return the scaled_target
     */
    public double[] getTarget() {
        return data.getScaledTargetValues();
    }

    /**
     * Utility class for computing the element-wise normalization of a vector.
     * That is, for a given vector <code>A</code>, where all values are in the
     * range <code>[min(A), max(A)]</code>, we want an easy way to compute the
     * scaled vector <code>B</code> such that every value in B is in the range
     * <code>[0, 1]</code>.
     * <p>
     * In particular, if the ith element of <code>A</code> has value
     * <code>a_i</code>, then the ith element of <code>B</code> will have the
     * value <code>(a_i-min(A))/(max(A)-min(A))</code>.
     * <p>
     * Also, provide the inverse transformation, to recover the values of
     * <code>A</code>.
     * 
     * @author Owen Derby
     * @see ScaledData
     */
    private class ScaledTransform {
        private final double min, range;

        public ScaledTransform(double min, double max) {
            this.min = min;
            this.range = max - min;
        }

        public Double scaleValue(Double val) {
            return (val - min) / range;
        }

        public Double unScaleValue(Double scaled_val) {
            return (scaled_val * range) + min;
        }
    }
}
