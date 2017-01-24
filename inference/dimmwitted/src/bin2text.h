#ifndef DIMMWITTED_BIN2TEXT_H_
#define DIMMWITTED_BIN2TEXT_H_

#include "cmd_parser.h"
#include "factor_graph.h"

#include <string>

namespace dd {

void dump_factorgraph(const FactorGraph& fg, const std::string& output_dir);
void dump_meta(const FactorGraph& fg, const std::string& filename);
void dump_variables(const FactorGraph& fg, const std::string& filename);
void dump_domains(const FactorGraph& fg, const std::string& filename);
void dump_factors(const FactorGraph& fg, const std::string& filename);
void dump_weights(const FactorGraph& fg, const std::string& filename);

int bin2text(const CmdParser& cmd_parser);

}  // namespace dd

#endif  // DIMMWITTED_BIN2TEXT_H_
