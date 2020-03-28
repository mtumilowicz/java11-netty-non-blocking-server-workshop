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
* Channel is a basic construct of Java NIO
    * an open connection to an entity such as a hardware device, a file, a
    network socket, or a program component that is capable of performing
    one or more distinct I/O operations, for example reading or writing
    * For now, think of a Channel as a vehicle for incoming (inbound) and outgoing (outbound)
      data
    * it can be open or closed
* callback is simply a method, a reference to which has been provided to another
  method
    * This enables the latter to call the former at an appropriate time
    * represents one of the most
      common ways to notify an interested party that an operation has completed
    * Netty uses callbacks internally when handling events; when a callback is triggered
      the event can be handled by an implementation of interface ChannelHandler
    * an example - hooks from ChannelInboundHandlerAdapter: channelActive(ChannelHandlerContext) 
    is called when a new connection is established
* Future
    * provides another way to notify an application when an operation has completed
    * acts as a placeholder for the result of an asynchronous operation
    * it will complete at some point in the future and provide access to the result
    * JDK ships with interface java.util.concurrent.Future, but the provided
      implementations allow you only to check manually whether the operation has completed
      or to block until it does
    * Netty provides its own
      implementation, ChannelFuture, for use when an asynchronous operation is executed
    * ChannelFuture provides additional methods that allow us to register one or
      more ChannelFutureListener instances
    * listener’s callback method, operation-
      Complete(), is called when the operation has completed
        * listener can then determine
          whether the operation completed successfully or with an error
        * If the latter, we
          can retrieve the Throwable that was produced
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
    * the notification mechanism
      provided by the ChannelFutureListener eliminates the need for manually checking
      operation completion
    * Each of Netty’s outbound I/O operations returns a ChannelFuture
    * ChannelFutureListener is a more elaborate version of a
      callback, you’re correct
    * callbacks and Futures are complementary mechanisms;
      in combination they make up one of the key building blocks of Netty itself
* Events and handlers
    * Netty uses distinct events to notify us about changes of state or the status of operations
    * This allows us to trigger the appropriate action based on the event that has
      occurred. Such actions might include
      * Logging
      * Data transformation
      * Flow-control
      * Application logic
    * Events that may be triggered by inbound data or an
      associated change of state include
      * Active or inactive connections
      * Data reads
      * User events
      * Error events
    * An outbound event is the result of an operation that will trigger an action in the
      future, which may be
      * Opening or closing a connection to a remote peer
      * Writing or flushing data to a socket
    * you can think of each ChannelHandler instance as a kind of callback to be executed
      in response to a specific event
* Event loop
    * Under the covers,
      an EventLoop is assigned to each Channel to handle all of the events, including
      * Registration of interesting events
      * Dispatching events to ChannelHandlers
      * Scheduling further actions
    * EventLoop itself is driven by only one thread that handles all of the I/O events for
      one Channel and does not change during the lifetime of the EventLoop
    * This simple
      and powerful design eliminates any concern you might have about synchronization in
      your ChannelHandlers, so you can focus on providing the right logic to be executed
      when there is interesting data to process
* All Netty servers require the following:
    * At least one ChannelHandler—This component implements the server’s processing
      of data received from the client—its business logic
    * Bootstrapping—This is the startup code that configures the server. At a minimum,
      it binds the server to the port on which it will listen for connection requests
* Every Channel has an associated ChannelPipeline, which holds a chain of Channel-
  Handler instances
  * By default, a handler will forward the invocation of a handler
    method to the next one in the chain
  * Therefore, if exceptionCaught()is not implemented
    somewhere along the chain, exceptions received will travel to the end of the
    ChannelPipeline and will be logged
  * For this reason, your application should supply
    at least one ChannelHandler that implements exceptionCaught()
* ChanelHandlers
    * ChannelHandlers are invoked for different types of events.
    * Applications implement or extend ChannelHandlers to hook into the event lifecycle
      and provide custom application logic
    * Architecturally, ChannelHandlers help to keep your business logic decoupled
      from networking code. This simplifies development as the code evolves in
      response to changing requirements
* Bootstrapping the server
    * bootstrapping involves
        * Bind to the port on which the server will listen for and accept incoming connection
          requests
        * Configure Channels to notify an EchoServerHandler instance about inbound
          messages
    * transport - in the standard, multilayered view
      of networking protocols, the transport layer is the one that provides services for endto-
      end or host-to-host communications
      * Internet communications are based on the TCP transport. NIO transport refers to a
        transport that’s mostly identical to TCP except for server-side performance enhancements
        provided by the Java NIO implementation
    * The following steps are required in bootstrapping:
      * Create a ServerBootstrap instance to bootstrap and bind the server.
      * Create and assign an NioEventLoopGroup instance to handle event processing,
      such as accepting new connections and reading/writing data.
      * Specify the local InetSocketAddress to which the server binds.
      * Initialize each new Channel with an EchoServerHandler instance.
      * Call ServerBootstrap.bind() to bind the server.
    * NIO is used in this example because it’s currently the most widely used transport,
      thanks to its scalability and thoroughgoing asynchrony
    * When a
      new connection is accepted, a new child Channel will be created, and the Channel-
      Initializer will add an instance of your EchoServerHandler to the Channel’s
      ChannelPipeline
      * this handler will receive notifications
        about inbound messages
* Bootstrapping the client
    * bootstrapping a client is similar to bootstrapping a
      server, with the difference that instead of binding to a listening port the client uses
      host and port parameters to connect to a remote address, here that of the Echo server
* EventLoop defines Netty’s core abstraction for handling events that occur during
  the lifetime of a connection
  An EventLoopGroup contains one or more EventLoops.
  * An EventLoop is bound to a single Thread for its lifetime.
  * All I/O events processed by an EventLoop are handled on its dedicated Thread.
  * A Channel is registered for its lifetime with a single EventLoop.
  * A single EventLoop may be assigned to one or more Channels.
* Note that this design, in which the I/O for a given Channel is executed by the same
  Thread, virtually eliminates the need for synchronization
* Netty provides ChannelFuture, whose addListener() method registers
  a ChannelFutureListener to be notified when an operation has completed
  (whether or not successfully)
  * Think of a ChannelFuture as a placeholder for the
    result of an operation that’s to be executed in the future
  * When exactly it will
    be executed may depend on several factors and thus be impossible to predict
    with precision, but it is certain that it will be executed
  * Furthermore, all operations
    belonging to the same Channel are guaranteed to be executed in the
    order in which they were invoked
* ChannelHandler
    * From the application developer’s standpoint, the primary component of Netty is the
      ChannelHandler, which serves as the container for all application logic that applies
      to handling inbound and outbound data
    * ChannelHandler
      methods are triggered by network events (where the term “event” is used very
      broadly)
    * ChannelHandler can be dedicated to almost any kind of action,
      such as converting data from one format to another or handling exceptions thrown
      during processing
    * ChannelInboundHandler is a subinterface you’ll implement frequently
        * This type receives inbound events and data to be handled by your application’s
          business logic
        * you can also flush data from a ChannelInboundHandler when
          you’re sending a response to a connected client
        * The business logic of your application
          will often reside in one or more ChannelInboundHandlers
* ChannelPipeline
    * ChannelPipeline provides a container for a chain of ChannelHandlers and defines
      an API for propagating the flow of inbound and outbound events along the chain
    * When a Channel is created, it is automatically assigned its own ChannelPipeline
    * ChannelHandlers are installed in the ChannelPipeline as follows:
        * A ChannelInitializer implementation is registered with a ServerBootstrap
        * When ChannelInitializer.initChannel() is called, the ChannelInitializer
          installs a custom set of ChannelHandlers in the pipeline
        * The ChannelInitializer removes itself from the ChannelPipeline
    * ChannelHandler has been designed specifically to support a broad range of uses,
      and you can think of it as a generic container for any code that processes events
      (including data) coming and going through the ChannelPipeline
    * movement of an event through the pipeline is the work of the ChannelHandlers
      that have been installed during the initialization, or bootstrapping phase of the application
    * These objects receive events, execute the processing logic for which they have
      been implemented, and pass the data to the next handler in the chain
    * The order in
      which they are executed is determined by the order in which they were added
    * From the point of view of a client application, events are said to
      be outbound if the movement is from the client to the server and inbound in the
      opposite case
    * both inbound and outbound handlers can be installed
      in the same pipeline
    * If a message or any other inbound event is read, it will start from
      the head of the pipeline and be passed to the first ChannelInboundHandler
      * This handler
        may or may not actually modify the data, depending on its specific function, after
        which the data will be passed to the next ChannelInboundHandler in the chain
      * Finally,
        the data will reach the tail of the pipeline, at which point all processing is terminated
    * The outbound movement of data (that is, data being written) is identical in concept.
      In this case, data flows from the tail through the chain of ChannelOutbound-
      Handlers until it reaches the head
      * Beyond this point, outbound data will reach the
        network transport, shown here as a Socket
      * Typically, this will trigger a write operation
    * An event can be forwarded to the next handler in the current chain by using the
      ChannelHandlerContext that’s supplied as an argument to each method
      * Because
        you’ll sometimes ignore uninteresting events, Netty provides the abstract base classes
        ChannelInboundHandlerAdapter and ChannelOutboundHandlerAdapter
      * Each provides
        method implementations that simply pass the event to the next handler by calling
        the corresponding method on the ChannelHandlerContext
      * You can then extend
        the class by overriding the methods that interest you
    * Although both inbound and outbound handlers extend ChannelHandler, Netty distinguishes
      implementations of ChannelInboundHandler and ChannelOutboundHandler
      and ensures that data is passed only between handlers of the same directional type
    * When a ChannelHandler is added to a ChannelPipeline, it’s assigned a Channel-
      HandlerContext, which represents the binding between a ChannelHandler and the
      ChannelPipeline
      * Although this object can be used to obtain the underlying Channel,
        it’s mostly utilized to write outbound data
    * There are two ways of sending messages in Netty:
        * You can write directly to the Channel - 
        causes the message to start from the tail of the ChannelPipeline
        * write to a ChannelHandlerContext object associated with a ChannelHandler - 
        causes the message to start from the next handler in the ChannelPipeline
    * These are the adapters you’ll call most often when creating your custom handlers:
        * ChannelHandlerAdapter
        * ChannelInboundHandlerAdapter
        * ChannelOutboundHandlerAdapter
        * ChannelDuplexHandlerAdapter