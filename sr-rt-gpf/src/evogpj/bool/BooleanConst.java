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
package evogpj.bool;

import evogpj.preprocessing.Interval;
import java.util.ArrayList;
import java.util.List;

public class BooleanConst extends ZeroArgBooleanFunction {

    private final Boolean val;

    public BooleanConst(String label) {
        super(label);
        val = Boolean.parseBoolean(label);
    }

    @Override
    public Boolean eval(List<Double> t,ArrayList<Interval> conditions) {
            return val;
    }

    /**
     * Return a format string for inserting children
     * @return 
     */
    public static String getPrefixFormatString() {
        return "%s";
    }
    
    public static String getInfixFormatString(){
        return "%s";
    }
}
