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
 * Implements evaluation on the validation set of GPFunction models in CUDA.
 * 
 * @author Ignacio Arnaldo
 */
public class GPFunctionCVCuda extends FitnessFunction {


    public static String FITNESS_KEY = Parameters.Operators.GPFUNCTION_CV_CUDA;

    public Boolean isMaximizingFunction = true;

    public Boolean discreteFitness = false;

    ArrayList<String> FUNC_SET, UNARY_FUNC_SET;
    String datasetPath;
    int numberOfFitnessCases, numberOfFeatures, numberOfResults,currentMaxSize,  numberOfThreads,maxIndSize;
    GenerateGPFunctionCVCuda gcudaROCCV;
    String binCUDA = "tempFiles/SRRocCVCuda";
    String fileCodedRPN = "tempFiles/individualsCodedRPNROCCV.txt";
    String cuFile = "tempFiles/SRRocCVCuda.cu";
    float fpWeight;
    float fnweight;
    int numLambdas;
        
    public GPFunctionCVCuda(List<String> aFUNC_SET,
                    List<String> aUNARY_FUNC_SET, String aDataset,
                    int aNumberOfFitnessCases, int aNumberOfFeatures,
                    int aNumberOfResults, int aNumberOfThreads,int aMaxIndSize,float afpWeight,float afnWeight,int aNumLambdas) throws IOException {
        FUNC_SET = (ArrayList<String>) aFUNC_SET;
        UNARY_FUNC_SET = (ArrayList<String>) aUNARY_FUNC_SET;
        datasetPath = aDataset;
        numberOfFitnessCases = aNumberOfFitnessCases;
        numberOfFeatures = aNumberOfFeatures;
        numberOfResults = aNumberOfResults;
        currentMaxSize = 0;
        numberOfThreads = aNumberOfThreads;
        maxIndSize = aMaxIndSize;
        gcudaROCCV = new GenerateGPFunctionCVCuda(cuFile);
        fpWeight = afpWeight;
        fnweight = afnWeight;
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
            evaluatePopulationGPU(pop);
        } catch (IOException ex) {
            Logger.getLogger(GPFunctionCVCuda.class.getName()).log(Level.SEVERE, null, ex);
        }
    }        
        
    public void compileInterpreter(int numberOfIndividuals, int aMaxIndividualSize) throws IOException{
        gcudaROCCV.generateCode(numberOfIndividuals,numberOfFitnessCases,numberOfFeatures,numberOfResults,aMaxIndividualSize,fileCodedRPN,fpWeight,fnweight,numLambdas);
        gcudaROCCV.printCodeToFile(cuFile);
        gcudaROCCV.compileFile(cuFile,binCUDA);
    }
        
    public void evaluatePopulationGPU(Population pop) throws IOException{

        int numberOfIndividuals = pop.size();
        Expression[] expressions = new Expression[numberOfIndividuals];
        for(int i=0;i<numberOfIndividuals;i++){
            expressions[i] = new Expression();
            expressions[i].setOps((ArrayList<String>)FUNC_SET);
            expressions[i].setUnOps((ArrayList<String>)UNARY_FUNC_SET);
            //Tree genotype = (Tree) pop.get(i).getGenotype();
            String prefixExpression = ((Tree) pop.get(i).getGenotype()).toPrefixString();//String prefixExpression = genotype.toString();
            expressions[i].setPrefixExpression(prefixExpression);
        }

        ParseExpression pe = new ParseExpression();
        for(int i=0;i<numberOfIndividuals;i++){
            pe.setExpression(expressions[i]);
            String prefixExpression = expressions[i].getPrefixExpression();
            String infix = pe.getInfixFromPrefix(prefixExpression);
            expressions[i].setInfixExpression(infix);
            pe.getPosfixFromInfix();
            pe.getcodedRPN();
        }
        gcudaROCCV.setExpressions(expressions);
        int newMaxIndividualSize = gcudaROCCV.printCodedExpressionsToFile(fileCodedRPN);
        if (newMaxIndividualSize>maxIndSize){
            compileInterpreter(numberOfIndividuals, newMaxIndividualSize);
            System.out.println("Compiling RPN CUDA ... new Max Individual Size = " + newMaxIndividualSize);
            maxIndSize = newMaxIndividualSize;
        }

        gcudaROCCV.runCode(binCUDA,numberOfIndividuals);
        ArrayList<Float> areaROCArrayList = new ArrayList<Float>();
        ArrayList<Float> bestThresoldsArrayList = new ArrayList<Float>();
        gcudaROCCV.readResults(areaROCArrayList,bestThresoldsArrayList);

        //for (Individual i : pop) {
        for(int i=0;i<numberOfIndividuals;i++){
            Individual ind = pop.get(i);
            double areaAux = (double)areaROCArrayList.get(i);
            ind.setCrossValAreaROC(areaAux);
            double bestLambda = (double)bestThresoldsArrayList.get(i);
            ind.setThreshold(bestLambda);
        }

    }

}