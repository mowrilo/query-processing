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
	private double maxIdf;
	private double maxTf;
	private double maxPos;
	private double minPos;
	
	public KNNClassifier(String dataPath, int kValue) throws ClassNotFoundException {
		this.k = kValue;
		loadData(dataPath);
		normalizeData();
	}
	
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
	
	public int predict(ArrayList<Double> sample) {
		
		ArrayList<Pair> distances = new ArrayList<Pair>();
		int dataSize = data.size();
		for (int i=0; i<dataSize; i++) {
			ArrayList<Double> trainSample = data.get(i);
			double dist = getEuclideanDistance(sample,trainSample);
			Pair thisSample = new Pair(dist,i);
			distances.add(thisSample);
		}
		
		Comparator<Pair> comparator = new PairComparator();
        Collections.sort(distances, comparator);
        
		double sumOfLabels = 0;
		for (int i=0; i<k; i++) {
			int index = distances.get(i).getSecond();
			ArrayList<Double> smp = data.get(index);
			double label = smp.get(smp.size()-1);
			sumOfLabels += label;
		}
		
		if (sumOfLabels > (((double) k)*.5)) {
			return 1;
		} else {
			return 0;
		}
		
	}
	
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

class PairComparator implements Comparator<Pair> {
    public int compare(Pair p1, Pair p2) {
    	if (p1.getFirst() > p2.getFirst())	return 1;
    	if (p2.getFirst() > p1.getFirst())	return -1;
    	return 0;
    }
}
