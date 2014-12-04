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
package evogpj.preprocessing;

import evogpj.evaluation.java.DataJava;
import java.util.ArrayList;

/**
 * Split variable ranges with a technique based on Kernel Density Estimation
 * @author Ignacio Arnaldo
 */
public class ComputeIntervals {
    
    DataJava data;
    int numberOfSteps;
    double minVariable,maxVariable;
    double wPos, wNeg;
    
    public ComputeIntervals(DataJava aD,int aNumberOfSteps,double aWPos, double aWNeg){
        data = aD;
        numberOfSteps = aNumberOfSteps;
        minVariable = Double.MAX_VALUE;
        maxVariable = - Double.MAX_VALUE;
        wPos = aWPos;
        wNeg = aWNeg;
    }
    
    public ArrayList<Interval> computeIntervalsVariable(int featureIndex, String varLabel){
        
        ArrayList<Double> pos = new ArrayList<Double>();
        ArrayList<Double> neg = new ArrayList<Double>();
        
        minVariable = Double.MAX_VALUE;
        maxVariable = - Double.MAX_VALUE;
        getVariableValues(featureIndex,pos,neg);
        
        double[] posExtrapolated = new double[numberOfSteps];
        extrapolateKDE(pos,posExtrapolated);
        double[] negExtrapolated = new double[numberOfSteps];
        extrapolateKDE(neg,negExtrapolated);
        
        double stepSize = (maxVariable-minVariable +1) / numberOfSteps;
        double currentStepValue = minVariable;
        
        ArrayList<Interval> iAL = new ArrayList<Interval>();
        Interval iAux = new Interval(varLabel);
        iAux.setLb(currentStepValue);
        int currentLabel = 1;
        if(posExtrapolated[0]<negExtrapolated[0])currentLabel = 0;
        currentStepValue += stepSize; 
        for(int i=1;i<numberOfSteps;i++){
            if(currentLabel==1){
                if((wPos*posExtrapolated[i])>(wNeg*negExtrapolated[i])){
                    // keep interval
                }else{
                    // end interval and start another
                    iAux.setUb(currentStepValue);
                    iAL.add(iAux);
                    iAux = new Interval(varLabel);
                    iAux.setLb(currentStepValue);
                    currentLabel = 0;
                }
            }else if(currentLabel==0){
                if((wPos*posExtrapolated[i])<(wNeg*negExtrapolated[i])){
                    // keep interval
                }else{
                    // end interval and start another
                    iAux.setUb(currentStepValue);
                    iAL.add(iAux);
                    iAux = new Interval(varLabel);
                    iAux.setLb(currentStepValue);
                    currentLabel = 1;
                }
            }
            currentStepValue += stepSize;
        }
        iAux.setUb(currentStepValue);
        iAL.add(iAux);
        return iAL;
    }
    
    private void getVariableValues(int variableIndex,ArrayList<Double> pos, ArrayList<Double> neg){
        double[][] inputValues = data.getInputValues();
        for (int i = 0; i < data.getNumberOfFitnessCases(); i++) {
            double variableValue = inputValues[i][variableIndex];
            double trueLabel = (data.getTargetValues())[i];
            if(trueLabel==1){
                pos.add(variableValue);
            } else if(trueLabel==0){
                neg.add(variableValue);
            }
            if(variableValue>maxVariable) maxVariable = variableValue;
            if (variableValue<minVariable) minVariable = variableValue;
        }
    }
    
    private void extrapolateKDE(ArrayList<Double> variableValues, double[] extrapolatedValues){
        double stepSize = (maxVariable-minVariable+1) / numberOfSteps;
        double currentValue = minVariable;
        for(int e=0;e<extrapolatedValues.length;e++){
            double extrapolatedProb = getProbability(variableValues,currentValue);
            extrapolatedValues[e] = extrapolatedProb;
            currentValue += stepSize;
        }
    }
    
    /* Compute Probability of new value x 
     * given the KDF
     * Fx(X) = 1/nh SUM_{j=1}^{n} K( (X-dj)/h )
     * where n = sample size
     * dj point value
     * K is a Gaussian Kernel, then h = ( (4 sigma^5) / 3n )^{1/5} = 1.06 sigma n^{-1/5}
     * and K(y) = 1/( sqrt(2PI) ) * exp(-0.5*y^2)
     *  note in this case y = (X-dj)/h
     */
    private double getProbability(ArrayList<Double> variableValues, double x){
        double Fx = 0;
        int n = variableValues.size();
        double sigma = getStd(variableValues);
        double h = 1.06 * sigma * Math.pow(n, -(1/5));
        for(int i=0;i<variableValues.size();i++){
            double y = (x - variableValues.get(i)) / h;
            double Ky = ( 1/Math.sqrt(2*Math.PI) ) * (Math.exp(-0.5*Math.pow(y,2)));
            Fx += Ky;
        }
        Fx = Fx / (n*h);
        return Fx;
    }
    
    private double getStd(ArrayList<Double> variableValues){
        double sumVariableValues = 0;
        for(int i=0;i<variableValues.size();i++) sumVariableValues += variableValues.get(i);
        double averageVarValue = sumVariableValues / variableValues.size();
        double sd = 0;
        for (int i = 0; i < variableValues.size(); i++){
            sd += Math.pow(variableValues.get(i) - averageVarValue,2) / variableValues.size();
        }
        double standardDeviation = Math.sqrt(sd);
        return standardDeviation;
    }
    
}
