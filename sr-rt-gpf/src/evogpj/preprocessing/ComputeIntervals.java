/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package evogpj.preprocessing;

import evogpj.evaluation.java.DataJava;
import java.util.ArrayList;

/**
 *
 * @author nacho
 */
public class ComputeIntervals {
    
    DataJava data;
    int numberOfSteps;
    double minVariable,maxVariable;
    
    public ComputeIntervals(DataJava aD,int aNumberOfSteps){
        data = aD;
        numberOfSteps = aNumberOfSteps;
        minVariable = Double.MAX_VALUE;
        maxVariable = - Double.MAX_VALUE;
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
                if(posExtrapolated[i]>negExtrapolated[i]){
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
                if(posExtrapolated[i]<negExtrapolated[i]){
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
