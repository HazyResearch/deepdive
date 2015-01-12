

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

dw: factor_graph.o gibbs_sampling.o main.o binary_parser.o single_thread_sampler.o timer.o gibbs.o
	$(COMPILE_CMD) -o dw factor_graph.o gibbs_sampling.o main.o binary_parser.o \
						single_thread_sampler.o timer.o gibbs.o \
			$(CPP_FLAG) 

gibbs.o: src/gibbs.cpp
	$(COMPILE_CMD) -c src/gibbs.cpp

binary_parser.o: src/io/binary_parser.cpp
	$(COMPILE_CMD) -c src/io/binary_parser.cpp

main.o: src/main.cpp
	$(COMPILE_CMD) -c src/main.cpp

factor_graph.o: src/dstruct/factor_graph/factor_graph.cpp
	$(COMPILE_CMD) -c src/dstruct/factor_graph/factor_graph.cpp

gibbs_sampling.o: src/app/gibbs/gibbs_sampling.cpp src/app/gibbs/single_thread_sampler.cpp
	$(COMPILE_CMD) -o gibbs_sampling.o -c src/app/gibbs/gibbs_sampling.cpp

single_thread_sampler.o: src/app/gibbs/single_thread_sampler.cpp
	$(COMPILE_CMD) -o single_thread_sampler.o -c src/app/gibbs/single_thread_sampler.cpp

timer.o : src/timer.cpp 
	$(COMPILE_CMD) -o timer.o -c src/timer.cpp 


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

