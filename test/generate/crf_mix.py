
import struct
import factor_graph_pb2
import random

NVAR = 1000000
NQVAR = 1000000

fo = open("crf_mix/graph.variables.pb", "wb")
for i in range(0,NVAR):
	v = factor_graph_pb2.Variable()
	v.id = i
	v.initialValue = 0
	if random.random() < 0.8:
		v.initialValue = 1 
	v.dataType = 0
	v.isEvidence = True
	v.cardinality = 1
	size = v.ByteSize()
	fo.write(struct.pack("i", size + 3))
	fo.write(v.SerializeToString())
	#break
for i in range(NVAR, NVAR+NQVAR):
	v = factor_graph_pb2.Variable()
	v.id = i
	v.initialValue = 0
	v.dataType = 0
	v.isEvidence = False
	v.cardinality = 1
	size = v.ByteSize()
	fo.write(struct.pack("i", size + 3))
	fo.write(v.SerializeToString())
fo.close()

fo = open("crf_mix/graph.factors.pb", "wb")
for i in range(0,NVAR):
	f = factor_graph_pb2.Factor()
	f.id = i
	f.weightId = 0
	f.factorFunction = 0
	fo.write(struct.pack("i", f.ByteSize()+3))
	fo.write(f.SerializeToString())
for i in range(NVAR, NVAR+NQVAR):
	f = factor_graph_pb2.Factor()
	f.id = i
	f.weightId = 0
	f.factorFunction = 0
	fo.write(struct.pack("i", f.ByteSize()+3))
	fo.write(f.SerializeToString())
for i in range(NVAR+NQVAR,NVAR+NQVAR+NVAR+NQVAR):
	f = factor_graph_pb2.Factor()
	f.id = i
	f.weightId = 1
	f.factorFunction = 0
	fo.write(struct.pack("i", f.ByteSize()+3))
	fo.write(f.SerializeToString())

fo.close()

fo = open("crf_mix/graph.edges.pb", "wb")
for i in range(0,NVAR):
	e = factor_graph_pb2.GraphEdge()
	e.variableId = i
	e.factorId = i
	e.position = 0
	e.isPositive = True
	fo.write(struct.pack("i", e.ByteSize()+3))
	fo.write(e.SerializeToString())
for i in range(NVAR, NVAR+NQVAR):
	e = factor_graph_pb2.GraphEdge()
	e.variableId = i
	e.factorId = i
	e.position = 0
	e.isPositive = True
	fo.write(struct.pack("i", e.ByteSize()+3))
	fo.write(e.SerializeToString())
for i in range(NVAR+NQVAR,NVAR+NQVAR+NVAR+NQVAR-1):
	start_id = i-NVAR-NQVAR
	end_id = start_id+1
	e = factor_graph_pb2.GraphEdge()
	e.variableId = start_id
	e.factorId = i
	e.position = 0
	e.isPositive = True
	fo.write(struct.pack("i", e.ByteSize()+3))
	fo.write(e.SerializeToString())
	e = factor_graph_pb2.GraphEdge()
	e.variableId = end_id
	e.factorId = i
	e.position = 1
	e.isPositive = True
	fo.write(struct.pack("i", e.ByteSize()+3))
	fo.write(e.SerializeToString())

fo.close()

fo = open("crf_mix/graph.weights.pb", "wb")
w = factor_graph_pb2.Weight()
w.id = 0
w.initialValue = 0
w.isFixed = False
fo.write(struct.pack("i", w.ByteSize()+3))
fo.write(w.SerializeToString())
w = factor_graph_pb2.Weight()
w.id = 1
w.initialValue = 0.001
w.isFixed = True
fo.write(struct.pack("i", w.ByteSize()+3))
fo.write(w.SerializeToString())
fo.close()

