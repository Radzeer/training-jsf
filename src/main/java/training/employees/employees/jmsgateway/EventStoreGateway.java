package training.employees.employees.jmsgateway;

import lombok.AllArgsConstructor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EventStoreGateway {

    private JmsTemplate jmsTemplate;

    public void sendEvent(String message){
        jmsTemplate.convertAndSend("eventsQueue",String.format("""
                {
                    "message": "%s"
                }
                """,message),msg -> {
                                        msg.setStringProperty("_typeId", "CreateEventCommand");
                                        return msg;
                                    });
    }
}
