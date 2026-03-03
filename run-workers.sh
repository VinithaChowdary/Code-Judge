#!/bin/bash
echo "🚀 Starting 3 workers..."

java -cp bin:lib/* WorkerMain &
WORKER1=$!

java -cp bin:lib/* WorkerMain &
WORKER2=$!

java -cp bin:lib/* WorkerMain &
WORKER3=$!

echo "✅ Workers started!"
echo "Worker PIDs: $WORKER1, $WORKER2, $WORKER3"
echo "Press Ctrl+C to stop all workers."

trap "kill $WORKER1 $WORKER2 $WORKER3" EXIT

wait
