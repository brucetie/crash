/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.crsh.shell;

import org.crsh.cli.impl.descriptor.IntrospectionException;
import org.crsh.command.BaseCommand;
import org.crsh.lang.impl.java.ClassShellCommand;
import org.crsh.plugin.CRaSHPlugin;
import org.crsh.shell.impl.command.spi.CommandResolver;
import org.crsh.shell.impl.command.spi.CreateCommandException;
import org.crsh.shell.impl.command.spi.Command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Julien Viet
 */
public class CustomCommandResolverTestCase extends AbstractCommandTestCase {

  public static class mycommand extends BaseCommand {
    @org.crsh.cli.Command
    public String main() {
      return "ok_mycommand";
    }
  }

  static class CustomCommandResolver extends CRaSHPlugin<CommandResolver> implements CommandResolver {

    @Override
    public CommandResolver getImplementation() {
      return this;
    }

    @Override
    public Iterable<Map.Entry<String, String>> getDescriptions() {
      return Collections.singletonMap("mycommand", "my command").entrySet();
    }

    @Override
    public Command<?> resolveCommand(String name) throws CreateCommandException, NullPointerException {
      if ("mycommand".equals(name)) {
        try {
          return new ClassShellCommand<mycommand>(mycommand.class);
        }
        catch (IntrospectionException e) {
          throw new CreateCommandException(name, ErrorType.EVALUATION, "Invalid cli annotations", e);
        }
      }
      return null;
    }
  }

  @Override
  protected List<CRaSHPlugin<?>> getPlugins() {
    List<CRaSHPlugin<?>> plugins = super.getPlugins();
    plugins.add(new CustomCommandResolver());
    return plugins;
  }

  public void testFoo() {
    assertEquals("ok_mycommand", assertOk("mycommand"));
    Map<String, String> commands = new HashMap<String, String>();
    for (Map.Entry<String, String> entry : session.getCommands()) {
      commands.put(entry.getKey(), entry.getValue());
    }
    assertTrue("Was expecting " + commands + " to contain mycommand", commands.containsKey("mycommand"));
  }
}
