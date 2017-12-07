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
 * KNN Classifier
 * 
 * Author: Murilo V. F. Menezes
 */

package queryReductor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import queryReductor.Pair;

public class KNNClassifier {
	
	private ArrayList<ArrayList<Double> > data;
	private int k;
	private double maxIdf; //Data used for normalizing continuous features
	private double maxTf;
	private double maxPos;
	private double minPos;
	private double maxLen;
	private double minLen;
	
	public KNNClassifier(String dataPath, int kValue) throws ClassNotFoundException {
		this.k = kValue; //Number of neighbors
		loadData(dataPath);
		normalizeData(); //Receives and normalizes data
	}
	
	/* This method scales data to fit between 0 and 1, using
	 * its max and min values stored during the building
	 * of the database.
	 */
	protected void normalizeData() {
		ArrayList<ArrayList<Double> > data2 = new ArrayList<ArrayList<Double> >();
		for (int i=0; i<data.size(); i++) {
			ArrayList<Double> sample = data.get(i);
			ArrayList<Double> sample2 = new ArrayList<Double>();
			for (int j=0; j<sample.size(); j++) {
				if (j==0) {
					double newIdf = sample.get(j)/maxIdf;
					sample2.add(newIdf);
				} else if (j==1) {
					double newTf = sample.get(j)/maxTf;
					sample2.add(newTf);
				} else if (j==2) {
					double newPos = (sample.get(j) - minPos)/(maxPos-minPos);
					sample2.add(newPos);
				} else {
					sample2.add(sample.get(j));
				}
			}
			data2.add(sample2);
		}
		this.data = data2;
	}
	
	/*
	 * Loading data previously built
	 */
	@SuppressWarnings("unchecked")
	protected void loadData(String file) throws ClassNotFoundException {
			try {
				FileInputStream fileIn = new FileInputStream(file);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				this.data = (ArrayList<ArrayList<Double> >) in.readObject();
				this.maxIdf = in.readDouble();
				this.maxPos = in.readDouble();
				this.maxTf = in.readDouble();
				this.minPos = in.readDouble();
				in.close();
				fileIn.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
	}
	
	
	/*
	 * Given two samples, this method computed the
	 * Euclidean distance between them
	 */
	protected double getEuclideanDistance(ArrayList<Double> sample1, ArrayList<Double> sample2) {
		int size = sample1.size();
		double dist = 0;
		for (int i=0; i<size; i++) {
			double diff = sample1.get(i).doubleValue() - sample2.get(i).doubleValue();
			dist += Math.pow(diff, 2);
		}
		dist = Math.sqrt(dist);
		return dist;
	}
	
	/*
	 * This method receives a sample and classifies it
	 * according to its neighbors' labels
	 */
	public int predict(ArrayList<Double> sample) {
		
		ArrayList<Pair> distances = new ArrayList<Pair>();
		int dataSize = data.size();
		for (int i=0; i<dataSize; i++) { //Computes the distance to all the training features
			ArrayList<Double> trainSample = data.get(i);
			double dist = getEuclideanDistance(sample,trainSample);
			Pair thisSample = new Pair(dist,i);
			distances.add(thisSample);
		}
		
		Comparator<Pair> comparator = new PairComparator();
        Collections.sort(distances, comparator); //Sorts by distance
        
		double sumOfLabels = 0;
		for (int i=0; i<k; i++) { //Gets the K nearest and computes how many are labeled '1'
			int index = distances.get(i).getSecond();
			ArrayList<Double> smp = data.get(index);
			double label = smp.get(smp.size()-1);
			sumOfLabels += label;
		}
		
		if (sumOfLabels > (((double) k)*.5)) { //If more than half of the neighbors is
			return 1;							// labeled 1, the sample is labeled 1 as well
		} else {
			return 0;
		}
		
	}
	
	/*
	 * Receives a set of samples and classifies each one
	 */
	public ArrayList<Integer> predictAll(ArrayList<ArrayList<Double> > dataToPredict){
		ArrayList<Integer> preds = new ArrayList<Integer>();
		
		for (int i=0; i<dataToPredict.size(); i++) {
			ArrayList<Double> sample = dataToPredict.get(i);
			int pred = predict(sample);
			preds.add(pred);
		}
		
		return preds;
	}
}

/*
 * Compares the class Pair, for sorting purposes
 */
class PairComparator implements Comparator<Pair> {
    public int compare(Pair p1, Pair p2) {
    	if (p1.getFirst() > p2.getFirst())	return 1;
    	if (p2.getFirst() > p1.getFirst())	return -1;
    	return 0;
    }
}
