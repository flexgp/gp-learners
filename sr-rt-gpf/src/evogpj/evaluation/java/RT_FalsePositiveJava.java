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
package evogpj.evaluation.java;

import java.util.ArrayList;
import java.util.List;


import evogpj.genotype.Tree;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import evogpj.algorithm.Parameters;
import evogpj.bool.BooleanFunction;
import evogpj.evaluation.FitnessFunction;
import evogpj.preprocessing.Interval;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements false positive fitness function.
 * 
 * @author Ignacio Arnaldo
 */
public class RT_FalsePositiveJava extends FitnessFunction {

    public static String FITNESS_KEY = Parameters.Operators.RT_FP_JAVA_FITNESS;
    
    private final DataJava data;
    ArrayList<Interval> intervals;
    private int numThreads;
    public Boolean isMaximizingFunction = false;
    

	
    /**
     * Create a new fitness operator, using the provided data, for assessing
     * individual solutions to classification problems. There are two
     * parameters for this fitness evaluation:
     * @param data
     *            The dataset (training cases, output variable) to use in
     *            computing the fitness of individuals.
     * @param intervals
     *            intervals obtained for each variable in the preprocessing step
     */
    public RT_FalsePositiveJava(DataJava aData,ArrayList<Interval> aIntervals, int aNumThreads) {
        this.data = aData;
        this.intervals = aIntervals;
        this.numThreads = aNumThreads;
    }

    /**
     * Should this fitness function be minimized (i.e. mean squared error) or
     * maximized?
     */
    @Override
    public Boolean isMaximizingFunction() {
        return this.isMaximizingFunction;
    }

    /**
     * @see Function
     */
    public void eval(Individual ind) {
        Tree genotype = (Tree) ind.getGenotype();
        BooleanFunction func = genotype.generateBoolean();
        List<Double> d;
        double[][] inputValuesAux = data.getInputValues();
        double[] targets = data.getTargetValues();
        double numNegatives = 0;
        double numPositives = 0;
        double numFalseNegatives = 0;
        double numFalsePositives = 0;
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            boolean target = false;
            if(targets[i]==1) target = true;
            d = new ArrayList<Double>();
            for (int j = 0; j < data.getNumberOfFeatures(); j++) {
                d.add(j, inputValuesAux[i][j]);
            }
            boolean val = func.eval(d,intervals);
            if(targets[i]==1){
                numPositives++;
            }else if(targets[i]==0){
                numNegatives++;
            }
            if(val==true && target==false) {
                numFalsePositives++;
            }else if(val==false && target==true){
                numFalseNegatives++;
            }
            d.clear();
        }
        double falsePositiveRate = numFalsePositives / numNegatives;
        ind.setFitness(RT_FalsePositiveJava.FITNESS_KEY, falsePositiveRate);
        double falseNegativeRate = numFalseNegatives / numPositives;
        ind.setFitness(RT_FalseNegativeJava.FITNESS_KEY, falseNegativeRate);
        func = null;
    }

    @Override
    public void evalPop(Population pop) {
        
        ArrayList<TBRCJavaThread> alThreads = new ArrayList<TBRCJavaThread>();
        for(int i=0;i<numThreads;i++){
            TBRCJavaThread threadAux = new TBRCJavaThread(i, pop,numThreads);
            alThreads.add(threadAux);
        }
        
        for(int i=0;i<numThreads;i++){
            TBRCJavaThread threadAux = alThreads.get(i);
            threadAux.start();
        }
        
        for(int i=0;i<numThreads;i++){
            TBRCJavaThread threadAux = alThreads.get(i);
            try {
                threadAux.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(RT_FalsePositiveJava.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        

    }

    /**
     * @return the fitnessCases
     */
    public double[][] getFitnessCases() {
        return data.getInputValues();
    }
    
    
    public class TBRCJavaThread extends Thread{
        private int indexThread, totalThreads;
        private Population pop;
        
        public TBRCJavaThread(int anIndex, Population aPop,int aTotalThreads){
            indexThread = anIndex;
            pop = aPop;
            totalThreads = aTotalThreads;
        }

        @Override
        public void run(){
            int indexIndi = 0;
            for (Individual individual : pop) {
                if(indexIndi%totalThreads==indexThread){
                    eval(individual);
                }
                indexIndi++;
            }
        }
        
        
     }
}
