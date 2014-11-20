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

import evogpj.genotype.Tree;
import evogpj.gp.GPException;
import evogpj.gp.Individual;
import evogpj.gp.MersenneTwisterFast;
import evogpj.gp.Population;

import java.util.Properties;

/**
 * Test operator that implements multiple operations. This operator performs
 * standard tournament selection, but then when asked to accept or reject the
 * newly created individuals, compares them to the candidates used in the
 * tournament. This is meant to demonstrate the potential to implement two
 * operators in one; it is not meant as an especially useful operator for GP
 * runs.
 * 
 * @author Owen Derby
 */
public class TournamentEqualization extends TournamentSelection implements Equalizer {

	private Population pool;// candidates considered in last tournament
	private double avg_f; // average fitness of tournament candidates
	private boolean reset = true;

	public TournamentEqualization(MersenneTwisterFast rand, Properties props) {
		super(rand, props);
	}

	@Override
	public Individual select(Population pop) {
		// reset the pool if this is a new round of selection
		if (reset) {
			reset = false;
			pool = new Population();
			avg_f = 0.0;
		}
		// select TOURNEY_SIZE individuals to add to the pool
		int n = pop.size();
		avg_f *= pool.size();
		int j = pool.size();
		for (int k = 0; k < TOURNEY_SIZE; k++) {
			pool.add(pop.get(rand.nextInt(n)));
		}
		// find the best individual from the last TOURNEY_SIZE individuals in
		// the pool
		Individual best, challenger;
		best = pool.get(j);
		// FIXME hard-coded to take the first fitness value
		avg_f += best.getFitness();
		for (; j < pool.size(); j++) {
			challenger = pool.get(j);
			avg_f += challenger.getFitness();
			if (challenger.getFitness() > best.getFitness())
				best = challenger;
		}
		avg_f /= pool.size();
		return best;
	}

	/**
	 * This is just a sample accept method, but many ways of deciding to accept
	 * the individual are possible
	 */
	@Override
	public boolean accept(Individual i) throws GPException {
		// reject an offspring if it is worse than all of the individuals in the
		// pool it was selected from
		reset = true;
		// if (i.getFitness() > avg_f)
		// return true;
		for (Individual j : pool) {
			// if (i.getFitness() > j.getFitness()) {
			if (((Tree) i.getGenotype()).getSize() > ((Tree) j.getGenotype())
					.getSize() && i.getFitness() < j.getFitness()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void update(Population init) throws GPException {
		// do nothing here. We only look at the pool of individuals used in the
		// tournament when accepting.
	}

}
