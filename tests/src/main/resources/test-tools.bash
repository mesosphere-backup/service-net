#!/bin/bash
set -o errexit -o nounset -o pipefail

printf %s '$@:'
printf ' %q' "$@" ; echo
