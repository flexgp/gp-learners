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
 * @author Ignacio Arnaldo
 * 
 */

package evogpj.evaluation.cpp;

import evogpj.evaluation.Expression;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

//import sun.management.counter.Variability;

/**
 * Just in time code generation to compute model error in C++
 * @author Ignacio Arnaldo
 */
public class GenerateSRCpp {

    FileWriter fw;
    Expression[] expressions;
    String code;

    // String cppCode;
    // Expression[] expressions;

    public GenerateSRCpp(String fileName) throws IOException {
        expressions = null;
        // cppCode = "";
        fw = new FileWriter(fileName);
    }

    public void setExpressions(Expression[] anExpressionArray) {
        expressions = anExpressionArray;
    }

    private void generateHeaders(int numberOfIndividuals, int numberOfLines, int numberOfVars, int numberOfResults) throws IOException {
        fw.append("#include <cmath> \n");
        fw.append("#include <fstream> \n");
        fw.append("#include <sstream> \n");
        fw.append("#include <cstdlib> \n");
        fw.append("#include <iostream> \n");
        fw.append("#include <sys/types.h>\n");
        fw.append("#include <sys/ipc.h>\n");
        fw.append("#include <sys/shm.h>\n");
        fw.append("#include <unistd.h>\n");
        fw.append("#include <cfloat>\n");
        fw.append("#include <pthread.h>\n");
        fw.append("\n");
        fw.append("using namespace std; \n");
        // add declaration of global array to keep results in
        fw.append("float *errorThread;\n");
        // add declaration of global vars for each thread to use
        fw.append("// arguments required by computeCPUIndividualXXX()\n");
        fw.append("float* sm_dataset;\n");
        fw.append("float minTarget;\n");
        fw.append("float maxTarget;\n");
        fw.append("int numberOfPoints = " + numberOfLines + ";\n");
        fw.append("int numberOfVars = " + numberOfVars + ";\n");
        fw.append("int numberOfResults = " + numberOfResults + ";\n");
        fw.append("int numberOfIndividuals = " + numberOfIndividuals + ";\n");

        fw.append("\n");

        fw.append("static inline float mydivide(float a, float b){ \n");
        fw.append("\tfloat result;\n");
        fw.append("\tif(fabs(b) < 0.000001){ \n");
        fw.append("\t\tresult = 1; \n");
        fw.append("\t}else { \n");
        fw.append("\t\t result = (a/b); \n");
        fw.append("\t} \n");
        fw.append("\treturn result; \n");
        fw.append("}\n");
        fw.append("\n");

        fw.append("static inline float mylog(float a){ \n");
        fw.append("\tfloat result; \n");
        fw.append("\tif(fabs(a) < 0.000001){ \n");
        fw.append("\t\tresult = 0.0; \n");
        fw.append("\t}else{ \n");
        fw.append("\t\tresult = log(fabs(a)); \n");
        fw.append("\t} \n");
        fw.append("\treturn result; \n");
        fw.append("} \n");
        fw.append("\n");

        fw.append("static inline float mysqrt(float a){ \n");
        fw.append("\tfloat result = sqrt(fabs(a));\n");
        fw.append("\treturn result; \n");
        fw.append("} \n");
        fw.append("\n");

        fw.append("static inline float scale(float val, float min, float max){\n");
        fw.append("\tfloat range = max - min;\n");
        fw.append("\tfloat scaled = (val - min) / range;\n");
        fw.append("\treturn scaled;\n");
        fw.append("}\n");
        fw.append("\n");
        fw.append("static inline float unscale(float val, float min, float max){\n");
        fw.append("\tfloat range = max - min;\n");
        fw.append("\tfloat unscaled = (val * range) + min;\n");
        fw.append("\treturn unscaled;\n");
        fw.append("}\n");
        fw.append("\n");
        fw.append("static inline float meanToFitness(float mean) {\n");
        fw.append("\tif (isnan(mean) || isinf(mean) || mean < 0.0 || mean >= 1.0) {\n");
        fw.append("\t\treturn 0.0;\n");
        fw.append("\t} else {\n");
        fw.append("\t\treturn (1.0 - mean) / (1 + mean);\n");
        fw.append("\t}\n");
        fw.append("}\n");

        fw.append("\n");

        fw.append("// declaration, forward \n");
        fw.append("void runTest(); \n");
        fw.append("\n");
    }

	private void translate2CPP(int indexExpression, String instruction)
			throws IOException {
		String[] instrSplit = instruction.split(" ");
		String varResult = instrSplit[0];
		String equal = instrSplit[1];
		String tokenAux = instrSplit[2];
		if (expressions[indexExpression].getFeatures().contains(tokenAux)
				|| tokenAux.startsWith("Var_")) {// tokenAux is a variable, it
													// is a binary operator
			if (instrSplit.length > 3) {// IT IS A BINARY OPERATOR
				String operand1 = tokenAux;
				String operator = instrSplit[3];
				String operand2 = instrSplit[4];
				if (operator.equals("plus") || operator.equals("+")) {
					fw.append("\t\t" + varResult + " = " + operand1 + " + "
							+ operand2 + ";\n");
				} else if (operator.equals("times") || operator.equals("*")) {
					fw.append("\t\t" + varResult + " = " + operand1 + " * "
							+ operand2 + ";\n");
				} else if (operator.equals("minus") || operator.equals("-")) {
					fw.append("\t\t" + varResult + " = " + operand1 + " - "
							+ operand2 + ";\n");
				} else if (operator.equals("mydivide") || operator.equals("/")) {
					fw.append("\t\t" + varResult + " = mydivide(" + operand1
							+ "," + operand2 + ");\n");
				} else {
					System.out
							.println("CPP Code Generation error: unrecognized operator in binary operation");
				}
			} else if (instrSplit.length == 3) {// IT IS A SINGLE VARIABLE
				fw.append("\t\t" + varResult + " = " + tokenAux + ";\n");
			}
		} else if (expressions[indexExpression].unaryOperator(tokenAux)) {// token
																			// aux
																			// is
																			// a
																			// unary
																			// operator
			String operator = tokenAux;
			String operand1 = instrSplit[3];
			if (operator.equals("sin")) {
				fw.append("\t\t" + varResult + " = sin(" + operand1 + ");\n");
			} else if (operator.equals("cos")) {
				fw.append("\t\t" + varResult + " = cos(" + operand1 + ");\n");
			} else if (operator.equals("log") || operator.equals("mylog")) {
				fw.append("\t\t" + varResult + " = mylog(" + operand1 + ");\n");
			} else if (operator.equals("exp")) {
				fw.append("\t\t" + varResult + " = exp(" + operand1 + ");\n");
			} else if (operator.equals("sqrt")) {
				fw.append("\t\t" + varResult + " = mysqrt(" + operand1 + ");\n");
			} else if (operator.equals("square")) {
				fw.append("\t\t" + varResult + " = pow(" + operand1 + ",2);\n");
			} else if (operator.equals("cube")) {
				fw.append("\t\t" + varResult + " = pow(" + operand1 + ",3);\n");
			} else if (operator.equals("quart")) {
				fw.append("\t\t" + varResult + " = pow(" + operand1 + ",4);\n");
			} else {
				System.out
						.println("CPP Code Generation error: unrecognized operator in unary operation");
			}
		} else {
			System.out
					.println("CPP Code Generation error: unrecognized third token");
		}
	}

	private void generateFunctionIndividualScaled(int index, boolean useInts,
			int p) throws IOException {
		String intermediateCode = expressions[index].getIntermediateCode();
		ArrayList<String> features = expressions[index].getFeatures();
		int varAuxCount = expressions[index].getVarCounter();

		fw.append("float computeCPUIndividual"
				+ (index + 1)
				+ "(float* dataset, int numberOfPoints, int numberOfVariables, int numberOfResults,int minTarget,int maxTarget){\n");

		fw.append("\tfloat ");
		for (int i = 0; i < features.size(); i++) {
			fw.append(features.get(i) + ",");
		}
		fw.append("result1;\n");

		fw.append("\tfloat ");
		for (int i = 0; i <= varAuxCount; i++) {
			fw.append("Var_" + i);
			if (i != varAuxCount)
				fw.append(",");
		}
		fw.append(";\n");

		fw.append("\tfloat totalDifference = 0;\n");

		fw.append("\tfloat sum = 0;\n");
		fw.append("\tint n = 0;\n");
		fw.append("\tfloat minPhenotype = FLT_MAX;\n");
		fw.append("\tfloat maxPhenotype = - FLT_MAX;\n");
		fw.append("\tfloat phenotype[numberOfPoints];\n");
		fw.append("\tfloat results[numberOfPoints];\n");
		fw.append("\tbool COERCE_INT = " + Boolean.toString(useInts) + ";\n");
		fw.append("\tint p = " + p + ";\n");

		fw.append("\tfor(int i=0;i<numberOfPoints;i++){\n");

		fw.append("\t\tint indexMemoryStart = i * (numberOfVariables + numberOfResults);\n");

		for (int i = 0; i < expressions[index].getFeatures().size(); i++) {
			String varX = expressions[index].getFeatures().get(i);
			String varXNumber = varX.substring(1, varX.length());
			int varXint = Integer.valueOf(varXNumber);
			fw.append("\t\t" + varX + " = dataset[indexMemoryStart + "
					+ (varXint - 1) + "];\n");
		}

		// fw.append("\t\tresult1 = dataset[indexMemoryStart + numberOfVariables];\n");
		fw.append("\t\tresults[i] = dataset[indexMemoryStart + numberOfVariables];\n");
		// TRANSLATE CODE
		String[] strSplit = intermediateCode.split("\n");
		for (int i = 0; i < strSplit.length; i++) {
			String instruction = strSplit[i];
			translate2CPP(index, instruction);
		}

		fw.append("\t\tphenotype[i] = Var_0;\n");
		fw.append("\t\tif(Var_0 < minPhenotype) minPhenotype = Var_0;\n");
		fw.append("\t\tif(Var_0 > maxPhenotype) maxPhenotype = Var_0;\n");
		fw.append("\t}\n");
		fw.append("\n");

		// fw.append("\tcout << \"maxPhenotype:\\t\" << maxPhenotype;\n");
		// fw.append("\tcout << \"\\t\\tminPhenotype:\\t\" << minPhenotype << endl;\n");

		fw.append("\tfor(int i=0;i<numberOfPoints;i++){\n");
		fw.append("\t\tfloat scaledValue = scale(phenotype[i],minPhenotype,maxPhenotype);\n");
		fw.append("\t\tif(COERCE_INT){\n");
		fw.append("\t\t\tint unscaledValue = unscale(scaledValue,minTarget,maxTarget);\n");
		fw.append("\t\t\tscaledValue = scale(unscaledValue,minTarget,maxTarget);\n");
		fw.append("\t\t}\n");
		fw.append("\t\tphenotype[i] = scaledValue;\n");
		fw.append("\t\t// result1 must have been scaled previously\n");
		// fw.append("\t\tfloat error = fabs(result1 - scaledValue);\n");
		fw.append("\t\tfloat error = fabs(results[i] - scaledValue);\n");
		fw.append("\t\tif(p==1){ // ARITHMETIC MEAN\n");
		fw.append("\t\t\tsum += error;\n");
		fw.append("\t\t}else if(p>1){ // POWER P MEAN\n");
		fw.append("\t\t\tsum += pow(error, p);\n");
		fw.append("\t\t}else{ // MAXIMUM\n");
		fw.append("\t\t\tif(error>sum) sum = error;\n");
		fw.append("\t\t}\n");
		fw.append("\t\tn++;\n");
		fw.append("\t}\n");
		fw.append("\tfloat mean = 0;\n");
		fw.append("\tif(p==1){// ARITHMETIC MEAN\n");
		fw.append("\t\tmean =  sum / (float) n;\n");
		fw.append("\t}else if(p>1){ // POWER P MEAN\n");
		fw.append("\t\tmean = pow(sum / (float) n, 1 / (float) p);\n");
		fw.append("\t}else{ // MAXIMUM\n");
		fw.append("\t\tmean = sum;\n");
		fw.append("\t}\n");
		fw.append("\tfloat fitness = meanToFitness(mean);\n");
		fw.append("\treturn fitness;\n");
		fw.append("}\n");

	}

	private void generateThreads(int numberOfIndividuals, String dataset,
			int numberOfLines, int numberOfVars, int numberOfResults, int numberOfThreads)
			throws IOException {

		// entry point for multithreading thread partitioning
		// the base number of individuals assigned to each thread
		int numberOfIndividualsPerThread = (int) Math.floor(numberOfIndividuals / numberOfThreads);
		// the number of extra leftover individuals to assign to threads
		int extraIndividuals = (int) numberOfIndividuals % numberOfThreads;
		int[] individualsPerThread = new int[numberOfThreads];
		// compute the number of individuals each thread is responsible for
		for (int p = 0; p < numberOfThreads; p++) {
			individualsPerThread[p] = numberOfIndividualsPerThread;
			if (extraIndividuals > 0) { // handle distributing the "extra" individuals
				extraIndividuals--;
				individualsPerThread[p]++;
			}
		}		

		// to keep track of where to start accessing individuals below
		int individualIndex = 0;

		
		
		// write each thread's method definition to file
		for (int threadID = 1; threadID <= numberOfThreads; threadID++) {

			fw.append("void* taskThread" + threadID + "(void *arg){\n");

			fw.append("\n");

			int individualEndIndex = individualIndex + individualsPerThread[threadID-1];
			// write calls to computeCPUIndividualXXX for each individual this thread is responsible for
			while (individualIndex < individualEndIndex) {
				fw.append("\terrorThread[" + individualIndex + "] = computeCPUIndividual" + (individualIndex+1)
						+ "(sm_dataset, numberOfPoints, numberOfVars,numberOfResults,minTarget,maxTarget);\n");
				individualIndex++;
			}
			fw.append("}\n");
			fw.append("\n");
		}
	}
	
	private void generateMain(int numberOfIndividuals, String dataset,
			int numberOfLines, int numberOfVars, int numberOfResults, int numberOfThreads)
			throws IOException {
		fw.append("int main(int argc, char** argv){\n");
		fw.append("\terrorThread = (float *)malloc(sizeof(float*) * " + numberOfIndividuals + ");\n");

		// system calls to get pointer to shared memory, plus minTarget and maxTarget
		fw.append("\tint shmid_semaphore, shmid_dataset, shmid_minTarget, shmid_maxTarget;\n");
		fw.append("\tkey_t key_semaphore, key_dataset, key_minTarget,key_maxTarget;\n");
		fw.append("\n");

		fw.append("\tint* sm_semaphore;\n");
		fw.append("\tfloat* sm_minTarget;\n");
		fw.append("\tfloat* sm_maxTarget;\n");
		fw.append("\n");

		fw.append("\tkey_semaphore = 1;\n");
		fw.append("\tkey_dataset = 2;\n");
		fw.append("\tkey_minTarget = 3;\n");
		fw.append("\tkey_maxTarget = 4;\n");
		fw.append("\n");
		fw.append("\tunsigned int mem_size_semaphore = sizeof(int);\n");
		fw.append("\tunsigned int size_dataset = numberOfPoints * (numberOfVars + numberOfResults);\n");
		fw.append("\tunsigned int mem_size_dataset = sizeof(float) * size_dataset;\n");
		fw.append("\tunsigned int mem_size_minTarget = sizeof(float);\n");
		fw.append("\tunsigned int mem_size_maxTarget = sizeof(float);\n");
		fw.append("\n");

		fw.append("\twhile ((shmid_semaphore = shmget(key_semaphore, mem_size_semaphore, 0666)) < 0) {}\n");
		fw.append("\t\n");
		fw.append("\tif ((sm_semaphore = (int *)shmat(shmid_semaphore, NULL, 0)) == (int *) -1) {\n");
		fw.append("\t\tperror(" + '"' + "shmat" + '"' + ");\n");
		fw.append("\t\texit(1);\n");
		fw.append("\t}\n");
		fw.append("\n");
		fw.append("\twhile (*sm_semaphore != 1)\n");
		fw.append("\t\tsleep(0.1);\n");
		fw.append("\t\n");
		fw.append("\tif ((shmid_dataset = shmget(key_dataset, mem_size_dataset, 0666)) < 0) {\n");
		fw.append("\t\tperror(" + '"' + "shmget" + '"' + ");\n");
		fw.append("\t\texit(1);\n");
		fw.append("\t}\n");
		fw.append("\n");
		
		fw.append("\tif ((shmid_minTarget = shmget(key_minTarget, mem_size_minTarget, 0666)) < 0) {\n");
		fw.append("\t\tperror(" + '"' + "shmget" + '"' + ");\n");
		fw.append("\t\texit(1);\n");
		fw.append("\t}\n");
		fw.append("\n");
		fw.append("\tif ((shmid_maxTarget = shmget(key_maxTarget, mem_size_maxTarget, 0666)) < 0) {\n");
		fw.append("\t\tperror(" + '"' + "shmget" + '"' + ");\n");
		fw.append("\t\texit(1);\n");
		fw.append("\t}\n");
		fw.append("\n");

		fw.append("\tif ((sm_dataset = (float *)shmat(shmid_dataset, NULL, 0)) == (float *) -1) {\n");
		fw.append("\t\tperror(" + '"' + "shmat" + '"' + ");\n");
		fw.append("\t\texit(1);\n");
		fw.append("\t}\n");
		fw.append("\n");
		fw.append("\tif ((sm_minTarget = (float *)shmat(shmid_minTarget, NULL, 0)) == (float *) -1) {\n");
		fw.append("\t\tperror(" + '"' + "shmat" + '"' + ");\n");
		fw.append("\t\texit(1);\n");
		fw.append("\t}\n");
		fw.append("\n");
		fw.append("\tif ((sm_maxTarget = (float *)shmat(shmid_maxTarget, NULL, 0)) == (float *) -1) {\n");
		fw.append("\t\tperror(" + '"' + "shmat" + '"' + ");\n");
		fw.append("\t\texit(1);\n");
		fw.append("\t}\n");
		fw.append("\n");

		fw.append("\tminTarget = *sm_minTarget;\n");
		fw.append("\tmaxTarget = *sm_maxTarget;\n");
		fw.append("\n");

		// add runTest invocation
		fw.append("\trunTest();\n");
		fw.append("}\n");
		fw.append("\n");

		// runTest declaration
		fw.append("void runTest(){\n");
		fw.append("\tstringstream output;\n");

		// declare thread ids
		String tids = "\tpthread_t";
		for (int id = 1; id <= numberOfThreads; id++) {
			tids += " tid" + id + ",";
		}
		// get rid of the trailing comma
		tids = tids.substring(0, tids.length()-1);
		tids += ";\n";
		fw.append(tids);
		fw.append("\n");

		// write each thread's invocation and joining
		for (int id = 1; id <= numberOfThreads; id++) {
			fw.append("\tpthread_create(&tid" + id + ", NULL, taskThread" + id + ", NULL);\n");
		}
		for (int id = 1; id <= numberOfThreads; id++) {
			fw.append("\tpthread_join(tid" + id + ", NULL);\n");
		}
		
		// write each individual's fitness to output
		for (int i = 0; i < numberOfIndividuals; i++) {
			fw.append("\toutput << " + '"' + "ErrorIndividual " + (i + 1)
					+ ": " + '"' + " << errorThread[" + i + "]"
					+ "<< endl;\n");
		}

		fw.append("\n");

		fw.append("\tofstream outFile;\n");
		fw.append("\tstring name=" + '"' + '"' + ";\n");
		fw.append("\tstringstream ssname;\n");
		fw.append("\tssname << " + '"' + "tempFiles/resultsSRCpp" + '"' + "<<" + '"'
				+ ".txt" + '"' + " ;\n");
		fw.append("\tname = ssname.str();\n");
		fw.append("\toutFile.open(name.c_str());\n");
		fw.append("\toutFile << output.str();\n");
		fw.append("\toutFile.close();\n");

		fw.append("\n");

		fw.append("}\n");

	}

	/**
	 * The main generation method.
	 * @param numberOfIndividuals
	 * @param dataset
	 * @param numberOfLines
	 * @param numberOfVars
	 * @param numberOfResults
	 * @param useInts
	 * @param p
	 * @throws IOException
	 */
	public void generateCode(int numberOfIndividuals, String dataset,
			int numberOfLines, int numberOfVars, int numberOfResults,
			int numberOfThreads, boolean useInts, int p) throws IOException {
		generateHeaders(numberOfIndividuals, numberOfLines, numberOfVars, numberOfResults);
		for (int i = 0; i < numberOfIndividuals; i++) {
			generateFunctionIndividualScaled(i, useInts, p);
		}
		generateThreads(numberOfIndividuals, dataset, numberOfLines,
				numberOfVars, numberOfResults, numberOfThreads);
		generateMain(numberOfIndividuals, dataset, numberOfLines, numberOfVars,
				numberOfResults, numberOfThreads);

	}

	public void printCodeToFile(String fileName) {
		try {
			// FileOutputStream file=new FileOutputStream(fileName);
			// file.write(cppCode.getBytes());
			// file.close();
			fw.flush();
			fw.close();
		} catch (Throwable e) {
			System.out.println("Error writing cpp file");
		}
	}

	public void compileFile(String fileName, String binName) {
		try {
			String command = "g++ -o2 -o " + binName + " -pthread " + fileName;
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		} catch (Throwable e) {
			System.out.println("Error compiling cpp file");
		}
	}

	public void runCode(String binName) {
		try {
			String command =  binName;
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		} catch (Throwable e) {
			System.out.println("Error running cpp binary");
		}
	}

	public ArrayList<Float> readResults() throws FileNotFoundException {
		String fileOutput = "tempFiles/resultsSRCpp.txt";
		ArrayList<Float> alFitness = new ArrayList<Float>();
		Scanner sc = new Scanner(new FileReader(fileOutput));
		while (sc.hasNextLine()) {
			String lineAux = sc.nextLine();
			String[] lineSplit = lineAux.split(" ");
			String fitnessSAux = lineSplit[2];
			if (fitnessSAux.equals("nan") || fitnessSAux.equals("inf")) {
				alFitness.add(Float.MAX_VALUE);
			} else {
				float fitnessFAux = Float.parseFloat(fitnessSAux);
				alFitness.add(fitnessFAux);
			}
		}
		return alFitness;
	}

}
