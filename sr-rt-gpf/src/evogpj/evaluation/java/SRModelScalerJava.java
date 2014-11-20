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

import evogpj.math.Function;
import evogpj.math.means.ArithmeticMean;
import evogpj.genotype.Tree;
import evogpj.gp.Individual;

/**
 * Performs linear regression to calculate optimal coefficients for linearly
 * fitting a model to the training data. DataJava in evogpj is scaled upon read-in,
 * so models are not useful unless they are scaled to fit the actual data again
 * 
 * @author Dylan Sherry
 * 
 */
public class SRModelScalerJava extends AbstractModelScaler {
	DataJava data;
	Integer features;
	Integer fitnessCases;
	
	/**
	 * @param _data training data to perform linear regression on
	 */
	public SRModelScalerJava(DataJava _data) {
		data = _data;
	}
	
	/**
	 * Performs linear regression to generate a scaled symbolic regression model
	 * 
	 * Calculates the linear regression slope and intercept (the a and b in y =
	 * ax * b). The slope is (r_xy * s_x / s_y), where r indicates a correlation
	 * coefficient and s indicates the standard deviation). For more
	 * information, see the wikipedia article on simple linear regression:
	 * http://en.wikipedia.org/wiki/Simple_linear_regression
	 * 
	 * @param model
	 * @return
	 */
        @Override
	public void scaleModel(Individual individual) {
		// first calculate the mean value of the unscaled output variable from the data, if we don't already have it
		double actualMean = data.getTargetMean();
		// get the actual predictions as well
		double[] actual = data.getTargetValues();

		// gather predictions from the data, and compute the mean prediction
		ArithmeticMean predictionMeanObject = new ArithmeticMean();
		double[] predictions = new double[data.getNumberOfFitnessCases()];
		// FIXME this won't work if "non-array data" is used, just ArrayScaledData subclasses
		Tree genotype = ((Tree) individual.getGenotype());
		Function func = genotype.generate();
		List<Double> d;
		double[][] inputValuesAux = data.getInputValues();
		for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
			d = new ArrayList<Double>();
			for (int j = 0; j < data.getNumberOfFeatures(); j++) {
				d.add(j, inputValuesAux[i][j]);
			}
			Double prediction = func.eval(d);
			predictions[i] = prediction;
			predictionMeanObject.addValue(prediction);
			d.clear();
		}
		double predictionMean = predictionMeanObject.getMean();

		// calculate slope and intercept
		double numDenSum = 0.0;
		double denSqSum = 0.0;
		for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
			double actualShifted = (actual[i] - actualMean);
			double predictionShifted = predictions[i] - predictionMean;
			denSqSum += Math.pow(predictionShifted, 2);
			numDenSum += actualShifted * predictionShifted;
		}
		double slope = numDenSum / denSqSum;
		double intercept = actualMean - (slope * predictionMean);

		// update the individual's genotype with these values
		genotype.setScalingSlope(slope);
		genotype.setScalingIntercept(intercept);
	}
}
