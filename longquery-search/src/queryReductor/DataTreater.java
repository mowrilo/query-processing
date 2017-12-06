package queryReductor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.Map;
//import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
//import java.util.Comparator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import queryReductor.PosTagger;

/*
 * 	"idf"            "mean_tf"        "query_place"    "wdt"            "nn"            
 [6] "jj"             "in"             "hvz"            "cc"             "vb"            
[11] "np"             "vbn"            "ben"            "ber"            "rb"            
[16] "cs"             "wps"            "hv"             "ap"             "to"            
[21] "dt"             "md"             "former_<s>"     "former_jj"      "former_in"     
[26] "former_at"      "former_hvz"     "former_cc"      "former_nns"     "former_comma"
[31] "former_doz"     "former_ber"     "former_wps"     "former_hv"      "former_np"     
[36] "former_to"      "former_."       "former_md"      "next_nn"        "next_at"       
[41] "next_."         "next_vbn"       "next_ben"       "next_pp$"       "next_wps"      
[46] "next_ap"
 */

public class DataTreater {
	
	private ArrayList<ArrayList<Double> > data;
	private PosTagger pos;
	private String[] features;
	private double maxIdf;
	private double maxTf;
	private double maxPos;
	private double minPos;
	private double minLen;
	private double maxLen;
	
	public DataTreater(String posFile) throws ClassNotFoundException {
		pos = new PosTagger();
    	pos.loadModel(posFile);
		this.data = new ArrayList<ArrayList<Double> >();
    	features = new String[] {"nn","former_<s>","former_nns","next_."};    	
	}
	
	public void buildData(String path, IndexReader reader) throws IOException, ClassNotFoundException {
		File folder = new File(path);
		long docCount = reader.getDocCount("contents");
        File[] files = folder.listFiles();
        String fileName = "";
        
        this.maxIdf = 0;
        this.maxPos = 0;
        this.minPos = 999999;
        this.maxTf = 0;
        this.maxLen = 0;
        this.minLen = 999999;
        
        for (int i=0; i<files.length;i++) {
        	fileName = files[i].getName();
        	BufferedReader rdr = new BufferedReader(new FileReader(path + fileName));
        	String line = rdr.readLine();
        	while (line != null) {
        		if (line.startsWith("<title>")) {
        			line = line.replaceAll("<title>", "");
        			String title = line;
        			line = rdr.readLine();
        			line = rdr.readLine();
        			line = rdr.readLine();
        			String desc = "";
        			while (!line.trim().isEmpty()) {
        				desc += (line + " ");
        				line = rdr.readLine();
        			}
        			desc = desc.replaceAll("/", " or ");
            		if (desc.endsWith(".")) {
        				desc = desc.replaceAll("[\\.]", "");
        				desc += ".";
        			} else {
        				desc = desc.replaceAll("[\\.]", "");
        			}
            		String regex = "(?<=[\\d])(,)(?=[\\d])";
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(desc);
                    desc = m.replaceAll("");
                    
                    desc = desc.replaceAll("[\\.?!,\")(:;]", " $0 ");
            		desc = desc.replaceAll("--", " -- ");
            		desc = desc.toLowerCase();
        			title = title.toLowerCase();
        			desc = desc.replaceAll("( )\\1+", " ");
        			String tags = this.pos.tag(desc);
        			String[] tokens = desc.split(" ");
        			String[] posTags = tags.split(" ");
        			String lastTag = "<s>";
        			int place = 0;
        			int phraseSize = tokens.length;
        			for (int ii=0; ii<phraseSize; ii++) {
        				place++;
        				String tok = tokens[ii];
        				String tg = posTags[ii];
        				tok = tok.trim();
        				tg = tg.trim();
        				if ((!tok.equals(".")) && (!tok.equals(",")) &&
        						(!tok.equals(":")) && (!tok.equals("?")) && 
        						(!tok.equals("!")) && (!tok.equals("\"")) &&
        						(!tok.equals("--")) && (!tok.equals(":")) &&
        						(!tok.equals(";")) && (!tok.equals(")")) &&
        						(!tok.equals("("))) {
            				ArrayList<Double> vec = new ArrayList<Double>();
            				
            				Term t = new Term("contents",tok);
            		        int df = reader.docFreq(t);
            		        double idf = 0;
            		        if (df > 0)	idf = Math.log(((double) docCount)/((double) df));
            		        vec.add(idf);
            		        
            		        if (idf > this.maxIdf)	this.maxIdf = idf;
            		        
            		        long totalTermFreq = reader.totalTermFreq(t);
            		        double termsPerDoc = ((double) totalTermFreq)/((double) df);
            		        if (df == 0) {
            		        	termsPerDoc = 0.;
            		        }
            		        vec.add(termsPerDoc);
            		        
            		        if (termsPerDoc > this.maxTf)	this.maxTf = termsPerDoc;
            		        
            		        double queryPlace = ((double) place)/((double) phraseSize);
//            				vec.add(queryPlace);
            				
            				if (queryPlace > this.maxPos)	this.maxPos = queryPlace;
            				if (queryPlace < this.minPos)	this.minPos = queryPlace;
       
            				double queryLength = (double) phraseSize;
            				vec.add(queryLength);
            				
            				if (queryLength > this.maxLen)	this.maxLen = queryLength;
            				if (queryLength < this.minLen)	this.minLen = queryLength;
            				
            				if (lastTag.equals(",")) {
            					lastTag = "comma";
            				}
            				
            				String nextTag;
            				if (ii == (phraseSize-1)) {
            					nextTag = "</s>";
            				} else {
            					nextTag = posTags[ii+1];
            				}
            				
            				if (nextTag.equals(",")) {
            					nextTag = "comma";
            				}            				
            				
            				double getOut = 1;
            				if (title.contains(tok)) {
            					getOut = 0;
            				}
            				for (String feat: features) {
            					double isThis = 0;
            					if (tg.equals(feat)) {
            						isThis = 1;
            					}
            					else if (feat.startsWith("former_")) {
            						String[] featSplit = feat.split("_");
            						if (lastTag.equals(featSplit[1])) {
            							isThis = 1;
            						}
            					}
            					else if (feat.startsWith("next_")) {
            						String[] featSplit = feat.split("_");
            						if (nextTag.equals(featSplit[1])) {
            							isThis = 1;
            						}
            					}
            					vec.add(isThis);
            				}
            				
            				vec.add(getOut);
            				this.data.add(vec);
        				}
        				lastTag = tg;
        			}
        		}
        		line = rdr.readLine();
        	}        	
        	rdr.close();
        }
	}
	
	public void saveData(String path) {
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(data);
			out.writeDouble(maxIdf);
			out.writeDouble(maxPos);
			out.writeDouble(maxTf);
			out.writeDouble(minPos);
			out.writeDouble(maxLen);
			out.writeDouble(minLen);
			out.close();
			fileOut.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadData(String file) throws ClassNotFoundException {
		try {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			this.data = (ArrayList<ArrayList<Double> >) in.readObject();
			this.maxIdf = in.readDouble();
			this.maxPos = in.readDouble();
			this.maxTf = in.readDouble();
			this.minPos = in.readDouble();
			this.maxLen = in.readDouble();
			this.minLen = in.readDouble();
			in.close();
			fileIn.close();
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<ArrayList<Double> > analyzeSentence(String sentence, IndexReader reader) 
			throws IOException {
		long docCount = reader.getDocCount("contents");
		
		sentence = sentence.replaceAll("/", " or ");
		if (sentence.endsWith(".")) {
			sentence = sentence.replaceAll("[\\.]", "");
			sentence += ".";
		} else {
			sentence = sentence.replaceAll("[\\.]", "");
		}
		String regex = "(?<=[\\d])(,)(?=[\\d])";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(sentence);
        sentence = m.replaceAll("");
        
        sentence = sentence.replaceAll("[\\.?!,\")(:;]", " $0 ");
		sentence = sentence.replaceAll("--", " -- ");
		sentence = sentence.replaceAll("( )\\1+", " ");
		sentence = sentence.toLowerCase();
		
		String taggedSentence = this.pos.tag(sentence);
		ArrayList<ArrayList<Double> > dataMatrix = 
				new ArrayList<ArrayList<Double> >();
		
		String[] terms = sentence.split(" ");
		String[] tags = taggedSentence.split(" ");
		
		int sentSize = terms.length;
		String lastTag = "<s>";
		int place = 0;
		for (int i=0; i<sentSize; i++) {
			place++;
			String tok = terms[i];
			String tg = tags[i];
			tok = tok.trim();
			tg = tg.trim();
			if ((!tok.equals(".")) && (!tok.equals(",")) &&
					(!tok.equals(":")) && (!tok.equals("?")) && 
					(!tok.equals("!")) && (!tok.equals("\"")) &&
					(!tok.equals("--")) && (!tok.equals(":")) &&
					(!tok.equals(";")) && (!tok.equals(")")) &&
					(!tok.equals("("))) {
				ArrayList<Double> thisSample = new ArrayList<Double>();
				Term t = new Term("contents",tok);
		        int df = reader.docFreq(t);
		        double idf = 0;
		        if (df > 0)	idf = Math.log(((double) docCount)/((double) df));
		        idf = idf/maxIdf;
		        thisSample.add(idf);
		        
		        long totalTermFreq = reader.totalTermFreq(t);
		        double termsPerDoc = ((double) totalTermFreq)/((double) df);
		        if (df == 0) {
		        	termsPerDoc = 0.;
		        }
		        termsPerDoc = termsPerDoc/maxTf;
		        thisSample.add(termsPerDoc);
		        
		        double queryPlace = ((double) place)/((double) sentSize);
		        queryPlace = (queryPlace - minPos)/(maxPos-minPos);
		        
		        double queryLength = (double) sentSize;
		        queryLength = (queryLength - minLen)/(maxLen-minLen);
		        thisSample.add(queryLength);
				
				if (lastTag.equals(",")) {
					lastTag = "comma";
				}
				
				String nextTag;
				if (i == (sentSize-1)) {
					nextTag = "</s>";
				} else {
					nextTag = tags[i+1];
				}
				
				if (nextTag.equals(",")) {
					nextTag = "comma";
				}
				
				double mult = 1;
				for (String feat: features) {
					double isThis = 0;
					if (tg.equals(feat)) {
						isThis = 1;
						if (tg.equals("nn") || tg.equals("np")){
							isThis = isThis*mult;
						}
					}
					else if (feat.startsWith("former_")) {
						String[] featSplit = feat.split("_");
						if (lastTag.equals(featSplit[1])) {
							isThis = 1;
						}
					}
					else if (feat.startsWith("next_")) {
						String[] featSplit = feat.split("_");
						if (nextTag.equals(featSplit[1])) {
							isThis = 1;
						}
					}
					thisSample.add(isThis);
				}
				dataMatrix.add(thisSample);
			}
			lastTag = tg;
		}
		return dataMatrix;
	}
	
	public void buildCSV(String path, IndexReader reader, String destinationFile) 
			throws IOException, ClassNotFoundException {
		File folder = new File(path);
		long docCount = reader.getDocCount("contents");
        File[] files = folder.listFiles();
        String fileName = "";
	        File dataFile = new File(destinationFile);
	        BufferedWriter writer = null;
	        writer = new BufferedWriter(new FileWriter(dataFile));
	        writer.write("idf,mean_tf,query_place,pos,last_pos,next_pos,get_out\n");
        for (int i=0; i<files.length;i++) {
        	fileName = files[i].getName();
        	BufferedReader rdr = new BufferedReader(new FileReader(path + fileName));
        	String line = rdr.readLine();
        	while (line != null) {
        		if (line.startsWith("<title>")) {
        			line = line.replaceAll("<title>", "");
        			String title = line;
        			line = rdr.readLine();
        			line = rdr.readLine();
        			line = rdr.readLine();
        			String desc = "";
        			while (!line.trim().isEmpty()) {
        				desc += (line + " ");
        				line = rdr.readLine();
        			}
        			if (desc.endsWith(".")) {
        				desc = desc.replaceAll("[\\.]", "");
        				desc += ".";
        			}
        			desc = desc.replaceAll("[\\.?!,]", " $0 ");
        			title = title.toLowerCase();
        			desc = desc.toLowerCase();
        			desc = desc.replaceAll("( )\\1+", " ");
        			String tags = this.pos.tag(desc);
        			String[] tokens = desc.split(" ");
        			String[] posTags = tags.split(" ");
        			String lastTag = "<s>";
        			int place = 0;
        			int phraseSize = tokens.length;
        			for (int ii=0; ii<phraseSize; ii++) {
        				place++;
        				String tok = tokens[ii];
        				String tg = posTags[ii];
        				tok = tok.trim();
        				tg = tg.trim();
        				if ((!tok.equals(".")) && (!tok.equals(",")) &&
        						(!tok.equals(":")) && (!tok.equals("?")) && (!tok.equals("!"))) {
            				
            				
            				Term t = new Term("contents",tok);
            		        int df = reader.docFreq(t);
            		        double idf = 0;
            		        if (df > 0)	idf = Math.log(((double) docCount)/((double) df));
            		        writer.write(String.valueOf(idf)+",");
            		        
            		        long totalTermFreq = reader.totalTermFreq(t);
            		        double termsPerDoc = ((double) totalTermFreq)/((double) df);
            		        if (df == 0) {
            		        	termsPerDoc = 0.;
            		        }
            		        writer.write(String.valueOf(termsPerDoc)+",");
            		        
            		        double queryPlace = ((double) place)/((double) phraseSize);
            				writer.write(String.valueOf(queryPlace)+",");
            		        
            				double queryLength = (double) phraseSize;
            				writer.write(String.valueOf(queryLength)+",");
            				
            				writer.write(tg+",");
            				
            				if (lastTag.equals(",")) {
            					lastTag = "comma";
            				}
            				
            				writer.write(lastTag+",");
            				
            				String nextTag;
            				if (ii == (phraseSize-1)) {
            					nextTag = "</s>";
            				} else {
            					nextTag = posTags[ii+1];
            				}
            				
            				if (nextTag.equals(",")) {
            					nextTag = "comma";
            				}
            				
            				writer.write(nextTag+",");
            				
            				int getOut = 1;
            				if (title.contains(tok)) {
            					getOut = 0;
            				}
            				writer.write(String.valueOf(getOut)+"\n");
        				}
        				lastTag = tg;
        			}
        		}
        		line = rdr.readLine();
        	}
        	
        	rdr.close();
        }
        writer.close();
	}
}
