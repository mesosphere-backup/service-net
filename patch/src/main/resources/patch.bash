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

function nat {
  local name="$1" device="$2" ipv6_range="$3" ; shift 3
  local backends=( "$@" )
  # This is just NAT, plain and simple -- route traffic to the instance IP
  # behind the service IP.
  # Rule 1: Rewrite the destination address on packets from this box.
  ip6tables -t nat -A OUTPUT -i "$device" -j DNAT --to "$ipv6_range"
  ip6tables -t nat -A OUTPUT -i "$device" -j DNAT --to "$ipv6_range"
  ip6tables -t nat -A OUTPUT -i "$device" -j DNAT --to "$ipv6_range"
  # Rule 2: Rewrite the source address on packets destined for the service IP.
  ip6tables -t nat -A POSTROUTING -d "$instance" -j SNAT --to "$service"
  # TODO: Associate each service with its own interface, so that the SNAT rule
  # above can be rewritten in a safer form.
}

function random_demo {
  local device="$1" ipv6="$2" ; shift
  dummy "$device" "$ipv6"
  random_ip6tables "$device" shard//1 0.25
  random_ip6tables "$device" shard//2 0.25
  random_ip6tables "$device" shard//3 0.25
  random_ip6tables "$device" shard//4
}

function fan {
  local ipv6="$1" mark="$2" ; shift 2
  local backends=( "$@" ) cmd=()
  # Use mark module to catch the packets before we rewrite the destination,
  # for use in the SNAT rule that is applied in the the POSTROUTING chain.
  ip6tables -t nat -A OUTPUT -d "$ipv6" -j MARK --set-mark "$mark"
  # Use the statistic module to randomly pick a backend IP. The backends are
  # passed as <ip>@<probability> but the probabilities stack so you have to
  # give some thought to setting them. The last backend should be passed
  # without a probability.
  for backend in "${backends[@]}"
  do
    local ip="${backend%@*}" probability="${backend#*@}"
    cmd=( ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW )
    if [[ $probability != $backend ]] # There was an @suffix to remove...
    then cmd+=(-m statistic --mode random --probability "$probability" )
    fi
  done
  local pre=( ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW )
  local post=( -j DNAT --to "$backend" )
  # We need a subsequence of the harmonic series here because the probabilities
  # stack up. The last rules is the catchall.
  "${pre[@]}" -m statistic --mode random --probability 0.25 "${post[@]}"
  "${pre[@]}" -m statistic --mode random --probability 0.33 "${post[@]}"
  "${pre[@]}" -m statistic --mode random --probability 0.50 "${post[@]}"
  "${pre[@]}"                                               "${post[@]}"
  # This rule ensures packets find their way back to the service address.
  ip6tables -t nat -A POSTROUTING -m mark --mark "$mark" -j SNAT --to "$ipv6"
}

function fan_ip6tables {
  local ipv6="$1" mark="$2" backend="$3"
  local pre=( ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW )
  local post=( -j DNAT --to "$backend" )
  # Use mark module to catch the packets before we rewrite the destination,
  # for use in the SNAT rule that is applied in the the POSTROUTING chain.
  ip6tables -t nat -A OUTPUT -d "$ipv6" -j MARK --set-mark "$mark"
  # We need a subsequence of the harmonic series here because the probabilities
  # stack up. The last rules is the catchall.
  "${pre[@]}" -m statistic --mode random --probability 0.25 "${post[@]}"
  "${pre[@]}" -m statistic --mode random --probability 0.33 "${post[@]}"
  "${pre[@]}" -m statistic --mode random --probability 0.50 "${post[@]}"
  "${pre[@]}"                                               "${post[@]}"
  # This rule ensures packets find their way back to the service address.
  ip6tables -t nat -A POSTROUTING -m mark --mark "$mark" -j SNAT --to "$ipv6"
}

function random_ip6tables {
  local ipv6="$1" backend="$2" probability="$3"
  ip6tables -t nat -A OUTPUT -d "$ipv6" -m state --state NEW \
            -m statistic --mode random --probability "$probability" \
            -j DNAT --to "$backend"
}

function random_ip6tables {
  local device="$1"
  local pre=( ip6tables -t nat -A OUTPUT -o "$device" -m state --state NEW )
  local post=( -j DNAT --to 2002:904c:dfe3::2 )
  "${pre[@]}" -m statistic --mode random --probability 0.25 "${post[@]}"
  "${pre[@]}" -m statistic --mode random --probability 0.33 "${post[@]}"
  "${pre[@]}" -m statistic --mode random --probability 0.50 "${post[@]}"
  "${pre[@]}"                                               "${post[@]}"
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

