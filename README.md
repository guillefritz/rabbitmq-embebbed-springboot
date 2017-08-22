# rabbitmq-embebbed-springboot
Integration test with an embebbed rabbit mq + springboot + kotlin

See RabbitEmbebbedIT class, that uses testcontainers for startup a RabbitMQ container, set the host/port to SpringBoot, then run the test! =)

## how to test

	./gradlew clean test

