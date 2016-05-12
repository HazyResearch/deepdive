# Makefile for sampler

.DEFAULT_GOAL := all

# common compiler flags
CXXFLAGS += -std=c++0x -Wall -fno-strict-aliasing
LDFLAGS =
LDLIBS =
CXXFLAGS += -I./lib/tclap/include/ -I./src

ifeq ($(DEBUG), true)
CXXFLAGS += -g -DDEBUG
endif

# platform dependent compiler flags
UNAME := $(shell uname)

ifeq ($(UNAME), Linux)
ifndef CXX
CXX = g++
endif
# optimization flags
CXXFLAGS += -Ofast
# using NUMA for Linux
CXXFLAGS += -I./lib/numactl-2.0.9/
LDFLAGS += -L./lib/numactl-2.0.9/
LDFLAGS += -Wl,-Bstatic -Wl,-Bdynamic
LDLIBS += -lnuma -lrt -lpthread
endif

ifeq ($(UNAME), Darwin)
ifndef CXX
CXX = clang++
endif
ifneq ($(DEBUG), true)
CXXFLAGS += -O3
endif
# optimization
CXXFLAGS += -stdlib=libc++ -mmacosx-version-min=10.7
CXXFLAGS += -flto
endif

# source files
SOURCES += src/gibbs.cpp
SOURCES += src/io/cmd_parser.cpp
SOURCES += src/io/binary_parser.cpp
SOURCES += src/io/bin2text.cpp
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
PROGRAM = dw

# header files
HEADERS += $(wildcard src/*.h src/*/*.h src/**/*.h src/*/*/*.hxx)

# test files
TEST_SOURCES += test/test.cpp
TEST_SOURCES += test/FactorTest.cpp
TEST_SOURCES += test/binary_parser_test.cpp
TEST_SOURCES += test/loading_test.cpp
TEST_SOURCES += test/checkpoint_test.cpp
TEST_SOURCES += test/factor_graph_test.cpp
TEST_SOURCES += test/sampler_test.cpp
TEST_OBJECTS = $(TEST_SOURCES:.cpp=.o)
TEST_PROGRAM = $(PROGRAM)_test
# test files need gtest
$(TEST_OBJECTS): CXXFLAGS += -I./lib/gtest-1.7.0/include/
$(TEST_PROGRAM): LDFLAGS += -L./lib/gtest/
$(TEST_PROGRAM): LDLIBS += -lgtest

# source files for other utilities
TEXT2BIN_SOURCES += src/io/text2bin.cpp
TEXT2BIN_OBJECTS = $(TEXT2BIN_SOURCES:.cpp=.o)
TEXT2BIN_PROGRAM = text2bin

all: $(PROGRAM) $(TEXT2BIN_PROGRAM)
.PHONY: all

# how to link our sampler
$(PROGRAM): $(OBJECTS)
	$(CXX) -o $@ $(LDFLAGS) $^ $(LDLIBS)

# how to link our sampler unit tests
$(TEST_PROGRAM): $(TEST_OBJECTS) $(filter-out src/main.o,$(OBJECTS))
	$(CXX) -o $@ $(LDFLAGS) $^ $(LDLIBS)

# how to link the format converters
$(TEXT2BIN_PROGRAM): $(TEXT2BIN_OBJECTS)
	$(CXX) -o $@ $(LDFLAGS) $^

# compiler generated dependency
# See: http://stackoverflow.com/a/16969086
DEPENDENCIES = $(SOURCES:.cpp=.d) $(TEST_SOURCES:.cpp=.d) $(TEXT2BIN_SOURCES:.cpp=.d)
-include $(DEPENDENCIES)
CXXFLAGS += -MMD

# how to compile each source
%.o: %.cpp
	$(CXX) -o $@ $(CPPFLAGS) $(CXXFLAGS) -c $<

# how to get dependencies prepared
dep:
	# gtest for tests
	cd lib;\
	unzip -o gtest-1.7.0.zip;\
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
.PHONY: dep

# how to clean
clean:
	rm -f $(PROGRAM) $(OBJECTS) $(TEST_PROGRAM) $(TEST_OBJECTS) $(TEXT2BIN_PROGRAM) $(TEXT2BIN_OBJECTS) $(DEPENDENCIES)
.PHONY: clean

# how to test
test: unit-test end2end-test
unit-test: $(TEST_PROGRAM)
	./$(TEST_PROGRAM)
PATH := $(shell pwd)/test/bats/bin:$(PATH)
end2end-test: $(PROGRAM) $(TEXT2BIN_PROGRAM)
	bats test/*.bats
.PHONY: test unit-test end2end-test

# how to format code
ifndef CLANG_FORMAT
ifneq ($(shell type clang-format-3.7 2>/dev/null),)
    CLANG_FORMAT = clang-format-3.7
else
    CLANG_FORMAT = clang-format
endif
endif
format:
	$(CLANG_FORMAT) -i $(SOURCES) $(TEST_SOURCES) $(TEXT2BIN_SOURCES) $(HEADERS)
.PHONY: format

# how to quickly turn actual test output into expected ones
actual-expected:
	for actual in test/*/*.bin; do \
	    expected="$$actual".txt; \
	    [[ -e "$$expected" ]] || continue; \
	    xxd "$$actual" >"$$expected"; \
	done
.PHONY: actual-expected
