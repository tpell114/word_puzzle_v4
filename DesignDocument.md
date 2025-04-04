# Protocol Design Document: Multiplayer Criss-Cross Word Puzzle (P2P with FIFO-Total Order Broadcast)

```
This doc outlines both our progress and the intended complete design
```

## Protocol Design
### Core Components: 
* Server: Manages gamelifecycle (start/join) and shares the initial puzzle state **(implemented)**
* Peers: Maintain PuzzleObject replicas and broadcast guesses **(partially implemented)**
* BroadcastHandler: Ensures FIFO-total order delivery with lamport clocks **(missing acks for total order)**

# Intended Data Flow
1. Server creates initial puzzleSlave state to all peers
2. Guesses are broadcasted by the peers
3. Peers deliver the guesses only when all prior messages from the sender are delivered
4. All peers have acknowledged the message

# Key Unfinished Parts
|Component|Missing piece|Why it matters|
|----|----|-----|
|BroadcastHandler|ack tracking|ensures all peers deliver messages in same order|
PuzzleObject|sync fixes|prevents different puzzle states across peers|
|Client|puzzle game start logic|ensure all peers initialize with identical state|
