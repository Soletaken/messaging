package com.example;

import com.example.util.HandlerInter;
import com.example.util.Utilis;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.channel.PointToPointSubscribableAmqpChannel;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.channel.DirectChannelSpec;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.springframework.integration.dsl.channel.MessageChannels.direct;


@Configuration
@EnableIntegration
@IntegrationComponentScan
public class FlowConfiguration {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    MessageChannel someChannel(){
//        final DirectChannelSpec direct = direct(Utilis.INBOUND_CHANNEL);
//        return direct.get();
        Optional<QueueArguments> arguments = Optional.of(new QueueArguments());
        String queueName="some.name.for.queue";
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(queueName+".%d").build()));
        container.setConcurrentConsumers(5);
        container.setMaxConcurrentConsumers(100);
        PointToPointSubscribableAmqpChannel channel =
                new PointToPointSubscribableAmqpChannel(Utilis.INBOUND_CHANNEL, container, template) {
            @Override
            protected String obtainQueueName(AmqpAdmin admin, String channelName) {
                arguments.ifPresent( (a)-> {
                    admin.declareQueue(new Queue(queueName, true, false, false, a.asMap()));
                });
                return super.obtainQueueName(admin, channelName);
            }
        };
        channel.setQueueName(queueName);

        
        return channel;
    }

    @Bean
    public IntegrationFlow testFlow(HandlerInter handlerInter){
        return flow -> flow
                .channel(Utilis.INBOUND_CHANNEL)
                .transform(message ->{
                    System.out.println(message);
                    return message;
                })
                .handle(handlerInter)
                .transform(message ->{
                    System.out.println(message);
                    return message;
                })
                .bridge(null);
    }


    @Data
    private static class QueueArguments{

        private int xMaxLength = Integer.MAX_VALUE;

        public Map<String, Object> asMap() {
            return ImmutableMap.<String, Object>builder().put("x-max-length", xMaxLength).build();
        }
    }

//    @Bean
//    public RetryOperations retryTemplate() {
//
//        RetryTemplate retryTemplate = new RetryTemplate();
//        retryTemplate.setBackOffPolicy(configuration.backOffPolicyForApplication());
//        retryTemplate.setRetryPolicy(configuration.retryPolicyForApplication());
//        return retryTemplate;
//    }
//
//    @Bean
//    public RetryOperationsInterceptor interceptor(RetryOperations retryTemplate) {
//        RetryOperationsInterceptor interceptor = new RetryOperationsInterceptor();
//        interceptor.setRetryOperations(retryTemplate);
//        return interceptor;
//    }
}
