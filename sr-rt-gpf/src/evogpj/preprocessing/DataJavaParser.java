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
package evogpj.preprocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataJavaParser {

	Map<String,List<String>> partitioned_data; 
	List<List<String>> data_fold;
	
	public DataJavaParser(String csvfile,int numOfFolds){
		BufferedReader f;
		partitioned_data=new HashMap<String,List<String>>();
		data_fold=new ArrayList<List<String>>();
		try {
			f = new BufferedReader(new FileReader(csvfile));
            String[] token;
            String line;
            String classLabel;
            StringBuffer temp;
            
            while((line=f.readLine())!=null){
            	token=line.split(",");
            	classLabel=token[token.length-1];
            	if (!partitioned_data.containsKey(classLabel)){
            		List<String> newClassData = new ArrayList<String>();
            		newClassData.add(line);
            		partitioned_data.put(classLabel,newClassData);
            	}else{
            		partitioned_data.get(classLabel).add(line);
            	}
            }
		}catch(IOException ex){
			ex.printStackTrace();
		}
		
		//Debug
		for(String key:partitioned_data.keySet()){
			System.out.println("Partitioned Class "+key+": "+partitioned_data.get(key).size());
		}
		generateFolds(numOfFolds,partitioned_data);
	}
	
	private void generateFolds(int numOfFolds, Map<String,List<String>> partitioned){
		Map<String,Integer> DataLength = new HashMap<String,Integer>();
		for(String key:partitioned.keySet()){
			DataLength.put(key,partitioned.get(key).size());
		}
		
		List<String> fold;
		for(int i=0;i<numOfFolds;i++){
			fold=new ArrayList<String>();
			if(i==numOfFolds-1){
				for(String key:partitioned.keySet()){
					fold.addAll(partitioned.get(key).subList(DataLength.get(key)/numOfFolds*i, DataLength.get(key)));
				}
			}else{
				for(String key:partitioned.keySet()){
					fold.addAll(partitioned.get(key).subList(DataLength.get(key)/numOfFolds*i, DataLength.get(key)/numOfFolds*(i+1)));
				}
			}
			System.out.println("Data fold amount: "+fold.size());
			data_fold.add(fold);
		}
		
	}
	
	/**
	 * Generate train and test dataset for crossvalidation
	 * @param index - the corresponding fold will be used as the test data.
	 * @return 0th index: train data, 1st index: test data
	 */
	public ArrayList<List<String>> generateDataSet(int index){
		ArrayList<List<String>> dataset = new ArrayList<List<String>>();
		List<String> testData=data_fold.get(index);
		List<String> trainData=new ArrayList<String>();
		for(int i=0;i<data_fold.size();i++){
			if(i!=index){
				trainData.addAll(data_fold.get(i));
			}
		}
		dataset.add(trainData);
		dataset.add(testData);
		return dataset;
	}
}
