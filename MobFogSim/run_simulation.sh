#!/bin/bash

# Base command
BASE_CMD="java -Xmx10g -Dfile.encoding=UTF-8 -classpath bin:jars/cloudsim-3.0.3-sources.jar:jars/cloudsim-3.0.3.jar:jars/cloudsim-examples-3.0.3-sources.jar:jars/cloudsim-examples-3.0.3.jar:jars/commons-math3-3.5/commons-math3-3.5.jar:jars/guava-18.0.jar:jars/json-simple-1.1.1.jar:jars/junit.jar:jars/org.hamcrest.core_1.3.0.v201303031735.jar:../Downloads/jackson-core-2.17.3.jar:../Downloads/jackson-databind-2.17.3.jar:../Downloads/jackson-annotations-2.17.3.jar org.fog.vmmobile.AppExample 1"

# Fixed parameters for each iteration
FIXED_PARAMS_1="0 0 9 11 0 0 0 61"
FIXED_PARAMS_2="1 0 9 11 0 0 0 61"

# Number of iterations
NUM_ITERATIONS=2

# Create outputs directory if it doesn't exist
mkdir -p outputs

# Loop to execute the command 2 times with different fixed parameters
for ((i=1; i<=NUM_ITERATIONS; i++))
do
  # Generate a random seed
  SEED=$RANDOM

  # Set fixed parameters based on iteration
  if [ $i -eq 1 ]; then
    FIXED_PARAMS=$FIXED_PARAMS_1
  elif [ $i -eq 2 ]; then
    FIXED_PARAMS=$FIXED_PARAMS_2
  fi

  # Construct the full command
  CMD="$BASE_CMD $SEED $FIXED_PARAMS"

  # Execute the command
  echo "Executing: $CMD"
  $CMD

  # Move the out.txt file to the outputs directory with the seed in the filename
  mv out.txt outputs/out_$SEED.txt

  # Clean up before the next execution
  make clean
done