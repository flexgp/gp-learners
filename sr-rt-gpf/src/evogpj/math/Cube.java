/**
 * Copyright (c) 2011-2013 Evolutionary Design and Optimization Group
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
 * @author Owen Derby and Ignacio Arnaldo
 * 
 */

package evogpj.math;

import java.util.List;

/**
 *
 * @author Owen Derby and Ignacio Arnaldo
 */
public class Cube extends OneArgFunction {

    /**
     *
     * @param a1
     */
    public Cube(Function a1) {
        super(a1);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Double eval(List<Double> t) {
        return Math.pow(arg.eval(t), 3);
    }

    /**
     *
     * @return
     */
    public static String getInfixFormatString() {
        //return "(%s.^3)";
        return "(cube %s)";
    }
}
