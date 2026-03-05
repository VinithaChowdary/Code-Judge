# Code Judge

A scalable, distributed, and secure remote code execution engine and Online Judge. 

This project receives code submissions, manages them in a PostgreSQL database, queues them via RabbitMQ for asynchronous processing, and securely evaluates the code inside isolated Docker containers. Redis is utilized for request rate-limiting and handling transient system failures.

---

## Prerequisites

Ensure you have the following installed on your system:
- **Java 17+**
- **Docker** (For secure code sandboxing)
- **PostgreSQL** (For storing submissions and test cases)
- **RabbitMQ** (Message broker for worker queues)
- **Redis** (For rate limiting and reliability/retries)
- **cURL** (Or any API testing tool like Postman)

---

## 1. Setup & Installation

### A. Database Initialization (PostgreSQL)

1. Connect to your PostgreSQL instance and create the database:
   ```sql
   CREATE DATABASE code_judge;
   ```
2. Connect to the `code_judge` database and create the required tables:

   ```sql
   -- Table for storing submissions
   CREATE TABLE submissions (
       id SERIAL PRIMARY KEY,
       problem_id INT NOT NULL,
       language VARCHAR(50) NOT NULL,
       source_code TEXT NOT NULL,
       status VARCHAR(50) DEFAULT 'PENDING',
       output TEXT
   );

   -- Table for storing test cases
   CREATE TABLE test_cases (
       id SERIAL PRIMARY KEY,
       problem_id INT NOT NULL,
       input TEXT NOT NULL,
       expected_output TEXT NOT NULL
   );
   ```

3. Insert some dummy test cases for Problem ID `1` to test the system:
   ```sql
   INSERT INTO test_cases (problem_id, input, expected_output) VALUES 
       (1, '2 3', '5'),
       (1, '10 20', '30');
   ```

*(Note: Verify your database credentials in `src/DatabaseUtil.java`. Default configurations target `localhost:5432`, db: `code_judge`, user: `vinithachowdary`, password: `mypassword`)*

### B. Docker Sandbox Setup

We use Docker to safely execute untrusted user code. Build the sandbox image containing the compilation and sandboxing scripts.

Run this command from the root of your project:
```bash
docker build -t code-judge-sandbox .
```

---

## 2. Compilation

Compile all the Java source files into the `bin/` directory, including external dependencies located in the `lib/` directory (RabbitMQ client, Jedis, PostgreSQL JDBC, etc.):

```bash
mkdir -p bin
javac -d bin -cp "bin:lib/*" src/*.java
```

*(For Windows, modify the classpath separator from `:` to `;` -> `"bin;lib/*"`)*

---

## 3. Running the System

### Start the API Server
Start the HTTP server which exposes the submission and polling endpoints on port `8081`.

```bash
java -cp "bin:lib/*" Main
```

### Start the Worker Nodes
Workers continuously poll the RabbitMQ Queue (`submission_queue`), pick up new code payloads, run them against Docker containers, and update the database. 

We have a script to run multiple workers in parallel (Horizontal Scaling):
```bash
./run-workers.sh
```

---

## 4. API Usage & Testing

### Submit Code (POST `/submit`)
The server enforces a combination of Application Security & Rate Limiting (5 requests per 60 seconds) and maximum payload sizes (50 KB).

```bash
curl -X POST http://localhost:8081/submit \
     -H "Content-Type: application/json" \
     -d '{
           "language": "java",
           "code": "import java.util.Scanner; public class Solution { public static void main(String[] args) { Scanner sc = new Scanner(System.in); int a = sc.nextInt(); int b = sc.nextInt(); System.out.println(a+b); } }"
         }'
```
**Response:**
```json
{ "submissionId": 1, "status": "PENDING" }
```

### Check Status (GET `/submission/{id}`)
Poll this endpoint using the `submissionId` received from your submission.

```bash
curl http://localhost:8081/submission/1
```
**Response (when complete):**
```json
{ "id": 1, "status": "ACCEPTED", "output": "All test cases passed" }
```

---

## 5. Load Testing & Reliability

Want to see horizontal scaling and rate-limiting limits in action?

Run the load test script. It will blast the system with 10 submissions concurrently:
```bash
./load-test.sh
```

You should see:
1. `Submissions` getting distributed among your 3 background workers evenly.
2. A couple of responses might hit the strictly enforced `Rate limit exceeded. Try again later` firewall, verifying our API Abuse configurations.

### **Features Implemented:**
* **Sandboxed Execution:** Untrusted code execution in short-lived, low-memory Docker environments (`network=none`, restricted CPU/Mem).
* **Asynchronous Queueing:** Producer-Consumer architecture using `RabbitMQ` decoupling API from Worker.
* **Horizontal Scaling:** Parallel background workers sharing traffic with fair queue limits (`basicQos(1)`).
* **Failure Handling:** Transient failure retries dynamically tracked via `Redis`.
* **Abuse Prevention:** IP-based API rate limiting, Memory/Payload Size limits, Language allow-listing.
