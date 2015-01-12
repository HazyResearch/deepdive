
#include "gibbs.h"


int main(int argv, char** argc){

  dd::CmdParser cmd_parser = parse_input(argv, argc);

  if(cmd_parser.app_name == "gibbs"){
    gibbs(cmd_parser);
  }

}







