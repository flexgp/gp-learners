/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package MultiClass.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author nacho
 */
public class MSDDecades {
    
    String inputFileMSD;
    String outputFileMSD;
    int numFeatures;

    public MSDDecades(String anInputFile,String anOutputFile){
        inputFileMSD = anInputFile;
        outputFileMSD = anOutputFile;
        numFeatures = 90;
    }
    
    public void yearsToDecades() throws IOException{
        BufferedReader fr = new BufferedReader(new FileReader(inputFileMSD));
        BufferedWriter fw = new BufferedWriter(new FileWriter(outputFileMSD));
        int counter1920s = 0; 
        int counter1930s = 0;
        int counter1940s = 0;
        int counter1950s = 0;
        int counter1960s = 0;
        int counter1970s = 0;
        int counter1980s = 0;
        int counter1990s = 0;
        int counter2000s = 0;
        int counter2010s = 0;
        String[] token;
        while (fr.ready()) {
            token = fr.readLine().split(",");
            for (int i = 0; i < token.length - 1; i++) {
                double dAux = Double.valueOf(token[i]);
                fw.append(dAux + ",");
            }
            Integer year = Integer.valueOf(token[token.length - 1]);
            if(year>=1920 && year<1930){
                //year = 1920;
                year = 1;
                counter1920s++;
            }else if (year>=1930 && year<1940){
                //year = 1930;
                year = 2;
                counter1930s++;
            }else if (year>=1940 && year<1950){
                //year = 1940;
                year = 3;
                counter1940s++;
            }else if (year>=1950 && year<1960){
                //year = 1950;
                year = 4;
                counter1950s++;
            }else if (year>=1960 && year<1970){
                //year = 1960;
                year = 5;
                counter1960s++;
            }else if (year>=1970 && year<1980){
                //year = 1970;
                year = 6;
                counter1970s++;
            }else if (year>=1980 && year<1990){
                //year = 1980;
                year = 7;
                counter1980s++;
            }else if (year>=1990 && year<2000){
                //year = 1990;
                year = 8;
                counter1990s++;
            }else if (year>=2000 && year<2010){
                //year = 2000;
                year = 9;
                counter2000s++;
            }else if (year>=2010 && year<2020){
                //year = 2010;
                year = 10;
                counter2010s++;
            }
            fw.append(year + "\n");
        }
        System.out.println("Songs in the 1920s: " + counter1920s);
        System.out.println("Songs in the 1930s: " + counter1930s);
        System.out.println("Songs in the 1940s: " + counter1940s);
        System.out.println("Songs in the 1950s: " + counter1950s);
        System.out.println("Songs in the 1960s: " + counter1960s);
        System.out.println("Songs in the 1970s: " + counter1970s);
        System.out.println("Songs in the 1980s: " + counter1980s);
        System.out.println("Songs in the 1990s: " + counter1990s);
        System.out.println("Songs in the 2000s: " + counter2000s);
        System.out.println("Songs in the 2010s: " + counter2010s);
        fr.close();
        fw.flush();
        fw.close();
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String inputMSD = "ProblemData/MSD-train-70pct-000.csv";
        String outputDecades = "ProblemData/MSD-Decades-1-10-train-70pct-000.csv";
        MSDDecades msd = new MSDDecades(inputMSD,outputDecades);
        msd.yearsToDecades();
        
        inputMSD = "ProblemData/MSD-fusion-train-10pct-000.csv";
        outputDecades = "ProblemData/MSD-Decades-1-10-fusion-train-10pct-000.csv";
        msd = new MSDDecades(inputMSD,outputDecades);
        msd.yearsToDecades();
        
        inputMSD = "ProblemData/MSD-fusion-train-plus-80pct-000.csv";
        outputDecades = "ProblemData/MSD-Decades-1-10-fusion-train-plus-80pct-000.csv";
        msd = new MSDDecades(inputMSD,outputDecades);
        msd.yearsToDecades();
        
        inputMSD = "ProblemData/MSD-test-20pct-000.csv";
        outputDecades = "ProblemData/MSD-Decades-1-10-test-20pct-000.csv";
        msd = new MSDDecades(inputMSD,outputDecades);
        msd.yearsToDecades();
        
        
        
    }
    
    
}
