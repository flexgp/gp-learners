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
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Test fused symbolic regression models
 * 
 * @author Ignacio Arnaldo
 */
public class TestSRFusedModel{

    private String pathToData;
    private final DataJava data;

    private String pathToPop;
    private Population models;

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
    public TestSRFusedModel(String aPathToData, String aPathToPop,boolean aRound) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToPop = aPathToPop;
        round = aRound;
        this.data = new CSVDataJava(pathToData);
        readFusedModel(pathToPop);
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
    
    public void predictions(String filePath) throws IOException {
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + ".csv"));
        PrintWriter printWriter = new PrintWriter(bw);
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
            printWriter.println(predictedValue);
            d.clear();
        }
        printWriter.flush();
        printWriter.close();
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
            double difference = target[i] - predictedValue;
            MSE += Math.pow(difference, 2);
            MAE += Math.abs(target[i] - predictedValue);
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