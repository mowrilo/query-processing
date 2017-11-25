package queryReductor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.lang.Math.*;
import queryReductor.Pair;

public class KNNClassifier {
	
	private ArrayList<ArrayList<Double> > data;
	private int k;
	
	public KNNClassifier(String dataPath, int kValue) throws ClassNotFoundException {
		this.k = kValue;
		loadData(dataPath);
	}
	
	protected void loadData(String file) throws ClassNotFoundException {
		try {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			this.data = (ArrayList<ArrayList<Double> >) in.readObject();
			in.close();
			fileIn.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	protected double getLogEuclideanDistance(ArrayList<Double> sample1, ArrayList<Double> sample2) {
		int size = sample1.size();
		double dist = 0;
		for (int i=0; i<(size-1); i++) { //size-1 because the last element is the Target value
			double diff = sample1.get(i) - sample2.get(i);
			dist += Math.pow(diff, 2);
		}
		dist = Math.sqrt(dist);
		return Math.log(dist);
	}
	
	public int predict(ArrayList<Double> sample) {
//		int closerIndex;
//		double smallerDist = 999999;
		
		ArrayList<Pair> distances = new ArrayList<Pair>();
		
		int dataSize = data.size();
		for (int i=0; i<dataSize; i++) {
			ArrayList<Double> trainSample = data.get(i);
			double dist = getLogEuclideanDistance(sample,trainSample);
			Pair thisSample = new Pair(dist,i);
			distances.add(thisSample);
		}
		
		Collections.sort(distances, new Comparator<Pair>(){
            public int compare(Pair p1, Pair p2) {
            	Double d = new Double(p1.getFirst()- p2.getFirst());
            	int ret = d.intValue();
                return ret;
            }
        });
		
		int sumOfLabels = 0;
		for (int i=0; i<k; i++) {
			int index = distances.get(i).getSecond();
			ArrayList<Double> smp = data.get(index);
			double label = smp.get(smp.size()-1);
			sumOfLabels += label;
		}
		
		if (sumOfLabels >= ((double) k/2)) {
			return 1;
		} else {
			return 0;
		}
		
	}
}
