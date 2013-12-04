/**
 * Copyright (c) 2011-2013 Evolutionary Design and Optimization Group
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
package MultiClass.main;

import evogpj.evaluation.java.DataJava;
import evogpj.genotype.Tree;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import evogpj.math.Function;
import evogpj.algorithm.Parameters;

/**
 * Implements fitness evaluation for symbolic regression.
 * 
 * @author Owen Derby
 */
public class PredictionMatrix {

    private final DataJava data;
    int[][] matrix;
    double[] phenotype;
    int[][] codeWord;
    int numPredictors;
    int[] multiClassPredictions;
    int numClasses;
    Population predictors;
    
    public PredictionMatrix(DataJava aData,int[][] aCodeWord,int aNumPredictors,int aNumClasses,Population aPopulation) {
            this.data = aData;
            codeWord = aCodeWord;
            numPredictors = aNumPredictors;
            matrix = new int[data.getNumberOfFitnessCases()][numPredictors];
            phenotype = new double[data.getNumberOfFitnessCases()];
            multiClassPredictions = new int[data.getNumberOfFitnessCases()];
            numClasses = aNumClasses;
            predictors = aPopulation;
    }

    public Double scaleValue(Double inVal,Double min, Double max) {
        double range = max - min;
        double scaledVal = (inVal - min) / range;
        return scaledVal;
    }

    public Double unScaleValue(Double inVal,Double min, Double max) {
        double range = max - min;
        double unscaled = (inVal * range) + min;
        return unscaled;
    }

    public void getBinaryPredictions(Population pop) {
        for (int i=0;i<pop.size();i++){
            Individual ind = pop.get(i);
            this.getBinaryPredictions(ind,i);
        }
    }
    
    public void getBinaryPredictions(Individual ind,int indexIndi) {
        double threshold = ind.getThreshold();
        Tree genotype = (Tree) ind.getGenotype();
        Function func = genotype.generate();
        List<Double> d;
        double minPhenotype = Double.MAX_VALUE;
        double maxPhenotype = - (Double.MAX_VALUE);
        double[][] inputValuesAux = data.getInputValues();
        
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            Double val = func.eval(d);
            if(val>maxPhenotype) maxPhenotype = val;
            if(val<minPhenotype) minPhenotype = val;
            phenotype[i] = val;
            d.clear();
        }
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            double scaled = scaleValue(phenotype[i],minPhenotype,maxPhenotype);
            if(scaled>=threshold){
                matrix[i][indexIndi] = 1;
            }else{
                matrix[i][indexIndi] = 0;
            }
            
        }
    }

    public int getIndexClassHammingDistance(int[] combinedPreds){
        int indexClass = 0;
        int minDist = Integer.MAX_VALUE;
        for(int i=0;i<numClasses;i++){
            int distAux = 0;
            for(int j=0;j<numPredictors;j++){
                if(combinedPreds[j]!= codeWord[i][j]){
                    distAux++;
                }
            }
            if(distAux<minDist){
                minDist = distAux;
                indexClass = i+1;
            }
        }
        return indexClass;
    }

    public int getIndexClassWeightedHammingDistance(int[] combinedPreds){
        int indexClass = 0;
        double minDist = Integer.MAX_VALUE;
        for(int i=0;i<numClasses;i++){
            double distAux = 0;
            for(int j=0;j<numPredictors;j++){
                //get predictor accuracy
                double areaRoc = predictors.get(j).getCrossValAreaROC();
                
                if(areaRoc>0.60){
                    if(combinedPreds[j] != codeWord[i][j]){
                        //double predAccuracy = (areaRoc-0.5) * 2;
                        distAux += areaRoc;
                        //distAux += 1;
                    }else{
                        //distAux += (1-areaRoc);
                        //distAux -= predAccuracy;
                    }
                }
            }
            if(distAux<minDist){
                minDist = distAux;
                indexClass = i+1;
            }
        }
        return indexClass;
    }  
        
    public int getIndexClassDistanceWeightedClassImportance(int[] combinedPreds){
        int indexClass = 0;
        double minDist = Integer.MAX_VALUE;
        
        /*Songs in the 1920s: 139
        Songs in the 1930s: 131
        Songs in the 1940s: 266
        Songs in the 1950s: 2147
        Songs in the 1960s: 8489
        Songs in the 1970s: 17828
        Songs in the 1980s: 29293
        Songs in the 1990s: 87074
        Songs in the 2000s: 210116
        Songs in the 2010s: 6464*/
        int[] appearances = new int[10];
        appearances[0] = 139;
        appearances[1] = 131;
        appearances[2] = 266;
        appearances[3] = 2147;
        appearances[4] = 8489;
        appearances[5] = 17828;
        appearances[6] = 29293;
        appearances[7] = 87074;
        appearances[8] = 210116;
        appearances[9] = 6464;
        int numLinesTrainingSet = 361947;
        for(int i=0;i<numClasses;i++){
            double distAux = 0;
            double weightClass = (numLinesTrainingSet - appearances[i]) / (double)numLinesTrainingSet;
            for(int j=0;j<numPredictors;j++){
                //get predictor accuracy
                double areaRoc = predictors.get(j).getCrossValAreaROC();
                
                if(areaRoc>0.60){
                    if(combinedPreds[j] != codeWord[i][j]){
                        //double predAccuracy = (areaRoc-0.5) * 2;
                        distAux += areaRoc;
                        //distAux += 1;
                    }else{
                        //distAux += (1-areaRoc);
                        //distAux -= predAccuracy;
                    }
                }
                distAux *= weightClass;
            }
            if(distAux<minDist){
                minDist = distAux;
                indexClass = i+1;
            }
        }
        return indexClass;
    }    
    
    public void computeMultiClassPredictions(){
        int[] combinedPreds = new int[numPredictors];
        for(int i=0;i<data.getNumberOfFitnessCases();i++){
            for(int j=0;j<numPredictors;j++){
                combinedPreds[j] = matrix[i][j];
            }
            //int indexClass = getIndexClassHammingDistance(combinedPreds);
            int indexClass = getIndexClassWeightedHammingDistance(combinedPreds);
            //int indexClass = getIndexClassDistanceWeightedClassImportance(combinedPreds);
            multiClassPredictions[i] = indexClass;
        }
    }
    
    private int getDecadeFromClass(int classNumber){
        int decade = 0;
        if(classNumber==1){
            decade = 1920;
        }else if (classNumber==2){
            decade = 1930;
        }else if (classNumber==3){
            decade = 1940;
        }else if (classNumber==4){
            decade = 1950;
        }else if (classNumber==5){
            decade = 1960;
        }else if (classNumber==6){
            decade = 1970;
        }else if (classNumber==7){
            decade = 1980;
        }else if (classNumber==8){
            decade = 1990;
        }else if (classNumber==9){
            decade = 2000;
        }else if (classNumber==10){
            decade = 2010;
        }
        return decade;
    }
        
    public void transformToDecades(){
        for(int i=0;i<data.getNumberOfFitnessCases();i++){
            int indexClass = multiClassPredictions[i];
            int decade = getDecadeFromClass(indexClass);
            multiClassPredictions[i] = decade;
        }
    }

    public float getAccuracy(){
        double[] trueYs = data.getTargetValues();
        int numHits = 0;
        for(int i=0;i<data.getNumberOfFitnessCases();i++){
            int targetDecade = (int)trueYs[i];
            if(multiClassPredictions[i]==targetDecade){
                numHits++;
            }
        }
        float accuracy = (float) numHits / (float) data.getNumberOfFitnessCases();
        return accuracy;
    }
    
    public void getMetrics(){
        double[] trueYs = data.getTargetValues();
        int numHits = 0;
        for(int i=0;i<data.getNumberOfFitnessCases();i++){
            int targetDecade = (int)trueYs[i];
            if(multiClassPredictions[i]==targetDecade){
                numHits++;
            }
        }
        float accuracy = (float) numHits / (float) data.getNumberOfFitnessCases();
        
        // compute precision and recall for each class
        for(int c=1;c<=numClasses;c++){
            float tp = 0;
            float fp = 0;
            float fn = 0;
            int currentDecade = getDecadeFromClass(c);
            for(int i=0;i<data.getNumberOfFitnessCases();i++){
                int trueDecade = (int)trueYs[i];
                if((trueDecade == currentDecade) && (multiClassPredictions[i] == currentDecade)){
                    tp++;
                }else if((trueDecade != currentDecade) && (multiClassPredictions[i] == currentDecade)){
                    fp++;
                }else if((trueDecade == currentDecade) && (multiClassPredictions[i] != currentDecade)){
                    fn++;
                }
            }
            float precision = tp / (tp + fp);
            float recall = tp / (tp + fn);
            float fscore = 2*((precision * recall)/(precision + recall));
            System.out.println("CLASS: " + currentDecade + " prec: " + precision + " recall: " + recall + " fscore: " + fscore);
        }
    }    
    
    
    /**
     * @return the fitnessCases
     */
    public double[][] getFitnessCases() {
        return data.getInputValues();
    }

    /**
     * @return the scaled_target
     */
    public double[] getTarget() {
        return data.getScaledTargetValues();
    }

}