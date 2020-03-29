# java11-netty-non-blocking-server-basics

* Consider
  email: you may or may not get a response to a message you have sent, or you may
  receive an unexpected message even while sending one.
* Asynchronous events can also
  have an ordered relationship. You generally get an answer to a question only after you
  have asked it, and you may be able to do something else while you are waiting for it
* Netty’s core components
    * Channels
    * Callbacks
    * Futures
    * Events and handlers
* Netty’s alternative to ByteBuffer is ByteBuf

## channel
* Channel is a basic construct of Java NIO
    * an open connection to an entity such as a hardware device, a file, a network socket, or a program component 
    that is capable of performing one or more distinct I/O operations (reading or writing)
    * think of a Channel as a vehicle for incoming (inbound) and outgoing (outbound) data
    * it can be open or closed
* Netty provides `ChannelFuture`, whose `addListener()` method registers a `ChannelFutureListener` to be notified 
when an operation has completed (whether or not successfully)
  * think of a `ChannelFuture` as a placeholder for the result of an operation that’s to be executed in the future
  * when exactly it will be executed may depend on several factors and thus be impossible to predict with 
  precision, but it is certain that it will be executed
  * furthermore, all operations belonging to the same Channel are guaranteed to be executed in the order in which 
  they were invoked
* `ChannelHandler`
    * from the application developer’s standpoint, the primary component of Netty is the `ChannelHandler`, which 
    serves as the container for all application logic that applies to handling inbound and outbound data
    * ChannelHandler methods are triggered by network events (where the term “event” is used very broadly)
    * ChannelHandler can be dedicated to almost any kind of action, such as converting data from one format to 
    another or handling exceptions thrown during processing
    * ChannelInboundHandler is a subinterface you’ll implement frequently
        * this type receives inbound events and data to be handled by your application’s
          business logic
        * you can also flush data from a ChannelInboundHandler when you’re sending a response to a connected client
        * the business logic of your application will often reside in one or more ChannelInboundHandlers
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
    * An event can be forwarded to the next handler in the current chain by using the ChannelHandlerContext that’s 
    supplied as an argument to each method
        * Because you’ll sometimes ignore uninteresting events, Netty provides the abstract base classes 
        ChannelInboundHandlerAdapter and ChannelOutboundHandlerAdapter
        * Each provides method implementations that simply pass the event to the next handler by calling the 
        corresponding method on the ChannelHandlerContext
        * You can then extend the class by overriding the methods that interest you
    * Although both inbound and outbound handlers extend ChannelHandler, Netty distinguishes implementations of 
    ChannelInboundHandler and ChannelOutboundHandler and ensures that data is passed only between handlers of the 
    same directional type
    * When a ChannelHandler is added to a ChannelPipeline, it’s assigned a Channel- HandlerContext, which represents 
    the binding between a ChannelHandler and the ChannelPipeline
        * Although this object can be used to obtain the underlying Channel, it’s mostly utilized to write outbound data
    * There are two ways of sending messages in Netty:
        * You can write directly to the Channel - causes the message to start from the tail of the ChannelPipeline
        * write to a ChannelHandlerContext object associated with a ChannelHandler - causes the message to start 
        from the next handler in the ChannelPipeline
    * These are the adapters you’ll call most often when creating your custom handlers:
        * ChannelHandlerAdapter
        * ChannelInboundHandlerAdapter
        * ChannelOutboundHandlerAdapter
        * ChannelDuplexHandlerAdapter
        
* All Netty servers require the following:
    * At least one ChannelHandler—This component implements the server’s processing of data received from the 
    client—its business logic
    * Bootstrapping—This is the startup code that configures the server. At a minimum, it binds the server to the port 
    on which it will listen for connection requests
* Every Channel has an associated ChannelPipeline, which holds a chain of ChannelHandler instances
    * By default, a handler will forward the invocation of a handler method to the next one in the chain
    * Therefore, if exceptionCaught()is not implemented somewhere along the chain, exceptions received will travel to 
    the end of the ChannelPipeline and will be logged
    * For this reason, your application should supply at least one ChannelHandler that implements exceptionCaught()
* ChanelHandlers
    * ChannelHandlers are invoked for different types of events.
    * Applications implement or extend ChannelHandlers to hook into the event lifecycle and provide custom 
    application logic
    * Architecturally, ChannelHandlers help to keep your business logic decoupled from networking code. This 
    simplifies development as the code evolves in response to changing requirements
### The ChannelHandler family
* The Channel lifecycle (Channel lifecycle states)
    * ChannelRegistered - The Channel is registered to an EventLoop.
    * ChannelActive - The Channel is active (connected to its remote peer). It’s now possible 
        to receive and send data.
    * ChannelInactive - The Channel isn’t connected to the remote peer.
    * ChannelUnregistered - The Channel was created, but isn’t registered to an EventLoop.
* The ChannelHandler lifecycle (ChannelHandler lifecycle methods)
    * Each method accepts a ChannelHandlerContext argument
    * handlerAdded - Called when a ChannelHandler is added to a ChannelPipeline
    * handlerRemoved - Called when a ChannelHandler is removed from a ChannelPipeline
    * exceptionCaught - Called if an error occurs in the ChannelPipeline during processing
    * Netty defines the following two important subinterfaces of ChannelHandler:
        * ChannelInboundHandler—Processes inbound data and state changes of all kinds
        * ChannelOutboundHandler—Processes outbound data and allows interception
        of all operations
* Interface ChannelInboundHandler
    * ChannelInboundHandler methods
        * channelRegistered - Invoked when a Channel is registered to its EventLoop and is able to handle I/O.
        * channelUnregistered - Invoked when a Channel is deregistered from its EventLoop and can’t handle any I/O.
        * channelActive - Invoked when a Channel is active; the Channel is connected/bound and ready.
        * channelInactive - Invoked when a Channel leaves active state and is no longer connected to its remote peer.
        * channelReadComplete - Invoked when a read operation on the Channel has completed.
        * channelRead - Invoked if data is read from the Channel.
        * channelWritabilityChanged - Invoked when the writability state of the Channel changes. 
            The user can ensure writes are not done too quickly (to avoid an OutOfMemoryError) or can resume writes 
            when the Channel becomes writable again. The Channel method isWritable() can be called to detect the 
            writability of the channel. The threshold for writability can be set via 
            Channel.config().setWriteHighWaterMark() and Channel.config().setWriteLowWaterMark().
        * userEventTriggered - Invoked when ChannelnboundHandler.fireUserEventTriggered() is called because a POJO was 
        passed through the ChannelPipeline.
    * These are called when data is received or when the state of the associated Channel changes
    * As we mentioned earlier, these methods map closely to the Channel lifecycle
    * When a ChannelInboundHandler implementation overrides channelRead(), it is responsible for explicitly releasing 
    the memory associated with pooled ByteBuf instances `ReferenceCountUtil.release(msg)`
        * But managing resources in this way can be cumbersome
        * A simpler alternative is to use SimpleChannelInboundHandler
        * SimpleChannelInboundHandler releases resources automatically, you shouldn’t store references to any 
        messages for later use, as these will become invalid
* Interface ChannelOutboundHandler
    * Outbound operations and data are processed by ChannelOutboundHandler
    * Its methods are invoked by Channel, ChannelPipeline, and ChannelHandlerContext
    * A powerful capability of ChannelOutboundHandler is to defer an operation or event on demand, which allows for 
    sophisticated approaches to request handling
        * If writing to the remote peer is suspended, for example, you can defer flush operations and resume them later
    * ChannelOutboundHandler methods
        * bind(ChannelHandlerContext,SocketAddress,ChannelPromise) - Invoked on request to bind the Channel to a local 
        address
        * connect(ChannelHandlerContext,SocketAddress,SocketAddress,ChannelPromise) - Invoked on request to connect the
        Channel to the remote peer
        * disconnect(ChannelHandlerContext,ChannelPromise) - Invoked on request to disconnect the Channel from the 
        remote peer
        * close(ChannelHandlerContext,ChannelPromise) - Invoked on request to close the Channel
        * deregister(ChannelHandlerContext,ChannelPromise) - Invoked on request to deregister the Channel from its 
        EventLoop
        * read(ChannelHandlerContext) - Invoked on request to read more data from the Channel
        * flush(ChannelHandlerContext) - Invoked on request to flush queued data to the remote peer through the Channel
        * write(ChannelHandlerContext,Object,ChannelPromise) - Invoked on request to write data through the Channel 
        to the remote peer
    * Most of the methods in ChannelOutboundHandler take a ChannelPromise argument to be notified when the operation 
    completes. ChannelPromise is a subinterface of ChannelFuture that defines the writable methods, such as 
    setSuccess() or setFailure(), thus making ChannelFuture immutable
* ChannelHandler adapters
    * You can use the classes ChannelInboundHandlerAdapter and ChannelOutboundHandlerAdapter as starting points for 
    your own ChannelHandlers
    * These adapters provide basic implementations of ChannelInboundHandler and ChannelOutboundHandler respectively
    * extends the abstract class ChannelHandlerAdapter
    * ChannelHandlerAdapter also provides the utility method isSharable()
        * This method returns true if the implementation is annotated as Sharable, indicating that it can be added 
        to multiple ChannelPipelines
* Resource management
    * Whenever you act on data by calling ChannelInboundHandler.channelRead() or ChannelOutboundHandler.write(), you 
    need to ensure that there are no resource leaks.
        * Netty uses reference counting to handle pooled ByteBufs
    * Netty provides class ResourceLeakDetector, which will sample about 1% of your application’s buffer allocations to
      check for memory leaks
    * Leak-detection levels
        * DISABLED - Disables leak detection. Use this only after extensive testing.
        * SIMPLE - Reports any leaks found using the default sampling rate of 1%. This is the default
            level and is a good fit for most cases.
        * ADVANCED - Reports leaks found and where the message was accessed. Uses the default sampling
            rate.
        * PARANOID - Like ADVANCED except that every access is sampled. This has a heavy impact on performance and 
        should be used only in the debugging phase.
    * java -Dio.netty.leakDetectionLevel=ADVANCED
    * release the message `ReferenceCountUtil.release(msg)` // Releases resource
    * Because consuming inbound data and releasing it is such a common task, Netty provides a special 
    ChannelInboundHandler implementation called SimpleChannelInboundHandler
        * This implementation will automatically release a message once it’s consumed by channelRead0()
    * handle a write()
        ```
        ReferenceCountUtil.release(msg);
        promise.setSuccess(); // Notifies ChannelPromise that data was handled
        ```
        * It’s important not only to release resources but also to notify the ChannelPromise
        * Otherwise a situation might arise where a ChannelFutureListener has not been notified about a message that 
        has been handled
        * it is the responsibility of the user to call ReferenceCountUtil.release() if a message is consumed or 
        discarded and not passed to the next ChannelOutboundHandler in the ChannelPipeline
        * If the message reaches the actual transport layer, it will be released automatically when it’s written or 
        the Channel is closed
* Interface ChannelPipeline
    * If you think of a ChannelPipeline as a chain of ChannelHandler instances that intercept the inbound and 
    outbound events that flow through a Channel, it’s easy to see how the interaction of these ChannelHandlers can 
    make up the core of an application’s data and event-processing logic
    * Every new Channel that’s created is assigned a new ChannelPipeline
        * This association is permanent
        * This is a fixed operation in Netty’s component lifecycle and requires no action on the part of the developer
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
    * When you’ve finished adding your mix of inbound and outbound handlers to a ChannelPipeline using the 
    ChannelPipeline.add*() methods, the ordinal of each ChannelHandler is its position from beginning to end as 
    we just defined them
    * As the pipeline propagates an event, it determines whether the type of the next ChannelHandler in the pipeline 
    matches the direction of movement
        * If not, the ChannelPipeline skips that ChannelHandler and proceeds to the next one, until it finds one 
        that matches the desired direction
            * Of course, a handler might implement both ChannelInboundHandler and ChannelOutboundHandler interfaces
    * Modifying a ChannelPipeline
        * A ChannelHandler can modify the layout of a ChannelPipeline in real time by adding, removing, or replacing 
        other ChannelHandlers
            * It can remove itself from the ChannelPipeline as well
        * This is one of the most important capabilities of the ChannelHandler
        * methods
            * addFirst
            * addBefore
            * addAfter
            * addLast
            * remove
                * remove(ChannelHandler handler)
                * remove(java.lang.String name)
            * replace
            * get
            * context - Returns the ChannelHandlerContext bound to a ChannelHandler
            * names
    * Normally each ChannelHandler in the ChannelPipeline processes events that are passed to it by its EventLoop 
    (the I/O thread). 
        * It’s critically important not to block this thread as it would have a negative effect on the overall handling 
        of I/O
        * Sometimes it may be necessary to interface with legacy code that uses blocking APIs.
            * For this case, the ChannelPipeline has add() methods that accept an EventExecutorGroup. If an event is 
            passed to a custom EventExecutorGroup, it will be handled by one of the EventExecutors contained in this 
            EventExecutorGroup and thus be removed from the EventLoop of the Channel itself
            * For this use case Netty provides an implementation called DefaultEventExecutorGroup
    * Firing events
        * The ChannelPipeline API exposes additional methods for invoking inbound and outbound operations
        * inbound operations
            * fireChannelRegistered Calls channelRegistered(ChannelHandlerContext) on the next ChannelInboundHandler 
            in the ChannelPipeline
            * fireChannelUnregistered Calls channelUnregistered(ChannelHandlerContext) on the next 
            ChannelInboundHandler in the ChannelPipeline
            * fireChannelActive Calls channelActive(ChannelHandlerContext) on the next ChannelInboundHandler in the 
            ChannelPipeline
            * fireChannelInactive Calls channelInactive(ChannelHandlerContext) on the next ChannelInboundHandler in 
            the ChannelPipeline
            * fireExceptionCaught Calls exceptionCaught(ChannelHandlerContext, Throwable) on the next ChannelHandler 
            in the ChannelPipeline
            * fireUserEventTriggered Calls userEventTriggered(ChannelHandlerContext, Object) on the next 
            ChannelInboundHandler in the ChannelPipeline
            * fireChannelRead Calls channelRead(ChannelHandlerContext,Object msg) on the next ChannelInboundHandler 
            in the ChannelPipeline
            * fireChannelReadComplete Calls channelReadComplete(ChannelHandlerContext) on the next ChannelStateHandler 
            in the ChannelPipeline    
        * On the outbound side, handling an event will cause some action to be taken on the underlying socket
            * bind Binds the Channel to a local address. This will call bind(ChannelHandlerContext, SocketAddress, 
            ChannelPromise) on the next ChannelOutboundHandler in the ChannelPipeline.
            * connect Connects the Channel to a remote address. This will call connect(ChannelHandlerContext, 
            SocketAddress, ChannelPromise) on the next ChannelOutboundHandler in the ChannelPipeline
            * disconnect Disconnects the Channel. This will call disconnect(ChannelHandlerContext, ChannelPromise) on 
            the next ChannelOutboundHandler in the ChannelPipeline.
            * close Closes the Channel. This will call close(ChannelHandlerContext, ChannelPromise) on the next 
            ChannelOutboundHandler in the ChannelPipeline.
            * deregister Deregisters the Channel from the previously assigned EventExecutor (the EventLoop). This 
            will call deregister(ChannelHandlerContext, ChannelPromise) on the next ChannelOutboundHandler in the 
            ChannelPipeline.
            * flush Flushes all pending writes of the Channel. This will call flush(ChannelHandlerContext) on the 
            next ChannelOutboundHandler in the ChannelPipeline.
            * write Writes a message to the Channel. This will call write(ChannelHandlerContext, Object msg, 
            ChannelPromise) on the next ChannelOutboundHandler in the ChannelPipeline.
                * Note: this does not write the message to the underlying Socket, but only queues it. To write it to 
                the Socket, call flush() or writeAndFlush().
            * writeAndFlush This is a convenience method for calling write() then flush().
            * read Requests to read more data from the Channel. This will call read(ChannelHandlerContext) on the 
            next ChannelOutboundHandler in the ChannelPipeline.
    * summary
        * A ChannelPipeline holds the ChannelHandlers associated with a Channel.
        * A ChannelPipeline can be modified dynamically by adding and removing ChannelHandlers as needed.
        * ChannelPipeline has a rich API for invoking actions in response to inbound and outbound events.
* Interface ChannelHandlerContext
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
    * When using the ChannelHandlerContext API, please keep the following points in mind
        * The ChannelHandlerContext associated with a ChannelHandler never changes, so it’s safe to cache a reference 
        to it.
        * ChannelHandlerContext methods, as we explained at the start of this section, involve a shorter event flow 
        than do the identically named methods available on other classes. This should be exploited where possible to 
        provide maximum performance.
* Using ChannelHandlerContext
    * Calling write() on the Channel causes a write event to flow all the way through the pipeline
        ```
        ChannelHandlerContext ctx = ..;
        Channel channel = ctx.channel();
        channel.write(Unpooled.copiedBuffer("Netty in Action", CharsetUtil.UTF_8));
        ```
    * Accessing the ChannelPipeline from a ChannelHandlerContext
        ```
        ChannelHandlerContext ctx = ..;
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.write(Unpooled.copiedBuffer("Netty in Action", CharsetUtil.UTF_8));
        ```
    * ChannelHandler passes event to next ChannelHandler in ChannelPipeline using assigned ChannelHandlerContext
        * ChannelHandlerContext has connection from its ChannelHandler to the next ChannelHandler
    * Why would you want to propagate an event starting at a specific point in the ChannelPipeline
        * To reduce the overhead of passing the event through ChannelHandlers that are not interested in it
        * To prevent processing of the event by handlers that would be interested in the event
    * To invoke processing starting with a specific ChannelHandler, you must refer to the ChannelHandlerContext 
    that’s associated with the ChannelHandler before that one
    * ChannelHandlerContext that’s associated with the ChannelHandler before that one. This ChannelHandlerContext 
    will invoke the ChannelHandler that follows the one with which it’s associated
        ```
        ChannelHandlerContext ctx = ..;
        ctx.write(Unpooled.copiedBuffer("Netty in Action", CharsetUtil.UTF_8));
        ```
        * write() sends the buffer to the next ChannelHandler
* Advanced uses of ChannelHandler and ChannelHandlerContext
    * you can acquire a reference to the enclosing Channel- Pipeline by calling the pipeline() method of a 
    ChannelHandlerContext
    * This enables runtime manipulation of the pipeline’s ChannelHandlers, which can be exploited to implement 
    sophisticated designs
        * For example, you could add a ChannelHandler to a pipeline to support a dynamic protocol change
    * Other advanced uses can be supported by caching a reference to a Channel- HandlerContext for later use, which 
    might take place outside any ChannelHandler methods and could even originate from a different thread
    * Because a ChannelHandler can belong to more than one ChannelPipeline, it can be bound to multiple 
    ChannelHandlerContext instances
    * ChannelHandler intended for this use must be annotated with @Sharable; otherwise, attempting to add it to more 
    than one ChannelPipeline will trigger an exception
    * A common reason for installing a single ChannelHandler in multiple ChannelPipelines is to gather statistics across
      multiple Channels
      
## callback
* callback is simply a method, a reference to which has been provided to another method
    * This enables the latter to call the former at an appropriate time
    * represents one of the most common ways to notify an interested party that an operation has completed
    * Netty uses callbacks internally when handling events; when a callback is triggered the event can be handled by 
    an implementation of interface ChannelHandler
    * an example - hooks from ChannelInboundHandlerAdapter: channelActive(ChannelHandlerContext) is called when a new 
    connection is established
    
## future
* Future
    * provides another way to notify an application when an operation has completed
    * acts as a placeholder for the result of an asynchronous operation
    * it will complete at some point in the future and provide access to the result
    * JDK ships with interface java.util.concurrent.Future, but the provided implementations allow you only to check 
    manually whether the operation has completed or to block until it does
    * Netty provides its own implementation, ChannelFuture, for use when an asynchronous operation is executed
    * ChannelFuture provides additional methods that allow us to register one or more ChannelFutureListener instances
    * listener’s callback method, operationComplete(), is called when the operation has completed
        * listener can then determine whether the operation completed successfully or with an error
        * If the latter, we can retrieve the Throwable that was produced
        ```
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()){
                    ByteBuf buffer = Unpooled.copiedBuffer(
                    "Hello",Charset.defaultCharset());
                    ChannelFuture wf = future.channel()
                    .writeAndFlush(buffer);
                    ....
                } else {
                    Throwable cause = future.cause();
                    cause.printStackTrace();
                }
            }
        });
        ```
    * the notification mechanism provided by the ChannelFutureListener eliminates the need for manually checking
    operation completion
    * Each of Netty’s outbound I/O operations returns a ChannelFuture
    * ChannelFutureListener is a more elaborate version of a callback, you’re correct
    * callbacks and Futures are complementary mechanisms; in combination they make up one of the key building blocks 
    of Netty itself

## Events and handlers
* Events and handlers
    * Netty uses distinct events to notify us about changes of state or the status of operations
    * This allows us to trigger the appropriate action based on the event that has occurred. Such actions might include
        * Logging
        * Data transformation
        * Flow-control
        * Application logic
    * Events that may be triggered by inbound data or an associated change of state include
        * Active or inactive connections
        * Data reads
        * User events
        * Error events
    * An outbound event is the result of an operation that will trigger an action in the future, which may be
        * Opening or closing a connection to a remote peer
        * Writing or flushing data to a socket
    * you can think of each ChannelHandler instance as a kind of callback to be executed in response to a specific event
      
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
* EventLoop/thread allocation
    * EventLoops that service I/O and events for Channels are contained in an EventLoopGroup
    * The manner in which EventLoops are created and assigned varies according to the transport implementation
    * ASYNCHRONOUS TRANSPORTS
        * Asynchronous implementations use only a few EventLoops (and their associated Threads), and in the current 
        model these may be shared among Channels
        * This allows many Channels to be served by the smallest possible number of Threads, rather than assigning 
        a Thread per Channel
        * The EventLoops (and their Threads) are allocated directly when the EventLoopGroup is created to ensure that 
        they will be available when needed
        * EventLoopGroup is responsible for allocating an EventLoop to each newly created Channel
        * the same EventLoop may be assigned to multiple Channels
        * Each EventLoop handles all events and tasks for all the channels assigned to it. Each EventLoop is 
        associated with one Thread.
        * Once a Channel has been assigned an EventLoop, it will use this EventLoop (and the associated Thread) 
        throughout its lifetime
        * Because an EventLoop usually powers more than one Channel, ThreadLocal will be the same for all associated Channels
            * This makes it a poor choice for implementing a function such as state tracking
            * However, in a stateless context it can still be useful for sharing heavy or expensive objects, or even 
            events, among Channels
    * BLOCKING TRANSPORTS
        * one EventLoop (and its Thread) is assigned to each Channel
        * it is guaranteed that the I/O events of each Channel will be handled by only one Thread—the one that powers 
        the Channel’s EventLoop

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