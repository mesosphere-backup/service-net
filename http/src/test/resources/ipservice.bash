#!/bin/bash
set -o errexit -o nounset -o pipefail
function -h {
cat <<USAGE
 USAGE: ipservice.bash <url>?

  Test throughput and stability of the Svcnet IP service. When no URL is passed,
  the test is run against:

    $default_url

USAGE
}; function --help { -h ;}                 # A nice way to handle -h and --help
export LC_ALL=en_US.UTF-8                    # A locale that works consistently

function main {
  local n=500 url="${1:-$default_url}"

  msg "Running $n concurrent queries against $url..."

  for id in $(seq 1 "$n")
  do curl -sSfL -X POST "$url" -d '{ "name": "'"$id.svc"'" }' \
                               -w 'HTTP//%{http_code}\n' \
                               -o /dev/null 2>/dev/null &
  done | pv -l -s "$n" | sort | uniq -c

  wait
}

default_url=http://localhost:9000/ip-request

##################################################################### Utilities

function msg { out "$*" >&2 ;}
function err { local x=$? ; msg "$*" ; return $(( $x == 0 ? 1 : $x )) ;}
function out { printf '%s\n' "$*" ;}

# Handles "no-match" exit code specified by POSIX for filtering tools.
function maybe { "$@" || return $(( $? == 1 ? 0 : $? )) ;}

######################### Delegates to subcommands or runs main, as appropriate
if [[ ${1:-} ]] && declare -F | cut -d' ' -f3 | fgrep -qx -- "${1:-}"
then "$@"
else main "$@"
fi


