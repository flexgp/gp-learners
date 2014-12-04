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
 * Just in time CUDA code generation evaluate GPFunction models
 * @author Ignacio Arnaldo
 */
public class GenerateGPFunctionCuda{
    
    Expression[] expressions;
    FileWriter fw;
    String filename;
    
    public GenerateGPFunctionCuda(String aFileName) throws IOException{
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
        
        fw.append("__global__ void reduce_step_1d_totalPositives(float *dataset, float *inter,int numberOfPoints,int numberOfVariables){\n");
	fw.append("\t__shared__ float as[512];\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint ind = bx * blockDim.x + tx;\n");
	fw.append("\tint ind_bl = bx;\n");
	fw.append("\tif(ind<numberOfPoints){\n");
	fw.append("\t\tint indexMemoryOffset = ind;\n");
	fw.append("\t\tfloat target = dataset[numberOfPoints*numberOfVariables + indexMemoryOffset];\n");
	fw.append("\t\t//as[tx] = target;\n");
	fw.append("\t\tif(target==1){\n");
	fw.append("\t\t\tas[tx] = 1;\n");
	fw.append("\t\t} else{\n");
	fw.append("\t\t\tas[tx] = 0;\n");
	fw.append("\t\t}\n");
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
	fw.append("\t}	\n");
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
        
	fw.append("__global__ void reduce_step_1d_numFalsePositives(float *dataset,float *fitness,float *inter,int numberOfPoints,int numberOfVariables){\n");
	fw.append("\t__shared__ float as[512];\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint ind = bx * blockDim.x + tx;\n");
	fw.append("\tint ind_bl = bx;\n");
	fw.append("\tif(ind<numberOfPoints){\n");
	fw.append("\t\tint indexMemoryOffset = ind;\n");
	fw.append("\t\tfloat target = dataset[numberOfPoints*numberOfVariables + indexMemoryOffset];\n");
	fw.append("\t\tfloat prediction = fitness[ind];\n");
	fw.append("\t\tif((prediction==1) && (target==0)){\n");
	fw.append("\t\t\tas[tx] = 1;\n");
	fw.append("\t\t} else{\n");
	fw.append("\t\t\tas[tx] = 0;\n");
	fw.append("\t\t}\n");
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
	fw.append("\t}	\n");
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
	fw.append("}\t\n");
	fw.append("\n");
        
	fw.append("__global__ void reduce_step_1d_numTruePositives(float *dataset,float *fitness,float *inter,int numberOfPoints,int numberOfVariables){\n");
	fw.append("\t__shared__ float as[512];\n");
	fw.append("\tint bx = blockIdx.x;\n");
	fw.append("\tint tx = threadIdx.x;\n");
	fw.append("\tint ind = bx * blockDim.x + tx;\n");
	fw.append("\tint ind_bl = bx;\n");
	fw.append("\tif(ind<numberOfPoints){\n");
	fw.append("\t\tint indexMemoryOffset = ind;\n");
	fw.append("\t\tfloat target = dataset[numberOfPoints*numberOfVariables + indexMemoryOffset];\n");
	fw.append("\t\tfloat prediction = fitness[ind];\n");
	fw.append("\t\tif((prediction==1) && (target==1)){\n");
	fw.append("\t\t\tas[tx] = 1;\n");
	fw.append("\t\t} else{\n");
	fw.append("\t\t\tas[tx] = 0;\n");
	fw.append("\t\t}\n");
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
	fw.append("\t}	\n");
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
        
        fw.append("__global__ void removeNaNs(float* fitness, int numberOfPoints){\n");
        fw.append("\tint bx = blockIdx.x;\n");
        fw.append("\tint tx = threadIdx.x;\n");
        fw.append("\tint threadId = bx * blockDim.x + tx;\n");
        fw.append("\tif(threadId<numberOfPoints){\n");
        fw.append("\t\tfloat val = fitness[threadId];\n");
        fw.append("\t\tif(isnan(val) != 0){\n");
        fw.append("\t\t\tfitness[threadId] = 0;\n");
        fw.append("\t\t}\n");
        fw.append("\t}\n");
        fw.append("}\n");
        fw.append("\n");
        
        fw.append("__global__ void computePredictions(float* outputs, float* predictions,int numberOfPoints, float threshold){\n");
        fw.append("\tint bx = blockIdx.x;\n");
        fw.append("\tint tx = threadIdx.x;\n");
        fw.append("\tint threadId = bx * blockDim.x + tx;\n");
        fw.append("\tif(threadId<numberOfPoints){\n");
        fw.append("\t\tfloat startInterval = 0;\n");
        fw.append("\t\tfloat endInterval = 1;\n");
        fw.append("\t\tfloat prediction = 0;\n");
        fw.append("\t\tif(threshold==0){\n");
        fw.append("\t\t\tprediction = endInterval;\n");
        fw.append("\t\t}else if(threshold==1){\n");
        fw.append("\t\t\tprediction = startInterval;\n");
        fw.append("\t\t}else if((threshold>0)&&(threshold<1)){\n");
        fw.append("\t\t\tif(outputs[threadId] >= threshold){\n");
        fw.append("\t\t\t\tprediction = endInterval;\n");
        fw.append("\t\t\t}else{\n");
        fw.append("\t\t\t\tprediction = startInterval;\n");
        fw.append("\t\t\t}\n");
        fw.append("\t\t}\n");
        fw.append("\t\tpredictions[threadId] = prediction;\n");
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
        fw.append("\t\t\tfloat Var_0 = stack[0];\n");
        fw.append("\t\t\tif((isinf(Var_0) != 0 ) && (Var_0 > 0)) {\n");
        fw.append("\t\t\t\tVar_0 = FLT_MAX - 1;\n");
        fw.append("\t\t\t}\n");
        fw.append("\t\t\tif((isinf(Var_0) != 0 ) && (Var_0 < 0)){\n");
        fw.append("\t\t\t\tVar_0 = - (FLT_MAX-1);\n");
        fw.append("\t\t\t}\n");
        fw.append("\t\t\tfitness[threadId] = Var_0;\n");
        fw.append("\t\t\t__syncthreads();\n");
	fw.append("\t\t}\n");
	fw.append("\t}\n");
	fw.append("}\n");
	fw.append("\n");
    }
    
 
    private void generateMain(int numberOfLines,int numberOfVars,int numberOfResults,int maxIndividualSize,String filePathIndividuals) throws IOException{
        
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
        fw.append("\tint numberOfIndividuals = aNumIndividuals;\n");
        fw.append("\n");
        
        fw.append("\tint shmid_semaphore, shmid_dataset;\n");
	fw.append("\tkey_t key_semaphore, key_dataset;\n");
	fw.append("\tfloat* sm_dataset;\n");
	fw.append("\tint* sm_semaphore;\n");
	fw.append("\n");
        
        fw.append("\tunsigned int mem_size_semaphore = sizeof(int);\n");
	fw.append("\tunsigned int size_dataset = numberOfPoints * (numberOfVars + numberOfResults);\n");
	fw.append("\tunsigned int mem_size_dataset = sizeof(float) * size_dataset;\n");
	fw.append("\n");
        
        fw.append("\tunsigned int size_individuals = numberOfIndividuals * maxIndividualSize;\n");
	fw.append("\tunsigned int mem_size_individuals = sizeof(int) * size_individuals;\n");
	fw.append("\tint* h_individuals = (int*) malloc(mem_size_individuals);\n");
	fw.append("\n");
        
	fw.append("\tunsigned int size_outputs = numberOfPoints;\n");
	fw.append("\tunsigned int mem_size_outputs = sizeof(float) * size_outputs;\n");
	fw.append("\n");
        
	fw.append("\tunsigned int size_expression = maxIndividualSize;\n");
	fw.append("\tunsigned int mem_size_expression = sizeof(int) * size_expression;\n");
	fw.append("\tint* h_expression = (int*) malloc(mem_size_expression);\n");
	fw.append("\n");
        
        fw.append("\tconst size_t block_size_red = 512;\n");
	fw.append("\tconst size_t num_blocks_red = (numberOfPoints/block_size_red) + ((numberOfPoints%block_size_red) ? 1 : 0);\n");
	fw.append("\tunsigned int mem_size_inter = sizeof(float) * (num_blocks_red);\n");
	fw.append("\tfloat* h_inter = (float*) malloc(mem_size_inter);\n");
	fw.append("\n");
        
        fw.append("\tstring filePathIndividuals = " + '"' + filePathIndividuals + '"' + ";\n");
	fw.append("\treadIndividuals(filePathIndividuals,h_individuals,maxIndividualSize);\n");
        fw.append("\n");   

	fw.append("\tkey_semaphore = 1;\n");
	fw.append("\tkey_dataset = 2;\n");
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
        
        fw.append("\tif ((sm_dataset = (float *)shmat(shmid_dataset, NULL, 0)) == (float *) -1) {\n");
        fw.append("\t\tperror(" + '"' + "shmat" + '"' + ");\n");
        fw.append("\t\texit(1);\n");
        fw.append("\t}\n");
        fw.append("\n");
        
	fw.append("\tfloat minPhenotype = 0;\n");
	fw.append("\tfloat maxPhenotype = 0;\n");
	fw.append("\n");
        
        fw.append("\tfloat* d_dataset;\n");
        fw.append("\tcudaMalloc((void**) &d_dataset, mem_size_dataset);\n");
        fw.append("\n");
        
        fw.append("\tfloat* d_outputs;\n");
        fw.append("\tcudaMalloc((void**) &d_outputs, mem_size_outputs);\n");
        fw.append("\n");

        fw.append("\tfloat* d_predictions;\n");
	fw.append("\tcudaMalloc((void**) &d_predictions, mem_size_outputs);\n");
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
        fw.append("\tssname << " + '"' + "tempFiles/resultsSRRocCuda" + '"' + "<<" + '"' + ".txt" + '"' + " ;\n");
        fw.append("\tname = ssname.str();\n");
        fw.append("\toutFile.open(name.c_str());\n");
        fw.append("\n");        
        
       	fw.append("\tfloat totalPositives = 0;\n");
	fw.append("\tfloat totalNegatives = 0;\n");
	fw.append("\treduce_step_1d_totalPositives<<<gridRed,threadsRed>>>(d_dataset,d_inter, numberOfPoints,numberOfVars);\n");
	fw.append("\tcudaThreadSynchronize();\n");
	fw.append("\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
	fw.append("\tfor(int i=0;i<num_blocks_red;i++){\n");
        fw.append("\t\ttotalPositives += h_inter[i];\n");
	fw.append("\t}\n");
	fw.append("\ttotalNegatives = numberOfPoints - totalPositives;\n");
        fw.append("\n"); 
        
        fw.append("\tfor(int indexIndi=0; indexIndi<numberOfIndividuals;indexIndi++){\n");
	fw.append("\t\tint indexStartMemIndi = indexIndi * maxIndividualSize;\n");
	fw.append("\t\tfor(int i=0;i<maxIndividualSize;i++){\n");
	fw.append("\t\t\th_expression[i] = h_individuals[indexStartMemIndi + i];\n");
	fw.append("\t\t}\n");
        fw.append("\n");
        
        fw.append("\t\tcudaMemcpy(d_expression, h_expression, mem_size_expression,cudaMemcpyHostToDevice);\t\n");
	fw.append("\t\tcomputeRPN<<< grid, threads >>>(d_dataset, d_outputs, numberOfVars, numberOfResults,d_expression,numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\n");
        
	fw.append("\t\t//getMaximumPhenotype\n");
	fw.append("\t\tmaxPhenotype=0;\n");
	fw.append("\t\treduce_step_1d_max<<<gridRed,threadsRed>>>(d_outputs, d_inter, numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
	fw.append("\t\tmaxPhenotype = h_inter[0];\n");
	fw.append("\t\tfor(int i=1;i<num_blocks_red;i++){\n");
	fw.append("\t\t\tif(h_inter[i]>maxPhenotype) maxPhenotype = h_inter[i];\n");
	fw.append("\t\t}\n");
        fw.append("\n");
        
	fw.append("\t\t//getMinimumPhenotype\n");
	fw.append("\t\tminPhenotype=0;\n");
	fw.append("\t\treduce_step_1d_min<<<gridRed,threadsRed>>>(d_outputs, d_inter, numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
	fw.append("\t\tminPhenotype = h_inter[0];\n");
	fw.append("\t\tfor(int i=1;i<num_blocks_red;i++){\n");
	fw.append("\t\t\tif(h_inter[i]<minPhenotype) minPhenotype = h_inter[i];\n");
	fw.append("\t\t}\n");
	fw.append("\n");
        
       	fw.append("\t\t// scale according to the min and max of the predictions\n");
	fw.append("\t\tscaleValues<<< grid, threads >>>(d_outputs,minPhenotype,maxPhenotype,numberOfPoints);\n");
	fw.append("\t\tcudaThreadSynchronize();\n");
	fw.append("\n");

        fw.append("\t\t// add kernel, if (pred==nan) then pred:=0\n");
        fw.append("\t\tremoveNaNs<<< grid, threads >>>(d_outputs,numberOfPoints);\n");
        fw.append("\t\tcudaThreadSynchronize();\n");
        fw.append("\n");

        fw.append("\t\tint numberOfLambdas = 10;\n");
        fw.append("\t\tfloat startInterval = 0;\n");
        fw.append("\t\tfloat endInterval = 1;\n");
        fw.append("\t\tfloat interval = (endInterval - startInterval) / (float) numberOfLambdas;\n");
        fw.append("\t\tfloat falsePositives[numberOfLambdas+1];\n");
        fw.append("\t\tfloat truePositives[numberOfLambdas+1];\n");
        fw.append("\t\tfor(int l=0;l<=numberOfLambdas;l++){\n");
        fw.append("\t\t\tfloat threshold = endInterval - l*interval;\n");

        fw.append("\t\t\t// kernel getPredictions - modify fitness vector according to threshold\n");
        fw.append("\t\t\tcomputePredictions<<< grid, threads >>>(d_outputs, d_predictions, numberOfPoints, threshold);\n");
        fw.append("\t\t\tcudaThreadSynchronize();\n");
        fw.append("\n");

        fw.append("\t\t\t// kernel getnumFalsePositives - parallel reduction\n");
        fw.append("\t\t\tfloat numFalsePositives = 0;\n");
        fw.append("\t\t\treduce_step_1d_numFalsePositives<<<gridRed,threadsRed>>>(d_dataset,d_predictions,d_inter, numberOfPoints,numberOfVars);\n");
        fw.append("\t\t\tcudaThreadSynchronize();\n");
        fw.append("\t\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
        fw.append("\t\t\tfor(int i=0;i<num_blocks_red;i++){\n");
        fw.append("\t\t\t\tnumFalsePositives += h_inter[i];\n");
        fw.append("\t\t\t}\n");
        fw.append("\n");
        
        fw.append("\t\t\t// kernel getnumTruePositives - parallel reduction\n");
        fw.append("\t\t\tfloat numTruePositives = 0;\n");
        fw.append("\t\t\treduce_step_1d_numTruePositives<<<gridRed,threadsRed>>>(d_dataset,d_predictions,d_inter, numberOfPoints,numberOfVars);\n");
        fw.append("\t\t\tcudaThreadSynchronize();\n");
        fw.append("\t\t\tcudaMemcpy(h_inter, d_inter, mem_size_inter, cudaMemcpyDeviceToHost);\n");
        fw.append("\t\t\tfor(int i=0;i<num_blocks_red;i++){\n");
        fw.append("\t\t\t\tnumTruePositives += h_inter[i];\n");
        fw.append("\t\t\t}\n");
        fw.append("\n");
        
        fw.append("\t\t\t// remains the same\n");
        fw.append("\t\t\tfloat fpRatio = numFalsePositives/ (float) totalNegatives;\n");
        fw.append("\t\t\tfloat tpRatio = numTruePositives / (float) totalPositives;\n");
        fw.append("\t\t\tfalsePositives[l] = fpRatio;\n");
        fw.append("\t\t\ttruePositives[l] = tpRatio;\n");
        fw.append("\t\t}// end for lambdas\n");
        fw.append("\n");

        fw.append("\t\tfloat totalArea = 0;\n");
        fw.append("\t\tfor(int l=1;l<=numberOfLambdas;l++){\n");
        fw.append("\t\t\tfloat a = falsePositives[l-1];\n");
        fw.append("\t\t\tfloat b = falsePositives[l];\n");
        fw.append("\t\t\tfloat fa = truePositives[l-1];\n");
        fw.append("\t\t\tfloat fb = truePositives[l];\n");
        fw.append("\t\t\tfloat areaTrap = (b-a) * ((fa+fb)/(float) 2);\n");
        fw.append("\t\t\ttotalArea += areaTrap;\n");
        fw.append("\t\t}\n");
	fw.append("\t\toutput << " + '"' + "FitnessIndividual " + '"' +" << (indexIndi+1) <<" + '"' + ": " + '"' + " << totalArea << \" \" << minPhenotype << \" \" << maxPhenotype << endl;\n");
        fw.append("\t}\n");
	fw.append("\n"); 
        
        fw.append("\toutFile << output.str();\n");
        fw.append("\toutFile.close();\n");        
        fw.append("\n");
    
        fw.append("\tfree(h_individuals);\n");
        fw.append("\tfree(h_expression);\n");
        fw.append("\tfree(h_inter);\n");	
        fw.append("\n");
	
        fw.append("\tcudaFree(d_inter);\n");
        fw.append("\tcudaFree(d_dataset);\n");
        fw.append("\tcudaFree(d_outputs);\n");
	fw.append("\tcudaFree(d_predictions);\n");
        fw.append("\tcudaFree(d_expression);\n");
        fw.append("\n");
        
        fw.append("\tcudaThreadExit();\n");
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
        
        generateMain(numberOfLines,numberOfVars,numberOfResults,maxIndividualSize,filePathIndividuals);
        
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
    
    public void readResults(ArrayList<Float> alFitness, ArrayList<Float> alMinOutputs, ArrayList<Float> alMaxOutputs) throws FileNotFoundException{
        String fileOutput = "tempFiles/resultsSRRocCuda.txt";
        Scanner sc = new Scanner(new FileReader(fileOutput));
        while(sc.hasNextLine()){
            String lineAux = sc.nextLine();
            String[] lineSplit = lineAux.split(" ");
            String fitnessSAux = lineSplit[2];
            if(fitnessSAux.equals("nan")|| fitnessSAux.equals("inf") || fitnessSAux.equals("-inf") || fitnessSAux.equals("-nan")){
                alFitness.add(Float.MAX_VALUE);
            }else{
                float fitnessFAux = Float.parseFloat(fitnessSAux);
                alFitness.add(fitnessFAux);
            }
            String minOutputS = lineSplit[3];
            float minTrainOutput = Float.MIN_VALUE;
            if(minOutputS.equals("nan")|| minOutputS.equals("inf") || minOutputS.equals("-inf") || minOutputS.equals("-nan")){
                alMinOutputs.add(minTrainOutput);
            }else{
                minTrainOutput = Float.parseFloat(minOutputS);
                alMinOutputs.add(minTrainOutput);
            }
            
            String maxOutputS = lineSplit[4];
            float maxTrainOutput = Float.MAX_VALUE;
            if(minOutputS.equals("nan")|| minOutputS.equals("inf") || minOutputS.equals("-inf") || minOutputS.equals("-nan")){
                alMaxOutputs.add(maxTrainOutput);
            }else{
                maxTrainOutput = Float.parseFloat(maxOutputS);
                alMaxOutputs.add(maxTrainOutput);
            }
            
        }
    }
    
}
