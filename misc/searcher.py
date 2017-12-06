import sys,os,codecs
import lucene
import gensim
import numpy as np
import pandas as pd
import nltk
import re
import string

from java.nio.file import Paths
from org.apache.lucene.analysis.standard import StandardAnalyzer
from org.apache.lucene.index import DirectoryReader
from org.apache.lucene.index import IndexReader
from org.apache.lucene.queryparser.classic import QueryParser
from org.apache.lucene.store import SimpleFSDirectory
from org.apache.lucene.search import IndexSearcher
from org.apache.lucene.store import FSDirectory
from org.apache.lucene.index import Term

relevancePath = "/home/murilo/Documentos/rm/project/data/relevance_judgments/test/test2"
relevance = {}

for file in os.listdir(relevancePath):
	with open(relevancePath + "/" + file) as f:
		for line in f:
			fields = line.split(' ')
			nQuery = int(fields[0])
			doc = fields[2]
			rel = int(fields[3])
			if nQuery not in relevance:
				relevance[nQuery] = {}
			relevance[nQuery][doc] = rel


lucene.initVM(vmargs=['-Djava.awt.headless=true'])
index = "/home/murilo/Documentos/rm/project/data/IndexFiles.index"
directory = SimpleFSDirectory(Paths.get(index))
searcher = IndexSearcher(DirectoryReader.open(directory))
analyzer = StandardAnalyzer()
reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)))

good_tags = ['NN','NNS','NNP','JJ']

model = gensim.models.Word2Vec.load("/home/murilo/Documentos/rm/project/data/w2v_modelcbow")
queriesFile = "/home/murilo/Documentos/rm/project/data/topics/test/with_relevance/shortTestQueries"
qf = open(queriesFile,'r')
pat10 = []
rat10 = []
mean_ap = 0.
for i in xrange(50):
	nq = int(qf.readline())
	query = qf.readline()
	query = query.replace("/"," or ")
	if query[len(query)-1] == '.':
		query = query.replace(".","")
		query += '.'
	else:
		query = query.replace(".","")
	#user_query = sys.argv[1]
	query = query.replace("\n","")
	query = re.sub("[\\.?!,;:]","",query)
	query = query.lower()
	query_terms = query.split(" ")
	idfList = []
	tokens = nltk.word_tokenize(query)
	tags = nltk.pos_tag(tokens)
	#print tags
	position = 0
	#print str(len(tokens)) + " BATMAN " + str(len(tags))
	#print query_terms
	#print tags
	for t in tokens:
		term = Term("contents",t)
		docf = reader.docFreq(term)
		idf = 0
		if (docf > 0 and tags[position][1] in good_tags):
			idf = np.log(reader.getDocCount("contents")/docf)
		idfList.append([idf,t])
		position += 1
	#idfList.sort(reverse=True)
	thresh_idf = sorted(idfList,reverse=True)[0][0]*.3
	important_words = []
	for t in idfList:
		if t[0] >= thresh_idf:
			important_words.append(t[1])
	print important_words
	n_terms = int(sys.argv[1])
	new_qt = []
	j = 0
	for term in tokens:
		new = term + ' '
		this_idf = idfList[j][0]
		print term  + " idf: " + str(this_idf)
		if (term in model.wv.vocab and term in important_words):
			mostSim = model.wv.most_similar(term)[1:10]
			term_sims = []
			for i in xrange(len(mostSim)):
				term_to_add = mostSim[i][0]
				tta_idf = reader.docFreq(Term("contents",term_to_add))
				if tta_idf > 0:
					tta_idf = np.log(reader.getDocCount("contents")/tta_idf)
				new_measure = 10
				if this_idf > 0:
					new_measure = mostSim[i][1]*(tta_idf/this_idf)
				term_sims.append([new_measure, term_to_add])
			term_sims.sort(reverse=True)
			for i in xrange(n_terms):
				new = new + ' ' + term_sims[i][1]
			new += ' '
		new_qt.append(new)
		j += 1

	new_query = ''
	for i in xrange(len(new_qt)):
		new_query += new_qt[i]
	
	number_of_relevants = 0
	for k in relevance[nq].keys():
#print relevance[nq][k]
		number_of_relevants += relevance[nq][k]

#	new_query = ''
#	for i in xrange(len(important_words)):
#		new_query += important_words[i] + ' '
		
	print "New query: ",new_query
	query = QueryParser("contents",analyzer).parse(new_query)
	scoreDocs = searcher.search(query,10).scoreDocs
	total_rel = 0
	ap = 0.
	nn = 0
	for d in scoreDocs:
		nn += 1
		doc = searcher.doc(d.doc)
		docname = doc.get("name")
		rel = 0
		if docname.strip() in relevance[nq]:
			rel = relevance[nq][docname.strip()]
		total_rel += rel
		if (rel == 1):
			ap += float(total_rel)/float(nn)
		print docname + " " + str(rel)
	if (ap > 0):
		ap = ap/float(total_rel)
	pat10.append(float(total_rel)/10)
	rat10.append(float(total_rel)/number_of_relevants)
	mean_ap += ap
	print "P@10: " + str(float(total_rel)/10.) + " R@10: " +  str(float(total_rel)/float(number_of_relevants))


mean_ap = mean_ap/50.
print "Mean Average Precision: " + str(mean_ap) + "\nMean P@10: " + str(sum(pat10)/len(pat10)) + "\nMean R@10: " + str(sum(rat10)/len(rat10))
