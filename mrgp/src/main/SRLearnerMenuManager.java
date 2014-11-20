/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import evogpj.algorithm.Parameters;
import evogpj.algorithm.SymbRegMOO;
import evogpj.gp.Individual;
import evogpj.preprocessing.NormalizeData;
import evogpj.test.TestRGPModels;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 *
 * @author Ignacio Arnaldo
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
        System.err.println("java -jar sr.jar -train path_to_data -minutes min [-properties path_to_properties]");
        System.err.println();
        System.err.println("java -jar sr.jar -train path_to_data -minutes min [-cpp numThreads -properties path_to_properties]");
        System.err.println("or");
        System.err.println("java -jar sr.jar -train path_to_data -minutes min [-cuda -properties path_to_properties]");
        System.err.println();
        System.err.println("OBTAIN PREDICTIONS:");
        System.err.println("java -jar sr.jar -predict path_to_data -o path_to_predictions -integer true -scaled path_to_scaled_models");
        System.err.println("or");
        System.err.println("java -jar sr.jar -predict path_to_data -o path_to_predictions -integer true -fused path_to_fused_model");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar sr.jar -test path_to_data");
        System.err.println("or");
        System.err.println("java -jar sr.jar -test path_to_data -integer true -scaled path_to_scaled_models");
        System.err.println("or");
        System.err.println("java -jar sr.jar -test path_to_data -integer true -fused path_to_fused_model");
        System.err.println();
        
        
    }
    
    public void parseSymbolicRegressionTrain(String args[]) throws IOException{
        String dataPath;
        int numMinutes=0;
        String propsFile = "";
        SymbRegMOO srEvoGPj;
        if(args.length==4 || args.length==5 || args.length==6 || args.length==7 || args.length==8){
            dataPath = args[1];
            // run evogpj with standard properties
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            if (args[2].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[3]);
                if(args.length==4){// JAVA NO PROPERTIES
                    // run evogpj with standard properties
                    srEvoGPj = new SymbRegMOO(props,numMinutes*60);
                    Individual bestIndi = srEvoGPj.run_population();
                }
                if(args.length==6 || args.length==8){
                    if(args[4].equals("-properties")){ // JAVA WITH PROPERTIES
                        propsFile = args[5];
                        // run evogpj with properties file and modified properties
                        srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
                        Individual bestIndi = srEvoGPj.run_population();
                    }else if(args[4].equals("-cpp")){ // CPP WITH OR WITHOUT PROPERTIES
                        props.put(Parameters.Names.FITNESS, Parameters.Operators.SR_CPP_FITNESS + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                        String numThreads = args[5];
                        props.put(Parameters.Names.EXTERNAL_THREADS, numThreads);
                        if(args.length==6){ // CPP WITHOUT PROPERTIES
                            srEvoGPj = new SymbRegMOO(props,numMinutes*60);
                            Individual bestIndi = srEvoGPj.run_population();
                        }else if(args.length==8){// CPP WITH PROPERTIES
                            if(args[6].equals("-properties")){
                                propsFile = args[7];
                                // run evogpj with properties file and modified properties
                                srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
                                Individual bestIndi = srEvoGPj.run_population();
                            }else{
                                System.err.println("Error: wrong argument. Expected -properties flag");
                                printUsage();
                                System.exit(-1);
                            }
                        }
                        // run evogpj with modified properties

                    }else{
                        System.err.println("Error: wrong argument. Expected -cpp flag");
                        printUsage();
                        System.exit(-1);
                    }
                }else if(args.length==5 || args.length==7){ // CUDA WITH OR WITHOUT PROPERTIES
                    if(args[4].equals("-cuda")){
                        props.put(Parameters.Names.FITNESS, Parameters.Operators.SR_CUDA_FITNESS + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                        if(args.length==5){//CUDA WITHOUT PROPERTIES
                            srEvoGPj = new SymbRegMOO(props,numMinutes*60);
                            Individual bestIndi = srEvoGPj.run_population();
                        }else if(args.length==7){// CUDA WITH PROPERTIES
                            if(args[5].equals("-properties")){
                                propsFile = args[6];
                                srEvoGPj = new SymbRegMOO(props,propsFile,numMinutes*60);
                                Individual bestIndi = srEvoGPj.run_population();
                            }else{
                                System.err.println("Error: wrong argument. Expected -properties flag");
                                printUsage();
                                System.exit(-1);
                            }
                        }
                    }else{
                        System.err.println("Error: wrong argument. Expected -cuda flag");
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
    
    
    //java -jar evogpj.jar -predictions path_to_data -o filename -integer true -scaled path_to_scaled_models
    public void parseSymbolicRegressionPredictions(String args[]) throws IOException, ClassNotFoundException{
        String dataPath;
        String popPath;
        String predPath;
        boolean integerTarget;
        if(args.length==8){
            dataPath = args[1];
            if(args[2].equals("-o")){
                predPath = args[3];
                if(args[4].equals("-integer")){
                    integerTarget = Boolean.valueOf(args[5]);
                    popPath = args[7];
                    if(args[6].equals("-scaled")){
                        TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                        tsm.predictionsPop(predPath);
                    }else{
                        System.err.println("Error: wrong argument. Expected -scaled flag");
                        printUsage();
                        System.exit(-1);
                    }
                }else{
                    System.err.println("Error: wrong argument. Expected -integer flag");
                    printUsage();
                    System.exit(-1);
                }
            }else{
                System.err.println("Error: wrong argument. Expected -o flag");
                printUsage();
                System.exit(-1);
            }
        }else {
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
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            popPath = "pareto.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING PARETO MODELS: ");
                TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                tsm.evalPop();
                tsm.saveModelsToFile("test"+popPath);
                System.out.println();
            }
            
        }else if(args.length==6){
            dataPath = args[1];
            if(args[2].equals("-integer")){
                integerTarget = Boolean.valueOf(args[3]);
                popPath = args[5];
                if(args[4].equals("-scaled")){
                    TestRGPModels tsm = new TestRGPModels(dataPath, popPath,integerTarget);
                    tsm.evalPop();
                    tsm.saveModelsToFile("test"+popPath);
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
    
    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException{
        SRLearnerMenuManager m = new SRLearnerMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-train")) {
                m.parseSymbolicRegressionTrain(args);
            }else if(args[0].equals("-predict")){
                m.parseSymbolicRegressionPredictions(args);
            }else if(args[0].equals("-test")){
                m.parseSymbolicRegressionTest(args);
            }else if(args[0].equals("-normalizeData")){
                m.parseNormalizeData(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
                System.exit(-1);
            }
        }
    }
}
