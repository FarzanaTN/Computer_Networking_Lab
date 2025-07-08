# 🖧 Computer Networking Lab Work

This repository contains my practical lab work from the **Computer Networking** course. It includes implementations of core networking concepts such as **socket programming**, **file transfer protocols**, **routing algorithms**, and a **multi-client chat application**.

These projects demonstrate how networking principles are applied programmatically, using Java's `socket`, `threading`, and other standard libraries.

---

## 📂 Contents

### 🔌 Socket Programming
- **TCP Client-Server Communication**
- **Multi-threaded TCP Server**

### 📁 File Transfer Protocols
- Stop-and-Wait Protocol
- Go-Back-N ARQ (Automatic Repeat reQuest)
- Simulated packet loss and ACK handling

### 🛰️ Routing Algorithms
- Distance Vector Routing
- Link State Routing
- Poisoned Reverse (to solve count-to-infinity)

### 💬 Chat Application
- Multi-client real-time chat
- Socket programming with threading

---

## 🚀 How to Run

Each sub-project has its own folder. Navigate into the desired folder and run the server and client code in separate terminals.

### Example: TCP Server-Client

**Step 1:** Start the server
```bash
javac Server.java && java Server
```

**Step 2:** Start the client
```bash
javac Client.java && java Client
```