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
 */
package evogpj.postprocessing;

import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import evogpj.evaluation.java.GPFunctionJava;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Compute the correlation between binary classifiers
 * 
 * @author Ignacio Arnaldo
 */
public class CorrelationsClassifiers {
    
    private String pathToCsv;
    private final DataJava preds;
    double[][] corrMatrix;
    int numClassifiers, numExemplars;
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToCsv
     * @throws java.io.IOException
     */   
    public CorrelationsClassifiers(String aPathToCsv) throws IOException {
        pathToCsv = aPathToCsv;
        preds = new CSVDataJava(pathToCsv);
        numClassifiers = preds.getNumberOfFeatures();
        numExemplars = preds.getNumberOfFitnessCases();
        corrMatrix = new double[numClassifiers][numClassifiers];
    }

    
    public void computeCorrelations() {
        int numThreads = 8;
        ArrayList<CCThread> alThreads = new ArrayList<CCThread>();
        for(int i=0;i<numThreads;i++){
            CCThread threadAux = new CCThread(i, numThreads);
            alThreads.add(threadAux);
        }
        
        for(int i=0;i<numThreads;i++){
            CCThread threadAux = alThreads.get(i);
            threadAux.start();
        }
        
        for(int i=0;i<numThreads;i++){
            CCThread threadAux = alThreads.get(i);
            try {
                threadAux.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(GPFunctionJava.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for(int i=0;i<numClassifiers;i++){
            for(int j=0;j<numClassifiers-1;j++){
                System.out.print(corrMatrix[i][j]+",");
            }
            System.out.print(corrMatrix[i][numClassifiers-1]+"\n");
        }
    }
    
    public void computeCorrelationClassifer(int i) {
        
        // COMPUTE COST FOR ALL CLASSIFIERS ON TRAIN SET
        double[][] predsMatrix = preds.getInputValues();
        double[] trueLabels = preds.getTargetValues();
        
        
        //System.out.println("Classifier " + i);
        for(int j=i;j<numClassifiers;j++){
            double n00 = 0;
            double n01 = 0;
            double n10 = 0;
            double n11 = 0;

            for(int z=0;z<numExemplars;z++){
                double dAux =predsMatrix[z][i];
                double dAux2 =predsMatrix[z][j];
                double dAux3 =trueLabels[z];
                if(predsMatrix[z][i] == trueLabels[z]){
                    if(predsMatrix[z][j]==trueLabels[z]){
                        n11 = n11 + 1;
                    }else{
                        n10 = n10 + 1;
                    }
                }else{
                    if(predsMatrix[z][j]==trueLabels[z]){
                        n01 = n01 + 1;
                    }else{
                        n00 = n00 +1;
                    }                           
                }
            }
            //double cij = (n11*n00 - n01*n10) / Math.sqrt((n11+n10)*(n01+n00)*(n11+n01)*(n10+n00));
            //double cij = (n11*n00 - n01*n10) / (n11*n00 + n01*n10);//q statistic
            //double cij = (n01 + n10) / (n11 + n10 + n01 + n00);//disagreement measure
            double cij = (n01 + n10 + n11) / (n11 + n10 + n01 + n00);//complementary measure

            /*if(Double.isNaN(cij) || Double.isInfinite(cij)){
                cij = 0;
            }*/
            corrMatrix[i][j]=cij;
            corrMatrix[j][i]=cij;

        }


    }
    

    public class CCThread extends Thread{
        private int indexThread, totalThreads;
        
        
        public CCThread(int anIndex, int aTotalThreads){
            indexThread = anIndex;
            totalThreads = aTotalThreads;
        }

        @Override
        public void run(){
            for (int i=0;i<numClassifiers;i++) {
                if(i%totalThreads==indexThread){
                    if(indexThread==0){
                        System.out.println("Computing classifier " + i);
                    }
                    computeCorrelationClassifer(i);
                }
            }
        }
     }

}