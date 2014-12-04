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
import java.io.FileOutputStream;
import java.io.FileWriter;
//import sun.management.counter.Variability;

/**
 * Just in time C code generation to read data and store it in shared memory
 * @author Ignacio Arnaldo
 */
public class GenerateReadDataCpp {
    
    String cCode;
    Expression[] expressions;
    FileWriter fw;

    int initDataset;
    
    public GenerateReadDataCpp(){
        expressions = null;
        cCode = "";
        initDataset = 1;
        
    } 
    
    public void setExpressions(Expression[] anExpressionArray){
        expressions = anExpressionArray;
    }

    private void generateHeaders(int numberOfIndividuals){
        cCode += "#include <math.h>\n";
        cCode += "#include <stdlib.h>\n";
        cCode += "#include <stdio.h>\n";
        cCode += "#include <sys/types.h>\n";
        cCode += "#include <sys/ipc.h>\n";
        cCode += "#include <sys/shm.h>\n";
        cCode += "#include <unistd.h>\n";
        cCode += "#include <float.h>\n";
        
        cCode += "\n";
        
        cCode += "inline float scale(float val, float min, float max){\n";
	cCode += "\tfloat range = max - min;\n";
        cCode += "\tfloat scaled = 0;\n";
        cCode += "\tif(range!=0){\n";
        cCode += "\t\tscaled = (val - min) / range;\n";
        cCode += "\t}else{\n";
        cCode += "\t\tscaled = min;\n";
        cCode += "\t}\n";
	cCode += "\treturn scaled;\n";
        cCode += "}\n";
        cCode += "void runTest(int); \n";
        //cCode += "void readDataSet(char*,float*, int, int, int);\n";
        
    }
    
    private void generateMain(int numberOfIndividuals,String dataset,int numberOfLines,int numberOfVars,int numberOfResults){
        cCode += "int main(int argc, char** argv){\n";
        cCode += "\tif (argc!=2){\n";
        cCode += "\t\texit(1);\n";
        cCode += "\t}else{\n";
        cCode += "\t\tint start = atoi(argv[1]);\n";
        cCode += "\t\trunTest(start);\n";
        cCode += "\t}\n";
        cCode += "}\n";        
        cCode += "\n";
            
            
        cCode += "void readDataSet(char* filePath,float* dataset, int numberOfPoints, int numberOfVars, int numberOfResults,float* minTarget,float* maxTarget){\n";
        cCode += "\tFILE * fp;\n";
        cCode += "\tchar * line = NULL;\n";
        cCode += "\tsize_t len = 0;\n";
        cCode += "\tsize_t read;\n";
        cCode += "\tfp = fopen(filePath," + '"' + "r" + '"' + ");\n";
        cCode += "\tif (fp == NULL) exit(EXIT_FAILURE);\n";
        cCode += "\tint pointIndex = 0;\n";
        cCode += "\twhile (((read = getline(&line, &len, fp)) != -1) && (pointIndex < numberOfPoints)){\n";
        cCode += "\t\tint j;\n";
        cCode += "\t\tchar* pch;\n";
        cCode += "\t\tpch = (char*)strtok(line," + '"' + "," + '"' + ");\n";
        cCode += "\t\tfloat fAux;\n";
        cCode += "\t\tfAux = (float)atof(pch);\n";
        cCode += "\t\tdataset[pointIndex*(numberOfVars+numberOfResults)] = fAux;\n";
        cCode += "\t\tfor(j=1; j<(numberOfVars+numberOfResults);j++){\n";
        cCode += "\t\t\tpch = (char*)strtok(NULL," + '"' + "," + '"' + ");\n";
        cCode += "\t\t\tfAux = (float)atof(pch);\n";
        cCode += "\t\t\tif(j<(numberOfVars+numberOfResults) - 1){\n";
        cCode += "\t\t\t\tdataset[pointIndex*(numberOfVars+numberOfResults) + j] = fAux;\n";
        cCode += "\t\t\t}else if(j==(numberOfVars+numberOfResults) - 1){\n";
        cCode += "\t\t\t\tdataset[pointIndex*(numberOfVars+numberOfResults) + j] = fAux;\n";
        cCode += "\t\t\t\tif(fAux < *minTarget) *minTarget = fAux;\n";
        cCode += "\t\t\t\tif(fAux > *maxTarget) *maxTarget = fAux;\n";
        cCode += "\t\t\t}\n";
        cCode += "\t\t}\n";
        cCode += "\t\tpointIndex++;\n";
        cCode += "\t}\n";
        cCode += "\tint i;\n";
        cCode += "\tfor(i=0;i<pointIndex; i++){\n";
        cCode += "\t\t// SCALE RESULT\n";
        cCode += "\t\tint index = i*(numberOfVars+numberOfResults) + numberOfVars;\n";
        cCode += "\t\tfloat value = dataset[index];\n";
        cCode += "\t\tfloat scaledValue = scale(value,*minTarget,*maxTarget);\n";
        cCode += "\t\tdataset[index] = scaledValue;\t\t\n";
        cCode += "\t}\n";
        cCode += "}\n";
    
        cCode += "void runTest(int start){\n";
        cCode += "\tint numberOfPoints = " + numberOfLines + ";\n";
        cCode += "\tint numberOfVars = " + numberOfVars + ";\n";
        cCode += "\tint numberOfResults = " +numberOfResults + ";\n";
        cCode += "\t\n";
	cCode += "\tint shmid_semaphore, shmid_dataset, shmid_minTarget, shmid_maxTarget;\n";
	cCode += "\tkey_t key_semaphore, key_dataset, key_minTarget, key_maxTarget;\n";
	cCode += "\tfloat* sm_dataset;\n";
	cCode += "\tint* sm_semaphore;\n";
	cCode += "\tfloat* sm_minTarget;\n";
	cCode += "\tfloat* sm_maxTarget;\n";
	cCode += "\tkey_semaphore = 1;\n";
	cCode += "\tkey_dataset = 2;\n";
	cCode += "\tkey_minTarget = 3;\n";
	cCode += "\tkey_maxTarget = 4;\n";
	cCode += "\n";
	cCode += "\tunsigned int mem_size_semaphore = sizeof(int);\n";
	cCode += "\tunsigned int size_dataset = numberOfPoints * (numberOfVars + numberOfResults);\n";
	cCode += "\tunsigned int mem_size_dataset = sizeof(float) * size_dataset;\n";
	cCode += "\tunsigned int mem_size_minTarget = sizeof(float);\n";
	cCode += "\tunsigned int mem_size_maxTarget = sizeof(float);\n";
	cCode += "\n";
	cCode += "\tif(start==1){\n";
	cCode += "\t\tif ((shmid_semaphore = shmget(key_semaphore, mem_size_semaphore, IPC_CREAT | 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\n";
	cCode += "\t\tif ((shmid_dataset = shmget(key_dataset, mem_size_dataset, IPC_CREAT | 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmid_minTarget = shmget(key_minTarget, mem_size_minTarget, IPC_CREAT | 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmid_maxTarget = shmget(key_maxTarget, mem_size_maxTarget, IPC_CREAT | 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\n";
	cCode += "\t\tif ((sm_semaphore = (int *)shmat(shmid_semaphore, NULL, 0)) == (int *) -1) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmat" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\n";
	cCode += "\t\tif ((sm_dataset = (float *)shmat(shmid_dataset, NULL, 0)) == (float *) -1) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmat" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((sm_minTarget = (float *)shmat(shmid_minTarget, NULL, 0)) == (float *) -1) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmat" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((sm_maxTarget = (float *)shmat(shmid_maxTarget, NULL, 0)) == (float *) -1) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmat" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\n";
	cCode += "\t\t*sm_semaphore = 0;\n";
	cCode += "\t\t*sm_maxTarget = - FLT_MAX;\n";
	cCode += "\t\t*sm_minTarget = FLT_MAX;\n";
	cCode += "\n";
	cCode += "\t\tchar* filePath = " + '"' + dataset + '"' + "; \n";
	cCode += "\t\treadDataSet(filePath,sm_dataset,numberOfPoints, numberOfVars, numberOfResults,sm_minTarget,sm_maxTarget);\n";
	cCode += "\n";
	cCode += "\t\t*sm_semaphore = 1;\n";
	cCode += "\n";
	cCode += "\t\texit(0);\n";
	cCode += "\n";
	cCode += "\t}else if(start == 0){  \n";
	cCode += "\t\tif ((shmid_semaphore = shmget(key_semaphore, mem_size_semaphore, 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\n";
	cCode += "\t\tif ((shmid_dataset = shmget(key_dataset, mem_size_dataset, 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmid_minTarget = shmget(key_minTarget, mem_size_minTarget, 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmid_maxTarget = shmget(key_maxTarget, mem_size_maxTarget, 0666)) < 0) {\n";
	cCode += "\t\t\tperror(" + '"' + "shmget" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmctl(shmid_dataset,IPC_RMID,0))==-1){\n";
	cCode += "\t\t\tperror(" + '"' + "shmctl" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmctl(shmid_semaphore,IPC_RMID,0))==-1){\n";
	cCode += "\t\t\tperror(" + '"' + "shmctl" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmctl(shmid_minTarget,IPC_RMID,0))==-1){\n";
	cCode += "\t\t\tperror(" + '"' + "shmctl" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\t\tif ((shmctl(shmid_maxTarget,IPC_RMID,0))==-1){\n";
	cCode += "\t\t\tperror(" + '"' + "shmctl" + '"' + ");\n";
	cCode += "\t\t\texit(1);\n";
	cCode += "\t\t}\n";
	cCode += "\n";
	cCode += "\t\texit(0);\n";
	cCode += "\t}\n";
	cCode += "}\n";
    }

    public void generateCode(int numberOfIndividuals,String dataset,int numberOfLines,int numberOfVars,int numberOfResults){
        generateHeaders(numberOfIndividuals);
        generateMain(numberOfIndividuals,dataset,numberOfLines,numberOfVars,numberOfResults);
    }
    
    public void printCode(){
        System.out.println(cCode);
    }
    
    public void printCodeToFile(String fileName){
        try{
            FileOutputStream file=new FileOutputStream(fileName);
            file.write(cCode.getBytes());
            file.close();
        }catch(Throwable e){
            System.out.println("Error writing c file");
        }
    }
    
    public void compileFile(String fileName,String binName){
        try{
            String command = "gcc -o2 -o " + binName + " " + fileName + " -lm" ;
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        }catch(Throwable e){
            System.out.println("Error writing c file");
        }
    }
    
    public void setInitDataset(int aValue){
        initDataset = aValue;
    }
    
    public void runCode(String binName){
        try{
            String command = binName + " " + initDataset;
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        }catch(Throwable e){
            System.out.println("Error running read Dataset binary");
        }
    }
    
}
