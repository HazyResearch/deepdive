
#include <iostream>
#include <fstream>
#include <sstream>
#include <stdlib.h> 
#include <stdint.h>
#include <vector>
#include <boost/algorithm/string.hpp>
using namespace std;

// 64-bit endian conversion
# define bswap_64(x) \
     ((((x) & 0xff00000000000000ull) >> 56)                                   \
      | (((x) & 0x00ff000000000000ull) >> 40)                                 \
      | (((x) & 0x0000ff0000000000ull) >> 24)                                 \
      | (((x) & 0x000000ff00000000ull) >> 8)                                  \
      | (((x) & 0x00000000ff000000ull) << 8)                                  \
      | (((x) & 0x0000000000ff0000ull) << 24)                                 \
      | (((x) & 0x000000000000ff00ull) << 40)                                 \
      | (((x) & 0x00000000000000ffull) << 56))

// 16-bit endian conversion
#define bswap_16(x) \
     ((unsigned short int) ((((x) >> 8) & 0xff) | (((x) & 0xff) << 8)))

double StrToDbl(string s) {
     double d;
     stringstream ss(s); //turn the string into a stream
     ss >> d; //convert
     return d;
}

long StrToLng(string s) {
     long d;
     stringstream ss(s); //turn the string into a stream
     ss >> d; //convert
     return d;
}

// read variables and convert to binary format
void load_var(std::string filename){
  std::ifstream fin(filename.c_str());
  std::ofstream fout((filename + ".bin").c_str(), std::ios::binary | std::ios::out);

  long image_id;
  long fid;
  long mid;
  long num_rows;
  long num_cols;
  vector<string> is_evidences;
  vector<string> initial_values;
  long layer;

  string line;
  while (getline(fin, line)) {
    istringstream ss(line);
    ss >> image_id;
    ss >> fid;
    ss >> mid;
    ss >> num_rows >> num_cols;

    image_id = bswap_64(image_id);
    fid = bswap_64(fid);
    mid = bswap_64(mid);
    num_rows = bswap_64(num_rows);
    num_cols = bswap_64(num_cols);

    fout.write((char*)&image_id, 8);
    fout.write((char*)&fid, 8);
    fout.write((char*)&mid, 8);
    fout.write((char*)&num_rows, 8);
    fout.write((char*)&num_cols, 8);


    /// Outputing is_evidence array
    string is_evidence_str;
    ss >> is_evidence_str;
    bool is_evidence;
    boost::algorithm::split( is_evidences, is_evidence_str, boost::algorithm::is_any_of( "{ , }" ) );
    for(int i=0; i<is_evidences.size(); i++)
      if(is_evidences[i]!=""){
        is_evidence=0;
        if(is_evidences[i]!="NULL")
          is_evidence=1;
        fout.write((char*)&is_evidence, 1);
      }
    is_evidences.clear();

    /// Outputing is_evidence array
    string initial_value_str;
    ss >> initial_value_str;
    double initial_value;
    boost::algorithm::split( initial_values, initial_value_str, boost::algorithm::is_any_of( "{ , }" ) );
    for(int i=0; i<initial_values.size(); i++)
      if(initial_values[i]!=""){
        initial_value=0;
        if(initial_values[i]!="NULL")
          initial_value=StrToDbl(initial_values[i]);
        uint64_t initval = bswap_64(*(uint64_t *)&initial_value);
        fout.write((char*)&initval, 8);
      }
    initial_values.clear();

    ss >> layer;
    layer = bswap_64(layer);
    fout.write((char*)&layer, 8);
  }
}                               

void load_weight(std::string filename){

  std::ifstream fin(filename.c_str());
  std::ofstream fout((filename + ".bin").c_str(), std::ios::binary | std::ios::out);

  long wid;
  long num_rows;
  long num_cols;
  bool is_fixed;
  double initial_value;

  while(fin >> wid >> num_rows >> num_cols >> is_fixed >> initial_value){

    wid = bswap_64(wid);
    num_rows = bswap_64(num_rows);
    num_cols = bswap_64(num_cols);
    uint64_t initval = bswap_64(*(uint64_t *)&initial_value);
    fout.write((char*)&wid, 8);
    fout.write((char*)&num_rows, 8);
    fout.write((char*)&num_cols, 8);
    fout.write((char*)&is_fixed, 1);
    fout.write((char*)&initval, 8);
  }

  fin.close();
  fout.close();
}

void load_edges(std::string filename){
  std::ifstream fin(filename.c_str());
  std::ofstream fout((filename + ".bin").c_str(), std::ios::binary | std::ios::out);

  vector<string> in_mat_ids;
  vector<string> in_center_xs;
  vector<string> in_center_ys;
  long out_mat_id;
  long out_center_x;
  long out_center_y;
  long num_inputs;
  long factor_function;
  vector<string> weight_ids;



  string line;
  while (getline(fin, line)) {
    istringstream ss(line);

    /// Outputing in_mat_ids array
    string in_mat_ids_str;
    string in_center_xs_str;
    string in_center_ys_str;
    string weight_ids_str;


    ss >> in_mat_ids_str;
    ss >> in_center_xs_str;
    ss >> in_center_ys_str;
    ss >> out_mat_id >> out_center_x >> out_center_y >> num_inputs >> factor_function;
    ss >> weight_ids_str;

    num_inputs = bswap_64(num_inputs);
    fout.write((char*)&num_inputs, 8);


    long in_mat_id;
    boost::algorithm::split( in_mat_ids, in_mat_ids_str, boost::algorithm::is_any_of( "{ , }" ) );
    for(int i=0; i<in_mat_ids.size(); i++)
      if(in_mat_ids[i]!=""){
        in_mat_id=StrToDbl(in_mat_ids[i]);
        in_mat_id = bswap_64(in_mat_id);
        fout.write((char*)&in_mat_id, 8);
      }
    in_mat_ids.clear();

    /// Outputing in_center_xs_str array
    long in_center_x;
    boost::algorithm::split( in_center_xs, in_center_xs_str, boost::algorithm::is_any_of( "{ , }" ) );
    for(int i=0; i<in_center_xs.size(); i++)
      if(in_center_xs[i]!=""){
        in_center_x=StrToDbl(in_center_xs[i]);
        in_center_x = bswap_64(in_center_x);
        fout.write((char*)&in_center_x, 8);
      }
    in_center_xs.clear() ;

    /// Outputing in_center_ys_str array
    long in_center_y;
    boost::algorithm::split( in_center_ys, in_center_ys_str, boost::algorithm::is_any_of( "{ , }" ) );
    for(int i=0; i<in_center_ys.size(); i++)
      if(in_center_ys[i]!=""){
        in_center_y=StrToDbl(in_center_ys[i]);
        in_center_y=bswap_64(in_center_y);
        fout.write((char*)&in_center_y, 8);
      }
    in_center_ys.clear();

    out_mat_id = bswap_64(out_mat_id);
    out_center_x = bswap_64(out_center_x);
    out_center_y = bswap_64(out_center_y);
    factor_function = bswap_64(factor_function);
    fout.write((char*)&out_mat_id, 8);
    fout.write((char*)&out_center_x, 8);
    fout.write((char*)&out_center_y, 8);
    fout.write((char*)&factor_function, 8);

    /// Outputing weight_id array
    long weight_id;
    boost::algorithm::split( weight_ids, weight_ids_str, boost::algorithm::is_any_of( "{ , }" ) );
    for(int i=0; i<weight_ids.size(); i++)
      if(weight_ids[i]!=""){
        weight_id=StrToDbl(weight_ids[i]);
        weight_id=bswap_64(weight_id);
        fout.write((char*)&weight_id, 8);
      }
    weight_ids.clear();
  }
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
  if(app.compare("edges")==0){
    load_edges(argv[2]);
  }
  return 0;
}





