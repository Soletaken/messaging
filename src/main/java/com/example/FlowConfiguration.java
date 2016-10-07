package com.example;

import com.example.util.HandlerInter;
import com.example.util.Utilis;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.util.Collections;
import java.util.Map;


@Configuration
@EnableIntegration
@IntegrationComponentScan
public class FlowConfiguration {
    PHandler handlerVoid = new PHandler();

    @Bean
    public IntegrationFlow testFlow(HandlerInter handlerInter, MessageChannel someChannel) {
        Map<String,Object> map = Collections.singletonMap("asdasda","adsadasdas");
        return flow -> flow
                .channel(Utilis.INBOUND_CHANNEL)
                .transform(message -> {
                    System.out.println("first channel: " + message);
                    return message;
                })
                .handle(handlerInter)
                .transform(message -> {
                    System.out.println("doing stuff before going to queue");
                    return message;
                })
                .enrichHeaders(map)
//                .channel(Utilis.TEST_CHANNEL);
              .channel(someChannel);
    }

    @Bean
    public IntegrationFlow testFlow2(HandlerInter handlerInter, MessageChannel someChannel) {
        return flow -> flow
//                .channel(Utilis.TEST_CHANNEL)
                .channel(someChannel)
                //jesli jest po nazwie, to działa (to znaczy nie wpada w petle, zwraca co trzeba z kontrolera)
                //nie korzysta wtedy wcale z kolejki - widać to na panelu rabbita
                //jeśli jest someChannel (czyli tak jak w ws), to wpada w petle - przechodzi przez cały flow (testFlow2)
                // w nieskonczoność
                //brakuje wtedy w headerach wiadomości reply channela - gdzieś wylatuje? w pierwszym kanale jeszcze sa
                // headery replyChannel, errorChannel, w drugim już ich brak - zmienia sie tez id wiadomości,
                // tylko payload takie samo

                .transform(message -> {
                    System.out.println("message in another channel: " + message);
                    return message;
                })
                .handle(handlerInter)
                .routeToRecipients(r -> r.recipientFlow(msg -> true, f -> f.channel(Utilis.TEST_CHANNEL2)))
                .channel(Utilis.TEST_CHANNEL2)
                .transform(message -> {
                    System.out.println("payload after handler inside transform: " + message);
                    return message;
                });
//                .handle(handlerVoid);

//                .bridge(null);
    }

    private class PHandler extends AbstractMessageHandler {

        @Override
        protected void handleMessageInternal(Message<?> message) throws Exception {
            System.out.println(message);
        }
    }
}
