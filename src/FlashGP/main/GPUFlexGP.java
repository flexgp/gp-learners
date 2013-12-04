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
package FlashGP.main;

import evogpj.evaluation.java.CSVDataJava;
import evogpj.evaluation.java.DataJava;
import FlashGP.postProcessing.EvalFusedModel;
import FlashGP.postProcessing.ModelFuserARM;
import evogpj.algorithm.SymbRegMOO;
import evogpj.gp.Individual;
import evogpj.gp.Population;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author nacho
 */
public class GPUFlexGP {
    
    String fileProps;
    
    boolean FACTOR_FUNCS, FACTOR_TERMS, FACTOR_FITNESS,FACTOR_DATA;
    int numberOfFeatures, numberOfLines;
    //int numIslands;
    String dataset,trainingSet, validationSet, nodeTrainingSet, fuseTrainingSet;
    float split_frac_tr_v, split_frac_ntr_ftr, frac_sample;    
    int numLinesNTr, numLinesFTr, numLinesTr, numLinesV;
    boolean round, sampleToTrain;
    Random r;
    Population bestPop;
    boolean genericDataset;
    boolean MSDdataset;
    boolean NOxDataset;
    double[] weights;
    
    
    public GPUFlexGP(String aFileProps,boolean fTERMS,boolean fDATA) throws FileNotFoundException, IOException{
        fileProps = aFileProps;
        r = new Random();
        bestPop = new Population();
        FACTOR_FUNCS = false;
        FACTOR_TERMS = fTERMS;
        FACTOR_FITNESS = false;
        FACTOR_DATA = fDATA;
    }
    
    private void loadProperties(){
        //(open and read from fileProps)      

        //numIslands = 30;
        
        genericDataset = false;
        MSDdataset = true;
        NOxDataset = false;
        // dataset
        if(genericDataset){
            numberOfLines = 1000;
            dataset = "ProblemData/years_tracks_train.csv";
            // dataset = TrainingSet u ValidationSet
            split_frac_tr_v = (float)0.9;
            numLinesTr = (int)(numberOfLines * split_frac_tr_v);
            trainingSet = "ProblemData/TrainingSet.txt";
            numLinesV = (int) (numberOfLines * (1-split_frac_tr_v));
            validationSet = "ProblemData/ValidationSet.txt";
            // trainingSet = NodeTrainingSet u FusioTrainingSet
            split_frac_ntr_ftr = (float) 0.8;
            numLinesNTr = (int)(numLinesTr * split_frac_ntr_ftr);    
            numLinesFTr = (int)(numLinesTr * (1-split_frac_ntr_ftr));
            // learning set = 0.6 * nodeTrainingSet
            frac_sample = (float) 0.6;    
            sampleToTrain = true;
        }else if(MSDdataset){
            numberOfFeatures = 90;
            numberOfLines = 466128;
            numLinesTr = 361947;
            trainingSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-train-70pct-000.csv";
            numLinesV = 102440;
            validationSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-test-20pct-000.csv";
            numLinesNTr = 361947;
            nodeTrainingSet = trainingSet;
            numLinesFTr = 51177;
            // this is running out of heap memory!!!
            //fuseTrainingSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-fusion-train-plus-80pct-000.csv";
            // this one works
            fuseTrainingSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-fusion-train-10pct-000.csv";
            round = true; 
            sampleToTrain = false;
        }else if(NOxDataset){
            numberOfFeatures = 18;
            numLinesTr = 4017;
            trainingSet = "/media/DATA/datasets/NOx/TraindatasetBalanced2.txt";
            numLinesNTr = 4017;
            nodeTrainingSet = trainingSet;
            numLinesFTr = 4017;
            fuseTrainingSet = "/media/DATA/datasets/NOx/TraindatasetBalanced2.txt";
            numLinesV = 1210;    
            validationSet = "/media/DATA/datasets/NOx/TestdatasetBalanced2.txt";
            round = false;
            sampleToTrain = false;
        }
    }
    
    public void splitDataset() throws FileNotFoundException, IOException{
        //Split dataset in Training and ValidationSet
        split_data(dataset, split_frac_tr_v, validationSet, trainingSet, numberOfLines);
        //Split Training Set in NodeTrainingSet and FusionTrainingSet
        split_data(trainingSet, split_frac_ntr_ftr, fuseTrainingSet, nodeTrainingSet,numLinesTr);
    }
    
    private void split_data(String finput,float aSplit_frac,String fout1, String fout2, int numberOfLines) throws FileNotFoundException, IOException{
        
        BufferedReader br = new BufferedReader(new FileReader(finput));

        FileWriter fileOutput1 = new FileWriter(fout1);
        FileWriter fileOutput2 = new FileWriter(fout2);
        float split_frac = 0;
        if (aSplit_frac > 1.0){
            split_frac = (float)aSplit_frac/ (float)numberOfLines;
        }else{
            split_frac = aSplit_frac;
        }
        //we want to select 1 out of every m, so we just take the smaller percentage and swap it back when we return
        int i;
        if (split_frac <= 0.5){
            i = (int) (numberOfLines * split_frac);
        }else{
            i = (int) (numberOfLines* (1.0-split_frac));
            FileWriter fwaux = fileOutput1;//swap pointers
            fileOutput1 = fileOutput2;
            fileOutput2 = fwaux;
        }
        int n = numberOfLines;
        while(i>0){
            int slice_size = n/i;
            int pos = r.nextInt(slice_size);
            String line;
            for(int lineIndex=0;lineIndex<slice_size;lineIndex++){
                line = br.readLine();
                if(lineIndex==pos){
                    fileOutput2.write(line + "\n");
                }else{
                    fileOutput1.write(line + "\n");
                }
            }
            n -= slice_size;
            i-=1;
        }
        fileOutput1.flush();
        fileOutput1.close();
        fileOutput2.flush();
        fileOutput2.close();
        
    }   
    
    private void sample_data(String finput,float aSample_frac,String fout,int numberOfLines) throws FileNotFoundException, IOException{
        
        BufferedReader br = new BufferedReader(new FileReader(finput));

        FileWriter fileOut = new FileWriter(fout);
        float split_frac = 0;
        if (aSample_frac > 1.0){
            split_frac = (float)aSample_frac/ (float)numberOfLines;
        }else{
            split_frac = aSample_frac;
        }
        //we want to select 1 out of every m, so we just take the smaller percentage and swap it back when we return
        int i;
        if (split_frac <= 0.5){
            i = (int) (numberOfLines * split_frac);
        }else{
            i = (int) (numberOfLines* (1.0-split_frac));
        }
        int n = numberOfLines;
        while(i>0){
            int slice_size = n/i;
            int pos = r.nextInt(slice_size);
            String line;
            for(int lineIndex=0;lineIndex<slice_size;lineIndex++){
                line = br.readLine();
                if(lineIndex==pos && (split_frac <= 0.5)){
                    fileOut.write(line + "\n");
                }else if (lineIndex!=pos && (split_frac > 0.5)){
                    fileOut.write(line + "\n");
                }else{
                    
                }
            }
            n -= slice_size;
            i-=1;
        }
        fileOut.flush();
        fileOut.close();
    }  

    
    private String rand_terms(String fileTerms,String line) throws FileNotFoundException, IOException{
        String newline = "terminal_set =";
        BufferedReader br = new BufferedReader(new FileReader(fileTerms));
        String sNumTerms = br.readLine();
        int numTerms = Integer.valueOf(sNumTerms);
        ArrayList<String> als = new ArrayList<String>();
        while((line = br.readLine()) != null ){
            als.add(line);
        }
        int pos = r.nextInt(als.size());
        int newNumTerms = Integer.valueOf(als.get(pos));
        boolean[] usedTerms = new boolean[numTerms];
        for(int i=0;i<numTerms;i++) usedTerms[i] = false;
        for(int i=0;i<newNumTerms;i++){
            int newTerm = r.nextInt(numTerms);
            while(usedTerms[newTerm]==true) newTerm = r.nextInt(numTerms);
            
            usedTerms[newTerm] = true;
        }
        for(int i=0;i<numTerms;i++){
            if(usedTerms[i] == true) newline += " X" + (i+1);
        }
        return newline;
    }
    
    private String rand_line(String fileName) throws FileNotFoundException, IOException{
        String newline;
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        ArrayList<String> als = new ArrayList<String>();
        while((line = br.readLine()) != null ){
            als.add(line);
        }
        int numLines = als.size();
        int pos = r.nextInt(numLines);
        newline = als.get(pos);
        return newline;
    }
    

    private String modify_properties(String fileName, int numRun,String trainingSet,int numberOfLines) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String newPath = fileName.replaceFirst(".properties", numRun+".properties");//"ExperimentalSetup/properties" + numRun + ".txt";
        FileWriter propsOut = new FileWriter(newPath);
        String line;
        while((line = br.readLine()) != null ){
            if(line.startsWith("function_set") && FACTOR_FUNCS){
                String newline = rand_line("ExperimentalSetup/evogpj_funcs.txt");
                propsOut.write(newline + "\n");
            }else if(line.startsWith("terminal_set") && FACTOR_TERMS){
                String newline = rand_terms("ExperimentalSetup/msd_terms.txt", line);
                propsOut.write(newline + "\n");
            }/*else if(line.startsWith("fitness_mean_pow") && FACTOR_FITNESS){
                String newline = rand_line("ExperimentalSetup/evogpj_pnorms.txt");
                propsOut.write(newline + "\n");
            } */else if(line.startsWith("problem") && !line.startsWith("problem_type")){
                propsOut.write("problem = " + trainingSet + "\n");
            }else if(line.startsWith("fitness_cases")){
                propsOut.write("fitness_cases = " + numberOfLines + "\n");
            }else if(line.startsWith("!")){
                
            }else{
                propsOut.write(line + "\n");
            }
        }
        propsOut.flush();
        propsOut.close();
        return newPath;
    }
    
    
    public void readScaledModels(){
        /*% inputs: a base search path with subdirectories which contain models-scaled.txt files
        % and the path to the fusion training data
        function [] = fuseModels(searchPath, fusionTrainDataPath)
            % first, find scaled models
            % iterate over subdirs in the searchpath
            modelSubdirs = dir(searchPath);
            % remove '.' and '..'
            tf = ismember( {modelSubdirs.name}, {'.', '..'});
            modelSubdirs(tf) = [];
            fusionModels = [];
            for i=1:length(modelSubdirs)
                modelSubdir = modelSubdirs(i);
                rootPath = fullfile(searchPath, modelSubdir.name);
                modelsPath = fullfile(rootPath, 'models-fit.txt');
                if (exist(modelsPath) == 0) % skip directories with no scaled models
                    continue;
                end
                models = readTextFile(modelsPath);
                % add models to fusionModels
                fusionModels = [fusionModels models];
            end*/
    }
    
    public void runIslands() throws FileNotFoundException, IOException{
        
        //int numLinesSample = (int)(numLinesNTr * frac_sample); 
        //int numLinesSample = numLinesTr; 
        int numLinesSample = 0; 
        int indexIsland = 0;
        long startTimeEnsemble = System.currentTimeMillis();
        long elapsedTimeIslandEnsemble = 0;
        while(elapsedTimeIslandEnsemble < 3600000){
        //for(int i=0;i<numIslands;i++){
            //String islandTrainingSet = "ProblemData/IslandTrainingSet"+i+".txt";
            String islandTrainingSet = "";
            if(MSDdataset){
                if(FACTOR_DATA){
                    numLinesSample = 179000; 
                    int indexSample = indexIsland%20;
                    if(indexSample<10){
                        islandTrainingSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-train-35pct-000-00" + indexSample + ".csv";
                    }else{
                        islandTrainingSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-train-35pct-000-0" + indexSample + ".csv";
                    }
                    //sample_data(trainingSet, frac_sample, islandTrainingSet,numLinesNTr);
                }else{
                    numLinesSample = numLinesTr;
                    islandTrainingSet = "/media/DATA/datasets/MSD/Dylan_splits/split000/MSD-train-70pct-000.csv";
                }
            }else if(NOxDataset){
                islandTrainingSet = nodeTrainingSet;
                numLinesSample = numLinesNTr;
            }
            System.out.println("RUNNING ISLAND " + indexIsland);
            long startTimeIsland = System.currentTimeMillis();
            String newProps = modify_properties(fileProps,indexIsland,islandTrainingSet,numLinesSample);
            //AlgorithmBase ab = new AlgorithmBase(newProps,indexIsland);
            long seed = System.currentTimeMillis();
            SymbRegMOO ab = new SymbRegMOO(newProps,seed);
            ab.run_population();
            Population bestPopAux = ab.getBestPop();
            long elapsedTimeIsland = System.currentTimeMillis() - startTimeIsland;
            System.out.println("TIME ISLAND " + indexIsland + ": " + elapsedTimeIsland);
            for(Individual ind:bestPopAux) {
                boolean contains = false;
                for(Individual indAux:bestPop){
                    if(indAux.equals(ind)) contains = true;
                }
                if(!contains) bestPop.add(ind);
            }
            elapsedTimeIslandEnsemble = System.currentTimeMillis() - startTimeEnsemble;
            indexIsland++;
        }
    }
    
    public void fuseModels(){
        int iters = 100;
        ModelFuserARM mfa = new ModelFuserARM(fuseTrainingSet,numberOfFeatures,bestPop,iters,round);
        weights = mfa.arm_weights();
        
        // print final model
        for(int i=0;i<bestPop.size();i++){
            if(weights[i]>0){
                System.out.println("\t" + weights[i] + " " + bestPop.get(i).toScaledString());
                if(i<bestPop.size()-1) System.out.print("+");
            }
        }
        
        System.out.println();
    }
    
    public void validateFusedModel(){
        //Data ad = new CSVData(validationSet,numLinesV,numberOfFeatures); 
        DataJava ad = new CSVDataJava(validationSet); 
        EvalFusedModel efm = new EvalFusedModel(ad);
        double MSE = efm.eval(bestPop,weights,round);
        System.out.println(MSE);
        
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
        if (args.length == 0) {
            System.err.println("Error: must specify a properties file and whether to factor variables and data");
            System.exit(-1);
        }
        String fileProps = args[0];
        boolean fTerms = Boolean.parseBoolean(args[1]);
        boolean fData = Boolean.parseBoolean(args[2]);
        //String fileProps = "ExperimentalSetup/msd_year_regression.properties";
        //String fileProps = "ExperimentalSetup/TraindatasetBalanced2.properties";
        GPUFlexGP gpufgp= new GPUFlexGP(fileProps,fTerms,fData);
        gpufgp.loadProperties();
        //gpufgp.splitDataset();
        gpufgp.runIslands();

        gpufgp.fuseModels();
        
        // evaluate Model (validationSet)
        gpufgp.validateFusedModel();
    }
}
