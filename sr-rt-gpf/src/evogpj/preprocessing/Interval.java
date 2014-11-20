/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package evogpj.preprocessing;

/**
 *
 * @author nacho
 */
public class Interval {
    
    String variable;
    double lb,ub;
    public Interval(String aVar){
        variable = aVar;
        lb = - Double.MAX_VALUE;
        ub = Double.MAX_VALUE;
    }
    
    public Interval(String aVar,double aLb,double aUb){
        variable = aVar;
        lb = aLb;
        ub = aUb;
    }
    
    public String getVariable(){
        return variable;
    }
    public void setLb(double aLb){
        lb = aLb;
    }
    
    public double getLb(){
        return lb;
    }
    
    public void setUb(double aUb){
        ub = aUb;
    }
    
    public double getUb(){
        return ub;
    }
    
}
