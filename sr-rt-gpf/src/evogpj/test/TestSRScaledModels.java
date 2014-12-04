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
 * Test linearly scaled symbolic regression models
 * 
 * @author Ignacio Arnaldo
 */
public class TestSRScaledModels {
    
    private String pathToData;
    private final DataJava data;
        
    private String pathToPop;
    private Population models;
    
    private boolean round;
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param data
     *            The dataset (training cases, output variable) to use in
     *            computing the fitness of individuals.
     */
    public TestSRScaledModels(String aPathToData, String aPathToPop,boolean aRound) throws IOException, ClassNotFoundException {
        pathToData = aPathToData;
        pathToPop = aPathToPop;
        round = aRound;
        this.data = new CSVDataJava(pathToData);
        readScaledModels(pathToPop);
    }

    
    private void readScaledModels(String filePath) throws IOException, ClassNotFoundException{
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
        for(int i=0;i<popSize;i++){
            String scaledModel = alModels.get(i);
            String model = "";
            
            String[] tokens = scaledModel.split(" ");
            String sSlope = tokens[0];
            // remove parenthesis token 0 and token (last - 2)
            sSlope = sSlope.substring(1);
            tokens[tokens.length-3] = tokens[tokens.length-3].substring(0, tokens[tokens.length-3].length()-1);
            
            
            double slope = Double.parseDouble(sSlope);
            String sIntercept = tokens[tokens.length-1];
            double intercept = Double.parseDouble(sIntercept);
            for(int t=2;t<tokens.length-2;t++){
                model += tokens[t] + " ";
            }
            Tree g = TreeGenerator.generateTree(model);
            g.setScalingSlope(slope);
            g.setScalingIntercept(intercept);
            Individual iAux = new Individual(g);
            models.add(i, iAux);
        }
    }
    
    /**
     * @see Function
     */
    public void predictionsPop(String filePath) throws IOException {
        int indexIndi = 0;
        for(Individual ind:models){
            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath + "_" + indexIndi + ".csv"));
            PrintWriter printWriter = new PrintWriter(bw);
            Tree genotype = (Tree) ind.getGenotype();
            Function func = genotype.generate();
            List<Double> d;
            double[][] inputValuesAux = data.getInputValues();
            for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
                d = new ArrayList<Double>();
                for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                    d.add(j, inputValuesAux[i][j]);
                }
                Double val = func.eval(d);
                double slope = genotype.getScalingSlope();
                double intercept = genotype.getScalingIntercept();
                val = (val*slope) + intercept;
                if(round) val = (double)Math.round(val);
                printWriter.println(val);
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
        double[] targets = data.getTargetValues();
        for(Individual ind:models){
            double sqDiff = 0;
            double absDiff = 0;
            Tree genotype = (Tree) ind.getGenotype();
            Function func = genotype.generate();
            List<Double> d;
            double[][] inputValuesAux = data.getInputValues();
            for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
                d = new ArrayList<Double>();
                for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                    d.add(j, inputValuesAux[i][j]);
                }
                Double val = func.eval(d);
                double slope = genotype.getScalingSlope();
                double intercept = genotype.getScalingIntercept();
                val = (val*slope) + intercept;
                if(round) val = (double)Math.round(val);

                sqDiff += Math.pow(targets[i] - val, 2);
                absDiff += Math.abs(targets[i] - val);
                d.clear();
            }
            sqDiff = sqDiff / data.getNumberOfFitnessCases();
            absDiff= absDiff / data.getNumberOfFitnessCases();
            ind.setScaledMSE(sqDiff);
            ind.setScaledMAE(absDiff);
            func = null;
        }
    }
    
    public void saveModelsToFile(String filePath) throws IOException{
        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
        PrintWriter printWriter = new PrintWriter(bw);
        
        for(Individual ind:models){
            System.out.print( ind.toScaledString() + "\nMSE: " + ind.getScaledMSE() + "\nMAE: " + ind.getScaledMAE() + "\n");
            printWriter.write(ind.toScaledString() + "\nMSE: " + ind.getScaledMSE() + "\nMAE: " + ind.getScaledMAE() + "\n"); 
        }
        printWriter.flush();
        printWriter.close();
    }



}