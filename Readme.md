# Spring Boot JMS example

[TOC]

## Spring JMS

Spring предоставляет JMS framework, который упрощает работу с JMS API. 

## Dependency 

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-activemq</artifactId>
</dependency>
```



## Entity

Нужно то, что наш брокер будет отправлять. В данном случае это Email POJO. 

```java
package com.luv2code.spring_boot_jms_activemq;

public class Email {
    private String to;
    private String body;

    public Email() {
    }

    public Email(String to, String body) {
        this.to = to;
        this.body = body;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return String.format("Email{to=%s, body=%s}", getTo(), getBody());
    }

}
```

Он довольно простой и содержит в себе всего 2 поля : **to** и **body**

## Message Receiver

 Нужно создать POJO, который будет получать сообщения. 

```java
package com.luv2code.spring_boot_jms_activemq;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class JmsReceiver {

    @JmsListener(destination = "mailBox", containerFactory = "containerForJmsListener")
    public void receiveMessage(Email email) {
        System.out.println("Received<" + email + ">");
    }
}
```

 Помечаем его как **@Component**, что бы Spring Boot сам создал объект данного класса. 

Внутри него есть метод помеченный **@JmsListener** , которая помечает данный метод как слушателя, он будет "слушать" указанную нами очередь. 

У данной аннотации есть 2 аттрибута : 

- **destination** - указываем какую очередь будем слушать.
- **containerFactory** -  имя бина **JmsListenerContainerFactory**, для создания контейнера слушателя сообщений. 

## Jms Bean Configuration

Создадим POJO **JmsConfiguration**. Пометим его **@Configuration** и **@EnableJms** (запускает обнаружение методов, аннотированных **@JmsListener**)

```java
package com.luv2code.spring_boot_jms_activemq;

@Configuration
@EnableJms
public class JmsConfiguration {
}
```

### JmsListenerContainerFactory Bean

Создаем метод который будет возвращать бин **JmsListenerContainerFactory** . На него есть ссылка в **@JmsListener**. 

```java
@Bean("containerForJmsListener")
public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer) {
    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
    // Через factory можно произвести настройку контейнера
    configurer.configure(factory, connectionFactory);
    return factory;
}
```

В качестве параметров он принимает :

- Объект **ConnectionFactory** инкапсулирует набор параметров конфигурации соединения.
- **DefaultJmsListenerContainerFactoryConfigurer** дефолтный кофигуратор для **DefaultJmsListenerContainerFactory**. 

Внутри создаем объект **DefaultJmsListenerContainerFactory**, который имеет значение по умолчанию которые подойдут для большинства пользователей. 

Далее мы помещаем в объект **configurer** нашу **factory** и **connectionFactory** с помощью метода `configurer.configure(factory, connectionFactory);` . 

И возвращаем нашу **factory**. 

### MessageConverter Bean

MessageConverter по умолчанию может преобразовывать только базовые типы (такие как String, Map, Serializable), а наш Email не помечен @Serializable. Нужно использовать Jakson и сериализовать контент в JSON. Spring Boot обнаруживает наличие MessageConverter и связывает его как со стандартным JmsTemplate, так и с любым JmsListenerContainerFactory, созданным DefaultJmsListenerContainerFactoryConfigurer.

```java
@Bean
public MessageConverter jaksonJmsMessageConverter(){
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setTargetType(MessageType.TEXT);
    converter.setTypeIdPropertyName("_type");
    return converter;
}
```

Мы создаем конвертер в Json формат. Указываем ему какого типа будут сообщения и указываем имя свойства, которое идентифицирует сущность. И в результате возвращаем наш конвертер. 

[полезно](https://ichihedge.wordpress.com/2016/05/23/correctly-configuring-mappingjackson2messageconverter-for-messaging-via-json/)  

### Окончательный результат класса JmsConfiguration

```java
package com.luv2code.spring_boot_jms_activemq;

import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import javax.jms.ConnectionFactory;

@Configuration
@EnableJms
public class JmsConfiguration {
    @Bean("containerForJmsListener")
    public JmsListenerContainerFactory<?> myFactory(ConnectionFactory connectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        // Через factory можно произвести настройку контейнера
        configurer.configure(factory, connectionFactory);
        return factory;
    }

    @Bean
    public MessageConverter jaksonJmsMessageConverter(){
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }
}

```

## Send JMS messages with Spring

```java
package com.luv2code.spring_boot_jms_activemq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        // Launch the application
        ConfigurableApplicationContext context = SpringApplication.run(DemoApplication.class, args);

        JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);

        // Send a message with a POJO - the template reuse the message converter
        System.out.println("Sending an email message.");
        jmsTemplate.convertAndSend("mailBox", new Email("info@example.com", "Hello"));
    }
}
```

**JmsTemplate** - API которое упрощает отправку сообщений (синхронный режим). **MessageConverter** был автоматически связан с **JmsTemplate** , документ JSON создается только в MessageType.TEXT.

Рекомендуем использовать контейнер слушателя, такой как DefaultMessageListenerContainer, с фабрикой соединений на основе кеша, чтобы вы могли получать сообщения асинхронно.

