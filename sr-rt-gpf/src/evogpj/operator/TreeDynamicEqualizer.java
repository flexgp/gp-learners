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

import evogpj.genotype.Genotype;
import evogpj.genotype.Tree;
import evogpj.gp.GPException;
import evogpj.gp.Individual;
import evogpj.gp.Population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import evogpj.algorithm.Parameters;

/**
 * This operator is an implementation of Silva's Dynamic Operator Equalization,
 * as described in S. Silva and S. Dignum, â€œExtending operator equalisation:
 * Fitness based self adaptive length distribution for bloat free GP,â€ Genetic
 * Programming, pp. 159â€“170, 2009.
 * <p>
 * The idea is we bin all individuals based on their size, and use the binning
 * to intelligently accept or reject individuals.
 * 
 * @author Owen Derby
 */
public class TreeDynamicEqualizer extends Operator implements Equalizer {

    @SuppressWarnings("serial")
    protected class Bin extends ArrayList<Individual> {
        // average fitness of this bin
        public double fitness;
        // best fitness of this bin
        public double most_fit;
        // target capacity of this bin
        public int capacity;

        public Bin() {
            super();
            this.fitness = 0;
            this.most_fit = 0;
            this.capacity = 1;
        }
    }

    // There is some debate over whether it makes sense to just add
    // all intermediate bins, but we believe this is what Silva did,
    // so we kept it. Set this const to false to turn off this behavior.
    private static final boolean KEEP_INTERMEDIATE_BINS = true;
    // Parameter pulled from the properties file
    protected final int BIN_WIDTH;

    // Our bins structure. use hashmap to allow for skipping bins, as well as
    // dynamic allocation of new bins
    protected Map<Integer, Bin> bins;
    protected int max_bin = 1;
    // Track best fitness seen thus far. New bins are only created for
    // individuals which have the best fitness seen so far
    protected double best_of_run;

    /**
     * Given the entire initial population and the properties file, initialize
     * our dynamic equalization operator. We need the initial population to
     * solve a chicken and egg problem: We need to compute the target
     * distribution before we can consider individuals for the next generation,
     * but we need to have a previous generation to base the target on. So we
     * just use the initial population to bootstrap this process.
     * <p>
     * All bins have the same width. This width is specified by the value at the
     * key {@value algorithm.Parameters.Names#BIN_WIDTH} in the properties file.
     * The default width is {@value algorithm.Parameters.Defaults#BIN_WIDTH}.
     * 
     * @param initPop population of individuals created for generation 0.
     * @param props properties file.
     * @throws GPException
     */
    public TreeDynamicEqualizer(Population initPop, Properties props) throws GPException {
        super();
        if (props.containsKey(Parameters.Names.BIN_WIDTH))
            BIN_WIDTH = Integer.valueOf(props.getProperty(Parameters.Names.BIN_WIDTH));
        else
            BIN_WIDTH = Parameters.Defaults.BIN_WIDTH;
        this.best_of_run = 0.0;
        this.bins = new HashMap<Integer, Bin>();
        prime(initPop);
    }

    @Override
    public void update(Population init) throws GPException {
        computeTargetDist();
        // ingest any elitist/initial individuals (accepted without question)
        prime(init);
    }

    @Override
    public boolean accept(Individual i) throws GPException {
        int bin_index = getBinIndex(i);
        boolean ret = false;
        // FIXME hard-coded to take the first fitness value
        double fitness = i.getFitness();
        Bin b;
        if (bins.containsKey(bin_index)) {
            b = bins.get(bin_index);
            if (b.size() < b.capacity || fitness > b.most_fit) {
                b.add(i);
                updateBinFitness(b, fitness);
                ret = true;
            }
        } else if (fitness > this.best_of_run) {
            b = new Bin();
            b.add(i);
            updateBinFitness(b, fitness);
            bins.put(bin_index, b);
            ret = true;
            addIntermediateBins(bin_index);
        }
        return ret;
    }

    /**
     * Like priming a well, take all the individuals in the initial population
     * without question, getting the binning started. Used for ingesting
     * generation 0, as well as elitist individuals.
     * 
     * @param init population of elite/initial individuals
     * @throws GPException if individuals don't have a Tree genotype.
     */
    private void prime(Population init) throws GPException {
        int bin_index;
        // initialize bins
        for (Individual i : init) {
            bin_index = getBinIndex(i);
            Bin bin;
            if (bins.containsKey(bin_index)) {
                bin = bins.get(bin_index);
            } else {
                bin = new Bin();
                bins.put(bin_index, bin);
                addIntermediateBins(bin_index);
            }
            bin.add(i);
            updateBinFitness(bin, i.getFitness());
        }
    }

    protected int getBinIndex(Individual i) throws GPException {
        Genotype gen = i.getGenotype();
        if (!(gen instanceof Tree)) {
            throw new GPException("attempting to use an individual not of type Tree");
        }
        return (((Tree) gen).getSize() - 1) / BIN_WIDTH;
    }

    /**
     * If new_index is greater than the last max_bin, create all intermediate
     * bins up until new max_bin.
     * 
     * @param new_index
     */
    private void addIntermediateBins(int new_index) {
        if (KEEP_INTERMEDIATE_BINS && new_index > max_bin) {
            // if the new bin is outside of the
            // continuous range of bins, add all
            // intermediate bins
            for (int j = max_bin + 1; j < new_index; j++) {
                if (bins.containsKey(j)) System.out.println("impossible bin?" + j + ": " + this.toString());
                bins.put(j, new Bin());
            }
            max_bin = new_index;
        }
    }

    private void computeTargetDist() {
        int n = 0;
        double f = 0;
        for (Bin b : bins.values()) {
            n += b.size();
            b.fitness = 0;
            for (Individual i : b) {
                b.fitness += i.getFitness();
                updateBinFitness(b, i.getFitness());
            }
            if (b.size() > 0) {
                b.fitness = b.fitness / b.size();
                f += b.fitness;
                b.clear();
            }
        }

        // rem is used to remove dead bins (no individuals). However, should not
        // be used if we want to have bins up to max_bin.
        List<Integer> rem = new ArrayList<Integer>();
        for (int i : bins.keySet()) {
            Bin b = bins.get(i);
            b.capacity = (int) Math.round(n * b.fitness / f);
            if (b.capacity < 1) {
                if (KEEP_INTERMEDIATE_BINS)
                    b.capacity = 1;
                else
                    rem.add(i);
            }
        }
        if (!KEEP_INTERMEDIATE_BINS) {
            for (int i : rem) {
                bins.remove(i);
            }
        }
    }

    private void updateBinFitness(Bin bin, double fitness) {
        if (fitness > bin.most_fit) {
            bin.most_fit = fitness;
            if (fitness > best_of_run) {
                best_of_run = fitness;
            }
        }
    }

    /**
     * Summarize current state of the equalizer bins in string form. For each
     * bin, the following is reported:
     * <p>
     * <ul>
     * <li><b>M</b> - The middle of the bin. That is, the mean of the two bin
     * edges, which are the bounds on the sizes of the various individuals
     * contained in that bin.
     * <li><b>N</b> - The number of individuals in the bin.
     * <li><b>C</b> - The capacity of the bin, computed during the call to
     * {@link #update(Population)}.
     * <li><b>A</b> - The average fitness of all individuals in the bin.
     * <li><b>F</b> - The maximum fitness of all individuals in the bin (the
     * "best of bin" fitness).
     * </ul>
     * <p>
     * Each bin substring is formatted as (using the defined codes given above)
     * "M:[N/C,A,F]" and bin substrings are separated by ";".
     */
    @Override
    public String toString() {
        String s = "bins={";
        for (int i : bins.keySet()) {
            Bin b = bins.get(i);
            s += String.format("%f:[%d/%d,%f,%f];", (i + 0.5) * BIN_WIDTH, b.size(), b.capacity, b.fitness, b.most_fit);
        }
        return s.substring(0, s.length() - 1) + "}";
    }
}