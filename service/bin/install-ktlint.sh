#!/bin/bash

set -euxo pipefail
cd $(dirname $0)
if [[ ! -f "ktlint" ]]; then
  curl -sSLO https://github.com/shyiko/ktlint/releases/download/0.19.0/ktlint
  chmod a+x ./ktlint
fi

