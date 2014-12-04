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

import evogpj.postprocessing.FusePredictions;
import java.io.IOException;

/**
 * wrapper class to parse the command line to fuse binary classifiers via majority vote 
 * @author Ignacio Arnaldo
 */
public class FuseClassifiersManager {
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("MAJORITY VOTE:");
        System.err.println("java -jar majorityvote.jar -csv path_to_filtered fnweight");
        System.err.println();

    }
    
   
    //java -jar majorityvote.jar -csv path_to_filtered fnweight
    public void parseMajorityVote(String args[]) throws IOException{
        String path_to_csv;
        double  fnw;
        if(args.length==3){
            path_to_csv = args[1];
            fnw = Double.parseDouble(args[2]);
            FusePredictions fp = new FusePredictions(path_to_csv,fnw);
            fp.computeStatsMajorityVote();
            
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }

        
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        FuseClassifiersManager f = new FuseClassifiersManager();
        if (args.length == 0) {
            System.err.println("Error: too few arguments");
            f.printUsage();
            System.exit(-1);
        }else{
            if (args[0].equals("-csv")) {
                    f.parseMajorityVote(args);
            }else{
                System.err.println("Error: unknown argument");
                f.printUsage();
                System.exit(-1);
            }
        }
    }
}
