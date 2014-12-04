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
package evogpj.evaluation.java;
import evogpj.algorithm.Parameters;
import evogpj.evaluation.FitnessFunction;
import evogpj.genotype.Tree;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.util.ArrayList;
import java.util.List;
import evogpj.math.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements fitness evaluation for GPFunction models in Java
 * 
 * @author Ignacio Arnaldo
 */
public class GPFunctionJava extends FitnessFunction{
    
    public static String FITNESS_KEY = Parameters.Operators.GPFUNCTION_JAVA;
    
    private final DataJava data;
    private int numThreads;
    public Boolean isMaximizingFunction = true;
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aData
     */
    public GPFunctionJava(DataJava aData, int aNumThreads) {
        this.data = aData;
        this.numThreads = aNumThreads;
    }


    
    private double scaleValue(double val,double min,double max) {
        double range = max - min;
        double scaledValue = (val - min) / range;
        return scaledValue;
    }

        /**
     * @see Function
     */
    public void eval(Individual ind) {
        double[] targets = data.getTargetValues();
        Tree genotype = (Tree) ind.getGenotype();
        Function func = genotype.generate();
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        double[] predictions = new double[data.getNumberOfFitnessCases()];
        double maxPhenotype = -Double.MAX_VALUE;
        double minPhenotype = Double.MAX_VALUE;
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            Double val = func.eval(d);
            if(val>maxPhenotype) maxPhenotype = val;
            if(val<minPhenotype) minPhenotype = val;
            predictions[i] = val;
            d.clear();
        }
        ind.setMinTrainOutput(minPhenotype);
        ind.setMaxTrainOutput(maxPhenotype);
        //scale values to 0-1 range
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            double daux = predictions[i];
            predictions[i] = scaleValue(daux, minPhenotype, maxPhenotype);
        }
        
        // compute FP and TP for different threshold values
        int numberOfLambdas = 10;
        double startInterval = 0;
        double endInterval = 1;
        double interval = (endInterval - startInterval) / (double) numberOfLambdas;
        double[] falsePositives = new double[numberOfLambdas+1];
        double[] truePositives = new double[numberOfLambdas+1];
        for(int l=0;l<=numberOfLambdas;l++){
            double threshold = endInterval - l*interval;
            double numFalsePositives = 0;
            double numTruePositives = 0;
            double totalPositives = 0;
            double totalNegatives = 0;
            for(int i=0;i<data.getNumberOfFitnessCases();i++){
                double target = targets[i];
                double prediction = 0;
                if(threshold==0){
                    prediction = endInterval;
                }else if(threshold==1){
                    prediction = startInterval;
                }else if((threshold>0)&&(threshold<1)){
                    if(predictions[i] >= threshold){
                        prediction = endInterval;
                    }else{
                        prediction = startInterval;
                    }
                }
                if(target==1){
                    totalPositives++;
                }else if(target==0){
                    totalNegatives++;
                }
                if((prediction == 1) && (target == 0)){
                    numFalsePositives++;
                } else if((prediction == 1) && (target == 1)){
                    numTruePositives++;
                }
            }
            double fpRatio = numFalsePositives/ (double) totalNegatives;
            double tpRatio = numTruePositives / (double) totalPositives;
            falsePositives[l] = fpRatio;
            truePositives[l] = tpRatio;
        }
        
        //compute trapezoidal rule: \n");
        // let a and b be two points\n");
        // let f be the function we want to integrate\n");
        // area between S_a^b f(X)dx = (b-a)*[( f(a) + (f(b) ) / 2 ]\n");
        double totalArea = 0;
        for(int l=1;l<=numberOfLambdas;l++){
            double a = falsePositives[l-1];
            double b = falsePositives[l];
            double fa = truePositives[l-1];
            double fb = truePositives[l];
            double areaTrap = (b-a) * ((fa+fb)/(double) 2);
            totalArea += areaTrap;
        }
        ind.setFitness(GPFunctionJava.FITNESS_KEY, totalArea);
        func = null;
    }
    
    @Override
    public void evalPop(Population pop) {
        
        ArrayList<GPFunctionJavaThread> alThreads = new ArrayList<GPFunctionJavaThread>();
        for(int i=0;i<numThreads;i++){
            GPFunctionJavaThread threadAux = new GPFunctionJavaThread(i, pop,numThreads);
            alThreads.add(threadAux);
        }
        
        for(int i=0;i<numThreads;i++){
            GPFunctionJavaThread threadAux = alThreads.get(i);
            threadAux.start();
        }
        
        for(int i=0;i<numThreads;i++){
            GPFunctionJavaThread threadAux = alThreads.get(i);
            try {
                threadAux.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(GPFunctionJava.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }


    @Override
    public Boolean isMaximizingFunction() {
        return this.isMaximizingFunction;
    }



    public class GPFunctionJavaThread extends Thread{
        private int indexThread, totalThreads;
        private Population pop;
        
        public GPFunctionJavaThread(int anIndex, Population aPop,int aTotalThreads){
            indexThread = anIndex;
            pop = aPop;
            totalThreads = aTotalThreads;
        }

        @Override
        public void run(){
            int indexIndi = 0;
            for (Individual individual : pop) {
                if(indexIndi%totalThreads==indexThread){
                    eval(individual);
                }
                indexIndi++;
            }
        }
     }

}