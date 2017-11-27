package queryReductor;

import java.util.ArrayList;
import java.util.Vector;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Math.*;

public class LogisticRegressionClassifier {
	
	private ArrayList<ArrayList<Double> > data;
	private ArrayList<Double> params;
	private double maxIdf;
	private double maxTf;
	private double maxPos;
	private double minPos;
	
	public LogisticRegressionClassifier() {
		params = new ArrayList<Double>();
	}
	
	protected double logisticFunction(double x) {
		return 1/(1+Math.exp(-x));
	}
	
	protected ArrayList<Double> gradient(){
		ArrayList<Double> grad = new ArrayList<Double>();
//		for (int i=0; i<params.size(); i++) {
		double[] gradElements = new double[params.size()];
		for (int j=0; j<data.size(); j++) {
			ArrayList<Double> sample = data.get(j);
			double net = 0;
			double commonElement = 0;
			for (int k=0; k<(params.size()-1); k++) {
				net += params.get(k).doubleValue()*sample.get(k).doubleValue();
			}
			net += params.get(params.size()-1).doubleValue();
				
			double logistValue = logisticFunction(net);
				
			if (sample.get(sample.size()-1).doubleValue() == 1) {
				commonElement += ((1/logistValue)*
						(logistValue * (1 - logistValue)));
			} else {
				commonElement += ((1/(1-logistValue))*
						-(logistValue * (1 - logistValue)));
			}
			for (int i=0;i<gradElements.length;i++) {
				gradElements[i] += commonElement;
				if (i != (gradElements.length-1)) {
					gradElements[i] *= sample.get(i).doubleValue();
				}
			}
		}
		for (int i=0;i<gradElements.length;i++) {
//			System.out.println("Gradient "+ i +": " + gradElements[i]);
			grad.add(gradElements[i]);
		}
//		}
		return grad;
	}
	
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
	
	public void loadModel(String file) throws ClassNotFoundException {
		try {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			this.params = (ArrayList<Double>) in.readObject();
			in.close();
			fileIn.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveModel(String path) {
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(params);
			out.close();
			fileOut.close();
			System.out.println("Saved model in file " + path);
		} catch(IOException e) {
			e.printStackTrace();
		}
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
	
	protected double getMaxLogLik() {
		double logProd = 0;
		
		for (int i=0; i<data.size(); i++) {
			ArrayList<Double> sample = data.get(i);
			double net = 0;
			for (int j=0; j<(sample.size()-1); j++) {
				net += (params.get(j)*sample.get(j));
			}
			net += params.get(sample.size()-1);
			
			double logLik = Math.log(logisticFunction(net));
			if (sample.get(sample.size()-1) == 0)	logLik = 1-logLik;
			logProd += logLik;
		}
		
		return logProd;
	}
	
	protected void initParams(int size) {
		for (int i=0; i<size; i++) {
			double number = .01;//Math.random();
			params.add(number);
		}
	}
	
	public void train(String dataPath, int maxIter, double step) throws ClassNotFoundException {
		loadData(dataPath);
		normalizeData();
		int size = data.get(0).size();
//		System.out.println("Batman size: " + size);
		initParams(size);
		double diff = 1;
//		for (int i=0;i<maxIter;i++) {
		while (true) {
			double thisDiff = 0;
			
			ArrayList<Double> grad = gradient();
			for (int j=0;j<params.size();j++) {
//				if (!params.get(j).isNaN())	System.out.println("param value " + params.get(j));
				double newValue = params.get(j).doubleValue() + 
						step*grad.get(j).doubleValue();
				thisDiff += Math.pow((params.get(j).doubleValue() - newValue),2);
				params.set(j, Double.valueOf(newValue));
			}
//			System.out.println("");
			thisDiff /= params.size();
			if (thisDiff < (.0001*diff))	break;
			diff = thisDiff;
		}
	}
	
	public int predict(ArrayList<Double> sample, double threshold) {
		double net = 0;
		for (int i=0; i<(params.size()-1);i++) {
			net += (sample.get(i).doubleValue() * 
					params.get(i).doubleValue());
		}
		net += params.get(params.size()-1).doubleValue();
//		System.out.println("net value: " + net);
		double logVal = logisticFunction(net);
		int pred = 0;
//		System.out.println("logistic value: " + logVal);
		if (logVal > threshold)	pred = 1;
//		if (sample.get(4).doubleValue() == 1)	pred = 0;
		return pred;
	}
	
	public ArrayList<Integer> predictAll(ArrayList<ArrayList<Double> > dataToPredict,
			double threshold){
		ArrayList<Integer> preds = new ArrayList<Integer>();
		System.out.println("Size1: " + dataToPredict.size());
		for (int i=0; i<dataToPredict.size(); i++) {
			ArrayList<Double> sample = dataToPredict.get(i);
			int pred = predict(sample,threshold);
			preds.add(pred);
		}
		System.out.println("Size1: " + preds.size());
		return preds;
	}
}
