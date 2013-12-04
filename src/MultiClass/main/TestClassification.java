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
import evogpj.genotype.Genotype;
import evogpj.genotype.TreeGenerator;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author nacho
 */
public class TestClassification {
    
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

    public TestClassification() throws FileNotFoundException, IOException{
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
    
    public void validateMulticlassModel() throws IOException{
        //Data ad = new CSVData(fileTestSet,numLinesTestSet,numberOfFeatures);
        DataJava ad = new CSVDataJava(fileTestSet);
        PredictionMatrix pm = new PredictionMatrix(ad,codeWord,bestPop.size(),numClasses,bestPop);
        pm.getBinaryPredictions(bestPop);
        pm.computeMultiClassPredictions();
        if(MSDdataset) pm.transformToDecades();
        float accuracy = pm.getAccuracy();
        pm.getMetrics();
        System.out.println("FINAL ACCURACY: " + accuracy);
    }
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filePath = "bestModelsNewLambdas.txt";
        TestClassification tc = new TestClassification();
        tc.readPopulation(filePath);
        tc.loadProperties();
        tc.createCodewordMatrix();
        tc.validateMulticlassModel();
        
        
        //gpufgp.splitDataset();
        //mcgpu.runBinaryProblems();

        
        
        // evaluate Model (validationSet)
        //mcgpu.validateMulticlassModel();
    }
}
