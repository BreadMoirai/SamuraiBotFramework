/*
 *        Copyright 2017-2018 Ton Ly (BreadMoirai)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.breadmoirai.breadbot.framework.command.internal.builder;

import com.github.breadmoirai.breadbot.framework.builder.BreadBotBuilder;
import com.github.breadmoirai.breadbot.framework.builder.CommandHandleBuilder;
import com.github.breadmoirai.breadbot.framework.builder.CommandParameterBuilder;
import com.github.breadmoirai.breadbot.framework.command.Command;
import com.github.breadmoirai.breadbot.framework.command.CommandPreprocessor;
import com.github.breadmoirai.breadbot.framework.command.CommandResultHandler;
import com.github.breadmoirai.breadbot.framework.command.internal.CommandHandleImpl;
import com.github.breadmoirai.breadbot.framework.command.internal.CommandObjectFactory;
import com.github.breadmoirai.breadbot.framework.command.internal.CommandPropertyMapImpl;
import com.github.breadmoirai.breadbot.framework.command.internal.InvokableCommand;
import com.github.breadmoirai.breadbot.framework.error.BreadBotException;
import com.github.breadmoirai.breadbot.framework.error.MissingCommandKeyException;
import com.github.breadmoirai.breadbot.framework.inject.BreadInjector;
import com.github.breadmoirai.breadbot.framework.parameter.CommandParameter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class CommandHandleBuilderImpl implements CommandHandleBuilderInternal {

    private final Object declaringObject;
    private final Class<?> declaringClass;
    private final Method declaringMethod;
    private String[] keys;
    private String name, group, description;
    private final BreadBotBuilder clientBuilder;
    private final CommandObjectFactory commandFactory;
    private final CommandParameterBuilder[] parameterBuilders;
    private final InvokableCommand commandFunction;
    private final List<CommandHandleBuilderInternal> subCommands;
    private final List<CommandPreprocessor> preprocessors;
    private final CommandPropertyMapImpl propertyMap;
    private transient CommandHandleBuilderFactoryImpl handleBuilderFactory;
    private CommandResultHandler resultHandler;
    private boolean isPersistent = false;
    private boolean shouldRetainProperties;
    private Pattern splitRegex;
    private int splitLimit;

    public CommandHandleBuilderImpl(Object declaringObject,
                                    Class<?> declaringClass,
                                    Method declaringMethod,
                                    BreadBotBuilder clientBuilder,
                                    CommandObjectFactory commandFactory,
                                    CommandParameterBuilder[] parameterBuilders,
                                    InvokableCommand commandFunction,
                                    CommandPropertyMapImpl propertyMap) {
        this.declaringObject = declaringObject;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.clientBuilder = clientBuilder;
        this.commandFactory = commandFactory;
        this.parameterBuilders = parameterBuilders;
        this.commandFunction = commandFunction;
        this.subCommands = new ArrayList<>();
        this.preprocessors = new ArrayList<>();
        this.propertyMap = propertyMap == null ? new CommandPropertyMapImpl(null, null) : propertyMap;
    }

    @Override
    public boolean hasProperty(Class<?> propertyType) {
        return propertyMap.hasProperty(propertyType);
    }

    @Override
    public <T> T getProperty(Class<T> propertyType) {
        return propertyMap.getProperty(propertyType);
    }

    @Override
    public CommandHandleBuilder setKeys(String... keys) {
        this.keys = keys;
        return this;
    }

    @Override
    public CommandHandleBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public CommandHandleBuilder setGroup(String group) {
        this.group = group;
        return this;
    }

    @Override
    public CommandHandleBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public <T> CommandHandleBuilder putProperty(Class<? super T> type, T property) {
        propertyMap.putProperty(type, property);
        return this;
    }

    @Override
    public CommandHandleBuilder putProperty(Object property) {
        propertyMap.putProperty(property);
        return this;
    }

    @Override
    public CommandHandleBuilder addPreprocessor(CommandPreprocessor preprocessor) {
        preprocessors.add(preprocessor);
        return this;
    }

    @Override
    public CommandHandleBuilder setResultHandler(CommandResultHandler resultHandler) {
        this.resultHandler = resultHandler;
        return this;
    }

    @Override
    public List<CommandPreprocessor> getPreprocessors() {
        return preprocessors;
    }

    @Override
    public CommandHandleBuilder sortPreprocessors(Comparator<CommandPreprocessor> preprocessorComparator) {
        preprocessors.sort(preprocessorComparator);
        return null;
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
    public Object getDeclaringObject() {
        return declaringObject;
    }

    @Override
    public BreadBotBuilder getClientBuilder() {
        return clientBuilder;
    }

    @Override
    public CommandHandleBuilder setPersistent(boolean isPersistent) {
        this.isPersistent = isPersistent;
        return this;
    }

    @Override
    public CommandHandleBuilder setSplitRegex(Pattern splitRegex, int splitLimit) {
        this.splitRegex = splitRegex;
        this.splitLimit = splitLimit;
        return this;
    }

    @Override
    public CommandHandleBuilder setRetainProperties(boolean shouldRetainProperties) {
        this.shouldRetainProperties = shouldRetainProperties;
        return this;
    }

    @Override
    public CommandParameterBuilder getParameter(int parameterIndex) {
        return parameterBuilders[parameterIndex];
    }

    @Override
    public String[] getKeys() {
        if (keys == null) return null;
        return Arrays.copyOf(keys, keys.length);
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
    public List<CommandHandleBuilder> getChildren() {
        return Collections.unmodifiableList(subCommands);
    }

    @Override
    public CommandPropertyMapImpl getPropertyMap() {
        return propertyMap;
    }

    @Override
    public void putCommandHandle(CommandHandleBuilderInternal handle) {
        subCommands.add(handle);
    }

    @Override
    public void putCommandHandles(Collection<CommandHandleBuilderInternal> commands) {
        subCommands.addAll(commands);
    }

    @Override
    public CommandHandleBuilderFactoryInternal getCommandFactory() {
        if (handleBuilderFactory == null) {
            handleBuilderFactory = new CommandHandleBuilderFactoryImpl(clientBuilder);
        }
        return handleBuilderFactory;
    }

    @Override
    public CommandObjectFactory getObjectFactory() {
        return commandFactory;
    }

    @Override
    public void setInjector(BreadInjector injector) {
        getObjectFactory().setInjector(injector);
        for (final CommandHandleBuilderInternal subCommand : subCommands) {
            subCommand.setInjector(injector);
        }
    }

    @Override
    public CommandHandleBuilder addCommand(Supplier<?> commandSupplier, Consumer<CommandHandleBuilder> configurator) {
        Object o = commandSupplier.get();
        if (o.getClass().isAnnotationPresent(com.github.breadmoirai.breadbot.framework.annotation.command.Command.class)) {
            CommandHandleBuilderInternal commandHandle = getCommandFactory().createCommand(commandSupplier, o);
            configurator.accept(commandHandle);
            subCommands.add(commandHandle);
        } else {
            List<CommandHandleBuilderInternal> commandHandles = getCommandFactory().createCommands(commandSupplier, o);
            commandHandles.forEach(configurator);
            subCommands.addAll(commandHandles);
        }
        return this;
    }

    @Override
    public CommandHandleBuilder addCommand(Supplier<?> commandSupplier) {
        Object o = commandSupplier.get();
        if (o.getClass().isAnnotationPresent(com.github.breadmoirai.breadbot.framework.annotation.command.Command.class)) {
            CommandHandleBuilderInternal commandHandle = getCommandFactory().createCommand(commandSupplier, o);
            subCommands.add(commandHandle);
        } else {
            List<CommandHandleBuilderInternal> commandHandles = getCommandFactory().createCommands(commandSupplier, o);
            subCommands.addAll(commandHandles);
        }
        return this;
    }

    @Override
    public List<CommandParameterBuilder> getParameters() {
        return Collections.unmodifiableList(Arrays.asList(parameterBuilders));
    }

    @Override
    public CommandHandleImpl build(Command parent) {
        if (keys == null || keys.length == 0) {
            throw new MissingCommandKeyException(this);
        }

        Map<String, CommandHandleImpl> subCommandMap;
        if (subCommands.isEmpty()) {
            subCommandMap = null;
        } else {
            subCommandMap = new HashMap<>();
        }
        CommandObjectFactory commandFactory;
        if (isPersistent) {
            final Object o;
            try {
                o = this.commandFactory.get();
            } catch (Throwable throwable) {
                throw new BreadBotException(throwable);
            }
            commandFactory = CommandObjectFactory.of(o.getClass(), o);
        } else {
            commandFactory = this.commandFactory;
        }
        final CommandParameter[] commandParameters = new CommandParameter[parameterBuilders.length];
        Arrays.setAll(commandParameters, value -> parameterBuilders[value].build());
        if (resultHandler == null && declaringMethod != null) {
            Class<?> returnType = declaringMethod.getReturnType();
            resultHandler = getClientBuilder().getResultHandler(returnType);
        }
        CommandHandleImpl commandHandle = new CommandHandleImpl(keys, name, group, description, declaringObject,
                                                                declaringClass, declaringMethod, commandFactory,
                                                                commandParameters, commandFunction, resultHandler,
                                                                subCommandMap, preprocessors,
                                                                shouldRetainProperties ? propertyMap : null, splitRegex,
                                                                splitLimit, parent);

        //would do null check on sucCommandMap but for loop does not run when subCommands isEmpty
        for (CommandHandleBuilderInternal subCommand : subCommands) {
            CommandHandleImpl command = subCommand.build(commandHandle);
            for (String key : command.getKeys()) {
                //noinspection ConstantConditions
                subCommandMap.put(key, command);
            }
        }
        return commandHandle;
    }

    private String toString(int indent) {
        final StringBuilder sb = new StringBuilder();
        sb.append("CommandHandle{\n");
        tab(sb, indent + 2);
        sb.append("keys: ").append(Arrays.toString(getKeys())).append('\n');
        tab(sb, indent + 2);
        sb.append("name: ").append(getName()).append('\n');
        tab(sb, indent + 2);
        sb.append("group: ").append(getGroup()).append('\n');
        tab(sb, indent + 2);
        sb.append("desc: ").append(getDescription()).append('\n');
        tab(sb, indent + 2);
        sb.append("source: ");
        if (getDeclaringMethod() != null) {
            sb.append(getDeclaringClass().getName()).append('#').append(getDeclaringMethod().getName());
        } else {
            sb.append(getDeclaringClass());
        }
        sb.append('\n');
        tab(sb, indent + 2);
        sb.append("subcommands: [\n");
        StringJoiner sj = new StringJoiner(",\n");
        for (CommandHandleBuilderInternal subCommand : subCommands) {
            sj.add(((CommandHandleBuilderImpl) subCommand).toString(indent + 4));
        }
        tab(sb, indent + 4);
        sb.append(sj);
        tab(sb, indent + 2);
        sb.append("]\n");
        tab(sb, indent);
        sb.append("}");

        return sb.toString();
    }

    private void tab(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(' ');
        }
    }

    @Override
    public CommandHandleBuilder self() {
        return this;
    }

}
