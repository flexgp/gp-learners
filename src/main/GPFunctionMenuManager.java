/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import evogpj.algorithm.ClassGPFunction;
import evogpj.algorithm.Parameters;
import evogpj.gp.Individual;
import evogpj.test.TestGPFunctionClassifiers;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author nacho
 */
public class GPFunctionMenuManager {
    
    public GPFunctionMenuManager(){
        
    }
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar gpfunction.jar -train path_to_data -cv path_to_cv_data -minutes min [-cpp numThreads -properties path_to_properties]");
        System.err.println("or");
        System.err.println("java -jar gpfunction.jar -train path_to_data -cv path_to_cv_data -minutes min [-cuda -properties path_to_properties]");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar gpfunction.jar -test path_to_data");
        System.err.println();
    }
    
    public void parseGPFunctionTrain(String args[]) throws IOException{
        String dataPath,cvDataPath;
        int numMinutes=0;
        String propsFile = "";
        ClassGPFunction gpfunction = null;
        if(args.length==6 || args.length==8 || args.length==9 || args.length==10){
            dataPath = args[1];
            // run evogpj with standard properties
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            if(args[2].equals("-cv")){
                cvDataPath = args[3];
                props.put(Parameters.Names.CROSS_VAL_SET, cvDataPath);
                if (args[4].equals("-minutes")) {
                    numMinutes = Integer.valueOf(args[5]);
                    if(args.length==6){
                        props.put(Parameters.Names.FITNESS, "fitness.SRFitness.CppROC,fitness.SubtreeComplexity");
                        props.put(Parameters.Names.EXTERNAL_THREADS, "1");
                        gpfunction = new ClassGPFunction(props,numMinutes*60);
                        Individual bestIndi = gpfunction.run_population();
                    }else if(args.length==8|| args.length==10){
                        if(args[6].equals("-cpp")){
                            props.put(Parameters.Names.FITNESS, "fitness.SRFitness.CppROC,fitness.SubtreeComplexity");
                            String numThreads = args[7];
                            props.put(Parameters.Names.EXTERNAL_THREADS, numThreads);
                            if(args.length==8){
                                gpfunction = new ClassGPFunction(props,numMinutes*60);
                                Individual bestIndi = gpfunction.run_population();
                            }else if(args.length==10){
                                if(args[8].equals("-properties")){
                                    propsFile = args[9];
                                    // run evogpj with properties file and modified properties
                                    gpfunction = new ClassGPFunction(props,propsFile,numMinutes*60);
                                    Individual bestIndi = gpfunction.run_population();
                                }else{
                                    System.err.println("Error: wrong argument. Expected -properties flag");
                                    printUsage();
                                    System.exit(-1);
                                }
                            }
                        }else{
                            System.err.println("Error: wrong argument. Expected -cpp flag");
                            printUsage();
                            System.exit(-1);
                        }
                    }else if(args.length==9){
                        if(args[6].equals("-cuda")){
                            props.put(Parameters.Names.FITNESS, "fitness.SRFitness.CudaROC,fitness.SubtreeComplexity");
                            if(args[7].equals("-properties")){
                                propsFile = args[8];
                                // run evogpj with properties file and modified properties
                                gpfunction = new ClassGPFunction(props,propsFile,numMinutes*60);
                                Individual bestIndi = gpfunction.run_population();
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
                    }                    
                }else{
                    System.err.println("Error: must specify the optimization time in minutes");
                    printUsage();
                    System.exit(-1);
                } 
            }else{
                System.err.println("Error: wrong argument. Expected -cv flag");
                printUsage();
                System.exit(-1);
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }
    
    //java -jar gpfunction.jar -test path_to_data
    public void parseGPFunctionTest(String args[]) throws IOException, ClassNotFoundException{
        String dataPath;
        String popPath;
        if (args.length==2){
            dataPath = args[1];
            // check if knee model exists
            popPath = "bestCrossValidation.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING BEST MODEL ON CROSS VALIDATION:");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(dataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "knee.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING KNEE MODEL:");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(dataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(dataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(dataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "pareto.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING PARETO FRONT: ");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(dataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        GPFunctionMenuManager m = new GPFunctionMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-train")) {
                m.parseGPFunctionTrain(args);
            }else if(args[0].equals("-test")){
                m.parseGPFunctionTest(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
                System.exit(-1);
            }
        }
    }
}
