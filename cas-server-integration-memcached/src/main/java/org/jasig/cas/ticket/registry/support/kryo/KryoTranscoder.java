/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.ticket.registry.support.kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;

import org.jasig.cas.authentication.BasicCredentialMetaData;
import org.jasig.cas.authentication.DefaultHandlerResult;
import org.jasig.cas.authentication.ImmutableAuthentication;
import org.jasig.cas.authentication.principal.SimpleWebApplicationServiceImpl;
import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredServiceImpl;
import org.jasig.cas.ticket.ServiceTicketImpl;
import org.jasig.cas.ticket.TicketGrantingTicketImpl;
import org.jasig.cas.ticket.registry.support.kryo.serial.RegisteredServiceSerializer;
import org.jasig.cas.ticket.registry.support.kryo.serial.SimpleWebApplicationServiceSerializer;
import org.jasig.cas.ticket.registry.support.kryo.serial.URLSerializer;
import org.jasig.cas.ticket.support.HardTimeoutExpirationPolicy;
import org.jasig.cas.ticket.support.MultiTimeUseOrTimeoutExpirationPolicy;
import org.jasig.cas.ticket.support.NeverExpiresExpirationPolicy;
import org.jasig.cas.ticket.support.RememberMeDelegatingExpirationPolicy;
import org.jasig.cas.ticket.support.ThrottledUseAndTimeoutExpirationPolicy;
import org.jasig.cas.ticket.support.TicketGrantingTicketExpirationPolicy;
import org.jasig.cas.ticket.support.TimeoutExpirationPolicy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaDateTimeSerializer;
import org.slf4j.impl.CasDelegatingLogger;

/**
 * {@link net.spy.memcached.MemcachedClient} transcoder implementation based on Kryo fast serialization framework
 * suited for efficient serialization of tickets.
 *
 * @author Marvin S. Addison
 * @since 3.0.0
 */
@SuppressWarnings("rawtypes")
public class KryoTranscoder implements Transcoder<Object> {

    /** Kryo serializer. */
    private final Kryo kryo = new Kryo();

    /** Logging instance. */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Map of class to serializer that handles it. */
    private Map<Class<?>, Serializer> serializerMap;

    /**
     * Creates a Kryo-based transcoder.
     */
    public KryoTranscoder() {
    }

    /**
     * Sets a map of additional types that should be regisetered with Kryo,
     * for example GoogleAccountsService and OpenIdService.
     *
     * @param map Map of class to the serializer instance that handles it.
     */
    public void setSerializerMap(final Map<Class<?>, Serializer> map) {
        this.serializerMap = map;
    }

    /**
     * Initialize and register classes with kryo.
     */
    public void initialize() {
        // Register types we know about and do not require external configuration
        kryo.register(ArrayList.class);
        kryo.register(BasicCredentialMetaData.class);
        kryo.register(Class.class, new DefaultSerializers.ClassSerializer());
        kryo.register(Date.class, new DefaultSerializers.DateSerializer());
        kryo.register(HardTimeoutExpirationPolicy.class);
        kryo.register(HashMap.class);
        kryo.register(DefaultHandlerResult.class);
        kryo.register(ImmutableAuthentication.class);
        kryo.register(MultiTimeUseOrTimeoutExpirationPolicy.class);
        kryo.register(NeverExpiresExpirationPolicy.class);
        kryo.register(RememberMeDelegatingExpirationPolicy.class);
        kryo.register(ServiceTicketImpl.class);
        kryo.register(SimpleWebApplicationServiceImpl.class, new SimpleWebApplicationServiceSerializer());
        kryo.register(ThrottledUseAndTimeoutExpirationPolicy.class);
        kryo.register(TicketGrantingTicketExpirationPolicy.class);
        kryo.register(TicketGrantingTicketImpl.class);
        kryo.register(TimeoutExpirationPolicy.class);
        kryo.register(URL.class, new URLSerializer());

        // we add these ones for tests only
        kryo.register(RegisteredServiceImpl.class, new RegisteredServiceSerializer());
        kryo.register(RegexRegisteredService.class, new RegisteredServiceSerializer());

        // new serializers to manage Joda dates and immutable collections
        kryo.register(DateTime.class, new JodaDateTimeSerializer());
        kryo.register(CasDelegatingLogger.class, new DefaultSerializers.VoidSerializer());

        // from the kryo-serializers library (https://github.com/magro/kryo-serializers)
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);

        // Register other types
        if (serializerMap != null) {
            for (final Map.Entry<Class<?>, Serializer> clazz : serializerMap.entrySet()) {
                kryo.register(clazz.getKey(), clazz.getValue());
            }
        }

        // don't reinit the registered classes after every write or read
        kryo.setAutoReset(false);
        // don't replace objects by references
        kryo.setReferences(false);
        // Catchall for any classes not explicitly registered
        kryo.setRegistrationRequired(false);
    }

    /**
     * Asynchronous decoding is not supported.
     *
     * @param d Data to decode.
     * @return False.
     */
    @Override
    public boolean asyncDecode(final CachedData d) {
        return false;
    }

    @Override
    public CachedData encode(final Object obj) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (final Output output = new Output(byteStream)) {
            kryo.writeClassAndObject(output, obj);
            output.flush();
            final byte[] bytes = byteStream.toByteArray();
            return new CachedData(0, bytes, bytes.length);
        }
    }

    @Override
    public Object decode(final CachedData d) {
        final byte[] bytes = d.getData();
        try (final Input input = new Input(new ByteArrayInputStream(bytes))) {
            final Object obj =  kryo.readClassAndObject(input);
            return obj;
        }
    }

    /**
     * Maximum size of encoded data supported by this transcoder.
     *
     * @return  <code>net.spy.memcached.CachedData#MAX_SIZE</code>.
     */
    @Override
    public int getMaxSize() {
        return CachedData.MAX_SIZE;
    }

    /**
     * Gets the kryo object that provides encoding and decoding services for this instance.
     *
     * @return Underlying Kryo instance.
     */
    public Kryo getKryo() {
        return kryo;
    }
}
