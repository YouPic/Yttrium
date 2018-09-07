# Yttrium Server Library

Yttrium is a set of mostly independent libraries that together form the base of a server stack.
The project currently consists of the following modules:
 - Yttrium-Core 
   - Functionality used by most other modules and serialization handling.
 - Yttrium-Router
   - A library for routing server requests to web server calls. It creates an internal description of each route and supports exporting the functionality in different ways, while also generating documentation.
 - Yttrium-Server
   - Manages creating servers that export the functionality from routes using different protocols. Currently supports HTTP and a custom binary format.
 - Yttrium-Codegen
   - Takes descriptions of data structures and generates very efficient encoding and decoding from and to different formats.
 - Yttrium-Metrics
   - Collects metrics from different servers and backend modules, analyses them and displays them.
   - Provides a metrics server that stores and analyses metric data. This data can then be retrieved and shown by GUI clients.
 - [MySQL-Async](https://github.com/YouPic/MySQL-Async)
   - A simple, fast, fully asynchronous MySQL driver for the JVM.
   - Provides a simple DSL for easy construction of queries from code.
 - [Redis-Async](https://github.com/YouPic/Redis-Async)
   - A non-blocking Redis driver for the JVM, focused on reducing performance overhead.
