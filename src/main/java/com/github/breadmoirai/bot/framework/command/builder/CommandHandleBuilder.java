/*    Copyright 2017 Ton Ly
 
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package com.github.breadmoirai.bot.framework.command.builder;

import com.github.breadmoirai.bot.framework.command.*;
import com.github.breadmoirai.bot.framework.command.impl.CommandHandle;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandHandleBuilder {

    private String name;
    private CommandPropertyMapBuilder propertyBuilder;
    private List<CommandPreprocessor> preprocessorList;

    public CommandHandleBuilder(String name) {
        this.name = name;
        propertyBuilder = new CommandPropertyMapBuilder();
        preprocessorList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public CommandHandleBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public abstract String[] getKeys();

    public abstract CommandHandle build();

    public CommandPropertyMapBuilder getPropertyBuilder() {
        return propertyBuilder;
    }

    public boolean hasProperty(Class<?> propertyType) {
        return getPropertyBuilder().containsProperty(propertyType);
    }

    public <T> T getProperty(Class<T> propertyType) {
        return getPropertyBuilder().getProperty(propertyType);
    }

    public CommandHandleBuilder putProperty(Object property) {
        getPropertyBuilder().putProperty(property);
        return this;
    }

    public <T> CommandHandleBuilder putProperty(Class<? super T> type, T property) {
        getPropertyBuilder().putProperty(type, property);
        return this;
    }

    public CommandHandleBuilder addPreprocessorFunction(String identifier, CommandPreprocessorFunction function) {
        preprocessorList.add(new CommandPreprocessor(identifier, function));
        return this;
    }

    public CommandHandleBuilder addPreprocessorPredicate(String identifier, CommandPreprocessorPredicate predicate) {
        preprocessorList.add(new CommandPreprocessor(identifier, predicate));
        return this;
    }

    public CommandHandleBuilder addPreprocessors(Iterable<CommandPreprocessor> preprocessors) {
        for (CommandPreprocessor preprocessor : preprocessors) {
            preprocessorList.add(preprocessor);
        }
        return this;
    }

    /**
     * Returns a modifiable list of the preprocessors
     *
     * @return a list
     */
    public List<CommandPreprocessor> getPreprocessorList() {
        return preprocessorList;
    }

    public CommandHandleBuilder sortPreprocessors() {
        getPreprocessorList().sort(CommandPreprocessors.getPriorityComparator());
        return this;
    }

    /**
     * Iterates through the currently registered properties and adds associated preprocessors
     */
    public CommandHandleBuilder addAssociatedPreprocessors() {
        CommandPreprocessors.addPrepocessors(this);
        return this;
    }
}