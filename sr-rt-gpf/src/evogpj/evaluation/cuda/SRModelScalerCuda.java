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

package evogpj.evaluation.cuda;

import evogpj.evaluation.Expression;
import evogpj.evaluation.ParseExpression;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import evogpj.algorithm.Parameters;
import evogpj.genotype.Tree;

/**
 * Generates CUDA code that scales symbolic regression models
 * 
 * @author Ignacio Arnaldo
 */
public class SRModelScalerCuda {

    private final boolean USE_INT;
    public static String FITNESS_KEY = Parameters.Operators.SR_CUDA_FITNESS;
    public Boolean isMaximizingFunction = true;
    public Boolean discreteFitness = false;
    ArrayList<String> FUNC_SET, UNARY_FUNC_SET;
    String datasetPath;
    int numberOfFitnessCases, numberOfFeatures, numberOfResults,currentMaxSize, pow, numberOfThreads;
	
    public SRModelScalerCuda(List<String> aFUNC_SET,
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

    public void scalePop(Population pop) {
        try {
            scalePopCPU(pop, USE_INT, pow, numberOfThreads);
        } catch (IOException ex) {
            Logger.getLogger(SRModelScalerCuda.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    public void scalePopCPU(Population pop, boolean useInts, int p, int numberOfThreads) throws IOException {
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
        String cppFile = "tempFiles/scaleModelsCuda.cpp";
        String binCPP = "tempFiles/scaleModelsCUDA";
        GenerateSRModelScalerCuda gcpps = new GenerateSRModelScalerCuda(cppFile);
        gcpps.setExpressions(expressions);
        gcpps.generateCode(numberOfIndividuals, datasetPath,numberOfFitnessCases, numberOfFeatures, numberOfResults,numberOfThreads, useInts, p);
        gcpps.printCodeToFile(cppFile);
        gcpps.compileFile(cppFile, binCPP);
        gcpps.runCode(binCPP);
        ArrayList<ArrayList<Float>> slopesInterArrayList = gcpps.readResults();


        // for (Individual i : pop) {
        for (int i = 0; i < numberOfIndividuals; i++) {
            Individual ind = pop.get(i);
            double slopeAux = (double) slopesInterArrayList.get(i).get(0);
            double interceptAux = (double) slopesInterArrayList.get(i).get(1);
            Tree tind = (Tree)ind.getGenotype();
            tind.setScalingSlope(slopeAux);
            tind.setScalingIntercept(interceptAux);
        }

    }


}