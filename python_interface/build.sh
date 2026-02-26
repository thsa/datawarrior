#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LIB_DIR="$REPO_ROOT/lib"
SRC_DIR="$SCRIPT_DIR/src"
BUILD_DIR="$SCRIPT_DIR/build"

mkdir -p "$BUILD_DIR"

CP="$LIB_DIR/openchemlib.jar:$LIB_DIR/molviewerlib.jar"

echo "Compiling PropertyCalculatorCLI..."
javac -cp "$CP" -d "$BUILD_DIR" "$SRC_DIR/PropertyCalculatorCLI.java"

echo "Creating property_calculator.jar..."
jar cfe "$BUILD_DIR/property_calculator.jar" PropertyCalculatorCLI -C "$BUILD_DIR" PropertyCalculatorCLI.class

echo "Compiling CLogSCalculator..."
javac -cp "$CP" -d "$BUILD_DIR" "$SRC_DIR/CLogSCalculator.java"

echo "Creating clogs_calculator.jar..."
jar cfe "$BUILD_DIR/clogs_calculator.jar" CLogSCalculator -C "$BUILD_DIR" CLogSCalculator.class

echo "Build complete:"
echo "  $BUILD_DIR/property_calculator.jar  (all 54 properties)"
echo "  $BUILD_DIR/clogs_calculator.jar     (cLogS only)"
