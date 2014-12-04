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
 * Implements fitness evaluation for symbolic regression in CUDA
 * 
 * @author Ignacio Arnaldo
 */
public class GPFunctionCuda extends FitnessFunction {

    private final boolean USE_INT;

    public static String FITNESS_KEY = Parameters.Operators.GPFUNCTION_CUDA;

    public Boolean isMaximizingFunction = true;

    public Boolean discreteFitness = false;

    ArrayList<String> FUNC_SET, UNARY_FUNC_SET;
    String datasetPath;
    int numberOfFitnessCases, numberOfFeatures, numberOfResults,currentMaxSize, pow, numberOfThreads,maxIndSize,numberOfIndi;
    GenerateGPFunctionCuda gcudaROC;
    String binCUDA = "tempFiles/SRRocCuda";
    String fileCodedRPN = "tempFiles/individualsCodedRPN.txt";
    String cuFile = "tempFiles/SRRocCuda.cu";
    boolean firstCompiling;
        
    public GPFunctionCuda(List<String> aFUNC_SET,
                    List<String> aUNARY_FUNC_SET, String aDataset,
                    int aNumberOfFitnessCases, int aNumberOfFeatures,
                    int aNumberOfResults, int aNumberOfThreads, int aPow,
                    boolean useInts,int aMaxIndSize) throws IOException {
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
        maxIndSize = aMaxIndSize;
        gcudaROC = new GenerateGPFunctionCuda(cuFile);
        firstCompiling = true;
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
            Logger.getLogger(GPFunctionCuda.class.getName()).log(Level.SEVERE, null, ex);
        }
    }        
        
    public void compileInterpreter(int numberOfIndividuals, int aMaxIndividualSize) throws IOException{
        gcudaROC.generateCode(numberOfIndividuals,numberOfFitnessCases,numberOfFeatures,numberOfResults,aMaxIndividualSize,fileCodedRPN,USE_INT,pow);
        gcudaROC.printCodeToFile(cuFile);
        gcudaROC.compileFile(cuFile,binCUDA);
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
            gcudaROC.setExpressions(expressions);
            int newMaxIndividualSize = gcudaROC.printCodedExpressionsToFile(fileCodedRPN);
            if (firstCompiling){
                compileInterpreter(numberOfIndividuals, Math.max(maxIndSize,newMaxIndividualSize));
                System.out.println("Compiling RPN CUDA ... new Max Individual Size = " + Math.max(maxIndSize,newMaxIndividualSize));
                maxIndSize = Math.max(maxIndSize,newMaxIndividualSize);
                firstCompiling = false;
                numberOfIndi = numberOfIndividuals;
            }else if (newMaxIndividualSize>maxIndSize){
                compileInterpreter(numberOfIndividuals, newMaxIndividualSize);
                System.out.println("Compiling RPN CUDA ... new Max Individual Size = " + newMaxIndividualSize);
                maxIndSize = newMaxIndividualSize*2;
                numberOfIndi = numberOfIndividuals;
            }else if(numberOfIndividuals!=numberOfIndi){
                compileInterpreter(numberOfIndividuals, newMaxIndividualSize);
                System.out.println("Compiling RPN CUDA ... new Max Individual Size = " + newMaxIndividualSize);
                numberOfIndi = numberOfIndividuals;
            }
            
            gcudaROC.runCode(binCUDA,numberOfIndividuals);

            ArrayList<Float> fitnessArrayList = new ArrayList<Float>();
            ArrayList<Float> minOutputArrayList = new ArrayList<Float>();
            ArrayList<Float> maxOutputArrayList = new ArrayList<Float>();
            gcudaROC.readResults(fitnessArrayList,minOutputArrayList,maxOutputArrayList);
            
            for (int i = 0; i < numberOfIndividuals; i++) {
                Individual ind = pop.get(i);
                double fitnessAux = (double) fitnessArrayList.get(i);
                ind.setFitness(GPFunctionCuda.FITNESS_KEY, fitnessAux);
                double minTrainOutput = (double) minOutputArrayList.get(i);
                ind.setMinTrainOutput(minTrainOutput);
                double maxTrainOutput = (double) maxOutputArrayList.get(i);
                ind.setMaxTrainOutput(maxTrainOutput);
            }            

        }

}