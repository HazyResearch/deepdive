#include "io/cmd_parser.h"

/**
 * Command-line interface.
 */
int dw(int argc, const char *const argv[]);

/**
 * Runs gibbs sampling using the given command line parser
 */
int gibbs(const dd::CmdParser &cmd_parser);
