# java11-netty-non-blocking-server-basics

* references
    * https://stackoverflow.com/questions/22842153/netty-4-buffers-pooled-vs-unpooled/22907131

## preface
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
    * an open connection to an entity (hardware device, file, network socket) 
    that is capable of performing I/O operations (reading or writing)
    
### ChannelHandler
* supports almost any kind of action, example: converting data, handling exceptions etc.
    * help to separate business logic from networking code
* is a kind of callback to be executed in response to a specific event
    * implemented to hook into the event lifecycle and provide custom logic
* when added to a `ChannelPipeline` - gets `ChannelHandlerContext` (binding between handler and the pipeline)
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
        
### Resource management
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
        * pools ByteBuf instances to improve performance and minimize memory fragmentation
        * uses memory allocation `jemalloc`
    * `UnpooledByteBufAllocator`
        * doesn’t pool ByteBuf instances and returns a new instance every time it’s called
* whenever you act on data by calling `ChannelInboundHandler.channelRead()` or `ChannelOutboundHandler.write()`, you 
need to ensure that there are no resource leaks
* Netty uses reference counting to handle pooled ByteBufs
* `SimpleChannelInboundHandler` (`ChannelInboundHandler` implementation) will automatically release a message once 
it’s consumed by `channelRead0()`
    * `channelRead()` has in finally block: `ReferenceCountUtil.release()`
    * if the message reaches the actual transport layer, it will be released automatically when it’s written or 
    the Channel is closed
    
### ChannelPipeline
* ChannelPipeline
    * ChannelPipeline provides a container for a chain of ChannelHandlers and defines an API for propagating the 
    flow of inbound and outbound events along the chain
    * When a Channel is created, it is automatically assigned its own ChannelPipeline
    * ChannelHandlers are installed in the ChannelPipeline as follows:
        * A ChannelInitializer implementation is registered with a ServerBootstrap
        * When ChannelInitializer.initChannel() is called, the ChannelInitializer installs a custom set of 
        ChannelHandlers in the pipeline
        * The ChannelInitializer removes itself from the ChannelPipeline
    * ChannelHandler has been designed specifically to support a broad range of uses, and you can think of it as a 
    generic container for any code that processes events (including data) coming and going through the ChannelPipeline
    * movement of an event through the pipeline is the work of the ChannelHandlers that have been installed during 
    the initialization, or bootstrapping phase of the application
    * These objects receive events, execute the processing logic for which they have been implemented, and pass the 
    data to the next handler in the chain
    * The order in which they are executed is determined by the order in which they were added
    * From the point of view of a client application, events are said to be outbound if the movement is from the 
    client to the server and inbound in the opposite case
        * both inbound and outbound handlers can be installed in the same pipeline
    * If a message or any other inbound event is read, it will start from the head of the pipeline and be passed 
    to the first ChannelInboundHandler
        * This handler may or may not actually modify the data, depending on its specific function, after which the 
        data will be passed to the next ChannelInboundHandler in the chain
        * Finally, the data will reach the tail of the pipeline, at which point all processing is terminated
    * The outbound movement of data (that is, data being written) is identical in concept. In this case, data flows 
    from the tail through the chain of ChannelOutboundHandlers until it reaches the head
        * Beyond this point, outbound data will reach the network transport, shown here as a Socket
        * Typically, this will trigger a write operation
    * There are two ways of sending messages in Netty:
        * You can write directly to the Channel - causes the message to start from the tail of the ChannelPipeline
        * write to a ChannelHandlerContext object associated with a ChannelHandler - causes the message to start 
        from the next handler in the ChannelPipeline
* Every new Channel that’s created is assigned a new ChannelPipeline
* Depending on its origin, an event will be handled by either a ChannelInboundHandler or a ChannelOutboundHandler
* Subsequently it will be forwarded to the next handler of the same supertype by a call to a ChannelHandlerContext 
implementation
* ChannelHandlerContext
    * A ChannelHandlerContext enables a ChannelHandler to interact with its ChannelPipeline and with other handlers
    * A handler can notify the next ChannelHandler in the ChannelPipeline and even dynamically modify the 
    ChannelPipeline it belongs to
* ChannelPipeline is primarily a series of ChannelHandlers
* ChannelPipeline also provides methods for propagating events through the ChannelPipeline itself
* If an inbound event is triggered, it’s passed from the beginning to the end of the ChannelPipeline
* Netty always identifies the inbound entry to the ChannelPipeline (the left side) as the beginning and the 
outbound entry (the right side) as the end
* As the pipeline propagates an event, it determines whether the type of the next ChannelHandler in the pipeline 
matches the direction of movement
    * If not, the ChannelPipeline skips that ChannelHandler and proceeds to the next one, until it finds one 
    that matches the desired direction
        * Of course, a handler might implement both ChannelInboundHandler and ChannelOutboundHandler interfaces
* summary
    * A ChannelPipeline holds the ChannelHandlers associated with a Channel.
    * A ChannelPipeline can be modified dynamically by adding and removing ChannelHandlers as needed.
    * ChannelPipeline has a rich API for invoking actions in response to inbound and outbound events.

### ChannelHandlerContext
* ChannelHandlerContext represents an association between a ChannelHandler and a ChannelPipeline and is created 
whenever a ChannelHandler is added to a ChannelPipeline
* The primary function of a ChannelHandlerContext is to manage the interaction of its associated ChannelHandler 
with others in the same ChannelPipeline
* ChannelHandlerContext has numerous methods, some of which are also present on Channel and on ChannelPipeline 
itself, but there is an important difference
    * If you invoke these methods on a Channel or ChannelPipeline instance, they propagate through the entire 
    pipeline
    * The same methods called on a ChannelHandlerContext will start at the current associated ChannelHandler and 
    propagate only to the next ChannelHandler in the pipeline that is capable of handling the event
    * The ChannelHandlerContext associated with a ChannelHandler never changes, so it’s safe to cache a reference 
    to it.
* Using ChannelHandlerContext
    * ChannelHandler passes event to next ChannelHandler in ChannelPipeline using assigned ChannelHandlerContext
    * ChannelHandlerContext has connection from its ChannelHandler to the next ChannelHandler
    
## future
* callback is simply a method, a reference to which has been provided to another method
    * This enables the latter to call the former at an appropriate time
    * represents one of the most common ways to notify an interested party that an operation has completed
    * Netty uses callbacks internally when handling events; when a callback is triggered the event can be handled by 
    an implementation of interface ChannelHandler
    * an example - hooks from ChannelInboundHandlerAdapter: channelActive(ChannelHandlerContext) is called when a new 
    connection is established
* Each of Netty’s outbound I/O operations returns a ChannelFuture
* ChannelFuture provides additional methods that allow us to register one or more ChannelFutureListener instances
    * ChannelFutureListener is a more elaborate version of a callback
    * listener’s callback method, `operationComplete()`, is called when the operation has completed
        * listener can then determine whether the operation completed successfully or with an error
        * If the latter, we can retrieve the Throwable that was produced
* provides another way to notify an application when an operation has completed
* acts as a placeholder for the result of an asynchronous operation
* it will complete at some point in the future and provide access to the result
* JDK ships with interface java.util.concurrent.Future, but the provided implementations allow you only to check 
manually whether the operation has completed or to block until it does

### event loop
* EventLoop defines Netty’s core abstraction for handling events that occur during the lifetime of a connection 
An EventLoopGroup contains one or more EventLoops.
    * An EventLoop is bound to a single Thread for its lifetime.
    * All I/O events processed by an EventLoop are handled on its dedicated Thread.
    * A Channel is registered for its lifetime with a single EventLoop.
    * A single EventLoop may be assigned to one or more Channels.
* under the covers, an EventLoop is assigned to each Channel to handle all of the events, including
  * registration of interesting events
  * dispatching events to ChannelHandlers
  * scheduling further actions
* EventLoop itself is driven by only one thread that handles all of the I/O events for one Channel and does not 
change during the lifetime of the EventLoop
    * This simple and powerful design eliminates any concern you might have about synchronization in your 
    ChannelHandlers, so you can focus on providing the right logic to be executed when there is interesting data to 
    process
    * Note that this design, in which the I/O for a given Channel is executed by the same Thread, virtually eliminates 
    the need for synchronization
* Interface EventLoop
    * basic idea of an event loop
        ```
        while (!terminated) {
            List<Runnable> readyEvents = blockUntilEventsReady();
            for (Runnable ev: readyEvents) {
                ev.run();
            }
        }
        ```
    * EventLoop is powered by exactly one Thread that never changes, and tasks (Runnable or Callable) can be submitted 
    directly to EventLoop implementations for immediate or scheduled execution
    * Depending on the configuration and the available cores, multiple EventLoops may be created in order to optimize 
    resource use, and a single EventLoop may be assigned to service multiple Channels
    * Netty’s EventLoop, while it extends ScheduledExecutorService, defines only one method, parent()
        * is intended to return a reference to the EventLoopGroup to which the current EventLoop implementation instance 
        belongs
    * Events and tasks are executed in FIFO order
* Thread management
    * The superior performance of Netty’s threading model hinges on determining the identity of the currently 
    executing Thread
        * that is, whether or not it is the one assigned to the current Channel and its EventLoop (EventLoop is 
        responsible for handling all events for a Channel during its lifetime)
    * If the calling Thread is that of the EventLoop, the code block in question is executed
        * Otherwise, the EventLoop schedules a task for later execution and puts it in an internal queue
        * When the EventLoop next processes its events, it will execute those in the queue
    * This explains how any Thread can interact directly with the Channel without requiring synchronization in the 
    ChannelHandlers
    * Note that each EventLoop has its own task queue, independent of that of any other EventLoop
    * Never put a long-running task in the execution queue, because it will block any other task from executing on the 
    same thread.
        * If you must make blocking calls or execute long-running tasks, we advise the use of a dedicated EventExecutor.
* EventLoopGroup is responsible for allocating an EventLoop to each newly created Channel
* Each EventLoop handles all events and tasks for all the channels assigned to it. Each EventLoop is 
associated with one Thread.

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

### Exception handling
* if an exception is thrown during processing of an inbound event, it will start to flow through the ChannelPipeline 
starting at the point in the ChannelInboundHandler where it was triggered
* `ChannelInboundHandler.exceptionCaught(ChannelHandlerContext ctx, Throwable cause)`
    * default implementation forwards the current exception to the next handler in the pipeline
    * if an exception reaches the end of the pipeline, it’s logged as unhandled.
* by default, a handler will forward the invocation of a handler method to the next one in the chain
* therefore, if exceptionCaught() is not implemented somewhere along the chain, exceptions received will travel to 
the end of the ChannelPipeline and will be logged
* For this reason, your application should supply at least one ChannelHandler that implements exceptionCaught()