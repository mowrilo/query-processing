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


lucene.initVM(vmargs=['-Djava.awt.headless=true'])
index = "/home/murilo/Documentos/rm/project/data/IndexFiles.index"
directory = SimpleFSDirectory(Paths.get(index))
searcher = IndexSearcher(DirectoryReader.open(directory))
analyzer = StandardAnalyzer()
reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)))

model = gensim.models.Word2Vec.load("/home/murilo/Documentos/rm/project/data/w2v_modelTest")

user_query = sys.argv[1]
mostSim = model.wv.most_similar(user_query)
n_terms = int(sys.argv[2])
new_query = user_query
for i in xrange(n_terms):
    new_query = new_query + ' ' + mostSim[i][0]

print "New query: ",new_query
query = QueryParser("contents",analyzer).parse(new_query)
scoreDocs = searcher.search(query,10).scoreDocs

for d in scoreDocs:
    doc = searcher.doc(d.doc)
    print doc.get("name")

