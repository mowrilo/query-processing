/* Part-Of-Speech Tagger using a Markov Model
 * 			and Viterbi's algorithm
 * Author: Murilo V. F. Menezes
 */

package queryReductor;

import java.util.StringTokenizer;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;

public class PosTagger {

	private Vector<Vector<Integer> > probTagTag;
	private Vector<Vector<Integer> > probWordTag;
	
	public PosTagger(){};
	
	public void train(String corpusPath) {
//		"/home/murilo/Documentos/rm/project/data/postags/nlp-master/resources/corpora/brown"
		try {
			File folder = new File(corpusPath); 
	        File[] files = folder.listFiles();
	        for (int i=0; i<files.length;i++) {
	        	String fileName = files[i].getName();
	        	System.out.println("Reading file " + fileName);
	        	fileName = corpusPath + fileName;
	        	BufferedReader rdr = new BufferedReader(new FileReader(fileName));
	        	String a;
	        	while((a = rdr.readLine()) != null) {
	        		if (a.length() > 5) {
//		        		System.out.println(batata + "\t" + a.length() + "\n" + a);
	        			StringTokenizer tokens = new StringTokenizer(a);
	        			while(tokens.hasMoreElements()) {
//	        				System.out.println(tokens.nextElement() + " ");
	        				
	        			}
	        		}
	        	}
	        	rdr.close();
	        }
		} catch(IOException e) {
			
		}
		
		
	}
	
	public void saveModel() {
		
	}
	
	public String tag(String phrase) {
		return "";
	}
	
	
}
