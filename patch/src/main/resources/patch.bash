#!/bin/bash
set -o errexit -o nounset -o pipefail
function -h {
cat <<USAGE
 USAGE: servicenet-patch.bash <subcommand>

  Add and remove network configuration items for the service net.

USAGE
}; function --help { -h ;}                 # A nice way to handle -h and --help

function main {
  : abstract
}

function globals {
  export LC_ALL=en_US.UTF-8                  # A locale that works consistently
}; globals

function dummy {
  local name="$1" ipv6="$2"
  ip link add "$name" type dummy
  ip -6 addr add "$ipv6" dev "$name"
  ip link set "$name" up
}

function tunnel {
  local name="$1" local_ipv4="$2" remote_ipv4="$3"
  local           local_ipv6="$4" remote_ipv6="${5%/*}" remote_bits="${5#*/}"
  ip tunnel add "$name" mode sit ttl 64 \
                 local "$local_ipv4" remote "$remote_ipv4"
  ip -6 addr add "$local_ipv6" dev "$name"
  ip -6 route add "$remote_ipv6"/"$remote_bits" dev "$name" metric 1
  ip link set "$name" up
}

function natfan {
  local ipv6="$1" mark="$2" ; shift 2
  local backends=( "$@" )
  # We "mark" the packets before rewriting the destination address in the
  # OUTPUT chain (a fairly early chain) so we can find them later in the
  # POSTROUTING chain. These marks are something internal to the Linux
  # networking stack -- packet marks IP protocol agnostic and don't get passed
  # on the wire.
  mark "$ipv6" "$mark"
  # The backends are passed as <ip>@<probability> but the probabilities stack
  # so you have to give some thought to setting them. The last backend should
  # be passed without a probability.
  for backend in "${backends[@]}"
  do
    local ip="${backend%@*}" probability="${backend#*@}"
    if [[ $probability != $backend ]] # There was an @suffix to remove...
    then
      # Route a percentage of the traffic that remains to this backend.
      dnat_random "$ipv6" "$ip" "$probability"
    else
      # Route all remaining traffic to this backend.
      dnat_fixed  "$ipv6" "$ip"
    fi
    # Ensure that traffic finds its way back to the service interface, by
    # rewriting the source address. Do this only for packets where the
    # destination is one of our backends and the mark matches.
    snat "$ipv6" "$ip" "$mark"
  done
}

function mark {
  local ipv6="$1" mark="$2"
  ip6tables -t nat -A OUTPUT -d "$ipv6" -j MARK --set-mark "$mark"
}

function dnat_random {
  # Use the statistic module to randomly pick a backend IP.
  local ipv6="$1" backend="$2" probability="$3"
  ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW \
            -m statistic --mode random --probability "$probability" \
            -j DNAT --to "$backend"
}

function dnat_fixed {
  local ipv6="$1" backend="$2"
  ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW \
            -j DNAT --to "$backend"
}

function snat {
  local ipv6="$1" backend="$2" mark="$3"
  ip6tables -t nat -A POSTROUTING -d "$2" \
            -m mark --mark "$mark" -j SNAT --to "$ipv6"
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

