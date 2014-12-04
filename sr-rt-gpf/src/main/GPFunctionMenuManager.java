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
 * @author Ignacio Arnaldo
 */
package main;

import evogpj.algorithm.ClassGPFunction;
import evogpj.algorithm.Parameters;
import evogpj.test.TestGPFunctionClassifiers;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * wrapper class to parse the command line interface of the GPFunction learner
 * @author Ignacio Arnaldo
 */
public class GPFunctionMenuManager {
    
    public GPFunctionMenuManager(){
        
    }
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar gpfunction.jar -train path_to_data -minutes min [-properties path_to_properties]");
        System.err.println("or");
        System.err.println("java -jar gpfunction.jar -train path_to_data -minutes min [-cpp numThreads -properties path_to_properties]");
        System.err.println("or");
        System.err.println("java -jar gpfunction.jar -train path_to_data -minutes min [-cuda -properties path_to_properties]");
        System.err.println();
        System.err.println("OBTAIN PREDICTIONS:");
        System.err.println("java -jar gpfunction.jar -predict path_to_test_data -model path_to_model -o path_to_predictions");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar gpfunction.jar -test path_to_test_data");
        System.err.println();
    }
    
    public void parseGPFunctionTrain(String args[]) throws IOException{
        String dataPath;
        int numMinutes;
        String propsFile;
        ClassGPFunction gpfunction;
        if(args.length==4 || args.length==5 || args.length==6 || args.length==7 || args.length==8){
            dataPath = args[1];
            // run evogpj with standard properties
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            props.put(Parameters.Names.VAL_SET, dataPath);
            if (args[2].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[3]);
                if(args.length==4){
                    props.put(Parameters.Names.FITNESS, Parameters.Operators.GPFUNCTION_JAVA + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                    props.put(Parameters.Names.EXTERNAL_THREADS, "1");
                    gpfunction = new ClassGPFunction(props,numMinutes*60);
                    gpfunction.run_population();
                }else if(args.length==6|| args.length==8){
                    if(args[4].equals("-cpp")){
                        props.put(Parameters.Names.FITNESS, Parameters.Operators.GPFUNCTION_CPP + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                        String numThreads = args[5];
                        props.put(Parameters.Names.EXTERNAL_THREADS, numThreads);
                        if(args.length==6){
                            gpfunction = new ClassGPFunction(props,numMinutes*60);
                            gpfunction.run_population();
                        }else if(args.length==8){
                            if(args[6].equals("-properties")){
                                propsFile = args[7];
                                // run evogpj with properties file and modified properties
                                gpfunction = new ClassGPFunction(props,propsFile,numMinutes*60);
                                gpfunction.run_population();
                            }else{
                                System.err.println("Error: wrong argument. Expected -properties flag");
                                printUsage();
                                System.exit(-1);
                            }
                        }
                    }else if(args[4].equals("-properties") || args.length==6 ){
                        propsFile = args[5];
                        props.put(Parameters.Names.FITNESS, Parameters.Operators.GPFUNCTION_JAVA + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                        //props.put(Parameters.Names.EXTERNAL_THREADS, "1");
                        gpfunction = new ClassGPFunction(props,propsFile,numMinutes*60);
                        gpfunction.run_population();
                    }else{
                        System.err.println("Error: wrong argument. Expected -cpp or -properties flag");
                        printUsage();
                        System.exit(-1);
                    }
                }else if(args.length==5 || args.length==7){ 
                   if(args[4].equals("-cuda")){
                        props.put(Parameters.Names.FITNESS, Parameters.Operators.GPFUNCTION_CUDA + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                        if(args.length==5){
                            gpfunction = new ClassGPFunction(props,numMinutes*60);
                            gpfunction.run_population();
                        }else if(args.length==7){
                            if(args[5].equals("-properties")){
                                propsFile = args[6];
                                // run evogpj with properties file and modified properties
                                gpfunction = new ClassGPFunction(props,propsFile,numMinutes*60);
                                gpfunction.run_population();
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

        //java -jar gpfunction.jar -predictions path_to_train_data path_to_test_data -model path_to_model -o path_to_predictions
    public void parseGPFunctionPredictions(String args[]) throws IOException, ClassNotFoundException{
        String testDataPath, predPath;
        String popPath;
        if (args.length==6){
            testDataPath = args[1];
            if(args[2].equals("-model")){
                popPath = args[3];
                if(args[4].equals("-o")){
                    predPath = args[5];
                    if(new File(popPath).isFile()){
                        TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(testDataPath,popPath);
                        tsm.predictionsSingleModel(predPath);
                        //tsm.outputSingleModel(predPath);
                    }
                }else{
                    System.err.println("Error: wrong argument. Expected -o flag");
                    printUsage();
                    System.exit(-1); 
                }
                
            }else{
                System.err.println("Error: wrong argument. Expected -model flag");
                printUsage();
                System.exit(-1);
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    //java -jar gpfunction.jar -predictions path_to_train_data path_to_test_data -model path_to_model -o path_to_predictions
    public void parseGPFunctionPredictionsSeveralModels(String args[]) throws IOException, ClassNotFoundException{
        String testDataPath, predPath;
        String popPath;
        if (args.length==6){
            testDataPath = args[1];
            if(args[2].equals("-model")){
                popPath = args[3];
                if(args[4].equals("-o")){
                    predPath = args[5];
                    if(new File(popPath).isFile()){
                        TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(testDataPath,popPath);
                        tsm.predictionsPop(predPath);
                    }
                }else{
                    System.err.println("Error: wrong argument. Expected -o flag");
                    printUsage();
                    System.exit(-1); 
                }
                
            }else{
                System.err.println("Error: wrong argument. Expected -model flag");
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
        String testDataPath;
        String popPath;
        if (args.length==2){
            testDataPath = args[1];
            // check if knee model exists
            popPath = "bestCrossValidation.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING BEST MODEL ON CROSS VALIDATION:");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(testDataPath,popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "knee.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING KNEE MODEL:");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "pareto.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING PARETO FRONT: ");
                TestGPFunctionClassifiers tsm = new TestGPFunctionClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException{
        GPFunctionMenuManager m = new GPFunctionMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            switch (args[0]) {
                case "-train":
                    m.parseGPFunctionTrain(args);
                    break;
                case "-test":
                    m.parseGPFunctionTest(args);
                    break;
                case "-predictSeveral":
                    m.parseGPFunctionPredictions(args);
                    break;
                case "-predict":
                    m.parseGPFunctionPredictions(args);
                    break;
                default:
                    System.err.println("Error: unknown argument");
                    m.printUsage();
                    System.exit(-1);
                    break;
            }
        }
    }
}
