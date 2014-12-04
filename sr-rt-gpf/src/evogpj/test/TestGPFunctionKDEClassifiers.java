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
 */
package evogpj.test;

import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import evogpj.genotype.Tree;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.util.ArrayList;
import java.util.List;
import evogpj.math.Function;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Test MAP GPFunction classifiers
 * 
 * @author Ignacio Arnaldo
 */
public class TestGPFunctionKDEClassifiers {
            
    private String pathToTestData;
    private final DataJava dataTest;
    
    private String pathToPop;
    private Population models;

    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToTestData
     * @param aPathToPop
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public TestGPFunctionKDEClassifiers(String aPathToTestData,String aPathToPop) throws IOException, ClassNotFoundException {
        pathToTestData = aPathToTestData;
        pathToPop = aPathToPop;
        dataTest = new CSVDataJava(pathToTestData);
        readGPFunctionClassifiers(pathToPop);
    }

    
    private void readGPFunctionClassifiers(String filePath) throws IOException, ClassNotFoundException{
        models = new Population();
        Scanner sc = new Scanner(new FileReader(filePath));
        while(sc.hasNextLine()){
            String line = sc.nextLine();
            String[] tokens = line.split(",");
            String model = tokens[0];
            Tree g = TreeGenerator.generateTree(model);
            Individual iAux = new Individual(g);
            String minOutputS = tokens[2];
            double minOutput = Double.parseDouble(minOutputS);
            iAux.setMinTrainOutput(minOutput);
            String maxOutputS = tokens[3];
            double maxOutput = Double.parseDouble(maxOutputS);
            iAux.setMaxTrainOutput(maxOutput);
            
            String[] estimatedDensNegS = tokens[4].split(" ");
            double[] estimatedDensNeg = new double[estimatedDensNegS.length];
            for(int i=0;i<estimatedDensNegS.length;i++){
                estimatedDensNeg[i] = Double.valueOf(estimatedDensNegS[i]);
            }
            iAux.setEstimatedDensNeg(estimatedDensNeg);
            
            String[] estimatedDensPosS = tokens[5].split(" ");
            double[] estimatedDensPos = new double[estimatedDensPosS.length];
            for(int i=0;i<estimatedDensPosS.length;i++){
                estimatedDensPos[i] = Double.valueOf(estimatedDensPosS[i]);
            }
            iAux.setEstimatedDensPos(estimatedDensPos);
            models.add(iAux);
        }
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
    
     /**
     * @param filePath
     * @throws java.io.IOException
     * @see Function
     */
    public void predictionsPop(String filePath) throws IOException {
        int indexIndi = 0;
        for(Individual ind:models){
            double minTrainOutput = ind.getMinTrainOutput();
            double maxTrainOutput = ind.getMaxTrainOutput();
            // estimate value of the two estimated density functions with KDE method
            // we obtian a density value for each point in the test set
            int numberOfSteps = ind.getEstimatedDensNeg().size();
            ArrayList<Double> alNeg = ind.getEstimatedDensNeg();
            double[] estimatedDensNeg = new double[numberOfSteps];
            for(int i=0;i<numberOfSteps;i++){
                estimatedDensNeg[i] = alNeg.get(i);
            }
            
            ArrayList<Double> alPos = ind.getEstimatedDensPos();
            double[] estimatedDensPos = new double[numberOfSteps];
            for(int i=0;i<numberOfSteps;i++){
                estimatedDensPos[i] = alPos.get(i);
            }
            
            // get outputs on test set
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + "_" + indexIndi + ".csv"));
            PrintWriter printWriter = new PrintWriter(bw);
            double[][] inputValuesTest = dataTest.getInputValues();
            double[] functionOutputsTest = new double[dataTest.getNumberOfFitnessCases()];
            List<Double> d;
            Tree genotype = (Tree) ind.getGenotype();
            Function func = genotype.generate();
            for (int i = 0; i < dataTest.getNumberOfFitnessCases(); i++) {
                d = new ArrayList<Double>();
                for (int j = 0; j < dataTest.getNumberOfFeatures(); j++) {
                    d.add(j, inputValuesTest[i][j]);
                }
                Double val = func.eval(d);
                //scale values to 0-1 range according to training set boundaries
                if(val<minTrainOutput) val = minTrainOutput;
                if(val>maxTrainOutput) val = maxTrainOutput;
                functionOutputsTest[i] = scaleValue(val, minTrainOutput, maxTrainOutput);
                int indexNearestValue = (int)Math.round(functionOutputsTest[i]*(numberOfSteps-1));
                // for loop for each of the weights
                boolean pred=false;
                if(estimatedDensPos[indexNearestValue]>estimatedDensNeg[indexNearestValue]){
                    pred=true;
                }
                if(pred==true){
                    printWriter.println(1);
                }else{
                    printWriter.println(0);
                }
                d.clear();
            }
            printWriter.flush();
            printWriter.close();
            func = null;
            indexIndi++;
        }
    }
    
    /**
     * @see Function
     */
    public void evalPop() {
        double[] targetsTest = dataTest.getTargetValues();
        for(Individual ind:models){
            double minTrainOutput = ind.getMinTrainOutput();
            double maxTrainOutput = ind.getMaxTrainOutput();
            // estimate value of the two estimated density functions with KDE method
            // we obtian a density value for each point in the test set
            int numberOfSteps = ind.getEstimatedDensNeg().size();
            ArrayList<Double> alNeg = ind.getEstimatedDensNeg();
            double[] estimatedDensNeg = new double[numberOfSteps];
            for(int i=0;i<numberOfSteps;i++){
                estimatedDensNeg[i] = alNeg.get(i);
            }
            
            ArrayList<Double> alPos = ind.getEstimatedDensPos();
            double[] estimatedDensPos = new double[numberOfSteps];
            for(int i=0;i<numberOfSteps;i++){
                estimatedDensPos[i] = alPos.get(i);
            }
            
            Tree genotype = (Tree) ind.getGenotype();
            Function func = genotype.generate();
            List<Double> d;
            

            // get outputs on test set
            double[][] inputValuesTest = dataTest.getInputValues();
            double[] functionOutputsTest = new double[dataTest.getNumberOfFitnessCases()];
            for (int i = 0; i < dataTest.getNumberOfFitnessCases(); i++) {
                d = new ArrayList<Double>();
                for (int j = 0; j < dataTest.getNumberOfFeatures(); j++) {
                    d.add(j, inputValuesTest[i][j]);
                }
                Double val = func.eval(d);
                //scale values to 0-1 range according to training set boundaries
                if(val<minTrainOutput) val = minTrainOutput;
                if(val>maxTrainOutput) val = maxTrainOutput;
                functionOutputsTest[i] = scaleValue(val, minTrainOutput, maxTrainOutput);
                d.clear();
            }
            
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numPositivePrediction = 0;
            double numNegativePrediction = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            double numTruePositives = 0;
            double numTrueNegatives = 0;
            double accuratePredictions = 0;
            
            for (int i = 0; i < dataTest.getNumberOfFitnessCases(); i++) {
                boolean target = false;
                if(targetsTest[i]==1) target = true;
    
                int indexNearestValue = (int)Math.round(functionOutputsTest[i]*(numberOfSteps-1));
                // for loop for each of the weights
                boolean pred=false;
                if(estimatedDensPos[indexNearestValue]>estimatedDensNeg[indexNearestValue]){
                    pred=true;
                }
                if(pred==true && target==true) {
                    numPositivePrediction++;
                    numPositiveTarget++;
                    numTruePositives++;
                    accuratePredictions++;
                }else if(pred==true && target==false) {
                    numPositivePrediction++;
                    numNegativeTarget++;
                    numFalsePositives++; 
                }else if(pred==false && target==true){
                    numNegativePrediction++;
                    numPositiveTarget++;
                    numFalseNegatives++;
                }else if(pred==false && target==false){
                    numNegativePrediction++;
                    numNegativeTarget++;
                    numTrueNegatives++;
                    accuratePredictions++;
                }
            }
            
            double falsePositiveRate = numFalsePositives / numNegativeTarget;
            double falseNegativeRate = numFalseNegatives / numPositiveTarget;
            double accuracy = accuratePredictions / dataTest.getNumberOfFitnessCases();
            double precision = numTruePositives / numPositivePrediction;
            double recall = numTruePositives / numPositiveTarget;
            double fscore = 2 * ( (precision*recall) / (precision + recall) );
            System.out.println();
            System.out.println("GP FUNCTION KDE: " + ind.getGenotype().toString());
            System.out.println("ACCURACY: " + accuracy);
            System.out.println("PRECISION: " + precision);
            System.out.println("RECALL: " + recall);
            System.out.println("F-SCORE: " + fscore);
            System.out.println("FALSE POSITIVE RATE: " + falsePositiveRate);
            System.out.println("FALSE NEGATIVE RATE: " + falseNegativeRate);
            System.out.println();
            func = null;
        }
    }

}