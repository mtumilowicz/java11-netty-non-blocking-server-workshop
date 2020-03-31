package answers


import spock.lang.Specification

class ClientTest extends Specification {

    def 'client-server communication'() {
        given: 'intercept the System.out'
        def buffer = new ByteArrayOutputStream()
        System.out = new PrintStream(buffer)

        and: 'run the server'
        new Thread({ new Server().start() }).start()
        Thread.sleep(10)

        when: 'client connects'
        new Client().start()

        then: 'verify communication'
        def out = buffer.toString()
        out.contains('Server received: Hello Netty!')
        out.contains('Client received: Hello Netty!')
    }
}
