package ti4.draft;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
public class EmptyRestAction implements RestAction<Void> {
    @Override
    public JDA getJDA() {
        return null;
    }

    @Override
    public RestAction<Void> setCheck(BooleanSupplier booleanSupplier) {
        return this;
    }

    @Override
    public void queue(Consumer<? super Void> consumer, Consumer<? super Throwable> consumer1) {
        if (consumer != null) {
            consumer.accept(null);
        }
    }

    @Override
    public Void complete(boolean b) throws RateLimitedException {
        return null;
    }

    @Override
    public CompletableFuture<Void> submit(boolean b) {
        var c = new CompletableFuture<Void>();
        c.complete(null);
        return c;
    }
}
