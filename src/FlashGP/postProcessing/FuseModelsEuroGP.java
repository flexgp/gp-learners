/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package FlashGP.postProcessing;

import evogpj.evaluation.java.DataJava;
import evogpj.evaluation.java.CSVDataJava;
import evogpj.genotype.Tree;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.Stack;

/**
 *
 * @author nacho
 * 
 * This class fuses the best model per generation of an ensemble run
 * For each new GP run, the fused model is refreshed,
 * This allows to study the time-wise evolution of the accuracy of the fuse model when the number of GP runs increases
 */
public class FuseModelsEuroGP {
    Population models;
    String fusionSet;
    int numLines;
    int numRuns;
    EvalFusedModel efm;
    EvalScaledModels esm;
    private int minute;
    private int second;
    private int hour;
    
    public FuseModelsEuroGP(String aFusionSet,int aNumLines,String testSet,int numLinesTest,int numFeatures,int aNumRuns){
        fusionSet = aFusionSet;
        numLines = aNumLines; 
        numRuns = aNumRuns;
        //Data adFusion = new CSVData(fusionSet, aNumLines, numFeatures);
        DataJava adFusion = new CSVDataJava(fusionSet);
        //Data adTest = new CSVData(testSet, numLinesTest, numFeatures);
        DataJava adTest = new CSVDataJava(testSet);
        efm = new EvalFusedModel(adTest);
        esm = new EvalScaledModels(adFusion);
    }

    // in .txt file ready for matlab:
    //      - remove round()
    //      - remove first and last bracket
    //      - separate all tokens with spaces
    //      - replace .* with *
    public void readScaledModels(String filePath) throws IOException, ClassNotFoundException{
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
            //String preFixModel = getPrefixFromInfix(model);
            //Tree g = TreeGenerator.generateTree(preFixModel);
            Tree g = TreeGenerator.generateTree(model);
            g.setScalingSlope(slope);
            g.setScalingIntercept(intercept);
            Individual iAux = new Individual(g);
            models.add(i, iAux);
        }
    }
    
    public String getPrefixFromInfix(String inFix) {
        String input = inFix;
        ArrayList<String> features = new ArrayList<String>();
        int numFeatures = 90;
        for(int i=0;i<numFeatures;i++) features.add("X"+(i+1));
        ArrayList<String> alOps = new ArrayList<String>();
        String[] ops = {"+","-","*","/"};
        for(int i=0;i<ops.length;i++) alOps.add(ops[i]);
        String[] inputTokens = input.split(" ");
        ArrayList<String> out = new ArrayList<String>();
        Stack<String> stackOperands = new Stack<String>();
        Stack<String> stackOperators = new Stack<String>();
        
        for (String token : inputTokens) {
            if(features.contains(token)){
                stackOperands.push(token);
            }else if(token.equals("(") || alOps.contains(token)){ //stackOperators.isEmpty()){
                stackOperators.push(token);
            }else if(token.equals(")")){
                while(!stackOperators.peek().equals("(")){
                    String operator = stackOperators.pop();
                    String rightOperand = stackOperands.pop();
                    String leftOperand = stackOperands.pop();
                    String operand = " ( " + operator + " " + leftOperand + " " + rightOperand + " ) ";
                    stackOperands.push(operand);
                }
                stackOperators.pop();
            }
        }
        while(!stackOperators.isEmpty()){
            String operator = stackOperators.pop();
            String rightOperand = stackOperands.pop();
            String leftOperand = stackOperands.pop();
            String operand = " ( " + operator + " " + leftOperand + " " + rightOperand + " ) ";
            stackOperands.push(operand);                    
        }
        String prefix = stackOperands.pop();
        return prefix;   
    }
    
    public double[] getArmWeights(Population pop,String fusionSet,int numLines){
       
        
        int numFeatures = 90;
        int numIters = 200;
        boolean round = true;
        
        ModelFuserARM mfa = new ModelFuserARM(fusionSet,numFeatures,pop,numIters,round);
        double[] weights = mfa.arm_weights();
        for(int i=0;i<weights.length;i++){
            //System.out.println(weights[i]);
        }
        return weights;
        
    }
    
    public void computeMSEtest(String aConf) throws IOException, ClassNotFoundException{

        String pathExperiments = "/home/nacho/Dropbox/MIT/papers/GPU-EnsembleGP/experiments/";
        /*ArrayList<String> configurations = new ArrayList<String>();
        configurations.add("sr/all/");
        configurations.add("sr/factor/");
        configurations.add("corr/all/");
        configurations.add("corr/factor/");
        configurations.add("srcorr/factor/");
        configurations.add("srcorr/all/");*/
        
        
        //String conf = aConf; 
        String conf = "corr/factor/"; 
        
        System.out.println(conf);
        //for(int r=0;r<numRuns;r++){
            int r = 1;
            System.out.println("RUN " + r);
            String filePath = pathExperiments + conf + "run" + r + "/modelsBestGen.txt";
            readScaledModels(filePath);
            int numModels = models.size();


            int numIslands = numModels / 102; // 102 models per run: generation 0 to 100 + printing final best model

            Population bestCrossVal = new Population();
            for(int i=0;i<numIslands;i++){
                Population popIsland = new Population();
                for(int p=i*102;p<(i+1)*102;p++){
                        Individual ind = models.get(p);
                        boolean contains = false;
                        for(Individual indAux:popIsland){
                            if(indAux.equals(ind)) contains = true;
                        }
                        if(!contains) popIsland.add(ind);
                }

                // evalpopFirstIsland according to eval
                esm.evalPop(popIsland,true);
                popIsland.sortCrossVal();
                for(int s=0;s<popIsland.size() && s<1; s++){
                    bestCrossVal.add(popIsland.get(s));
                }

                // get best 100 inds



                double[] weights = getArmWeights(bestCrossVal,fusionSet,numLines);
                double mseFirstRuns = efm.eval(bestCrossVal, weights, true);
                System.out.println("ACCUMULATED " + bestCrossVal.size()+ " MODELS UP TO ISLAND " + i + ": " + mseFirstRuns);
                GregorianCalendar date = new GregorianCalendar();
                System.out.println("Current time is  " + date.get(Calendar.HOUR) + " : " + date.get(Calendar.MINUTE) + " : " + date.get(Calendar.SECOND));
            }
        //}
        System.out.println();


        
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException{
        
        String fusionSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-fusion-train-10pct-000.csv";
        int numLines = 51177; 
        String validationSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-test-20pct-000.csv";
        int numLinesV = 102440;
        int numFeatures = 90;
        int numRuns = 20;
        
        FuseModelsEuroGP fm = new FuseModelsEuroGP(fusionSet,numLines,validationSet,numLinesV,numFeatures,numRuns);
        //fm.computeMSEtest(args[0]);
        fm.computeMSEtest("");
        

        
    }
    
    
}
