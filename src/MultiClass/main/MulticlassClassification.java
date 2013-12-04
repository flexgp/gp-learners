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
import FlashGP.postProcessing.EvalFusedModel;
import evogpj.algorithm.SymbRegMOO;
import evogpj.evaluation.cuda.DataCuda;
import evogpj.evaluation.cuda.SRRocCuda;
import evogpj.evaluation.cuda.SRRocCVCuda;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author nacho
 */
public class MulticlassClassification {
    
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

    public MulticlassClassification() throws FileNotFoundException, IOException{
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
        
        // just to debug
        /*codeWord[0][0] = 1;
        codeWord[1][0] = 1;
        codeWord[2][0] = 1;
        codeWord[3][0] = 1;
        codeWord[4][0] = 1;
        codeWord[5][0] = 0;
        codeWord[6][0] = 1;*/
        
        
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

    public void runBinaryProblems() throws FileNotFoundException, IOException{
        for(int i=0;i<numBinaryProblems;i++){
            boolean isTrainSet = true;
            modifyTempFile(i,isTrainSet);
            isTrainSet = false;
            modifyTempFile(i,isTrainSet);

            String propsFile = "";
            if(MSDdataset){
                propsFile = "ExperimentalSetup/MSDDecadeTemp.properties";
            }else if(shuttleDataset){
                propsFile = "ExperimentalSetup/shuttleTemp.properties";
            }
            if(MSDdatasetCPU){
                propsFile = "ExperimentalSetup/MSDDecadeTempCPU.properties";
            }
            
            SymbRegMOO ab = new SymbRegMOO(propsFile,i);
            Individual best = ab.run_population();
            bestPop.add(best);
            if(i==0){
                FUNC_SET = ab.getFuncs();
                UNARY_FUNC_SET = ab.getUnaryFuncs();
            }
        }
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
    
    
    /**
    * Load serialized population from file
    * Filename to load from is parameter filePath
    * @throws IOException 
    * @throws ClassNotFoundException 
    */
    public Population loadPopulationFromFile(String filePath) throws IOException, ClassNotFoundException {
        
        FileInputStream fileInputStream = new FileInputStream(filePath);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        Population pop = (Population) objectInputStream.readObject();
        objectInputStream.close();
        fileInputStream.close();
        return pop;
    }
    
    /**
    * Serialize population to file
    * Filename is given by the parameter filePath
    * @throws IOException 
    */
    public void serializePopulation(Population pop, String filePath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(pop);
        objectOutputStream.close();
        fileOutputStream.close();
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        //read properties file with parameters
        //String fileProps = "ExperimentalSetup/properties.txt";
        
        //String fileProps = "ExperimentalSetup/TraindatasetBalanced2.properties";
        MulticlassClassification mcgpu= new MulticlassClassification();
        mcgpu.loadProperties();
        mcgpu.createCodewordMatrix();
        mcgpu.runBinaryProblems();
        mcgpu.validateMulticlassModel();
        
        
        //gpufgp.splitDataset();
        //mcgpu.runBinaryProblems();

        
        
        // evaluate Model (validationSet)
        //mcgpu.validateMulticlassModel();
    }
}
