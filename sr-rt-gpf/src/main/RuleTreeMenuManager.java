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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import evogpj.algorithm.ClassRuleTree;
import evogpj.algorithm.Parameters;
import evogpj.preprocessing.DataJavaParser;
import evogpj.test.TestRuleTrees;

/**
 * wrapper class to parse the command line interface of the ruleTree learner
 * @author Ignacio Arnaldo
 */
public class RuleTreeMenuManager {
    
    public RuleTreeMenuManager(){
        
    }
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("TRAIN:");
        System.err.println("java -jar ruletree.jar -train path_to_data -minutes minutes [-properties path_to_properties]");
        System.err.println();
        System.err.println("TEST:");
        System.err.println("java -jar ruletree.jar -test path_to_data");
        System.err.println();
        System.err.println("OBTAIN PREDICTIONS:");
        System.err.println("java -jar ruletree.jar -predict path_to_data -model path_to_model -o path_to_predictions");
        System.err.println();
        System.err.println("Crossval:");
        System.err.println("java -jar ruletree.jar -crossval num_of_fold path_to_data -minutes minutes [-properties path_to_properties]");
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
            props.put(Parameters.Names.FITNESS, Parameters.Operators.RT_MO_JAVA_FITNESS + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
            if (args[2].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[3]);
                if(args.length==4){
                    ruleTree = new ClassRuleTree(props,numMinutes*60);
                    ruleTree.run_population();
                }else if(args.length==6){
                    if(args[4].equals("-properties")){
                        propsFile = args[5];
                        // run evogpj with properties file and modified properties
                        ruleTree = new ClassRuleTree(props,propsFile,numMinutes*60);
//                        Individual bestIndi = ruleTree.run_population();
                        ruleTree.run_population();
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
    
    //java -jar ruletree.jar -predict path_to_data -model path_to_model -o filename
    public void parseRuleTreePredictions(String args[]) throws IOException, ClassNotFoundException{
        String dataPath, popPath, predPath;
        if (args.length==6){
            // by default integer targets = false
            dataPath = args[1];
            if(args[2].equals("-model")){
                popPath = args[3];
                if(args[4].equals("-o")){
                    predPath = args[5];
                    if(new File(popPath).isFile()){
                        System.out.println("TESTING " + popPath + ":");
                        TestRuleTrees trt = new TestRuleTrees(dataPath, popPath);
                        trt.predictionsPop(predPath);
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
        }else {
            System.err.println();
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    //java -jar sr.jar -test path_to_data 
    public void parseRuleTreeTest(String args[]) throws IOException, ClassNotFoundException{
        String dataPath, popPath;
        if (args.length==2){
            // by default integer targets = false
            dataPath = args[1];
            // check if knee model exists
            popPath = "knee.txt";
            System.out.println();
            if(new File(popPath).isFile()){
                System.out.println("TESTING KNEE MODEL:");
                TestRuleTrees trt = new TestRuleTrees(dataPath, popPath);
                trt.evalPop();
                System.out.println();
            }
            popPath = "mostAccurate.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING MOST ACCURATE MODEL: ");
                TestRuleTrees trt = new TestRuleTrees(dataPath, popPath);
                trt.evalPop();
                System.out.println();
            }
            popPath = "leastComplex.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING SIMPLEST MODEL: ");
                TestRuleTrees trt = new TestRuleTrees(dataPath, popPath);
                trt.evalPop();
                System.out.println();
            }
            popPath = "pareto.txt";
            if(new File(popPath).isFile()){
                System.out.println("TESTING PARETO FRONT: ");
                TestRuleTrees trt = new TestRuleTrees(dataPath, popPath);
                trt.evalPop();
                System.out.println();
            } 
        }else {
            System.err.println();
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
        
    }
    
    public void parseRuleTreeCrossVal(String[] args) throws IOException, ClassNotFoundException{
        String dataPath, propsFile;
        int numMinutes;
        int numOfFolds;
        ClassRuleTree ruleTree;
        if(args.length==5 || args.length==7){
            numOfFolds = Integer.parseInt(args[1]);
            dataPath = args[2];
            DataJavaParser parser = new DataJavaParser(dataPath,numOfFolds);
            /**
             * data.get(0) : train data
             * data.get(1) : test data
             */
            ArrayList<List<String>> data = new ArrayList<List<String>>();
            Properties props = new Properties();
            props.put(Parameters.Names.PROBLEM, dataPath);
            props.put(Parameters.Names.FITNESS, Parameters.Operators.RT_MO_JAVA_FITNESS + ", " + Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS);
            if (args[3].equals("-minutes")) {
                numMinutes = Integer.valueOf(args[4]);
                if(args.length==5){
                    String dirPath;
                    for(int i=0;i<numOfFolds;i++){
                        data=parser.generateDataSet(i);
                        dirPath = "crossval/"+Integer.toString(i);
                        File dir = new File(dirPath);
                        dir.mkdirs();
                        dirPath=dirPath+"/";
                        ruleTree = new ClassRuleTree(props,numMinutes*60, data.get(0),dirPath);
                        System.out.println("Running ClassRuleTree");
                        ruleTree.run_population();

                        String popPath = dirPath + "mostAccurate.txt";

                        if(new File(popPath).isFile()){
                            System.out.println("TESTING MOST ACCURATE MODEL: ");
                            TestRuleTrees trt = new TestRuleTrees(data.get(1), dirPath, popPath);
                            //Write it to a log file
                            trt.evalPop(true);
                            System.out.println();
                        }
                    }
                }else if(args.length==7){
                    if(args[5].equals("-properties")){
                        String dirPath;
                        propsFile = args[6];
                        for(int i=0;i<numOfFolds;i++){
                            data=parser.generateDataSet(i);
                            dirPath = "crossval/"+Integer.toString(i);
                            File dir = new File(dirPath);
                            dir.mkdirs();
                            dirPath=dirPath+"/";
                            ruleTree = new ClassRuleTree(props, propsFile, numMinutes*60, data.get(0),dirPath);
                            ruleTree.run_population();

                            String popPath = dirPath + "mostAccurate.txt";
                            if(new File(popPath).isFile()){
                                System.out.println("TESTING MOST ACCURATE MODEL: ");
                                TestRuleTrees trt = new TestRuleTrees(data.get(1), dirPath, popPath);
                                //Write it to a log file
                                trt.evalPop(true);
                                System.out.println();
                            }
                        }

                        dirPath = "crossval/"+Integer.toString(numOfFolds);
                        File dir = new File(dirPath);
                        dir.mkdirs();

                        dirPath=dirPath+"/";
                        ruleTree= new ClassRuleTree(props,propsFile, numMinutes*60,null,dirPath);
                        ruleTree.run_population();

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
    
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        RuleTreeMenuManager m = new RuleTreeMenuManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            m.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-train")) {
                m.parseRuleTreeTrain(args);
            }else if(args[0].equals("-predict")){
                m.parseRuleTreePredictions(args);
            }else if(args[0].equals("-test")){
                m.parseRuleTreeTest(args);
            }else if(args[0].equals("-crossval")){
            	m.parseRuleTreeCrossVal(args);
            }else{
                System.err.println("Error: unknown argument");
                m.printUsage();
                System.exit(-1);
            }
        }
    }
}
