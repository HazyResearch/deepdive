

UNAME := $(shell uname)

ifeq ($(UNAME), Linux)
ifndef CXX
CXX = g++
endif
OPT_FLAG = -Ofast
GCC_INCLUDE = -I./lib/tclap/include/ -I./lib/protobuf/include/ -I./src -I./lib/numactl-2.0.9/
GCC_LIB = -L./lib/protobuf/lib/ -L./lib/numactl-2.0.9/
CPP_FLAG = -std=c++0x -Wl,-Bstatic -lprotobuf -Wl,-Bdynamic -lnuma -lrt
endif

ifeq ($(UNAME), Darwin)
#install_name_tool -change /Users/czhang/Desktop/dimmwitted/lib/protobuf-2.5.0/../protobuf/lib/libprotobuf.8.dylib ./lib/protobuf/lib/libprotobuf.8.dylib dw
ifndef CXX
CXX = LIBRARY_PATH=$LIBRARY_PATH:./lib/protobuf/lib/ clang++
endif
OPT_FLAG =  -O3 -stdlib=libc++ -mmacosx-version-min=10.7
GCC_INCLUDE = -L./lib/protobuf/lib/ -I./lib/tclap/include/ -I./lib/protobuf/include/ -I./src
GCC_LIB = 
CPP_FLAG = -std=c++0x -lprotobuf 
endif

COMPILE_CMD = $(CXX) $(OPT_FLAG) $(GCC_INCLUDE) $(GCC_LIB) $(CPP_FLAG)

# LIBRARY_PATH=$LIBRARY_PATH:./lib/protobuf/lib/

#dw2:
#	$(COMPILE_CMD) \
#	src/main.cpp   \
#	src/dstruct/factor_graph/factor_graph.cpp \
#	src/dstruct/factor_graph/factor_graph.pb.cc \
#	src/app/gibbs/gibbs_sampling.cpp

dw: factor_graph.o factor_graph.pb.o gibbs_sampling.o main.o binary_parser.o
	$(COMPILE_CMD) -o dw factor_graph.o factor_graph.pb.o gibbs_sampling.o main.o binary_parser.o $(CPP_FLAG) 

binary_parser.o: src/io/binary_parser.cpp
	$(COMPILE_CMD) -c src/io/binary_parser.cpp

main.o: src/main.cpp
	$(COMPILE_CMD) -c src/main.cpp

factor_graph.o: src/dstruct/factor_graph/factor_graph.cpp src/io/pb_parser.h
	$(COMPILE_CMD) -c src/dstruct/factor_graph/factor_graph.cpp

factor_graph.pb.o: src/dstruct/factor_graph/factor_graph.pb.cc
	$(COMPILE_CMD) -c src/dstruct/factor_graph/factor_graph.pb.cc

gibbs_sampling.o: src/app/gibbs/gibbs_sampling.cpp
	$(COMPILE_CMD) -c src/app/gibbs/gibbs_sampling.cpp

assembly:
	$(COMPILE_CMD) -S src/app/gibbs/gibbs_sampling.cpp

dep:
ifeq ($(UNAME), Darwin)
	cd lib;\
	tar xf protobuf-2.5.0.tar.bz2;\
	cd protobuf-2.5.0;\
	./configure --prefix=`pwd`/../protobuf CC=clang CXX=clang++ CXXFLAGS='-std=c++11 -stdlib=libc++ -O3 -g' LDFLAGS='-stdlib=libc++' LIBS="-lc++ -lc++abi";\
	make -j8;\
	make install

	cd lib;\
	tar xf tclap-1.2.1.tar.gz;\
	cd tclap-1.2.1;\
	./configure --prefix=`pwd`/../tclap;\
	make;\
	make install
endif

ifeq ($(UNAME), Linux)
	cd lib;\
	tar xf protobuf-2.5.0.tar.bz2;\
	cd protobuf-2.5.0;\
	./configure --prefix=`pwd`/../protobuf ;\
	make -j8;\
	make install

	cd lib;\
	tar xf tclap-1.2.1.tar.gz;\
	cd tclap-1.2.1;\
	./configure --prefix=`pwd`/../tclap;\
	make;\
	make install
endif


clean:
	rm -rf factor_graph.o factor_graph.pb.o gibbs_sampling.o main.o
	rm -rf dw

#gibbs:
#	./dw gibbs -m data2/graph.meta.pb		\
			   -e data2/graph.edges.pb 		\
			   -w data2/graph.weights.pb 	\
			   -v data2/graph.variables.pb 	\
			   -f data2/graph.factors.pb    \
			   -o data2/					\
			   -i 100 -l 100 -s 10 --alpha 0.01 --diminish 0.95

gibbs2:
	./dw gibbs 						       \
			   -e data/graph.edges.pb 		\
			   -w data/graph.weights.pb 	\
			   -v data/graph.variables.pb 	\
			   -f data/graph.factors.pb    \
			   -o data/					\
			   -i 100 -l 0 -s 10 --alpha 0.01 --diminish 0.95

test:
	./dw gibbs -e ./test/factor_graph/lr_inf/ 		\
			   -o ./test/factor_graph/lr_inf/ 		\
			   -i 100 -l 100 -s 10

test_multi:
	./dw gibbs -m test/generate/lr_multi/graph.meta.pb		\
			   -e test/generate/lr_multi/graph.edges.pb 		\
			   -w test/generate/lr_multi/graph.weights.pb 	\
			   -v test/generate/lr_multi/graph.variables.pb 	\
			   -f test/generate/lr_multi/graph.factors.pb    \
			   -o test/generate/lr_multi/					\
			   -i 100 -l 100 -s 10 --alpha 0.01 --diminish 0.95


test_learn:
	./dw gibbs -m test/generate/lr_learn/graph.meta.pb		\
			   -e test/generate/lr_learn/graph.edges.pb 		\
			   -w test/generate/lr_learn/graph.weights.pb 	\
			   -v test/generate/lr_learn/graph.variables.pb 	\
			   -f test/generate/lr_learn/graph.factors.pb    \
			   -o test/generate/lr_learn/					\
			   -i 100 -l 100 -s 10 --alpha 0.01 --diminish 0.95

test_learn2:
	./dw gibbs -m test/generate/lr_learn2/graph.meta.pb		\
			   -e test/generate/lr_learn2/graph.edges.pb 		\
			   -w test/generate/lr_learn2/graph.weights.pb 	\
			   -v test/generate/lr_learn2/graph.variables.pb 	\
			   -f test/generate/lr_learn2/graph.factors.pb    \
			   -o test/generate/lr_learn2/					\
			   -i 100 -l 100 -s 10 --alpha 0.001 --diminish 0.95



test_learn_dep:
	./dw gibbs -e ./test/factor_graph/lr_learn_dep/ 	\
			   -o ./test/factor_graph/lr_learn_dep/ 	\
			   -i 1000 -l 100 -s 10 --alpha 0.0001

test_crf:
	./dw gibbs -e ./test/factor_graph/crf_mix/ 		\
			   -o ./test/factor_graph/crf_mix/ 		\
			   -i 1000 -l 100 -s 10 --alpha 0.0001

gibbs_pb:
	./lib/protobuf/bin/protoc -I=./src/dstruct/factor_graph/ --cpp_out=./src/dstruct/factor_graph/ ./src/dstruct/factor_graph/factor_graph.proto

gibbs_pb_py:
	./lib/protobuf/bin/protoc -I=./src/dstruct/factor_graph/ --python_out=./test/generate/ ./src/dstruct/factor_graph/factor_graph.proto

gibbs:
	./dw gibbs -m data3/graph.meta.csv		\
			   -e data3/graph.edges 		\
			   -w data3/graph.weights 	\
			   -v data3/graph.variables 	\
			   -f data3/graph.factors    \
			   -o data3/					\
			   -i 100 -l 100 -s 10 --alpha 0.01 --diminish 0.95

