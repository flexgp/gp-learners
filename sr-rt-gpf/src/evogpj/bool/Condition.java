/**
 * Copyright (c) 2011-2013 Alfa Group
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

import evogpj.gp.GPException;
import evogpj.preprocessing.Interval;
import java.util.ArrayList;

import java.util.List;

/**
 * A split of the data used to form rules - only applies to the ruleTree learner
 * @author Ignacio Arnaldo
 */
public class Condition extends ZeroArgBooleanFunction {

    private final int indexCondition;

    
    public Condition(String label) throws GPException {
        super(label);
        if (label.startsWith("C") || label.startsWith("c")) {
            String numPart = label.substring(1);
            indexCondition = Integer.parseInt(numPart) - 1; // zero-index
        } else {
            throw new GPException("Unknonwn condition: " + label);
        }
    }

    @Override
    public Boolean eval(List<Double> t,ArrayList<Interval> conditions) {
        Interval condition = conditions.get(indexCondition);
        String variableLabel = condition.getVariable();
        String numPart = variableLabel.substring(1);
        int indexVariable = Integer.parseInt(numPart) - 1; // zero-index
        double varValue = t.get(indexVariable);
        double lb = condition.getLb();
        double ub = condition.getUb();
        boolean val = false;
        if( (varValue>=lb) && (varValue<=ub)){
            val = true;
        }
        return val;
    }

    public static String getPrefixFormatString() {
        return "%s";
    }
    
    public static String getInfixFormatString(){
        return "%s";
    }
}
