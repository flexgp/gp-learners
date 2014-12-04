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

/**
 * Numeric intervals to form rules the form: if (X1 in [a;b]) and (X2 in[c;d])
 * Only applied to the ruleTree algorithm
 * @author Ignacio Arnaldo
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
