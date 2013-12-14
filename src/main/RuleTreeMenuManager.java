/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import evogpj.algorithm.ClassRuleTree;
import evogpj.algorithm.Parameters;
import evogpj.gp.Individual;
import evogpj.test.TestRuleTrees;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author nacho
 */
public class RuleTreeMenuManager {
    
    public RuleTreeMenuManager(){
        
    }
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar sr.jar -train path_to_data -minutes minutes [-properties path_to_properties]");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar sr.jar -test path_to_data -conditions path_to_conditions");
        System.err.println();
    }
    
    public void parseRuleTreeTrain(String args[]) throws IOException{
        String dataPath, propsFile;
        int numMinutes;
        ClassRuleTree ruleTree;
        if(args.length==4 || args.length==6){
            dataPath = args[1];
            // run evogpj with standard properties
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            props.put(Parameters.Names.FITNESS, "fitness.TBRCFitness.Java, fitness.SubtreeComplexity");
            if (args[2].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[3]);
                if(args.length==4){
                    ruleTree = new ClassRuleTree(props,numMinutes*60);
                    Individual bestIndi = ruleTree.run_population();
                }else if(args.length==6){
                    if(args[4].equals("-properties")){
                        propsFile = args[5];
                        // run evogpj with properties file and modified properties
                        ruleTree = new ClassRuleTree(props,propsFile,numMinutes*60);
                        Individual bestIndi = ruleTree.run_population();
                    }else{
                        System.err.println("Error: wrong argument. Expected -properties flag");
                        printUsage();
                        System.exit(-1);
                    }
                }
            }else{
                System.err.println("Error: must specify the optimization time in minutes");
                printUsage();
                System.exit(-1);
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }
    
    //java -jar sr.jar -test path_to_data -pathToConditions path_to_conditions
    public void parseRuleTreeTest(String args[]) throws IOException, ClassNotFoundException{
        String dataPath, popPath, conditionsPath;
        if (args.length==4){
            // by default integer targets = false
            dataPath = args[1];
            if(args[2].equals("-conditions")){
                conditionsPath = args[3];
                // check if knee model exists
                popPath = "knee.txt";
                System.out.println();
                if(new File(popPath).isFile()){
                    System.out.println("TESTING KNEE MODEL:");
                    TestRuleTrees trt = new TestRuleTrees(dataPath, conditionsPath, popPath);
                    trt.evalPop();
                    System.out.println();
                }
                popPath = "mostAccurate.txt";
                if(new File(popPath).isFile()){
                    System.out.println("TESTING MOST ACCURATE MODEL: ");
                    TestRuleTrees trt = new TestRuleTrees(dataPath, conditionsPath, popPath);
                    trt.evalPop();
                    System.out.println();
                }
                popPath = "leastComplex.txt";
                if(new File(popPath).isFile()){
                    System.out.println("TESTING SIMPLEST MODEL: ");
                    TestRuleTrees trt = new TestRuleTrees(dataPath, conditionsPath, popPath);
                    trt.evalPop();
                    System.out.println();
                }
                popPath = "pareto.txt";
                if(new File(popPath).isFile()){
                    System.out.println("TESTING PARETO FRONT: ");
                    TestRuleTrees trt = new TestRuleTrees(dataPath, conditionsPath, popPath);
                    trt.evalPop();
                    System.out.println();
                }
                
            }else{
                System.err.println("Error: wrong argument. Expected -pathToConditions flag");
                printUsage();
                System.exit(-1);
            }   
        }else {
            System.err.println();
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        RuleTreeMenuManager m = new RuleTreeMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-train")) {
                m.parseRuleTreeTrain(args);
            }else if(args[0].equals("-test")){
                m.parseRuleTreeTest(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
                System.exit(-1);
            }
        }
    }
}
