//
//  ========================================================================
//  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.jsr356.messages;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import javax.websocket.Decoder;
import javax.websocket.Decoder.TextStream;

import org.eclipse.jetty.websocket.common.events.EventDriver;
import org.eclipse.jetty.websocket.common.message.MessageSink;
import org.eclipse.jetty.websocket.common.message.MessageInputStream;
import org.eclipse.jetty.websocket.common.message.MessageReader;

public class JsrReaderMessage implements MessageSink
{
    private final EventDriver events;
    private final Decoder.TextStream<?> decoder;
    private final Executor executor;
    private MessageReader stream = null;        

    public JsrReaderMessage(TextStream<?> decoder, EventDriver events, Executor executor)
    {
        this.decoder = decoder;
        this.events = events;
        this.executor = executor;
    }

    @Override
    public void appendFrame(ByteBuffer framePayload, boolean fin) throws IOException
    {
        boolean first = (stream == null);

        stream = new MessageReader(new MessageInputStream());
        stream.appendFrame(framePayload,fin);
        if (first)
        {
            executor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if (decoder != null)
                        {
                            Object o = decoder.decode(stream);
                            events.onObject(o);
                        }
                        else
                        {
                            events.onReader(stream);
                        }
                    }
                    catch (Throwable t)
                    {
                        events.onError(t);
                    }
                }
            });
        }
    }

    @Override
    public void messageComplete()
    {
        stream.messageComplete();
        stream = null;
    }
}
