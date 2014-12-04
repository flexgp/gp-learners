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
 * 
 */
package main;

import evogpj.algorithm.ClassGPFunctionKDE;
import evogpj.algorithm.Parameters;
import evogpj.test.TestGPFunctionKDEClassifiers;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * wrapper class to parse the command line interface of the MAP GPFunction learner
 * @author Ignacio Arnaldo
 */
public class GPFunctionKDEMenuManager {
    
    public GPFunctionKDEMenuManager(){
        
    }
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar gpfunctionkde.jar -train path_to_data -minutes min [-properties path_to_properties]");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar gpfunctionkde.jar -test path_to_test_data");
        System.err.println();
        System.err.println("OBTAIN PREDICTIONS:");
        System.err.println("java -jar gpfunctionkde.jar -predict path_to_test_data -model path_to_model -o path_to_predictions");
        System.err.println();
    }
    
    public void parseGPFunctionKDETrain(String args[]) throws IOException{
        String dataPath;
        int numMinutes;
        String propsFile;
        ClassGPFunctionKDE gpfunctionkde;
        if(args.length==4 || args.length==6){
            dataPath = args[1];
            // run evogpj with standard properties
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            if (args[2].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[3]);
                if(args.length==4){
                    props.put(Parameters.Names.FITNESS, Parameters.Operators.GPFUNCTION_KDE_JAVA + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                    //props.put(Parameters.Names.EXTERNAL_THREADS, "1");
                    gpfunctionkde = new ClassGPFunctionKDE(props,numMinutes*60);
                    gpfunctionkde.run_population();
                }else if(args.length==6){
                    if(args[4].equals("-properties")){
                        propsFile = args[5];
                        // run evogpj with properties file and modified properties
                        props.put(Parameters.Names.FITNESS, Parameters.Operators.GPFUNCTION_KDE_JAVA + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
                        gpfunctionkde = new ClassGPFunctionKDE(props,propsFile,numMinutes*60);
                        gpfunctionkde.run_population();
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
    
    //java -jar gpfunction.jar -test path_to_test_data
    public void parseGPFunctionKDETest(String args[]) throws IOException, ClassNotFoundException{
        String testDataPath;
        String popPath;
        if (args.length==2){
            testDataPath = args[1];
            popPath = "bestCrossValidation.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING BEST MODEL ON CROSS VALIDATION:");
                TestGPFunctionKDEClassifiers tsm = new TestGPFunctionKDEClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "knee.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING KNEE MODEL:");
                TestGPFunctionKDEClassifiers tsm = new TestGPFunctionKDEClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestGPFunctionKDEClassifiers tsm = new TestGPFunctionKDEClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestGPFunctionKDEClassifiers tsm = new TestGPFunctionKDEClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
            popPath = "pareto.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING PARETO FRONT: ");
                TestGPFunctionKDEClassifiers tsm = new TestGPFunctionKDEClassifiers(testDataPath, popPath);
                tsm.evalPop();
                System.out.println();
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }

    //java -jar gpfunction.jar -predictions path_to_train_data path_to_test_data -model path_to_model -o path_to_predictions
    public void parseGPFunctionKDEPredictions(String args[]) throws IOException, ClassNotFoundException{
        String testDataPath, predPath;
        String popPath;
        if (args.length==6){
            testDataPath = args[1];
            if(args[2].equals("-model")){
                popPath = args[3];
                if(args[4].equals("-o")){
                    predPath = args[5];
                    if(new File(popPath).isFile()){
                        TestGPFunctionKDEClassifiers tsm = new TestGPFunctionKDEClassifiers(testDataPath,popPath);
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

    public static void main(String args[]) throws IOException, ClassNotFoundException, InterruptedException{
        GPFunctionKDEMenuManager m = new GPFunctionKDEMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            if(args[0].equals("-train")) {
                m.parseGPFunctionKDETrain(args);
            }else if(args[0].equals("-test")){
                m.parseGPFunctionKDETest(args);
            }else if(args[0].equals("-predict")){
                m.parseGPFunctionKDEPredictions(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
                System.exit(-1);
            }
        }
    }
}
