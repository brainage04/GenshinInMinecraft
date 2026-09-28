package io.github.brainage04.genshininminecraft.command.core;

import net.minecraft.network.chat.Component;

/**
 * Sends client-side command feedback through the loader's own client command source type.
 *
 * @param <S> the loader's client command source type
 */
@FunctionalInterface
public interface ClientCommandFeedback<S> {
    void sendFeedback(S source, Component message);
}
