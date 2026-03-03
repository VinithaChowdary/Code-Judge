#!/bin/bash
echo "Generating 10 submissions..."
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/submit \
       -H "Content-Type: application/json" \
       -d "{\"language\":\"java\", \"code\":\"public class Solution { public static void main(String[] args) { try { Thread.sleep(2000); } catch(Exception e){} System.out.println(1); } }\"}" > /dev/null &
done
wait
echo "All submissions sent!"
