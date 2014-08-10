
#include <iostream>
#include <fstream>
#include <stdlib.h> 


void load_var(std::string filename){
  std::ifstream fin(filename.c_str());
  std::ofstream fout((filename + ".bin").c_str(), std::ios::binary | std::ios::out);

  long vid;
  int is_evidence;
  double initial_value;
  short type;
  long cardinality;
  int n1 = -1;

  while(fin >> vid >> is_evidence >> initial_value >> type >> cardinality){
    fout.write((char*)&vid, 8);
    fout.write((char*)&is_evidence, 1);
    fout.write((char*)&initial_value, 8);
    fout.write((char*)&type, 2);
    fout.write((char*)&cardinality, 8);
    //fout << vid << is_evidence << initial_value << type << n1 << equal_predicate;
  }

  fin.close();
  fout.close();
}

void load_weight(std::string filename){
  std::ifstream fin(filename.c_str());
  std::ofstream fout((filename + ".bin").c_str(), std::ios::binary | std::ios::out);

  long wid;
  int isfixed;
  double initvalue;

  while(fin >> wid >> isfixed >> initvalue){
    //fout << wid << isfixed << initvalue;
    fout.write((char*)&wid, 8);
    fout.write((char*)&isfixed, 1);
    fout.write((char*)&initvalue, 8);
  }

  fin.close();
  fout.close();
}

void load_factor(std::string filename, short funcid, long nvar, char** positives){
  std::ifstream fin(filename.c_str());
  std::ofstream fout((filename + ".bin").c_str(), std::ios::binary | std::ios::out);
  std::cout << "starting" << std::endl;

  fout.write((char*)&funcid, 2);
  fout.write((char*)&nvar, 8);

  //fout << funcid << std::endl;
  //fout << nvar << std::endl;
  long nedge = 0;
  for(int i=0;i<nvar;i++){
    int tmp = atoi(positives[i]);
    fout.write((char*)&tmp, sizeof(int));
  }

  long a;
  while(fin >> a){ 
    nedge ++; 
    fout.write((char*)&a, sizeof(long));
  }

  nedge = nedge/(nvar + 1)*nvar;
  std::cout << nedge << std::endl;

  fin.close();
  fout.close();
}

int main(int argc, char** argv){
  std::string app(argv[1]);
  if(app.compare("variable")==0){
    load_var(argv[2]);
  }
  if(app.compare("weight")==0){
    load_weight(argv[2]);
  }
  if(app.compare("factor")==0){
    load_factor(argv[2], atoi(argv[3]), atoi(argv[4]), &argv[5]);
  }
  return 0;
}









