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
package com.github.breadmoirai.bot.framework.event.impl;

import com.github.breadmoirai.bot.framework.BreadBotClient;
import com.github.breadmoirai.bot.framework.event.CommandEvent;
import com.github.breadmoirai.bot.framework.event.ICommandEventFactory;
import com.github.breadmoirai.bot.modules.prefix.IPrefixModule;
import com.github.breadmoirai.bot.util.DiscordPatterns;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.utils.SimpleLog;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

public class CommandEventFactoryImpl implements ICommandEventFactory {

    private static final SimpleLog LOG = SimpleLog.getLog("CommandEvent");

    private final IPrefixModule prefixModule;

    public CommandEventFactoryImpl(IPrefixModule prefixSupplier) {
        this.prefixModule = prefixSupplier;
    }

    @Override
    public CommandEvent createEvent(GenericGuildMessageEvent event, Message message, BreadBotClient client) {
        String prefix = prefixModule.getPrefix(event.getGuild().getIdLong());
        String contentRaw = message.getRawContent();
        final Matcher matcher = DiscordPatterns.USER_MENTION_PREFIX.matcher(contentRaw);
        if (matcher.find() && matcher.start() == 0 && matcher.group(1).equals(event.getJDA().getSelfUser().getId())) {
            contentRaw = contentRaw.substring(matcher.end()).trim();
            return parseContent(event, message, client, prefix, contentRaw);
        } else {
            if (contentRaw.startsWith(prefix)) {
                contentRaw = contentRaw.substring(prefix.length()).trim();
                return parseContent(event, message, client, prefix, contentRaw);
            }
            return null;
        }
    }

    @NotNull
    private CommandEvent parseContent(GenericGuildMessageEvent event, Message message, BreadBotClient client, String prefix, String contentRaw) {
        final String[] split = DiscordPatterns.WHITE_SPACE.split(contentRaw, 2);
        final String key = split[0];
        final String content = split.length > 1 ? split[1].trim() : null;
        if (key.equalsIgnoreCase("help")) {
            if (content == null) return new MessageReceivedCommandEvent(client, event, message, prefix, "help", null, true);
            else {
                final String[] split2 = DiscordPatterns.WHITE_SPACE.split(content, 2);
                final String key2 = split2[0];
                final String content2 = split2.length > 1 ? split2[1].trim() : null;
                return new MessageReceivedCommandEvent(client, event, message, prefix, key2, content2 + " help", true);
            }
        }
        final CommandEvent commandEvent = new MessageReceivedCommandEvent(client, event, message, prefix, key, content, false);
        LOG.trace(commandEvent);
        return commandEvent;
    }


}
