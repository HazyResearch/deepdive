#!/usr/bin/env perl
use v5.10;
use strict;
use utf8; binmode $_, ":encoding(UTF-8)" for *STDIN, *STDOUT, *STDERR;
select(STDERR); $| = 1; select(STDOUT); # auto flush STDERR

# XXX for higher throughput maybe we need to increase I/O buffer size?
# See: http://stackoverflow.com/a/1251842
#use IO::Handle '_IOLBF';
#my $buffer;
#STDOUT->setvbuf($buffer, _IOLBF, 0x10000);

# transcode PostgreSQL TSV encoded values into JSON
sub map_pgtsv_escape_seqs_to_json($) {
    my $v = shift;
    # map octal \OOO and hexadecimal \xHH escape sequences to JSON \uXXXX
    $v =~ s/(?<!\\)((?:\\\\)*)\\([0-3][0-7]{2})/"$1\\u00".(sprintf "%02x", oct($2))/ge;
    $v =~ s/(?<!\\)((?:\\\\)*)\\x([0-9a-fA-F]{2})/$1\\u00$2/g;
    $v =~ s/[\x00-\x1F]/"\\u00".(sprintf "%02x", ord($&))/ge;
    # backslash double quotes
    $v =~ s/"/\\"/g;
    # and backslash and the rest of control characters are already escaped
    $v
}
our $NULL_IN_TSV = "\\N";
sub json_string($) {
    my $v = shift;
    if ($v eq $NULL_IN_TSV) { "null" }
    else { '"'. (map_pgtsv_escape_seqs_to_json $v) .'"' }
}
sub json_boolean($) {
    my $v = shift;
    if ($v eq $NULL_IN_TSV) { "null" }
    elsif ($v eq "t") { "true" }
    elsif ($v eq "f") { "false" }
    else { die "Cannot parse PostgreSQL Boolean in TSV: $v" }
}
sub json_value($) {
    my $v = shift;
    if ($v eq $NULL_IN_TSV) { "null" }
    else { $v }
}

# parse PostgreSQL ARRAY and turn them into JSON
# XXX this may become a performance bottleneck, so JSON type column is recommended
# XXX nested arrays aren't supported
sub json_array($$) {
    my $transcode_element = shift;
    my $v = shift;
    if ((substr $v, 0, 1) eq "{" and (substr $v, -1, 1) eq "}") {
        my @json;
        my $csv = substr $v, 1, -1;
        $csv =~ s/\\(["\\])/$1/g;  # XXX unescape PG TSV's awkward backslashes here for quotes used in arrays
        my @fields;
        my $field_quoted = undef;
        while (length($csv) > 0) {
            if (not defined $field_quoted) {
                if ($csv =~ /^"/gc) {
                    # found the beginning of a quoted field
                    $field_quoted = $&;
                } else {
                    # just look for the comma or end-of-line that ends the unquoted field
                    $csv =~ /^([^,]*?)(?:,|$)/gc;
                    push @fields, $1;
                }
                substr($csv, 0, pos($csv)) = "";
            } else {
                # look for the end of the quoted field
                if ($csv =~ /^([^"]*)"/gc) {
                    $field_quoted .= $&;
                } else {
                    die "Unterminated quoted field ($field_quoted) while parsing: $v";
                }
                substr($csv, 0, pos($csv)) = "";
                # check if the double quote actually terminates the field
                if ($field_quoted =~ /(?<!\\)(?:\\\\)*\\"$/) {
                    # it's an actual double quote, so keep finding the terminating quote
                    next;
                } elsif ($csv =~ /^(,|$)/gc) {
                    # double quote terminates the field
                    push @fields, $field_quoted;
                    $field_quoted = undef;
                } else {
                    die "Unexpected characters ($csv) after quoted field ($field_quoted) while parsing: $v";
                }
                substr($csv, 0, pos($csv)) = "";
            }
        }
        for my $el (@fields) {
            if ($el eq "NULL") {
                # unquoted NULL in ARRAYs are transcoded to null
                push @json, "null";
            } else {
                # handle quoted fields
                if ($el =~ /^"(.*)"$/) {
                    $el = $1;
                    $el =~ s/(?<!\\)((?:\\\\)*)\\(["])/$1$2/g;  # XXX uncertain if this is 100% accurate
                }
                push @json, $transcode_element->($el);
            }
        }
        "[".join(",", @json)."]"
    } else { die "Cannot parse PostgreSQL ARRAY in TSV: $v" }
}
################################################################################
