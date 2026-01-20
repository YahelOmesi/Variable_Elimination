👤 Author
Yahel Omesi
📧 yahelomessi@gmail.com

# 🧠 Bayesian Network Inference Engine
AI Algorithms course project implementing probabilistic inference algorithms for Bayesian Networks.

## 🚀 Overview
This project implements a Bayesian Network inference engine in Java.  
The system parses a Bayesian Network from an XML file, processes probabilistic queries from an input file, and computes results using multiple inference strategies.

The project emphasizes algorithmic correctness, efficiency, and comparison between different inference approaches by explicitly measuring computational cost.

## 🧩 Supported Inference Algorithms
- **Simple Inference** – direct probability computation without optimizations  
- **Variable Elimination (fixed order)** – elimination using a predefined variable ordering (ABC order)  
- **Variable Elimination (heuristic order)** – optimized elimination order based on a heuristic strategy  

For each query, the engine reports:
- Final probability result  
- Number of addition operations  
- Number of multiplication operations  

## 🧠 Core Features
- Full Bayesian Network representation (variables, parents, CPTs, factors)  
- XML-based parsing of network structure and probability tables  
- Support for joint and conditional probability queries  
- Implementation of probabilistic inference algorithms  
- Heuristic optimization for Variable Elimination  
- Explicit performance analysis via operation counting  
- Input validation and structured query handling  


## 📁 File Structure
- `Ex1.java` – Program entry point  
- `BayesianNetwork.java` – Bayesian Network representation  
- `Variable.java` – Representation of a Bayesian variable  
- `Factor.java` – Factor and CPT representation  
- `XMLParser.java` – Parses Bayesian Network structure from XML  
- `InputReader.java` – Reads input files and queries  
- `Query.java` – Represents probabilistic queries  
- `QueryValidator.java` – Validates query correctness  
- `JointProbability.java` – Joint probability computations  
- `SimpleInference.java` – Basic inference algorithm  
- `VariableElimination.java` – Variable Elimination implementations  

## ▶️ Build & Run
Compile and run using Java 8:
```bash
javac *.java
java Ex1

📤 Output
The program writes results to an output file, including:
Query probability (formatted to 5 decimal places)
Number of addition operations
Number of multiplication operations



