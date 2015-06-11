
#include "gibbs.h"


int main(int argv, char** argc){
	// parse arguments
  dd::CmdParser cmd_parser = parse_input(argv, argc);

  // run gibbs sampler
  if (cmd_parser.app_name == "gibbs") {
    gibbs(cmd_parser);
  } else if (cmd_parser.app_name == "mat") {
    mat(cmd_parser);
  } else if (cmd_parser.app_name == "inc") {
    inc(cmd_parser);
  }


}







