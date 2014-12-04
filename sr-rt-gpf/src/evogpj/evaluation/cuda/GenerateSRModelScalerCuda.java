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
package evogpj.evaluation.cuda;

import evogpj.evaluation.Expression;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

//import sun.management.counter.Variability;

/**
 * Just in time CUDA code generation to scale symbolic regression models
 * @author Ignacio Arnaldo
 */
public class GenerateSRModelScalerCuda {

	FileWriter fw;
	Expression[] expressions;
    
	// String cppCode;
	// Expression[] expressions;

	public GenerateSRModelScalerCuda(String fileName) throws IOException {
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
		// add declaration of global array to keep slopes and intercepts
		fw.append("float *slopes;\n");
                fw.append("float *intercepts;\n");
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
                
		fw.append("inline float mydivide(float a, float b){ \n");
		fw.append("\tfloat result;\n");
		fw.append("\tif(fabs(b) < 0.000001){ \n");
		fw.append("\t\tresult = 1; \n");
		fw.append("\t}else { \n");
		fw.append("\t\t result = (a/b); \n");
		fw.append("\t} \n");
		fw.append("\treturn result; \n");
		fw.append("}\n");
		fw.append("\n");

		fw.append("inline float mylog(float a){ \n");
		fw.append("\tfloat result; \n");
		fw.append("\tif(fabs(a) < 0.000001){ \n");
		fw.append("\t\tresult = 0.0; \n");
		fw.append("\t}else{ \n");
		fw.append("\t\tresult = log(fabs(a)); \n");
		fw.append("\t} \n");
		fw.append("\treturn result; \n");
		fw.append("} \n");
		fw.append("\n");

		fw.append("inline float mysqrt(float a){ \n");
		fw.append("\tfloat result = sqrt(fabs(a));\n");
		fw.append("\treturn result; \n");
		fw.append("} \n");
		fw.append("\n");

		fw.append("inline float scale(float val, float min, float max){\n");
		fw.append("\tfloat range = max - min;\n");
		fw.append("\tfloat scaled = (val - min) / range;\n");
		fw.append("\treturn scaled;\n");
		fw.append("}\n");
		fw.append("\n");
		fw.append("inline float unscale(float val, float min, float max){\n");
		fw.append("\tfloat range = max - min;\n");
		fw.append("\tfloat unscaled = (val * range) + min;\n");
		fw.append("\treturn unscaled;\n");
		fw.append("}\n");
		fw.append("\n");
		fw.append("inline float meanToFitness(float mean) {\n");
		fw.append("\tif (isnan(mean) || isinf(mean) || mean < 0.0 || mean >= 1.0) {\n");
		fw.append("\t\treturn 0.0;\n");
		fw.append("\t} else {\n");
		fw.append("\t\treturn (1.0 - mean) / (1 + mean);\n");
		fw.append("\t}\n");
		fw.append("}\n");

		fw.append("\n");

		fw.append("// declaration, forward \n");
		fw.append("void runTest(); \n");

		// don't need these declarations
//		for (int i = 0; i < numberOfIndividuals; i++) {
//			fw.append("float computeCPUIndividual");
//			String saux = Integer.toString(i + 1);
//			fw.append(saux);
//			fw.append("(float*, int, int, int); \n");
//		}

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

                fw.append("void scaleIndividual" + (index + 1) + "(float* dataset, int numberOfPoints, int numberOfVariables, int numberOfResults,int indId){\n");

		fw.append("\tfloat ");
		for (int i = 0; i < features.size(); i++) {
			fw.append(features.get(i));
                        if (i != (features.size()-1)) fw.append(",");
		}
		fw.append(";\n");

		fw.append("\tfloat ");
		for (int i = 0; i <= varAuxCount; i++) {
			fw.append("Var_" + i);
			if (i != varAuxCount)
				fw.append(",");
		}
		fw.append(";\n");

		fw.append("\tfloat predictions[numberOfPoints];\n");
                fw.append("\tfloat predictionMean = 0;\n");
		fw.append("\tfloat targets[numberOfPoints];\n");
                fw.append("\tfloat targetMean = 0;\n");
                fw.append("\tbool COERCE_INT = " + Boolean.toString(useInts) + ";\n");

		fw.append("\tfor(int i=0;i<numberOfPoints;i++){\n");

		//fw.append("\t\tint indexMemoryStart = i * (numberOfVariables + numberOfResults);\n");
                fw.append("\t\tint offset = i;\n");

		for (int i = 0; i < expressions[index].getFeatures().size(); i++) {
			String varX = expressions[index].getFeatures().get(i);
			String varXNumber = varX.substring(1, varX.length());
			int varXint = Integer.valueOf(varXNumber);
			//fw.append("\t\t" + varX + " = dataset[indexMemoryStart + "+ (varXint - 1) + "];\n");
                        //X78 = dataset[77*numberOfPoints + offset]
                        fw.append("\t\t" + varX + " = dataset["+ (varXint - 1) + " * numberOfPoints + offset];\n");    
		}
                //fw.append("\t\ttargets[i] = dataset[indexMemoryStart + numberOfVariables];\n");
                fw.append("\t\ttargets[i] = dataset[numberOfPoints*numberOfVariables + offset];\n");
                fw.append("\t\ttargets[i] = unscale(targets[i],minTarget,maxTarget);\n");
                fw.append("\t\ttargetMean += targets[i];\n");
		
		// TRANSLATE CODE
		String[] strSplit = intermediateCode.split("\n");
		for (int i = 0; i < strSplit.length; i++) {
			String instruction = strSplit[i];
			translate2CPP(index, instruction);
		}

		fw.append("\t\tpredictions[i] = Var_0;\n");
                fw.append("\t\tpredictionMean += predictions[i];\n");
                
		fw.append("\t}\n");
		fw.append("\n");
                
                fw.append("\ttargetMean = targetMean / numberOfPoints;\n");
                fw.append("\tpredictionMean = predictionMean / numberOfPoints;\n");
                fw.append("\tfloat numSum = 0.0;\n");
                fw.append("\tfloat denSum = 0.0;\n");
                fw.append("\tfor (int i = 0; i < numberOfPoints; i++){\n");
                fw.append("\t\tfloat predictionDiff = predictions[i] - predictionMean;\n");
                fw.append("\t\tfloat targetDiff = targets[i] - targetMean;\n");
                fw.append("\t\tnumSum += (predictionDiff * targetDiff);\n");
                fw.append("\t\tdenSum += pow(predictionDiff,2);\n");
                fw.append("\t}\n");
                fw.append("\tfloat slope = numSum / denSum;\n");
                fw.append("\tfloat intercept = targetMean - (slope * predictionMean);\n");
                fw.append("\tslopes[indId] = slope;\n");
                fw.append("\tintercepts[indId] = intercept;\n");


		fw.append("}\n");

	}

	private void generateThreads(int numberOfIndividuals, String dataset,int numberOfLines, int numberOfVars, int numberOfResults, int numberOfThreads)
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
			// write calls to scaleIndividualXXX for each individual this thread is responsible for
			while (individualIndex < individualEndIndex) {
				fw.append("\tscaleIndividual" + (individualIndex+1) + "(sm_dataset, numberOfPoints, numberOfVars,numberOfResults," + individualIndex + ");\n");
				individualIndex++;
			}
			fw.append("}\n");
			fw.append("\n");
		}
	}
	
	private void generateMain(int numberOfIndividuals, String dataset,int numberOfLines, int numberOfVars, int numberOfResults, int numberOfThreads)throws IOException {
		fw.append("int main(int argc, char** argv){\n");
                
		fw.append("\tslopes = (float *)malloc(sizeof(float) * " + numberOfIndividuals + ");\n");
                fw.append("\tintercepts = (float *)malloc(sizeof(float) * " + numberOfIndividuals + ");\n");

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
                    fw.append("\toutput << " + '"' + "slope " + (i + 1)+ ": " + '"' + " << slopes[" + i + "]" + "<< endl;\n");
                    fw.append("\toutput << " + '"' + "intercept " + (i + 1)+ ": " + '"' + " << intercepts[" + i + "]" + "<< endl;\n");
		}

		fw.append("\n");

		fw.append("\tofstream outFile;\n");
		fw.append("\tstring name=" + '"' + '"' + ";\n");
		fw.append("\tstringstream ssname;\n");
		fw.append("\tssname << " + '"' + "tempFiles/SRScaledModelsCuda" + '"' + "<<" + '"' + ".txt" + '"' + " ;\n");
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
			String command =  binName ;
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
		} catch (Throwable e) {
			System.out.println("Error running cpp binary");
		}
	}

	public ArrayList<ArrayList<Float>> readResults() throws FileNotFoundException {
		String fileOutput = "tempFiles/SRScaledModelsCuda.txt";
		ArrayList<ArrayList<Float>> alSlopesInt = new ArrayList<ArrayList<Float>>();
		Scanner sc = new Scanner(new FileReader(fileOutput));
		while (sc.hasNextLine()) {
                    ArrayList<Float> alPair = new ArrayList<Float>();
                    // READ SLOPE
                    String lineAux = sc.nextLine();
                    String[] lineSplit = lineAux.split(" ");
                    String slopeSAux = lineSplit[2];
                    if (slopeSAux.equals("nan") || slopeSAux.equals("-nan") || slopeSAux.equals("inf") || slopeSAux.equals("-inf")){
                        alPair.add(Float.MIN_VALUE);
                    }else{
                        float slopeFAux = Float.parseFloat(slopeSAux);
                        alPair.add(slopeFAux);
                    }
                    // READ INTERCEPT
                    lineAux = sc.nextLine();
                    lineSplit = lineAux.split(" ");
                    String interSAux = lineSplit[2];
                    if(interSAux.equals("nan") || interSAux.equals("-nan") || interSAux.equals("inf") || interSAux.equals("-inf") ){
                        alPair.add(Float.MIN_VALUE);
                    }else{
                        float interFAux = Float.parseFloat(interSAux);
                        alPair.add(interFAux);  
                    }   
                    alSlopesInt.add(alPair);
		}
		return alSlopesInt;
	}

}