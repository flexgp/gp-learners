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

import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import evogpj.evaluation.FitnessFunction;
import evogpj.evaluation.java.SRModelScalerJava;
import evogpj.evaluation.java.SRJava;
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

import evogpj.operator.CrowdedTournamentSelection;
import evogpj.operator.Equalizer;
import evogpj.operator.SinglePointKozaCrossover;
import evogpj.operator.SinglePointUniformCrossover;
import evogpj.operator.SubtreeMutate;
import evogpj.operator.TournamentEqualization;
import evogpj.operator.TournamentSelection;
import evogpj.operator.TreeDynamicEqualizer;
import evogpj.operator.TreeInitialize;
import java.util.LinkedList;

/**
 * This contains the main method that runs the entire GP algorithm.
 * 
 * @author Owen Derby
 **/
public class SymRegDynEqGP {
    
    /* DATA */
    // SYMBOLIC REGRESSION ON FUNCTION OR DATA
    protected String PROBLEM_TYPE = Parameters.Defaults.PROBLEM_TYPE;
    // TRAINING SET
    protected String PROBLEM = Parameters.Defaults.PROBLEM;
    protected int PROBLEM_SIZE = Parameters.Defaults.PROBLEM_SIZE;
    // INTEGER TARGETS
    protected boolean COERCE_TO_INT = Parameters.Defaults.COERCE_TO_INT;
    // FEATURES
    protected List<String> TERM_SET;
    // CROSS-VALIDATION SET FOR SYMBOLIC REGRESSION-BASED CLASSIFICATION
    protected String CROSS_VAL_SET = Parameters.Defaults.CROSS_VAL_SET;
    
    
    /* PARAMETERS GOVERNING THE GENETIC PROGRAMMING PROCESS */
    // POPULATION SIZE
    protected int POP_SIZE = Parameters.Defaults.POP_SIZE;
    // NUMBER OF GENERATIONS
    protected int NUM_GENS = Parameters.Defaults.NUM_GENS;
    // MUTATION RATE
    protected double MUTATION_RATE = Parameters.Defaults.MUTATION_RATE;
    // CROSSOVER RATE
    protected double XOVER_RATE = Parameters.Defaults.XOVER_RATE;
    
    
    // DEFAULT MUTATION OPERATOR
    protected String INITIALIZE = Parameters.Defaults.INITIALIZE;
    // DEFAULT ELITISM SIZE
    protected int ELITE = Parameters.Defaults.ELITE;
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
    // EQUALIZER EMPLOYED TO CONTROL FOR BLOATING
    protected String EQUALIZER = Parameters.Defaults.EQUALIZER;
    
    // ALL THE OPERATORS USED TO BUILD GP TREES
    protected List<String> FUNC_SET;
    // UNARY OPERATORS USED TO BUILD GP TREES
    protected List<String> UNARY_FUNC_SET;  
    
    // RANDOM SEED
    protected Long SEED = Parameters.Defaults.SEED;
    
    /* LOG FILES*/
    // LOG BEST INDIVIDUAL PER GENERATION
    protected String MODELS_PATH = Parameters.Defaults.MODELS_PATH;
    // LOG BEST INDIVIDUAL WITH RESPECT TO CROSS-VALIDATION SET IN CLASSIFICATION
    protected String MODELS_CV_PATH = Parameters.Defaults.MODELS_CV_PATH;    
    

    /* CANDIDATE SOLUTIONS MAINTAINED DURING THE SEARCH */
    // CURRENT POPULATION
    protected evogpj.gp.Population pop;
    // OFFSPRING
    protected evogpj.gp.Population childPop;
    // OFFSPRING + PARENTS
    protected evogpj.gp.Population totalPop;
    // CURRENT NON-DOMINATED SOLUTIONS
    protected evogpj.gp.Population paretoFront;
    // CURRENT GENERATION'S BEST INDIVIDUAL
    protected evogpj.gp.Individual best;
    // BEST INDIVIDUAL OF EACH GENERATION
    protected evogpj.gp.Population bestPop;
    
    
    /* OPERATORS EMPLOYED IN THE SEARCH PROCESS */
    // RANDOM NUMBER GENERATOR
    protected evogpj.gp.MersenneTwisterFast rand;
    // INITIALIZATION METHOD
    protected evogpj.operator.Initialize initialize;
    // CROSSOVER
    protected evogpj.operator.Crossover xover;
    // SELECTION
    protected evogpj.operator.Select select;
    // MUTATION
    protected evogpj.operator.Mutate mutate;
    // FITNESS FUNCTIONS
    protected LinkedHashMap<String, evogpj.evaluation.FitnessFunction> fitnessFunctions;
    // POPULATION EQUALIZER
    protected Equalizer equalizer;
    
    /* CONTROL FOR END OF EVOLUTIONARY PROCESS*/
    // CURRENT GENERATION
    protected Integer generation;
    // CONTROL FOR END OF PROCESS
    protected Boolean finished;
    // NUMBER OF GENERATIONS WITHOUT FITNESS IMPROVEMENT
    protected int counterConvergence;
    // CURRENT FITNESS OF BEST INDIVIDUAL
    protected double lastFitness;
    
    // LINEAR SCALING OF BEST INDIVIDUAL PER GENERATION
    protected SRModelScalerJava modelScalerJava;

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
    public SymRegDynEqGP(Properties props) throws IOException {
        this();
        loadParams(props);
        create_operators(props);
    }

    /**
     * Empty constructor, to allow subclasses to easily override
     */
    public SymRegDynEqGP() {
        fitnessFunctions = new LinkedHashMap<String, FitnessFunction>();
        finished = false;
        generation = 0;
    }

    /**
     * Read parameters from the Property object and set Algorithm variables.
     * 
     * @see Parameters
     */
    protected void loadParams(Properties props) {
        if (props.containsKey(Parameters.Names.SEED))
            SEED = Long.valueOf(props.getProperty(Parameters.Names.SEED)).longValue();
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
        if (props.containsKey(Parameters.Names.EQUALIZER))
            EQUALIZER = props.getProperty(Parameters.Names.EQUALIZER);
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
     * Create all the operators from the loaded params.
     */
    protected void create_operators(Properties props) throws IOException {
        create_operators(props, SEED);
    }

    /**
     * Create all the operators from the loaded params.
     * 
     * Seed is the seed to use for the rng. If specified, dataIn is some Data to
     * use. Otherwise, d_in should be null and fitness will load in the
     * appropriate data.
     * 
     * @param seed
     * @param dataIn
     */
    protected void create_operators(Properties props, long seed) throws IOException {
        System.out.println("Running evogpj with seed: " + seed);
        rand = new MersenneTwisterFast(seed);
        fitnessFunctions = splitFitnessOperators(FITNESS);
        // add all fitness functions indicated in the properties
        for (String fitnessOperatorName : fitnessFunctions.keySet()) {
            // Set up fitness evaluator
            DataJava data = new CSVDataJava(PROBLEM);
            if (TERM_SET == null) {
                TERM_SET = new ArrayList<String>();
                for (int i = 0; i < data.getNumberOfFeatures(); i++) TERM_SET.add("X" + (i + 1));
                System.out.println(TERM_SET);
            }
            fitnessFunctions.put(fitnessOperatorName,new SRJava(data, props,fitnessOperatorName));
            modelScalerJava = new SRModelScalerJava(data);
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
        } else if (SELECT.equals(Parameters.Operators.TOURNEY_EQUAL)) {
            select = new TournamentEqualization(rand, props);
        } else {
            System.err.format("Invalid select function %s specified%n", SELECT);
            System.exit(-1);
        }
        if (MUTATE.equals(Parameters.Operators.SUBTREE_MUTATE)) {
            mutate = new SubtreeMutate(rand, props, treeGen);
        } else {
            System.err.format("Invalid mutate function %s specified%n", MUTATE);
            System.exit(-1);
        }
        if (XOVER.equals(Parameters.Operators.SPU_XOVER)) {
            xover = new SinglePointUniformCrossover(rand, props);
        } else if (XOVER.equals(Parameters.Operators.SPK_XOVER)) {
            xover = new SinglePointKozaCrossover(rand, props);
        } else {
            System.err.format("Invalid crossover function %s specified%n",XOVER);
            System.exit(-1);
        }

        // set up the initial population
        pop = initialize.initialize(POP_SIZE);
        // initialize totalPop to simply the initial population
        totalPop = pop;
        // initial evaluation of all individuals
        for (FitnessFunction f : fitnessFunctions.values())
            f.evalPop(pop);

        // initialize equalizer if using dynamic equalization
        try {
            if (SELECT.equals(Parameters.Operators.TOURNEY_EQUAL)) {
                equalizer = (Equalizer) select;
            } else if (EQUALIZER.equals(Parameters.Operators.TREE_DYN_EQUAL)) {
                equalizer = new TreeDynamicEqualizer(pop, props);
            } 
            equalizer.update(pop);
        } catch (GPException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Run standard elitism function. The number of best individuals to return
     * is specified by the <code>elite</code> parameter.
     * 
     * This method was written and will run correctly for problems
     * with only one fitness function.
     * 
     * @return a Population of size {@link #ELITE} of the top individuals in the
     *         current {@link #pop}
     */
    protected Population elitism(Integer numEliteIndividuals) {
        LinkedList<Individual> elite = new LinkedList<Individual>();
        Population newPop = new Population();
        if (numEliteIndividuals == 0)
            return newPop;
        for (int i = 0; i < numEliteIndividuals; i++) {
            elite.add(pop.get(i));
        }
        Double f;
        for (Individual i : pop) {
            f = i.getFitness();
            for (int j = 0; j < numEliteIndividuals; j++) {
                if (f > elite.get(j).getFitness()) {
                    elite.add(j, i);
                    break;
                }
            }
        }
        for (int i = 0; i < numEliteIndividuals; i++) {
            newPop.add(elite.get(i));
        }
        return newPop;
    }

    protected Population elitism() {
        return elitism(ELITE);
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
        Population childPop = elitism();
        equalizer.update(childPop);
        Population children;
        while (childPop.size() < POP_SIZE) {
            Individual p1 = select.select(pop);
            String s1 = null;
            children = new Population();
            double prob = rand.nextDouble();
            if (prob < XOVER_RATE) {
                Individual p2 = select.select(pop);
                Tree g2 = (Tree) p2.getGenotype();
                children = xover.crossOver(p1, p2);
            } else if (prob < MUTATION_RATE + XOVER_RATE) {
                children.add(mutate.mutate(p1));
            } else {
                children.add(p1.copy());
            }
            FitnessFunction f = fitnessFunctions.get(Parameters.Operators.SR_JAVA_FITNESS);
            f.evalPop(children);
            for (Individual i : children) {
                if (equalizer.accept(i)) childPop.add(i);
            }
        }
        pop = childPop;
        pop.sort();
        best = pop.get(0);
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
            System.out.println(best.getFitnesses());
            System.out.flush();

            bestPop.add(best);
            generation++;
            finished = stopCriteria();
            
        }

        String firstFitnessFunction = fitnessFunctions.keySet().iterator().next();
        if (firstFitnessFunction.equals(Parameters.Operators.SR_JAVA_FITNESS) ){
            //scale and save best model of each iteration
            for(Individual ind:bestPop){
                modelScalerJava.scaleModel(ind);
                this.saveText(MODELS_PATH, ind.toScaledString() + "\n", true);
            }
        }  
        return bestOnCrossVal;
    }

	
    public boolean stopCriteria(){
        boolean stop = false;
        String firstFitnessFunction = fitnessFunctions.keySet().iterator().next();
        if(firstFitnessFunction.equals(Parameters.Operators.SR_JAVA_FITNESS)){
            double currentFitness = best.getFitness(Parameters.Operators.SR_JAVA_FITNESS);
            if(currentFitness>0.999){
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

        String prevModelsPath = Parameters.Defaults.MODELS_PATH;
        if (props.containsKey(Parameters.Names.MODELS_PATH)) {
            prevModelsPath = props.getProperty(Parameters.Names.MODELS_PATH);
        }
        File prevModelsFile = new File(prevModelsPath);
        if (prevModelsFile.exists())
            System.out.println(String.format("Deleting previous models.txt at %s",prevModelsPath));
        prevModelsFile.delete();

        String prevJSONPath = Parameters.Defaults.JSON_LOG_PATH;
        if (props.containsKey(Parameters.Names.JSON_LOG_PATH)) {
            prevJSONPath = props.getProperty(Parameters.Names.JSON_LOG_PATH);
        }
        File prevJSONFile = new File(prevJSONPath);
        if (prevJSONFile.exists())
            System.out.println(String.format("Deleting previous JSON log at %s",prevJSONPath));
        prevJSONFile.delete();

        String prevPopLogPath = Parameters.Defaults.POP_DATA_PATH;
        if (props.containsKey(Parameters.Names.POP_DATA_PATH)) {
            prevPopLogPath = props.getProperty(Parameters.Names.POP_DATA_PATH);
        }
        File prevPopLogFile = new File(prevPopLogPath);
        if (prevPopLogFile.exists())
            System.out.println(String.format("Deleting previous population log at %s",prevPopLogPath));
        prevPopLogFile.delete();

        System.out.println("Entering trial loop");
        SymRegDynEqGP a;

        Long newSeed = seed;
        props.put(Parameters.Names.SEED, newSeed.toString());
        System.out.println("Initializing AlgorithmBase");
        a = new SymRegDynEqGP(props);

        System.out.println("Starting trial");
        a.run_population();

        System.out.println("Finished evolution, terminating.");
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
            // FIXME fitness overloading: think about how to handle this better
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
            // FIXME fitness overloading: think about how to handle this better
            std_f += Math.pow(i.getFitness() - mean_f, 2);
            std_l += Math.pow(((Tree) i.getGenotype()).getSize() - mean_l, 2);
        }
        std_f = Math.sqrt(std_f / pop.size());
        std_l = Math.sqrt(std_l / pop.size());
        return String.format("%.5f %.5f %f %f %9.5f %9.5f", mean_f, std_f,min_f, max_f, mean_l, std_l);
    }


    /**
     * Save information about number of fitness evaluations made to a file
     * @param iteration
     * @param generation
     * @param generationTimestamp
     */
    protected void saveFitnessEvaluationData(Integer iteration,Integer generation, java.sql.Timestamp generationTimestamp,Integer numFitnessEvaluations) {
        // format: timestamp iteration generation numFitnessEvaluations
        String newString = String.format("%s %d %d %d%n", generationTimestamp.toString(), iteration, generation, numFitnessEvaluations);
    }
	
    /**
     * Save a text to a filepath
     * @param filepath
     * @param text
     */
    protected void saveText(String filepath, String text, Boolean append) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(filepath,	append));
            PrintWriter printWriter = new PrintWriter(bw);
            printWriter.write(text);
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}