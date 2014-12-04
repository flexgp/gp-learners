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
package evogpj.postprocessing;

import evogpj.evaluation.java.DataJava;
import evogpj.genotype.Tree;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.util.ArrayList;
import java.util.List;

import evogpj.math.Function;

/**
 * Evaluate scaled symbolic regression models
 * 
 * @author Ignacio Arnaldo
 */
public class EvalScaledModels {
        
        public static String name = "evalScaledModels";
                
	private final DataJava data;

	public Boolean isMaximizingFunction = true;

	public Boolean discreteFitness = false;
	
	/**
	 * Create a new fitness operator, using the provided data, for assessing
	 * individual solutions to Symbolic Regression problems. There is one
	 * parameter for this fitness evaluation:
	 * @param data
	 *            The dataset (training cases, output variable) to use in
	 *            computing the fitness of individuals.
	 */
	public EvalScaledModels(DataJava aData) {
		this.data = aData;
	}

	/**
	 * @see Function
	 */
	public void eval(Individual ind,int indIndex,double[][] predictions,boolean round) {
		Tree genotype = (Tree) ind.getGenotype();
		Function func = genotype.generate();
		List<Double> d;
		double[][] inputValuesAux = data.getInputValues();
		for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
			d = new ArrayList<Double>();
			for (int j = 0; j < data.getNumberOfFeatures(); j++) {
				d.add(j, inputValuesAux[i][j]);
			}
			Double val = func.eval(d);
                        double slope = genotype.getScalingSlope();
                        double intercept = genotype.getScalingIntercept();
                        val = (val*slope) + intercept;
                        if(round) val = (double)Math.round(val);
                        predictions[i][indIndex] = val;
			d.clear();
		}
		func = null;
	}

    /**
     * @see Function
     */
    public void evalPop(Population pop,boolean round) {
        double[] targets = data.getTargetValues();
        for(Individual ind:pop){
            double sqdiff = 0;
            Tree genotype = (Tree) ind.getGenotype();
            Function func = genotype.generate();
            List<Double> d;
            double[][] inputValuesAux = data.getInputValues();
            for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
                    d = new ArrayList<Double>();
                    for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                            d.add(j, inputValuesAux[i][j]);
                    }
                    Double val = func.eval(d);
                    double slope = genotype.getScalingSlope();
                    double intercept = genotype.getScalingIntercept();
                    val = (val*slope) + intercept;
                    if(round) val = (double)Math.round(val);
                    
                    sqdiff += Math.pow(targets[i] - val, 2);
                    d.clear();
            }
            sqdiff = sqdiff / data.getNumberOfFitnessCases();
            ind.setScaledCrossValFitness(sqdiff);
            func = null;
        }
    }

}