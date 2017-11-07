package com.github.breadmoirai.breadbot.framework.command.internal;

import com.github.breadmoirai.breadbot.framework.command.CommandHandle;
import com.github.breadmoirai.breadbot.framework.command.CommandPreprocessor;
import com.github.breadmoirai.breadbot.framework.command.CommandProcessStack;
import com.github.breadmoirai.breadbot.framework.command.CommandPropertyMap;
import com.github.breadmoirai.breadbot.framework.command.parameter.CommandParameter;
import com.github.breadmoirai.breadbot.framework.command.parameter.CommandParser;
import com.github.breadmoirai.breadbot.framework.event.CommandEvent;
import com.github.breadmoirai.breadbot.util.EventStringIterator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CommandHandleImpl implements CommandHandle {

    private final String[] keys;
    private final String name;
    private final String group;
    private final String description;
    private final Object declaringObject;
    private final Class<?> declaringClass;
    private final Method declaringMethod;
    //    private final BreadBotClientImpl client;
    private final CommandObjectFactory commandSupplier;
    private final CommandParameter[] commandParameters;
    private final InvokableCommand invokableCommand;
    private final Map<String, CommandHandle> subCommandMap;
    private final List<CommandPreprocessor> preprocessors;
    private final CommandPropertyMap propertyMap;

    public CommandHandleImpl(String[] keys,
                             String name,
                             String group,
                             String description,
                             Object declaringObject,
                             Class<?> declaringClass,
                             Method declaringMethod,
//                             BreadBotClientImpl client,
                             CommandObjectFactory commandSupplier,
                             CommandParameter[] commandParameters,
                             InvokableCommand commandFunction,
                             Map<String, CommandHandle> subCommandMap,
                             List<CommandPreprocessor> preprocessors,
                             CommandPropertyMap propertyMap) {
        this.keys = keys;
        this.name = name;
        this.group = group;
        this.description = description;
        this.declaringObject = declaringObject;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
//        this.client = client;
        this.commandSupplier = commandSupplier;
        this.commandParameters = commandParameters;
        this.invokableCommand = commandFunction;
        this.subCommandMap = subCommandMap;
        this.preprocessors = preprocessors;
        this.propertyMap = propertyMap;
    }

    @Override
    public boolean handle(CommandEvent event) {
        return handle(event, new EventStringIterator(event));
    }

    @Override
    public boolean handle(CommandEvent event, Iterator<String> keyItr) {
        if (keyItr != null && keyItr.hasNext() && subCommandMap != null) {
            String next = keyItr.next().toLowerCase();
            if (subCommandMap.containsKey(next)) {
                CommandHandle subHandle = subCommandMap.get(next);
                if (event.isHelpEvent()) {
                    return subHandle.handle(event, keyItr) || (subCommandMap.containsKey("help") && subCommandMap.get("help").handle(event, null));
                } else {
                    return subHandle.handle(event, keyItr) || runThis(event);
                }
            }
        }
        return runThis(event);
    }

    private boolean runThis(CommandEvent event) {
        Object commandObj = commandSupplier.get();
        if (invokableCommand != null) {
            final CommandParser parser = new CommandParser(event, this, event.getArguments(), commandParameters);
            final CommandRunner runner = new CommandRunner(commandObj, event, invokableCommand, parser, Throwable::printStackTrace); //todo log this
            final CommandProcessStack commandProcessStack = new CommandProcessStack(commandObj, this, event, preprocessors, runner);
            commandProcessStack.runNext();
            return commandProcessStack.result();
        } else return false;
    }

    @Override
    public String[] getKeys() {
        return keys;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getDescription() {
        return description;
    }


    @Override
    public Object getDeclaringObject() {
        return declaringObject;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public Method getDeclaringMethod() {
        return declaringMethod;
    }

    @Override
    public CommandPropertyMap getPropertyMap() {
        return propertyMap;
    }

    @Override
    public List<CommandPreprocessor> getPreprocessors() {
        return Collections.unmodifiableList(preprocessors);
    }

    @Override
    public Map<String, CommandHandle> getSubCommandMap() {
        return subCommandMap;
    }
}
