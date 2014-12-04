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

import evogpj.postprocessing.FilterPredictions;
import java.io.IOException;

/**
 * wrapper class to parse the command line to filter binary classifiers
 * @author Ignacio Arnaldo
 */
public class FilterClassifiersManager {
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("FILTER:");
        System.err.println("java -jar filter.jar -filter path_to_train_preds -test path_to_test_preds -filterTrain path_to_filtered_train -filterTest path_to_filter_test -indices path_to_indices -fnweight fnw");
        System.err.println();
        System.err.println("COST ALL:");
        System.err.println("java -jar filter.jar -costall path_to_train_preds path_to_test_preds fnw");
        
        System.err.println("COST FILTERED:");
        System.err.println("java -jar filter.jar -costfiltered path_to_train_preds path_to_test_preds fnw");
        System.err.println();
    }
    
    public void parseFilterClassifiers(String args[]) throws IOException{
        String path_to_train_preds, path_to_test_preds, path_to_filtered_train, path_to_filter_test, path_to_indices;
        double  fnw;
        if(args.length==12){
            path_to_train_preds = args[1];
            // run evogpj with standard properties
            if(args[2].equals("-test")){
                path_to_test_preds = args[3];
                if (args[4].equals("-filterTrain")) {
                    path_to_filtered_train = args[5];
                    if (args[6].equals("-filterTest")) {
                        path_to_filter_test = args[7];
                        if (args[8].equals("-indices")) {
                            path_to_indices = args[9];
                            if (args[10].equals("-fnweight")) {
                                fnw = Double.parseDouble(args[11]);
                                FilterPredictions fp = new FilterPredictions(path_to_train_preds, path_to_test_preds, path_to_filtered_train, path_to_filter_test, path_to_indices, fnw);
                                fp.setMajorityVote();
                                fp.setMajorityCost();
                                fp.filterModels();
                                fp.saveFilteredModels();
                            }else{
                            System.err.println("Error: wrong argument. Expected -fnweight flag");
                            printUsage();
                            System.exit(-1);
                            }
                        }else{
                            System.err.println("Error: wrong argument. Expected -fnweight flag");
                            printUsage();
                            System.exit(-1);
                        }
                    }else{
                        System.err.println("Error: wrong argument. Expected -filterTest flag");
                        printUsage();
                        System.exit(-1);
                    }
                    
                }else{
                    System.err.println("Error: wrong argument. Expected -filterTrain flag");
                    printUsage();
                    System.exit(-1);
                } 
            }else{
                System.err.println("Error: wrong argument. Expected -test flag");
                printUsage();
                System.exit(-1);
            }
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }
    
    //java -jar filter.jar -costall path_to_train_preds path_to_test_preds fnw
    public void parseCostAllClassifiers(String args[]) throws IOException{
        String path_to_train_preds, path_to_test_preds;
        double  fnw;
        if(args.length==4){
            path_to_train_preds = args[1];
            path_to_test_preds = args[2];
            fnw = Double.parseDouble(args[3]);
            FilterPredictions fp = new FilterPredictions(path_to_train_preds,path_to_test_preds, fnw);
            fp.computeCostAll();
            
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }

    //java -jar filter.jar -costfiltered path_to_test_preds path_to_test_preds fnw
    public void parseCostFilteredClassifiers(String args[]) throws IOException{
        String path_to_train_preds, path_to_test_preds;
        double  fnw;
        if(args.length==4){
            path_to_train_preds = args[1];
            path_to_test_preds = args[2];
            fnw = Double.parseDouble(args[3]);
            FilterPredictions fp = new FilterPredictions(path_to_train_preds, path_to_test_preds,  fnw);
            fp.computeCostFiltered();
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }    
        
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        FilterClassifiersManager f = new FilterClassifiersManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            f.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-filter")) {
                f.parseFilterClassifiers(args);
            }else if (args[0].equals("-costall")) {
                f.parseCostAllClassifiers(args);
            }else if (args[0].equals("-costfiltered")) {
                f.parseCostFilteredClassifiers(args);
            }else{
                System.err.println("Error: unknown argument");
                f.printUsage();
                System.exit(-1);
            }
        }
    }
}
