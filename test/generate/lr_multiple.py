
import struct
import factor_graph_pb2
import random

NVAR = 100000

fo = open("lr_learn2/graph.meta.pb", "wb")
w = factor_graph_pb2.FactorGraph()
w.numWeights = 10
w.numVariables = NVAR*2
w.numFactors = NVAR*2*10
w.numEdges = NVAR*2*10
w.weightsFile = ""
w.variablesFile = ""
w.factorsFile = ""
w.edgesFile = ""
fo.write(w.SerializeToString())
fo.close()


fo = open("lr_learn2/graph.variables.pb", "wb")
for i in range(0,NVAR):
	v = factor_graph_pb2.Variable()
	v.id = i
	if random.random() < 0.8:
		v.initialValue = 1
	else:
		v.initialValue = 0
	v.dataType = 0
	v.isEvidence = True
	v.cardinality = 1
	v.edgeCount = 10
	size = v.ByteSize()
	fo.write(struct.pack("i", size + 3))
	fo.write(v.SerializeToString())

for i in range(NVAR,2*NVAR):
	v = factor_graph_pb2.Variable()
	v.id = i
	v.initialValue = 0
	v.dataType = 0
	v.isEvidence = False
	v.cardinality = 1
	v.edgeCount = 10
	size = v.ByteSize()
	fo.write(struct.pack("i", size + 3))
	fo.write(v.SerializeToString())
fo.close()

fo = open("lr_learn2/graph.factors.pb", "wb")
for i in range(0,NVAR*10):
	f = factor_graph_pb2.Factor()
	f.id = i
	f.weightId = i % 10
	f.factorFunction = 0
	f.edgeCount = 1
	fo.write(struct.pack("i", f.ByteSize()+3))
	fo.write(f.SerializeToString())
for i in range(NVAR*10,2*NVAR*10):
	f = factor_graph_pb2.Factor()
	f.id = i
	f.weightId = i % 10
	f.factorFunction = 0
	f.edgeCount = 1
	fo.write(struct.pack("i", f.ByteSize()+3))
	fo.write(f.SerializeToString())
fo.close()

fo = open("lr_learn2/graph.edges.pb", "wb")
for i in range(0,NVAR*10):
	e = factor_graph_pb2.GraphEdge()
	e.variableId = int(i/10)
	e.factorId = i
	e.position = 0
	e.isPositive = True
	fo.write(struct.pack("i", e.ByteSize()+3))
	fo.write(e.SerializeToString())
for i in range(NVAR*10,2*NVAR*10):
	e = factor_graph_pb2.GraphEdge()
	e.variableId = int(i/10)
	e.factorId = i
	e.position = 0
	e.isPositive = True
	fo.write(struct.pack("i", e.ByteSize()+3))
	fo.write(e.SerializeToString())
fo.close()

fo = open("lr_learn2/graph.weights.pb", "wb")
for i in range(0,10):
	w = factor_graph_pb2.Weight()
	w.id = i
	w.initialValue = 0.0
	w.isFixed = False
	fo.write(struct.pack("i", w.ByteSize()+3))
	fo.write(w.SerializeToString())
fo.close()



