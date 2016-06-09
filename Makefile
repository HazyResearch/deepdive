# Makefile for sampler

.DEFAULT_GOAL := all

# common compiler flags
CXXFLAGS += -std=c++0x -Wall -fno-strict-aliasing
LDFLAGS =
LDLIBS =
CXXFLAGS += -I./lib/tclap/include/ -I./src

ifdef DEBUG
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
ifndef DEBUG
CXXFLAGS += -O3
endif
# optimization
CXXFLAGS += -stdlib=libc++ -mmacosx-version-min=10.7
CXXFLAGS += -flto
endif

# source files
SOURCES += src/dimmwitted.cc
SOURCES += src/cmd_parser.cc
SOURCES += src/binary_format.cc
SOURCES += src/bin2text.cc
SOURCES += src/text2bin.cc
SOURCES += src/main.cc
SOURCES += src/weight.cc
SOURCES += src/variable.cc
SOURCES += src/factor.cc
SOURCES += src/factor_graph.cc
SOURCES += src/inference_result.cc
SOURCES += src/gibbs_sampling.cc
SOURCES += src/single_thread_sampler.cc
SOURCES += src/single_node_sampler.cc
SOURCES += src/timer.cc
OBJECTS = $(SOURCES:.cc=.o)
PROGRAM = dw

# header files
HEADERS += $(wildcard src/*.h src/*.hh)

# test files
TEST_SOURCES += test/test_main.cc
TEST_SOURCES += test/factor_test.cc
TEST_SOURCES += test/binary_format_test.cc
TEST_SOURCES += test/loading_test.cc
TEST_SOURCES += test/checkpoint_test.cc
TEST_SOURCES += test/factor_graph_test.cc
TEST_SOURCES += test/sampler_test.cc
TEST_OBJECTS = $(TEST_SOURCES:.cc=.o)
TEST_PROGRAM = $(PROGRAM)_test
# test files need gtest
$(TEST_OBJECTS): CXXFLAGS += -I./lib/gtest-1.7.0/include/
$(TEST_PROGRAM): LDFLAGS += -L./lib/gtest/
$(TEST_PROGRAM): LDLIBS += -lgtest

all: $(PROGRAM)
.PHONY: all

# how to link our sampler
$(PROGRAM): $(OBJECTS)
	$(CXX) -o $@ $(LDFLAGS) $^ $(LDLIBS)

# how to link our sampler unit tests
$(TEST_PROGRAM): $(TEST_OBJECTS) $(filter-out src/main.o,$(OBJECTS))
	$(CXX) -o $@ $(LDFLAGS) $^ $(LDLIBS)

# compiler generated dependency
# See: http://stackoverflow.com/a/16969086
DEPENDENCIES = $(SOURCES:.cc=.d) $(TEST_SOURCES:.cc=.d) $(TEXT2BIN_SOURCES:.cc=.d)
-include $(DEPENDENCIES)
CXXFLAGS += -MMD

# how to compile each source
%.o: %.cc
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
	rm -f $(PROGRAM) $(OBJECTS) $(TEST_PROGRAM) $(TEST_OBJECTS) $(DEPENDENCIES)
.PHONY: clean

# how to test
include test/bats.mk
test: $(PROGRAM) $(TEST_PROGRAM)

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
