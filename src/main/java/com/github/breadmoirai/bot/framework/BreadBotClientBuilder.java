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
package com.github.breadmoirai.bot.framework;

import com.github.breadmoirai.bot.framework.command.Command;
import com.github.breadmoirai.bot.framework.command.CommandHandleBuilder;
import com.github.breadmoirai.bot.framework.command.CommandProperties;
import com.github.breadmoirai.bot.framework.command.impl.CommandHandleBuilderFactory;
import com.github.breadmoirai.bot.framework.event.CommandEvent;
import com.github.breadmoirai.bot.framework.event.ICommandEventFactory;
import com.github.breadmoirai.bot.framework.event.impl.CommandEventFactoryImpl;
import com.github.breadmoirai.bot.framework.impl.BreadBotClientImpl;
import com.github.breadmoirai.bot.modules.admin.DefaultAdminModule;
import com.github.breadmoirai.bot.modules.prefix.DefaultPrefixModule;
import com.github.breadmoirai.bot.modules.prefix.IPrefixModule;
import com.github.breadmoirai.bot.modules.source.SourceModule;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.hooks.AnnotatedEventManager;
import net.dv8tion.jda.core.hooks.IEventManager;
import net.dv8tion.jda.core.hooks.InterfacedEventManager;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BreadBotClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger("CommandClient");

    private final List<ICommandModule> modules;
    private ICommandEventFactory commandEventFactory;
    private CommandProperties commandProperties;
    private Predicate<Message> preProcessPredicate;
    private List<CommandHandleBuilder> commands;
    private CommandHandleBuilderFactory factory;

    public BreadBotClientBuilder() {
        commandProperties = new CommandProperties();
        modules = new ArrayList<>();
        commands = new ArrayList<>();
        factory = new CommandHandleBuilderFactory(this);
    }

    public BreadBotClientBuilder addModule(ICommandModule... modules) {
        for (ICommandModule module : modules) {
            module.initialize(this);
        }
        return this;
    }

    public BreadBotClientBuilder addModule(Collection<ICommandModule> modules) {
        for (ICommandModule module : modules) {
            addModule(module);
        }
        return this;
    }

    public boolean hasModule(Class<? extends ICommandModule> moduleClass) {
        return moduleClass != null && modules.stream().map(Object::getClass).anyMatch(moduleClass::isAssignableFrom);
    }

    /**
     * Finds and returns the first Module that is assignable to the provided {@code moduleClass}
     *
     * @param moduleClass The class of the Module to find
     * @return The module if found. Else {@code null}.
     */
    public <T extends ICommandModule> T getModule(Class<T> moduleClass) {
        //noinspection unchecked
        return moduleClass == null ? null : modules.stream().filter(module -> moduleClass.isAssignableFrom(module.getClass())).map(iModule -> (T) iModule).findAny().orElse(null);
    }


    public CommandHandleBuilder createCommand(Consumer<CommandEvent> onCommand) {
        CommandHandleBuilder commandHandleBuilder = factory.fromConsumer(onCommand);
        commands.add(commandHandleBuilder);
        return commandHandleBuilder;
    }

    public <T> CommandHandleBuilder createCommand(Class<T> commandClass, @Nullable T object) throws NoSuchMethodException, IllegalAccessException {

        CommandHandleBuilder commandHandleBuilder = factory.fromClass(commandClass, object, null);
        commands.add(commandHandleBuilder);
        return commandHandleBuilder;
    }

    public <T> CommandHandleBuilder createCommand(T object) throws NoSuchMethodException, IllegalAccessException {
        @SuppressWarnings("unchecked") CommandHandleBuilder commandHandleBuilder = factory.fromClass((Class<T>) object.getClass(), object, null);
        commands.add(commandHandleBuilder);
        return commandHandleBuilder;
    }

    public <T> List<CommandHandleBuilder> createCommands(String packageName) throws NoSuchMethodException, IllegalAccessException {
        List<CommandHandleBuilder> builders = new ArrayList<>();
        final Reflections reflections = new Reflections(packageName);
        final Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);
        for (Class<?> commandClass : classes) {
            final int mod = commandClass.getModifiers();
            if (commandClass.isInterface()
                    || commandClass.isSynthetic()
                    || commandClass.isAnonymousClass()
                    || commandClass.isArray()
                    || commandClass.isAnnotation()
                    || commandClass.isEnum()
                    || commandClass.isPrimitive()
                    || commandClass.isLocalClass()
                    || commandClass.isMemberClass()
                    || Modifier.isAbstract(mod)
                    || Modifier.isPrivate(mod)
                    || Modifier.isProtected(mod))
                continue;
            Stream<GenericDeclaration> classStream = Stream.concat(Stream.concat(Stream.of(commandClass), Arrays.stream(commandClass.getMethods())), Arrays.stream(commandClass.getClasses()));
            boolean hasCommandAnnotation = classStream.map(AnnotatedElement::getAnnotations)
                    .flatMap(Arrays::stream)
                    .map(Annotation::annotationType)
                    .anyMatch(aClass -> aClass == Command.class);
            if (!hasCommandAnnotation) continue;
            builders.add(createCommand(commandClass, null));
        }
        return builders;
    }

    /**
     * This module provides a static prefix that cannot be changed. By default, the prefix is set to "!".
     *
     * <p>This method's implementation is:
     * <pre><code> {@link BreadBotClientBuilder#addModule(ICommandModule...) addModule}(new {@link DefaultPrefixModule DefaultPrefixModule}(prefix)) </code></pre>
     *
     * <p>You can define a different prefix implementation by providing a class to {@link BreadBotClientBuilder#addModule(ICommandModule...)} that implements {@link IPrefixModule IPrefixModule}
     */
    public BreadBotClientBuilder addDefaultPrefixModule(String prefix) {
        addModule(new DefaultPrefixModule(prefix));
        return this;
    }

    /**
     * This enables the {@link com.github.breadmoirai.bot.modules.admin.Admin @Admin} annotation that is marked on Command classes.
     * This ensures that Commands marked with {@link com.github.breadmoirai.bot.modules.admin.Admin @Admin} are only usable by Administrators.
     * <p>It is <b>important</b> to include an implementation of {@link com.github.breadmoirai.bot.modules.admin.IAdminModule IAdminModule} through either this method, {@link BreadBotClientBuilder#addAdminModule(Predicate)}, or your own implementation.
     * Otherwise, all users will have access to Administrative Commands
     * <p>
     * <p>The default criteria for defining an Administrator is as follows:
     * <ul>
     * <li>Has Kick Members Permission</li>
     * <li>Is higher than the bot on the role hierarchy</li>
     * </ul>
     * <p>
     * <p>Different criteria to determine which member has administrative status with {@link BreadBotClientBuilder#addAdminModule(Predicate)}
     * or your own implementation of {@link com.github.breadmoirai.bot.modules.admin.IAdminModule}
     */
    public BreadBotClientBuilder addDefaultAdminModule() {
        addModule(new DefaultAdminModule());
        return this;
    }

    /**
     * Define custom behavior to determine which members can use Commands marked with {@link com.github.breadmoirai.bot.modules.admin.Admin @Admin}
     * <p>
     * <p>This method's implementation is:
     * <pre><code> {@link BreadBotClientBuilder#addModule(ICommandModule...) addModule}(new {@link DefaultAdminModule DefaultAdminModule}(isAdmin)) </code></pre>
     */
    public BreadBotClientBuilder addAdminModule(Predicate<Member> isAdmin) {
        addModule(new DefaultAdminModule(isAdmin));
        return this;
    }

    /**
     * Adding this module will enable {@link com.github.breadmoirai.bot.modules.source.SourceGuild @SourceGuild} annotations on Commands.
     *
     * @param sourceGuildId The guild id
     */
    public BreadBotClientBuilder addSourceModule(long sourceGuildId) {
        addModule(new SourceModule(sourceGuildId));
        return this;
    }

    public BreadBotClientBuilder setPreProcessPredicate(Predicate<Message> predicate) {
        preProcessPredicate = predicate;
        return this;
    }

    public BreadBotClientBuilder addPreProcessPredicate(Predicate<Message> predicate) {
        if (preProcessPredicate == null) {
            preProcessPredicate = predicate;
        } else {
            preProcessPredicate = preProcessPredicate.and(predicate);
        }
        return this;
    }

    public CommandProperties getCommandProperties() {
        return commandProperties;
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
     *
     * This implementation is as follows:
     * <pre><code>
     *     return {@link com.github.breadmoirai.bot.framework.BreadBotClientBuilder breadBotBuilder}.{@link com.github.breadmoirai.bot.framework.BreadBotClientBuilder#build build}(new {@link net.dv8tion.jda.core.hooks.AnnotatedEventManager AnnotatedEventManager()});
     * </code></pre>
     *
     * @return The {@link com.github.breadmoirai.bot.framework.BreadBotClient} for use with {@link net.dv8tion.jda.core.JDABuilder#setEventManager(IEventManager) JDABuilder#setEventManager}({@link com.github.breadmoirai.bot.framework.BreadBotClient#getEventManager client.getEventManager()})
     */
    public BreadBotClient buildAnnotated() {
        return build(new AnnotatedEventManager());
    }

    /**
     * Builds a BreadBotClient with an {@link net.dv8tion.jda.core.hooks.InterfacedEventManager}
     *
     * This implementation is as follows:
     * <pre><code>
     *     return {@link com.github.breadmoirai.bot.framework.BreadBotClientBuilder breadBotBuilder}.{@link com.github.breadmoirai.bot.framework.BreadBotClientBuilder#build build}(new {@link net.dv8tion.jda.core.hooks.InterfacedEventManager InterfacedEventManager()});
     * </code></pre>
     *
     * @return The {@link com.github.breadmoirai.bot.framework.BreadBotClient} for use with {@link net.dv8tion.jda.core.JDABuilder#setEventManager(IEventManager) JDABuilder#setEventManager}({@link com.github.breadmoirai.bot.framework.BreadBotClient#getEventManager client.getEventManager()})
     */
    public BreadBotClient buildInterfaced() {
        return build(new InterfacedEventManager());
    }

    /**
     * Builds the BreadBotClient with the provided EventManager.
     * It is at this point that all Modules are initialized and Commands built.
     * If an {@link com.github.breadmoirai.bot.modules.prefix.IPrefixModule} has not been provided, a {@link com.github.breadmoirai.bot.modules.prefix.DefaultPrefixModule new DefaultPrefixModule("!")} is provided.
     * @param eventManager The IEventManager of which to attach all the listeners (CommandModules) to. If the module is an instanceof {@link net.dv8tion.jda.core.hooks.InterfacedEventManager} it will only use {@link IEventManager#register(Object)} on Modules that extend {@link net.dv8tion.jda.core.hooks.EventListener}. Otherwise, the BreadBotClient will register all the CommandModules as listeners.
     * @return a new BreadBotClient.
     */
    public BreadBotClient build(IEventManager eventManager) {
        if (!hasModule(IPrefixModule.class)) modules.add(new DefaultPrefixModule("!"));
        if (commandEventFactory == null) commandEventFactory = new CommandEventFactoryImpl(getModule(IPrefixModule.class));


        BreadBotClient client = new BreadBotClientImpl(modules, eventManager, commandEventFactory, commands, preProcessPredicate);
        LOG.info("Top Level Commands registered: " + client.getCommandMap().values().size() + ".");
        LOG.info("CommandClient Initialized.");
        return client;
    }

}