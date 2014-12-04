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
package evogpj.postprocessing;

import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * Filters classifiers that perform worse than a naive classifier that always
 * predicts the majority class
 * 
 * @author Ignacio Arnaldo
 */
public class FilterPredictions {
    
    private String pathToPredsTrain, pathToPredsTest, pathToFilterTrain, pathToFilterTest,pathToIndices;
    private final DataJava predsTrain, predsTest;
    private double FN_weight, FP_weight, majorityCost;
    private int majorityClass;
    boolean[] filteredClassifiers; // a boolean flag that determines whether the classifier passes the filter (true) or is discarded (false)
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToTrain
     * @param aPathToTest
     * @param aPathToFilterTrain
     * @param aPathToFilterTest
     * @param aPathToIndices
     * @param aFN_weight
     * @throws java.io.IOException
     */   
    public FilterPredictions(String aPathToTrain, String aPathToTest, String aPathToFilterTrain, String aPathToFilterTest, String aPathToIndices, double aFN_weight) throws IOException {
        pathToPredsTrain = aPathToTrain;
        pathToPredsTest = aPathToTest;
        pathToFilterTrain = aPathToFilterTrain;
        pathToFilterTest = aPathToFilterTest;
        predsTrain = new CSVDataJava(pathToPredsTrain);
        predsTest = new CSVDataJava(pathToPredsTest);
        pathToIndices = aPathToIndices;
        int numClassifiers = predsTrain.getNumberOfFeatures();
        filteredClassifiers = new boolean[numClassifiers];
        FN_weight = aFN_weight;
        FP_weight = 1 - FN_weight;
        majorityClass = 0;
        majorityCost = 0;
    }
    
    public FilterPredictions(String aPathToTrain,String aPathToTest,double aFN_weight){
        pathToPredsTrain = aPathToTrain;
        pathToPredsTest = aPathToTest;
        FN_weight = aFN_weight;
        FP_weight = 1 - FN_weight;
        predsTrain = new CSVDataJava(pathToPredsTrain);
        predsTest = new CSVDataJava(pathToPredsTest);
        int numClassifiers = predsTrain.getNumberOfFeatures();
        filteredClassifiers = new boolean[numClassifiers];
        
    }
    
    public void setMajorityVote(){
        double[] trueLabels = predsTrain.getTargetValues();
        int counter0 = 0;
        int counter1 = 0;
        for(int i=0;i<predsTrain.getNumberOfFitnessCases();i++){
            if(trueLabels[i]==0){
                counter0++;
            }else{
                counter1++;
            }
        }
        majorityClass = 1;
        if(counter0>counter1){
            majorityClass = 0;
        }
    }
    
    public void setMajorityCost(){
        int numFitnessCasesTrain = predsTrain.getNumberOfFitnessCases();
        double[] trueLabels = predsTrain.getTargetValues();
        
        double numPositiveTarget = 0;
        double numNegativeTarget = 0;
        double numPositivePrediction = 0;
        double numNegativePrediction = 0;
        double numFalsePositives = 0;
        double numFalseNegatives = 0;
        double numTruePositives = 0;
        double numTrueNegatives = 0;
        double accuratePredictions = 0;
        boolean val = false;
        if(majorityClass==1){
                val = true;
        }
        for(int i=0;i<numFitnessCasesTrain;i++){
            boolean target = false;
            if(trueLabels[i]==1) target = true;
            if(val==true && target==true) {
                numPositivePrediction++;
                numPositiveTarget++;
                numTruePositives++;
                accuratePredictions++;
            }else if(val==true && target==false) {
                numPositivePrediction++;
                numNegativeTarget++;
                numFalsePositives++; 
            }else if(val==false && target==true){
                numNegativePrediction++;
                numPositiveTarget++;
                numFalseNegatives++;
            }else if(val==false && target==false){
                numNegativePrediction++;
                numNegativeTarget++;
                numTrueNegatives++;
                accuratePredictions++;
            }
        }
        double falsePositiveRate = numFalsePositives / numNegativeTarget;
        double falseNegativeRate = numFalseNegatives / numPositiveTarget;
        majorityCost = FN_weight*falseNegativeRate + FP_weight*falsePositiveRate;
    }
  
    public void filterModels() {
        
        int numClassifiers = predsTrain.getNumberOfFeatures();
        int numFitnessCasesTrain = predsTrain.getNumberOfFitnessCases();

        double[][] predsTrainMatrix = predsTrain.getInputValues();
        double[] trueLabels = predsTrain.getTargetValues();
        
        for(int j=0;j<numClassifiers;j++){
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numPositivePrediction = 0;
            double numNegativePrediction = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            double numTruePositives = 0;
            double numTrueNegatives = 0;
            double accuratePredictions = 0;
            for(int i=0;i<numFitnessCasesTrain;i++){
                boolean val = false;
                if(predsTrainMatrix[i][j]==1){
                    val = true;
                }
                boolean target = false;
                if(trueLabels[i]==1) target = true;
                if(val==true && target==true) {
                    numPositivePrediction++;
                    numPositiveTarget++;
                    numTruePositives++;
                    accuratePredictions++;
                }else if(val==true && target==false) {
                    numPositivePrediction++;
                    numNegativeTarget++;
                    numFalsePositives++; 
                }else if(val==false && target==true){
                    numNegativePrediction++;
                    numPositiveTarget++;
                    numFalseNegatives++;
                }else if(val==false && target==false){
                    numNegativePrediction++;
                    numNegativeTarget++;
                    numTrueNegatives++;
                    accuratePredictions++;
                }
            }
            double falsePositiveRate = numFalsePositives / numNegativeTarget;
            double falseNegativeRate = numFalseNegatives / numPositiveTarget;
            double cost = FN_weight*falseNegativeRate + FP_weight*falsePositiveRate;
            if(cost<majorityCost){
                filteredClassifiers[j] = true;
            }else{
                filteredClassifiers[j] = false;
            }
        }
    }

    public void computeCostAll() {
        
        
        int numClassifiers = predsTrain.getNumberOfFeatures();
        int numFitnessCasesTrain = predsTrain.getNumberOfFitnessCases();
        // | cost on train | cost on test | fp train | fp test | fn train | fn test
        double[][] costs = new double[numClassifiers][6];
        
        // COMPUTE COST FOR ALL CLASSIFIERS ON TRAIN SET
        double[][] predsTrainMatrix = predsTrain.getInputValues();
        double[] trueLabels = predsTrain.getTargetValues();
        for(int j=0;j<numClassifiers;j++){
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            for(int i=0;i<numFitnessCasesTrain;i++){
                boolean val = false;
                if(predsTrainMatrix[i][j]==1){
                    val = true;
                }
                boolean target = false;
                if(trueLabels[i]==1) target = true;
                if(val==true && target==true) {
                    numPositiveTarget++;
                }else if(val==true && target==false) {
                    numNegativeTarget++;
                    numFalsePositives++; 
                }else if(val==false && target==true){
                    numPositiveTarget++;
                    numFalseNegatives++;
                }else if(val==false && target==false){
                    numNegativeTarget++;
                }
            }
            double falsePositiveRate = numFalsePositives / numNegativeTarget;
            double falseNegativeRate = numFalseNegatives / numPositiveTarget;
            double cost = FN_weight*falseNegativeRate + FP_weight*falsePositiveRate;
            costs[j][0] = cost;
            costs[j][2] = falsePositiveRate;
            costs[j][4] = falseNegativeRate;
        }
        
        // COMPUTE COST FOR ALL CLASSIFIERS ON TEST SET
        double[][] predsTestMatrix = predsTest.getInputValues();
        double[] trueTest = predsTest.getTargetValues();
        int numFitnessCasesTest = predsTest.getNumberOfFitnessCases();
        for(int j=0;j<numClassifiers;j++){
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            for(int i=0;i<numFitnessCasesTest;i++){
                boolean val = false;
                if(predsTestMatrix[i][j]==1){
                    val = true;
                }
                boolean target = false;
                if(trueTest[i]==1) target = true;
                if(val==true && target==true) {
                    numPositiveTarget++;
                }else if(val==true && target==false) {
                    numNegativeTarget++;
                    numFalsePositives++; 
                }else if(val==false && target==true){
                    numPositiveTarget++;
                    numFalseNegatives++;
                }else if(val==false && target==false){
                    numNegativeTarget++;
                }
            }
            double falsePositiveRate = numFalsePositives / numNegativeTarget;
            double falseNegativeRate = numFalseNegatives / numPositiveTarget;
            double cost = FN_weight*falseNegativeRate + FP_weight*falsePositiveRate;
            costs[j][1] = cost;
            costs[j][3] = falsePositiveRate;
            costs[j][5] = falseNegativeRate;
        }
        
        for(int j=0;j<numClassifiers;j++){
            System.out.println(costs[j][0] + "," + costs[j][1] + "," + costs[j][2] + "," + costs[j][3] + "," + costs[j][4] + "," + costs[j][5]);
        }
    }
    
    public void computeCostFiltered() {
        setMajorityVote();
        setMajorityCost();
        filterModels();
        int numClassifiers = predsTrain.getNumberOfFeatures();
        int numFitnessCasesTrain = predsTrain.getNumberOfFitnessCases();
        // | cost on train | cost on test | fp train | fp test | fn train | fn test
        double[][] costs = new double[numClassifiers][6];
        
        // COMPUTE COST FOR ALL CLASSIFIERS ON TRAIN SET
        double[][] predsTrainMatrix = predsTrain.getInputValues();
        double[] trueLabels = predsTrain.getTargetValues();
        for(int j=0;j<numClassifiers;j++){
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numPositivePrediction = 0;
            double numNegativePrediction = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            double numTruePositives = 0;
            double numTrueNegatives = 0;
            double accuratePredictions = 0;
            for(int i=0;i<numFitnessCasesTrain;i++){
                boolean val = false;
                if(predsTrainMatrix[i][j]==1){
                    val = true;
                }
                boolean target = false;
                if(trueLabels[i]==1) target = true;
                if(val==true && target==true) {
                    numPositivePrediction++;
                    numPositiveTarget++;
                    numTruePositives++;
                    accuratePredictions++;
                }else if(val==true && target==false) {
                    numPositivePrediction++;
                    numNegativeTarget++;
                    numFalsePositives++; 
                }else if(val==false && target==true){
                    numNegativePrediction++;
                    numPositiveTarget++;
                    numFalseNegatives++;
                }else if(val==false && target==false){
                    numNegativePrediction++;
                    numNegativeTarget++;
                    numTrueNegatives++;
                    accuratePredictions++;
                }
            }
            double falsePositiveRate = numFalsePositives / numNegativeTarget;
            double falseNegativeRate = numFalseNegatives / numPositiveTarget;
            double cost = FN_weight*falseNegativeRate + FP_weight*falsePositiveRate;
            costs[j][0] = cost;
            costs[j][2] = falsePositiveRate;
            costs[j][4] = falseNegativeRate;
        }
        
        // COMPUTE COST FOR ALL CLASSIFIERS ON TEST SET
        int numFitnessCasesTest = predsTest.getNumberOfFitnessCases();
        double[][] predsTestMatrix = predsTest.getInputValues();
        double[] trueTest = predsTest.getTargetValues();
        for(int j=0;j<numClassifiers;j++){
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            for(int i=0;i<numFitnessCasesTest;i++){
                boolean val = false;
                if(predsTestMatrix[i][j]==1){
                    val = true;
                }
                boolean target = false;
                if(trueTest[i]==1) target = true;
                if(val==true && target==true) {
                    numPositiveTarget++;
                }else if(val==true && target==false) {
                    numNegativeTarget++;
                    numFalsePositives++; 
                }else if(val==false && target==true){
                    numPositiveTarget++;
                    numFalseNegatives++;
                }else if(val==false && target==false){
                    numNegativeTarget++;
                }
            }
            double falsePositiveRate = numFalsePositives / numNegativeTarget;
            double falseNegativeRate = numFalseNegatives / numPositiveTarget;
            double cost = FN_weight*falseNegativeRate + FP_weight*falsePositiveRate;
            costs[j][1] = cost;
            costs[j][3] = falsePositiveRate;
            costs[j][5] = falseNegativeRate;
        }
        
        for(int j=0;j<numClassifiers;j++){
            if(filteredClassifiers[j]==true){
                System.out.println(costs[j][0] + "," + costs[j][1] + "," + costs[j][2] + "," + costs[j][3] + "," + costs[j][4] + "," + costs[j][5]);
            }
        }
    }
        
        
    /**
     * Save text to a filepath
     * @param filepath
     * @param text
     */
    public void saveFilteredModels() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(pathToFilterTrain));
            PrintWriter printWriter = new PrintWriter(bw);
            double[][] predsTrainMatrix = predsTrain.getInputValues();
            for(int i=0;i<predsTrain.getNumberOfFitnessCases();i++){
                for(int j=0;j<predsTrain.getNumberOfFeatures();j++){
                    if(filteredClassifiers[j]==true){
                        printWriter.write((int)predsTrainMatrix[i][j] + ",");
                    }
                }
                printWriter.write((int)predsTrain.getTargetValues()[i] + "\n");
            }
            printWriter.flush();
            printWriter.close();
            
            BufferedWriter bwTest = new BufferedWriter(new FileWriter(pathToFilterTest));
            PrintWriter printWriterTest = new PrintWriter(bwTest);
            for(int i=0;i<predsTest.getNumberOfFitnessCases();i++){
                for(int j=0;j<predsTest.getNumberOfFeatures();j++){
                    if(filteredClassifiers[j]==true){
                        printWriterTest.write((int)predsTrainMatrix[i][j] + ",");
                    }
                }
                printWriterTest.write((int)predsTest.getTargetValues()[i] + "\n");
            }
            printWriterTest.flush();
            printWriterTest.close();
            
            BufferedWriter bwIndices = new BufferedWriter(new FileWriter(pathToIndices));
            PrintWriter printWriterIndices = new PrintWriter(bwIndices);
            for(int j=0;j<predsTest.getNumberOfFeatures();j++){
                if(filteredClassifiers[j]==true){
                    printWriterIndices.write(j + "\n");
                }
            }
            printWriterIndices.flush();
            printWriterIndices.close();
        } catch (IOException e) {
            System.exit(-1);
        }
    }

}