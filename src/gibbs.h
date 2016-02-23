
#include <assert.h>
#include <iostream>
#include "common.h"
#include <unistd.h>

#include "io/cmd_parser.h"
#include "io/binary_parser.h"

#include "app/gibbs/gibbs_sampling.h"
#include "dstruct/factor_graph/factor_graph.h"

/*
 * Parse input arguments
 */
dd::CmdParser parse_input(int argc, char **argv);

/**
 * Runs gibbs sampling using the given command line parser
 */
void gibbs(dd::CmdParser &cmd_parser);

/**
 *
 */
void mat(dd::CmdParser &cmd_parser);

/**
 *
 */
void inc(dd::CmdParser &cmd_parser);
