import spock.lang.Specification

class EchoClientTest extends Specification {

    def 'xxx'() {
        given:
        def buffer = new ByteArrayOutputStream()
        System.out = new PrintStream(buffer)
        new Thread({ new EchoServer().start() }).start()

        Thread.sleep(10)

        when:
        new EchoClient().start()
        def out = buffer.toString()

        then:
        out.contains('Server received: Hello Netty!')
        out.contains('Client received: Hello Netty!')
    }
}
