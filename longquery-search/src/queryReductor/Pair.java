//Copyright 2017 Murilo V. F. Menezes
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

/*
 * Class to store a pair of values, used with the KNN classifier.
 * 
 * Author: Murilo V. F. Menezes
 */

package queryReductor;

public class Pair {
	private final double value;
	private final int index;
	
	public Pair(double x, int y) {
		this.value = x;
		this.index = y;
	}
	
	public double getFirst() {
		return(value);
	}
	
	public int getSecond() {
		return(index);
	}
	
	public Boolean isEqual(Pair P) {
		if (P.getFirst() == this.value) {
			if (P.getSecond() == this.index) {
				return true;
			}
		}
		return false;
	}
}
