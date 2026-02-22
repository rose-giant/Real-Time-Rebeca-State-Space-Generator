# State Space Generator Artefact

This artefact provides a command-line state space generator for Rebeca models,
used to produce the experimental results reported in the paper.

## Requirements
- Java 17 or later
- Tested on Linux and Windows

No installation is required.

## Artefact Structure

artifact/
├── modelchecker-3.1.jar     # State space generator executable
├── examples/               # Example Rebeca models used in the paper
│   └── motivatingexample.rebeca
├── README.md
└── QUICKSTART.md
└── run.sh

## Usage

java -jar modelchecker-3.1.jar <model> <min> <max>

### Arguments
- model: path to a Rebeca model
- min: lower bound for state-space exploration
- max: upper bound for state-space exploration

### Example

java -jar modelchecker-3.1.jar examples/motivatingexample.rebeca 0 300

### Expected Outcome

Expected output:
- The tool terminates without error
- The console prints the number of states
- The generated DOT file is written to the current directory as "output.dot"
- copy the dot file result and paste it in https://dreampuf.github.io/GraphvizOnline/ to see the visual representation of the state space
- The reported result corresponds to the motivating example discussed
    in Section 3 of the paper.
- All kinds of transitions including TAU and message transitions of the state space are included for a better precision, however, in the paper only time transitions were shown for the sake of simplicity
- All possible interleaving cases among events are observable as explained in the paper