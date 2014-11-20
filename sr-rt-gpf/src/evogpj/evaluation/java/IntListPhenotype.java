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
package evogpj.evaluation.java;

import java.util.ArrayList;
import java.util.List;

/**
 * The IntListPhenotype class holds a variable-length list of integers.
 * 
 */
public class IntListPhenotype extends Phenotype {
	private static final long serialVersionUID = 658490083299366913L;

	private List<Integer> dataValues;

	public IntListPhenotype() {
		dataValues = new ArrayList<Integer>();
	}

	public Integer getDataValue(int i) {
		return dataValues.get(i);
	}

	public void setDataValue(Integer dataValue) {
		this.dataValues.add(dataValue);
	}

	public int size() {
		return dataValues.size();
	}

	public boolean contains(int i) {
		return dataValues.contains(i);
	}

	@Override
	public String toString() {
		return dataValues.toString();
	}

	@Override
	public Phenotype copy() {
		IntListPhenotype i = new IntListPhenotype();
		i.dataValues = new ArrayList<Integer>();
		i.dataValues.addAll(this.dataValues);
		return i;
	}
}
