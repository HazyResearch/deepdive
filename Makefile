# Makefile for sampler

.DEFAULT_GOAL := all

# common compiler flags
CXXFLAGS += -std=c++0x -Wall -Werror -fno-strict-aliasing
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
ifndef DEBUG
CXXFLAGS += -Ofast
endif
# using NUMA for Linux
CXXFLAGS += -I./lib/numactl/include
LDFLAGS += -L./lib/numactl/lib
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
SOURCES += src/gibbs_sampler.cc
SOURCES += src/timer.cc
SOURCES += src/numa_nodes.cc
OBJECTS = $(SOURCES:.cc=.o)
PROGRAM = dw

# header files
HEADERS += $(wildcard src/*.h)

# test files
TEST_SOURCES += test/test_main.cc
TEST_SOURCES += test/factor_test.cc
TEST_SOURCES += test/binary_format_test.cc
TEST_SOURCES += test/loading_test.cc
TEST_SOURCES += test/factor_graph_test.cc
TEST_SOURCES += test/sampler_test.cc
TEST_OBJECTS = $(TEST_SOURCES:.cc=.o)
TEST_PROGRAM = $(PROGRAM)_test
# test files need gtest
$(OBJECTS): CXXFLAGS += -I./lib/gtest-1.7.0/include/
$(TEST_OBJECTS): CXXFLAGS += -I./lib/gtest-1.7.0/include/
$(TEST_PROGRAM): LDFLAGS += -L./lib/gtest/
$(TEST_PROGRAM): LDLIBS += -lgtest -lpthread

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
	make install;\
	cd ..
ifeq ($(UNAME), Linux)
	# libnuma
	echo "installing libnuma";\
	cd lib;\
	tar xf numactl-2.0.11.tar.gz;\
	cd numactl-2.0.11;\
	./configure --prefix=`pwd`/../numactl;\
	make;\
	make install
endif
.PHONY: dep

# how to clean
clean:
	rm -f $(PROGRAM) $(OBJECTS) $(TEST_PROGRAM) $(TEST_OBJECTS) $(DEPENDENCIES)
.PHONY: clean

# how to test
include test/bats.mk
test-build: $(PROGRAM) $(TEST_PROGRAM)

# how to format code
# XXX requiring a particular version since clang-format is not backward-compatible
CLANG_FORMAT_REQUIRED_VERSION := 3.7
ifndef CLANG_FORMAT
ifneq ($(shell which clang-format-$(CLANG_FORMAT_REQUIRED_VERSION) 2>/dev/null),)
    CLANG_FORMAT := clang-format-$(CLANG_FORMAT_REQUIRED_VERSION)
else
    CLANG_FORMAT := clang-format
endif
endif
ifeq (0,$(shell $(CLANG_FORMAT) --version | grep -cF $(CLANG_FORMAT_REQUIRED_VERSION)))
format:
	@echo '# ERROR: clang-format $(CLANG_FORMAT_REQUIRED_VERSION) required'
	@echo '# On a Mac, try:'
	@echo 'brew reinstall https://github.com/Homebrew/homebrew-core/raw/0c1a8721e1d2aeca63647f4f1b5f5a1dbe5d9a8b/Formula/clang-format.rb'
	@echo '# Otherwise, install a release for your OS from http://llvm.org/releases/'
	@false
else
format:
	$(CLANG_FORMAT) -i $(SOURCES) $(TEST_SOURCES) $(TEXT2BIN_SOURCES) $(HEADERS)
endif
.PHONY: format

# how to quickly turn actual test output into expected ones
actual-expected:
	for actual in test/*/*.bin; do \
	    expected="$$actual".txt; \
	    [[ -e "$$expected" ]] || continue; \
	    xxd "$$actual" >"$$expected"; \
	done
.PHONY: actual-expected
