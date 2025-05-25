/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.bukkit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.games647.fastlogin.core.scheduler.AsyncScheduler;

public class BukkitScheduler extends AsyncScheduler {

    private final Executor syncExecutor;
    private final Plugin plugin;

    public BukkitScheduler(Plugin plugin, Logger logger) {
        super(logger, command -> UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(command));
        this.plugin = plugin;
        this.syncExecutor = task -> UniversalScheduler.getScheduler(plugin).runTask(task);
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(
            () -> UniversalScheduler.getScheduler(plugin).runTaskAsynchronously(runnable)
        );
    }

    public CompletableFuture<Void> runLater(Runnable runnable, long delayTicks) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        UniversalScheduler.getScheduler(plugin).runTaskLater(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, delayTicks);
        return future;
    }

    public Executor getSyncExecutor() {
        return syncExecutor;
    }
}
