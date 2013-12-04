/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import evogpj.algorithm.Parameters;
import evogpj.algorithm.SymbRegMOO;
import evogpj.gp.Individual;
import evogpj.test.TestFusedModel;
import evogpj.test.TestScaledModels;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author nacho
 */
public class MenuManager {
    
    public MenuManager(){
        
    }
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar evogpj.jar -train path_to_data -minutes min [-cpp numThreads-properties path_to_properties]");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar evogpj.jar -test path_to_data -integer true -scaled path_to_scaled_models");
        System.err.println("or");
        System.err.println("java -jar evogpj.jar -test path_to_data -integer true -fused path_to_fused_model");
        System.err.println();
    }
    
    public void parseSymbolicRegressionTrain(String args[]) throws IOException{
        String dataPath = "";
        int numMinutes=0;
        String propsFile = "";
        SymbRegMOO srEvoGPj = null;
        if(args.length==4 || args.length==6 || args.length==8){
            dataPath = args[1];
            // run evogpj with standard properties
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            if (args[2].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[3]);
            }else{
                System.err.println("Error: must specify the optimization time in minutes");
                printUsage();
                System.exit(-1);
            }
            if(args.length==6){
                if(args[4].equals("-cpp")){
                    props.put(Parameters.Names.FITNESS, "fitness.SRFitness.Cpp, fitness.SubtreeComplexity");
                    String numThreads = args[5];
                    props.put(Parameters.Names.EXTERNAL_THREADS, numThreads);
                    // run evogpj with modified properties
                    srEvoGPj = new SymbRegMOO(props,numMinutes*60);
                } else if(args[4].equals("-properties")){
                    propsFile = args[5];
                    // run evogpj with properties file and modified properties
                    srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
                }else{
                    System.err.println("Error: wrong argument. Expected -cpp flag");
                    printUsage();
                    System.exit(-1);
                }
            }else if(args.length==8){
                if(args[4].equals("-cpp")){
                    props.put(Parameters.Names.FITNESS, "fitness.SRFitness.Cpp, fitness.SubtreeComplexity");
                    String numThreads = args[5];
                    props.put(Parameters.Names.EXTERNAL_THREADS, numThreads);
                }else{
                    System.err.println("Error: wrong argument. Expected -cpp flag");
                    printUsage();
                    System.exit(-1);
                }
                if(args[6].equals("-properties")){
                    propsFile = args[7];
                }else{
                    System.err.println("Error: wrong argument. Expected -properties flag");
                    printUsage();
                    System.exit(-1);
                }
                // run evogpj with properties file and modified properties
                srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
            }else{
                // run evogpj with standard properties
                srEvoGPj = new SymbRegMOO(props,numMinutes*60);
                
            }
            
            Individual bestIndi = srEvoGPj.run_population();
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }
    //java -jar evogpj.jar -test path_to_data -integer true -scaled path_to_scaled_models
    public void parseSymbolicRegressionTest(String args[]) throws IOException, ClassNotFoundException{
        String dataPath;
        String popPath;
        boolean integerTarget;
        if(args.length==6){
            dataPath = args[1];
            if(args[2].equals("-integer")){
                integerTarget = Boolean.valueOf(args[3]);
                popPath = args[5];
                if(args[4].equals("-scaled")){
                    TestScaledModels tsm = new TestScaledModels(dataPath, popPath,integerTarget);
                    tsm.evalPop();
                    tsm.saveModelsToFile("test"+popPath);
                }else if(args[4].equals("-fused")){
                    TestFusedModel tfm = new TestFusedModel(dataPath, popPath, integerTarget);
                    ArrayList<Double> errors = tfm.eval();
                    System.out.println();
                    System.out.println(" MSE fused Model: " + errors.get(0));
                    System.out.println(" MAE fused Model: " + errors.get(1));
                    System.out.println();
                }else{
                    System.err.println("Error: wrong argument. Expected -scaled or -fused flag");
                    printUsage();
                    System.exit(-1);
                }
            }else{
                System.err.println("Error: wrong argument. Expected -integer flag");
                printUsage();
                System.exit(-1);
            }
        }else if (args.length==2){
            // by default integer targets = false
            integerTarget = false;
            dataPath = args[1];
            // check if knee model exists
            popPath = "knee.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING KNEE MODEL:");
                TestScaledModels tsm = new TestScaledModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestScaledModels tsm = new TestScaledModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestScaledModels tsm = new TestScaledModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "fusedModel.txt";
            if(new File(popPath).isFile()){
                TestFusedModel tfm = new TestFusedModel(dataPath, popPath, integerTarget);
                ArrayList<Double> errors = tfm.eval();
                System.out.println("TESTING FUSED MODEL:");
                System.out.println("MSE fused Model: " + errors.get(0));
                System.out.println("MAE fused Model: " + errors.get(1));
                System.out.println();
            }
            
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        MenuManager m = new MenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-train")) {
                m.parseSymbolicRegressionTrain(args);
            }else if(args[0].equals("-test")){
                m.parseSymbolicRegressionTest(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
                System.exit(-1);
            }
        }
    }
}
