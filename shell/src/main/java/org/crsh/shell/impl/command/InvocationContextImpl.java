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

package org.crsh.shell.impl.command;

import org.crsh.command.CommandContext;
import org.crsh.shell.impl.command.spi.CreateCommandException;
import org.crsh.command.InvocationContext;
import org.crsh.command.ScriptException;
import org.crsh.lang.impl.script.Token;
import org.crsh.shell.ScreenContext;
import org.crsh.lang.impl.script.PipeLineFactory;
import org.crsh.shell.impl.command.spi.CommandInvoker;
import org.crsh.text.Chunk;
import org.crsh.text.RenderPrintWriter;
import org.crsh.util.Utils;

import java.io.IOException;
import java.util.Map;

public final class InvocationContextImpl<P> implements InvocationContext<P> {

  /** . */
  private static final int WRITTEN = 0;

  /** . */
  private static final int FLUSHED = 1;

  /** . */
  private static final int CLOSED = 2;

  /** . */
  private final CommandContext<P> commandContext;

  /** . */
  private RenderPrintWriter writer;

  /** . */
  int status;

  public InvocationContextImpl(CommandContext<P> commandContext) {
    this.commandContext = commandContext;
    this.status = FLUSHED;
  }

  public boolean isPiped() {
    return commandContext.isPiped();
  }

  public RenderPrintWriter getWriter() {
    if (writer == null) {
      writer = new RenderPrintWriter(new ScreenContext() {
        public int getWidth() {
          return InvocationContextImpl.this.getWidth();
        }
        public int getHeight() {
          return InvocationContextImpl.this.getHeight();
        }
        public void write(Chunk chunk) throws IOException {
          InvocationContextImpl.this.write(chunk);
        }
        public void flush() throws IOException {
          InvocationContextImpl.this.flush();
        }
      });
    }
    return writer;
  }

  public boolean takeAlternateBuffer() throws IOException {
    return commandContext.takeAlternateBuffer();
  }

  public boolean releaseAlternateBuffer() throws IOException {
    return commandContext.releaseAlternateBuffer();
  }

  public CommandInvoker<?, ?> resolve(String s) throws ScriptException, IOException {
    CRaSHSession session = (CRaSHSession)getSession();
    Token token2 = Token.parse(s);
    PipeLineFactory factory = token2.createFactory();
    try {
      return factory.create(session);
    }
    catch (CreateCommandException e) {
      throw new ScriptException(e);
    }
  }

  public Class<P> getConsumedType() {
    return commandContext.getConsumedType();
  }

  public String getProperty(String propertyName) {
    return commandContext.getProperty(propertyName);
  }

  public String readLine(String msg, boolean echo) throws IOException, InterruptedException {
    return commandContext.readLine(msg, echo);
  }

  public int getWidth() {
    return commandContext.getWidth();
  }

  public int getHeight() {
    return commandContext.getHeight();
  }

  public void write(Chunk chunk) throws IOException {
    if (status != CLOSED) {
      status = WRITTEN;
      commandContext.write(chunk);
    }
  }

  public void provide(P element) throws IOException {
    if (status != CLOSED) {
      status = WRITTEN;
      commandContext.provide(element);
    }
  }

  public void flush() throws IOException {
    if (status == WRITTEN) {
      status = FLUSHED;
      commandContext.flush();
    }
  }

  public void close() throws IOException {
    if (status != CLOSED) {
      Utils.flush(this);
      status = CLOSED;
      Utils.close(commandContext);
    }
  }

  public Map<String, Object> getSession() {
    return commandContext.getSession();
  }

  public Map<String, Object> getAttributes() {
    return commandContext.getAttributes();
  }

  public InvocationContextImpl<P> leftShift(Object o) throws IOException {
    Class<P> consumedType = getConsumedType();
    if (consumedType.isInstance(o)) {
      P p = consumedType.cast(o);
      provide(p);
    }
    return this;
  }
}
