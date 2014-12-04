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
package evogpj.bool;

import evogpj.preprocessing.Interval;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * abstract class to implement boolean functions
 * 
 * @author Ignacio Arnaldo
 */
public abstract class BooleanFunction {

    /**
     * Given the set of assignments of features to values for a particular
     * training case, return the double result of evaluating this function on
     * that training case.
     * 
     * @param t the training case to evaluate.
     * @param conditions conditions found in the preprocessing step
     * @return value computed by applying this function to the training case.
     */
    public abstract Boolean eval(List<Double> t,ArrayList<Interval> conditions);

    /**
     * Encapsulate the mapping from a textual label to a Function object's
     * class. To be used for introspectively determining how to generate a
     * Function for evaluation.
     * 
     * @param label the string from an S-expression encoding a particular
     *        function
     * @return the class of the function encoded in label.
     */
    public static Class<? extends BooleanFunction> getClassFromLabel(String label) {
        if (label.startsWith("C") || label.startsWith("c")) {
            return Condition.class;
        } else if (label.equals("not") || label.equals("Not") || label.equals("NOT")) {
            return Not.class;
        } else if (label.equals("and") || label.equals("And") || label.equals("AND")) {
            return And.class;
        } else if (label.equals("or") || label.equals("Or") || label.equals("OR")) {
            return Or.class;
        } else {
            return BooleanConst.class;
        }
    }

        
    /**
     * Given a label, return the constructor for the class of the function which
     * represents the label.
     * 
     * @param label
     * @return
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public static Constructor<? extends BooleanFunction> getConstructorFromLabel(String label) throws SecurityException, NoSuchMethodException {
        Class<? extends BooleanFunction> f = BooleanFunction.getClassFromLabel(label);
        int arity = BooleanFunction.getArityFromLabel(label);
        if (arity == 1) {
            return f.getConstructor(BooleanFunction.class);
        } else if (arity == 2) {
            return f.getConstructor(BooleanFunction.class, BooleanFunction.class);
        }
        return f.getConstructor(String.class);
    }

    /**
     * Simple method for extracting the arity of the function (number of args
     * the function takes) encoded by the provided label string by introspecting
     * on the function class represented by the label.
     * 
     * @param label string of function, from an S-expression.
     * @return arity of encoded function
     */
    public static int getArityFromLabel(String label) {
        Class<? extends BooleanFunction> f = BooleanFunction.getClassFromLabel(label);
        if (OneArgBooleanFunction.class.isAssignableFrom(f)) {
            return 1;
        } else if (TwoArgBooleanFunction.class.isAssignableFrom(f)) {
            return 2;
        } else {
            // Conditions and default case
            return 0;
        }
    }

    /**
     * @return an infix format string representing this function, with #arity %s inclusions.
     */
    public static String getPrefixFormatString(){
        return "";
    }
    
    /**
     * @return an infix format string representing this function, with #arity %s inclusions.
     */
    public static String getInfixFormatString(){
        return "";
    }
    
    
}