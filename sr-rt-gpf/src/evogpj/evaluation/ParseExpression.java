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
import java.util.Stack;

/**
 * Utility class to translate expressions to/from prefix, infix, postfix, intermediary code notations
 * @author Ignacio Arnaldo
 */
public class ParseExpression {
    
    Expression expression;
    ArrayList<String> FUNC_SET, UNARY_FUNC_SET;
    int numberOfFitnessCases, numberOfFeatures, numberOfResults;
    //int varCounter;
    //String intermediateCode;
        
    public ParseExpression(){
        expression = null;
    }
    
    
    
    public void setExpression(Expression anExpression){
        expression = anExpression;
        expression.resetIntermediateCode();
    }
    
    private int getSideExpression(String side, String token, String[] exprSplit,int anIndex){
        int indexSplit = anIndex;
        int countParenthesis = 1;
        side = side + " " + token;
        indexSplit++;

        // GET LEFT EXPRESSION
        while (countParenthesis != 0){
            token = exprSplit[indexSplit];
            if(token.charAt(0)=='('){
                countParenthesis++;
            }else if(token.charAt(token.length()-1)==')'){
                countParenthesis--;
            }
            side = side + " " + token;
            indexSplit++;
        }
        
        return indexSplit;
    }
    
    public void getIntermediateFromPrefix(String aPreExpression, String varResult){
        String operator;
        String left = "";
        String right = "";
        String instruction = "";
        
        
        //REMOVE WHITE SPACES AT THE BEGINNING AND AT THE END
        while (aPreExpression.charAt(0)==' ') aPreExpression = aPreExpression.subSequence(1, aPreExpression.length()).toString();
        while (aPreExpression.charAt(aPreExpression.length()-1)==' ') aPreExpression = aPreExpression.subSequence(0, aPreExpression.length()-1).toString();
        
        //REMOVE FIRST AND LAST PARENTHESIS
        while (aPreExpression.charAt(0)=='(' && aPreExpression.charAt(aPreExpression.length()-1)==')') aPreExpression = aPreExpression.subSequence(1, aPreExpression.length()-1).toString();
        
        String[] exprSplit = aPreExpression.split(" ");
        int indexSplit=0;
        
        //READ OPERATOR
        operator = exprSplit[indexSplit];
        if(!expression.getAlOps().contains(operator)){
            //NO OPERATOR, JUST A VARIABLE
            expression.addFeature(operator);
            String interCode = expression.getIntermediateCode();
            interCode += varResult + " = " + operator + "\n";
            expression.setIntermediateCode(interCode);
            
            return;
        }
        
        indexSplit++;
        
        //READ NEXT TOKEN, CAN BE A VARIABLE OR AN EXPRESSION
        String token = exprSplit[indexSplit];
        if (token.charAt(0)!='('){// Base case: it is a variable
            left = exprSplit[indexSplit];
            expression.addFeature(left);
            indexSplit++;
            if(!expression.unaryOperator(operator)){
                instruction = instruction + varResult + " = " + left + " " + operator;
            }else{
                //System.out.println(varResult + " = " + operator + " " + left);
                String interCode = expression.getIntermediateCode();
                interCode += varResult + " = " + operator + " " + left + "\n";
                expression.setIntermediateCode(interCode);
            }
            
        }else{ // Recursive case, it is an expression
            int countParenthesis = 1;
            left = left + " " + token;
            indexSplit++;
            
            // GET LEFT EXPRESSION
            while (countParenthesis != 0){
                token = exprSplit[indexSplit];
                if(token.charAt(0)=='('){
                    countParenthesis++;
                }else if(token.charAt(token.length()-1)==')'){
                    countParenthesis--;
                }
                left = left + " " + token;
                indexSplit++;
            }
            expression.incrementVarCounter();
            String leftVarResult = "Var_" + expression.getVarCounter();
            getIntermediateFromPrefix(left, leftVarResult);
            if(!expression.unaryOperator(operator)){
                instruction = varResult + " = " + leftVarResult + " " + operator;
            }else{
                //System.out.println(varResult + " = " + operator + " " + leftVarResult);
                String interCode = expression.getIntermediateCode();
                interCode += varResult + " = " + operator + " " + leftVarResult  + "\n";
                expression.setIntermediateCode(interCode);
            }
        }
        
        if(!expression.unaryOperator(operator)){
            token = exprSplit[indexSplit];
            if (token.charAt(0)!='('){// Base case: it is a variable
                right = exprSplit[indexSplit];
                expression.addFeature(right);
                instruction = instruction + " " + right;
                
                //System.out.println(instruction);
                String interCode = expression.getIntermediateCode();
                interCode += instruction + "\n";
                expression.setIntermediateCode(interCode);
            }else{
                int countParenthesis = 1;
                right = right + " " + token;
                indexSplit++;

                // GET RIGHT EXPRESSION
                while (countParenthesis != 0){
                    token = exprSplit[indexSplit];
                    if(token.charAt(0)=='('){
                        countParenthesis++;
                    }else if(token.charAt(token.length()-1)==')'){
                        countParenthesis--;
                    }
                    right = right + " " + token;
                    indexSplit++;
                }
                expression.incrementVarCounter();
                String rightVarResult = "Var_" + expression.getVarCounter();
                
                getIntermediateFromPrefix(right,rightVarResult);
                instruction = instruction + " " + rightVarResult;
                //System.out.println(instruction);
                String interCode = expression.getIntermediateCode();
                interCode += instruction + "\n";
                expression.setIntermediateCode(interCode);
            }
        }
    }
    
    public String getInfixFromPrefix(String aPreExpression) {
        String operator;
        String left = "";
        String right = "";
        String infix = "";


        //REMOVE WHITE SPACES AT THE BEGINNING AND AT THE END
        while (aPreExpression.charAt(0)==' ') aPreExpression = aPreExpression.subSequence(1, aPreExpression.length()).toString();
        while (aPreExpression.charAt(aPreExpression.length()-1)==' ') aPreExpression = aPreExpression.subSequence(0, aPreExpression.length()-1).toString();

        //REMOVE FIRST AND LAST PARENTHESIS
        while (aPreExpression.charAt(0)=='(' && aPreExpression.charAt(aPreExpression.length()-1)==')')
            aPreExpression = aPreExpression.subSequence(1, aPreExpression.length()-1).toString();

        String[] exprSplit = aPreExpression.split(" ");
        int indexSplit=0;

        //READ OPERATOR
        operator = exprSplit[indexSplit];
        indexSplit++;
        
        if(!expression.getAlOps().contains(operator)){
            //NO OPERATOR, JUST A VARIABLE
            expression.addFeature(operator);
            infix =  operator;// + "\n";
            return infix;
        }
        
        //READ NEXT TOKEN, CAN BE A VARIABLE OR AN EXPRESSION
        String token = exprSplit[indexSplit];
        if (token.charAt(0)!='('){// Base case: it is a variable
            left = exprSplit[indexSplit];
            expression.addFeature(left);
            indexSplit++;
            if(!expression.unaryOperator(operator)){
                infix +=  " ( " + left + " " + operator;
            }else{
                infix +=  " ( " + operator + " " + left + " ) " ;
            }

        }else{ // Recursive case, it is an expression
            int countParenthesis = 1;
            left = left + " " + token;
            indexSplit++;
            
            // GET LEFT EXPRESSION
            while (countParenthesis != 0){
                token = exprSplit[indexSplit];
                if(token.charAt(0)=='('){
                    countParenthesis++;
                }else if(token.charAt(token.length()-1)==')'){
                    countParenthesis--;
                }
                left = left + " " + token;
                indexSplit++;
            }
            String infixLeft = getInfixFromPrefix(left);
            if(!expression.unaryOperator(operator)){
                infix += " (  ( " + infixLeft + " ) " + operator;
            }else{
                infix += " ( " + operator + " ( " + infixLeft + " ) ) ";
                
            }
        }

        if(!expression.unaryOperator(operator)){
            token = exprSplit[indexSplit];
            if (token.charAt(0)!='('){// Base case: it is a variable
                right = exprSplit[indexSplit];
                expression.addFeature(right);
                infix +=  " " + right + " ) ";
            }else{
                int countParenthesis = 1;
                right = right + " " + token;
                indexSplit++;

                // GET RIGHT EXPRESSION
                while (countParenthesis != 0){
                    token = exprSplit[indexSplit];
                    if(token.charAt(0)=='('){
                        countParenthesis++;
                    }else if(token.charAt(token.length()-1)==')'){
                        countParenthesis--;
                    }
                    right = right + " " + token;
                    indexSplit++;
                }
                String infixRight = getInfixFromPrefix(right);
                infix += " ( " + infixRight + " ) ) " ;
            }
        }
        return infix;
    }
    
    public void getPosfixFromInfix() {
        String input = expression.getInfixExpression();
        //System.out.println(input);
        String[] inputTokens = input.split(" ");
        ArrayList<String> out = new ArrayList<String>();
        Stack<String> stack = new Stack<String>();
        for (String token : inputTokens) {
            if (expression.getAlOps().contains(token)) {
                while (!stack.empty() && expression.getAlOps().contains(stack.peek())) {
                    out.add(stack.pop());
                }
                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.empty() && !stack.peek().equals("(")) {
                    out.add(stack.pop()); // [S10]
                }
                stack.pop();
            } else if(expression.getFeatures().contains(token)){
                out.add(token);
            }
        }
        while (!stack.empty()) {
            out.add(stack.pop());
        }
        String output = "";
        for(int i=0;i<out.size();i++){
            output += out.get(i) + " ";
        }
        expression.setPostfixExpression(output);
    }
    
    public void getcodedRPN(){
        String postfix = expression.getPostfixExpression();
        String[] postfixSplit = postfix.split(" ");
        for(int i=0;i<postfixSplit.length;i++){
            String token = postfixSplit[i];
            if(expression.getFeatures().contains(token)){// it is a variable
                if(token.charAt(0)=='X'){
                    token = token.substring(1);
                    int featureIndex = Integer.parseInt(token);
                    int codedToken = featureIndex += 111111000;
                    expression.setCodedToken(i,codedToken);
                }else{
                    System.out.println("Unrecognized variable in the generation of the codedRPN");
                }
            }if(expression.getAlOps().contains(token)){
                if(token.equals("+")){
                    expression.setCodedToken(i,-111111001);
                }else if(token.equals("plus")){
                    expression.setCodedToken(i,-111111001);
                }else if(token.equals("*")){
                    expression.setCodedToken(i,-111111002);
                }else if(token.equals("times")){
                    expression.setCodedToken(i,-111111002);
                }else if(token.equals("-")){
                    expression.setCodedToken(i,-111111003);
                }else if(token.equals("minus")){
                    expression.setCodedToken(i,-111111003);
                }else if(token.equals("mydivide")){
                    expression.setCodedToken(i,-111111004);
                }
                
                else if(token.equals("sin")){
                    expression.setCodedToken(i,-111111011);
                }else if(token.equals("cos")){
                    expression.setCodedToken(i,-111111012);
                }else if(token.equals("sqrt") || token.equals("mysqrt")){
                    expression.setCodedToken(i,-111111013);
                }else if(token.equals("mylog") || token.equals("log")){
                    expression.setCodedToken(i,-111111014);
                }else if(token.equals("exp")){
                    expression.setCodedToken(i,-111111015);
                }else if(token.equals("square")){
                    expression.setCodedToken(i,-111111016);
                }else if(token.equals("cube")){
                    expression.setCodedToken(i,-111111017);
                }else if(token.equals("quart")){
                    expression.setCodedToken(i,-111111018);
                }
            }
        }
        expression.setCodedToken(postfixSplit.length,-999999999);       
    }
    
    
}
