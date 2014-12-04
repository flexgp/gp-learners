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
 * Implements fitness evaluation for MAP GPFunction models in Java
 * 
 * @author Ignacio Arnaldo
 */
public class GPFunctionKDEJava extends FitnessFunction{
    
    public static String FITNESS_KEY = Parameters.Operators.GPFUNCTION_KDE_JAVA;
    
    private final DataJava data;
    private int numThreads;
    public Boolean isMaximizingFunction = true;
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aData
     * @param aNumThreads
     */
    public GPFunctionKDEJava(DataJava aData, int aNumThreads) {
        this.data = aData;
        this.numThreads = aNumThreads;
    }

    private double scaleValue(double val,double min,double max) {
        double range = max - min;
        double scaledValue = (val - min) / range;
        return scaledValue;
    }

    private double getStd(ArrayList<Double> variableValues){
        double sumVariableValues = 0;
        for(int i=0;i<variableValues.size();i++) sumVariableValues += variableValues.get(i);
        double averageVarValue = sumVariableValues / variableValues.size();
        double sd = 0;
        for (int i = 0; i < variableValues.size(); i++){
            sd += Math.pow(variableValues.get(i) - averageVarValue,2) / variableValues.size();
        }
        double standardDeviation = Math.sqrt(sd);
        return standardDeviation;
    }
    
    /**
     * @param ind
     * @see Function
     */
    public void eval(Individual ind) {
        double[] targets = data.getTargetValues();
        Tree genotype = (Tree) ind.getGenotype();
        Function func = genotype.generate();
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        double[] functionOutputs = new double[data.getNumberOfFitnessCases()];
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
            functionOutputs[i] = val;
            d.clear();
        }
        
        ind.setMinTrainOutput(minPhenotype);
        ind.setMaxTrainOutput(maxPhenotype);
        
        //scale values to 0-1 range
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            double daux = functionOutputs[i];
            functionOutputs[i] = scaleValue(daux, minPhenotype, maxPhenotype);
        }
        ArrayList<Double> functionOutputsTruePos = new ArrayList<Double>();
        ArrayList<Double> functionOutputsTrueNeg = new ArrayList<Double>();
        getVariableValues(functionOutputs,functionOutputsTruePos,functionOutputsTrueNeg);
        
        int numberOfSteps = 101;
        double[] estimatedDensPos = new double[numberOfSteps];
        double stdPos = getStd(functionOutputsTruePos);
        estimateDensityKDE(functionOutputsTruePos,stdPos,estimatedDensPos,numberOfSteps);
        ind.setEstimatedDensPos(estimatedDensPos);
                
        double[] estimatedDensNeg = new double[numberOfSteps];
        double stdNeg = getStd(functionOutputsTrueNeg);
        estimateDensityKDE(functionOutputsTrueNeg,stdNeg,estimatedDensNeg,numberOfSteps);
        ind.setEstimatedDensNeg(estimatedDensNeg);
        
        // do a sweep according to moving weight to determine if densPos > densNeg
        // obtain AUC
        int numStepsROC = 10;
        double startInterval = 0;
        double endInterval = 1;
        double stepRoc = (endInterval - startInterval) / (double) numStepsROC;
        // convert each of these into a 10 values array
        // weight fp = 1 - fn
        // weight fp = [0:0.1:1]
        double[] numPositiveTarget = new double[numStepsROC+1];
        double[] numNegativeTarget = new double[numStepsROC+1];
        double[] numFalsePositives = new double[numStepsROC+1];
        double[] numTruePositives = new double[numStepsROC+1];
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            boolean target = false;
            if(targets[i]==1) target = true;
            int indexNearestValue = (int)Math.round(functionOutputs[i]*(numberOfSteps-1));
            
            for(int r=0;r<=numStepsROC;r++){
                // for loop for each of the weights
                // if r = 0 predict negative class
                // if r=stepsRoc predict always 1 
                double weightPositive = startInterval + r*stepRoc;
                double weightNegative = 1 - weightPositive;
                boolean pred;
                if(r==0){
                    pred = false;
                }else if(r==numStepsROC){
                    pred =true;
                }else if((weightPositive*estimatedDensPos[indexNearestValue]) > (weightNegative*estimatedDensNeg[indexNearestValue])){
                    pred=true;
                }else{
                    pred=false;
                }
                if(pred==true && target==true) {
                    numPositiveTarget[r]++;
                    numTruePositives[r]++;
                }else if(pred==true && target==false) {
                    numNegativeTarget[r]++;
                    numFalsePositives[r]++; 
                }else if(pred==false && target==true){
                    numPositiveTarget[r]++;
                }else if(pred==false && target==false){
                    numNegativeTarget[r]++;
                }
            }
        }
        double[] falsePositiveRate = new double[numStepsROC+1];
        double[] truePositiveRate = new double[numStepsROC+1];
        for(int r=0;r<=numStepsROC;r++){
            falsePositiveRate[r] = numFalsePositives[r] / numNegativeTarget[r];
            truePositiveRate[r] = numTruePositives[r] / numPositiveTarget[r];
        }
        //compute trapezoidal rule: \n");
        // let a and b be two points\n");
        // let f be the function we want to integrate\n");
        // area between S_a^b f(X)dx = (b-a)*[( f(a) + (f(b) ) / 2 ]\n");
        double totalArea = 0;
        for(int r=1;r<=numStepsROC;r++){
            double a = falsePositiveRate[r-1];
            double b = falsePositiveRate[r];
            double fa = truePositiveRate[r-1];
            double fb = truePositiveRate[r];
            double areaTrap = (b-a) * ((fa+fb)/(double) 2);
            totalArea += areaTrap;
        }
        ind.setFitness(GPFunctionKDEJava.FITNESS_KEY, totalArea);
        
        func = null;
    }
    
    private void getVariableValues(double[] preds, ArrayList<Double> pos, ArrayList<Double> neg){
        for (int i = 0; i < preds.length; i++) {
            double variableValue = preds[i];
            double trueLabel = (data.getTargetValues())[i];
            if(trueLabel==1){
                pos.add(variableValue);
            } else if(trueLabel==0){
                neg.add(variableValue);
            }
        }
    }    
    
    
    /* Compute Probability of new value x 
     * given the KDF
     * Fx(X) = 1/nh SUM_{j=1}^{n} K( (X-dj)/h )
     * where n = sample size
     * dj point value
     * K is a Gaussian Kernel, then h = ( (4 sigma^5) / 3n )^{1/5} = 1.06 sigma n^{-1/5}
     * and K(y) = 1/( sqrt(2PI) ) * exp(-0.5*y^2)
     *  note in this case y = (X-dj)/h
     */
    private double getDensity(ArrayList<Double> variableValues, double stdValues,double x){
        double Fx = 0;
        int n = variableValues.size();
        double sigma = stdValues;
        double h = 1.06 * sigma * Math.pow(n, -(1/5));
        for(int i=0;i<variableValues.size();i++){
            double y = (x - variableValues.get(i)) / h;
            double Ky = ( 1/Math.sqrt(2*Math.PI) ) * (Math.exp(-0.5*Math.pow(y,2)));
            Fx += Ky;
        }
        Fx = Fx / (n*h);
        return Fx;
    }
    
    private void estimateDensityKDE(ArrayList<Double> functionOutputsClass, double stdOutputsClass, double[] estimatedDF,int numberOfSteps){
        double startInterval = 0;
        double endInterval = 1;
        double interval = (endInterval - startInterval) / (double) numberOfSteps;
        double currentValue = 0;
        for(int i=0;i<numberOfSteps;i++){
            double estimatedProb = getDensity(functionOutputsClass,stdOutputsClass,currentValue);
            estimatedDF[i] = estimatedProb;
            currentValue += interval;
        }
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
                Logger.getLogger(GPFunctionKDEJava.class.getName()).log(Level.SEVERE, null, ex);
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