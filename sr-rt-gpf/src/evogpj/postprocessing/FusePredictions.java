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
import java.io.IOException;


/**
 * Fuse the predictions of binary classifiers via majority vote
 * 
 * @author Ignacio Arnaldo
 */
public class FusePredictions {
    
    private String pathToCsv;
    private final DataJava preds;
    private double FN_weight, FP_weight;
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToCsv
     * @param aFN_weight
     * @throws java.io.IOException
     */   
    public FusePredictions(String aPathToCsv,double aFN_weight) throws IOException {
        pathToCsv = aPathToCsv;
        preds = new CSVDataJava(pathToCsv);
        FN_weight = aFN_weight;
        FP_weight = 1 - FN_weight;
    }

    public void computeStatsMajorityVote() {
        
        
        int numClassifiers = preds.getNumberOfFeatures();
        int numFitnessCasesTrain = preds.getNumberOfFitnessCases();
        
        // COMPUTE COST FOR ALL CLASSIFIERS ON TRAIN SET
        double[][] predsTrainMatrix = preds.getInputValues();
        double[] trueLabels = preds.getTargetValues();

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
            int countPositives = 0;
            int countNegatives = 0;
            for(int j=0;j<numClassifiers;j++){
                if(predsTrainMatrix[i][j]==1){
                    countPositives++;
                }else{
                    countNegatives++;
                }
            }
            if(countPositives>=countNegatives){
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

        double accuracy = accuratePredictions / preds.getNumberOfFitnessCases();
        double precision = numTruePositives / numPositivePrediction;
        double recall = numTruePositives / numPositiveTarget;
        double fscore = 2 * ( (precision*recall) / (precision + recall) );

        // cost , fpr, fnr, accuracy, precision, recall, F-score
        System.out.println(cost + "," + falsePositiveRate + "," + falseNegativeRate + "," + accuracy + "," + precision + "," + recall + "," + fscore);        

    }
    



}