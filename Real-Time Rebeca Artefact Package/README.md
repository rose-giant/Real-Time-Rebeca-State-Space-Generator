# State Space Generator Artefact

This artefact provides a command-line state space generator for Rebeca models,
used to produce the experimental results reported in the paper.

The artefact is packaged as a Docker image to eliminate the need for installing
Java. Direct execution using Java is also supported.

## Artefact Structure

artifact/
├── Dockerfile
├── ssgen-3.1.jar          # State space generator executable
├── examples/                     # Example Rebeca models used in the paper
│   └── motivatingexample.rebeca
├── README.md
└── QUICKSTART.md

## Option A: Docker (Recommended)

### Requirements
- Docker (Linux, macOS, or Windows)

### Build
From the artefact root directory, run:

docker build -t state-space-generator .

### Run
Arguments passed to `docker run` are forwarded to the tool.

docker run --rm -v $(pwd):/artifact state-space-generator \
  examples/motivatingexample.rebeca 0 300

The generated DOT file representing the state space is written to the artefact
directory on the host machine.

Notice. replace motivatingexample with each of the test cases to check their outputs. 

## Option B: Java (Alternative)

### Requirements
- Java 17 or later
- Tested on Linux and Windows

### Usage

java -jar ssgen-3.1.jar <model> <min> <max>

#### Arguments
- model: path to a Rebeca model
- min: lower bound for state-space exploration
- max: upper bound for state-space exploration

### Example

java -jar ssgen-3.1.jar examples/motivatingexample.rebeca 0 300

## Expected Outcome

For both execution modes:

- The tool terminates without error
- The console prints the number of states
- A DOT file containing the full state-space representation is generated
- The DOT file can be visualised using Graphviz (e.g. https://dreampuf.github.io/GraphvizOnline/)
- The reported result corresponds to the motivating example discussed in Section 3 of the paper
- The generated state space includes TAU and message transitions for full precision
- All possible interleavings among events are included

In the paper, only time transitions are shown for clarity.