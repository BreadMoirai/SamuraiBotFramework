package com.github.breadmoirai.bot.framework.command.parameter.impl;

import com.github.breadmoirai.bot.framework.event.CommandEvent;
import net.dv8tion.jda.core.entities.TextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TextChannelArgument extends MentionArgument {

    private final TextChannel channel;

    public TextChannelArgument(CommandEvent event, String s, TextChannel textChannel) {
        super(event, s);
        this.channel = textChannel;
    }

    @Override
    public boolean isTextChannel() {
        return true;
    }

    @Override
    public boolean isValidTextChannel() {
        return true;
    }

    @NotNull
    @Override
    public TextChannel getTextChannel() {
        return channel;
    }

    @NotNull
    @Override
    public Optional<TextChannel> findTextChannel() {
        return Optional.of(channel);
    }

    @NotNull
    @Override
    public List<TextChannel> searchTextChannels() {
        return Collections.singletonList(channel);
    }
}