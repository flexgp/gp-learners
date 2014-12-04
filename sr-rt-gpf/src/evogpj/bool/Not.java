/**
 * Copyright (c) 2011-2013 ALFA
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
 * SOFTWARE
 * 
 * @author Ignacio Arnaldo.  
 *
 */
package evogpj.bool;

import evogpj.preprocessing.Interval;
import java.util.ArrayList;
import java.util.List;

public class Not extends OneArgBooleanFunction {

    public Not(BooleanFunction a1) {
            super(a1);
    }

    @Override
    public Boolean eval(List<Double> t,ArrayList<Interval> conditions) {
        boolean val = ! arg.eval(t,conditions);
        return val;
    }

    public static String getPrefixFormatString() {
        //return "cos(%s)";
        return "(not %s)";
    }
    
    public static String getInfixFormatString(){
        return "not( %s )";
    }
}
