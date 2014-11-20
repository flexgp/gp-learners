/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package evogpj.preprocessing;

import evogpj.evaluation.java.CSVDataJava;
import java.io.IOException;

/**
 *
 * @author Ignacio Arnaldo
 */
public class NormalizeData {
    
    String filePath;
    
    public NormalizeData(String aFilePath){
        filePath = aFilePath;
    }
    
    public void normalize(String newFilePath,String pathToBounds) throws IOException{
        CSVDataJava data = new CSVDataJava(filePath);
        data.normalizeValues(newFilePath,pathToBounds);
    }
    
    
}
