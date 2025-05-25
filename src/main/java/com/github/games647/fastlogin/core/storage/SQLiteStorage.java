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
package com.github.games647.fastlogin.core.storage;

import com.github.games647.fastlogin.core.shared.PlatformPlugin;
import com.zaxxer.hikari.HikariConfig;
import org.sqlite.JDBC;
import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SQLiteStorage extends SQLStorage {

    protected static final String CREATE_TABLE_STMT = "CREATE TABLE IF NOT EXISTS `" + PREMIUM_TABLE + "` ("
            + "`UserID` INTEGER PRIMARY KEY AUTO_INCREMENT, "
            + "`UUID` CHAR(36), "
            + "`Name` VARCHAR(16) NOT NULL, "
            + "`Premium` BOOLEAN NOT NULL, "
            + "`LastIp` VARCHAR(255) NOT NULL, "
            + "`LastLogin` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            //the premium shouldn't steal the cracked account by changing the name
            + "UNIQUE (`Name`) "
            + ')';

    private static final String SQLITE_DRIVER = "org.sqlite.SQLiteDataSource";
    private final Lock lock = new ReentrantLock();

    public SQLiteStorage(PlatformPlugin<?> plugin, String databasePath, HikariConfig config) {
        super(plugin.getLog(), plugin.getName(), plugin.getThreadFactory(),
                setParams(config, replacePathVariables(plugin.getPluginFolder(), databasePath)));
    }

    private static HikariConfig setParams(HikariConfig config, String path) {
        config.setDataSourceClassName(SQLITE_DRIVER);

        config.setConnectionTestQuery("SELECT 1");
        config.setMaximumPoolSize(1);

        config.addDataSourceProperty("url", JDBC.PREFIX + path);

        // a try to fix https://www.spigotmc.org/threads/fastlogin.101192/page-26#post-1874647
        // format strings retrieved by the timestamp column to match them from MySQL
        // vs the default: yyyy-MM-dd HH:mm:ss.SSS
        try {
            SQLiteConfig.class.getDeclaredMethod("setDateStringFormat", String.class);

            SQLiteConfig sqLiteConfig = new SQLiteConfig();
            sqLiteConfig.setDateStringFormat("yyyy-MM-dd HH:mm:ss");
            config.addDataSourceProperty("config", sqLiteConfig);
        } catch (NoSuchMethodException noSuchMethodException) {
            // Versions below this driver version do set the default timestamp value, so this change is not necessary
        }

        return config;
    }

    @Override
    public StoredProfile loadProfile(String name) {
        lock.lock();
        try {
            return super.loadProfile(name);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public StoredProfile loadProfile(UUID uuid) {
        lock.lock();
        try {
            return super.loadProfile(uuid);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void save(StoredProfile playerProfile) {
        lock.lock();
        try {
            super.save(playerProfile);
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected String getCreateTableStmt() {
        // SQLite has a different syntax for auto increment
        return CREATE_TABLE_STMT.replace("AUTO_INCREMENT", "AUTOINCREMENT");
    }

    private static String replacePathVariables(Path dataFolder, String input) {
        String pluginFolder = dataFolder.toAbsolutePath().toString();
        return input.replace("{pluginDir}", pluginFolder);
    }
}
