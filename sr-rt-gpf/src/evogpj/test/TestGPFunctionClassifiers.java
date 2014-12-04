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
 * Test GPFunction classifiers
 * 
 * @author Ignacio Arnaldo
 */
public class TestGPFunctionClassifiers {
    
    private String pathToTestData;
    private final DataJava testData;
        
    private String pathToPop;
    private Population models;
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToTrainData
     * @param aPathToTestData
     * @param aPathToPop
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public TestGPFunctionClassifiers(String aPathToTestData, String aPathToPop) throws IOException, ClassNotFoundException {
        pathToTestData = aPathToTestData;
        pathToPop = aPathToPop;
        testData = new CSVDataJava(pathToTestData);
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
            double theshold = Double.valueOf(tokens[3]);
            iAux.setThreshold(theshold);
            double minTrainOutput = Double.valueOf(tokens[4]);
            iAux.setMinTrainOutput(minTrainOutput);
            double maxTrainOutput = Double.valueOf(tokens[5]);
            iAux.setMaxTrainOutput(maxTrainOutput);
            models.add(iAux);
        }
    }
    
    private double scaleValue(double val,double min,double max) {
        double range = max - min;
        double scaledValue = (val - min) / range;
        return scaledValue;
    }
    
    
     /**
     * @param filePath
     * @throws java.io.IOException
     * @see Function
     */
    public void predictionsSingleModel(String filePath) throws IOException {
        Individual ind = models.get(0);
        Tree genotype = (Tree) ind.getGenotype();
        Function func = genotype.generate();
        // SET MIN AND MAX WITH TRAINING SET
        double maxTrainOutput = ind.getMaxTrainOutput();
        double minTrainOutput = ind.getMinTrainOutput();

        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
        PrintWriter printWriter = new PrintWriter(bw);
        double threshold = ind.getThreshold();
        double[][] inputValuesTest = testData.getInputValues();
        double[] predictions = new double[testData.getNumberOfFitnessCases()];
        List<Double> d;
        for (int i = 0; i < testData.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < testData.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesTest[i][j]);
            }
            Double val = func.eval(d);
            if(val>maxTrainOutput) val = maxTrainOutput;
            if(val<minTrainOutput) val = minTrainOutput;
            predictions[i] = val;
            d.clear();
            double scaled = scaleValue(predictions[i],minTrainOutput,maxTrainOutput);
            boolean pred=false;
            if(scaled>=threshold){
                pred = true;
            }
            if(pred==true){
                printWriter.println(1);
            }else{
                printWriter.println(0);
            }
        }
        printWriter.flush();
        printWriter.close();
        func = null;
    }
    
         /**
     * @param filePath
     * @throws java.io.IOException
     * @see Function
     */
    public void outputSingleModel(String filePath) throws IOException {
        Individual ind = models.get(0);
        Tree genotype = (Tree) ind.getGenotype();
        Function func = genotype.generate();
        // SET MIN AND MAX WITH TRAINING SET
        double maxTrainOutput = ind.getMaxTrainOutput();
        double minTrainOutput = ind.getMinTrainOutput();

        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
        PrintWriter printWriter = new PrintWriter(bw);
        double threshold = ind.getThreshold();
        double[][] inputValuesTest = testData.getInputValues();
        double[] predictions = new double[testData.getNumberOfFitnessCases()];
        List<Double> d;
        for (int i = 0; i < testData.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < testData.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesTest[i][j]);
            }
            Double val = func.eval(d);
            if(val>maxTrainOutput) val = maxTrainOutput;
            if(val<minTrainOutput) val = minTrainOutput;
            predictions[i] = val;
            d.clear();
            double scaled = scaleValue(predictions[i],minTrainOutput,maxTrainOutput);
            printWriter.println(scaled);
        }
        printWriter.flush();
        printWriter.close();
        func = null;
    }
    
     /**
     * @param filePath
     * @throws java.io.IOException
     * @see Function
     */
    public void predictionsPop(String filePath) throws IOException {
        int indexIndi = 0;
        for(Individual ind:models){
            Tree genotype = (Tree) ind.getGenotype();
            Function func = genotype.generate();
            // SET MIN AND MAX WITH TRAINING SET
            double maxTrainOutput = ind.getMaxTrainOutput();
            double minTrainOutput = ind.getMinTrainOutput();
            
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + "_" + indexIndi + ".csv"));
            PrintWriter printWriter = new PrintWriter(bw);
            double threshold = ind.getThreshold();
            double[][] inputValuesTest = testData.getInputValues();
            double[] predictions = new double[testData.getNumberOfFitnessCases()];
            List<Double> d;
            for (int i = 0; i < testData.getNumberOfFitnessCases(); i++) {
                d = new ArrayList<Double>();
                for (int j = 0; j < testData.getNumberOfFeatures(); j++) {
                    d.add(j, inputValuesTest[i][j]);
                }
                Double val = func.eval(d);
                if(val>maxTrainOutput) val = maxTrainOutput;
                if(val<minTrainOutput) val = minTrainOutput;
                predictions[i] = val;
                d.clear();
                double scaled = scaleValue(predictions[i],minTrainOutput,maxTrainOutput);
                boolean pred=false;
                if(scaled>=threshold){
                    pred = true;
                }
                if(pred==true){
                    printWriter.println(1);
                }else{
                    printWriter.println(0);
                }
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
        
        for(Individual ind:models){
            Tree genotype = (Tree) ind.getGenotype();
            Function func = genotype.generate();
            // SET MIN AND MAX WITH TRAINING SET
            double maxTrainOutput = ind.getMaxTrainOutput();
            double minTrainOutput = ind.getMinTrainOutput();
            
            // check predictions on test set
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numPositivePrediction = 0;
            double numNegativePrediction = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            double numTruePositives = 0;
            double numTrueNegatives = 0;
            double accuratePredictions = 0;
            
            double threshold = ind.getThreshold();
            double[] targetsTest = testData.getTargetValues();
            double[][] inputValuesTest = testData.getInputValues();
            double[] predictions = new double[testData.getNumberOfFitnessCases()];
            List<Double> d;
            for (int i = 0; i < testData.getNumberOfFitnessCases(); i++) {
                d = new ArrayList<Double>();
                for (int j = 0; j < testData.getNumberOfFeatures(); j++) {
                    d.add(j, inputValuesTest[i][j]);
                }
                Double val = func.eval(d);
                if(val>maxTrainOutput) val = maxTrainOutput;
                if(val<minTrainOutput) val = minTrainOutput;
                predictions[i] = val;
                d.clear();
                boolean target = false;
                if(targetsTest[i]==1) target = true;
                double scaled = scaleValue(predictions[i],minTrainOutput,maxTrainOutput);
                boolean pred=false;
                if(scaled>=threshold){
                    pred = true;
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
            double accuracy = accuratePredictions / testData.getNumberOfFitnessCases();
            double precision = numTruePositives / numPositivePrediction;
            double recall = numTruePositives / numPositiveTarget;
            double fscore = 2 * ( (precision*recall) / (precision + recall) );
            System.out.println();
            System.out.println("GP FUNCTION: " + ind.getGenotype().toString());
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