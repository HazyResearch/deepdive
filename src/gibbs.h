#include "io/cmd_parser.h"

/*
 * Parse input arguments
 */
dd::CmdParser parse_input(int argc, char **argv);

/**
 * Runs gibbs sampling using the given command line parser
 */
void gibbs(dd::CmdParser &cmd_parser);
