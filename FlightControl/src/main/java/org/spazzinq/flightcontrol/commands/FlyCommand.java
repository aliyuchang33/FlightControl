/*
 * This file is part of FlightControl-parent, which is licensed under the MIT License
 *
 * Copyright (c) 2019 Spazzinq
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

package org.spazzinq.flightcontrol.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.spazzinq.flightcontrol.FlightControl;

public final class FlyCommand implements CommandExecutor {
    private FlightControl pl;
    public FlyCommand(FlightControl pl) { this.pl = pl; }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (s instanceof Player) {
                if (s.hasPermission("flightcontrol.fly") || s.hasPermission("essentials.fly")) {
                    Player p = (Player) s;
                    if (p.getAllowFlight()) { pl.manager.disableFlight(p); pl.manager.notif.add(p); }
                    else pl.manager.check(p, p.getLocation(), true);
                } else FlightControl.msg(s, pl.config.noPerm);
            } else pl.getLogger().info("Only players can use this command (the console can't fly, can it?)");
        } else if (args.length == 1) pl.tempFlyCommand.onCommand(s, cmd, label, args);
        return true;
    }
}