#ifndef DIMMWITTED_TEXT2BIN_H_
#define DIMMWITTED_TEXT2BIN_H_

#include "cmd_parser.h"

namespace dd {

constexpr char text_field_delim = '\t';  // tsv file delimiter

int text2bin(const CmdParser &args);

}  // namespace dd

#endif  // DIMMWITTED_TEXT2BIN_H_
