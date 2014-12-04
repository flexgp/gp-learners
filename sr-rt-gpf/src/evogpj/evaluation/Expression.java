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

package evogpj.evaluation;

import java.util.ArrayList;

/**
 * Class than encapsulates an arithmetic expression
 * @author Ignacio Arnaldo
 */
public class Expression {
    
    String prefixExpression;
    String infixExpression;
    String postfixExpression;
    String intermediateCode;
    ArrayList<String> alFeatures, alOps, alUnaryOps;
    int auxVarCounter,indexFeatures;
    ArrayList<Integer> alCodedRPN;
        
    public Expression(){
        prefixExpression = "";
        infixExpression = "";
        postfixExpression = "";
        intermediateCode = "";
        alFeatures = new ArrayList<String>();
        indexFeatures = 0;
        alOps = new ArrayList<String>();
        alUnaryOps = new ArrayList<String>();
        auxVarCounter = 0;
        alCodedRPN = new ArrayList<Integer>();
    }
    
    public void initOps(String[] ops){
        alOps = new ArrayList<String>();
        for(int i=0;i<ops.length;i++){
            alOps.add(i, ops[i]);
        }
    }
        
    public void initUnOps(String[] unops){
        alUnaryOps = new ArrayList<String>();
        for(int i=0;i<unops.length;i++){
            alUnaryOps.add(i, unops[i]);
        }
    }
    
    public void setOps(ArrayList<String> someOps){
        alOps = someOps;
    }
    
    public ArrayList<String> getAlOps(){
        return alOps;
    }
    
    public void setUnOps(ArrayList<String> someUnOps){
        alUnaryOps = someUnOps;
    }
    
    
    public void resetIntermediateCode(){
        intermediateCode = "";
    }
    
    public boolean unaryOperator(String anOperator){
        boolean isUnary= false;
        if(alUnaryOps.contains(anOperator)){
            isUnary = true;
        }
        return isUnary;
    }
    
    public void addFeature(String aFeature){
        if(!alFeatures.contains(aFeature)){
            alFeatures.add(aFeature);
            indexFeatures++;
        }
    }
    
    public void incrementVarCounter(){
        auxVarCounter++;
    }
        
    public void setPrefixExpression(String aPrefixExpression){
        aPrefixExpression = aPrefixExpression.replace("))", ") )");
        aPrefixExpression = aPrefixExpression.replace("))", ") )");
        prefixExpression = aPrefixExpression;
    }
    
    public void printCodedRPN(){
        for(int i=0;i<alCodedRPN.size();i++){
            System.out.print(alCodedRPN.get(i) + " ");
        }
        System.out.println();
    }
    
    public ArrayList<Integer> getAlCodedRpn(){
        return alCodedRPN;
    }
    
    public void setCodedToken(int index,int token){
        alCodedRPN.add(index, token);
    }    
        
    public String getPrefixExpression(){
        return prefixExpression;
    }
    
    public void setInfixExpression(String anInfixExpression){
        infixExpression = anInfixExpression;
    }
    
    public String getInfixExpression(){
        return infixExpression;
    }
    
    public String getPostfixExpression(){
        return postfixExpression;
    }
    
    public void setPostfixExpression(String anExpression){
        postfixExpression = anExpression;
    }
    public void setIntermediateCode(String anIntermediateCode){
        intermediateCode = anIntermediateCode;
    }
    
    public String getIntermediateCode(){
        return intermediateCode;
    }
    
    public void setFeatures(ArrayList<String> someFeatures){
        alFeatures = someFeatures;
    }
    
    public ArrayList<String> getFeatures(){
        return alFeatures;
    }
    
    public void setVarCounter(int aCounter){
        auxVarCounter = aCounter;
    }
    
    public int getVarCounter(){
        return auxVarCounter;
    }
}
