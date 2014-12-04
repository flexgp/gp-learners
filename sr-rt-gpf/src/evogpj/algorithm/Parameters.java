/**
 * Copyright (c) 2014 ALFA Group
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

package evogpj.algorithm;

import java.util.ArrayList;
import java.util.List;


/**
 * Simple class to collect all default values and names, so they can be found in
 * one location and properly documented.
 * 
 */
public final class Parameters {
    /**
     * Names used to identify properties in the properties file. Every key in
     * the properties file is matched against these strings by
     * {@link AlgorithmBase} to extract the new property value.
     * 
     * @author Owen Derby
     */
    public final static class Names {

        /**
         * Mutation rate
         */
        public static final String MUTATION_RATE = "mutation_rate";

        /**
         * Crossover rate
         */
        public static final String XOVER_RATE = "xover_rate";

        /**
         * Population size
         */
        public static final String POP_SIZE = "pop_size";

        /**
         * number of generations
         */
        public static final String NUM_GENS = "num_gens";

        /**
         * timeout
         */
        public static final String TIME_OUT = "timeout";

        /**
         * path to the data
         */
        public static final String PROBLEM = "problem";

        /**
         * path to data used for validation
         */
        public static final String VAL_SET = "cross_validation_set";

        /**
         * type of problem
         */
        public static final String PROBLEM_TYPE = "problem_type";

        /**
         * number of exemplars
         */
        public static final String PROBLEM_SIZE = "problem_size";

        /**
         * number of threads used in parallel execution
         */
        public static final String EXTERNAL_THREADS = "external_threads";

        /**
         * functions used to build expression trees
         */
        public static final String FUNCTION_SET = "function_set";

        /**
         * size of the function set
         */
        public static final String FUNCTION_SET_SIZE = "function_set_size";

        /**
         * unary functions used to build expression trees
         */
        public static final String UNARY_FUNCTION_SET = "unary_function_set";

        /**
         * terminal set or variables of the problem
         */
        public static final String TERMINAL_SET = "terminal_set";

        /**
         * mutation operator
         */
        public static final String MUTATE = "mutate_op";

        /**
         * crossover operator
         */
        public static final String XOVER = "xover_op";

        /**
         * selection operator
         */
        public static final String SELECTION = "selection_op";

        /**
         * fitness function
         */
        public static final String FITNESS = "fitness_op";

        /**
         * false negative weight - only applies to gpfunction and rule tree classifiers
         */
        public static final String FALSE_NEGATIVE_WEIGHT = "false_negative_weight";

        /**
         * initialization of the population
         */
        public static final String INITIALIZE = "initialize_op";

        /**
         * equalization operator (not used in multi-objective optimization)
         */
        public static final String EQUALIZER = "equalizer_op";

        /**
         * population size bins - only applies when equalization is used
         */
        public static final String BIN_WIDTH = "bin_width";

        /**
         * number of trials - defaulted to one
         */
        public static final String NUM_TRIALS = "num_trials";

        /**
         * random seed
         */
        public static final String SEED = "rng_seed";

        /**
         * initial maximum tree size
         */
        public static final String TREE_INIT_MAX_DEPTH = "tree_initial_max_depth";

        /**
         * power used to average error of the models (1-> MAE, 2-> MSE)
         */
        public static final String MEAN_POW = "fitness_mean_pow";
        
        /**
         * whether the output variable for our problem integer-valued
         */
        public static final String COERCE_TO_INT = "integer_fitness";

        /**
         * brood size used during crossover
         */
        public static final String BROOD_SIZE = "brood_size";

        /**
         * Koza's function rate constant
         */
        public static final String KOZA_FUNC_RATE = "koza_function_rate";

        /**
         * maximum depth at which crossover is performed
         */
        public static final String TREE_XOVER_MAX_DEPTH = "tree_xover_max_depth";

        /**
         * number of tries to perform crossover
         */
        public static final String TREE_XOVER_TRIES = "tree_xover_tries";

        /**
         * maximum depth at which mutation is performed
         */
        public static final String TREE_MUTATE_MAX_DEPTH = "tree_mutate_max_depth";

        /**
         * tournament size used during selection
         */
        public static final String TOURNEY_SIZE = "tourney_size";
        
        
        // For multi-objective optimization
        /**
         * methods for sorting fronts of solutions
         */
        public static final String FRONT_RANK_METHOD = "front_rank_method";

        /**
         * sort fronts by Euclidean distance of the objectives to the origin
         */
        public static final String EUCLIDEAN = "euclidean";

        /**
         * sort fronts by the first evaluated fitness function
         */
        public static final String FIRST_FITNESS = "first_fitness"; 
        
        // FOR LOGGING PURPOSES
        /**
         * path to the main json log
         */
        public static final String JSON_LOG_PATH= "json_path";
        
        /**
         * path to the file where the population will be logged
         */
        public static final String POP_SAVE_FILENAME = "pop_save_filename";

        /**
         * path to the file where the population will be loaded from
         */
        public static final String POP_LOAD_FILENAME = "pop_load_filename";

        /**
         * path to the file where the data population will be saved
         */
        public static final String POP_DATA_PATH = "pop_data_path";

        /**
         * prefix to the path to the file where the data population will be saved
         */
        public static final String POP_DATA_PATH_PREFIX = "pop_data_path_prefix";
        
        /**
         * path to the file where the best models will be logged
         */
        public static final String MODELS_PATH = "models_path";	
        
        /**
         * whether to log the population
         */
        public static final String ENABLE_POP_LOG = "enable_pop_log";
        
        /**
         * display trees in prefix format
         */
        public static final String PREFIX_SCHEME = "prefix";

        /**
         * display trees in infix format
         */
        public static final String INFIX_MATLAB = "infix";
        
        /**
         * Should JSON logs be enabled?
         */
        public static final String SAVE_JSON = "save_json";
    }

    /**
     * Names for specific operators, as understood by the library when reading
     * in values from the properties file.
     * 
     * @author Owen Derby and Ignacio Arnaldo
     */
    public final static class Operators {

        // FITNESS values
        /**
         * Symbolic Regression MSE in Java
         */
        public static final String SR_JAVA_FITNESS = "fitness.SRFitness.Java";

        /**
         * Symbolic Regression MSE in optimized C++
         */
        public static final String SR_CPP_FITNESS = "fitness.SRFitness.Cpp";

        /**
         * Symbolic Regression MSE in CUDA
         */
        public static final String SR_CUDA_FITNESS = "fitness.SRFitness.Cuda";

        /**
         * Symbolic Regression MSE in dual GPU CUDA execution
         */
        public static final String SR_CUDA_FITNESS_DUAL = "fitness.SRFitness.CudaDual";

        /**
         * Symbolic Regression Correlation in CUDA
         */
        public static final String SR_CUDA_FITNESS_CORRELATION = "fitness.SRFitness.CudaCorrelation";

        /**
         * Symbolic Regression Correlation in CUDA in dual GPU execution
         */
        public static final String SR_CUDA_FITNESS_CORRELATION_DUAL = "fitness.SRFitness.CudaCorrelationDual";

        /**
         * GPFunction Classification AUC in Java
         */
        public static final String GPFUNCTION_JAVA = "fitness.GPFunctionFitness.Java";

        /**
         * GPFunction Classification AUC in Java on validation set
         */
        public static final String GPFUNCTION_CV_JAVA = "fitness.GPFunctionCVFitness.Java";

        /**
         * GPFunction Classification AUC in C++
         */
        public static final String GPFUNCTION_CPP = "fitness.GPFunctionFitness.Cpp";

        /**
         * GPFunction Classification AUC in CUDA
         */
        public static final String GPFUNCTION_CUDA = "fitness.GPFunctionFitness.Cuda";

        /**
         * GPFunction Classification AUC in C++ on validation set
         */
        public static final String GPFUNCTION_CV_CPP = "fitness.GPFunctionCVFitness.Cpp";

        /**
         * GPFunction Classification AUC in CUDA
         */
        public static final String GPFUNCTION_CV_CUDA = "fitness.GPFunctionCVFitness.Cuda";

        /**
         * GPFunction Classification predict in CUDA
         */
        public static final String GPFUNCTION_PRED_CUDA = "fitness.GPFunctionPredFitness.Cuda";
        
        /**
         * GPFunction-KDE Classification in Java
         */
        public static final String GPFUNCTION_KDE_JAVA = "fitness.GPFunctionKDEFitness.Java";

        /**
         * GPFunction-KDE Classification in C++
         */
        public static final String GPFUNCTION_KDE_CPP = "fitness.GPFunctionKDEFitness.Cpp";

        
        /**
         * RULE TREE CLASSIFIER in Java
         */
        public static final String RT_COST_JAVA_FITNESS = "fitness.RT_Cost_Fitness.Java";

        /**
         * RULE TREE CLASSIFIER multi-objective optimization (False Negative rate - False Positive rate) in Java
         */
        public static final String RT_MO_JAVA_FITNESS = "fitness.RT_MO_Fitness.Java";

        /**
         * RULE TREE CLASSIFIER False Positive rate in Java
         */
        public static final String RT_FP_JAVA_FITNESS = "fitness.RT_FP_Fitness.Java";

        /**
         * RULE TREE CLASSIFIER False Negative rate in Java
         */
        public static final String RT_FN_JAVA_FITNESS = "fitness.RT_FN_Fitness.Java";
        
        /**
         * Subtree Complexity
         */
        public static final String SUBTREE_COMPLEXITY_FITNESS = "fitness.SubtreeComplexity";

        /**
         * standard tree initialization GP
         */
        public static final String TREE_INITIALIZE = "operator.TreeInitialize";
        
        /**
         * tournament selection operator
         */
        public static final String TOURNEY_SELECT = "operator.TournamentSelection";

        /**
         * crowd tournament selection operator
         */
        public static final String CROWD_SELECT = "operator.CrowdedTournamentSelection";

        /**
         * tournament equalization operator
         */
        public static final String TOURNEY_EQUAL = "operator.TournamentEqualization";

        /**
         * dynamic equalization operator
         */
        public static final String TREE_DYN_EQUAL = "operator.TreeDynamicEqualizer";

        /**
         * dummy equalization operator
         */
        public static final String DUMB_EQUAL = "operator.DummyEqualizer";

        /**
         * dummy tree equalization operator
         */
        public static final String DUMB_TREE_EQUAL = "operator.DummyTreeEqualizer";

        /**
         * subtree mutation
         */
        public static final String SUBTREE_MUTATE = "operator.SubtreeMutate";

        /**
         * brood selection operator
         */
        public static final String BROOD_XOVER = "operator.BroodSelection";
        // single point uniform crossover

        /**
         * single point uniform crossover operator
         */
        public static final String SPU_XOVER = "operator.SinglePointUniformCrossover";
        // single point Koza crossover

        /**
         * single point Koza crossover operator
         */
        public static final String SPK_XOVER = "operator.SinglePointKozaCrossover";
    }

    /**
     * All default values for running the library.
     * <p>
     * To specify other values, please use the properties file.
     * 
     * @author Owen Derby
     */
    public final static class Defaults {
        /**
         * verbosity flag. Helpful for debugging.
         */
        public static final Boolean VERBOSE = false;

        /**
         * population size
         */
        public static final int POP_SIZE = 1000;

        /**
         * number of generations
         */
        public static final int NUM_GENS = 10000;

        /**
         * timeout in minutes
         */
        public static final int TIME_OUT = 60;
        
        // Frequency for selecting each operator
        // reproduction/replication frequency is implicitly defined as
        // (1 - XOVER_RATE - MUTATION_RATE)
        /**
         * mutation rate
         */
        public static final double MUTATION_RATE = 0.1;

        /**
         * crossover rate
         */
        public static final double XOVER_RATE = 0.7;
        
        /**
         * number of best individuals to carry over to next generation
         */
        public static final int ELITE = 3;
        

        /**
         * initial maximum tree size
         */
        public static final int TREE_INIT_MAX_DEPTH = 10;

        /**
         * population size bin width - only applies when equalization is used
         */
        public static final int BIN_WIDTH = 5;
        
        /**
         * The power p to use in the power mean for computing model error.
         */
        public static final int MEAN_POW = 2;

        /**
         * brood size
         */
        public static final int BROOD_SIZE = 1;
        
        /**
         * default value of 90% suggested by Koza.
         */
        public static final double KOZA_FUNC_RATE = .9;

        /**
         * maximum tree depth after crossover
         */
        public static final int TREE_XOVER_MAX_DEPTH = 17;

        /**
         * maximum number of crossover tries
         */
        public static final int TREE_XOVER_TRIES = 10;

        /**
         * maximum tree depth after mutation
         */
        public static final int TREE_MUTATE_MAX_DEPTH = 17;

        /**
         * tournament size used in the selection step
         */
        public static final int TOURNEY_SIZE = 7;

        /**
         * default number of exemplars -- dummy value
         */
        public static final int PROBLEM_SIZE = 3;

        /**
         * default type of problem -- regression
         */
        public static final String PROBLEM_TYPE = "SRFunction";

        /**
         * default pair of objectives for regression: fitness in Java + subtree complexity
         */
        public static final String SRFITNESS = "fitness.SRFitness.Java, fitness.SubtreeComplexity";

        /**
         * default pair of objectives for classification via GPfunction: fitness in Java + subtree complexity
         */
        public static final String GPFUNCTION_FITNESS = "fitness.GPFunctionFitness.Java, fitness.SubtreeComplexity";

        /**
         * default pair of objectives for classification via GPfunctionMAP: fitness in Java + subtree complexity
         */
        public static final String GPFUNCTIONKDE_FITNESS = "fitness.GPFunctionKDEFitness.Java, fitness.SubtreeComplexity";

        /**
         * default pair of objectives for classification via Ruletree: fitness in Java + subtree complexity
         */
        public static final String RULETREE_FITNESS = "fitness.RT_MO_Fitness.Java, fitness.SubtreeComplexity";

        /**
         * default path to training data
         */
        public static final String PROBLEM = "ProblemData/TrainDatasetBalanced2.txt";

        /**
         * default path to validation set
         */
        public static final String VAL_SET = "ProblemData/TrainDatasetBalanced2.txt";

        /**
         * default number of threads
         */
        public static final int EXTERNAL_THREADS = 1;

        /**
         * default number of target vectors
         */
        public static final int TARGET_NUMBER = 1;
        
        /**
         * default false negative weight
         */
        public static final double FALSE_NEGATIVE_WEIGHT = 0.5;
        
        /**
         * the initial seed to use for the rng in the algorithm.
         */
        public static final long SEED = System.currentTimeMillis();
        
        /**
         * Normally regression is over real numbers. Sometimes we want to do
         * regression over Integers. Set this to true to do so.
         */
        public static final boolean COERCE_TO_INT = false;

        /**
         * default tree initialization operator
         */
        public static final String INITIALIZE = Operators.TREE_INITIALIZE;

        /**
         * default selection operator
         */
        public static final String SELECT = Operators.CROWD_SELECT;

        /**
         * default mutation operator
         */
        public static final String MUTATE = Operators.SUBTREE_MUTATE;

        /**
         * default equalization operator
         */
        public static final String EQUALIZER = Operators.TREE_DYN_EQUAL;

        /**
         * default crossover operator
         */
        public static final String XOVER = Operators.SPU_XOVER;
        
        /**
         * default set of terminals
         */
        public static final List<String> TERMINALS;
        static {
                TERMINALS = new ArrayList<String>();
                TERMINALS.add("X1");
        }

        /**
         * default set of functions used to generate expression trees
         */
        public static final List<String> FUNCTIONS;
        static {
                FUNCTIONS = new ArrayList<String>();
                FUNCTIONS.add("+");
                FUNCTIONS.add("*");
                FUNCTIONS.add("-");
                FUNCTIONS.add("mydivide");
                
                FUNCTIONS.add("mylog");
                FUNCTIONS.add("exp");
                FUNCTIONS.add("sin");
                FUNCTIONS.add("cos");
                FUNCTIONS.add("sqrt");
                FUNCTIONS.add("square");
                FUNCTIONS.add("cube");
                FUNCTIONS.add("quart");
        }
        
        /**
         * the main JSON log's default name
         */
        public static final String JSON_LOG_PATH= "evogpj-log.json";

        /**
         * path to the file where the population is serialized
         */
        public static final String POP_SAVE_FILENAME = "evogpj-population.ser";

        /**
         * path to load the serialized population
         */
        public static final String POP_LOAD_FILENAME = POP_SAVE_FILENAME;

        /**
         * default prefix to the path to save the population
         */
        public static final String POP_DATA_PATH_PREFIX = "populationLog";

        /**
         * default path to save the population
         */
        public static final String POP_DATA_PATH = POP_DATA_PATH_PREFIX
                        + "-unspecifiedAlgorighmType-unspecifiedSeed.txt";

        /**
         * default path to save the best models of the generation
         */
        public static final String MODELS_PATH = "bestModelGeneration.txt";

        /**
         * default path to save the data-driven condition - only applies for 
         * the Ruletree learner
         */
        public static final String CONDITIONS_PATH = "conditions.txt";

        /**
         * default path to save the best model on the validation set
         */
        public static final String MODELS_CV_PATH = "bestCrossValidation.txt";

        /**
         * default path to save the Pareto front
         */
        public static final String PARETO_PATH = "pareto.txt";

        /**
         * default path to save the simplest model of the Pareto front
         */
        public static final String LEAST_COMPLEX_PATH = "leastComplex.txt";

        /**
         * default path to save the most accurate model of the Pareto front
         */
        public static final String MOST_ACCURATE_PATH = "mostAccurate.txt";

        /**
         * default path to save the knee model of the Pareto front
         */
        public static final String KNEE_PATH = "knee.txt";

        /**
         * default path to save the fused model of the Pareto front
         */
        public static final String FUSED_PATH = "fusedModel.txt";

        /**
         * default path to save the result
         */
        public static final String RESULT_PATH = "result.txt";

        /**
         * default flag to save the population -- activate for research purposes
         */
        public static final String ENABLE_POP_LOG = "false";

        /**
         * default flag to cache predictions
         */
        public static final String CACHE_PREDICTIONS = "false";

        /**
         * default method to sort solutions within a front
         */
        public static final String FRONT_RANK_METHOD = Names.FIRST_FITNESS;

        /**
         * default flag to log the run in a json file
         */
        public static final String SAVE_JSON = "false";
    }
}