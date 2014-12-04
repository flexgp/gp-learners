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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


/**
 * Posthoc analysis of the accuracy of binary classifiers
 * 
 * @author Ignacio Arnaldo
 */
public class SelectTop10Pairs {
    
    private String pathToCsv;
    double[][] compMatrix;
    int numClassifiers;
    
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to Symbolic Regression problems. There is one
     * parameter for this fitness evaluation:
     * @param aPathToCsv
     * @throws java.io.IOException
     */   
    public SelectTop10Pairs(String aPathToCsv) throws IOException {
        pathToCsv = aPathToCsv;
        BufferedReader f = new BufferedReader(new FileReader(pathToCsv));
        int numLines=0;
        String[] tokens=null;
        while (f.ready()) {
            tokens = f.readLine().split(",");
            numLines++;
        }
        int numCols = tokens.length;
        compMatrix = new double[numLines][numCols];
        int exemplarIndex = 0;
        f = new BufferedReader(new FileReader(pathToCsv));
        while (f.ready()) {
            tokens = f.readLine().split(",");
            for (int j = 0; j < tokens.length; j++) {
                compMatrix[exemplarIndex][j] = Double.valueOf(tokens[j]);
            }
            exemplarIndex++;
        }
        numClassifiers = numCols;
    }

    public void getTop10Comp() {
        System.out.println(compMatrix[0][1] + " == " + compMatrix[1][0]);
        System.out.println(compMatrix[2][3] + " == " + compMatrix[2][3]);
        ArrayList<MatrixCell> almc = new ArrayList<MatrixCell>();
        for(int i=0;i<numClassifiers;i++){
            for(int j=i+1;j<numClassifiers;j++){
                MatrixCell mc = new MatrixCell();
                mc.setI(i);
                mc.setJ(j);
                mc.setValue(compMatrix[i][j]);
                almc.add(mc);
            }
        }
        Collections.sort(almc);
        for(int i=1;i<=100;i++){
            System.out.println(almc.get(almc.size()-i).getI() + " " + almc.get(almc.size()-i).getJ() + " " + almc.get(almc.size()-i).getValue());
        }
    }
    
    
    public class MatrixCell implements Comparable{
        private int i;
        private int j;
        private double value;
        
        
        @Override
        public int compareTo(Object o) {
            MatrixCell mcAux = (MatrixCell)o;
            if(this.getValue()> mcAux.getValue()){
                return 1;
            }else if(this.getValue()==mcAux.getValue()){
                return 0;
            }else{
                return -1;
            }
        }

        /**
         * @return the i
         */
        public int getI() {
            return i;
        }

        /**
         * @param i the i to set
         */
        public void setI(int i) {
            this.i = i;
        }

        /**
         * @return the j
         */
        public int getJ() {
            return j;
        }

        /**
         * @param j the j to set
         */
        public void setJ(int j) {
            this.j = j;
        }

        /**
         * @return the value
         */
        public double getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(double value) {
            this.value = value;
        }
        
    }
    
    public static void main(String args[]) throws IOException{
        SelectTop10Pairs st10 = new SelectTop10Pairs(args[0]);
        st10.getTop10Comp();
    }
}