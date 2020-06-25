#!/usr/bin/env bash

curl --silent --fail "http://${BOOT_ADDRESS:-localhost:8080}/actuator/health" \
  | grep --ignore-case --silent '"status"\s*:\s*"up"'
