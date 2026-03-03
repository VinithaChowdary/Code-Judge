#!/bin/sh

javac Solution.java 2> compile_error.txt

if [ $? -ne 0 ]; then
  echo "COMPILATION_ERROR"
  cat compile_error.txt
  exit 1
fi

timeout 5 java Solution < input.txt > output.txt 2>&1

EXIT_CODE=$?

if [ $EXIT_CODE -eq 124 ]; then
  echo "TIME_LIMIT_EXCEEDED"
elif [ $EXIT_CODE -ne 0 ]; then
  echo "RUNTIME_ERROR"
else
  cat output.txt
fi
