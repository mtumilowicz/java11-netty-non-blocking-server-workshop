# java11-netty-non-blocking-server-basics

* references
    * https://stackoverflow.com/questions/22842153/netty-4-buffers-pooled-vs-unpooled/22907131
    * https://netty.io/4.1/api/io/netty/channel/ChannelPipeline.html

## preface

## introduction
* consider email: you may or may not get a response to a message you have sent, or you may receive an unexpected 
message even while sending one
* asynchronous events can also have an ordered relationship
    * you generally get an answer to a question only after you have asked it, and you may be able to do something 
    else while you are waiting for it
* Netty’s core components
    * Channels
    * Callbacks
    * Futures
    * Events and handlers

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
        List<Runnable> readyEvents = blockUntilEventsReady();
        for (Runnable ev: readyEvents) {
            ev.run();
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

## bootstrapping
* NIO transport refers to a transport that’s mostly  identical to TCP except for server-side performance enhancements 
provided by the Java NIO implementation
* The following steps are required in bootstrapping:
    * create a ServerBootstrap instance to bootstrap and bind the server
    * create and assign an NioEventLoopGroup instance to handle event processing, such as accepting new connections 
    and reading/writing data
    * create and assign an NioServerSocketChannel as a channel implementation to be used 
* when a new connection is accepted, a new child Channel will be created, and the `ChannelInitializer` will add an 
instance of your `EchoServerHandler` to the Channel’s `ChannelPipeline`
* bootstrapping a client is similar to bootstrapping a server - instead of binding to a listening port the client 
connects to a remote address
* a server devotes a parent channel to accepting connections from clients and creating child channels for 
conversing with them
    * a client will most likely require only a single, non-parent channel for all network interactions

* Bootstrapping clients and connectionless protocols
    * group(EventLoopGroup) Sets the EventLoopGroup that will handle all events for the Channel
    * channel(Class<? extends C>) specifies the Channel implementation class (C extends Channel)
    * handler(ChannelHandler) Sets the ChannelHandler that’s added to the
      ChannelPipeline to receive event notification
    * remoteAddress(SocketAddress) - Sets the remote address
    * ChannelFuture connect() - Connects to the remote peer
* Bootstrapping servers
    * group Sets the EventLoopGroup to be used by the ServerBootstrap . This
      EventLoopGroup serves the I/O of the ServerChannel and accepted
      Channel s.
    * channel Sets the class of the ServerChannel to be instantiated.
    * localAddress Specifies the local address the ServerChannel should be bound to
    * childHandler Sets the ChannelHandler that’s added to the ChannelPipeline of
      accepted Channels
    * The difference between handler() and childHandler() is that the former adds a handler that’s processed by the 
    accepting ServerChannel, whereas childHandler() adds a handler that’s processed by an accepted Channel, which 
    represents a socket bound to a remote peer
    * bind Binds the ServerChannel and returns a ChannelFuture , which is notified
      once the connection operation is complete (with the success or error result).
    
* ServerChannel implementations are responsible for creating child Channels, which represent accepted 
connections
    1. A ServerChannel is created when bind() is called
    1. A new Channel is created by the ServerChannel when a connection is accepted
    
* Suppose your server is processing a client request that requires it to act as a client to a third system
    * In such cases you’ll need to bootstrap a client Channel from a ServerChannel
* You could create a new Bootstrap, but this is not the most efficient solution, as it would require you to define 
another EventLoop for the new client Channel
    * This would produce additional threads, necessitating context switching when exchanging data between the 
    accepted Channel and the client Channel
    * A better solution is to share the EventLoop of the accepted Channel by passing it to the group() method of the 
    Bootstrap
        * Because all Channels assigned to an EventLoop use the same thread, this avoids the extra thread creation 
        and related context-switching mentioned previously
* how can you do this if you can set only one ChannelHandler during the bootstrapping process?
    * Netty supplies a special subclass of ChannelInboundHandlerAdapter
    * provides an easy way to add multiple ChannelHandlers to a ChannelPipeline
    * You simply provide your implementation of ChannelInitializer to the bootstrap, and once the Channel is 
    registered with its EventLoop your version of initChannel() is called
    * After the method returns, the ChannelInitializer instance removes itself from the ChannelPipeline
* Shutdown
    * you need to shut down the EventLoopGroup, which will handle any pending events and tasks and subsequently release 
    all active threads
        * This is a matter of calling EventLoopGroup.shutdownGracefully()
            * This call will return a Future, which is notified when the shutdown completes
            * Note that shutdownGracefully() is also an asynchronous operation, so you’ll need to either block until it 
            completes or register a listener with the returned Future to be notified of completion

## Exception handling
* if an exception is thrown during processing of an inbound event, it will start to flow through the ChannelPipeline 
starting at the point in the ChannelInboundHandler where it was triggered
* `ChannelInboundHandler.exceptionCaught(ChannelHandlerContext ctx, Throwable cause)`
    * default implementation forwards the current exception to the next handler in the pipeline
    * if an exception reaches the end of the pipeline, it’s logged as unhandled.
* by default, a handler will forward the invocation of a handler method to the next one in the chain
* therefore, if exceptionCaught() is not implemented somewhere along the chain, exceptions received will travel to 
the end of the ChannelPipeline and will be logged
* For this reason, your application should supply at least one ChannelHandler that implements exceptionCaught()