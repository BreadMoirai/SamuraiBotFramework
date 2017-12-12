package com.github.breadmoirai.tests;

import com.github.breadmoirai.breadbot.framework.BreadBotClient;
import com.github.breadmoirai.breadbot.framework.builder.BreadBotClientBuilder;
import com.github.breadmoirai.breadbot.framework.internal.event.CommandEventInternal;
import com.github.breadmoirai.tests.commands.SSICommand;
import org.junit.Test;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

import static com.github.breadmoirai.tests.MockFactory.mockCommand;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class ParameterTester {

    static {
        TestLoggerFactory.getInstance().setPrintLevel(Level.INFO);
    }

    private BreadBotClient client;

    @Test
    public void width1() {
        client = new BreadBotClientBuilder()
                .addCommand(SSICommand.class)
                .build();
        assertResponse("!ssi", "null, null, null");
        assertResponse("!ssi a b", "a, b, null");
        assertResponse("!ssi 1 a b", "1, a, null");
        assertResponse("!sis a 1 b", "a, 1, b");
        assertResponse("!sis 1 a b", "1, null, a");
        assertResponse("!sis a b 1", "a, 1, b");
        assertResponse("!iss a", "null, a, null");
    }

    @Test
    public void width0() {
        client = new BreadBotClientBuilder()
                .addCommand(SSICommand.class, handleBuilder -> handleBuilder.getParameters().forEach(param -> param.setWidth(0)))
                .build();
        assertResponse("!ssi", "null, null, null");
        assertResponse("!ssi a b 1", "a b 1, null, null");
        assertResponse("!iss a b 1", "1, a b, null");
        assertResponse("!iss a b 1 c \"ddd\"", "1, a b, c ddd");
    }

    @Test
    public void widthNegative() {
        client = new BreadBotClientBuilder()
                .addCommand(SSICommand.class, handleBuilder -> handleBuilder.getParameters().forEach(param -> param.setWidth(-1)))
                .build();
        assertResponse("!ssi", "null, null, null");
        assertResponse("!ssi a b 1", "a b 1, null, null");
        assertResponse("!iss a b 1", "null, a b 1, null");
        assertResponse("!iss 1 a b", "null, 1 a b, null");
    }

    @Test
    public void widthPositive() {
        client = new BreadBotClientBuilder()
                .addCommand(SSICommand.class, handleBuilder -> handleBuilder.getParameters().forEach(param -> param.setWidth(2)))
                .build();
        assertResponse("!ssi", "null, null, null");
        assertResponse("!ssi a b 1", "a b, null, null");
        assertResponse("!iss a b 1 2", "null, a b, 1 2");
        assertResponse("!iss 1 a b 2 4", "null, 1 a, b 2");
    }


    private void assertResponse(final String input, final String expected) {
        CommandEventInternal spy = mockCommand(client, input);


        doAnswer(invocation -> {
            String argument = invocation.getArgument(0);
            assertEquals(expected, argument);
            return null;
        }).when(spy).reply(anyString());

        client.getCommandEngine().handle(spy);

        verify(spy).reply(expected);
    }
}
