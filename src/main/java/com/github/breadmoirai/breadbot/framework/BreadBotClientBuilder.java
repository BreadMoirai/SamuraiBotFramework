/*
 *       Copyright 2017 Ton Ly (BreadMoirai)
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
package com.github.breadmoirai.breadbot.framework;

import com.github.breadmoirai.breadbot.framework.builder.*;
import com.github.breadmoirai.breadbot.framework.command.CommandPreprocessor;
import com.github.breadmoirai.breadbot.framework.command.CommandPreprocessorFunction;
import com.github.breadmoirai.breadbot.framework.command.CommandPreprocessorPredicate;
import com.github.breadmoirai.breadbot.framework.command.parameter.ArgumentParser;
import com.github.breadmoirai.breadbot.framework.command.parameter.ArgumentTypeMapper;
import com.github.breadmoirai.breadbot.framework.command.parameter.ArgumentTypePredicate;
import com.github.breadmoirai.breadbot.framework.command.parameter.CommandArgument;
import com.github.breadmoirai.breadbot.framework.internal.ArgumentTypes;
import com.github.breadmoirai.breadbot.framework.internal.ArgumentTypesImpl;
import com.github.breadmoirai.breadbot.framework.internal.CommandPropertiesImpl;
import com.github.breadmoirai.breadbot.framework.event.CommandEvent;
import com.github.breadmoirai.breadbot.framework.event.ICommandEventFactory;
import com.github.breadmoirai.breadbot.framework.event.impl.CommandEventFactoryImpl;
import com.github.breadmoirai.breadbot.framework.internal.BreadBotClientImpl;
import com.github.breadmoirai.breadbot.modules.prefix.DefaultPrefixModule;
import com.github.breadmoirai.breadbot.modules.prefix.PrefixModule;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.hooks.AnnotatedEventManager;
import net.dv8tion.jda.core.hooks.IEventManager;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import net.dv8tion.jda.core.utils.Checks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.*;

public class BreadBotClientBuilder implements
        CommandHandleBuilderFactory<BreadBotClientBuilder>,
        CommandModuleBuilder<BreadBotClientBuilder>,
        CommandProperties,
        ArgumentTypes {

    private static final Logger LOG = LoggerFactory.getLogger(BreadBotClientBuilder.class);

    private final List<CommandModule> modules;
    private final CommandProperties commandProperties;
    private final ArgumentTypes argumentTypes;
    private final CommandHandleBuilderFactoryImpl factory;
    private final List<CommandHandleBuilderInternal> commands;
    private Predicate<Message> preProcessPredicate;
    private ICommandEventFactory commandEventFactory;

    public BreadBotClientBuilder() {
        modules = new ArrayList<>();
        commandProperties = new CommandPropertiesImpl();
        argumentTypes = new ArgumentTypesImpl();
        factory = new CommandHandleBuilderFactoryImpl(this);
        commands = new ArrayList<>();
    }

    @Override
    public BreadBotClientBuilder self() {
        return this;
    }

    @Override
    public BreadBotClientBuilder addModule(Collection<CommandModule> modules) {
        Checks.noneNull(modules, "modules");
        for (CommandModule module : modules) {
            module.initialize(this);
        }
        this.modules.addAll(modules);
        return this;
    }

    @Override
    public BreadBotClientBuilder addModule(CommandModule module) {
        Checks.notNull(module, "module");
        this.modules.add(module);
        module.initialize(this);
        return this;
    }

    @Override
    public boolean hasModule(Class<? extends CommandModule> moduleClass) {
        return moduleClass != null && modules.stream().map(Object::getClass).anyMatch(moduleClass::isAssignableFrom);
    }
    @Override
    public <T extends CommandModule> T getModule(Class<T> moduleClass) {
        return moduleClass == null ? null : modules.stream().filter(module -> moduleClass.isAssignableFrom(module.getClass())).map(moduleClass::cast).findAny().orElse(null);
    }

    @Override
    public CommandHandleBuilder createCommand(Consumer<CommandEvent> onCommand) {
        CommandHandleBuilderInternal commandHandle = factory.createCommand(onCommand);
        commands.add(commandHandle);
        return commandHandle;
    }

    @Override
    public CommandHandleBuilder createCommand(Class<?> commandClass) {
        CommandHandleBuilderInternal commandHandle = factory.createCommand(commandClass);
        commands.add(commandHandle);
        return commandHandle;
    }

    @Override
    public CommandHandleBuilder createCommand(Object commandObject) {
        CommandHandleBuilderInternal commandHandle = factory.createCommand(commandObject);
        commands.add(commandHandle);
        return commandHandle;
    }

    @Override
    public CommandHandleBuilder createCommand(Supplier<?> commandSupplier) {
        CommandHandleBuilderInternal commandHandle = factory.createCommand(commandSupplier);
        commands.add(commandHandle);
        return commandHandle;
    }

    @Override
    public List<CommandHandleBuilder> createCommands(String packageName) {
        List<CommandHandleBuilderInternal> commandHandles = factory.createCommands(packageName);
        commands.addAll(commandHandles);
        return Collections.unmodifiableList(commandHandles);
    }

    @Override
    public List<CommandHandleBuilder> createCommands(Class<?> commandClass) {
        List<CommandHandleBuilderInternal> commandHandles = factory.createCommands(commandClass);
        commands.addAll(commandHandles);
        return Collections.unmodifiableList(commandHandles);
    }

    @Override
    public List<CommandHandleBuilder> createCommands(Object commandObject) {
        List<CommandHandleBuilderInternal> commandHandles = factory.createCommands(commandObject);
        commands.addAll(commandHandles);
        return Collections.unmodifiableList(commandHandles);
    }

    @Override
    public List<CommandHandleBuilder> createCommands(Supplier<?> commandSupplier) {
        List<CommandHandleBuilderInternal> commandHandles = factory.createCommands(commandSupplier);
        commands.addAll(commandHandles);
        return Collections.unmodifiableList(commandHandles);
    }

    @Override
    public List<CommandHandleBuilder> createCommandsFromClasses(Collection<Class<?>> commandClasses) {
        List<CommandHandleBuilderInternal> commandHandles = factory.createCommandsFromClasses(commandClasses);
        commands.addAll(commandHandles);
        return Collections.unmodifiableList(commandHandles);
    }

    @Override
    public List<CommandHandleBuilder> createCommandsFromObjects(Collection<?> commandObjects) {
        List<CommandHandleBuilderInternal> commandHandles = factory.createCommandsFromObjects(commandObjects);
        commands.addAll(commandHandles);
        return Collections.unmodifiableList(commandHandles);
    }

    @Override
    public List<CommandHandleBuilder> createCommandsFromSuppliers(Collection<Supplier<?>> commandSupplier) {
        List<CommandHandleBuilderInternal> commandHandles = factory.createCommandsFromSuppliers(commandSupplier);
        commands.addAll(commandHandles);
        return Collections.unmodifiableList(commandHandles);
    }

    @Override
    public <T> void putCommandModifier(Class<T> propertyType, BiConsumer<T, CommandHandleBuilder> configurator) {
        commandProperties.putCommandModifier(propertyType, configurator);
    }

    @Override
    public <T> void appendCommandModifier(Class<T> propertyType, BiConsumer<T, CommandHandleBuilder> configurator) {
        commandProperties.appendCommandModifier(propertyType, configurator);
    }

    @Override
    public <T> void putParameterModifier(Class<T> propertyType, BiConsumer<T, CommandParameterBuilder> configurator) {
        commandProperties.putParameterModifier(propertyType, configurator);
    }

    @Override
    public <T> void appendParameterModifier(Class<T> propertyType, BiConsumer<T, CommandParameterBuilder> configurator) {
        commandProperties.appendParameterModifier(propertyType, configurator);
    }

    @Override
    public void applyModifiers(CommandHandleBuilder builder) {
        commandProperties.applyModifiers(builder);
    }

    @Override
    public <T> BiConsumer<T, CommandHandleBuilder> getCommandModifier(Class<T> propertyType) {
        return commandProperties.getCommandModifier(propertyType);
    }

    @Override
    public <T> void applyCommandModifier(Class<T> propertyType, CommandHandleBuilder builder) {
        commandProperties.applyCommandModifier(propertyType, builder);
    }

    @Override
    public void applyModifiers(CommandParameterBuilder builder) {
        commandProperties.applyModifiers(builder);
    }

    @Override
    public <T> BiConsumer<T, CommandParameterBuilder> getParameterModifier(Class<T> propertyType) {
        return commandProperties.getParameterModifier(propertyType);
    }

    @Override
    public <T> void applyParameterModifier(Class<T> propertyType, CommandParameterBuilder builder) {
        commandProperties.applyParameterModifier(propertyType, builder);
    }

    @Override
    public <T> void associatePreprocessorFactory(String identifier, Class<T> propertyType, Function<T, CommandPreprocessorFunction> factory) {
        commandProperties.associatePreprocessorFactory(identifier, propertyType, factory);
    }

    @Override
    public <T> void associatePreprocessorPredicateFactory(String identifier, Class<T> propertyType, Function<T, CommandPreprocessorPredicate> factory) {
        commandProperties.associatePreprocessorPredicateFactory(identifier, propertyType, factory);
    }

    @Override
    public void associatePreprocessor(String identifier, Class<?> propertyType, CommandPreprocessorFunction function) {
        commandProperties.associatePreprocessor(identifier, propertyType, function);
    }

    @Override
    public void associatePreprocessorPredicate(String identifier, Class<?> propertyType, CommandPreprocessorPredicate predicate) {
        commandProperties.associatePreprocessorPredicate(identifier, propertyType, predicate);
    }

    @Override
    public List<String> getPreprocessorPriorityList() {
        return commandProperties.getPreprocessorPriorityList();
    }

    @Override
    public void setPreprocessorPriority(String... identifiers) {
        commandProperties.setPreprocessorPriority(identifiers);
    }

    @Override
    public void setPreprocessorPriority(List<String> identifierList) {
        commandProperties.setPreprocessorPriority(identifierList);
    }

    @Override
    public Comparator<CommandPreprocessor> getPriorityComparator() {
        return commandProperties.getPriorityComparator();
    }

    /**
     * Registers an ArgumentMapper with the type provided.
     *  @param type   the Type class
     * @param predicate may be null. This returns {@code true} if the {@link CommandArgument} can be mapped to the {@code type}. If the computation cost is similar to mapping the argument, leave this field null.
     * @param mapper the mapper
     */
    public <T> void registerArgumentMapper(Class<T> type, ArgumentTypePredicate predicate, ArgumentTypeMapper<T> mapper) {
        argumentTypes.registerArgumentMapper(type, predicate, mapper);
    }

    /**
     * This ignores flags. Use {@link ArgumentTypes#registerArgumentMapper} otherwise.
     *  @param type      The type class
     * @param isType    predicate to test if the argument can be parsed to the type provided. This param can be left {@code null} if the complexity is close to {@code getAsType.apply(arg) != null}
     * @param getAsType A function to convert the argument to the type provided.
     */
    public <T> void registerArgumentMapperSimple(Class<T> type, Predicate<CommandArgument> isType, Function<CommandArgument, T> getAsType) {
        argumentTypes.registerArgumentMapperSimple(type, isType, getAsType);
    }

    /**
     * Sets a predicate to be used on each message before processing it. This will override any existing predicates.
     * @param predicate a predicate which returns {@code true} if a message should be processed as a command.
     * @return this
     */
    public BreadBotClientBuilder setPreProcessPredicate(Predicate<Message> predicate) {
        preProcessPredicate = predicate;
        return this;
    }

    /**
     * Appends a predicate to be used on each message before processing it.
     * @param predicate a predicate which returns {@code true} if a message should be processed as a command.
     * @return this
     */
    public BreadBotClientBuilder addPreProcessPredicate(Predicate<Message> predicate) {
        if (preProcessPredicate == null) {
            preProcessPredicate = predicate;
        } else {
            preProcessPredicate = preProcessPredicate.and(predicate);
        }
        return this;
    }

    /**
     * Not much use for this at the moment.
     */
    public BreadBotClientBuilder setEventFactory(ICommandEventFactory commandEventFactory) {
        this.commandEventFactory = commandEventFactory;
        return this;
    }

    /**
     * Builds a BreadBotClient with an {@link net.dv8tion.jda.core.hooks.AnnotatedEventManager}
     * <p>
     * This implementation is as follows:
     * <pre><code>
     *     return {@link BreadBotClientBuilder breadBotBuilder}.{@link BreadBotClientBuilder#build build}(new {@link net.dv8tion.jda.core.hooks.AnnotatedEventManager AnnotatedEventManager()});
     * </code></pre>
     *
     * @return The {@link com.github.breadmoirai.breadbot.framework.BreadBotClient} for use with {@link net.dv8tion.jda.core.JDABuilder#setEventManager(IEventManager) JDABuilder#setEventManager}({@link com.github.breadmoirai.breadbot.framework.BreadBotClient#getEventManager client.getEventManager()})
     */
    public BreadBotClient buildAnnotated() {
        return build(new AnnotatedEventManager());
    }

    /**
     * Builds a BreadBotClient with an {@link net.dv8tion.jda.core.hooks.InterfacedEventManager}
     * <p>
     * This implementation is as follows:
     * <pre><code>
     *     return {@link BreadBotClientBuilder breadBotBuilder}.{@link BreadBotClientBuilder#build build}(new {@link net.dv8tion.jda.core.hooks.InterfacedEventManager InterfacedEventManager()});
     * </code></pre>
     *
     * @return The {@link com.github.breadmoirai.breadbot.framework.BreadBotClient} for use with {@link net.dv8tion.jda.core.JDABuilder#setEventManager(IEventManager) JDABuilder#setEventManager}({@link com.github.breadmoirai.breadbot.framework.BreadBotClient#getEventManager client.getEventManager()})
     */
    public BreadBotClient buildInterfaced() {
        return build(new InterfacedEventManager());
    }

    /**
     * Builds the BreadBotClient with the provided EventManager.
     * It is at this point that all Modules are initialized and Commands built.
     * If an {@link PrefixModule} has not been provided, a {@link com.github.breadmoirai.breadbot.modules.prefix.DefaultPrefixModule new DefaultPrefixModule("!")} is provided.
     *
     * @param eventManager The IEventManager of which to attach all the listeners (CommandModules) to. If the module is an instanceof {@link net.dv8tion.jda.core.hooks.InterfacedEventManager} it will only use {@link IEventManager#register(Object)} on Modules that extend {@link net.dv8tion.jda.core.hooks.EventListener}. Otherwise, the BreadBotClient will register all the CommandModules as listeners.
     * @return a new BreadBotClient.
     */
    public BreadBotClient build(IEventManager eventManager) {
        if (!hasModule(PrefixModule.class)) modules.add(new DefaultPrefixModule("!"));
        if (commandEventFactory == null)
            commandEventFactory = new CommandEventFactoryImpl(getModule(PrefixModule.class));

        BreadBotClient client = new BreadBotClientImpl(modules, commands, commandProperties, argumentTypes, eventManager, commandEventFactory, preProcessPredicate);
        LOG.info("Top Level Commands registered: " + client.getCommandMap().values().size() + ".");
        LOG.info("CommandClient Initialized.");
        return client;
    }
}