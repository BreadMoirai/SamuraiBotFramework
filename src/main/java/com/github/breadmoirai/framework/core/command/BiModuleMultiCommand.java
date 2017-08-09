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
package com.github.breadmoirai.framework.core.command;

import com.github.breadmoirai.framework.event.CommandEvent;
import com.github.breadmoirai.framework.core.IModule;
import com.github.breadmoirai.framework.util.TypeFinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

public abstract class BiModuleMultiCommand<M1 extends IModule, M2 extends IModule> extends BiModuleCommand<M1, M2> {

    @Override
    public final void execute(CommandEvent event, M1 module1, M2 module2) {
        Commands.invokeCommand(event.getKey().toLowerCase(), this, event, module1, module2);
    }

    @Override
    public boolean isMarkedWith(Class<? extends Annotation> annotation) {
        return super.isMarkedWith(annotation) || (Commands.isAnnotatedWith(getEvent().getKey().toLowerCase(), annotation));
    }

    public static String[] register(Class<? extends BiModuleMultiCommand> commandClass) {
        final Type[] typeArguments = TypeFinder.getTypeArguments(commandClass.getClass(), BiModuleCommand.class);
        final Type moduleType1 = typeArguments[0];
        final Type moduleType2 = typeArguments[1];
        return Arrays.stream(commandClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Command.class))
                .filter(method -> method.getReturnType() == Void.TYPE)
                .filter(method -> method.getParameterCount() == 2)
                .filter(method -> method.getParameterTypes()[0] == CommandEvent.class)
                .filter(method -> method.getParameterTypes()[1] == moduleType1)
                .filter(method -> method.getParameterTypes()[2] == moduleType2)
                .flatMap(method -> Commands.mapMethodKeys(commandClass, method))
                .toArray(String[]::new);
    }


}