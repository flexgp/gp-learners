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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * Test fused symbolic regression models
 * 
 * @author Ignacio Arnaldo
 */
public class TestSRFusedModelNorm{

    private String pathToData;
    private final DataJava data;

    private String pathToPop;
    private Population models;

        
    String pathToTrainBounds;
    double minTargetTr;
    double maxTargetTr;
    
    String pathToTestBounds;
    double minTargetTest;
    double maxTargetTest;
    
    private boolean round;

    private double[] weights;
    
    double mse;
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param data
     *            The dataset (training cases, output variable) to use in
     *            computing the fitness of individuals.
     */
    public TestSRFusedModelNorm(String aPathToData, String aPathToTrainBounds, String aPathToTestBounds,String aPathToPop,boolean aRound) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToTrainBounds = aPathToTrainBounds;
        pathToTestBounds = aPathToTestBounds;
        pathToPop = aPathToPop;
        round = aRound;
        this.data = new CSVDataJava(pathToData);
        readFusedModel(pathToPop);
        readBoundsTrain(aPathToTrainBounds);
        readBoundsTest(aPathToTestBounds);
        mse = 0;
    }

    private void readFusedModel(String filePath) throws IOException, ClassNotFoundException{
        models = new Population();
        ArrayList<String> alModels = new ArrayList<String>();
        Scanner sc = new Scanner(new FileReader(filePath));
        int indexModel =0;
        while(sc.hasNextLine()){
            String sAux = sc.nextLine();
            alModels.add(indexModel, sAux);
            indexModel++;
        }
        int popSize = alModels.size();
        weights = new double[popSize];
        for(int i=0;i<popSize;i++){
            String scaledModel = alModels.get(i);
            String model = "";
            String[] tokens = scaledModel.split(" ");
            String armCoeff = tokens[1];
            weights[i] = Double.valueOf(armCoeff);
            String sSlope = tokens[2];
             // remove parenthesis token 0 and token (last - 2)
            sSlope = sSlope.substring(1);
            tokens[tokens.length-3] = tokens[tokens.length-3].substring(0, tokens[tokens.length-3].length()-1);
            double slope = Double.parseDouble(sSlope);
            String sIntercept = tokens[tokens.length-1];
            double intercept = Double.parseDouble(sIntercept);
            for(int t=4;t<tokens.length-2;t++){
                    model += tokens[t] + " ";
            }
            Tree g = TreeGenerator.generateTree(model);
            g.setScalingSlope(slope);
            g.setScalingIntercept(intercept);
            Individual iAux = new Individual(g);
            models.add(i, iAux);
        }

    }

    private void readBoundsTrain(String boundPath) throws FileNotFoundException{
        Scanner sc = new Scanner(new FileReader(boundPath));
        for(int i=0;i<data.getNumberOfFeatures();i++){
            sc.nextLine();
        }
        String line = sc.nextLine();
        String[] values = line.split(" ");
        minTargetTr = Double.valueOf(values[0]);
        maxTargetTr = Double.valueOf(values[1]);
    }
    
    private void readBoundsTest(String boundPath) throws FileNotFoundException{
        Scanner sc = new Scanner(new FileReader(boundPath));
        for(int i=0;i<data.getNumberOfFeatures();i++){
            sc.nextLine();
        }
        String line = sc.nextLine();
        String[] values = line.split(" ");
        minTargetTest = Double.valueOf(values[0]);
        maxTargetTest = Double.valueOf(values[1]);
    }  
    
    public double unscale(Double scaled_val,double min,double max) {
        double range = max - min;
        double unscaled_val =  (scaled_val * range) + min;
        return unscaled_val;
    } 
        
    /**
     * @see Function
     */
    public ArrayList<Double> eval() {
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        double MSE = 0;
        double MAE = 0;
        double[] target = data.getTargetValues();

        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                    d.add(j, inputValuesAux[i][j]);
            }
            double predictedValue = 0;
            for(int j=0;j<models.size();j++){
                if(weights[j] >= 0.00001){
                    Individual ind = models.get(j);
                    Tree genotype = (Tree) ind.getGenotype();
                    Function func = genotype.generate();
                    Double val = func.eval(d);
                    double slope = genotype.getScalingSlope();
                    double intercept = genotype.getScalingIntercept();
                    val = (val*slope) + intercept;
                    if(round) val = (double)Math.round(val);
                    predictedValue += weights[j] * val;
                    func = null;
                }
            }
            if(round) predictedValue = Math.round(predictedValue);
            double unscaledPrediction = unscale(predictedValue,minTargetTr,maxTargetTr);
            double unscaledTarget = unscale(target[i],minTargetTest,maxTargetTest);
            MSE += Math.pow((unscaledTarget - unscaledPrediction), 2);
            MAE += Math.abs(unscaledTarget - unscaledPrediction);
            d.clear();
        }
        int numFitnessCases = data.getNumberOfFitnessCases();
        MSE = MSE / numFitnessCases;
        MAE = MAE / numFitnessCases;
        ArrayList<Double> errors = new ArrayList<Double>();
        errors.add(MSE);
        errors.add(MAE);
        return errors;
    }
}