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
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Math.*;

public class PosTagger {

	private Map<String,Map<String,Integer> > countWordTag;
	private Map<String,Map<String,Integer> > countTagTag;
	private Map<String,Integer> sumOfWords;
	private Map<String,Integer> sumOfTags;
	private Vector<String> tags;
//	private int numberOfTags;
	
	public PosTagger() {
		this.countWordTag = new LinkedHashMap<String,Map<String,Integer> >();
		this.countTagTag = new LinkedHashMap<String,Map<String,Integer> >();
		this.sumOfTags = new HashMap<String,Integer>();
		this.sumOfWords = new HashMap<String,Integer>();
//		this.numberOfTags = 0;
		this.tags = new Vector<String>(0);
	};
	
	public void train(String corpusPath) {
//		"/home/murilo/Documentos/rm/project/data/postags/nlp-master/resources/corpora/brown"
		try {
			File folder = new File(corpusPath); 
	        File[] files = folder.listFiles();
	        String fileName = "";
	        for (int i=0; i<files.length;i++) {
	        	fileName = files[i].getName();
//	        	System.out.println("Reading file " + fileName);
	        	fileName = corpusPath + fileName;
	        	BufferedReader rdr = new BufferedReader(new FileReader(fileName));
	        	String a;
	        	while((a = rdr.readLine()) != null) {
	        		String lastTag = "<s>";
	        		if (a.length() > 5) {
//		        		System.out.println(batata + "\t" + a.length() + "\n" + a);
	        			StringTokenizer tokens = new StringTokenizer(a);
	        			while(tokens.hasMoreElements()) {
	        				String nextToken = tokens.nextElement().toString();
	        				String[] parts = nextToken.split("/");
	        				String word = parts[0];
	        				for (int k=1; k<(parts.length-1); k++) {
	        					word+="/";
	        					word+=parts[k];
	        				}
	        				String tag = parts[parts.length-1];
	        				int titleTag = tag.indexOf("-");
	        				if (titleTag > 0) {
//	        					System.out.println(tag);
	        					tag = tag.substring(0, titleTag);
//	        					System.out.println(tag);
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
	        }
//	        System.out.println(fileName);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	protected void addToWordMap(String word, String tag) {
//		System.out.println("Incrementing count of tag " + tag + 
//				" for word " + word);
		
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
//		System.out.println("Incrementing count of tag " + latterTag + 
//				" given tag " + formerTag);
		
		if (!this.countTagTag.containsKey(formerTag)) {
			Map<String,Integer> empty = new HashMap<String,Integer>();
			this.countTagTag.put(formerTag, empty);
//			this.numberOfTags++;
			if (!formerTag.equals("<s>")) {
				this.tags.add(formerTag);
				System.out.println("Number of tags so far: " + this.tags.size() +
						". Added: " + formerTag);
			}
		}
		
		if (!this.countTagTag.get(formerTag).containsKey(latterTag)) {
			this.countTagTag.get(formerTag).put(latterTag, 0);
		}
		
		this.countTagTag.get(formerTag).put(latterTag, countTagTag.get(formerTag).get(latterTag)+1);
	}
	
	public void saveModel() {
		
	}
	
	public void loadModel(String file) {
		
	}
	
	//Viterbi algorithm
	public String tag(String phrase) {
		phrase = phrase.replaceAll("[\\.,()\\\"-:;?!]", " $0 ");
		StringTokenizer st = new StringTokenizer(phrase);
		int phraseSize = st.countTokens();
		int numberOfTags = this.tags.size();
		int[][] track = new int[phraseSize][numberOfTags];
		double[][] scores = new double[phraseSize][numberOfTags];
		int nWord = 0;
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
		
		String returnTags = "";
		for (int i=phraseSize-1; i>=0; i--) {
			
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
		
		prob = lambda*prob + (1-lambda)*(1/tags.size());
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
