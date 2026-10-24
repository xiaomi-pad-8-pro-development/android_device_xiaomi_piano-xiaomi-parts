#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
keyboard_tests_dir="$(cd -- "$(dirname -- "$0")" && pwd)"
keyboard_parts_dir="$(dirname -- "$keyboard_tests_dir")"
keyboard_test_output="$(mktemp -d)"
trap 'rm -rf -- "$keyboard_test_output"' EXIT
javac -d "$keyboard_test_output" \
    "$keyboard_parts_dir/XiaomiKeyboard/src/org/lineageos/xiaomikeyboard/KeyboardInputPolicy.java" \
    "$keyboard_tests_dir/KeyboardInputPolicyTest.java"
java -cp "$keyboard_test_output" org.lineageos.xiaomikeyboard.KeyboardInputPolicyTest
