package com.github.breadmoirai.breadbot.framework.internal.parameter;

import com.github.breadmoirai.breadbot.framework.CommandArgumentList;
import com.github.breadmoirai.breadbot.framework.internal.argument.GenericCommandArgument;

import java.util.StringJoiner;
import java.util.stream.IntStream;

public class CommandParameterImpl implements CommandParameter {

    private Class<?> type;
    private int flags;
    private int index;
    private int width;
    private ArgumentTypeMapper<?> mapper;
    private boolean mustBePresent;
    private MissingArgumentHandler onParamNotFound;

    public CommandParameterImpl(Class<?> type, int flags, int index, int width, ArgumentTypeMapper<?> mapper, boolean mustBePresent, MissingArgumentHandler onParamNotFound) {
        this.type = type;
        this.flags = flags;
        this.index = index;
        this.width = width;
        this.mapper = mapper;
        this.mustBePresent = mustBePresent;
        this.onParamNotFound = onParamNotFound;
    }

    @Override
    public Object map(CommandArgumentList list, CommandParser set) {
        int i;
        final int limit;
        if (index >= 0) {
            i = index;
            limit = i + 1;
        } else {
            i = 0;
            limit = list.size();
        }

        for (; i < limit; i++) {
            if (set.contains(i)) continue;
            int[] indexes;
            final CommandArgument commandArgument;
            if (width == 1) {
                indexes = new int[]{i};
                commandArgument = list.get(i);
            } else if (width < 1) {
                final StringJoiner sj = new StringJoiner(" ");
                int j;
                for (j = i; j < list.size(); j++) {
                    if (set.contains(j)) break;
                    sj.add(list.get(j).getArgument());
                }
                indexes = IntStream.range(i, j).toArray();
                commandArgument = new GenericCommandArgument(set.getEvent(), sj.toString());
            } else {
                final StringJoiner sj = new StringJoiner(" ");
                for (int j = i; j < i + width && j < list.size(); j++) {
                    sj.add(list.get(i).getArgument());
                }
                indexes = IntStream.range(i, i + width).toArray();
                commandArgument = new GenericCommandArgument(set.getEvent(), sj.toString());
            }
            final Object o = mapper.map(commandArgument, flags);
            if (o != null) {
                set.addAll(indexes);
                return o;
            }
        }

        if (mustBePresent) {
            if (onParamNotFound != null)
                onParamNotFound.handle(list.getEvent(), this);
            set.fail();
        }
        return null;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public boolean isMustBePresent() {
        return mustBePresent;
    }

}