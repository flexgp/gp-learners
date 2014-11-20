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
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * Implements fitness evaluation for symbolic regression.
 * 
 * @author Ignacio Arnaldo
 */
public class TestGPFunctionClassifiers {
    
    private String pathToData;
    private final DataJava data;
        
    private String pathToPop;
    private Population models;
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param data
     *            The dataset (training cases, output variable) to use in
     *            computing the fitness of individuals.
     */
    public TestGPFunctionClassifiers(String aPathToData, String aPathToPop) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToPop = aPathToPop;
        this.data = new CSVDataJava(pathToData);
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
            models.add(iAux);
        }
    }
    
    private double scaleValue(double val,double min,double max) {
        double range = max - min;
        double scaledValue = (val - min) / range;
        return scaledValue;
    }
    
    
    /**
     * @see Function
     */
    public void evalPop() {
        double[] targets = data.getTargetValues();
        for(Individual ind:models){
            double threshold = ind.getThreshold();
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
                
            double numPositiveTarget = 0;
            double numNegativeTarget = 0;
            double numPositivePrediction = 0;
            double numNegativePrediction = 0;
            double numFalsePositives = 0;
            double numFalseNegatives = 0;
            double numTruePositives = 0;
            double numTrueNegatives = 0;
            double accuratePredictions = 0;
            
            for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
                boolean target = false;
                if(targets[i]==1) target = true;
                double scaled = scaleValue(predictions[i],minPhenotype,maxPhenotype);
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
            double accuracy = accuratePredictions / data.getNumberOfFitnessCases();
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