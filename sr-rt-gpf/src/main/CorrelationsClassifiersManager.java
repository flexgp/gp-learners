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
 * @author Ignacio Arnaldo
 */

package main;

import evogpj.postprocessing.CorrelationsClassifiers;
import java.io.IOException;

/**
 * wrapper class to parse the command line to compute correlations between binary classifiers
 * @author Ignacio Arnaldo
 */
public class CorrelationsClassifiersManager {
    
    public void printUsage(){
        System.err.println();
        System.err.println("USAGE:");
        System.err.println();
        System.err.println("COMPUTE CORRELATIONS:");
        System.err.println("java -jar corrs.jar -csv path_to_preds");
        System.err.println();

    }
    
   
    //java -jar majorityvote.jar -csv path_to_filtered fnweight
    public void parseMajorityVote(String args[]) throws IOException{
        String path_to_csv;
        double  fnw;
        if(args.length==2){
            path_to_csv = args[1];
            
            CorrelationsClassifiers fp = new CorrelationsClassifiers(path_to_csv);
            fp.computeCorrelations();
            
        }else{
            System.err.println("Error: wrong number of arguments");
            printUsage();
            System.exit(-1);
        }
    }

        
    public static void main(String args[]) throws IOException, ClassNotFoundException{
        CorrelationsClassifiersManager f = new CorrelationsClassifiersManager();
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
