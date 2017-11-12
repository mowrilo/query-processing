import sys,os,codecs
import lucene
import gensim
import numpy as np
import pandas as pd

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

path = "../data/docs"
os.chdir(path)
files = os.listdir(".")

#os.chdir("./docs")
#file = "12-0.txt"
#sentences = gensim.models.word2vec.LineSentence(file)
#model = gensim.models.Word2Vec(sentences,size=10,window=8,workers=4,sg=1)
#sentences = gensim.models.word2vec.LineSentence("./docs/doc2.txt")
#model.train(sentences)
sentences = []
first_files = files[0:10000]
for fl in first_files:
    f = codecs.open(fl,'r',encoding='utf-8')
    raw = f.read()
    tok = raw.split()
    sentences.append(tok)
    
model = gensim.models.Word2Vec(sentences,min_count=1,size=200,sg=1,workers=4)

ndocs = 10000
sentences = []
for fl in files[10000:]:
    ndocs += 1
    print "\tNdocs: " + str(ndocs)
    f = codecs.open(fl,'r',encoding='utf-8')
    raw = f.read()
    tok = raw.split()
    sentences.append(tok)
    if (ndocs%10000 == 0):
        model.train(sentences,total_examples=len(sentences),epochs=model.iter)
        sentences = []
    
    print "Document " + fl
    #sentences.append(tok)#gensim.models.word2vec.LineSentence(fl)

#model = gensim.models.Word2Vec(sentences,min_count=1,size=200,sg=1,workers=4)
   
#model.train(sentences,total_examples = len(sentences), epochs=model.iter)
    
model.save("../data/w2v_model")

#sentences = gensim.models.word2vec.PathLineSentences(dirr)


#lucene.initVM(vmargs=['-Djava.awt.headless=true'])
#index = "IndexFiles.index"
#reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)))

#query = "system"

#mostSim = model.wv.most_similar(query)
#print mostSim
