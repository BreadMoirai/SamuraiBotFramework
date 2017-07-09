/*
 *      Copyright 2017 Ton Ly (BreadMoirai)
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
 *
 */
package samurai7.responses.menu;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import samurai7.core.response.Response;
import samurai7.responses.CloseableResponse;
import samurai7.waiter.EventWaiter;

public class ResponseMenu extends Response implements CloseableResponse {

    private transient final Menu menu;
    private transient Message message;

    protected ResponseMenu(Menu menu) {
        this.menu = menu;
    }

    @Override
    public Message buildMessage() {
        return menu.getMessage();
    }

    @Override
    public final void onSend(Message message) {
        this.message = message;
        menu.addReactions(message);
        menu.waitForEvent(this, EventWaiter.get());
    }

    @Override
    public void onDeletion(MessageDeleteEvent event) {
        menu.onDelete(this);
    }


    /**
     * replaces message with {@code newResponse}
     *
     * @param newResponse the response to be used to replace
     */
    public void replaceWith(Response newResponse) {
        newResponse.replace(message);
    }

    /**
     * deletes the menu.
     */
    public void delete() {
        message.delete().queue();
    }

    @Override
    public void cancel(Response cancelMessage, boolean clearReactions) {
        if (clearReactions)
            message.clearReactions().queue();
        cancelMessage.replace(message);
    }

}
