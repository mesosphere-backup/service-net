#!/bin/bash
set -o errexit -o nounset -o pipefail
function -h {
cat <<USAGE
 USAGE: servicenet-patch.bash <subcommand>

  Add and remove network configuration items for the service net.

USAGE
}; function --help { -h ;}                 # A nice way to handle -h and --help

function main {
  case "${1:-}" in
    --dry-run) dry_run=true ; shift ;;
  esac
  "$@"
}

function globals {
  export LC_ALL=en_US.UTF-8
  export LANG=en_US.UTF-8
  dry_run=false
  verbose=false
}; globals

function dummy {
  local name="$1" ipv6="$2"
  perform ip link add "$name" type dummy
  perform ip -6 addr add "$ipv6" dev "$name"
  perform ip link set "$name" up
}

function tunnel {
  local name="$1" local_ipv4="$2" remote_ipv4="$3"
  local           local_ipv6="$4" remote_ipv6="${5%/*}" remote_bits="${5#*/}"
  perform ip tunnel add "$name" mode sit ttl 64 \
                         local "$local_ipv4" remote "$remote_ipv4"
  perform ip -6 addr add "$local_ipv6" dev "$name"
  perform ip -6 route add "$remote_ipv6"/"$remote_bits" dev "$name" metric 1
  perform ip link set "$name" up
}

function natfan {
  local name="$1" ipv6="$2" mark="$3" ; shift 3
  [[ $# -gt 0 ]] || return 0
  local backends=( "$@" )
  # The backends are passed as <ip>@<weight>. We parse them and put the
  # weights and IPs in separate arrays.
  local ips=( "${backends[@]%@*}" ) weights=( ) weight=
  for backend in "${backends[@]}"
  do
    weight="${backend#*@}"
    # For backends without an explicit weight, we assign a weight of 1.
    [[ $weight != $backend ]] && weights+=( "$weight" ) || weights+=( 1 )
  done
  # We need to pass probabilities to iptables. Because the rules fallthrough,
  # we need probabilities that stack. For example, if we have three backends
  # with even weights, the probabilities should be 1/3, 1/2 and 1. (1/3 of all
  # the traffic, 1/2 of two thirds of the traffic and the whole of one third
  # of the traffic.)
  local probabilities=( $(cascade "${weights[@]}") )
  # We "mark" the packets before rewriting the destination address in the
  # OUTPUT chain (a fairly early chain) so we can find them later in the
  # POSTROUTING chain. These marks are something internal to the Linux
  # networking stack. Packet marks are IP protocol agnostic and don't get
  # passed on the wire.
  mark "$ipv6" "$mark"
  local n=0 next_to_last=$(( ${#ips[@]} - 1 ))
  while [[ $n -lt $next_to_last ]]
  do
    # Rewrite the destination address to point to this backend.
    dnat "$ipv6" "${ips[$n]}" "${probabilities[$n]}"
    # Rewrite the source address, so traffic can find its way back to the
    # client connection. Do this only for packets where the destination is one
    # of our backends and the mark matches.
    snat "$ipv6" "${ips[$n]}" "$mark"
    n=$(( $n + 1 ))
  done
  # The final backend is treated as a catchall -- no probabilities involved.
  # For services with only one backend, we have a fast path.
  dnat "$ipv6" "${ips[$n]}"
  snat "$ipv6" "${ips[$n]}" "$mark"
}

function cascade {
  while [[ $# != 0 ]]
  do
    # Probability needed for this weight is fraction of remaining weights. The
    # last weight is always assigned probability 1.
    out "scale = 9 ; $1 / ( 0 ${@/#/+ } )"
    shift
  done | bc
}

function mark {
  local ipv6="$1" mark="$2"
  perform ip6tables -t nat -A OUTPUT -d "$ipv6" -j MARK --set-mark "$mark"
}

function dnat {
  local ipv6="$1" backend="$2"
  if [[ ${3:+isset} ]]
  then
    perform ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW \
                      -m statistic --mode random --probability "$3" \
                      -j DNAT --to "$backend"
  else
    perform ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW \
                      -j DNAT --to "$backend"
  fi
}

function snat {
  local ipv6="$1" backend="$2" mark="$3"
  perform ip6tables -t nat -A POSTROUTING -d "$backend" \
                    -m mark --mark "$mark" -j SNAT --to "$ipv6"
}

function perform {
  [[ ! ( $verbose || $dry_run ) ]] || format_command _ "$@" >&2
  [[ ! $dry_run ]] || return 0
  if command "$@"
  then [[ ! $verbose ]] || format_command 0 "$@" >&2
  else
    local code=$?
    format_command "$code" "$@" >&2
    return $code
  fi
}

function format_command {
  [[ $1 = _ ]] && printf ': call ; ' || printf ": exit/$1 ; "
  shift
  printf ' %q' "$@"
  echo
}

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

