package com.example;

import com.example.util.Utilis;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.channel.PointToPointSubscribableAmqpChannel;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.backoff.SleepingBackOffPolicy;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.Executors.newCachedThreadPool;

@Configuration
public class ChannelConfig {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Data
    private static class QueueArguments {

        private int xMaxLength = Integer.MAX_VALUE;

        public Map<String, Object> asMap() {
            return ImmutableMap.<String, Object>builder().put("x-max-length", xMaxLength).build();
        }
    }

    //pomysł: customowy header mapper który nie zjada żadnych headerów - ale jak go wpiąć?
    @Bean
    DefaultAmqpHeaderMapper mapper(){
        DefaultAmqpHeaderMapper mapper = new DefaultAmqpHeaderMapper();
        mapper.setRequestHeaderNames("*");
        return mapper;
    }

/*
kopia tworzenia kanału z integration
 */
    @Bean
    MessageChannel someChannel(RetryOperationsInterceptor interceptor) {
//        final DirectChannelSpec direct = direct(Utilis.INBOUND_CHANNEL);
//        return direct.get();
        Optional<QueueArguments> arguments = Optional.of(new QueueArguments());
        String queueName = "some.name.for.queue";
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(queueName + ".%d").build()));
        container.setConcurrentConsumers(5);
        container.setMaxConcurrentConsumers(100);
        Advice[] advices = new Advice[] { interceptor };
        //bez tego ustawionego wpada w petle kiedy próbuje przetwarzać wiadomości (ale nie przetwarza, nawet nie
        // wchodzi w handlera)
        //org.springframework.integration.MessageDispatchingException: Dispatcher has no subscribers
        //jak sie to ustawi, to wszystkie zignorowane wiadomości zostają na kolejce w stanie unacked
        //też nie wchodzi nawet w handler
        //w panelu rabbita widać, że po wyłączeniu aplikacji przestawia wszystkie message na ready
        // po ponownym włączeniu spowrotem na unacked i tam juz zostaja
        container.setAdviceChain(advices);
        PointToPointSubscribableAmqpChannel channel =
                new PointToPointSubscribableAmqpChannel(Utilis.TEST_CHANNEL, container, template) {
                    @Override
                    protected String obtainQueueName(AmqpAdmin admin, String channelName) {
                        arguments.ifPresent((a) -> {
                            admin.declareQueue(new Queue(queueName, true, false, false, a.asMap()));
                        });
                        return super.obtainQueueName(admin, channelName);
                    }
        };
        channel.setQueueName(queueName);


        return channel;
    }


    @Bean
    public RetryOperations retryTemplate() {

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicyForApplication());
        retryTemplate.setRetryPolicy(retryPolicyForApplication());
        return retryTemplate;
    }

    @Bean
    public RetryOperationsInterceptor interceptor(RetryOperations retryTemplate) {
        RetryOperationsInterceptor interceptor = new RetryOperationsInterceptor();
        interceptor.setRetryOperations(retryTemplate);
        return interceptor;
    }

    public SleepingBackOffPolicy<?> backOffPolicyForApplication() {
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(5000);
        return fixedBackOffPolicy;
    }

    public RetryPolicy retryPolicyForApplication() {
        return new AlwaysRetryPolicy();
    }

}
