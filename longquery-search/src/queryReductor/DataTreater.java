package queryReductor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import queryReductor.PosTagger;

/*
 * 	"idf"            "mean_tf"        "query_place"    "wdt"            "nn"            
 [6] "jj"             "in"             "hvz"            "cc"             "vb"            
[11] "np"             "vbn"            "ben"            "ber"            "rb"            
[16] "cs"             "wps"            "hv"             "ap"             "to"            
[21] "dt"             "md"             "former_<s>"     "former_jj"      "former_in"     
[26] "former_at"      "former_hvz"     "former_cc"      "former_nns"     "former_virgula"
[31] "former_doz"     "former_ber"     "former_wps"     "former_hv"      "former_np"     
[36] "former_to"      "former_."       "former_md"      "next_nn"        "next_at"       
[41] "next_."         "next_vbn"       "next_ben"       "next_pp$"       "next_wps"      
[46] "next_ap"
 */

public class DataTreater {
	
	private long docCount;
	private Vector<Vector<Double> > data;
//	private PosTagger pos;
	private String[] features;
	private double maxIdf;
	private double maxTf;
	private double maxPos;
	private double minPos;
	
	public DataTreater() throws ClassNotFoundException {
		this.data = new Vector<Vector<Double> >();
    	features = new String[]{"wdt","nn",
    		 		"jj", "in", "hvz","cc", "vb",
    		 		"np", "vbn","ben","ber","rb",
    		 		"cs", "wps","hv", "ap", "to",
    		 		"dt", "md", "former_<s>","former_jj","former_in", 
    		 		"former_at","former_hvz","former_cc","former_nns","former_virgula",
    		 		"former_doz","former_ber","former_wps","former_hv","former_np",
    		 		"former_to","former_.","former_md","next_nn","next_at",
    		 		"next_.","next_vbn","next_ben","next_pp$","next_wps",
    				"next_ap"};
	}
	
	public void buildData(String path, IndexReader reader) throws IOException, ClassNotFoundException {
		File folder = new File(path);
		this.docCount = reader.getDocCount("contents");
        File[] files = folder.listFiles();
        String fileName = "";
        PosTagger pos = new PosTagger();
    	pos.loadModel("/home/murilo/Documentos/rm/project/data/POSModel.ser");
        long docCount = reader.getDocCount("contents");
        
        this.maxIdf = 0;
        this.maxPos = 0;
        this.minPos = 999999;
        this.maxTf = 0;
        
        for (int i=0; i<files.length;i++) {
        	fileName = files[i].getName();
        	System.out.println(fileName);
        	BufferedReader rdr = new BufferedReader(new FileReader(path + fileName));
        	String line = rdr.readLine();
//        	int waitingTitle = 1;
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
//        			System.out.println("Title: " + title + 
//        					" Description: " + desc);
        			desc = desc.replaceAll("\\((.*?)\\)", "");//\\\"
        			if (desc.endsWith(".")) {
        				desc = desc.replaceAll("[\\.]", "");
        				desc += ".";
        			}
        			desc = desc.replaceAll("[\\.?!,]", " $0 ");
        			title = title.toLowerCase();
        			desc = desc.toLowerCase();
        			desc = desc.replaceAll("( )\\1+", " ");
        			String tags = pos.tag(desc);
        			System.out.println(tags);
//        			StringTokenizer tokens = new StringTokenizer(desc);
//        			StringTokenizer posTags = new StringTokenizer(tags);
        			String[] tokens = desc.split(" ");
        			String[] posTags = tags.split(" ");
//        			System.out.println("tokens " + tokens.countTokens() + " tags " + posTags.countTokens());
        			String lastTag = "<s>";
        			//"idf,total_tf,query_place,pos,last_pos,next_pos"
        			int place = 0;
        			int phraseSize = tokens.length;//tokens.countTokens();
//        			while (tokens.hasMoreElements()) {
        			for (int ii=0; ii<phraseSize; ii++) {
        				place++;
//        				System.out.println(tokens[ii]);
        				String tok = tokens[ii];//tokens.nextElement().toString();
        				String tg = posTags[ii];//posTags.nextElement().toString();
        				tok = tok.trim();
        				tg = tg.trim();
//        				tg = tg.replaceAll("[$]", "");
        				if ((!tg.equals(".")) && (!tg.equals(",")) &&
        						(!tg.equals("*")) && (!tg.equals(":"))) {
//        					writer.write("\n");
            				Vector<Double> vec = new Vector<Double>();
            				
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
//            		        writer.write(String.valueOf(termsPerDoc)+",");
            		        vec.add(termsPerDoc);
            		        
            		        if (termsPerDoc > this.maxTf)	this.maxTf = termsPerDoc;
            		        
            		        double queryPlace = ((double) place)/((double) phraseSize);
//            				writer.write(String.valueOf(queryPlace)+",");
            				vec.add(queryPlace);
            				
            				if (queryPlace > this.maxPos)	this.maxPos = queryPlace;
            				if (queryPlace < this.minPos)	this.minPos = queryPlace;
       
            				
            				if (lastTag.equals(",")) {
            					lastTag = "virgula";
            				}
            				
            				String nextTag;
            				if (ii == (phraseSize-1)) {
            					nextTag = "</s>";
            				} else {
            					nextTag = posTags[ii+1];
            				}
            				
            				if (nextTag.equals(",")) {
            					nextTag = "virgula";
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
            				
//            				writer.write(tg+",");
//            				writer.write(lastTag+",");
//            				writer.write(nextTag+",");
            				
            				double getOut = 1;
            				if (title.contains(tok)) {
            					getOut = 0;
            				}
            				vec.add(getOut);
//            				writer.write(String.valueOf(getOut)+"\n");
            				data.add(vec);
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
			out.close();
			fileOut.close();
			System.out.println("Saved model in file " + path);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadData(String file) throws ClassNotFoundException {
		try {
			FileInputStream fileIn = new FileInputStream(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			this.data = (Vector<Vector<Double> >) in.readObject();
//			out.writeDouble(maxIdf);
//			out.writeDouble(maxPos);
//			out.writeDouble(maxTf);
//			out.writeDouble(minPos);
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
	
//	public static void main(String[] args) throws IOException {
////		System.out.println("Hello world!");
////		buildData("/home/murilo/Documentos/rm/project/data/topics/train/");
//	}
//	
//	public void 
	
	public void buildCSV(String path, IndexReader reader) throws IOException, ClassNotFoundException {
		File folder = new File(path);
		this.docCount = reader.getDocCount("contents");
        File[] files = folder.listFiles();
        String fileName = "";
        File dataFile = new File("/home/murilo/Documentos/rm/project/data/data.csv");
        BufferedWriter writer = null;
        PosTagger pos = new PosTagger();
    	pos.loadModel("/home/murilo/Documentos/rm/project/data/POSModel.ser");
        writer = new BufferedWriter(new FileWriter(dataFile));
        writer.write("idf,mean_tf,query_place,pos,last_pos,next_pos,get_out\n");
        long docCount = reader.getDocCount("contents");
        for (int i=0; i<files.length;i++) {
        	fileName = files[i].getName();
        	System.out.println(fileName);
        	BufferedReader rdr = new BufferedReader(new FileReader(path + fileName));
        	String line = rdr.readLine();
//        	int waitingTitle = 1;
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
//        			System.out.println("Title: " + title + 
//        					" Description: " + desc);
        			desc = desc.replaceAll("\\((.*?)\\)", "");//\\\"
        			if (desc.endsWith(".")) {
        				desc = desc.replaceAll("[\\.]", "");
        				desc += ".";
        			}
        			desc = desc.replaceAll("[\\.?!,]", " $0 ");
        			title = title.toLowerCase();
        			desc = desc.toLowerCase();
        			desc = desc.replaceAll("( )\\1+", " ");
        			String tags = pos.tag(desc);
        			System.out.println(tags);
//        			StringTokenizer tokens = new StringTokenizer(desc);
//        			StringTokenizer posTags = new StringTokenizer(tags);
        			String[] tokens = desc.split(" ");
        			String[] posTags = tags.split(" ");
//        			System.out.println("tokens " + tokens.countTokens() + " tags " + posTags.countTokens());
        			String lastTag = "<s>";
        			//"idf,total_tf,query_place,pos,last_pos,next_pos"
        			int place = 0;
        			int phraseSize = tokens.length;//tokens.countTokens();
//        			while (tokens.hasMoreElements()) {
        			for (int ii=0; ii<phraseSize; ii++) {
        				place++;
//        				System.out.println(tokens[ii]);
        				String tok = tokens[ii];//tokens.nextElement().toString();
        				String tg = posTags[ii];//posTags.nextElement().toString();
        				tok = tok.trim();
        				tg = tg.trim();
//        				tg = tg.replaceAll("[$]", "");
        				if ((!tg.equals(".")) && (!tg.equals(",")) &&
        						(!tg.equals("*")) && (!tg.equals(":"))) {
//        					writer.write("\n");
            				
            				
            				Term t = new Term("contents",tok);
            		        int df = reader.docFreq(t);
            		        double idf = ((double) df)/((double) docCount);
            		        writer.write(String.valueOf(idf)+",");
            		        
            		        long totalTermFreq = reader.totalTermFreq(t);
            		        double termsPerDoc = ((double) totalTermFreq)/((double) df);
            		        if (df == 0) {
            		        	termsPerDoc = 0.;
            		        }
            		        writer.write(String.valueOf(termsPerDoc)+",");
            		        
            		        double queryPlace = ((double) place)/((double) phraseSize);
            				writer.write(String.valueOf(queryPlace)+",");
            		        
            				writer.write(tg+",");
            				
            				if (lastTag.equals(",")) {
            					lastTag = "virgula";
            				}
            				
            				writer.write(lastTag+",");
            				
            				String nextTag;
            				if (ii == (phraseSize-1)) {
            					nextTag = "</s>";
            				} else {
            					nextTag = posTags[ii+1];
            				}
            				
            				if (nextTag.equals(",")) {
            					nextTag = "virgula";
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
//        	String content = readFile(path + fileName, StandardCharsets.UTF_8);
//        	int a = content.indexOf("<title>");
        	
        	rdr.close();
        }
        writer.close();
//        Term t = new Term("contents","children");
//        int ch = reader.docFreq(t);
//        System.out.println("Total doc freq of 'children': " + ch);
//        
//        long dictSize = reader.getSumTotalTermFreq("contents");
//        System.out.println("Dictionary size: " + dictSize);
//        
//        long totalTermFreq = reader.totalTermFreq(t);
//        System.out.println("Total term frequency of 'children': " + 
//        		totalTermFreq);
//        
//        long docCount = reader.getDocCount("contents");
//        System.out.println("Total doc count: " + docCount);
//        
//        long docCount2 = reader.getDocCount("path");
//        System.out.println("Total doc count in 'path': " + docCount2);
        
//        reader.
	}
}
