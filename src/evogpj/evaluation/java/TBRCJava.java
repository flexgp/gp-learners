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


import evogpj.genotype.Tree;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import evogpj.algorithm.Parameters;
import evogpj.bool.BooleanFunction;
import evogpj.evaluation.FitnessFunction;
import evogpj.preprocessing.Interval;

/**
 * Implements fitness evaluation for symbolic regression.
 * 
 * @author Owen Derby
 */
public class TBRCJava extends FitnessFunction {

    public static String FITNESS_KEY = Parameters.Operators.RBC_JAVA_FITNESS;
    
    private final DataJava data;
    ArrayList<Interval> intervals;
    private double wFP,wFN;
    public Boolean isMaximizingFunction = false;

	
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to classification problems. There are two
     * parameters for this fitness evaluation:
     * @param data
     *            The dataset (training cases, output variable) to use in
     *            computing the fitness of individuals.
     * @param intervals
     *            intervals obtained for each variable in the preprocessing step
     */
    public TBRCJava(DataJava aData,ArrayList<Interval> aIntervals,double aWFP, double aWFN) {
        this.data = aData;
        this.intervals = aIntervals;
        this.wFP = aWFP;
        this.wFN = aWFN;
    }

    /**
     * Should this fitness function be minimized (i.e. mean squared error) or
     * maximized?
     */
    @Override
    public Boolean isMaximizingFunction() {
        return this.isMaximizingFunction;
    }

    /**
     * @see Function
     */
    public void eval(Individual ind) {
        Tree genotype = (Tree) ind.getGenotype();
        BooleanFunction func = genotype.generateBoolean();
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        double[] targets = data.getTargetValues();
        double numPositives = 0;
        double numNegatives = 0;
        double numFalsePositives = 0;
        double numFalseNegatives = 0;
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            boolean target = false;
            if(targets[i]==1) target = true;
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            boolean val = func.eval(d,intervals);
            if(targets[i]==0){
                numNegatives++;
            }else if(targets[i]==1){
                numPositives++;
            }
            if(val==true && target==false) {
                numFalsePositives++;
            }else if(val==false && target==true){
                numFalseNegatives++;
            }
            d.clear();
        }
        double falsePositiveRate = numFalsePositives / numNegatives;
        double falseNegativeRate = numFalseNegatives / numPositives;
        Double error = wFP*falsePositiveRate + wFN*falseNegativeRate;
        ind.setFitness(this.FITNESS_KEY, error);
        func = null;
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
}
