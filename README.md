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