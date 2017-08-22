package ar.gmf.rabbit.embebbed

import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.core.Queue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.EnvironmentTestUtils
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.LogMessageWaitStrategy
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS

//Hack for some generic issues with GenericContainer class
class KGenericContainer (imageName: String) : GenericContainer<KGenericContainer>(imageName)

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = arrayOf(RabbitEmbebbedIT.Initializer::class))
class RabbitEmbebbedIT {

    companion object {

        val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        val RABBIT_PORT = 5672

        @ClassRule
        @JvmField
        var rabbit: KGenericContainer = KGenericContainer("rabbitmq:alpine").withExposedPorts(RABBIT_PORT)
                .waitingFor(LogMessageWaitStrategy().withRegEx(".*Server startup complete.*\\s"))
                .withStartupTimeout(Duration.of(10, SECONDS))
    }

    /**
     * Set spring.rabbit host/port before spring init with docker container info
     */
    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            EnvironmentTestUtils.addEnvironment("it", configurableApplicationContext.environment,
                    "spring.rabbitmq.host=" + rabbit.containerIpAddress,
                    "spring.rabbitmq.port=" + rabbit.getMappedPort(RABBIT_PORT)
            )
        }
    }


    @Autowired
    lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    lateinit var amqpAdmin: AmqpAdmin

    @Test
    fun testSendReceive() {

        val queueName = "testQueue"
        val message = "message1"

        //create Queue (required)
        amqpAdmin.declareQueue(Queue(queueName))

        //send message
        amqpTemplate.convertAndSend(queueName, message)

        //receive message
        val receive = amqpTemplate.receiveAndConvert(queueName, 5000)?.toString()

        assertThat(receive).isNotNull()
        assertThat(receive).isEqualTo(message)

        logger.info("message {}", receive.toString())
    }
}