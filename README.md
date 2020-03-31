# java11-netty-non-blocking-server-basics

* references
    * https://stackoverflow.com/questions/22842153/netty-4-buffers-pooled-vs-unpooled/22907131
    * https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html

## preface
* goals of this workshop:
    * introduction to non-blocking server implementation using Netty
    * understand internals of Netty: event loop, bootstrapping, callbacks
    * compare with: https://github.com/mtumilowicz/java12-nio-non-blocking-selector-server-workshop
    * introduction to KiTTY
    * write simple echo server with exemplary client
* workshops with hints: `workshop` package, answers: `answers`

## introduction
* asynchronous events can also have an ordered relationship
    * you generally get an answer to a question only after you have asked it, and you may be able to do something 
    else while you are waiting for it
    * consider email: you may or may not get a response to a message you have sent, or you may receive an unexpected 
    message even while sending one

## channel
* lifecycle
    * `ChannelRegistered` - registered to an EventLoop
    * `ChannelActive` - active (connected to its remote peer); ready to receive and send data
    * `ChannelInactive` - not connected to the remote peer.
    * `ChannelUnregistered` - not registered to an EventLoop
* `Channel` is a basic construct of Java NIO
    * an open connection to an entity (hardware device, file, network socket)  that is capable of performing I/O 
    operations (reading or writing)
    
## ChannelHandler
* supports almost any kind of action, example: converting data, handling exceptions etc.
    * help to separate business logic from networking code
    * is a generic container for any code that processes events
* is a kind of callback to be executed in response to a specific event
* when added to a `ChannelPipeline` - gets `ChannelHandlerContext` (binding between handler and the pipeline)
    * `ChannelHandlerContext` enables a `ChannelHandler` to interact with other handlers
    * `ChannelHandler` passes event to next `ChannelHandler` in pipeline using assigned `ChannelHandlerContext`
* lifecycle
    * each method accepts a `ChannelHandlerContext` argument
    * `handlerAdded` - called when added to a `ChannelPipeline`
    * `handlerRemoved` - called when removed from a `ChannelPipeline`
    * `exceptionCaught` - called if an error occurs in the `ChannelPipeline` during processing
* interfaces:
    * `ChannelInboundHandler`
        * implementation: `ChannelInboundHandlerAdapter`
    * `ChannelOutboundHandler`
        * implementation: `ChannelOutboundHandlerAdapter`
* if the implementation is annotated as `@Sharable,` it means handler can be added to multiple `ChannelPipelines`

## ChannelHandlerContext
* is an association between a `ChannelHandler` and a `ChannelPipeline`
* is created whenever a `ChannelHandler` is added to a pipeline
* manages the interaction of its associated `ChannelHandler` with others in the same pipeline
* has connection from its `ChannelHandler` to the next `ChannelHandler`

## ChannelPipeline
```
                                               I/O Request
                                          via Channel or
                                      ChannelHandlerContext
                                                    |
+---------------------------------------------------+---------------+
|                           ChannelPipeline         |               |
|                                                  \|/              |
|    +---------------------+            +-----------+----------+    |
|    | Inbound Handler  N  |            | Outbound Handler  1  |    |
|    +----------+----------+            +-----------+----------+    |
|              /|\                                  |               |
|               |                                  \|/              |
|    +----------+----------+            +-----------+----------+    |
|    | Inbound Handler N-1 |            | Outbound Handler  2  |    |
|    +----------+----------+            +-----------+----------+    |
|              /|\                                  .               |
|               .                                   .               |
|               .                                   .               |
|               .                                  \|/              |
|    +----------+----------+            +-----------+----------+    |
|    | Inbound Handler  2  |            | Outbound Handler M-1 |    |
|    +----------+----------+            +-----------+----------+    |
|              /|\                                  |               |
|               |                                  \|/              |
|    +----------+----------+            +-----------+----------+    |
|    | Inbound Handler  1  |            | Outbound Handler  M  |    |
|    +----------+----------+            +-----------+----------+    |
|              /|\                                  |               |
+---------------+-----------------------------------+---------------+
                |                                  \|/
+---------------+-----------------------------------+---------------+
|               |                                   |               |
|       [ Socket.read() ]                    [ Socket.write() ]     |
|                                                                   |
|  Netty Internal I/O Threads (Transport Implementation)            |
+-------------------------------------------------------------------+
```
* beginning: the inbound entry (the left side)
* the end: outbound entry (the right side)
* `ChannelPipeline` is primarily a series of `ChannelHandlers` with API for propagating the inbound and outbound 
events along the chain
* when a `Channel` is created, is assigned a new `ChannelPipeline`
* `ChannelHandlers` are installed in the `ChannelPipeline` as follows
    1. `ChannelInitializer` implementation is registered with a `ServerBootstrap`
    1. `ChannelInitializer.initChannel()` installs a custom set of `ChannelHandlers` in the pipeline
    1. The `ChannelInitializer` removes itself from the pipeline
* `ChannelHandlers` receive events, execute the logic and pass the data to the next handler in the chain
    * order of execution is determined by the order in which they were added
* both inbound and outbound handlers can be installed in the same pipeline
    * depending on its origin, an event will be handled by either a `ChannelInboundHandler` or a 
    `ChannelOutboundHandler`
        * handler might implement both interfaces
    * as the pipeline propagates an event, it determines whether the type of the next `ChannelHandler` matches the 
    direction of movement
        * if not skips that `ChannelHandler` and proceeds to the next one
* two ways of sending messages:
    * direct write to the `Channel` - message starts from the tail
    * write to a `ChannelHandlerContext` (associated with a `ChannelHandler`) - message starts from the next handler
  
## Resource management
* Netty’s alternative to `ByteBuffer` is `ByteBuf`
* heap buffers
    * most frequently used `ByteBuf` pattern
    * stores the data in the heap space
* direct buffers
    * `ByteBuf` pattern
    * allows a JVM implementation to allocate memory via native calls
    * aims to avoid copying the buffer’s contents to (or from) an intermediate buffer before (or after) each 
    invocation of a native I/O operation
* two `ByteBufAllocator` implementations
    * `PooledByteBufAllocator`
        * pools `ByteBuf` instances to improve performance and minimize memory fragmentation
        * uses memory allocation `jemalloc`
    * `UnpooledByteBufAllocator`
        * doesn’t pool `ByteBuf` instances and returns a new instance every time it’s called
* Netty uses reference counting to handle pooled ByteBufs
* whenever calling `ChannelInboundHandler.channelRead()` or `ChannelOutboundHandler.write()`, you need to ensure that 
there are no resource leaks
* `SimpleChannelInboundHandler` (`ChannelInboundHandler` implementation) will automatically release a message once 
it’s consumed by `channelRead0()`
    * `channelRead()` has in finally block: `ReferenceCountUtil.release()`
    * if the message reaches the actual transport layer, it will be released automatically when it’s written or 
    the `Channel` is closed
    
## future
* JDK's `java.util.concurrent.Future` provided implementations allow you only to check manually whether the operation 
has completed or to block until it does
* callback is simply a method that has been provided to another method
    * this enables the latter to call the former at an appropriate time
    * represents one of the most common ways to notify an interested party that an operation has completed
    * Netty uses callbacks internally when handling events
        * example: `ChannelInboundHandlerAdapter.channelActive(ChannelHandlerContext)` is called when 
        a new connection is established
* each of Netty’s outbound I/O operations returns a `ChannelFuture`
    * acts as a placeholder for the result of an asynchronous operation
    * it will complete at some point in the future and provide access to the result
* `ChannelFuture` provides additional methods that allow us to register one or more `ChannelFutureListener` instances
    * `ChannelFutureListener` is a more elaborate version of a callback
    * listener’s callback method, `operationComplete()`, is called when the operation has completed
        * listener can then determine whether the operation completed successfully or with an error
        * if the latter, we can retrieve the Throwable that was produced

## EventLoop
* basic idea of an event loop
    ```
    while (!terminated) {
        var readyEvents = blockUntilEventsReady();
        for (var event: readyEvents) {
            event.run();
        }
    }
    ```
* Netty’s core abstraction for handling events that occur during the lifetime of a connection
* extends `ScheduledExecutorService`
* has its own independent task queue
* is bound to a single `Thread` for its lifetime
    * all I/O events processed are handled on its dedicated `Thread`
    * eliminates any concern you might have about synchronization in your `ChannelHandlers`
* `Channel` is registered for its lifetime with a single `EventLoop`
    * a single `EventLoop` may be assigned to one or more `Channels`
        * `ThreadLocal` will be the same for all associated `Channels`
* any long-running task put in the execution queue will block any other task from executing on the same thread
* execution logic
    1. task to be executed in the `EventLoop`: `Channel.eventLoop().execute(Task)`
    1. calling thread is the one assigned to the `EventLoop`?
        * yes: you are in the appropriate `EventLoop` and the task can be executed directly
        * no: you are not in the appropriate `EventLoop` - queue the task in the appropriate loop
            * task will be executed when the EventLoop processes its events again
* an `EventLoopGroup` contains one or more `EventLoops`
    * is responsible for allocating an `EventLoop` to each newly created `Channel`
* `EventLoopGroup` needs to be shutdown gracefully - it has to handle any pending events and tasks and subsequently 
release all active threads
    * `EventLoopGroup.shutdownGracefully()`
        * an asynchronous operation - either block until it completes or register a listener to be notified of 
        completion
        
## bootstrapping
* `ServerBootstrap`
    * `ServerBootstrap group(EventLoopGroup)`
    * `ServerBootstrap channel(Class<? extends ServerChannel>)` - `ServerChannel` to be instantiated
        * parent channel - accepts connections from clients
    * `ServerBootstrap localAddress(SocketAddress)` - local address the server should be bound to
    * `ServerBootstrap childHandler(ChannelHandler childHandler)` - `ChannelHandler` added to the `ChannelPipeline`
        * child channels - converses with clients
    * `ChannelFuture bind()` - binds the `ServerChannel`
* `Bootstrap`: clients
    * `Bootstrap group(EventLoopGroup)`
    * `Bootstrap channel(Class<? extends Channel>)`
    * `Bootstrap handler(ChannelHandler)` handler to receive event notification
    * `Bootstrap remoteAddress(SocketAddress)`
    * `ChannelFuture connect()` - connects to the remote peer
    
## Exception handling
* if an exception is thrown during processing of an inbound event, it will start to flow through the ChannelPipeline 
starting at the point in the ChannelInboundHandler where it was triggered
* `ChannelInboundHandler.exceptionCaught(ChannelHandlerContext ctx, Throwable cause)`
    * default implementation forwards the current exception to the next handler in the pipeline
    * if an exception reaches the end of the pipeline, it’s logged as unhandled.
* by default, a handler will forward the invocation of a handler method to the next one in the chain
* therefore, if `exceptionCaught()` is not implemented somewhere along the chain, exceptions received will travel to 
the end of the ChannelPipeline and will be logged
* For this reason, your application should supply at least one ChannelHandler that implements exceptionCaught()