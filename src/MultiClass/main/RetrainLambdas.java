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
package MultiClass.main;

import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import evogpj.algorithm.SymbRegMOO;
import evogpj.algorithm.Parameters;
import evogpj.evaluation.cuda.DataCuda;
import evogpj.evaluation.cuda.SRRocCVCuda;
import evogpj.genotype.Genotype;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author nacho
 */
public class RetrainLambdas {
    
    String fileTrainSet;
    String fileCrossValSet;
    String fileTestSet;
    String fileTempTrainSet;
    String fileTempCrossValSet;
    int numClasses;
    int numBinaryProblems;
    int numberOfFeatures;
    int numLinesTrainSet, numLinesCrossValSet, numLinesTestSet;
    Random r;
    Population bestPop;
    boolean MSDdataset;
    boolean MSDdatasetCPU;
    boolean shuttleDataset;
    boolean exhaustiveCode;
    boolean oneVsAll;
    boolean consecutiveDecades;
    int[][] codeWord;
    List<String> FUNC_SET;
    List<String> UNARY_FUNC_SET;

    public RetrainLambdas() throws FileNotFoundException, IOException{
        r = new Random();
        bestPop = new Population();
        FUNC_SET = null;
        UNARY_FUNC_SET = null;    
    }
    
    private void loadProperties(){
        //(open and read from fileProps)      
        MSDdataset = true;
        MSDdatasetCPU = false;
        shuttleDataset = false;
        exhaustiveCode = false;
        consecutiveDecades = false;
        oneVsAll = true;
        // dataset
        if(MSDdataset){
            numClasses = 10;
            if(exhaustiveCode || consecutiveDecades){
                numBinaryProblems = 511;
            }else{
                numBinaryProblems = numClasses;
            }
            numberOfFeatures = 90;
            numLinesTrainSet = 361947;
            fileTrainSet = "ProblemData/MSD-Decades-train-70pct-000.csv";
            fileTempTrainSet = "ProblemData/TEMP-MSD-Decades-train-70pct-000.csv";
            numLinesCrossValSet = 51177;
            fileCrossValSet = "ProblemData/MSD-Decades-fusion-train-10pct-000.csv";
            fileTempCrossValSet = "ProblemData/TEMP-MSD-Decades-fusion-train-10pct-000.csv";
            numLinesTestSet = 102440;
            fileTestSet = "ProblemData/MSD-Decades-test-20pct-000.csv";           
        }else if(shuttleDataset){
            numClasses = 7;
            if(exhaustiveCode){
                numBinaryProblems = 63;
            }else{
                numBinaryProblems = numClasses;
            }
            numberOfFeatures = 9;
            numLinesTrainSet = 37700;
            //numLinesTrainSet = 43500;
            fileTrainSet = "ProblemData/shuttleTrain.data";
            //fileTrainSet = "ProblemData/shuttleTrainAndCrossVal.data";
            fileTempTrainSet = "ProblemData/TEMP-shuttleTrain.data";
            //fileTempTrainSet = "ProblemData/TEMP-shuttleTrainAndCrossVal.data";
            //numLinesCrossValSet = 5800;
            numLinesCrossValSet = 43500;
            //fileCrossValSet = "ProblemData/shuttleCrossVal.data";
            fileCrossValSet = "ProblemData/shuttleTrainAndCrossVal.data";
            //fileTempCrossValSet = "ProblemData/TEMP-shuttleCrossVal.data";
            fileTempCrossValSet = "ProblemData/TEMP-shuttleTrainAndCrossVal.data";
            numLinesTestSet = 14500;
            fileTestSet = "ProblemData/shuttleTest.data";           
        }
    }

    public void createCodewordMatrix() throws IOException{
        if(exhaustiveCode){
            createCodewordMatrixExhaustive();
        }else if(oneVsAll){
            createCodewordMatrixOneVsAll();
        }else if(consecutiveDecades){
            createCodeWordMatrixConsecutives();
        }
    }
    
    public void createCodeWordMatrixConsecutives() throws IOException{
        //int numCols = (int)Math.pow(2,(numClasses-1)) - 1;
        int numCols = numBinaryProblems;
        int[][] codeWordExhaustive = new int[numClasses][numBinaryProblems];
        int k = numClasses;
        for(int j=0;j<numCols;j++){
            codeWordExhaustive[0][j] = 1;
        }
        for(int i=1;i<k;i++){
            int stepSize = (int)Math.pow(2, (k-(i+1)));
            int numSteps = numCols / stepSize;
            for(int j=0;j<numSteps;j++){
                int indexStart = j*stepSize;
                int indexEnd = (j+1)*stepSize;
                if(j%2==0){// fill with zeros
                    for(int l=indexStart;l<indexEnd;l++) codeWordExhaustive[i][l] = 0;
                }else{//fill with ones
                    for(int l=indexStart;l<indexEnd;l++) codeWordExhaustive[i][l] = 1;
                }
            }
            // deal with the rest
            int indexStart = numSteps*stepSize;
            int indexEnd = numCols;
            if (numSteps%2==0){
                for(int l=indexStart;l<indexEnd;l++) codeWordExhaustive[i][l] = 0;
            }else{
                for(int l=indexStart;l<indexEnd;l++) codeWordExhaustive[i][l] = 1;
            }
        }
        boolean[] validCols = new boolean[numBinaryProblems];
        int countValid = 0;
        for(int j=0;j<numBinaryProblems;j++){
            boolean colIsValid = true;
            int numChanges = 0;
            //for(int i=0;i<numClasses;i++){
            for(int i=0;i<numClasses;i++){
                if(codeWordExhaustive[i][j] != codeWordExhaustive[(i+1)%numClasses][j]) numChanges++;
                //if(codeWordExhaustive[i][j] != codeWordExhaustive[i+1][j]) numChanges++;
            }
            //if(numChanges==2) {
            if(numChanges==2) {
                colIsValid = true;
            }else colIsValid = false;
            if(colIsValid) countValid++;
            validCols[j] = colIsValid;
        }
        
        codeWord = new int[numClasses][countValid];
        int colIndex =0;
        for(int j=0;j<numBinaryProblems;j++){
            if(validCols[j]){
                for(int i=0;i<numClasses;i++){
                    codeWord[i][colIndex] = codeWordExhaustive[i][j];
                }
                colIndex++;
            }
        }
        numBinaryProblems = countValid;
        
        BufferedWriter fw;
        if(MSDdataset){
            fw = new BufferedWriter(new FileWriter("codewordMSD.txt"));
        }else{
            fw = new BufferedWriter(new FileWriter("codewordShuttle.txt"));
        }        
        for(int i=0;i<numClasses;i++){
            for(int j=0;j<numBinaryProblems;j++){
                System.out.print(codeWord[i][j] + " ");
                fw.append(codeWord[i][j] + " ");
            }
            System.out.println();
            fw.append("\n");
        }
        fw.flush();
        fw.close();
        
    }
    
    public void createCodewordMatrixExhaustive() throws IOException{
        //int numCols = (int)Math.pow(2,(numClasses-1)) - 1;
        int numCols = numBinaryProblems;
        codeWord = new int[numClasses][numCols];
        int k = numClasses;
        for(int j=0;j<numCols;j++){
            codeWord[0][j] = 1;
        }
        for(int i=1;i<k;i++){
            int stepSize = (int)Math.pow(2, (k-(i+1)));
            int numSteps = numCols / stepSize;
            for(int j=0;j<numSteps;j++){
                int indexStart = j*stepSize;
                int indexEnd = (j+1)*stepSize;
                if(j%2==0){// fill with zeros
                    for(int l=indexStart;l<indexEnd;l++) codeWord[i][l] = 0;
                }else{//fill with ones
                    for(int l=indexStart;l<indexEnd;l++) codeWord[i][l] = 1;
                }
            }
            // deal with the rest
            int indexStart = numSteps*stepSize;
            int indexEnd = numCols;
            if (numSteps%2==0){
                for(int l=indexStart;l<indexEnd;l++) codeWord[i][l] = 0;
            }else{
                for(int l=indexStart;l<indexEnd;l++) codeWord[i][l] = 1;
            }
        }
        BufferedWriter fw;
        if(MSDdataset){
            fw = new BufferedWriter(new FileWriter("codewordMSD.txt"));
        }else{
            fw = new BufferedWriter(new FileWriter("codewordShuttle.txt"));
        }        
        for(int i=0;i<k;i++){
            for(int j=0;j<numCols;j++){
                System.out.print(codeWord[i][j] + " ");
                fw.append(codeWord[i][j] + " ");
            }
            System.out.println();
            fw.append("\n");
        }
        fw.flush();
        fw.close();        
    }
    
    public void createCodewordMatrixOneVsAll() throws IOException{
        //int numCols = (int)Math.pow(2,(numClasses-1)) - 1;
        int numCols = numBinaryProblems;
        codeWord = new int[numClasses][numCols];
        int k = numClasses;
        for(int i=0;i<numClasses;i++){
            for(int j=0;j<numCols;j++){
                if(i==j){
                    codeWord[i][j] = 1;
                }else{
                    codeWord[i][j] = 0;
                }
            }
        }
        BufferedWriter fw;
        if(MSDdataset){
            fw = new BufferedWriter(new FileWriter("codewordMSD.txt"));
        }else{
            fw = new BufferedWriter(new FileWriter("codewordShuttle.txt"));
        }        
        for(int i=0;i<k;i++){
            for(int j=0;j<numCols;j++){
                System.out.print(codeWord[i][j] + " ");
                fw.append(codeWord[i][j] + " ");
            }
            System.out.println();
            fw.append("\n");
        }
        fw.flush();
        fw.close();
    }
        
    private int getClassFromDecade(int decade){
        int classNumber = 0;
        if(decade==1920){
            classNumber = 1;
        }else if (decade==1930){
            classNumber = 2;
        }else if (decade==1940){
            classNumber = 3;
        }else if (decade==1950){
            classNumber = 4;
        }else if (decade==1960){
            classNumber = 5;
        }else if (decade==1970){
            classNumber = 6;
        }else if (decade==1980){
            classNumber = 7;
        }else if (decade==1990){
            classNumber = 8;
        }else if (decade==2000){
            classNumber = 9;
        }else if (decade==2010){
            classNumber = 10;
        }
        return classNumber;
    }
    
    public void modifyTempFile(int numBinaryRun,boolean isTrain) throws FileNotFoundException, IOException{
        BufferedReader fr;
        BufferedWriter fw;
        if(isTrain){
            fr = new BufferedReader(new FileReader(fileTrainSet));
            fw = new BufferedWriter(new FileWriter(fileTempTrainSet));
        }else{// modify the cross validation file
            fr = new BufferedReader(new FileReader(fileCrossValSet));
            fw = new BufferedWriter(new FileWriter(fileTempCrossValSet));
        }
        String[] token;
        while (fr.ready()) {
            token = fr.readLine().split(",");
            for (int i = 0; i < token.length - 1; i++) {
                double dAux = Double.valueOf(token[i]);
                fw.append(dAux + ",");
            }
            Integer label = Integer.valueOf(token[token.length - 1]);
            int classNumber = 0;
            if(MSDdataset){
                classNumber = getClassFromDecade(label);
            }else if(shuttleDataset){
                classNumber = label;
            }
            float binaryLabel = 0;
            if(codeWord[(classNumber-1)][numBinaryRun]==1){
                binaryLabel = 1;
            }
            fw.append(binaryLabel + "\n");
        }
        fr.close();
        fw.flush();
        fw.close();
    }

    public float getFalsePositiveWeight(int binProblemIndex){
        /*TRAINING SET: MSD-Decades-train-70pct-000.csv
        Songs in the 1920s: 139
        Songs in the 1930s: 131
        Songs in the 1940s: 266
        Songs in the 1950s: 2147
        Songs in the 1960s: 8489
        Songs in the 1970s: 17828
        Songs in the 1980s: 29293
        Songs in the 1990s: 87074
        Songs in the 2000s: 210116
        Songs in the 2010s: 6464*/
        int[] appearances = new int[10];
        appearances[0] = 139;
        appearances[1] = 131;
        appearances[2] = 266;
        appearances[3] = 2147;
        appearances[4] = 8489;
        appearances[5] = 17828;
        appearances[6] = 29293;
        appearances[7] = 87074;
        appearances[8] = 210116;
        appearances[9] = 6464;
        int numPositives = 0;
        for(int i=0;i<numClasses;i++){
            if(codeWord[i][binProblemIndex]==1){
                numPositives += appearances[i];
            }
        }
        int numNegatives = numLinesTrainSet - numPositives;
        float weight = numNegatives / (float)numLinesTrainSet;
        return weight;
    }
    
    public float getFalseNegativeWeight(int binProblemIndex){
        /*TRAINING SET: MSD-Decades-train-70pct-000.csv
        Songs in the 1920s: 139
        Songs in the 1930s: 131
        Songs in the 1940s: 266
        Songs in the 1950s: 2147
        Songs in the 1960s: 8489
        Songs in the 1970s: 17828
        Songs in the 1980s: 29293
        Songs in the 1990s: 87074
        Songs in the 2000s: 210116
        Songs in the 2010s: 6464*/
        int[] appearances = new int[10];
        appearances[0] = 139;
        appearances[1] = 131;
        appearances[2] = 266;
        appearances[3] = 2147;
        appearances[4] = 8489;
        appearances[5] = 17828;
        appearances[6] = 29293;
        appearances[7] = 87074;
        appearances[8] = 210116;
        appearances[9] = 6464;
        int numPositives = 0;
        for(int i=0;i<numClasses;i++){
            if(codeWord[i][binProblemIndex]==1){
                numPositives += appearances[i];
            }
        }
        int numNegatives = numLinesTrainSet - numPositives;
        float weight = numPositives / (float)numLinesTrainSet;
        return weight;
    }
    
    public void retrainBinaryProblems() throws FileNotFoundException, IOException{
        String bestNewLambdas = "bestModelsNewLambdas.txt";
        BufferedWriter bw = new BufferedWriter(new FileWriter(bestNewLambdas));
        PrintWriter printWriter = new PrintWriter(bw);
        
        int targetNumber = 1;
        int externalThreads = 1;

        String[] ops = {"plus", "sin", "times", "minus", "mydivide", "mylog", "log","exp", "square", "cube", "+", "*", "-", "quart", "cos", "sqrt", "mysqrt"};
        String[] unaryOps = {"sin", "mylog", "log", "exp", "square", "cube", "quart", "cos", "sqrt", "mysqrt"};

        FUNC_SET = new ArrayList<String>();
        UNARY_FUNC_SET = new ArrayList<String>();
        FUNC_SET.addAll(Arrays.asList(ops));
        UNARY_FUNC_SET.addAll(Arrays.asList(unaryOps));
        
        for(int i=0;i<numBinaryProblems;i++){
            boolean isTrainSet = false;
            modifyTempFile(i,isTrainSet);
            
            DataCuda ed = new DataCuda(fileTempCrossValSet, targetNumber);
            ed.readAndStoreDataset();
            int numberFeatures = ed.getNumberOfFeatures();
            int numberOfFitnessCases = ed.getNumberOfFitnessCases();
            

            float fpWeight = getFalsePositiveWeight(i);
            float fnWeight = getFalseNegativeWeight(i);
            int numLambdas = 10;
            SRRocCVCuda ecv = new SRRocCVCuda(FUNC_SET, UNARY_FUNC_SET, fileTempCrossValSet,numberOfFitnessCases,
                    numberFeatures,targetNumber, externalThreads, 0,fpWeight,fnWeight,numLambdas);
            // check that same results are obtained as in original run
            // modify weights according to representation in the data
            // modify granularity of Lambda, from 10 to 100
            
            Individual classifier = bestPop.get(i);
            Population popAux = new Population();
            popAux.add(classifier);
            ecv.evalPop(popAux);

            // SAVE BEST PER GENERATION + fitness + areaROCCV + threshold
            printWriter.write(classifier.getGenotype().toString() + "\n");
            // the first fitness migt cause issues, if so replace with dummy value
            printWriter.write(classifier.getFitness(Parameters.Operators.SR_CUDA_ROC) + " " + classifier.getCrossValAreaROC() + " " + classifier.getThreshold() + "\n");
            printWriter.flush();            
            ed.deallocateDataset();
            ed = null;
        }
        printWriter.flush();
        printWriter.close();
        
    }
    

    
    public void validateMulticlassModel() throws IOException{
        /*ExternalDataCUDA ed = new ExternalDataCUDA(fileTestSet, 1);
        ed.readAndStoreDataset();
        int numberOfFeatures = ed.getNumberOfFeatures();
        int numberOfFitnessCases = ed.getNumberOfFitnessCases();
        ed = null;        
        int numExternalThreads = 1;
        int maxIndSize = 0;
        ExternalSRCUDAPred exPred = new ExternalSRCUDAPred(FUNC_SET, UNARY_FUNC_SET, fileTestSet, numberOfFitnessCases, numberOfFeatures,numExternalThreads,maxIndSize);
        exPred.evalPop(bestPop);*/
        
        //Data ad = new CSVData(fileTestSet,numLinesTestSet,numberOfFeatures);
        DataJava ad = new CSVDataJava(fileTestSet); 
        PredictionMatrix pm = new PredictionMatrix(ad,codeWord,bestPop.size(),numClasses,bestPop);
        pm.getBinaryPredictions(bestPop);
        pm.computeMultiClassPredictions();
        if(MSDdataset) pm.transformToDecades();
        float accuracy = pm.getAccuracy();
        System.out.println("FINAL ACCURACY: " + accuracy);
    }
    
    public void readPopulation(String filePath) throws FileNotFoundException, UnsupportedEncodingException{
        Scanner sc = new Scanner(new FileReader(filePath));
        ArrayList<String> alModels = new ArrayList<String>();
        ArrayList<Double> alCVAreas = new ArrayList<Double>();
        ArrayList<Double> alLambdas = new ArrayList<Double>();
        //Scanner sc = new Scanner(new FileReader(prefixExpressionsFile));
        int indexLine = 0;
        int indexModel = 0;
        while(sc.hasNextLine()){
            String line = sc.nextLine();
            if(indexLine%2==0){
                alModels.add(indexModel, line);
                indexModel++;
            }else{
                StringTokenizer st = new StringTokenizer(line, " ");
                String areaTraining = st.nextToken();
                String areaCVS = st.nextToken();
                double areaCV = Double.parseDouble(areaCVS);
                alCVAreas.add(indexModel-1,areaCV);
                String lambdaS = st.nextToken();
                double lambda = Double.parseDouble(lambdaS);
                alLambdas.add(indexModel-1,lambda);
            }
            indexLine++;
        }
        int popSize = alModels.size();
        bestPop = new Population();
        for(int i=0;i<popSize;i++){
            Genotype g = TreeGenerator.generateTree(alModels.get(i));
            Individual iAux = new Individual(g);
            iAux.setCrossValAreaROC(alCVAreas.get(i));
            iAux.setThreshold(alLambdas.get(i));
            bestPop.add(i, iAux);
        }
        
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filePath = "bestModels.txt";
        RetrainLambdas rl= new RetrainLambdas();
        rl.readPopulation(filePath);
        rl.loadProperties();
        rl.createCodewordMatrix();
        rl.retrainBinaryProblems();
        rl.validateMulticlassModel();
        
        
        //gpufgp.splitDataset();
        //mcgpu.runBinaryProblems();

        
        
        // evaluate Model (validationSet)
        //mcgpu.validateMulticlassModel();
    }
}
