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

function remove {
  local kind="$1" name="$2"
  case "$kind" in
    tunnel|dummy) if link_exists
                  then perform ip link del "$name"
                  else
                    local code=$?
                    if [[ $code -ne 1 ]]
                    then
                      msg "Fatal error while checking for link/device $name"
                      return "$code"
                    fi
                  fi ;;
    natfan)       for chain in OUTPUT POSTROUTING
                  do
                    for rulenum in $(rules_for_name "$name" "$chain")
                    do perform ip6tables -t nat -D "$chain" "$rulenum"
                    done
                  done ;;
  esac
}

function dummy {
  local name="$1" ; shift
  perform ip link add "$name" type dummy
  for ipv6 in "$@"
  do perform ip -6 addr add "$ipv6" dev "$name"
  done
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
  # We need to pass probabilities to iptables. Because the rules fall through,
  # we need probabilities that stack. For example, if we have three backends
  # with even weights, the probabilities should be 1/3, 1/2 and 1. (1/3 of all
  # the traffic, 1/2 of two thirds of the traffic and the whole of one third
  # of the traffic.)
  local probabilities=( $(cascade "${weights[@]}") )
  # We "mark" the packets before rewriting the destination address in the
  # OUTPUT chain (a fairly early chain) so we can find them later in the
  # POSTROUTING chain. These marks are something internal to the Linux
  # networking stack. Packet marks are IP protocol agnostic and are not passed
  # on the wire.
  mark "$name" "$ipv6" "$mark"
  local n=0 next_to_last=$(( ${#ips[@]} - 1 ))
  while [[ $n -lt $next_to_last ]]
  do
    # Rewrite the destination address to point to this backend.
    dnat "$name" "$ipv6" "${ips[$n]}" "${probabilities[$n]}"
    # Rewrite the source address, so traffic can find its way back to the
    # client connection. Do this only for packets where the destination is one
    # of our backends and the mark matches.
    snat "$name" "$ipv6" "${ips[$n]}" "$mark"
    n=$(( $n + 1 ))
  done
  # The final backend is treated as a catchall -- no probabilities involved.
  # For services with only one backend, we have a fast path.
  dnat "$name" "$ipv6" "${ips[$n]}"
  snat "$name" "$ipv6" "${ips[$n]}" "$mark"
} # TODO: Create a chain for these natfans and use iptables-restore to manage
########  the chain in aggregate. This should be more performant
########  (iptables-restore does the rule changes all-of-a-piece) and it will
########  be much safer in the presence of user-defined rules.

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
  local name="$1" ipv6="$2" mark="$3"
  perform ip6tables -t nat -A OUTPUT -d "$ipv6" \
                    -m comment --comment servicenet//"$name" \
                    -j MARK --set-mark "$mark"
}

function dnat {
  local name="$1" ipv6="$2" backend="$3"
  if [[ ${4:+isset} ]]
  then
    perform ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW \
                      -m comment --comment servicenet//"$name" \
                      -m statistic --mode random --probability "$4" \
                      -j DNAT --to "$backend"
  else
    perform ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW \
                      -m comment --comment servicenet//"$name" \
                      -j DNAT --to "$backend"
  fi
}

function snat {
  local name="$1" ipv6="$2" backend="$3" mark="$4"
  perform ip6tables -t nat -A POSTROUTING -d "$backend" \
                    -m comment --comment servicenet//"$name" \
                    -m mark --mark "$mark" -j SNAT --to "$ipv6"
}

function rules_for_name {
  local name="$1" chain="$2"
  perform ip6tables -t nat -n --line-numbers -L "$chain" |
  maybe fgrep "/* servicenet//$name */" | maybe egrep -o '^[0-9]+' |
  sort -rn # We want to delete the rules in reverse numerical order because if
           # we don't the indices change underneath us and the deletes will
           # hit the wrong rules or simply fail.
}

function link_exists {
  perform ip link list dev "$1" &>/dev/null
}

function perform {
  ! ( $verbose || $dry_run ) || format_command _ "$@" >&2
  ! $dry_run || return 0
  if command "$@"
  then ! $verbose || format_command 0 "$@" >&2
  else
    local code=$?
    format_command "$code" "$@" >&2
    return "$code"
  fi
}

function format_command {
  [[ $1 = _ ]] && printf ':; ' || printf ": exit/$1 ; "
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

