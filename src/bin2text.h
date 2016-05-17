#ifndef DIMMWITTED_BIN2TEXT_H_
#define DIMMWITTED_BIN2TEXT_H_

#include "cmd_parser.h"
#include "factor_graph.h"

#include <string>

namespace dd {

void dump_factorgraph(const dd::FactorGraph& fg, const std::string& output_dir);
void dump_meta(const dd::FactorGraph& fg, const std::string& filename);
void dump_variables(const dd::FactorGraph& fg, const std::string& filename);
void dump_domains(const dd::FactorGraph& fg, const std::string& filename);
void dump_factors(const dd::FactorGraph& fg, const std::string& filename);
void dump_weights(const dd::FactorGraph& fg, const std::string& filename);

int bin2text(const dd::CmdParser& cmd_parser);

}  // namespace dd

#endif  // DIMMWITTED_BIN2TEXT_H_
