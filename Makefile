# Makefile for sampler

# run tests when doing just `make`
.DEFAULT_GOAL := test

# platform dependent flags
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

# source files
SOURCES += src/gibbs.cpp
SOURCES += src/io/cmd_parser.cpp
SOURCES += src/io/binary_parser.cpp
SOURCES += src/main.cpp
SOURCES += src/dstruct/factor_graph/weight.cpp
SOURCES += src/dstruct/factor_graph/variable.cpp
SOURCES += src/dstruct/factor_graph/factor.cpp
SOURCES += src/dstruct/factor_graph/factor_graph.cpp
SOURCES += src/dstruct/factor_graph/inference_result.cpp
SOURCES += src/app/gibbs/gibbs_sampling.cpp
SOURCES += src/app/gibbs/single_thread_sampler.cpp
SOURCES += src/app/gibbs/single_node_sampler.cpp
SOURCES += src/timer.cpp 
OBJECTS = $(SOURCES:.cpp=.o)

# unit test files
TEST_SOURCES += test/test.cpp
TEST_SOURCES += test/FactorTest.cpp
TEST_SOURCES += test/LogisticRegressionTest.cpp
TEST_SOURCES += test/binary_parser_test.cpp
TEST_SOURCES += test/loading_test.cpp
TEST_SOURCES += test/factor_graph_test.cpp
TEST_SOURCES += test/sampler_test.cpp
TEST_SOURCES += test/multinomial.cpp
TEST_OBJECTS = $(TEST_SOURCES:.cpp=.o)
# unit test files need gtest
$(TEST_OBJECTS): CPP_FLAG += -I./lib/gtest-1.7.0/include/

# how to link our sampler
dw: $(OBJECTS)
	$(COMPILE_CMD) -o $@ $^

# how to link our sampler unit tests
dw_test: $(TEST_OBJECTS) $(filter-out src/main.o,$(OBJECTS))
	$(COMPILE_CMD) -L./lib/gtest/ -o $@ $^ -lgtest

# how to compile each source
%.o: %.cpp
	$(COMPILE_CMD) -o $@ -c $<

# how to get dependencies prepared
dep:
	# gtest for tests
	cd lib;\
	unzip gtest-1.7.0.zip;\
	mkdir gtest;\
	cd gtest;\
	cmake ../gtest-1.7.0;\
	make
	# tclap for command-line args parsing
	cd lib;\
	tar xf tclap-1.2.1.tar.gz;\
	cd tclap-1.2.1;\
	./configure --prefix=`pwd`/../tclap;\
	make;\
	make install
	# bats for end-to-end tests
	git clone https://github.com/sstephenson/bats test/bats
.PHONY: dep

# how to clean
clean:
	rm -f dw dw_test $(OBJECTS) $(TEST_OBJECTS)
.PHONY: clean

# how to test
test: unit-test end2end-test
unit-test: dw_test
	./dw_test
PATH := $(shell pwd)/test/bats/bin:$(PATH)
end2end-test: dw
	bats test/*.bats
.PHONY: test unit-test end2end-test
