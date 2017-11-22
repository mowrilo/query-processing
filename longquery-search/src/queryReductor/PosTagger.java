/* Part-Of-Speech Tagger using a Markov Model
 * 			and Viterbi's algorithm
 * Author: Murilo V. F. Menezes
 */

package queryReductor;

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

public class PosTagger {

	private Map<String,Map<String,Integer> > countWordTag;
	private Map<String,Map<String,Integer> > countTagTag;
	
	public PosTagger() {
		countWordTag = new LinkedHashMap<String,Map<String,Integer> >();
		countTagTag = new LinkedHashMap<String,Map<String,Integer> >();
	};
	
	public void train(String corpusPath) {
//		"/home/murilo/Documentos/rm/project/data/postags/nlp-master/resources/corpora/brown"
		try {
			File folder = new File(corpusPath); 
	        File[] files = folder.listFiles();
	        String fileName = "";
	        for (int i=0; i<files.length;i++) {
	        	fileName = files[i].getName();
	        	System.out.println("Reading file " + fileName);
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
	        				String tag = parts[1];
	        				int titleTag = tag.indexOf("-");
	        				if (titleTag > 0) {
//	        					System.out.println(tag);
	        					tag = tag.substring(0, titleTag);
//	        					System.out.println(tag);
	        				}
//	        				System.out.println("Word: " + word + 
//	        						" Tag: " + tag + "\n");
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
		System.out.println("Incrementing count of  tag " + tag + 
				" for word " + word);
		
		if (!countWordTag.containsKey(word)) {
			Map<String,Integer> empty = new HashMap<String,Integer>();
			countWordTag.put(word, empty);
		}
		
		if (!countWordTag.get(word).containsKey(tag)) {
			countWordTag.get(word).put(tag, 0);
		}
		
		countWordTag.get(word).put(tag, countWordTag.get(word).get(tag)+1);
	}
	
	protected void addToTagMap(String formerTag, String latterTag) {
		System.out.println("Incrementing count of  tag " + latterTag + 
				" given tag " + formerTag);
		
		if (!countTagTag.containsKey(formerTag)) {
			Map<String,Integer> empty = new HashMap<String,Integer>();
			countTagTag.put(formerTag, empty);
		}
		
		if (!countTagTag.get(formerTag).containsKey(latterTag)) {
			countTagTag.get(formerTag).put(latterTag, 0);
		}
		
		countTagTag.get(formerTag).put(latterTag, countTagTag.get(formerTag).get(latterTag)+1);
	}
	
	public void saveModel() {
		
	}
	
	public String tag(String phrase) {
		return "";
	}
	
	
}
