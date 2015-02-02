#include <iostream>
#include <fstream>
#include <stdint.h>
#include "binary_parser.h"


// 64-bit big endian to little endian
# define bswap_64(x) \
     ((((x) & 0xff00000000000000ull) >> 56)                                   \
      | (((x) & 0x00ff000000000000ull) >> 40)                                 \
      | (((x) & 0x0000ff0000000000ull) >> 24)                                 \
      | (((x) & 0x000000ff00000000ull) >> 8)                                  \
      | (((x) & 0x00000000ff000000ull) << 8)                                  \
      | (((x) & 0x0000000000ff0000ull) << 24)                                 \
      | (((x) & 0x000000000000ff00ull) << 40)                                 \
      | (((x) & 0x00000000000000ffull) << 56))

// 16-bit big endian to little endian
#define bswap_16(x) \
     ((unsigned short int) ((((x) >> 8) & 0xff) | (((x) & 0xff) << 8)))

// Read meta data file, return Meta struct 
Meta read_meta(string meta_file)
{
	ifstream file;
	file.open(meta_file.c_str());
	string buf;
	Meta meta;
	getline(file, buf, ',');
	meta.num_weights = atoll(buf.c_str());
	getline(file, buf, ',');
	meta.num_variables = atoll(buf.c_str());
	getline(file, buf, ',');
	meta.num_factors = atoll(buf.c_str());
	getline(file, buf, ',');
	meta.num_edges = atoll(buf.c_str());
	getline(file, meta.weights_file, ',');
	getline(file, meta.variables_file, ',');
	getline(file, meta.factors_file, ',');
	getline(file, meta.edges_file, ',');
	file.close();
	return meta;
}

// Read weights and load into factor graph
long long read_weights(string filename, dd::FactorGraph &fg)
{
	ifstream file;
    file.open(filename.c_str(), ios::in | ios::binary);
    long long count = 0;
    long long id;
    bool isfixed;
    char padding;
    double initial_value;
    while (file.good()) {
    	// read fields
        file.read((char *)&id, 8);
        file.read((char *)&padding, 1);
        if (!file.read((char *)&initial_value, 8)) break;
        // convert endian
        id = bswap_64(id);
        isfixed = padding;
        long long tmp = bswap_64(*(uint64_t *)&initial_value);
        initial_value = *(double *)&tmp;
        // load into factor graph
        fg.weights[fg.c_nweight] = dd::Weight(id, initial_value, isfixed);
		fg.c_nweight++;
		count++;
    }
    file.close();
    return count;
}


// Read variables
long long read_variables(string filename, dd::FactorGraph &fg)
{
    ifstream file;
    file.open(filename.c_str(), ios::in | ios::binary);
    long long count = 0;
    long long id;
    bool isevidence;
    char padding1;
    double initial_value;
    short type;
    long long edge_count;
    long long cardinality;
    while (file.good()) {
        // read fields
        file.read((char *)&id, 8);
        file.read((char *)&padding1, 1);
        file.read((char *)&initial_value, 8);
        file.read((char *)&type, 2);
        file.read((char *)&edge_count, 8);
        if (!file.read((char *)&cardinality, 8)) break;
        // convert endian
        id = bswap_64(id);
        isevidence = padding1;
        type = bswap_16(type);
        long long tmp = bswap_64(*(uint64_t *)&initial_value);
        initial_value = *(double *)&tmp;
        edge_count = bswap_64(edge_count);
        cardinality = bswap_64(cardinality);
        count++;

        // add to factor graph
        if (type == 0){ // boolean
            if (isevidence) {
                fg.variables[fg.c_nvar] = dd::Variable(id, DTYPE_BOOLEAN, true, 0, 1, 
                    initial_value, initial_value, edge_count);
                fg.c_nvar++;
                fg.n_evid++;
            } else {
                fg.variables[fg.c_nvar] = dd::Variable(id, DTYPE_BOOLEAN, false, 0, 1, 
                    0, 0, edge_count);
                fg.c_nvar++;
                fg.n_query++;
            }
        } else if (type == 1) { // multinomial
            if (isevidence) {
                fg.variables[fg.c_nvar] = dd::Variable(id, DTYPE_MULTINOMIAL, true, 0, 
                    cardinality-1, initial_value, initial_value, edge_count);
                fg.c_nvar ++;
                fg.n_evid ++;
            } else {
                fg.variables[fg.c_nvar] = dd::Variable(id, DTYPE_MULTINOMIAL, false, 0, 
                    cardinality-1, 0, 0, edge_count);
                fg.c_nvar ++;
                fg.n_query ++;
            }
        } else if (type == 3){
            if (isevidence) {
                fg.variables[fg.c_nvar] = dd::Variable(id, DTYPE_REAL, true, 0, cardinality, 
                    initial_value, initial_value, edge_count);
                fg.c_nvar++;
                fg.n_evid++;
            }else{
                fg.variables[fg.c_nvar] = dd::Variable(id, DTYPE_REAL, true, 0, cardinality, 
                    initial_value, initial_value, edge_count);
                fg.c_nvar++;
                fg.n_evid++;
            }
        }else {
            cout << "[ERROR] Only Boolean and Multinomial variables are supported now!" << endl;
            exit(1);
        }

    }
    file.close();
    return count;
}

long long read_factors(string filename, dd::FactorGraph &fg)
{
    ifstream file;
    file.open(filename.c_str(), ios::in | ios::binary);
    long long count = 0;
    long long id;
    long long weightid;
    short type;
    long long edge_count;
    while (file.good()) {
        file.read((char *)&id, 8);
        file.read((char *)&weightid, 8);
        file.read((char *)&type, 2);
        if (!file.read((char *)&edge_count, 8)) break;
        id = bswap_64(id);
        weightid = bswap_64(weightid);
        type = bswap_16(type);
        edge_count = bswap_64(edge_count);
        count++;
        fg.factors[fg.c_nfactor] = dd::Factor(id, weightid, type, edge_count);
        fg.c_nfactor ++;
    }
    file.close();
    return count;
}

long long read_edges(string filename, dd::FactorGraph &fg)
{
    ifstream file;
    file.open(filename.c_str(), ios::in | ios::binary);
    long long count = 0;
    long long variable_id;
    long long factor_id;
    long long position;
    bool ispositive;
    char padding;
    long long equal_predicate;
    while (file.good()) {
        // read fields
        file.read((char *)&variable_id, 8);
        file.read((char *)&factor_id, 8);
        file.read((char *)&position, 8);
        file.read((char *)&padding, 1);
        if (!file.read((char *)&equal_predicate, 8)) break;
        variable_id = bswap_64(variable_id);
        factor_id = bswap_64(factor_id);
        position = bswap_64(position);
        ispositive = padding;
        equal_predicate = bswap_64(equal_predicate);
        count++;

        // wrong id
    	if(variable_id >= fg.n_var || variable_id < 0){
    	  assert(false);
    	}

    	if(factor_id >= fg.n_factor || factor_id < 0){
    	  std::cout << "wrong fid = " << factor_id << std::endl;
    	  assert(false);
        }

        // add variables to factors
        if (fg.variables[variable_id].domain_type == DTYPE_BOOLEAN) {
            fg.factors[factor_id].tmp_variables.push_back(
                dd::VariableInFactor(variable_id, fg.variables[variable_id].upper_bound, variable_id, position, ispositive));
        } else {
            fg.factors[factor_id].tmp_variables.push_back(
                dd::VariableInFactor(variable_id, position, ispositive, equal_predicate));
        }
        fg.variables[variable_id].tmp_factor_ids.push_back(factor_id);

    }
    file.close();
    return count;   
}

