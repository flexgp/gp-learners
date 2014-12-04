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

/**
 * Just in time CUDA code generation to evaluate symbolic regression models
 * @author Ignacio Arnaldo
 */
public class GenerateSRCuda{
    
    Expression[] expressions;
    FileWriter fw;
    String filename;
    
    public GenerateSRCuda(String aFileName) throws IOException{
        expressions = null;
        filename = aFileName;
    } 

    public void setExpressions(Expression[] anExpressionArray){
        expressions = anExpressionArray;
    }


    public void generateHeaders(int numberOfIndividuals) throws IOException{
        //fw.append("#include " + '"' + "cutil_inline.h" + '"' + "\n";
        fw.append("#include <cmath>\n");
	fw.append("#include <fstream>\n");
	fw.append("#include <sstream>\n");
	fw.append("#include <cstdlib>\n");
	fw.append("#include <iostream>\n");
	fw.append("#include <sys/types.h>\n");
	fw.append("#include <sys/ipc.h>\n");
	fw.append("#include <sys/shm.h>\n");
	fw.append("#include <unistd.h>\n");
	fw.append("#include <float.h>\n");
	fw.append("using namespace std;\n");
	fw.append("\n");
	fw.append("const int BLOCK_SIZE=512;\n");
	fw.append("\n");
	fw.append("// declaration, forward \n");
	fw.append("void runTest(int numIndi); \n");
	fw.append("void readDataSet(string,float*, int, int, int); \n");
	fw.append("\n");
        
    }
    
 
    private void generateMain(int numberOfIndividuals,int numberOfLines,int numberOfVars,int numberOfResults,int maxIndividualSize,String filePathIndividuals, boolean COERCE_INT,int p) throws IOException{
        
        fw.append("int main(int argc, char** argv){\n");
        fw.append("\tint numIndi = atoi(argv[1]);\n");
	fw.append("\trunTest(numIndi);\n");
        fw.append("}\n");        
        fw.append("\n");
        
	fw.append("void readIndividuals(string filePath,int* individuals, int maxSize){\n");
	fw.append("\tifstream file(filePath.c_str());\n");
	fw.append("\tstring line = " + '"' + '"' +";\n");
	fw.append("\tint pointIndex = 0;\n");
	fw.append("\twhile (getline(file, line)){\n");
	fw.append("\t\tstringstream strstr(line.c_str());\n");
	fw.append("\t\tstring word = " + '"' + '"' + ";\n");
	fw.append("\t\tfor(int j=0; j<maxSize;j++){\n");
	fw.append("\t\t\tgetline(strstr,word, ' ');\n");
	fw.append("\t\t\tint iAux = atoi(word.c_str());\n");
	fw.append("\t\t\tindividuals[pointIndex*maxSize+j] = iAux;\n");
	fw.append("\t\t}\n");
	fw.append("\t\tpointIndex++;\n");
	fw.append("\t}\n");
	fw.append("}\n");
	fw.append("\n");
        
        fw.append("void runTest(int aNumIndividuals){\n");
	fw.append("\tint numberOfPoints = " + numberOfLines + ";\n");
        fw.append("\tint numberOfVars = " + numberOfVars + ";\n");
        fw.append("\tint numberOfResults = " +numberOfResults + ";\n");
        fw.append("\tint maxIndividualSize = " + maxIndividualSize + ";\n");
        //fw.append("\tint numberOfIndividuals = " + numberOfIndividuals + ";\n");
        fw.append("\tint numberOfIndividuals = aNumIndividuals;\n");
        fw.append("\n");
	fw.append("\tbool COERCE_INT = " + COERCE_INT + ";\n");
	fw.append("\tint p = " + p + ";\n");
        fw.append("\n");
        
        fw.append("\tint shmid_semaphore, shmid_dataset, shmid_minTarget, shmid_maxTarget;\n");
	fw.append("\tkey_t key_semaphore, key_dataset, key_minTarget, key_maxTarget;\n");
	fw.append("\tfloat* sm_dataset;\n");
	fw.append("\tint* sm_semaphore;\n");
	fw.append("\tfloat* sm_minTarget;\n");
	fw.append("\tfloat* sm_maxTarget;\n");
	fw.append("\n");
        
        fw.append("\tunsigned int mem_size_semaphore = sizeof(int);\n");
	fw.append("\tunsigned int size_dataset = numberOfPoints * (numberOfVars + numberOfResults);\n");
	fw.append("\tunsigned int mem_size_dataset = sizeof(float) * size_dataset;\n");
	fw.append("\tunsigned int mem_size_minTarget = sizeof(float);\n");
	fw.append("\tunsigned int mem_size_maxTarget = sizeof(float);\n");
	fw.append("\n");
        
        fw.append("\tunsigned int size_individuals = numberOfIndividuals * maxIndividualSize;\n");
	fw.append("\tunsigned int mem_size_individuals = sizeof(int) * size_individuals;\n");
	fw.append("\tint* h_individuals = (int*) malloc(mem_size_individuals);\n");
	fw.append("\n");
	fw.append("\tunsigned int size_fitness = numberOfPoints;\n");
	fw.append("\tunsigned int mem_size_fitness = sizeof(float) * size_fitness;\n");
	fw.append("\tfloat* h_fitness = (float*) malloc(mem_size_fitness);\n");
	fw.append("\n");
	fw.append("\tunsigned int size_expression = maxIndividualSize;\n");
	fw.append("\tunsigned int mem_size_expression = sizeof(int) * size_expression;\n");
	fw.append("\tint* h_expression = (int*) malloc(mem_size_expression);\n");
	fw.append("\n");
        
        fw.append("\tconst size_t block_size_red = 512;\n");
	fw.append("\tconst size_t num_blocks_red = (numberOfPoints/block_size_red) + ((numberOfPoints%block_size_red) ? 1 : 0);\n");
	fw.append("\tunsigned int mem_size_inter = sizeof(float) * (num_blocks_red);\n");
	fw.append("\tfloat* h_inter = (float*) malloc(mem_size_fitness);\n");
	fw.append("\n");
        
        fw.append("\tstring filePathIndividuals = " + '"' + filePathIndividuals + '"' + ";\n");
	fw.append("\treadIndividuals(filePathIndividuals,h_individuals,maxIndividualSize);\n");
        fw.append("\n");   

	fw.append("\tkey_semaphore = 1;\n");
	fw.append("\tkey_dataset = 2;\n");
	fw.append("\tkey_minTarget = 3;\n");
	fw.append("\tkey_maxTarget = 4;\n");
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
	fw.append("\tfloat minTarget = *sm_minTarget;\n");
	fw.append("\tfloat maxTarget = *sm_maxTarget;\n");
	fw.append("\tfloat minPhenotype = 0;\n");
	fw.append("\tfloat maxPhenotype = 0;\n");
	fw.append("\n");
        
        fw.append("\tfloat* d_dataset;\n");
        fw.append("\tcudaMalloc((void**) &d_dataset, mem_size_dataset);\n");
        fw.append("\n");
        
        fw.append("\tfloat* d_fitness;\n");
        fw.append("\tcudaMalloc((void**) &d_fitness, mem_size_fitness);\n");
        fw.append("\n");

        fw.append("\tint* d_expression;\n");
	fw.append("\tcudaMalloc((void**) &d_expression, mem_size_expression);\n");
        fw.append("\n");
        
        fw.append("\tfloat* d_inter;\n");
	fw.append("\tcudaMalloc((void**)&d_inter, mem_size_inter);\n");
        fw.append("\n");
        
        fw.append("\tcudaMemcpy(d_dataset, sm_dataset, mem_size_dataset,cudaMemcpyHostToDevice);\n");
        fw.append("\n");

        fw.append("\tdim3 threads(BLOCK_SIZE);\n");
        fw.append("\tint gridx;\n");
        fw.append("\tif((numberOfPoints % threads.x) == 0){\n");
        fw.append("\t\tgridx = numberOfPoints / threads.x;\n");
        fw.append("\t}else{\n");
        fw.append("\t\tgridx = (numberOfPoints / threads.x) + 1;\n");
        fw.append("\t}\n");
        fw.append("\tdim3 grid(gridx);\n");
        fw.append("\n");
        
        fw.append("\tdim3 threadsRed(block_size_red);\n");
	fw.append("\tdim3 gridRed(num_blocks_red);\n");
    
        fw.append("\tstringstream output;\n");
        fw.append("\tofstream outFile;\n");
        fw.append("\tstring name=" + '"' + '"' + ";\n");
        fw.append("\tstringstream ssname;\n");
        fw.append("\tssname << " + '"' + "tempFiles/resultsCUDAInterpret" + '"' + "<<" + '"' + ".txt" + '"' + " ;\n");
        fw.append("\tname = ssname.str();\n");
        fw.append("\toutFile.open(name.c_str());\n");
        fw.append("\n");        
        
        fw.append("\tfor(int indexIndi=0; indexIndi<numberOfIndividuals;indexIndi++){\n");
	fw.append("\t\tint indexStartMemIndi = indexIndi * maxIndividualSize;\n");
	fw.append("\t\tfor(int i=0;i<maxIndividualSize;i++){\n");
	fw.append("\t\t\th_expression[i] = h_individuals[indexStartMemIndi + i];\n");
	fw.append("\t\t}\n");
        
        fw.append("\t\tcudaMemcpy(d_expression, h_expression, mem_size_expression,cudaMemcpyHostToDevice);\t\n");
	fw.append("\t\tcomputeRPN<<< grid, threads >>>(d_dataset, d_fitness, numberOfVars, numberOfResults,d_expression,numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\n");
        
	fw.append("\t\t//getMaximumPhenotype\n");
	fw.append("\t\tmaxPhenotype=0;\n");
	fw.append("\t\treduce_step_1d_max<<<gridRed,threadsRed>>>(d_fitness, d_inter, numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
	fw.append("\t\tmaxPhenotype = h_inter[0];\n");
	fw.append("\t\tfor(int i=1;i<num_blocks_red;i++){\n");
	fw.append("\t\t\tif(h_inter[i]>maxPhenotype) maxPhenotype = h_inter[i];\n");
	fw.append("\t\t}\n");

	fw.append("\n");
	fw.append("\t\t//getMinimumPhenotype\n");
	fw.append("\t\tminPhenotype=0;\n");
	fw.append("\t\treduce_step_1d_min<<<gridRed,threadsRed>>>(d_fitness, d_inter, numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
	fw.append("\t\tminPhenotype = h_inter[0];\n");
	fw.append("\t\tfor(int i=1;i<num_blocks_red;i++){\n");
	fw.append("\t\t\tif(h_inter[i]<minPhenotype) minPhenotype = h_inter[i];\n");
	fw.append("\t\t}\n");
	fw.append("\n");
        
       	fw.append("\t\t// scale according to the min and max of the predictions\n");
	fw.append("\t\tscaleValues<<< grid, threads >>>(d_fitness,minPhenotype,maxPhenotype,numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\n");
	fw.append("\t\tif(COERCE_INT){\n");
	fw.append("\t\t\tunscaleValues<<< grid, threads >>>(d_fitness,minTarget,maxTarget,numberOfPoints);\n");
	fw.append("\t\t\tcudaThreadSynchronize();\n");
	fw.append("\t\t\tscaleValues<<< grid, threads >>>(d_fitness,minTarget,maxTarget,numberOfPoints);\n");
	fw.append("\t\t\tcudaThreadSynchronize();\n");
	fw.append("\t\t}\n");
	fw.append("\n");
	fw.append("\t\t// targets must have been scaled previously\n");
	fw.append("\t\tcomputeScaledErrors<<< grid, threads >>>(d_dataset, d_fitness, numberOfPoints, numberOfVars, numberOfResults);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\t\t\n");
	fw.append("\t\tfloat sum = 0;\n");
	fw.append("\t\tif(p==1){ // ARITHMETIC MEAN\n");
	fw.append("\t\t\treduce_step_1d_sum<<<gridRed,threadsRed>>>(d_fitness, d_inter, numberOfPoints);\t// sum += error;\n");
	fw.append("\t\t\tcudaThreadSynchronize();\n");
	fw.append("\t\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
	fw.append("\t\t\tfor(int i=0;i<num_blocks_red;i++){\n");
	fw.append("\t\t\t\tsum += h_inter[i];\n");
	fw.append("\t\t\t}\t\n");
	fw.append("\t\t}else if(p>1){ // POWER P MEAN\n");
	fw.append("\t\t\tcomputePowerErrors<<< grid, threads >>>(d_fitness,p,numberOfPoints);\n");
	fw.append("\t\t\treduce_step_1d_sum<<<gridRed,threadsRed>>>(d_fitness,d_inter, numberOfPoints); // sum += pow(error, p);\n");
	fw.append("\t\t\tcudaThreadSynchronize();\n");
	fw.append("\t\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\t\n");
	fw.append("\t\t\tfor(int i=0;i<num_blocks_red;i++){\n");
	fw.append("\t\t\t\tsum += h_inter[i];\n");
	fw.append("\t\t\t}\n");
	fw.append("\t\t}else{ // MAXIMUM\n");
	fw.append("\t\t\treduce_step_1d_max<<<gridRed,threadsRed>>>(d_fitness, d_inter,numberOfPoints);\t\t// if(error>sum) sum = error;\n");
	fw.append("\t\t\tcudaThreadSynchronize();\n");
	fw.append("\t\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
	fw.append("\t\t\tsum = h_inter[0];\n");
	fw.append("\t\t\tfor(int i=1;i<num_blocks_red;i++){\n");
	fw.append("\t\t\t\tif(h_inter[i]>sum) sum = h_inter[i];\n");
	fw.append("\t\t\t}\n");
	fw.append("\t\t}\n");
	fw.append("\t\tint n = numberOfPoints;\n");
	fw.append("\t\tfloat mean = 0;\n");
	fw.append("\t\tif(p==1){// ARITHMETIC MEAN\n");
	fw.append("\t\t\tmean =  sum / (float) n;\n");
	fw.append("\t\t}else if(p>1){ // POWER P MEAN\n");
	fw.append("\t\t\tmean = pow(sum / (float) n, 1 / (float) p);\n");
	fw.append("\t\t}else{ // MAXIMUM\n");
	fw.append("\t\t\tmean = sum;\n");
	fw.append("\t\t}\n");
	fw.append("\t\tfloat fitness = meanToFitness(mean);\n");
	fw.append("\t\toutput << " + '"' + "FitnessIndividual " + '"' +" << (indexIndi+1) <<" + '"' + ": " + '"' + " << fitness << endl;\n");
        fw.append("\t}\n");
	fw.append("\n"); 
        
        fw.append("\toutFile << output.str();\n");
        fw.append("\toutFile.close();\n");        
        fw.append("\n");
    
        fw.append("\tfree(h_fitness);\n");
        fw.append("\tfree(h_individuals);\n");
        fw.append("\tfree(h_expression);\n");
        fw.append("\tfree(h_inter);\n");	

        fw.append("\n");
	fw.append("\tcudaFree(d_inter);\n");
        fw.append("\tcudaFree(d_dataset);\n");
        fw.append("\tcudaFree(d_fitness);\n");
        fw.append("\tcudaFree(d_expression);\n");
        fw.append("\t\n");
        fw.append("\tcudaThreadExit();\n");
        fw.append("}\n");
        fw.append("\n");

    }
    
    public void generateHeadersKernels(int numberOfIndividuals) throws IOException{
        
	fw.append("__device__ inline float mydivideK(float a, float b){\n");
	fw.append("\tfloat result;\n");
	fw.append("\tif(fabs(b) < 0.000001){\n");
	fw.append("\t\tresult = 1;\n");
	fw.append("\t}else {\n");
	fw.append("\t\tresult = (a/b);\n");
	fw.append("\t}\n");
	fw.append("\treturn result;\n");
	fw.append("}\n");
	fw.append("\n");
        
	fw.append("__device__ inline float mylogK(float a){\n");
	fw.append("\tfloat result;\n");
	fw.append("\tif(fabs(a) < 0.000001){\n");
	fw.append("\t\tresult = 0.0;\n");
	fw.append("\t}else{\n");
	fw.append("\t\tresult = log(fabs(a));\n");
	fw.append("\t}\n");
	fw.append("\treturn result;\n");
	fw.append("}\n");
	fw.append("\n");
        
	fw.append("__device__ inline float mysqrtK(float a){\n");
	fw.append("\tfloat result = sqrt(fabs(a));\n");
	fw.append("\treturn result;\n");
	fw.append("}\n");
	fw.append("\n");
        
        
        fw.append("__global__ void reduce_step_1d_sum(float *input, float *inter,int numberOfPoints){\n");
	fw.append("\t__shared__ float as[512];\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint ind = bx * blockDim.x + tx;\n");
	fw.append("\tint ind_bl = bx;\n");
	fw.append("\tif(ind<numberOfPoints){\n");
	fw.append("\t\tas[tx]=input[ind];\n");
	fw.append("\t\t__syncthreads();\n");
	fw.append("\t}else{\n");
	fw.append("\t\tas[tx]=0;\n");
	fw.append("\t\t__syncthreads();\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<256){\n");
	fw.append("\t    as[tx] += as[511-tx];\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<128){\n");
	fw.append("\t    as[tx] += as[255-tx];\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<64){\n");
	fw.append("\t    as[tx] += as[127-tx];\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<32){\n");
	fw.append("\t    as[tx] += as[63-tx];\n");
	fw.append("\t}\t\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<16){\n");
	fw.append("\t    as[tx] += as[31-tx];\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<8){\n");
	fw.append("\t    as[tx] += as[15-tx];\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<4){\n");
	fw.append("\t    as[tx] += as[7-tx];\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<2){\n");
	fw.append("\t    as[tx] += as[3-tx];\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<1){\n");
	fw.append("\t    as[tx] += as[1-tx];\n");
	fw.append("\t    __syncthreads();\n");
	fw.append("\t    inter[ind_bl]=as[tx];\n");
	fw.append("\t    __syncthreads();\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("}\n");
	fw.append("\n");
        
        
	fw.append("__global__ void reduce_step_1d_max(float *input, float *inter,int numberOfPoints){\n");
	fw.append("\t__shared__ float as[512];\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint ind = bx * blockDim.x + tx;\n");
	fw.append("\tint ind_bl = bx;\n");
	fw.append("\tif(ind<numberOfPoints){\n");
	fw.append("\t\tas[tx]=input[ind];\n");
	fw.append("\t\t__syncthreads();\n");
	fw.append("\t}else{\n");
	fw.append("\t\tas[tx]=-FLT_MAX;\n");
	fw.append("\t\t__syncthreads();\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<256){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[511-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<128){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[255-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<64){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[127-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<32){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[63-tx]);\n");
	fw.append("\t}\t\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<16){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[31-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<8){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[15-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<4){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[7-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<2){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[3-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<1){\n");
	fw.append("\t    as[tx] = fmaxf(as[tx],as[1-tx]);\n");
	fw.append("\t    __syncthreads();\n");
	fw.append("\t    inter[ind_bl]=as[tx];\n");
	fw.append("\t    __syncthreads();\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("}\n");
	fw.append("\n");
	fw.append("\n");
        
	fw.append("__global__ void reduce_step_1d_min(float *input, float *inter,int numberOfPoints){\n");
	fw.append("\t__shared__ float as[512];\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint ind = bx * blockDim.x + tx;\n");
	fw.append("\tint ind_bl = bx;\n");
	fw.append("\tif(ind<numberOfPoints){\n");
	fw.append("\t\tas[tx]=input[ind];\n");
	fw.append("\t\t__syncthreads();\n");
	fw.append("\t}else{\n");
	fw.append("\t\tas[tx]=FLT_MAX;\n");
	fw.append("\t\t__syncthreads();\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<256){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[511-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<128){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[255-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<64){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[127-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<32){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[63-tx]);\n");
	fw.append("\t}\t\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<16){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[31-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<8){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[15-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<4){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[7-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<2){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[3-tx]);\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("\tif(tx<1){\n");
	fw.append("\t    as[tx] = fminf(as[tx],as[1-tx]);\n");
	fw.append("\t    __syncthreads();\n");
	fw.append("\t    inter[ind_bl]=as[tx];\n");
	fw.append("\t    __syncthreads();\n");
	fw.append("\t}\n");
	fw.append("\t__syncthreads();\n");
	fw.append("}\n");
	fw.append("\n");
        
        fw.append("__global__ void scaleValues(float* fitness, float minValue, float maxValue, int numberOfPoints){\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint threadId = bx * blockDim.x + tx;\n");
	fw.append("\tif(threadId<numberOfPoints){\n");
	fw.append("\t\tfloat val = fitness[threadId];\n");
	fw.append("\t\tfloat range = maxValue - minValue;\t\n");
	fw.append("\t\tfloat scaledValue = (val - minValue) / range;\n");
	fw.append("\t\tfitness[threadId] = scaledValue;\t\t\n");
	fw.append("\t}\t\n");
	fw.append("}\n");
	fw.append("\n");
        
	fw.append("__global__ void unscaleValues(float* fitness, float minValue, float maxValue, int numberOfPoints){\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint threadId = bx * blockDim.x + tx;\n");
	fw.append("\tif(threadId<numberOfPoints){\n");
	fw.append("\t\tfloat val = fitness[threadId];\n");
	fw.append("\t\tfloat range = maxValue - minValue;\t\n");
	fw.append("\t\tfloat unscaledValue = (val * range) + minValue;\n");
	fw.append("\t\tfitness[threadId] = unscaledValue;\n");
	fw.append("\t}\t\n");
	fw.append("}\n");
	fw.append("\n");
	
        fw.append("__global__ void computeScaledErrors(float* dataset, float* fitness, int numberOfPoints, int numberOfVariables, int numberOfResults){\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint threadId = bx * blockDim.x + tx;\n");
	fw.append("\tif(threadId<numberOfPoints){\n");
	fw.append("\t\tint indexMemoryOffset = threadId;\n");
	fw.append("\t\tfloat scaledTarget = dataset[numberOfPoints*numberOfVariables + indexMemoryOffset];\n");
	fw.append("\t\tfloat scaledError = fabs(scaledTarget - fitness[threadId]);\n");
	fw.append("\t\tfitness[threadId] = scaledError;\n");
	fw.append("\t}\n");
	fw.append("}\n");
	fw.append("\n");
        
	fw.append("__global__ void computePowerErrors(float* fitness, int p, int numberOfPoints){\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint threadId = bx * blockDim.x + tx;\n");
	fw.append("\tif(threadId<numberOfPoints){\n");
	fw.append("\t\tfloat scaledError = fitness[threadId];\n");
	fw.append("\t\tfloat powScaledError = powf(scaledError,p);// pow(error, p);\n");
	fw.append("\t\tfitness[threadId] = powScaledError;\n");
	fw.append("\t}\n");
	fw.append("}\n");
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
    }
        
    private void generateKernelInterpreter(int maxIndividualSize) throws IOException{
	fw.append("__global__ void computeRPN(float* dataset, float* fitness, int numberOfVariables,int numberOfResults,int* expression,int numberOfPoints){\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint threadId = bx * blockDim.x + tx;\n");
	fw.append("\tif(threadId<numberOfPoints){\n");
	fw.append("\t\tint indexMemoryOffset = threadId;\n");
	fw.append("\tconst int maxExpressionSize = " + maxIndividualSize + ";\n");
	fw.append("\tconst int maxStackSize = " + maxIndividualSize + ";\n");
        fw.append("\n");
	fw.append("\n");
	fw.append("\t\tfloat stack[maxStackSize];\n");
	fw.append("\t\tint indexRead = 0;\n");
	fw.append("\t\tint maxRead = maxExpressionSize;\n");
	fw.append("\t\tint numElemsStack = 0;\n");
	fw.append("\n");
	fw.append("\t\twhile( (indexRead<maxRead) && (expression[indexRead] != -999999999) ) {\n");
	fw.append("\t\t\tint token = expression[indexRead];\n");
	fw.append("\t\t\t\tindexRead++;\n");
	fw.append("\t\t\tif(! (token<-111111000) ){\n");
	fw.append("\t\t\t\tint indexVariable = token - 111111000;\n");
	fw.append("\t\t\t\tfloat fAux = dataset[(indexVariable-1)*numberOfPoints + indexMemoryOffset];\n");
	fw.append("\t\t\t\tstack[numElemsStack] = fAux;\n");
	fw.append("\t\t\t\tnumElemsStack++;\n");
	fw.append("\t\t\t}else if ((token<-111111000)){\n");
	fw.append("\t\t\t\tint arity = 2;\n");
	fw.append("\t\t\t\tif(token<-111111010){\n");
	fw.append("\t\t\t\t\tarity = 1;\n");
	fw.append("\t\t\t\t}\n");
	fw.append("\t\t\t\tfloat result = 0;\n");
	fw.append("\t\t\t\tfloat operand2 = stack[numElemsStack-1];\n");
	fw.append("\t\t\t\tnumElemsStack--;\n");
	fw.append("\t\t\t\tif(arity == 2){\n");
	fw.append("\t\t\t\t\tfloat operand1 = stack[numElemsStack-1];\n");
	fw.append("\t\t\t\t\tnumElemsStack--;\n");
	fw.append("\t\t\t\t\tif(token == -111111001){\n");
	fw.append("\t\t\t\t\t\tresult = operand1 + operand2;\n");
	fw.append("\t\t\t\t\t}else if(token == -111111002){\n");
	fw.append("\t\t\t\t\t\tresult = operand1 * operand2;\n");
	fw.append("\t\t\t\t\t}else if (token == -111111003){\n");
	fw.append("\t\t\t\t\t\tresult = operand1 - operand2;\n");
	fw.append("\t\t\t\t\t}else if (token == -111111004){\n");
	fw.append("\t\t\t\t\t\tresult = mydivideK(operand1,operand2);\n");
	fw.append("\t\t\t\t\t}\n");
	fw.append("\t\t\t\t}else if(arity==1){\n");
	fw.append("\t\t\t\t\tif(token == -111111011){\n");
	fw.append("\t\t\t\t\t\tresult = sin(operand2);\n");
	fw.append("\t\t\t\t\t}else if(token == -111111012){\n");
	fw.append("\t\t\t\t\t\tresult = cos(operand2);\n");
	fw.append("\t\t\t\t\t}else if(token == -111111013){\n");
	fw.append("\t\t\t\t\t\tresult = mysqrtK(operand2);\n");
	fw.append("\t\t\t\t\t}else if(token == -111111014){\n");
	fw.append("\t\t\t\t\t\tresult = mylogK(operand2);\n");
	fw.append("\t\t\t\t\t}else if(token == -111111015){\n");
	fw.append("\t\t\t\t\t\tresult = exp(operand2);\n");
	fw.append("\t\t\t\t\t}else if(token == -111111016){\n");
	fw.append("\t\t\t\t\t\tresult = pow(operand2,2);\n");
	fw.append("\t\t\t\t\t}else if(token == -111111017){\n");
	fw.append("\t\t\t\t\t\tresult = pow(operand2,3);\n");
	fw.append("\t\t\t\t\t}else if(token == -111111018){\n");
	fw.append("\t\t\t\t\t\tresult = pow(operand2,4);\n");
	fw.append("\t\t\t\t\t}\n");
	fw.append("\t\t\t\t}\n");
	fw.append("\t\t\t\tstack[numElemsStack] = result;\n");
	fw.append("\t\t\t\tnumElemsStack++;\n");
	fw.append("\t\t\t}\n");
	fw.append("\t\t}\n");
	fw.append("\t\tif(numElemsStack!=1){\n");
	fw.append("\t\t\tfitness[threadId] = -1000000;\n");
	fw.append("\t\t\t__syncthreads();\n");
	fw.append("\t\t}else{\n");
	fw.append("\t\t\tfitness[threadId] = stack[0];\n");
	fw.append("\t\t\t__syncthreads();\n");
	fw.append("\t\t}\n");
	fw.append("\t}\n");
	fw.append("}\n");
	fw.append("\n");
    }
        
    public void generateCode(int numberOfIndividuals,int numberOfLines,int numberOfVars,int numberOfResults,int maxIndividualSize,String filePathIndividuals,boolean COERCE_INT,int p) throws IOException{
        fw = new FileWriter(filename);
        
        // CREATE THE CPU FILE THAT USES THE CUDA API
        generateHeaders(numberOfIndividuals);
        
        // CREATE THE KERNELS
        generateHeadersKernels(numberOfIndividuals);
        generateKernelInterpreter(maxIndividualSize);
        
        // FINISH THE GENERATION OF THE CPU FILE THAT USES THE CUDA API
        
        generateMain(numberOfIndividuals,numberOfLines,numberOfVars,numberOfResults,maxIndividualSize,filePathIndividuals,COERCE_INT,p);
        
    }
    
    public int printCodedExpressionsToFile(String fileName){
        int maxSize = 0;
        try{
            FileWriter fwIndividuals;
            fwIndividuals = new FileWriter(fileName);
            int sizeAux;
            for(int i=0;i<expressions.length;i++){
                sizeAux=0;
                for(int j=0;j<expressions[i].getAlCodedRpn().size();j++){
                    fwIndividuals.append(expressions[i].getAlCodedRpn().get(j) + " ");
                    if(expressions[i].getAlCodedRpn().get(j) != -999999999 ){
                        sizeAux++;
                    }
                }
                fwIndividuals.append("\n");
                if(sizeAux > maxSize) maxSize = sizeAux;
            }
            fwIndividuals.flush();
            fwIndividuals.close();
        }catch(Throwable e){
            System.out.println("Error writing interpret individuals file");
        }
        return maxSize;
    }
        
    public void printCodeToFile(String fileName){
        try{
            fw.flush();
            fw.close();
        }catch(Throwable e){
            System.out.println("Error writing cu interpret file");
        }
    }

    public void compileFile(String fileName,String binName){
        try{
            // usr/local/cuda/bin/nvcc --ptx -use_fast_math -I /home/ambu/NVIDIA_GPU_Computing_SDK/C/common/inc evalCUDA.cu -o evalCUDA
            //String command = "/usr/local/cuda/bin/nvcc --ptxas-options= -O2 -arch=sm_20 -use_fast_math -I /home/ambu/NVIDIA_GPU_Computing_SDK/C/common/inc " + fileName + " -o " + binName;
            String command = "/usr/local/cuda/bin/nvcc --ptxas-options= -O2 -arch=sm_30 -use_fast_math -I/usr/local/cuda-5.0/samples/common/inc/ " + fileName + " -o " + binName;
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        }catch(Throwable e){
            System.out.println("Error compiling cuda file");
        }
    }
    
    public void runCode(String binName,int numberOfIndividuals){
        try{
            String command = binName + " " + numberOfIndividuals;
            String[] env = new String[1];
            env[0] = "LD_LIBRARY_PATH=/usr/local/cuda/lib64:/lib";
            Process p = Runtime.getRuntime().exec(command, env);
            p.waitFor();
        }catch(Throwable e){
            System.out.println("Error running cuda binary");
        }
    }
    
    public ArrayList<Float> readResults() throws FileNotFoundException{
        String fileOutput = "tempFiles/resultsCUDAInterpret.txt";
        ArrayList<Float> alFitness = new ArrayList<Float>();
        Scanner sc = new Scanner(new FileReader(fileOutput));
        while(sc.hasNextLine()){
            String lineAux = sc.nextLine();
            String[] lineSplit = lineAux.split(" ");
            String fitnessSAux = lineSplit[2];
            if(fitnessSAux.equals("nan")|| fitnessSAux.equals("inf")){
                alFitness.add(Float.MAX_VALUE);
            }else{
                float fitnessFAux = Float.parseFloat(fitnessSAux);
                alFitness.add(fitnessFAux);
            }
        }
        return alFitness;
    }
    
}
