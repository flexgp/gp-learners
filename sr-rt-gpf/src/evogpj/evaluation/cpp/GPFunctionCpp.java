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
 * Implements fitness evaluation for the GPFunction algorithm in C++.
 * 
 * @author Ignacio Arnaldo
 */
public class GPFunctionCpp extends FitnessFunction {

	private final boolean USE_INT;

	// defaults to SR_DATA_FITNESS
	public static String FITNESS_KEY = Parameters.Operators.GPFUNCTION_CPP;

	public Boolean isMaximizingFunction = true;

	public Boolean discreteFitness = false;

	ArrayList<String> FUNC_SET, UNARY_FUNC_SET;
	String datasetPath;
	int numberOfFitnessCases, numberOfFeatures, numberOfResults,
			currentMaxSize, pow, numberOfThreads;
	
	public GPFunctionCpp(List<String> aFUNC_SET,
			List<String> aUNARY_FUNC_SET, String aDataset,
			int aNumberOfFitnessCases, int aNumberOfFeatures,
			int aNumberOfResults, int aNumberOfThreads, int aPow,
			boolean useInts) {
		USE_INT = useInts;
		pow = aPow;
		FUNC_SET = (ArrayList<String>) aFUNC_SET;
		UNARY_FUNC_SET = (ArrayList<String>) aUNARY_FUNC_SET;
		datasetPath = aDataset;
		numberOfFitnessCases = aNumberOfFitnessCases;
		numberOfFeatures = aNumberOfFeatures;
		numberOfResults = aNumberOfResults;
		currentMaxSize = 0;
		numberOfThreads = aNumberOfThreads;
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
			evaluatePopulationCPU(pop, USE_INT, pow, numberOfThreads);
		} catch (IOException ex) {
			Logger.getLogger(GPFunctionCpp.class.getName()).log(
					Level.SEVERE, null, ex);
		}
	}

	public void evaluatePopulationCPU(Population pop, boolean useInts, int p, int numberOfThreads)throws IOException {

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
		
		String cppFile = "tempFiles/SRRoCCpp.cpp";
		String binCPP = "tempFiles/SRRocCpp";
		GenerateGPFunctionCpp groc = new GenerateGPFunctionCpp(cppFile);
		groc.setExpressions(expressions);
		// gcpp.generateCode(numberOfIndividuals,datasetPath,numberOfFitnessCases,numberOfFeatures,numberOfResults);
		startTime = System.currentTimeMillis();
		groc.generateCode(numberOfIndividuals, datasetPath,
				numberOfFitnessCases, numberOfFeatures, numberOfResults,
				numberOfThreads, useInts, p);
		System.out.format("Code generation: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
		groc.printCodeToFile(cppFile);
		System.out.format("Write code to file: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
		groc.compileFile(cppFile, binCPP);
		System.out.format("C++ Compilation: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
		groc.runCode(binCPP);
		System.out.format("C++ Runtime: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);
		startTime = System.currentTimeMillis();
                ArrayList<Float> fitnessArrayList = new ArrayList<Float>();
                ArrayList<Float> minOutputArrayList = new ArrayList<Float>();
                ArrayList<Float> maxOutputArrayList = new ArrayList<Float>();
		groc.readResults(fitnessArrayList,minOutputArrayList,maxOutputArrayList);
		System.out.format("Read results: %f\n", (System.currentTimeMillis() - startTime) / 1000.0);

		// for (Individual i : pop) {
		for (int i = 0; i < numberOfIndividuals; i++) {
                    Individual ind = pop.get(i);
                    double fitnessAux = (double) fitnessArrayList.get(i);
                    ind.setFitness(GPFunctionCpp.FITNESS_KEY, fitnessAux);
                    double minTrainOutput = (double) minOutputArrayList.get(i);
                    ind.setMinTrainOutput(minTrainOutput);
                    double maxTrainOutput = (double) maxOutputArrayList.get(i);
                    ind.setMaxTrainOutput(maxTrainOutput);
		}

	}

}