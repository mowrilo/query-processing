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

/* Part-Of-Speech Tagger using a Markov Model
 * 			and Viterbi's algorithm
 * Author: Murilo V. F. Menezes
 */

package queryReductor;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.FileOutputStream;
//import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.lang.Math.*;

public class PosTagger {

	private Map<String,Map<String,Integer> > countWordTag;
	private Map<String,Map<String,Integer> > countTagTag;
	private Map<String,Integer> sumOfWords;
	private Map<String,Integer> sumOfTags;
	private Vector<String> tags;
	private int nWords;
	
	public PosTagger() {
		this.countWordTag = new LinkedHashMap<String,Map<String,Integer> >();
		this.countTagTag = new LinkedHashMap<String,Map<String,Integer> >();
		this.sumOfTags = new HashMap<String,Integer>();
		this.sumOfWords = new HashMap<String,Integer>();
		this.tags = new Vector<String>(0);
		this.nWords = 0;
	};
	
	public void train(String corpusPath) {
		try {
			System.out.println("Training POS Tagger...");
			File folder = new File(corpusPath); 
	        File[] files = folder.listFiles();
	        String fileName = "";
	        for (int i=0; i<files.length;i++) {
	        	fileName = files[i].getName();
	        	fileName = corpusPath + fileName;
	        	BufferedReader rdr = new BufferedReader(new FileReader(fileName));
	        	String a;
	        	while((a = rdr.readLine()) != null) {
	        		String lastTag = "<s>";
	        		if (a.length() > 5) {
	        			StringTokenizer tokens = new StringTokenizer(a);
	        			while(tokens.hasMoreElements()) {
	        				String nextToken = tokens.nextElement().toString();
	        				String[] parts = nextToken.split("/");
	        				String word = parts[0];
	        				word = word.toLowerCase();
	        				for (int k=1; k<(parts.length-1); k++) {
	        					word+="/";
	        					word+=parts[k];
	        				}
	        				String tag = parts[parts.length-1];
	        				int titleTag = tag.indexOf("-");
	        				if (titleTag > 0) {
	        					tag = tag.substring(0, titleTag);
	        				}
//	        				
	        				addToWordMap(word,tag);
	        				addToTagMap(lastTag,tag);
	        				lastTag = tag;
	        			}
	        			String tag = "</s>";
	        			addToTagMap(lastTag,tag);
	        		}
	        	}
	        	rdr.close();
	        	saveSumOfWords();
	        	saveSumOfTags();
				for (Map.Entry<String, Integer> entry : sumOfTags.entrySet()) {
				    int n = entry.getValue().intValue();
				    this.nWords += n;
				}
	        }
	        System.out.println("Done!");
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	protected void addToWordMap(String word, String tag) {
		
		if (!this.countWordTag.containsKey(word)) {
			Map<String,Integer> empty = new HashMap<String,Integer>();
			this.countWordTag.put(word, empty);
		}
		
		if (!this.countWordTag.get(word).containsKey(tag)) {
			this.countWordTag.get(word).put(tag, 0);
		}
		
		this.countWordTag.get(word).put(tag, countWordTag.get(word).get(tag)+1);
	}
	
	protected void addToTagMap(String formerTag, String latterTag) {
		
		if (!this.countTagTag.containsKey(formerTag)) {
			Map<String,Integer> empty = new HashMap<String,Integer>();
			this.countTagTag.put(formerTag, empty);
			if (!formerTag.equals("<s>")) {
				this.tags.add(formerTag);
			}
		}
		
		if (!this.countTagTag.get(formerTag).containsKey(latterTag)) {
			this.countTagTag.get(formerTag).put(latterTag, 0);
		}
		
		this.countTagTag.get(formerTag).put(latterTag, countTagTag.get(formerTag).get(latterTag)+1);
	}
	
	public void saveModel(String path) {
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(countWordTag);
			out.writeObject(countTagTag);
			out.writeObject(sumOfWords);
			out.writeObject(sumOfTags);
			out.writeObject(tags);
			out.writeObject(nWords);
			out.close();
			fileOut.close();
			System.out.println("Saved model in file " + path);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public void loadModel(String file) throws ClassNotFoundException {
		try {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			this.countWordTag = (Map<String,Map<String,Integer> >) in.readObject();
			this.countTagTag = (Map<String,Map<String,Integer> >) in.readObject();
			this.sumOfWords = (Map<String,Integer>) in.readObject();
			this.sumOfTags = (Map<String,Integer>) in.readObject();
			this.tags = (Vector<String>) in.readObject();
			this.nWords = (int) in.readObject();
			in.close();
			fileIn.close();
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	//Viterbi algorithm
	public String tag(String phrase) {
		phrase = phrase.toLowerCase();
		StringTokenizer st = new StringTokenizer(phrase);
		int phraseSize = st.countTokens();
		int numberOfTags = this.tags.size();
		int[][] track = new int[numberOfTags][phraseSize];
		double[][] scores = new double[numberOfTags][phraseSize];
		String formerTag = "<s>";
		String word = st.nextElement().toString();
		for (int i=0; i<numberOfTags; i++) {
			String thisTag = tags.get(i);
			double logProbWord = -Math.log(getProbWordTag(word,thisTag));
			double logProbTag = -Math.log(getProbTagTag(formerTag,thisTag));
			scores[i][0] = logProbWord + logProbTag;
		}
		
		int indexTrack = 1;
		while(st.hasMoreElements()) {
			word = st.nextElement().toString();
			for (int nTag = 0; nTag < numberOfTags; nTag++) {
				String thisTag = tags.get(nTag);
				int minInd = 0;
				double min = 100000;
				for (int fTag = 0; fTag < numberOfTags; fTag++) {
					formerTag = tags.get(fTag);
					double prob = -Math.log(getProbWordTag(word,thisTag)) - 
							Math.log(getProbTagTag(formerTag,thisTag));
					prob += scores[fTag][indexTrack-1];
					if (prob < min) {
						minInd = fTag;
						min = prob;
					}
				}
				scores[nTag][indexTrack] = min;
				track[nTag][indexTrack-1] = minInd;
			}
			indexTrack++;
		}
		String thisTag = "</s>";
		int minInd = 0;
		double min = 100000;
		for (int fTag=0; fTag < numberOfTags; fTag++) {
			formerTag = tags.get(fTag);
			double prob = scores[fTag][indexTrack-1] -
					Math.log(getProbTagTag(formerTag,thisTag));
			if (prob < min) {
				minInd = fTag;
				min = prob;
			}
		}
		
		//Backtracking
		
		String returnTags = tags.get(minInd);
		for (int i=phraseSize-2; i>=0; i--) {
			returnTags = tags.get(track[minInd][i]) + " " + returnTags;
			minInd = track[minInd][i];
		}
		return returnTags;
	}
	
	protected void saveSumOfWords() {
		for (Map.Entry<String, Map<String,Integer> > entry : countWordTag.entrySet()) {
		    String word = entry.getKey();
		    Collection<Integer> tagSet = entry.getValue().values();
		    int totalSum = 0;
		    for (Integer i: tagSet) {
		    	totalSum += i.intValue();
		    }
		    sumOfWords.put(word, totalSum);
		}
	}

	protected void saveSumOfTags() {
		for (Map.Entry<String, Map<String,Integer> > entry : countTagTag.entrySet()) {
		    String tag = entry.getKey();
		    Collection<Integer> tagSet = entry.getValue().values();
		    int totalSum = 0;
		    for (Integer i: tagSet) {
		    	totalSum += i.intValue();
		    }
		    sumOfTags.put(tag, totalSum);
		}
	}
	
	protected double getProbWordTag(String word, String tag) {
		double lambda = .85;
		double prob = 0;
		if (countWordTag.containsKey(word)) {
			if (countWordTag.get(word).containsKey(tag)) {
				prob = ((double) countWordTag.get(word).get(tag))/
						((double) sumOfWords.get(word));
			}
		}
		
		prob = lambda*prob + (1-lambda)*(((double) sumOfTags.get(tag))/((double) nWords));
		return prob;
	}

	protected double getProbTagTag(String formerTag, String latterTag) {
		double prob = .001;
		if (countTagTag.get(formerTag).containsKey(latterTag)) {
			prob = ((double) countTagTag.get(formerTag).get(latterTag))/
					((double) sumOfTags.get(formerTag));
		}
		return prob;
	}
	
	
}
