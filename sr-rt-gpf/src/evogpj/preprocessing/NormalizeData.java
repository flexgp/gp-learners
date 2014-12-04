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
 */
package evogpj.preprocessing;

import evogpj.evaluation.java.CSVDataJava;
import java.io.IOException;

/**
 * Normalize a dataset column-wise
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
