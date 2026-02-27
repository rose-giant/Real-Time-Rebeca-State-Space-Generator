# Quick Start

This quick start confirms that the artefact and its environment are fully
operational.

## Option A: Docker (5 minutes, Recommended)

### Requirements
- Docker

### Steps

From the artefact root directory:

1. Build the Docker image:

   docker build -t state-space-generator .

2. Run the tool on the motivating example:

   docker run --rm -v $(pwd):/artifact state-space-generator \
     examples/motivatingexample.rebeca 0 300

### Expected Outcome

- The tool terminates without error
- The number of states is printed to the console
- A DOT file representing the full state space is generated in the artefact directory called "output.dot"


## Option B: Java (Alternative)

### Requirements
- Java 17 or later

### Steps

From the artefact root directory, run:

java -jar ssgen-3.1.jar examples/motivatingexample.rebeca 0 300