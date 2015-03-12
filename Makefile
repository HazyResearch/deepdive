

UNAME := $(shell uname)

ifeq ($(UNAME), Linux)
ifndef CXX
CXX = g++
endif
OPT_FLAG = -Ofast
GCC_INCLUDE = -I./lib/tclap/include/ -I./src -I./lib/numactl-2.0.9/
GCC_LIB = -L./lib/numactl-2.0.9/ 
CPP_FLAG = -std=c++0x -Wl,-Bstatic -Wl,-Bdynamic -lnuma -lrt -Wall -fno-strict-aliasing
endif

ifeq ($(UNAME), Darwin)
ifndef CXX
CXX = clang++
endif
OPT_FLAG =  -O3 -stdlib=libc++ -mmacosx-version-min=10.7
GCC_INCLUDE = -I./lib/tclap/include/ -I./src
GCC_LIB = 
CPP_FLAG = -flto -std=c++0x -Wall -fno-strict-aliasing
endif

COMPILE_CMD = $(CXX) $(OPT_FLAG) $(GCC_INCLUDE) $(GCC_LIB) $(CPP_FLAG)

dw: factor_graph.o inference_result.o gibbs_sampling.o main.o binary_parser.o single_thread_sampler.o \
	timer.o gibbs.o single_node_sampler.o factor.o variable.o weight.o cmd_parser.o
	$(COMPILE_CMD) -o dw gibbs_sampling.o main.o binary_parser.o \
					    timer.o gibbs.o single_node_sampler.o \
						factor_graph.o inference_result.o single_thread_sampler.o factor.o \
						variable.o weight.o cmd_parser.o \
	$(CPP_FLAG) 

gibbs.o: src/gibbs.cpp
	$(COMPILE_CMD) -c src/gibbs.cpp

cmd_parser.o: src/io/cmd_parser.cpp
	$(COMPILE_CMD) -c src/io/cmd_parser.cpp

binary_parser.o: src/io/binary_parser.cpp
	$(COMPILE_CMD) -c src/io/binary_parser.cpp

main.o: src/main.cpp
	$(COMPILE_CMD) -c src/main.cpp

weight.o: src/dstruct/factor_graph/weight.cpp
	$(COMPILE_CMD) -c src/dstruct/factor_graph/weight.cpp

variable.o: src/dstruct/factor_graph/variable.cpp
	$(COMPILE_CMD) -c src/dstruct/factor_graph/variable.cpp

factor.o: src/dstruct/factor_graph/factor.cpp
	$(COMPILE_CMD) -c src/dstruct/factor_graph/factor.cpp

factor_graph.o: src/dstruct/factor_graph/factor_graph.cpp
	$(COMPILE_CMD) -c src/dstruct/factor_graph/factor_graph.cpp

inference_result.o: src/dstruct/factor_graph/inference_result.cpp
	$(COMPILE_CMD) -c src/dstruct/factor_graph/inference_result.cpp

gibbs_sampling.o: src/app/gibbs/gibbs_sampling.cpp 
	$(COMPILE_CMD)  -c src/app/gibbs/gibbs_sampling.cpp

single_thread_sampler.o: src/app/gibbs/single_thread_sampler.cpp
	$(COMPILE_CMD) -c src/app/gibbs/single_thread_sampler.cpp

single_node_sampler.o: src/app/gibbs/single_node_sampler.cpp
	$(COMPILE_CMD) -c src/app/gibbs/single_node_sampler.cpp

timer.o : src/timer.cpp 
	$(COMPILE_CMD) -o timer.o -c src/timer.cpp 

dw_test: factor_graph.o inference_result.o gibbs_sampling.o  binary_parser.o single_thread_sampler.o \
	timer.o gibbs.o single_node_sampler.o factor.o variable.o weight.o cmd_parser.o
	
	$(COMPILE_CMD) -I./lib/gtest-1.7.0/include/ -L./lib/gtest/ \
	-o dw_test test/test.cpp test/FactorTest.cpp test/LogisticRegressionTest.cpp \
	test/binary_parser_test.cpp test/loading_test.cpp test/factor_graph_test.cpp \
	test/sampler_test.cpp test/multinomial.cpp \
	gibbs_sampling.o binary_parser.o \
    timer.o gibbs.o single_node_sampler.o \
	factor_graph.o inference_result.o single_thread_sampler.o factor.o \
	variable.o weight.o cmd_parser.o \
	$(CPP_FLAG) -lgtest

dep:
ifeq ($(UNAME), Darwin)

	cd lib;\
	unzip gtest-1.7.0.zip;\
	mkdir gtest;\
	cd gtest;\
	cmake ../gtest-1.7.0;\
	make

	cd lib;\
	tar xf tclap-1.2.1.tar.gz;\
	cd tclap-1.2.1;\
	./configure --prefix=`pwd`/../tclap;\
	make;\
	make install
endif

ifeq ($(UNAME), Linux)

	cd lib;\
	unzip gtest-1.7.0.zip;\
	mkdir gtest;\
	cd gtest;\
	cmake ../gtest-1.7.0;\
	make

	cd lib;\
	tar xf tclap-1.2.1.tar.gz;\
	cd tclap-1.2.1;\
	./configure --prefix=`pwd`/../tclap;\
	make;\
	make install
endif

clean:
	rm -rf *.o
	rm -rf dw dw_test

gibbs:
	./dw gibbs -m data3/graph.meta.csv      \
		-e data3/graph.edges         \
		-w data3/graph.weights   \
		-v data3/graph.variables     \
		-f data3/graph.factors    \
		-o data3/                    \
		-i 1000 -l 1000 -s 10 --alpha 0.01 --diminish 0.95

test: clean dw_test
	./dw_test

.PHONY: test clean dw
