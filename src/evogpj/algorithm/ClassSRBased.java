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
package evogpj.algorithm;

import evogpj.evaluation.cpp.DataCpp;
import evogpj.evaluation.cuda.DataCuda;
import evogpj.evaluation.cuda.SRRocCuda;
import evogpj.evaluation.cuda.SRRocCVCuda;
import evogpj.evaluation.cpp.SRRocCpp;
import evogpj.evaluation.cpp.SRRocCVCpp;
import evogpj.evaluation.FitnessFunction;
import evogpj.evaluation.java.SubtreeComplexityFitness;
import evogpj.genotype.Tree;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.GPException;
import evogpj.gp.Individual;
import evogpj.gp.MersenneTwisterFast;
import evogpj.gp.Population;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import evogpj.operator.Crossover;
import evogpj.operator.CrowdedTournamentSelection;
import evogpj.operator.Initialize;
import evogpj.operator.Mutate;
import evogpj.operator.Select;
import evogpj.operator.SinglePointKozaCrossover;
import evogpj.operator.SinglePointUniformCrossover;
import evogpj.operator.SubtreeMutate;
import evogpj.operator.TournamentSelection;
import evogpj.operator.TreeInitialize;
import evogpj.sort.CrowdingSort;
import evogpj.sort.NonDominatedSort;
import evogpj.sort.NonDominatedSort.DominationException;


/**
 * This class contains the main method that runs the GP algorithm.
 * 
 * @author Owen Derby
 **/
public class ClassSRBased {
    
    /* NUMBER OF THREADS EMPLOYED IN THE EXERNAL EVALUATION */
    protected int EXTERNAL_THREADS = Parameters.Defaults.EXTERNAL_THREADS;
    
    /* DATA */
    // SYMBOLIC REGRESSION ON FUNCTION OR DATA
    protected String PROBLEM_TYPE = Parameters.Defaults.PROBLEM_TYPE;
    // TRAINING SET
    protected String PROBLEM = Parameters.Defaults.PROBLEM;
    protected int PROBLEM_SIZE = Parameters.Defaults.PROBLEM_SIZE;
    // INTEGER TARGETS
    protected boolean COERCE_TO_INT = Parameters.Defaults.COERCE_TO_INT;
    protected int TARGET_NUMBER = 1;
    // FEATURES
    protected List<String> TERM_SET;
    // CROSS-VALIDATION SET FOR SYMBOLIC REGRESSION-BASED CLASSIFICATION
    protected String CROSS_VAL_SET = Parameters.Defaults.CROSS_VAL_SET;
    
    
    /* PARAMETERS GOVERNING THE GENETIC PROGRAMMING PROCESS */
    // POPULATION SIZE
    protected int POP_SIZE = Parameters.Defaults.POP_SIZE;
    // NUMBER OF GENERATIONS
    protected int NUM_GENS = Parameters.Defaults.NUM_GENS;
    // start time
    protected Long startTime;
    // TIME OUT
    protected Long TIMEOUT;
    
    // MUTATION RATE
    protected double MUTATION_RATE = Parameters.Defaults.MUTATION_RATE;
    // CROSSOVER RATE
    protected double XOVER_RATE = Parameters.Defaults.XOVER_RATE;
    
    
    // DEFAULT MUTATION OPERATOR
    protected String INITIALIZE = Parameters.Defaults.INITIALIZE;
    // DEFAULT MUTATION OPERATOR
    protected String SELECT = Parameters.Defaults.SELECT;
    // DEFAULT CROSSOVER OPERATOR
    protected String XOVER = Parameters.Defaults.XOVER;
    // DEFAULT MUTATION OPERATOR
    protected String MUTATE = Parameters.Defaults.MUTATE;
    // DEFAULT MUTATION OPERATOR
    protected String FITNESS = Parameters.Defaults.FITNESS;
    // METHOD EMPLOYED TO AGGREGATE THE FITNESS OF CANDIDATE SOLUTIONS
    protected int MEAN_POW = Parameters.Defaults.MEAN_POW;
    // METHOD EMPLOYED TO SELECT A SOLUTION FROM A PARETO FRONT
    protected String FRONT_RANK_METHOD = Parameters.Defaults.FRONT_RANK_METHOD;
    
    
    // ALL THE OPERATORS USED TO BUILD GP TREES
    protected List<String> FUNC_SET = Parameters.Defaults.FUNCTIONS;
    
    // UNARY OPERATORS USED TO BUILD GP TREES
    protected List<String> UNARY_FUNC_SET = Parameters.Defaults.UNARY_FUNCTIONS;  
    
    // RANDOM SEED
    protected Long SEED = Parameters.Defaults.SEED;
    
    /* LOG FILES*/
    // LOG BEST INDIVIDUAL PER GENERATION
    protected String MODELS_PATH = Parameters.Defaults.MODELS_PATH;
    // LOG BEST INDIVIDUAL WITH RESPECT TO CROSS-VALIDATION SET IN CLASSIFICATION
    protected String MODELS_CV_PATH = Parameters.Defaults.MODELS_CV_PATH;
    // LOG FINAL PARETO FRONT
    protected String PARETO_PATH = Parameters.Defaults.PARETO_PATH;
    // LOG least complex ind
    protected String LEAST_COMPLEX_PATH = Parameters.Defaults.LEAST_COMPLEX_PATH;
    // LOG most accurate individual
    protected String MOST_ACCURATE_PATH = Parameters.Defaults.MOST_ACCURATE_PATH;
    // LOG the model on the knee of the Pareto Front
    protected String KNEE_PATH = Parameters.Defaults.KNEE_PATH;
    
    /* CANDIDATE SOLUTIONS MAINTAINED DURING THE SEARCH */
    // CURRENT POPULATION
    protected Population pop;
    // OFFSPRING
    protected Population childPop;
    // OFFSPRING + PARENTS
    protected Population totalPop;
    // CURRENT NON-DOMINATED SOLUTIONS
    protected Population paretoFront;
    // CURRENT GENERATION'S BEST INDIVIDUAL
    protected Individual best;
    // BEST INDIVIDUAL OF EACH GENERATION
    protected Population bestPop;
    
    
    /* OPERATORS EMPLOYED IN THE SEARCH PROCESS */
    // RANDOM NUMBER GENERATOR
    protected MersenneTwisterFast rand;
    // INITIALIZATION METHOD
    protected Initialize initialize;
    // CROSSOVER
    protected Crossover xover;
    // SELECTION
    protected Select select;
    // MUTATION
    protected Mutate mutate;
    // FITNESS FUNCTIONS
    protected LinkedHashMap<String, FitnessFunction> fitnessFunctions;
    
    
    /* CONTROL FOR END OF EVOLUTIONARY PROCESS*/
    // CURRENT GENERATION
    protected Integer generation;
    // CONTROL FOR END OF PROCESS
    protected Boolean finished;
    // NUMBER OF GENERATIONS WITHOUT FITNESS IMPROVEMENT
    protected int counterConvergence;
    // CURRENT FITNESS OF BEST INDIVIDUAL
    protected double lastFitness;
    
    /**
     * Create an instance of the algorithm. This simply initializes all the
     * operators to the default parameters or whatever they are set to in the
     * passed in properties object. Use {@link #run_population()} to actually
     * run the population for the specified number of generations.
     * <p>
     * If an invalid operator type is specified, then the program will
     * terminate, indicating which parameter is incorrect.
     * 
     * @param props
     *            Properties object created from a .properties file specifying
     *            parameters for the algorithm
     * @param seed
     *            A seed to use for the RNG. This allows for repeating the same
     *            trials over again.
     */
    public ClassSRBased(Properties props,long timeout) throws IOException {
        this();
        if (timeout > 0)
            TIMEOUT = startTime + (timeout * 1000);
        loadParams(props);
        create_operators(props,SEED);
    }
    
    public ClassSRBased(String propFile,long timeout) throws IOException {
        this();
        if (timeout > 0)
            TIMEOUT = startTime + (timeout * 1000);
        Properties props = loadProps(propFile);
        loadParams(props);
        create_operators(props,SEED);
    }
    
    public ClassSRBased(String propFile) throws IOException {
        this();
        Properties props = loadProps(propFile);
        loadParams(props);
        create_operators(props,SEED);
    }
    
    

    /**
     * Empty constructor, to allow subclasses to override
     */
    public ClassSRBased() {
        fitnessFunctions = new LinkedHashMap<String, FitnessFunction>();
        finished = false;
        generation = 0;
        counterConvergence = 0;
        lastFitness = 0;
        startTime = System.currentTimeMillis();
    }

    /**
     * Read parameters from the Property object and set Algorithm variables.
     * 
     * @see Parameters
     */
    protected void loadParams(Properties props) {
        if (props.containsKey(Parameters.Names.SEED))
            SEED = Long.valueOf(props.getProperty(Parameters.Names.SEED)).longValue();
        if (props.containsKey(Parameters.Names.EXTERNAL_THREADS))
            EXTERNAL_THREADS = Integer.valueOf(props.getProperty(Parameters.Names.EXTERNAL_THREADS));
        if (props.containsKey(Parameters.Names.FITNESS))
            FITNESS = props.getProperty(Parameters.Names.FITNESS);            
        if (props.containsKey(Parameters.Names.MUTATION_RATE))
            MUTATION_RATE = Double.valueOf(props.getProperty(Parameters.Names.MUTATION_RATE));
        if (props.containsKey(Parameters.Names.XOVER_RATE))
            XOVER_RATE = Double.valueOf(props.getProperty(Parameters.Names.XOVER_RATE));
        if (props.containsKey(Parameters.Names.POP_SIZE))
            POP_SIZE = Integer.valueOf(props.getProperty(Parameters.Names.POP_SIZE));
        if (props.containsKey(Parameters.Names.POP_DATA_PATH))
            MODELS_PATH = props.getProperty(Parameters.Names.MODELS_PATH);
        if (props.containsKey(Parameters.Names.NUM_GENS))
            NUM_GENS = Integer.valueOf(props.getProperty(Parameters.Names.NUM_GENS));
        if (props.containsKey(Parameters.Names.PROBLEM))
            PROBLEM = props.getProperty(Parameters.Names.PROBLEM);
        if (props.containsKey(Parameters.Names.CROSS_VAL_SET))
            CROSS_VAL_SET = props.getProperty(Parameters.Names.CROSS_VAL_SET);            
        if (props.containsKey(Parameters.Names.PROBLEM_TYPE))
            PROBLEM_TYPE = props.getProperty(Parameters.Names.PROBLEM_TYPE);
        if (props.containsKey(Parameters.Names.PROBLEM_SIZE))
            PROBLEM_SIZE = Integer.parseInt(props.getProperty(Parameters.Names.PROBLEM_SIZE));
        if (props.containsKey(Parameters.Names.MEAN_POW))
            MEAN_POW = Integer.valueOf(props.getProperty(Parameters.Names.MEAN_POW));
        if (props.containsKey(Parameters.Names.COERCE_TO_INT))
            COERCE_TO_INT = Boolean.parseBoolean(props.getProperty(Parameters.Names.COERCE_TO_INT));
        if (props.containsKey(Parameters.Names.FUNCTION_SET)) {
            String funcs[] = props.getProperty(Parameters.Names.FUNCTION_SET).split(" ");
            FUNC_SET = new ArrayList<String>();
            for (int i = 0; i < funcs.length; i++)
                    FUNC_SET.add(funcs[i]);
        }
        if (props.containsKey(Parameters.Names.UNARY_FUNCTION_SET)) {
            String funcs[] = props.getProperty(Parameters.Names.UNARY_FUNCTION_SET).split(" ");
            UNARY_FUNC_SET = new ArrayList<String>();
            for (int i = 0; i < funcs.length; i++)
                UNARY_FUNC_SET.add(funcs[i]);
        }
        if (props.containsKey(Parameters.Names.TERMINAL_SET)) {
            String term = props.getProperty(Parameters.Names.TERMINAL_SET);
            if (term.equalsIgnoreCase("all")) {
                // defer populating terminal list until we know our problem
                // size!
                TERM_SET = null;
            } else {
                String terms[] = term.split(" ");
                TERM_SET = new ArrayList<String>();
                for (int i = 0; i < terms.length; i++)
                        TERM_SET.add(terms[i]);
            }
        }
        if (props.containsKey(Parameters.Names.MUTATE))
                MUTATE = props.getProperty(Parameters.Names.MUTATE);
        if (props.containsKey(Parameters.Names.XOVER))
                XOVER = props.getProperty(Parameters.Names.XOVER);
        if (props.containsKey(Parameters.Names.SELECTION))
                SELECT = props.getProperty(Parameters.Names.SELECTION);
        if (props.containsKey(Parameters.Names.INITIALIZE))
                INITIALIZE = props.getProperty(Parameters.Names.INITIALIZE);
        if (props.containsKey(Parameters.Names.FRONT_RANK_METHOD))
                FRONT_RANK_METHOD = props.getProperty(Parameters.Names.FRONT_RANK_METHOD);
        
    }

    /**
     * Handle parsing the FITNESS field (fitness_op), which could contain
     * multiple fitness operators
     * 
     * @return a LinkedHashMap with properly ordered operators and null
     *         FitnessFunctions. This enforces the iteration order
     */
    protected LinkedHashMap<String, FitnessFunction> splitFitnessOperators(String fitnessOpsRaw) {
        LinkedHashMap<String, FitnessFunction> fitnessOperators = new LinkedHashMap<String, FitnessFunction>();
        List<String> fitnessOpsSplit = Arrays.asList(fitnessOpsRaw.split("\\s*,\\s*"));
        for (String f : fitnessOpsSplit) {
            fitnessOperators.put(f, null);
        }
        return fitnessOperators;
    }

    /**
     * Create all the operators from the loaded params. Seed is the seed to use
     * for the rng. If specified, d_in is some DataJava to use. Otherwise, d_in
     * should be null and fitness will load in the appropriate data.
     * 
     * @param seed
     * 
     */
    protected void create_operators(Properties props, long seed) throws IOException {
        System.out.println("Running evogpj with seed: " + seed);
        rand = new MersenneTwisterFast(seed);
        fitnessFunctions = splitFitnessOperators(FITNESS);
        for (String fitnessOperatorName : fitnessFunctions.keySet()) {
            if (fitnessOperatorName.equals(Parameters.Operators.SR_CPP_ROC)) {
                // this loads the data into shared memory
                DataCpp ed = new DataCpp(PROBLEM, 1);
                ed.readAndStoreDataset();
                int numberOfFeatures = ed.getNumberOfFeatures();
                int numberOfFitnessCases = ed.getNumberOfFitnessCases();
                ed = null;
                if (TERM_SET == null) {
                        TERM_SET = new ArrayList<String>();
                        for (int i = 0; i < numberOfFeatures; i++){
                            TERM_SET.add("X" + (i + 1));
                        }
                        System.out.println(TERM_SET);
                }
                SRRocCpp ecr = new SRRocCpp(FUNC_SET, UNARY_FUNC_SET, PROBLEM,numberOfFitnessCases, numberOfFeatures,TARGET_NUMBER, EXTERNAL_THREADS, MEAN_POW,COERCE_TO_INT);                    
                fitnessFunctions.put(fitnessOperatorName, ecr);
            } if (fitnessOperatorName.equals(Parameters.Operators.SR_CUDA_ROC)) {
                // this loads the data into shared memory
                DataCuda ed = new DataCuda(PROBLEM, 1);
                ed.readAndStoreDataset();
                int numberOfFeatures = ed.getNumberOfFeatures();
                int numberOfFitnessCases = ed.getNumberOfFitnessCases();
                ed = null;
                if (TERM_SET == null) {
                    TERM_SET = new ArrayList<String>();
                    for (int i = 0; i < numberOfFeatures; i++) {
                        TERM_SET.add("X" + (i + 1));
                    }
                    System.out.println(TERM_SET);
                }
                SRRocCuda ecr = new SRRocCuda(FUNC_SET, UNARY_FUNC_SET, PROBLEM,numberOfFitnessCases, numberOfFeatures,
                                TARGET_NUMBER, EXTERNAL_THREADS, MEAN_POW,COERCE_TO_INT,250);
                fitnessFunctions.put(fitnessOperatorName, ecr);
            } else if (fitnessOperatorName.equals(Parameters.Operators.SUBTREE_COMPLEXITY_FITNESS)) {
                fitnessFunctions.put(fitnessOperatorName,new SubtreeComplexityFitness());
            } else {
                System.err.format("Invalid fitness function %s specified for problem type %s%n",fitnessOperatorName, PROBLEM_TYPE);
                System.exit(-1);
            }
        }

        TreeGenerator treeGen = new TreeGenerator(rand, FUNC_SET, TERM_SET);
        if (INITIALIZE.equals(Parameters.Operators.TREE_INITIALIZE)) {
            initialize = new TreeInitialize(rand, props, treeGen);
        } else {
            System.err.format("Invalid initialize function %s specified%n",INITIALIZE);
            System.exit(-1);
        }

        // Set up operators.
        if (SELECT.equals(Parameters.Operators.TOURNEY_SELECT)) {
            select = new TournamentSelection(rand, props);
        } else if (SELECT.equals(Parameters.Operators.CROWD_SELECT)) {
            select = new CrowdedTournamentSelection(rand, props);
        } else {
            System.err.format("Invalid select function %s specified%n", SELECT);
            System.exit(-1);
        }

        mutate = new SubtreeMutate(rand, props, treeGen);

        if (XOVER.equals(Parameters.Operators.SPU_XOVER)) {
            xover = new SinglePointUniformCrossover(rand, props);
        } else if (XOVER.equals(Parameters.Operators.SPK_XOVER)) {
            xover = new SinglePointKozaCrossover(rand, props);
        } else {
            System.err.format("Invalid crossover function %s specified%n",XOVER);
            System.exit(-1);
        }

        // to set up equalization operator, we need to evaluate all the
        // individuals first
        pop = initialize.initialize(POP_SIZE);
        // initialize totalPop to simply the initial population
        for (FitnessFunction f : fitnessFunctions.values())
            f.evalPop(pop);
        // calculate domination counts of initial population for tournament selection
        try {
            NonDominatedSort.sort(pop, fitnessFunctions);
        } catch (DominationException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        // save first front of initial population
        paretoFront = new Population();
        for (int index = 0; index < pop.size(); index++) {
            Individual individual = pop.get(index);
            if (individual.getDominationCount().equals(0))
                paretoFront.add(individual);
        }
        // calculate crowding distances of initial population for crowding sort
        if (SELECT.equals(Parameters.Operators.CROWD_SELECT)) {
            CrowdingSort.computeCrowdingDistances(pop, fitnessFunctions);
        }
    }

    /**
     * Accept potential migrants into the population
     * @param migrants
     */
    protected void acceptMigrants(Population migrants) {
            pop.addAll(migrants);
    }
	
    /**
     * This is the heart of the algorithm. This corresponds to running the
     * {@link #pop} forward one generation
     * <p>
     * Basically while we still need to produce offspring, we select an
     * individual (or two) as parent(s) and perform a genetic operator, chosen
     * at random according to the parameters, to apply to the parent(s) to
     * produce children. Then evaluate the fitness of the new child(ren) and if
     * they are accepted by the equalizer, add them to the next generation.
     * <p>
     * The application of operators is mutually exclusive. That is, for each
     * iteration of this algorithm, we will choose exactly one of crossover,
     * mutation and replication. However, which one we choose is determined by
     * sampling from the distribution specified by the mutation and crossover
     * rates.
     * 
     * @returns a LinkedHashMap mapping fitness function name to the best
     *          individual for that fitness function
     * @throws GPException
     *             if any of the operators receive a individual with an
     *             unexpected genotype, this is an error.
     */
    protected void step() throws GPException {
        // generate children from previous population. don't use elitism
        // here since that's done later
        childPop = new Population();
        Population children;
        while (childPop.size() < POP_SIZE) {
            Individual p1 = select.select(pop);
            children = new Population();
            double prob = rand.nextDouble();
            // Select exactly one operator to use
            if (prob < XOVER_RATE) {
                Individual p2 = select.select(pop);
                children = xover.crossOver(p1, p2);
            } else if (prob < MUTATION_RATE + XOVER_RATE) {
                Individual mutated = mutate.mutate(p1);
                children.add(mutated);
            } else
                continue;
            for (Individual i : children) {
                if (!(childPop.size() < POP_SIZE)) {
                    break;
                }
                childPop.add(i);
            }
        }
        // evaluate all children
        for (String fname : fitnessFunctions.keySet()) {
            FitnessFunction f = fitnessFunctions.get(fname);
            f.evalPop(childPop);
        }
        // combine the children and parents for a total of 2*POP_SIZE
        totalPop = new Population(pop, childPop);
        try {
            // for each individual, count number of individuals that dominate it
            NonDominatedSort.sort(totalPop, fitnessFunctions);
        } catch (DominationException e) {
                e.printStackTrace();
                System.exit(-1);
        }
        // if crowding tournament selection is enabled, calculate crowding distances
        if (SELECT.equals(Parameters.Operators.CROWD_SELECT)) {
            CrowdingSort.computeCrowdingDistances(totalPop, fitnessFunctions);
        }
        // sort the entire 2*POP_SIZE population by domination count and by crowding distance if enabled
        totalPop.sort(SELECT.equals(Parameters.Operators.CROWD_SELECT));

        // use non-dominated sort to take the POP_SIZE best individuals
        // also find the latest pareto front
        pop = new Population();
        paretoFront = new Population();
        for (int index = 0; index < POP_SIZE; index++) {
            Individual individual = totalPop.get(index);
            pop.add(individual);
            // also save the first front for later use
            if (individual.getDominationCount().equals(0))
                paretoFront.add(individual);
        }
        // find best individual
        pop.calculateEuclideanDistances(fitnessFunctions);
        best = pop.get(0);
        for (int index = 0; index < POP_SIZE; index++) {
            Individual individual = pop.get(index);
            // two methods for selecting the best here from the entire population:
            // 1) euclidean distance
            // 2) "first fitness", which for dynamic equalization is simply
            // the individual with the best fitness, and for multi-objective optimization
            // is the individual with the best first fitness
            if (FRONT_RANK_METHOD.equals(Parameters.Names.EUCLIDEAN)) {
                if (individual.getEuclideanDistance() < best.getEuclideanDistance()) {
                    best = individual;
                }
            } else if (FRONT_RANK_METHOD.equals(Parameters.Names.FIRST_FITNESS)) {
                if(individual.getFitness() > best.getFitness()){
                    best = individual;
                }
            } else {
                System.err.format("No such selection method \"%s\"%n", FRONT_RANK_METHOD);
                System.exit(-1);
            }
        }
    }

    /**
     * get the best individual per generation in a Population object
     * 
     * @return the best individual per generation.
     */
    public Population getBestPop(){
        return bestPop;
    }
                
    /**
    * Run the current population for the specified number of generations.
    * 
    * @return the best individual found.
    */
    public Individual run_population() throws IOException {
        Individual bestOnCrossVal = null;
        bestPop = new Population();
        // get the best individual
        best = pop.get(0);
        System.out.println(best.getFitnesses());
        // record the best individual in models.txt
        bestPop.add(best);
        while ((generation <= NUM_GENS) && (!finished)) {
            System.out.format("Generation %d\n", generation);
            System.out.flush();
            try {
                step();
            } catch (GPException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            // print information about this generation
            //System.out.format("Statistics: %d " + calculateStats() + "%n", generation);
            System.out.format("Best individual for generation %d:%n", generation);
            double MSE = best.getFitness();
            MSE = ((1-MSE) / (MSE + 1));
            System.out.println(best.getFitnesses());
            System.out.println(MSE);
            System.out.flush();

            bestPop.add(best);
            generation++;
            finished = stopCriteria();
            
        }

        String firstFitnessFunction = fitnessFunctions.keySet().iterator().next();
        if(firstFitnessFunction.equals(Parameters.Operators.SR_CPP_ROC)){
            DataCpp ed = new DataCpp(PROBLEM, TARGET_NUMBER);
            ed.deallocateDataset();
            ed = new DataCpp(CROSS_VAL_SET, TARGET_NUMBER);
            ed.readAndStoreDataset();
            int numberOfFeatures = ed.getNumberOfFeatures();
            int numberOfFitnessCases = ed.getNumberOfFitnessCases();
            ed = null;
            SRRocCVCpp ecv = new SRRocCVCpp(FUNC_SET, UNARY_FUNC_SET, CROSS_VAL_SET,numberOfFitnessCases, numberOfFeatures,
                            TARGET_NUMBER, EXTERNAL_THREADS);
            ecv.evalPop(bestPop);
            bestOnCrossVal = bestPop.get(0);
            for(Individual indAux:bestPop){
                if(indAux.getCrossValAreaROC()>bestOnCrossVal.getCrossValAreaROC()){
                    bestOnCrossVal = indAux;
                }
            }
            // SAVE BEST PER GENERATION + fitness + areaROCCV + threshold
            for(Individual ind:bestPop){
                this.saveText(MODELS_PATH, ind.getGenotype().toString() + "\n", true);
                this.saveText(MODELS_PATH, ind.getFitness(Parameters.Operators.SR_CPP_ROC) + " " + ind.getCrossValAreaROC() + " " + ind.getThreshold() + "\n" , true);
            }
            this.saveText(MODELS_PATH, "\n", true);
            this.saveText(MODELS_CV_PATH, bestOnCrossVal.getGenotype().toString() + "\n", true);
            this.saveText(MODELS_CV_PATH, bestOnCrossVal.getFitness(Parameters.Operators.SR_CPP_ROC) + " " 
                                        + bestOnCrossVal.getCrossValAreaROC() + " " 
                                        + bestOnCrossVal.getThreshold() + "\n" , true);
        } if(firstFitnessFunction.equals(Parameters.Operators.SR_CUDA_ROC)){
            DataCuda ed = new DataCuda(PROBLEM, TARGET_NUMBER);
            ed.deallocateDataset();
            ed = new DataCuda(CROSS_VAL_SET, TARGET_NUMBER);
            ed.readAndStoreDataset();
            int numberOfFeatures = ed.getNumberOfFeatures();
            int numberOfFitnessCases = ed.getNumberOfFitnessCases();
            ed = null;
            float fpWeight = (float) 0.5;
            float fnWeight = (float) 0.5;
            int numLambdas = 10;
            SRRocCVCuda ecv = new SRRocCVCuda(FUNC_SET, UNARY_FUNC_SET, CROSS_VAL_SET,numberOfFitnessCases, numberOfFeatures,
                            TARGET_NUMBER, EXTERNAL_THREADS, 0,fpWeight,fnWeight,numLambdas);
            ecv.evalPop(bestPop);
            bestOnCrossVal = bestPop.get(0);
            for(Individual indAux:bestPop){
                if(indAux.getCrossValAreaROC()>bestOnCrossVal.getCrossValAreaROC()){
                    bestOnCrossVal = indAux;
                }
            }
            // SAVE BEST PER GENERATION + fitness + areaROCCV + threshold
            for(Individual ind:bestPop){
                this.saveText(MODELS_PATH, ind.getGenotype().toString() + "\n", true);
                this.saveText(MODELS_PATH, ind.getFitness(Parameters.Operators.SR_CUDA_ROC) + " " + ind.getCrossValAreaROC() + " " + ind.getThreshold() + "\n" , true);
            }
            this.saveText(MODELS_PATH, "\n", true);
            this.saveText(MODELS_CV_PATH, bestOnCrossVal.getGenotype().toString() + "\n", true);
            this.saveText(MODELS_CV_PATH, bestOnCrossVal.getFitness(Parameters.Operators.SR_CUDA_ROC) + " " 
                                        + bestOnCrossVal.getCrossValAreaROC() + " " 
                                        + bestOnCrossVal.getThreshold() + "\n" , true);
        } 
        // finally, deallocate dataset from shared memory
        if (firstFitnessFunction.equals(Parameters.Operators.SR_CUDA_ROC)) {
            DataCuda ed = new DataCuda(PROBLEM, TARGET_NUMBER);
            ed.deallocateDataset();
        } else if(firstFitnessFunction.equals(Parameters.Operators.SR_CPP_ROC)){
            DataCpp ed = new DataCpp(PROBLEM, TARGET_NUMBER);
            ed.deallocateDataset();
        }
        
        return bestOnCrossVal;
    }
    
    public boolean stopCriteria(){
        boolean stop = false;
        if( System.currentTimeMillis() >= TIMEOUT){
            System.out.println("Timout exceeded, exiting.");
            return true;
        }
        String firstFitnessFunction = fitnessFunctions.keySet().iterator().next();
        if(firstFitnessFunction.equals(Parameters.Operators.SR_CPP_ROC)){
            double currentFitness = best.getFitness(Parameters.Operators.SR_CPP_ROC);
            if((lastFitness==currentFitness)){
                counterConvergence++;
            }else{
                counterConvergence = 0;
                lastFitness = currentFitness;
            }
            if(counterConvergence>=15){
                stop = true;
            }
            if(currentFitness>0.99){
                stop = true;
            }
        } else if(firstFitnessFunction.equals(Parameters.Operators.SR_CUDA_ROC)){
            double currentFitness = best.getFitness(Parameters.Operators.SR_CUDA_ROC);
            if((lastFitness==currentFitness)){
                counterConvergence++;
            }else{
                counterConvergence = 0;
                lastFitness = currentFitness;
            }
            if(counterConvergence>=15){
                stop = true;
            }
            if(currentFitness>0.99){
                stop = true;
            }
        }
        return stop;
    }
        
    public static Properties loadProps(String propFile) {
            Properties props = new Properties();
            BufferedReader f;
            try {
                    f = new BufferedReader(new FileReader(propFile));
            } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
            }
            try {
                    props.load(f);
            } catch (IOException e) {
                    e.printStackTrace();
            }
            System.out.println(props.toString());
            return props;
    }

    /**
     * calculate some useful statistics about the current generation of the
     * population
     *
     * @return String of the following form:
     *         "avg_fitness fitness_std_dev avg_size size_std_dev"
     */
    protected String calculateStats() {
        double mean_f = 0.0;
        double mean_l = 0.0;
        double min_f = 1.0;
        double max_f = -1.0;
        for (Individual i : pop) {
            mean_f += i.getFitness();
            mean_l += ((Tree) i.getGenotype()).getSize();
            if (i.getFitness() < min_f) min_f = i.getFitness();
            if (i.getFitness() > max_f) max_f = i.getFitness();
        }
        mean_f /= pop.size();
        mean_l /= pop.size();
        double std_f = 0.0;
        double std_l = 0.0;
        for (Individual i : pop) {
            std_f += Math.pow(i.getFitness() - mean_f, 2);
            std_l += Math.pow(((Tree) i.getGenotype()).getSize() - mean_l, 2);
        }
        std_f = Math.sqrt(std_f / pop.size());
        std_l = Math.sqrt(std_l / pop.size());
        return String.format("%.5f %.5f %f %f %9.5f %9.5f", mean_f, std_f,min_f, max_f, mean_l, std_l);
    }
        
    /**
     * Save text to a filepath
     * @param filepath
     * @param text
     */
    protected void saveText(String filepath, String text, Boolean append) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filepath,append));
            PrintWriter printWriter = new PrintWriter(bw);
            printWriter.write(text);
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
        
    public List<String> getFuncs(){
        return FUNC_SET;
    }

    public List<String> getUnaryFuncs(){
        return UNARY_FUNC_SET;
    }
      
    
    public static void main(String args[]) throws IOException {
        System.out.println("Loading properties file");
        Properties props = null;
        if (args.length == 0) {
            System.err.println("Error: must specify a properties file to load");
            System.exit(-1);
        }
        props = loadProps(args[0]);
        // override path to problem data
        if (args.length > 1) {
            props.put(Parameters.Names.PROBLEM, args[1]);
            System.out.println("Overriding data path");
        }

        if (!props.containsKey(Parameters.Names.PROBLEM)) {
            System.err.println("Error: no data file specified in properties file");
            System.exit(-1);
        }
        System.out.println(String.format("Will load data from %s", props.get(Parameters.Names.PROBLEM)));

        long seed = Parameters.Defaults.SEED;
        if (props.containsKey(Parameters.Names.SEED))
            seed = Long.parseLong(props.getProperty(Parameters.Names.SEED));
        System.out.println(String.format("Using seed of %d", seed));
        if (props.containsKey(Parameters.Names.MODELS_PATH)) {
            File prevPopLogFile = new File(props.getProperty(Parameters.Names.MODELS_PATH));
            if (prevPopLogFile.exists()){
                System.out.println(String.format("Deleting previous models.txt at %s",props.getProperty(Parameters.Names.MODELS_PATH)));
                prevPopLogFile.delete();
            }
        }

        ClassSRBased a;
        Long newSeed = seed + 1;
        props.put(Parameters.Names.SEED, newSeed.toString());
        a = new ClassSRBased(props,1*60);
        Individual bestIndi = a.run_population();
    }    
}
