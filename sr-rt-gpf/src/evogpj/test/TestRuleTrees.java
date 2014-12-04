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
import evogpj.algorithm.Parameters;

import java.util.ArrayList;
import java.util.List;

import evogpj.preprocessing.Interval;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Test ruleTree models
 * 
 * @author Ignacio Arnaldo
 */
public class TestRuleTrees {
    
    private String pathToData;
    private final DataJava data;
        
    private String dirPath;
    
    private String pathToPop;
    private Population models;
    
    ArrayList<Interval> intervals;
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToData
     * @param aPathToPop
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public TestRuleTrees(String aPathToData, String aPathToPop) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToPop = aPathToPop;
        this.dirPath="";
        this.data = new CSVDataJava(pathToData);
        readConditionsAndRuleTrees(pathToPop);
    }
    
    public TestRuleTrees(List<String> testData, String dirPath, String aPathToPop) throws IOException, ClassNotFoundException {
        //pathToData = aPathToData;
        pathToPop = aPathToPop;
        this.data = new CSVDataJava(testData);
        this.dirPath=dirPath;
        readConditionsAndRuleTrees(pathToPop);
    }

    
    private void readConditionsAndRuleTrees(String filePath) throws IOException, ClassNotFoundException{
        intervals = new ArrayList<Interval>();
        Scanner sc = new Scanner(new FileReader(filePath));
        String line = sc.nextLine();
        while(!line.equals("MODELS:")){
            String[] tokens = line.split(" ");
            String varAux = tokens[2];
            double lbAux = Double.valueOf(tokens[5]);
            double ubAux = Double.valueOf(tokens[7]);
            Interval iAux = new Interval(varAux, lbAux, ubAux);
            intervals.add(iAux);
            line = sc.nextLine();
        }
        models = new Population();
        while(sc.hasNextLine()){
            line = sc.nextLine();
            String tokens[] = line.split(",");
            String model = tokens[0];
            Tree g = TreeGenerator.generateTree(model);
            Individual iAux = new Individual(g);
            models.add(iAux);
        }
    }

    public void predictions(Individual ind,String filePath, int indexIndi) throws IOException {
        Tree genotype = (Tree) ind.getGenotype();
        BooleanFunction func = genotype.generateBoolean();
        List<Double> d;
        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + "_" + indexIndi + ".csv"));
        PrintWriter printWriter = new PrintWriter(bw);
        double[][] inputValuesAux = data.getInputValues();
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            boolean val = func.eval(d,intervals);
            if(val==true){
                printWriter.println(1);
            }else{
                printWriter.println(0);
            }
            d.clear();
        }
        func = null;
        printWriter.flush();
        printWriter.close();
    }
    
    public void eval(Individual ind, boolean log) {
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
        
        StringBuffer result = new StringBuffer();
        
        result.append("\n");
        result.append("RULE TREE: " + ind.getGenotype().toString()+'\n');
        result.append("ACCURACY: " + accuracy+'\n');
        result.append("PRECISION: " + precision+'\n');
        result.append("RECALL: " + recall+'\n');
        result.append("F-SCORE: " + fscore+'\n');
        result.append("FALSE POSITIVE RATE: " + falsePositiveRate+'\n');
        result.append("FALSE NEGATIVE RATE: " + falseNegativeRate+'\n');
        result.append("\n");
        
        if(log){
        	saveText(this.dirPath+Parameters.Defaults.RESULT_PATH,result.toString(),false);
        }
        
        System.out.println(result.toString());
        
        func = null;
    }
    
    
    public void predictionsPop(String filePath) throws IOException {
        int indexIndi=0;
        for (Individual individual : models) {
            this.predictions(individual,filePath,indexIndi);
            indexIndi++;
        }
    }  
    
    public void evalPop() {
        evalPop(false);
    }    
    
    public void evalPop(boolean log){
    	for (Individual individual : models) {
            this.eval(individual,log);
        }
    }
    
    /**
     * Save text to a filepath
     * @param filepath
     * @param text
     */
    protected void saveText(String filepath, String text, Boolean append) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filepath,append));
            PrintWriter printWriter = new PrintWriter(bw);
            printWriter.write(text);
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            System.exit(-1);
        }
    }


}