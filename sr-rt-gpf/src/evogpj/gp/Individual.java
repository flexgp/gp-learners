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
package evogpj.gp;

import evogpj.evaluation.FitnessComparisonStandardizer;
import evogpj.evaluation.FitnessFunction;
import evogpj.genotype.Genotype;
import evogpj.genotype.Tree;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Set;


/**
 * This class represents the individuals in a population. Individuals have a
 * {@link Genotype}, a {@link Phenotype} and a fitness. Genotypes are what
 * genetic operations use to create new offspring. Phenotypes are like a preview
 * of how this individual looks, in the problem space (ie the output of an
 * individual in a SR problem for each of the test inputs). Fitness is the
 * standard number representing how fit this individual is. Higher fitness is
 * better.
 * 
 * @author Owen Derby
 * @see Genotype
 * @see Phenotype
 */
public class Individual implements Serializable {
	private static final long serialVersionUID = -1448696047207647168L;
	private final Genotype genotype;
	// store fitness values as key:value pairs to support multiple objectives
	private LinkedHashMap<String, Double> fitnesses;
	// distance from the origin
	private Double euclideanDistance;
	// the "hypervolume" of a particular individual
	private Double fitnessVolume;
	private Double crowdingDistance;
	private Integer dominationCount;
	private double threshold;
        private double crossValAreaROC;	
        private double crossValFitness;
        private double scaledMAE;
        private double scaledMSE;
	/**
	 * Create an individual with the given genotype. The new individuals
	 * phenotype and fitness are left unspecified.
	 * 
	 * @param genotype an Instance of a {@link Genotype}
	 */
	public Individual(Genotype genotype) {
		this.genotype = genotype;
		this.fitnesses = new LinkedHashMap<String, Double>();
		this.euclideanDistance = Double.MAX_VALUE;
		this.fitnessVolume = 0.0;
		this.crowdingDistance = 0.0;
		this.dominationCount = 0;
                threshold = 0;
                crossValAreaROC = 0;                
	}

	@SuppressWarnings("unchecked")
	private Individual(Individual i) {
		this.crowdingDistance = i.crowdingDistance;
		this.dominationCount = i.dominationCount;
		this.genotype = i.genotype.copy();
		this.fitnesses = (LinkedHashMap<String, Double>) i.fitnesses.clone();
		this.euclideanDistance = i.euclideanDistance;
		this.fitnessVolume = i.fitnessVolume;
                this.threshold = i.threshold;
                this .crossValAreaROC = i.crossValAreaROC;                
	}

	/**
	 * deep copy of an individual. THe new individual will be identical to the
	 * old in every way, but will be completely independent. So any changes made
	 * to the old do not affect the new, and vice versa.
	 * 
	 * @return new individual
	 */
	public Individual copy() {
		return new Individual(this);
	}

	public Genotype getGenotype() {
		return this.genotype;
	}

	public Set<String> getFitnessNames() {
		return this.fitnesses.keySet();
	}
	
	/**
	 * Return the LinkedHashMap storing fitness values
	 * @return
	 */
	public LinkedHashMap<String, Double> getFitnesses() {
		return this.fitnesses;
	}

	public Double getFitness(String key) {
		return this.fitnesses.get(key);
	}

	/**
	 * Overload getFitness to support returning simply the first fitness value if prompted
	 * @return
	 */
	public Double getFitness() {
		String first = getFirstFitnessKey();
		return getFitness(first);
	}

	/**
	 * @return the first fitness key from this individual's stored fitness values
	 */
	public String getFirstFitnessKey() {
		Set<String> keys = getFitnesses().keySet();
		return keys.iterator().next();
	}
	
	public void setFitnesses(LinkedHashMap<String, Double> d) {
		this.fitnesses = d;
	}

	/**
	 * Update a particular fitness value
	 * @param i the key for this fitness
	 * @param d the new fitness
	 * @throws Exception 
	 */
	public void setFitness(String key, Double d) {
		fitnesses.put(key, d);
	}

	/**
	 * Obtain the memoized euclidean distance of fitnesses
	 * @return
	 */
	public Double getEuclideanDistance() {
		return euclideanDistance;
	}

	/**
	 * Calculate the euclidean distance of the fitnesses of this individual from
	 * the origin.
	 */
	public void calculateEuclideanDistance(LinkedHashMap<String, FitnessFunction> fitnessFunctions,LinkedHashMap<String, Double> standardizedMins,
			LinkedHashMap<String, Double> standardizedRanges) {
		// reset euclidean distance to 0
		euclideanDistance = 0.0;
		for (String fitnessKey : fitnesses.keySet()) {
			// get fitness converted to minimization if necessary
			Double standardizedFitness = FitnessComparisonStandardizer.getFitnessForMinimization(this, fitnessKey,fitnessFunctions);
			// normalize 
			Double normalizedStandardizedFitness = (standardizedFitness - standardizedMins.get(fitnessKey)) / standardizedRanges.get(fitnessKey);
			// add to euclidean distance
			euclideanDistance += Math.pow(normalizedStandardizedFitness,2);
		}
	}

	/**
	 * Obtain the volume represented by this individuals' fitnesses, which is set externally
	 * @return
	 */
	public Double getFitnessVolume() {
		return fitnessVolume;
	}

	/**
	 * Calculate the volume represented by this individual's fitness scores, to be used as a relative comparison with others.
	 */
	public void calculateFitnessVolume(LinkedHashMap<String, FitnessFunction> fitnessFunctions,LinkedHashMap<String, Double> standardizedMins,
			LinkedHashMap<String, Double> standardizedRanges) {
		// reset euclidean distance to 0
		fitnessVolume = 1.0;
		// a flag for ensuring distance_2 < distance_1
		int index = 0;
		for (String fitnessKey : fitnesses.keySet()) {
			// get fitness converted to minimization if necessary
			Double standardizedFitness = FitnessComparisonStandardizer.getFitnessForMinimization(this, fitnessKey,fitnessFunctions);
			// normalize 
			Double normalizedStandardizedFitness = (standardizedFitness - standardizedMins.get(fitnessKey)) / standardizedRanges.get(fitnessKey);
			// calculate the distance to the reference point, which after normalization is 1.0,1.0,...
			Double normalizedStandardizedDistance = 1.0 - normalizedStandardizedFitness;
			// only proceed if the distance of the second objective is less than the distance of the first
			// also, if the calculated distance is 0.0, subsequent distances won't matter, so return 0.0
			if ((index == 1) && (normalizedStandardizedDistance > fitnessVolume)) {
				fitnessVolume = 0.0;
				return;
			}
			// add to euclidean distance
			fitnessVolume *= normalizedStandardizedDistance;
			// update index
			index++;
		}
	}
	
	public Double getCrowdingDistance() {
		return this.crowdingDistance;
	}
	
	public void setCrowdingDistance(Double newCrowdingDistance) {
		this.crowdingDistance = newCrowdingDistance;
	}

	/**
	 * Update the crowding distance of this individual with information from a new fitness function
	 * @param localCrowdingDistance
	 */
	public void updateCrowdingDistance(Double localCrowdingDistance) {
		crowdingDistance *= localCrowdingDistance;
	}
	
	public Integer getDominationCount() {
		return dominationCount;
	}

	public void incrementDominationCount() {
		dominationCount++;
	}

	public void setDominationCount(Integer dominationCount) {
		this.dominationCount = dominationCount;
	}

        public double getThreshold(){
            return threshold;
        }
        
        public void setThreshold(double aThreshold){
            threshold = aThreshold;
        }	
        
        public double getCrossValAreaROC(){
            return crossValAreaROC;
        }
        
        public void setCrossValAreaROC(double anArea){
            crossValAreaROC = anArea;
        }
        
	/**
	 * Clear any nonessential memoized values
	 */
	public void reset() {
		euclideanDistance = Double.MAX_VALUE;
		fitnessVolume = 0.0;
		crowdingDistance = 0.0;
		dominationCount = 0;
	}
	
	public Boolean equals(Individual i){
		if (!this.getGenotype().equals(i.getGenotype())){
			return false;
                } else{ // g and p are equal, check fitnesses
                    return (this.getFitnesses().equals(i.getFitnesses()));
                }
	}

	/**
	 * @return unscaled MATLAB infix string
	 */
	@Override
	public String toString() {
		return this.genotype.toString();
	}

	/**
	 * @return scaled MATLAB infix string, with appropriate rounding if necessary
	 */
	public String toScaledString(boolean shouldRound) {
		String scaledModel = ((Tree) this.genotype).toScaledString();
		return (shouldRound) ? "round(" + scaledModel + ")" : scaledModel ;
	}

	public String toScaledString() {
		return toScaledString(false);
	}
        
        public void setScaledCrossValFitness(double aFitness){
            this.crossValFitness = aFitness;
        }
        
        public double getCrossValFitness(){
            return crossValFitness;
        }
        
        public void setScaledMSE(double aMSE){
            scaledMSE = aMSE;
        }
        
        public double getScaledMSE(){
            return scaledMSE;
        }
        
        public void setScaledMAE(double aMAE){
            scaledMAE = aMAE;
        }
        
        public double getScaledMAE(){
            return scaledMAE;
        }
}