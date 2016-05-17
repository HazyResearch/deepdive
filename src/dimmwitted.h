#ifndef DIMMWITTED_DIMMWITTED_H_
#define DIMMWITTED_DIMMWITTED_H_

#include "cmd_parser.h"

namespace dd {

/**
 * Command-line interface.
 */
int dw(int argc, const char *const argv[]);

/**
 * Runs gibbs sampling using the given command line parser
 */
int gibbs(const dd::CmdParser &cmd_parser);

}  // namespace dd

#endif  // DIMMWITTED_DIMMWITTED_H_
