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
package evogpj.bool;

/**
 * Encodes functions of two arguments.
 * 
 * @author Ignacio Arnaldo
 */
public abstract class TwoArgBooleanFunction extends BooleanFunction {

	protected final BooleanFunction arg1;
	protected final BooleanFunction arg2;

	public TwoArgBooleanFunction(BooleanFunction a1, BooleanFunction a2) {
		arg1 = a1;
		arg2 = a2;
	}

}