###############################################################################
## jq helper functions for DeepDive Code Compiler
###############################################################################

def merge(objects): reduce objects as $es ({}; . + $es);

def trimWhitespace: gsub("^\\s+|\\s+$"; ""; "m");

def nullOr(expr): if type == "null" then null else expr end;
