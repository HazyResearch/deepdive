
#include "gibbs.h"
#include "io/bin2text.h"

int main(int argv, char **argc) {
  // parse arguments
  dd::CmdParser cmd_parser = parse_input(argv, argc);

  // run gibbs sampler
  if (cmd_parser.app_name == "gibbs") {
    gibbs(cmd_parser);
  } else if (cmd_parser.app_name == "mat") {
    mat(cmd_parser);
  } else if (cmd_parser.app_name == "inc") {
    inc(cmd_parser);
  } else if (cmd_parser.app_name == "bin2text") {
    return bin2text(cmd_parser);
  }
}
