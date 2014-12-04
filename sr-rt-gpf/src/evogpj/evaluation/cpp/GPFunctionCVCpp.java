/**
 * Copyright (c) 2011-2013 ALFA Group
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
 * @author Ignacio Arnaldo
 * 
 */
package evogpj.evaluation.cpp;

import evogpj.evaluation.Expression;
import evogpj.evaluation.ParseExpression;
import evogpj.genotype.Tree;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import evogpj.algorithm.Parameters;
import evogpj.evaluation.FitnessFunction;

/**
 * Implements fitness evaluation for the GPFunction classifiers on the 
 * validation set in C++.
 * 
 * @author Owen Derby
 */
public class GPFunctionCVCpp extends FitnessFunction {

	// defaults to SR_DATA_FITNESS
	public static String FITNESS_KEY = Parameters.Operators.GPFUNCTION_CV_CPP;

	public Boolean isMaximizingFunction = true;

	public Boolean discreteFitness = false;

	ArrayList<String> FUNC_SET, UNARY_FUNC_SET;
	String datasetPath;
	int numberOfFitnessCases, numberOfFeatures, numberOfResults,
			currentMaxSize, numberOfThreads;
        
        private float fpWeight,fnWeight;
        private int numLambdas;
        
	
	public GPFunctionCVCpp(List<String> aFUNC_SET,List<String> aUNARY_FUNC_SET, String aDataset,int aNumberOfFitnessCases, int aNumberOfFeatures,
                                int aNumberOfResults, int aNumberOfThreads, float afpWeight,float afnWeight,int aNumLambdas) {
		FUNC_SET = (ArrayList<String>) aFUNC_SET;
		UNARY_FUNC_SET = (ArrayList<String>) aUNARY_FUNC_SET;
		datasetPath = aDataset;
		numberOfFitnessCases = aNumberOfFitnessCases;
		numberOfFeatures = aNumberOfFeatures;
		numberOfResults = aNumberOfResults;
		currentMaxSize = 0;
		numberOfThreads = aNumberOfThreads;
                fpWeight = afpWeight;
                fnWeight = afnWeight;
                numLambdas = aNumLambdas;
	}

	/**
	 * Should this fitness function be minimized (i.e. mean squared error) or
	 * maximized?
	 */
	@Override
	public Boolean isMaximizingFunction() {
		return this.isMaximizingFunction;
	}

	@Override
	public void evalPop(Population pop) {
		try {
			evaluatePopulationCPU(pop, numberOfThreads);
		} catch (IOException ex) {
			Logger.getLogger(GPFunctionCpp.class.getName()).log(
					Level.SEVERE, null, ex);
		}
	}

	public void evaluatePopulationCPU(Population pop, int numberOfThreads)throws IOException {

		int numberOfIndividuals = pop.size();
		Expression[] expressions = new Expression[numberOfIndividuals];
		for (int i = 0; i < numberOfIndividuals; i++) {
			expressions[i] = new Expression();
			expressions[i].setOps((ArrayList<String>) FUNC_SET);
			expressions[i].setUnOps((ArrayList<String>) UNARY_FUNC_SET);
			// final Function func = genotype.generate();
			// String sAux = func.toString();
			String prefixExpression = ((Tree) pop.get(i).getGenotype()).toPrefixString();
			expressions[i].setPrefixExpression(prefixExpression);
		}

		ParseExpression pe = new ParseExpression();
		for (int i = 0; i < numberOfIndividuals; i++) {
			pe.setExpression(expressions[i]);
			String varResult = "Var_0";
			String prefixExpression = expressions[i].getPrefixExpression();
			pe.getIntermediateFromPrefix(prefixExpression, varResult);
		}
		
		Long startTime;
		
		String cppFile = "tempFiles/SRRocCVCpp.cpp";
		String binCPP = "tempFiles/SRRocCVCpp";
		GenerateGPFunctionCVCpp groccv = new GenerateGPFunctionCVCpp(cppFile);
		groccv.setExpressions(expressions);
		// gcpp.generateCode(numberOfIndividuals,datasetPath,numberOfFitnessCases,numberOfFeatures,numberOfResults);
		startTime = System.currentTimeMillis();
		groccv.generateCode(numberOfIndividuals, datasetPath,numberOfFitnessCases, numberOfFeatures, numberOfResults,numberOfThreads,fpWeight,fnWeight,numLambdas);
		System.out.format("Code generation: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
		groccv.printCodeToFile(cppFile);
		System.out.format("Write code to file: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
		groccv.compileFile(cppFile, binCPP);
		System.out.format("C++ Compilation: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
		groccv.runCode(binCPP);
		System.out.format("C++ Runtime: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
		ArrayList<Float> areaROCArrayList = new ArrayList<Float>();
                ArrayList<Float> bestThresoldsArrayList = new ArrayList<Float>();
                groccv.readResults(areaROCArrayList,bestThresoldsArrayList);

                //for (Individual i : pop) {
                for(int i=0;i<numberOfIndividuals;i++){
                    Individual ind = pop.get(i);
                    double areaAux = (double)areaROCArrayList.get(i);
                    ind.setCrossValAreaROC(areaAux);
                    double bestLambda = (double)bestThresoldsArrayList.get(i);
                    ind.setThreshold(bestLambda);
                }
		System.out.format("Read results: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);



	}

}