/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package FlashGP.main;

import FlashGP.postProcessing.ModelFuserARM;
import evogpj.genotype.Tree;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;

/**
 *
 * @author nacho
 */
public class FuseModels {
    
    Population models;
    
    public FuseModels(){
        
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
            double slope = Double.parseDouble(sSlope);
            String sIntercept = tokens[tokens.length-1];
            double intercept = Double.parseDouble(sIntercept);
            for(int t=2;t<tokens.length-2;t++){
                model += tokens[t] + " ";
            }
            String preFixModel = getPrefixFromInfix(model);
            Tree g = TreeGenerator.generateTree(preFixModel);
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
    
    public void getArmWeights(){
        //String fusionSet = "matlabTests/MSD-fusion-train-plus-80pct-000.csv";
        //int numLines = 413124;
        String fusionSet = "/media/DATA/datasets/MSD/MSD-fusion-train-10pct-000.csv";
        int numLines = 51177; 
        
        int numFeatures = 90;
        int numIters = 10000;
        boolean round = true;
        // public ModelFuserARM(String aTrainingSet,int aNumberOfLines, int aNumberOfFeatures, Population aPop,int aNumIters){
        ModelFuserARM mfa = new ModelFuserARM(fusionSet,numFeatures,models,numIters,round);
        double[] weights = mfa.arm_weights();
        for(int i=0;i<weights.length;i++){
            System.out.println(weights[i]);
        }
        
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException{
        
        FuseModels fm = new FuseModels();
        String filePath = "pop.txt";
        fm.readScaledModels(filePath);
        fm.getArmWeights();        
    }
}
