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

import evogpj.bool.BooleanFunction;
import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import evogpj.genotype.Tree;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.util.ArrayList;
import java.util.List;

import evogpj.preprocessing.Interval;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * Implements fitness evaluation for symbolic regression.
 * 
 * @author Ignacio Arnaldo
 */
public class TestRuleTrees {
    
    private String pathToData;
    private final DataJava data;
        
    private String pathToPop;
    private Population models;
    
    private String pathToConditions;
    
    ArrayList<Interval> intervals;
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param data
     *            The dataset (training cases, output variable) to use in
     *            computing the fitness of individuals.
     */
    public TestRuleTrees(String aPathToData, String aPathToConditions,String aPathToPop) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToConditions = aPathToConditions;
        pathToPop = aPathToPop;
        this.data = new CSVDataJava(pathToData);
        readRuleTrees(pathToPop);
        readConditions(pathToConditions);
    }

    
    public void readRuleTrees(String filePath) throws IOException, ClassNotFoundException{
        models = new Population();
        ArrayList<String> alModels = new ArrayList<String>();
        Scanner sc = new Scanner(new FileReader(filePath));
        while(sc.hasNextLine()){
            String line = sc.nextLine();
            String tokens[] = line.split(",");
            String model = tokens[0];
            Tree g = TreeGenerator.generateTree(model);
            Individual iAux = new Individual(g);
            models.add(iAux);
        }
    }
  
   public void readConditions(String conditionsPath) throws IOException, ClassNotFoundException{
        intervals = new ArrayList<Interval>();
        Scanner sc = new Scanner(new FileReader(conditionsPath));
        while(sc.hasNextLine()){
            String line = sc.nextLine();
            String[] tokens = line.split(" ");
            String varAux = tokens[2];
            double lbAux = Double.valueOf(tokens[5]);
            double ubAux = Double.valueOf(tokens[7]);
            Interval iAux = new Interval(varAux, lbAux, ubAux);
            intervals.add(iAux);
        }
    }
   
    public void eval(Individual ind) {
        Tree genotype = (Tree) ind.getGenotype();
        BooleanFunction func = genotype.generateBoolean();
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        double[] targets = data.getTargetValues();
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
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            boolean val = func.eval(d,intervals);
            
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
            d.clear();
        }
        double falsePositiveRate = numFalsePositives / numNegativeTarget;
        double falseNegativeRate = numFalseNegatives / numPositiveTarget;
        double accuracy = accuratePredictions / data.getNumberOfFitnessCases();
        double precision = numTruePositives / numPositivePrediction;
        double recall = numTruePositives / numPositiveTarget;
        double fscore = 2 * ( (precision*recall) / (precision + recall) );
        System.out.println();
        System.out.println("RULE TREE: " + ind.getGenotype().toString());
        System.out.println("ACCURACY: " + accuracy);
        System.out.println("PRECISION: " + precision);
        System.out.println("RECALL: " + recall);
        System.out.println("F-SCORE: " + fscore);
        System.out.println("FALSE POSITIVE RATE: " + falsePositiveRate);
        System.out.println("FALSE NEGATIVE RATE: " + falseNegativeRate);
        
        System.out.println();
        func = null;
    }

    public void evalPop() {
        for (Individual individual : models) {
            this.eval(individual);
        }
    }    


}