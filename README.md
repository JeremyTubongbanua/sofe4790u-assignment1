# sofe4790u-assignment1

## Introduction

This is my submission for Distributed Systems Assignment 1 (SOFE 4790U).

My project is a chat room hosted on a server and clients can connect to it and send messages to each other. I also implemented a feature for sending files to the other clients.

## How To Run It

### Linux/MacOS Scripts

I wrote some scripts to make it easier to compile and run the server and client.

Instructions:

1. Open 4 terminals (1 for the server and 3 for the clients)

1. On the first terminal, run the server by running `tools/build_and_run_server.sh`

1. On the other 3 terminals, run the clients by running `tools/build_and_run_client1.sh`, `tools/build_and_run_client2.sh`, and `tools/build_and_run_client3.sh`

### Manual Compilation

If you want to compile and run the server and clients manually, you can follow these steps:

1. Compile and run server

```bash
cd server
javac -d out ../sofe4790u/a1/server/Server.java
jar cfe server.jar sofe4790u.a1.server.Server -C out sofe4790u
java -jar server.jar 5555 5556
```

2. Compile and run client1

```bash
cd client1
javac -d out ../sofe4790u/a1/client/Client.java
jar cfe client.jar sofe4790u.a1.client.Client -C out sofe4790u
java -jar client.jar localhost 5555 5556 client1
```

3. Compile and run client2

```bash
cd client2
javac -d out ../sofe4790u/a1/client/Client.java
jar cfe client.jar sofe4790u.a1.client.Client -C out sofe4790u
java -jar client.jar localhost 5555 5556 client2
```

4. Compile and run client3

```bash
cd client3
javac -d out ../sofe4790u/a1/client/Client.java
jar cfe client.jar sofe4790u.a1.client.Client -C out sofe4790u
java -jar client.jar localhost 5555 5556 client3
```
