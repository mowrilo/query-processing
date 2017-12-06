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

def decodeASCII(text):
    text = text.encode("utf-8")
    text = text.decode("utf-8")
    textASCII = text.encode("ascii","ignore")
    return textASCII

def cleanhtml(raw_html):
    cleanr = re.compile('<.*?>')
    cleantext = re.sub(cleanr, '', raw_html)
    return cleantext

path = "../data/docs"
os.chdir(path)
files = os.listdir(".")
#files = files[0:10000]
#os.chdir("./docs")
#file = "12-0.txt"
#sentences = gensim.models.word2vec.LineSentence(file)
#model = gensim.models.Word2Vec(sentences,size=10,window=8,workers=4,sg=1)
#sentences = gensim.models.word2vec.LineSentence("./docs/doc2.txt")
#model.train(sentences)
sentences = []
first_files = files[0:10000]
ndocs = 0
for fl in first_files:
    ndocs += 1
    print "\nNdocs: " + str(ndocs)
    print "Document " + fl

    f = open(fl,'r')
    raw = f.read()
    raw = cleanhtml(raw)
    raw = raw.translate(None,string.punctuation)
    tok = nltk.word_tokenize(raw)
    single = [i for i,x in enumerate(tok) if len(x)==1]
    for i in sorted(single,reverse=True):
        del tok[i]
    tok = [w.lower() for w in tok]
    sentences.append(tok)
    
model = gensim.models.Word2Vec(sentences,min_count=5,size=200,sg=0,workers=4)

ndocs = 10000
sentences = []
for fl in files[10000:]:
    ndocs += 1
    print "\tNdocs: " + str(ndocs)
    f = open(fl,'r')
    raw = f.read()
    raw = cleanhtml(raw)
    raw = raw.translate(None,string.punctuation)
    tok = nltk.word_tokenize(raw)
    
    single = [i for i,x in enumerate(tok) if len(x)==1]

    for i in sorted(single,reverse=True):
        del tok[i]
    tok = [w.lower() for w in tok]
    sentences.append(tok)
    if ((ndocs%10000 == 0) or (ndocs == len(files)-1)):
        model.train(sentences,total_examples=len(sentences),epochs=model.iter)
        sentences = []
    
    print "Document " + fl
    #sentences.append(tok)#gensim.models.word2vec.LineSentence(fl)

#model = gensim.models.Word2Vec(sentences,min_count=1,size=200,sg=1,workers=4)
   
#model.train(sentences,total_examples = len(sentences), epochs=model.iter)
    
model.save("../w2v_modelcbow")

#sentences = gensim.models.word2vec.PathLineSentences(dirr)


#lucene.initVM(vmargs=['-Djava.awt.headless=true'])
#index = "IndexFiles.index"
#reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)))

#query = "system"

#mostSim = model.wv.most_similar(query)
#print mostSim
