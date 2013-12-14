/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import evogpj.algorithm.Parameters;
import evogpj.algorithm.SymbRegMOO;
import evogpj.gp.Individual;
import evogpj.preprocessing.NormalizeData;
import evogpj.test.TestSRFusedModel;
import evogpj.test.TestSRFusedModelNorm;
import evogpj.test.TestSRScaledModels;
import evogpj.test.TestSRScaledModelsNorm;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author nacho
 */
public class SRLearnerMenuManager {
    
    public SRLearnerMenuManager(){
        
    }
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("NORMALIZE DATA:");
        System.err.println("java -jar sr.jar -normalizeData path_to_data -newPath path_to_normalized_data -pathToBounds path_to_variable_bounds");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar sr.jar -train path_to_data -minutes min [-cpp numThreads-properties path_to_properties]");
        System.err.println("or");
        System.err.println("java -jar sr.jar -train path_to_data -minutes min [-cuda -properties path_to_properties]");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar sr.jar -test path_to_data");
        System.err.println("or");
        System.err.println("java -jar sr.jar -test path_to_data -integer true -scaled path_to_scaled_models");
        System.err.println("or");
        System.err.println("java -jar sr.jar -test path_to_data -integer true -fused path_to_fused_model");
        System.err.println();
        System.err.println("TEST ON NORMALIZED DATA:");
        System.err.println("java -jar sr.jar -normTest path_to_data -pathToTrainBounds path_to_tr_bounds -pathToTestBounds path_to_test_bounds");
        //System.err.println("or");
        //System.err.println("java -jar sr.jar -normTest path_to_data -pathToBounds path_to_training_bounds -integer true -scaled path_to_scaled_models");
        //System.err.println("or");
        //System.err.println("java -jar sr.jar -normTest path_to_data -pathToBounds path_to_training_bounds -integer true -fused path_to_fused_model");
    }
    
    public void parseSymbolicRegressionTrain(String args[]) throws IOException{
        String dataPath = "";
        int numMinutes=0;
        String propsFile = "";
        SymbRegMOO srEvoGPj = null;
        if(args.length==4 || args.length==6 || args.length==7 || args.length==8){
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
                    Individual bestIndi = srEvoGPj.run_population();
                } else if(args[4].equals("-properties")){
                    propsFile = args[5];
                    // run evogpj with properties file and modified properties
                    srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
                    Individual bestIndi = srEvoGPj.run_population();
                }else{
                    System.err.println("Error: wrong argument. Expected -cpp flag");
                    printUsage();
                    System.exit(-1);
                }
            }else if(args.length==7){
                if(args[4].equals("-cuda")){
                    props.put(Parameters.Names.FITNESS, "fitness.SRFitness.Cuda, fitness.SubtreeComplexity");
                    if(args[5].equals("-properties")){
                        propsFile = args[6];
                        srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
                        Individual bestIndi = srEvoGPj.run_population();
                    }else{
                        System.err.println("Error: wrong argument. Expected -properties flag");
                        printUsage();
                        System.exit(-1);
                    }
                }else{
                    System.err.println("Error: wrong argument. Expected -cuda flag");
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
                Individual bestIndi = srEvoGPj.run_population();
            }else{
                // run evogpj with standard properties
                srEvoGPj = new SymbRegMOO(props,numMinutes*60);
                Individual bestIndi = srEvoGPj.run_population();
            }
            
            
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
        if (args.length==2){
            // by default integer targets = false
            integerTarget = false;
            dataPath = args[1];
            // check if knee model exists
            popPath = "knee.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING KNEE MODEL:");
                TestSRScaledModels tsm = new TestSRScaledModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestSRScaledModels tsm = new TestSRScaledModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestSRScaledModels tsm = new TestSRScaledModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "fusedModel.txt";
            if(new File(popPath).isFile()){
                TestSRFusedModel tfm = new TestSRFusedModel(dataPath, popPath, integerTarget);
                ArrayList<Double> errors = tfm.eval();
                System.out.println("TESTING FUSED MODEL:");
                System.out.println("MSE fused Model: " + errors.get(0));
                System.out.println("MAE fused Model: " + errors.get(1));
                System.out.println();
            }
            
        }else if(args.length==6){
            dataPath = args[1];
            if(args[2].equals("-integer")){
                integerTarget = Boolean.valueOf(args[3]);
                popPath = args[5];
                if(args[4].equals("-scaled")){
                    TestSRScaledModels tsm = new TestSRScaledModels(dataPath, popPath,integerTarget);
                    tsm.evalPop();
                    tsm.saveModelsToFile("test"+popPath);
                }else if(args[4].equals("-fused")){
                    TestSRFusedModel tfm = new TestSRFusedModel(dataPath, popPath, integerTarget);
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
        }else {
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    
    public void parseNormalizeData(String args[]) throws IOException{
        if (args.length == 6) {    
            String filePath = args[1];
            if(args[2].equals("-newPath")){
                String newPath = args[3];
                if(args[4].equals("-pathToBounds")){
                    String pathToBounds = args[5];
                    NormalizeData nd = new NormalizeData(filePath);
                    nd.normalize(newPath,pathToBounds);
                }else{
                    System.err.println("Error: expected flag newPath");
                    printUsage();
                    System.exit(-1);
                }
            }else{
                System.err.println("Error: expected flag newPath");
                printUsage();
                System.exit(-1);
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }

    /*TEST ON NORMALIZED DATA:
    java -jar sr.jar -normTest path_to_data -pathToBounds path_to_training_bounds
    java -jar sr.jar -normTest path_to_data -pathToBounds path_to_training_bounds -integer true -scaled path_to_scaled_models
    java -jar sr.jar -normTest path_to_data -pathToBounds path_to_training_bounds -integer true -fused path_to_fused_model
    */     
    public void parseNormalizeTest(String args[]) throws IOException, ClassNotFoundException{
        String dataPath,boundsTrainPath,boundsTestPath;
        String popPath;
        boolean integerTarget;
        if (args.length==6){
            // by default integer targets = false
            integerTarget = false;
            dataPath = args[1];
            if(args[2].equals("-pathToTrainBounds")){
                boundsTrainPath = args[3];
                if(args[4].equals("-pathToTestBounds")){
                    boundsTestPath = args[5];
                    // check if knee model exists
                    popPath = "knee.txt";
                    System.out.println();
                    if(new File(popPath).isFile()){
                        System.out.println("TESTING KNEE MODEL:");
                        TestSRScaledModelsNorm tsm = new TestSRScaledModelsNorm(dataPath, boundsTrainPath,boundsTestPath,popPath,integerTarget);
                        tsm.evalPop();
                        tsm.saveModelsToFile("test"+popPath);
                        System.out.println();
                    }
                    popPath = "mostAccurate.txt";
                    if(new File(popPath).isFile()){
                        System.out.println("TESTING MOST ACCURATE MODEL: ");
                        TestSRScaledModelsNorm tsm = new TestSRScaledModelsNorm(dataPath, boundsTrainPath,boundsTestPath,popPath,integerTarget);
                        tsm.evalPop();
                        tsm.saveModelsToFile("test"+popPath);
                        System.out.println();
                    }
                    popPath = "leastComplex.txt";
                    if(new File(popPath).isFile()){
                        System.out.println("TESTING SIMPLEST MODEL: ");
                        TestSRScaledModelsNorm tsm = new TestSRScaledModelsNorm(dataPath,boundsTrainPath,boundsTestPath,popPath,integerTarget);
                        tsm.evalPop();
                        tsm.saveModelsToFile("test"+popPath);
                        System.out.println();
                    }
                    popPath = "pareto.txt";
                    if(new File(popPath).isFile()){
                        System.out.println("TESTING PARETO FRONT: ");
                        TestSRScaledModelsNorm tsm = new TestSRScaledModelsNorm(dataPath,boundsTrainPath,boundsTestPath,popPath,integerTarget);
                        tsm.evalPop();
                        tsm.saveModelsToFile("test"+popPath);
                        System.out.println();
                    }
                    popPath = "fusedModel.txt";
                    if(new File(popPath).isFile()){
                        TestSRFusedModelNorm tfm = new TestSRFusedModelNorm(dataPath, boundsTrainPath,boundsTestPath, popPath, integerTarget);
                        ArrayList<Double> errors = tfm.eval();
                        System.out.println("TESTING FUSED MODEL:");
                        System.out.println("MSE fused Model: " + errors.get(0));
                        System.out.println("MAE fused Model: " + errors.get(1));
                        System.out.println();
                    }
                }else{
                    System.err.println("Error: wrong argument. Expected -pathToTestBounds flag");
                    printUsage();
                    System.exit(-1);
                }
                
            }else{
                System.err.println("Error: wrong argument. Expected -pathToTrainBounds flag");
                printUsage();
                System.exit(-1);
            }
            
            
        }/*else if(args.length==6){
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
        }*/else {
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }
    
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        SRLearnerMenuManager m = new SRLearnerMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-train")) {
                m.parseSymbolicRegressionTrain(args);
            }else if(args[0].equals("-test")){
                m.parseSymbolicRegressionTest(args);
            }else if(args[0].equals("-normalizeData")){
                m.parseNormalizeData(args);
            }else if(args[0].equals("-normTest")){
                m.parseNormalizeTest(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
                System.exit(-1);
            }
        }
    }
}
