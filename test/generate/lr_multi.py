
import struct
import factor_graph_pb2
import random

NVAR = 10000

fo = open("lr_multi/graph.meta.pb", "wb")
w = factor_graph_pb2.FactorGraph()
w.numWeights = 4
w.numVariables = NVAR*2
w.numFactors = NVAR*8
w.numEdges = NVAR*8
w.weightsFile = ""
w.variablesFile = ""
w.factorsFile = ""
w.edgesFile = ""
fo.write(w.SerializeToString())
fo.close()

fo = open("lr_multi/graph.variables.pb", "wb")
for i in range(0,NVAR):
	v = factor_graph_pb2.Variable()
	v.id = i
	if random.random() < 0.03205860328008499:
		v.initialValue = 0
	elif random.random() < 0.08714431874203257+0.03205860328008499:
		v.initialValue = 1
	elif random.random() < 0.08714431874203257+0.03205860328008499+0.23688281808991013:
		v.initialValue = 2
	else:
		v.initialValue = 3
	v.dataType = 1
	v.isEvidence = True
	v.cardinality = 4
	v.edgeCount = 4
	size = v.ByteSize()
	fo.write(struct.pack("i", size + 3))
	fo.write(v.SerializeToString())
for i in range(NVAR,NVAR*2):
	v = factor_graph_pb2.Variable()
	v.id = i
	v.initialValue = 0
	v.dataType = 1
	v.isEvidence = False
	v.cardinality = 4
	v.edgeCount = 4
	size = v.ByteSize()
	fo.write(struct.pack("i", size + 3))
	fo.write(v.SerializeToString())
fo.close()

fo = open("lr_multi/graph.factors.pb", "wb")
for i in range(0,NVAR*8):
	f = factor_graph_pb2.Factor()
	f.id = i
	f.weightId = i%4
	f.factorFunction = 0
	f.edgeCount = 1
	fo.write(struct.pack("i", f.ByteSize()+3))
	fo.write(f.SerializeToString())
fo.close()

fo = open("lr_multi/graph.edges.pb", "wb")
for i in range(0,NVAR*8):
	e = factor_graph_pb2.GraphEdge()
	e.variableId = int(i/4)
	e.factorId = i
	e.position = 0
	e.isPositive = True
	e.equalPredicate = i%4
	fo.write(struct.pack("i", e.ByteSize()+3))
	fo.write(e.SerializeToString())
fo.close()

fo = open("lr_multi/graph.weights.pb", "wb")
for i in [0,1,2,3]:
	w = factor_graph_pb2.Weight()
	w.id = i
	w.initialValue = 0.0
	w.isFixed = False
	fo.write(struct.pack("i", w.ByteSize()+3))
	fo.write(w.SerializeToString())
fo.close()



