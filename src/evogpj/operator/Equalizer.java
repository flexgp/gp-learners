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
 */
package evogpj.operator;

import evogpj.gp.GPException;
import evogpj.gp.Individual;
import evogpj.gp.Population;

/**
 * Interface for implementing Operator Equalization operator
 * 
 * @author Owen Derby
 */
public interface Equalizer {

    /**
     * Initialize the equalizer for a new generation of init. Reset the
     * equalizer for a new generation, starting out with an initial population
     * given by init.
     * 
     * @param init the initial population (possibly empty), often from elitism.
     * @throws GPException
     */
    public abstract void update(Population init) throws GPException;

    /**
     * Given i, decide whether he ought to be accepted into new population.
     * 
     * @param i candidate individual up for consideration
     * @return true iff i is accepted (survives) into new population.
     * @throws GPException
     */
    public abstract boolean accept(Individual i) throws GPException;


}
